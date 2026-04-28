package io.arcblock.canonical_cbor

import com.upokecenter.cbor.CBORObject
import io.arcblock.canonical_cbor.BigIntCodec.Kind
import io.arcblock.canonical_cbor.FieldResolver.ProtoField
import java.math.BigInteger

/**
 * Canonical CBOR encoder for OCAP messages.
 *
 * Mirrors the encoder half of canonical-cbor.ts:
 *  - encodeMessageFields (line 361-407)
 *  - encodeFieldValue    (line 292-359)
 *  - encodeAnyValue      (line 231-290)
 *  - resolveAnyTypeUrl   (line 177-199)
 *  - normalizeOpaquePayload (line 215-229)
 *
 * Package-internal. Public entry points live on [CanonicalCbor].
 */
internal object Encoder {

  private val BIGINT_WRAPPER_TYPES = setOf("BigUint", "BigSint")
  private val TIMESTAMP_TYPES = setOf("google.protobuf.Timestamp", "Timestamp")
  private val ANY_TYPES = setOf("google.protobuf.Any", "Any")

  private val OPAQUE_TYPE_URLS = setOf("json", "vc", "fg:x:address")

  /**
   * Encode a message of the given [type] from its [data] map. Output is a
   * CBOR map keyed by proto field ids (inserted in ascending order for
   * deterministic bytes). Default-valued fields are omitted per protobuf3
   * semantics.
   *
   * Special case: when [type] is `BigUint` / `BigSint` and [data] represents
   * the wrapper shape, emit `{1: tagged-bytes, 2?: true}` (§spec 5 —
   * canonical-cbor.ts line 365-373).
   */
  internal fun encodeMessageFields(type: String, data: Map<String, Any?>): CBORObject {
    if (BIGINT_WRAPPER_TYPES.contains(type)) {
      return encodeBigIntTopLevel(type, data)
    }

    val fields = FieldResolver.getFields(type)
      ?: throw CanonicalCborException("canonical-cbor: unknown message type \"$type\"")

    val sortedFields = fields.values.sortedBy { it.id }
    val out = CBORObject.NewMap()

    for (spec in sortedFields) {
      val name = spec.name
      val aliasName = when {
        spec.rule == "repeated" -> "${name}List"
        spec.keyType != null -> "${name}Map"
        else -> null
      }
      val hasCanonical = data.containsKey(name)
      val hasAlias = aliasName != null && data.containsKey(aliasName)
      if (!hasCanonical && !hasAlias) continue
      val rawValue = if (hasCanonical) data[name] else data[aliasName]
      // JS `undefined` fold — explicit null is treated as "field unset".
      // The `in` / `containsKey` check above already picked up the "key
      // present with null value" path, but we still skip it here per
      // canonical-cbor.ts line 387.
      if (rawValue == null) continue

      if (spec.rule == "repeated") {
        if (rawValue !is List<*>) {
          throw CanonicalCborException(
            "canonical-cbor: repeated field \"$name\" expects array"
          )
        }
        if (rawValue.isEmpty()) continue
        out[CBORObject.FromObject(spec.id)] = encodeRepeated(rawValue, spec)
        continue
      }

      if (Scalars.isDefaultScalar(rawValue, spec.type)) continue

      val encoded = encodeFieldValue(rawValue, spec) ?: continue
      out[CBORObject.FromObject(spec.id)] = encoded
    }

    return out
  }

  /**
   * Top-level BigUint / BigSint encoding. Returns an empty map (field
   * omitted) when the magnitude is zero.
   */
  private fun encodeBigIntTopLevel(type: String, data: Map<String, Any?>): CBORObject {
    val kind = if (type == "BigSint") Kind.BIG_SINT else Kind.BIG_UINT
    val repr = BigIntCodec.normalize(data, kind)
    val out = CBORObject.NewMap()
    if (repr !is BigIntCodec.BigIntRepr.Tagged) return out
    out[CBORObject.FromObject(1)] = BigIntCodec.toCbor(repr)
    if (type == "BigSint" && repr.negative) {
      out[CBORObject.FromObject(2)] = CBORObject.True
    }
    return out
  }

  private fun encodeRepeated(values: List<*>, spec: ProtoField): CBORObject {
    val arr = CBORObject.NewArray()
    val itemField = spec.copy(rule = null)
    for (item in values) {
      val encoded = encodeFieldValue(item, itemField)
        ?: throw CanonicalCborException(
          "canonical-cbor: repeated field \"${spec.name}\" contains an " +
            "element that folds to omit — arrays must not contain default values"
        )
      arr.Add(encoded)
    }
    return arr
  }

