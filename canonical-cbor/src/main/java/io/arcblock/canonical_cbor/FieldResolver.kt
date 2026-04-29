package io.arcblock.canonical_cbor

import org.json.JSONObject

/**
 * Proto schema lookup for canonical CBOR encoding.
 *
 * Schema data comes from `ocap-spec.core.json` (pbjs `proto.json` format,
 * same source the blockchain TypeScript uses via `@ocap/proto/schema`). The
 * file is loaded lazily from classpath resources on first access; parsing
 * and index building happens once per JVM.
 *
 * Equivalent to canonical-cbor.ts `getFields` / `isEnumType` /
 * `toTypeUrl` / `fromTypeUrl`.
 */
internal object FieldResolver {

  /** Schema field descriptor — mirrors the `ProtoField` interface in
   *  canonical-cbor.ts:24. */
  internal data class ProtoField(
    val name: String,
    val id: Int,
    val type: String,
    val rule: String? = null,
    val keyType: String? = null
  )

  internal data class MessageDescriptor(
    val name: String,
    /** field name → descriptor. Ordered by declaration order from the JSON
     *  (not by id) — CanonicalCbor sorts by id at encode time per spec §2. */
    val fields: Map<String, ProtoField>,
    val oneofs: Map<String, List<String>>
  )

  private const val RESOURCE = "/ocap-spec.core.json"
  private const val SCHEMA_ROOT_PATH = "nested.ocap.nested"

  // Lazily initialized on first use.
  @Volatile private var initialized = false
  private val messages = HashMap<String, MessageDescriptor>()
  private val enums = HashMap<String, Map<String, Int>>()
  private val typeUrlByName = HashMap<String, String>()
  private val nameByTypeUrl = HashMap<String, String>()

  /** Force initialization (optional — called implicitly by lookup functions). */
  @Synchronized
  internal fun ensureLoaded() {
    if (initialized) return
    val stream = FieldResolver::class.java.getResourceAsStream(RESOURCE)
      ?: throw CanonicalCborException("schema resource $RESOURCE not found on classpath")
    val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    val root = JSONObject(text)
    indexSchema(root)
    buildTypeUrls()
    initialized = true
  }

  /**
   * Look up fields for a message [type]. Returns null when the type is not
   * declared in the schema (callers decide whether that's an error — the
   * canonical encoder throws, but opaque-payload branches tolerate it).
   */
  internal fun getFields(type: String): Map<String, ProtoField>? {
    ensureLoaded()
    return messages[type]?.fields
  }

  /** True iff [type] names an enum declared at the ocap schema root. */
  internal fun isEnumType(type: String): Boolean {
    ensureLoaded()
    return enums.containsKey(type)
  }

  /** Numeric value for an enum [type]'s named [member]. Returns null when
   *  either the type isn't an enum or the member name doesn't exist. */
  internal fun getEnumValue(type: String, member: String): Int? {
    ensureLoaded()
    return enums[type]?.get(member)
  }

  /**
   * Map a message NAME (e.g. "TransferV2Tx") to its typeUrl
   * (e.g. "fg:t:transfer_v2"). Rules mirror blockchain
   * `core/proto/lib/schema.js createTypeUrls`.
   */
  internal fun toTypeUrl(messageName: String): String {
    ensureLoaded()
    return typeUrlByName[messageName] ?: messageName
  }

  /** Inverse of [toTypeUrl]. Returns input string unchanged when no match
   *  is known — matches TypeScript behavior. */
  internal fun fromTypeUrl(url: String): String {
    ensureLoaded()
    return nameByTypeUrl[url] ?: url
  }

  // ---------------------------------------------------------------------
  // JSON parsing + index building
  // ---------------------------------------------------------------------

  private fun indexSchema(root: JSONObject) {
    val ocapNested = root.optJSONObject("nested")
      ?.optJSONObject("ocap")
      ?.optJSONObject("nested")
      ?: throw CanonicalCborException("schema missing $SCHEMA_ROOT_PATH")

    for (key in ocapNested.keys()) {
      val entry = ocapNested.optJSONObject(key) ?: continue
      when {
        entry.has("fields") -> messages[key] = parseMessage(key, entry)
        entry.has("values") -> enums[key] = parseEnum(entry)
      }
    }
  }

