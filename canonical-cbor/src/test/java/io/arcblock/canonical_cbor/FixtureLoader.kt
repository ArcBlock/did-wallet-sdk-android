package io.arcblock.canonical_cbor

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger

/**
 * Load `<name>.input.json` / `<name>.cbor.bin` fixture pairs from classpath
 * resources at `/vectors/`. Tokens in the JSON:
 *
 *   {"$bytes":  "<hex>"}     -> ByteArray
 *   {"$bigint": "<decimal>"} -> BigInteger
 *
 * Returns the Transaction field map ready for
 * `CanonicalCbor.canonicalBytes("Transaction", map)`.
 */
internal object FixtureLoader {

  internal data class Fixture(
    val name: String,
    val type: String,
    val data: Map<String, Any?>,
    val cborBytes: ByteArray
  )

  internal fun load(name: String): Fixture {
    val inputStream = FixtureLoader::class.java.getResourceAsStream("/vectors/$name.input.json")
      ?: throw IllegalStateException("missing /vectors/$name.input.json")
    val inputText = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }

    val cborStream = FixtureLoader::class.java.getResourceAsStream("/vectors/$name.cbor.bin")
      ?: throw IllegalStateException("missing /vectors/$name.cbor.bin")
    val cborBytes = cborStream.use { it.readBytes() }

    // Two accepted root shapes:
    //   Blockchain baseline: {"type": "<MessageName>", "data": {...}}
    //   Wallet fixtures:    {<Transaction fields>...} with type="Transaction"
    val obj = JSONObject(inputText)
    return if (obj.has("type") && obj.has("data") && obj.length() == 2) {
      Fixture(
        name = name,
        type = obj.getString("type"),
        data = jsonObjectToMap(obj.getJSONObject("data")),
        cborBytes = cborBytes
      )
    } else {
      Fixture(
        name = name,
        type = "Transaction",
        data = jsonObjectToMap(obj),
        cborBytes = cborBytes
      )
    }
  }

  // ---------------------------------------------------------------------
  // JSON -> wallet-internal type conversion
  // ---------------------------------------------------------------------

  private fun jsonToAny(value: Any?): Any? {
    return when (value) {
      null, JSONObject.NULL -> null
      is JSONObject -> {
        // Token shortcuts
        if (value.length() == 1) {
          if (value.has("\$bytes")) return hexToBytes(value.getString("\$bytes"))
          if (value.has("\$bigint")) return BigInteger(value.getString("\$bigint"))
        }
        jsonObjectToMap(value)
      }
      is JSONArray -> jsonArrayToList(value)
      is Number, is String, is Boolean -> value
      else -> value
    }
  }

  private fun jsonObjectToMap(obj: JSONObject): Map<String, Any?> {
    val out = LinkedHashMap<String, Any?>(obj.length())
    for (k in obj.keys()) out[k] = jsonToAny(obj.opt(k))
    return out
  }

  private fun jsonArrayToList(arr: JSONArray): List<Any?> {
    val out = ArrayList<Any?>(arr.length())
    for (i in 0 until arr.length()) out.add(jsonToAny(arr.opt(i)))
    return out
  }

  private fun hexToBytes(hex: String): ByteArray {
    val s = if (hex.startsWith("0x") || hex.startsWith("0X")) hex.substring(2) else hex
    val clean = if (s.length % 2 == 1) "0$s" else s
    val out = ByteArray(clean.length / 2)
    for (i in out.indices) {
      val hi = Character.digit(clean[i * 2], 16)
      val lo = Character.digit(clean[i * 2 + 1], 16)
      require(hi >= 0 && lo >= 0) { "invalid hex" }
      out[i] = ((hi shl 4) or lo).toByte()
    }
    return out
  }
}