  /**
   * Encode a single non-repeated field value. Returns null when the value
   * should be omitted (e.g. BigUint wrapper with zero magnitude, Any with
   * undefined value).
   */
  internal fun encodeFieldValue(value: Any?, field: ProtoField): CBORObject? {
    if (value == null) return null

    if (field.keyType != null) {
      throw CanonicalCborException(
        "canonical-cbor: map<${field.keyType},${field.type}> fields are not supported yet"
      )
    }

    val type = field.type

    if (BIGINT_WRAPPER_TYPES.contains(type)) {
      val kind = if (type == "BigSint") Kind.BIG_SINT else Kind.BIG_UINT
      return BigIntCodec.encode(value, kind)
    }

    if (TIMESTAMP_TYPES.contains(type)) {
      return CBORObject.FromObject(Scalars.encodeTimestamp(value))
    }

    if (ANY_TYPES.contains(type)) {
      @Suppress("UNCHECKED_CAST")
      val asMap = value as? Map<String, Any?>
        ?: throw CanonicalCborException(
          "canonical-cbor: Any field expects object, got ${value::class.java.simpleName}"
        )
      return encodeAnyValue(asMap)
    }

    if (FieldResolver.isEnumType(type)) {
      return when (value) {
        is Number -> CBORObject.FromObject(value.toInt())
        is String -> {
          val numeric = FieldResolver.getEnumValue(type, value)
            ?: throw CanonicalCborException("canonical-cbor: unknown enum value")
          CBORObject.FromObject(numeric)
        }
        else -> throw CanonicalCborException(
          "canonical-cbor: invalid enum input for $type"
        )
      }
    }

    if (Scalars.isScalarInt(type)) return Scalars.encodeInteger(value)
    if (Scalars.isScalarFloat(type)) {
      return CBORObject.FromObject((value as Number).toDouble())
    }
    if (type == "bool") return CBORObject.FromObject(value == true)
    if (type == "string") return CBORObject.FromObject(value.toString())
    if (type == "bytes") return encodeBytesField(value)

    // Nested message
    @Suppress("UNCHECKED_CAST")
    val nested = value as? Map<String, Any?>
      ?: throw CanonicalCborException(
        "canonical-cbor: expected object for nested $type"
      )
    return encodeMessageFields(type, nested)
  }

  private fun encodeBytesField(value: Any?): CBORObject {
    return when (value) {
      is ByteArray -> CBORObject.FromObject(value)
      is String -> {
        // Hex string path (matches toUint8Array in TS). Empty string ->
        // empty byte string.
        if (value.isEmpty()) CBORObject.FromObject(ByteArray(0))
        else CBORObject.FromObject(hexToBytes(value))
      }
      is List<*> -> {
        val bytes = ByteArray(value.size)
        for ((i, b) in value.withIndex()) {
          val n = (b as? Number) ?: throw CanonicalCborException(
            "canonical-cbor: bytes list contains non-number"
          )
          bytes[i] = n.toInt().toByte()
        }
        CBORObject.FromObject(bytes)
      }
      else -> throw CanonicalCborException(
        "canonical-cbor: cannot encode bytes field from ${value!!::class.java.simpleName}"
      )
    }
  }

  private fun hexToBytes(hex: String): ByteArray {
    val s = if (hex.startsWith("0x") || hex.startsWith("0X")) hex.substring(2) else hex
    val clean = if (s.length % 2 == 1) "0$s" else s
    val out = ByteArray(clean.length / 2)
    for (i in out.indices) {
      val hi = Character.digit(clean[i * 2], 16)
      val lo = Character.digit(clean[i * 2 + 1], 16)
      if (hi < 0 || lo < 0) {
        throw CanonicalCborException("canonical-cbor: invalid hex in bytes field")
      }
      out[i] = ((hi shl 4) or lo).toByte()
    }
    return out
  }

  // ---------------------------------------------------------------------
  // Any (google.protobuf.Any) — the `itx` field lives here
  // ---------------------------------------------------------------------

  private data class AnyDescriptor(
    val typeUrl: String,
    val messageName: String,
    val inner: Any?,
    val wasUnwrapped: Boolean
  )

  /**
   * Mirror of resolveAnyTypeUrl (canonical-cbor.ts:177-199). The discriminant
   * between "flat" and "friendly" shapes MUST be precise: a naive
   * `value.type != null` check false-positives on messages whose inner has
   * a legitimate `type` field (AccountMigrateTx.type = WalletType). See
   * §spec 7.1.
   */
  private fun resolveAnyTypeUrl(value: Map<String, Any?>): AnyDescriptor {
    val typeUrl: String = when {
      value["typeUrl"] is String -> value["typeUrl"] as String
      value["type_url"] is String -> value["type_url"] as String
      value["type"] is String -> FieldResolver.toTypeUrl(value["type"] as String)
      else -> throw CanonicalCborException(
        "canonical-cbor: Any value missing typeUrl/type"
      )
    }
    val messageName = FieldResolver.fromTypeUrl(typeUrl)
    val wasUnwrapped = value["typeUrl"] == null &&
      value["type_url"] == null &&
      value["type"] is String
    val inner = if (wasUnwrapped) value["value"] else value
    return AnyDescriptor(typeUrl, messageName, inner, wasUnwrapped)
  }