  private fun parseMessage(name: String, entry: JSONObject): MessageDescriptor {
    val fieldsJson = entry.getJSONObject("fields")
    val fields = LinkedHashMap<String, ProtoField>(fieldsJson.length())
    for (fieldName in fieldsJson.keys()) {
      val f = fieldsJson.getJSONObject(fieldName)
      fields[fieldName] = ProtoField(
        name = fieldName,
        id = f.getInt("id"),
        type = f.getString("type"),
        rule = f.optString("rule").takeIf { it.isNotEmpty() },
        keyType = f.optString("keyType").takeIf { it.isNotEmpty() }
      )
    }

    val oneofs = LinkedHashMap<String, List<String>>()
    val oneofsJson = entry.optJSONObject("oneofs")
    if (oneofsJson != null) {
      for (oneofName in oneofsJson.keys()) {
        val members = oneofsJson.getJSONObject(oneofName).optJSONArray("oneof")
        if (members != null) {
          oneofs[oneofName] = (0 until members.length()).map { members.getString(it) }
        }
      }
    }

    return MessageDescriptor(name, fields, oneofs)
  }

  private fun parseEnum(entry: JSONObject): Map<String, Int> {
    val values = entry.getJSONObject("values")
    val out = LinkedHashMap<String, Int>(values.length())
    for (k in values.keys()) out[k] = values.getInt(k)
    return out
  }

  /**
   * Build typeUrl mappings per blockchain core/proto/lib/schema.js rules.
   *
   * - Name ending in `Tx`    → `fg:t:<snake>`    (TransferV2Tx → fg:t:transfer_v2)
   * - Name ending in `State` → `fg:s:<snake>`    (AccountState → fg:s:account)
   * - Prefix `StakeFor`      → `fg:x:stake_<snake>`
   * - `TransactionInfo`      → `fg:x:transaction_info`
   * - `AssetFactoryState`    → `fg:s:asset_factory_state` (override)
   * - `AssetFactory`         → `fg:x:asset_factory` (override)
   * - `DummyCodec`           → `fg:x:address` (override)
   * - Prefix `Request` or `Response` → no remap (keep original name)
   */
  private fun buildTypeUrls() {
    for (name in messages.keys) {
      if (name.startsWith("Request") || name.startsWith("Response")) {
        typeUrlByName[name] = name
        nameByTypeUrl[name] = name
        continue
      }

      val typeUrl: String = when {
        name == "AssetFactoryState" -> "fg:s:asset_factory_state"
        name == "AssetFactory" -> "fg:x:asset_factory"
        name == "DummyCodec" -> "fg:x:address"
        name == "TransactionInfo" -> "fg:x:${toSnakeCase(name)}"
        name.startsWith("StakeFor") ->
          "fg:x:${toSnakeCase("Stake" + name.removePrefix("StakeFor"))}"
        name.endsWith("Tx") -> "fg:t:${toSnakeCase(name.removeSuffix("Tx"))}"
        name.endsWith("State") -> "fg:s:${toSnakeCase(name.removeSuffix("State"))}"
        else -> name
      }

      typeUrlByName[name] = typeUrl
      nameByTypeUrl[typeUrl] = name
    }

    // Explicit overrides that are independent of schema presence. Blockchain
    // JS registers these unconditionally — some (AssetFactory, DummyCodec)
    // do not appear in spec.core.json because they are runtime typeUrls
    // without a message definition, but wallets still need to resolve them.
    for ((name, url) in TYPE_URL_OVERRIDES) {
      typeUrlByName[name] = url
      nameByTypeUrl[url] = name
    }
  }

  private val TYPE_URL_OVERRIDES: Map<String, String> = mapOf(
    "AssetFactoryState" to "fg:s:asset_factory_state",
    "AssetFactory" to "fg:x:asset_factory",
    "DummyCodec" to "fg:x:address",
    "TransactionInfo" to "fg:x:transaction_info"
  )

  // Mirrors JS `lowerUnder`: split before every uppercase letter, join with
  // underscore, lowercase the whole thing. "TransferV2" → "transfer_v2",
  // "AccountMigrate" → "account_migrate".
  private fun toSnakeCase(input: String): String {
    if (input.isEmpty()) return input
    val sb = StringBuilder(input.length + 4)
    for ((i, c) in input.withIndex()) {
      if (i > 0 && c.isUpperCase()) sb.append('_')
      sb.append(c.lowercaseChar())
    }
    return sb.toString()
  }
}
