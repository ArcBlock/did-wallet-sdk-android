package io.arcblock.canonical_cbor

import com.upokecenter.cbor.CBORObject
import java.math.BigInteger

/**
 * Canonical encoding for OCAP `BigUint` / `BigSint` wrapper messages.
 *
 * Mirrors canonical-cbor.ts `normalizeBigIntWrapper` (line 82-119) +
 * `stripBignumLeadingZeros` (line 67-72) + the BIGINT_WRAPPER_TYPES branch
 * of `encodeFieldValue` (line 310-315).
 *
 * Rules:
 *  - Zero magnitude -> omit entirely (return [BigIntRepr.OMIT]).
 *  - Non-zero positive -> CBOR tag 2 + magnitude bytes (big-endian, no
 *    leading zeros).
 *  - Non-zero negative BigSint -> CBOR tag 3 + magnitude bytes.
 *
 * Accepted input shapes:
 *  - [BigInteger] (native)
 *  - [Long] / [Int]
 *  - [String] — decimal; empty string is treated as zero
 *  - [Map] — the OCAP wrapper `{value: ByteArray | hex-String, minus?: Boolean}`
 *  - `{value: *}` wrapper with any of the above as the `value` entry
 *
 * Zero-folding applies to every shape. A wrapper whose `value` is an all-zero
 * byte string still folds to [BigIntRepr.OMIT] — matches canonical-cbor.ts
 * line 95.
 */
internal object BigIntCodec {

  /** Opaque result — either a CBOR-ready magnitude+sign or "omit this field". */
  internal sealed class BigIntRepr {
    /** Encoded as a bignum tagged value (tag 2 or 3). */
    data class Tagged(val bytes: ByteArray, val negative: Boolean) : BigIntRepr() {
      override fun equals(other: Any?): Boolean = other is Tagged &&
        negative == other.negative && bytes.contentEquals(other.bytes)
      override fun hashCode(): Int = bytes.contentHashCode() * 31 + if (negative) 1 else 0
    }
    /** Value is zero -> caller must omit the whole field. */
    object Omit : BigIntRepr()
  }

  /** BigUint vs BigSint affects whether a negative result is legal. */
  internal enum class Kind { BIG_UINT, BIG_SINT }

  /**
   * Normalize an OCAP BigUint/BigSint input to a canonical representation.
   * Returns [BigIntRepr.Omit] for zero, null, missing, or empty-byte inputs.
   *
   * @throws CanonicalCborException on unsupported input types (not BigInteger,
   *         Number, String, or wrapper Map)
   */
  internal fun normalize(value: Any?, kind: Kind): BigIntRepr {
    if (value == null) return BigIntRepr.Omit

    // Wrapper shape {value: ByteArray | hex-String, minus?: Boolean}
    if (value is Map<*, *>) {
      val raw = value["value"] ?: return BigIntRepr.Omit
      val minus = value["minus"] == true
      // Reject negative values into BigUint at the boundary instead of
      // silently re-coercing them to positive (which would corrupt the
      // user's data without an error). Mirrors the TS `normalizeBigIntWrapper`
      // strictness for unsigned wrappers.
      if (kind == Kind.BIG_UINT && minus) {
        throw CanonicalCborException(
          "canonical-cbor: BigUint cannot carry minus=true"
        )
      }
      val bytes: ByteArray = toMagnitudeBytes(raw) ?: return BigIntRepr.Omit
      val trimmed = stripLeadingZeros(bytes)
      if (trimmed.size == 1 && trimmed[0] == 0.toByte()) return BigIntRepr.Omit
      val negative = kind == Kind.BIG_SINT && minus
      return BigIntRepr.Tagged(trimmed, negative)
    }

    // BigInteger (native)
    if (value is BigInteger) {
      if (value.signum() == 0) return BigIntRepr.Omit
      if (kind == Kind.BIG_UINT && value.signum() < 0) {
        throw CanonicalCborException(
          "canonical-cbor: BigUint cannot encode negative BigInteger"
        )
      }
      val negative = value.signum() < 0 && kind == Kind.BIG_SINT
      val magnitude = if (value.signum() < 0) value.negate() else value
      return BigIntRepr.Tagged(stripLeadingZeros(magnitude.toByteArray()), negative)
    }

    // Numeric scalars
    if (value is Long || value is Int || value is Short || value is Byte) {
      val n = (value as Number).toLong()
      if (n == 0L) return BigIntRepr.Omit
      if (kind == Kind.BIG_UINT && n < 0L) {
        throw CanonicalCborException(
          "canonical-cbor: BigUint cannot encode negative scalar"
        )
      }
      val magnitude = if (n < 0L) BigInteger.valueOf(n).negate() else BigInteger.valueOf(n)
      val negative = n < 0L && kind == Kind.BIG_SINT
      return BigIntRepr.Tagged(stripLeadingZeros(magnitude.toByteArray()), negative)
    }

    // Decimal string
    if (value is String) {
      if (value.isEmpty() || value == "0") return BigIntRepr.Omit
      val parsed = try {
        BigInteger(value)
      } catch (err: NumberFormatException) {
        throw CanonicalCborException(
          "canonical-cbor: invalid decimal string for ${kind.label()}",
          err
        )
      }
      return normalize(parsed, kind)
    }

    throw CanonicalCborException(
      "canonical-cbor: cannot encode ${kind.label()} from ${value::class.java.simpleName}"
    )
  }