  /**
   * Encode a google.protobuf.Any value. Key 0 is the typeUrl string; keys 1+
   * are the expanded inner-message fields. For opaque payload typeUrls
   * (json / vc / fg:x:address) the payload under key 1 is passed through
   * without schema-driven encoding (only Date->ISO and undefined-prop
   * stripping via [normalizeOpaquePayload]).
   */
  internal fun encodeAnyValue(value: Map<String, Any?>): CBORObject {
    val desc = resolveAnyTypeUrl(value)
    val map = CBORObject.NewMap()
    map[CBORObject.FromObject(0)] = CBORObject.FromObject(desc.typeUrl)

    if (OPAQUE_TYPE_URLS.contains(desc.typeUrl)) {
      val payload = if (desc.wasUnwrapped) desc.inner else (value["value"] ?: desc.inner)
      map[CBORObject.FromObject(1)] = opaqueToCbor(normalizeOpaquePayload(payload))
      return map
    }

    // Known message type. Strip wrapper keys (typeUrl / type_url, and
    // conditionally `type` when it is NOT a declared inner-schema field).
    @Suppress("UNCHECKED_CAST")
    val rawInner = desc.inner as? Map<String, Any?>
    val innerObj: Map<String, Any?> = if (rawInner == null) {
      emptyMap()
    } else if (desc.wasUnwrapped) {
      rawInner
    } else {
      val cleaned = LinkedHashMap(rawInner)
      cleaned.remove("typeUrl")
      cleaned.remove("type_url")
      if (cleaned["type"] is String) {
        val innerFields = FieldResolver.getFields(desc.messageName)
        if (innerFields == null || !innerFields.containsKey("type")) {
          cleaned.remove("type")
        }
      }
      cleaned
    }

    try {
      val encoded = encodeMessageFields(desc.messageName, innerObj)
      for ((k, v) in encoded.entries) {
        if (k.AsInt32() == 0) continue // never overwrite our typeUrl slot
        map[k] = v
      }
    } catch (err: CanonicalCborException) {
      if (err.message?.contains("unknown message type") == true) {
        // Unknown inner type — fall back to passthrough of raw bytes under key 1
        if (value.containsKey("value")) {
          map[CBORObject.FromObject(1)] = opaqueToCbor(value["value"])
          return map
        }
      }
      throw err
    }
    return map
  }

  /**
   * Normalize an opaque payload before handing to CBOR. Converts Date/Instant
   * to ISO-8601 strings, strips `null` / `undefined`-equivalent map values
   * (matches JSON.stringify semantics and protobuf3 default-fold so clients
   * and chain produce identical bytes).
   *
   * Mirrors canonical-cbor.ts line 215-229.
   */
  internal fun normalizeOpaquePayload(value: Any?): Any? {
    return when (value) {
      null -> null
      is java.util.Date -> java.time.Instant.ofEpochMilli(value.time).toString()
      is java.time.Instant -> value.toString()
      is java.time.OffsetDateTime -> value.toInstant().toString()
      is ByteArray -> value
      is List<*> -> value.map { normalizeOpaquePayload(it) }
      is Map<*, *> -> {
        val out = LinkedHashMap<Any?, Any?>()
        for ((k, v) in value) {
          if (v == null) continue // drop null-valued keys (JSON.stringify semantics)
          out[k] = normalizeOpaquePayload(v)
        }
        out
      }
      else -> value
    }
  }

  /**
   * Convert an arbitrary JSON-like object tree into a CBORObject. Used only
   * for opaque Any payloads where the schema is unknown.
   */
  internal fun opaqueToCbor(value: Any?): CBORObject {
    return when (value) {
      null -> CBORObject.Null
      is CBORObject -> value
      is Boolean -> CBORObject.FromObject(value)
      is Int -> CBORObject.FromObject(value.toLong())
      is Long -> CBORObject.FromObject(value)
      is Short -> CBORObject.FromObject(value.toLong())
      is Byte -> CBORObject.FromObject(value.toLong())
      is Float -> CBORObject.FromObject(value.toDouble())
      is Double -> CBORObject.FromObject(value)
      is BigInteger -> CBORObject.FromObject(value)
      is String -> CBORObject.FromObject(value)
      is ByteArray -> CBORObject.FromObject(value)
      is List<*> -> {
        val arr = CBORObject.NewArray()
        for (item in value) arr.Add(opaqueToCbor(item))
        arr
      }
      is Map<*, *> -> {
        val map = CBORObject.NewMap()
        for ((k, v) in value) {
          val key = when (k) {
            is Int -> CBORObject.FromObject(k.toLong())
            is Long -> CBORObject.FromObject(k)
            else -> CBORObject.FromObject(k.toString())
          }
          map[key] = opaqueToCbor(v)
        }
        map
      }
      else -> throw CanonicalCborException(
        "canonical-cbor: cannot encode opaque value of type ${value::class.java.simpleName}"
      )
    }
  }
}
