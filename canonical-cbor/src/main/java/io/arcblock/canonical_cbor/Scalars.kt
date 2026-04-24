package io.arcblock.canonical_cbor

import com.upokecenter.cbor.CBORObject
import java.math.BigInteger

/**
 * Scalar encoding + default-value folding helpers.
 *
 * Mirrors canonical-cbor.ts:
 *  - `encodeScalarInteger` (line 121-135)
 *  - `isDefaultScalar`     (line 137-155)
 *  - `encodeTimestampValue` (line 157-175)
 */
internal object Scalars {

  private val SCALAR_INT_TYPES: Set<String> = setOf(
    "int32", "sint32", "uint32", "sfixed32", "fixed32",
    "int64", "sint64", "uint64", "sfixed64", "fixed64"
  )

  private val SCALAR_FLOAT_TYPES: Set<String> = setOf("double", "float")

  internal fun isScalarInt(type: String): Boolean = SCALAR_INT_TYPES.contains(type)
  internal fun isScalarFloat(type: String): Boolean = SCALAR_FLOAT_TYPES.contains(type)

  /**
   * Protobuf3 default-fold: return true when [value] should be omitted from
   * the canonical output for a field of the given proto [type].
   *
   * Mirrors canonical-cbor.ts line 137-155. `isEnumType` membership check
   * lives in [FieldResolver]; we delegate there.
   */
  internal fun isDefaultScalar(value: Any?, type: String): Boolean {
    if (value == null) return true
    if (isScalarInt(type)) {
      return when (value) {
        is Number -> value.toLong() == 0L && (value !is Double || !value.isInfinite())
        is String -> value.isEmpty() || value == "0"
        is BigInteger -> value.signum() == 0
        else -> false
      }
    }
    if (isScalarFloat(type)) return value is Number && value.toDouble() == 0.0
    if (type == "bool") return value == false
    if (type == "string") return value == ""
    if (type == "bytes") return when (value) {
      is String -> value.isEmpty()
      is ByteArray -> value.isEmpty()
      is List<*> -> value.isEmpty()
      else -> false
    }
    if (FieldResolver.isEnumType(type)) {
      return value == 0 || value == "" || value == null
    }
    return false
  }

  /**
   * Encode a scalar integer value. Accepts number / bigint / string per the
   * TypeScript reference (line 121-135); strings are BigInt-parsed so
   * >2^53-1 values survive the encode path.
   *
   * @throws CanonicalCborException on non-integer inputs
   */
  internal fun encodeInteger(value: Any?): CBORObject {
    return when (value) {
      is Long -> CBORObject.FromObject(value)
      is Int -> CBORObject.FromObject(value.toLong())
      is Short -> CBORObject.FromObject(value.toLong())
      is Byte -> CBORObject.FromObject(value.toLong())
      is BigInteger -> CBORObject.FromObject(value)
      is Double -> {
        if (value != Math.floor(value) || value.isInfinite() || value.isNaN()) {
          throw CanonicalCborException(
            "canonical-cbor: non-integer value for integer field"
          )
        }
        CBORObject.FromObject(value.toLong())
      }
      is Float -> encodeInteger(value.toDouble())
      is String -> {
        if (value.isEmpty()) return CBORObject.FromObject(0L)
        try {
          CBORObject.FromObject(BigInteger(value))
        } catch (err: NumberFormatException) {
          throw CanonicalCborException(
            "canonical-cbor: cannot parse integer string",
            err
          )
        }
      }
      null -> CBORObject.FromObject(0L)
      else -> throw CanonicalCborException(
        "canonical-cbor: cannot encode integer value of type ${value::class.java.simpleName}"
      )
    }
  }

  /**
   * Encode a Timestamp field as an ISO-8601 string (milliseconds precision).
   * Accepts:
   *  - empty/null -> empty string
   *  - ISO-8601 string -> canonicalized via Date round-trip
   *  - `{seconds, nanos?}` wrapper
   *
   * @throws CanonicalCborException on unparseable strings or unknown shapes
   */
  internal fun encodeTimestamp(value: Any?): String {
    if (value == null) return ""
    if (value is String) {
      if (value.isEmpty()) return ""
      val millis = try {
        java.time.OffsetDateTime.parse(value).toInstant().toEpochMilli()
      } catch (err: java.time.format.DateTimeParseException) {
        throw CanonicalCborException(
          "canonical-cbor: invalid timestamp string (unparseable)",
          err
        )
      }
      // Truncate sub-ms precision by round-tripping through Instant.ofEpochMilli
      return java.time.Instant.ofEpochMilli(millis).toString()
    }
    if (value is Map<*, *>) {
      val seconds = when (val s = value["seconds"]) {
        is Number -> s.toLong()
        is String -> s.toLongOrNull() ?: 0L
        null -> 0L
        else -> throw CanonicalCborException("canonical-cbor: invalid timestamp.seconds")
      }
      val nanos = when (val n = value["nanos"]) {
        is Number -> n.toLong()
        null -> 0L
        else -> throw CanonicalCborException("canonical-cbor: invalid timestamp.nanos")
      }
      // Math.floorDiv-truncation so 1ns input does not inflate to 1ms
      val millis = seconds * 1000L + (nanos / 1_000_000L)
      return java.time.Instant.ofEpochMilli(millis).toString()
    }
    throw CanonicalCborException(
      "canonical-cbor: unsupported timestamp input type ${value::class.java.simpleName}"
    )
  }
}