  /** Build a tagged CBOR object from [BigIntRepr.Tagged]. Callers that want
   *  to omit entirely should branch on `repr is BigIntRepr.Omit` upstream. */
  internal fun toCbor(repr: BigIntRepr.Tagged): CBORObject {
    val tag = if (repr.negative) CanonicalCbor.TAG_NEGATIVE_BIGNUM else CanonicalCbor.TAG_POSITIVE_BIGNUM
    return CBORObject.FromObjectAndTag(repr.bytes, tag)
  }

  /** Convenience for the common path: normalize + build CBOR, or return null
   *  for the omit case. */
  internal fun encode(value: Any?, kind: Kind): CBORObject? {
    val repr = normalize(value, kind)
    return if (repr is BigIntRepr.Tagged) toCbor(repr) else null
  }

  /**
   * Decode a CBOR bignum-tagged object back to the OCAP wrapper shape.
   * Returns `{value: ByteArray}` for BigUint, `{value, minus}` for BigSint.
   * Non-tagged input passes through unchanged (matches canonical-cbor.ts
   * line 502 fall-through).
   */
  internal fun decode(raw: Any?, kind: Kind): Any? {
    if (raw !is CBORObject || !raw.isTagged) return raw
    val tagE = raw.mostInnerTag
    val tag = tagE.ToInt32Checked()
    if (tag != CanonicalCbor.TAG_POSITIVE_BIGNUM && tag != CanonicalCbor.TAG_NEGATIVE_BIGNUM) {
      throw CanonicalCborException(
        "canonical-cbor: ${kind.label()} wrapper expects tag 2/3, got $tag"
      )
    }
    val bytes = raw.Untag().GetByteString()
    return when (kind) {
      Kind.BIG_UINT -> mapOf("value" to bytes)
      Kind.BIG_SINT -> mapOf(
        "value" to bytes,
        "minus" to (tag == CanonicalCbor.TAG_NEGATIVE_BIGNUM)
      )
    }
  }

  // ---------------------------------------------------------------------

  /** Big-endian magnitude bytes with leading zero bytes removed.
   *  Always returns at least one byte; an all-zero input becomes `[0]`. */
  internal fun stripLeadingZeros(bytes: ByteArray): ByteArray {
    if (bytes.isEmpty()) return byteArrayOf(0)
    var start = 0
    while (start < bytes.size - 1 && bytes[start] == 0.toByte()) start++
    return if (start == 0) bytes else bytes.copyOfRange(start, bytes.size)
  }

  private fun toMagnitudeBytes(raw: Any?): ByteArray? {
    return when (raw) {
      null -> null
      is ByteArray -> if (raw.isEmpty()) null else raw
      is List<*> -> {
        val ba = ByteArray(raw.size)
        for ((i, v) in raw.withIndex()) {
          ba[i] = when (v) {
            is Number -> v.toInt().toByte()
            else -> throw CanonicalCborException(
              "canonical-cbor: BigInt bytes list contains non-number"
            )
          }
        }
        if (ba.isEmpty()) null else ba
      }
      is String -> if (raw.isEmpty()) null else hexToBytes(raw)
      else -> throw CanonicalCborException(
        "canonical-cbor: unsupported BigInt wrapper value type " +
          raw::class.java.simpleName
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
        throw CanonicalCborException("canonical-cbor: invalid hex in BigInt bytes")
      }
      out[i] = ((hi shl 4) or lo).toByte()
    }
    return out
  }

  private fun Kind.label(): String = when (this) {
    Kind.BIG_UINT -> "BigUint"
    Kind.BIG_SINT -> "BigSint"
  }
}
