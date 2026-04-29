package io.arcblock.canonical_cbor

import com.upokecenter.cbor.CBORObject
import io.arcblock.canonical_cbor.BigIntCodec.Kind
import io.arcblock.canonical_cbor.BigIntCodec.BigIntRepr
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigInteger

class BigIntCodecTest {

  // ---- normalize: zero-folding -----------------------------------------

  @Test
  fun `null folds to Omit`() {
    assertSame(BigIntRepr.Omit, BigIntCodec.normalize(null, Kind.BIG_UINT))
  }

  @Test
  fun `BigInteger zero folds to Omit`() {
    assertSame(BigIntRepr.Omit, BigIntCodec.normalize(BigInteger.ZERO, Kind.BIG_UINT))
    assertSame(BigIntRepr.Omit, BigIntCodec.normalize(BigInteger.ZERO, Kind.BIG_SINT))
  }

  @Test
  fun `Long zero folds to Omit`() {
    assertSame(BigIntRepr.Omit, BigIntCodec.normalize(0L, Kind.BIG_UINT))
    assertSame(BigIntRepr.Omit, BigIntCodec.normalize(0, Kind.BIG_UINT))
  }

  @Test
  fun `decimal string zero folds to Omit`() {
    assertSame(BigIntRepr.Omit, BigIntCodec.normalize("0", Kind.BIG_UINT))
    assertSame(BigIntRepr.Omit, BigIntCodec.normalize("", Kind.BIG_UINT))
  }

  @Test
  fun `wrapper with all-zero bytes folds to Omit`() {
    val repr = BigIntCodec.normalize(mapOf("value" to byteArrayOf(0, 0, 0)), Kind.BIG_UINT)
    assertSame(BigIntRepr.Omit, repr)
  }

  @Test
  fun `wrapper with missing or empty value folds to Omit`() {
    assertSame(BigIntRepr.Omit, BigIntCodec.normalize(mapOf<String, Any?>(), Kind.BIG_UINT))
    assertSame(
      BigIntRepr.Omit,
      BigIntCodec.normalize(mapOf("value" to ByteArray(0)), Kind.BIG_UINT)
    )
  }

  // ---- normalize: non-zero magnitudes ----------------------------------

  @Test
  fun `BigInteger 10^18 encodes to 8 bytes`() {
    val repr = BigIntCodec.normalize(BigInteger("1000000000000000000"), Kind.BIG_UINT)
    assertTrue(repr is BigIntRepr.Tagged)
    val tagged = repr as BigIntRepr.Tagged
    // 10^18 = 0x0de0b6b3a7640000
    assertArrayEquals(
      byteArrayOf(0x0d, 0xe0.toByte(), 0xb6.toByte(), 0xb3.toByte(),
                  0xa7.toByte(), 0x64, 0x00, 0x00),
      tagged.bytes
    )
    assertEquals(false, tagged.negative)
  }

  @Test
  fun `decimal string 10^18 matches BigInteger path`() {
    val a = BigIntCodec.normalize("1000000000000000000", Kind.BIG_UINT) as BigIntRepr.Tagged
    val b = BigIntCodec.normalize(BigInteger("1000000000000000000"), Kind.BIG_UINT) as BigIntRepr.Tagged
    assertArrayEquals(a.bytes, b.bytes)
  }

  @Test
  fun `wrapper with leading-zero bytes gets trimmed`() {
    val input = mapOf("value" to byteArrayOf(0, 0, 0x12, 0x34))
    val repr = BigIntCodec.normalize(input, Kind.BIG_UINT) as BigIntRepr.Tagged
    assertArrayEquals(byteArrayOf(0x12, 0x34), repr.bytes)
  }

  @Test
  fun `wrapper hex string value decodes correctly`() {
    val input = mapOf("value" to "0de0b6b3a7640000")
    val repr = BigIntCodec.normalize(input, Kind.BIG_UINT) as BigIntRepr.Tagged
    assertArrayEquals(
      byteArrayOf(0x0d, 0xe0.toByte(), 0xb6.toByte(), 0xb3.toByte(),
                  0xa7.toByte(), 0x64, 0x00, 0x00),
      repr.bytes
    )
  }

  // ---- normalize: sign handling ----------------------------------------

  @Test
  fun `BigSint negative BigInteger sets negative=true`() {
    val repr = BigIntCodec.normalize(BigInteger("-42"), Kind.BIG_SINT) as BigIntRepr.Tagged
    assertEquals(true, repr.negative)
    assertArrayEquals(byteArrayOf(0x2a), repr.bytes) // |−42| = 0x2a
  }

  @Test
  fun `BigSint with minus=true wrapper`() {
    val input = mapOf("value" to byteArrayOf(0x2a), "minus" to true)
    val repr = BigIntCodec.normalize(input, Kind.BIG_SINT) as BigIntRepr.Tagged
    assertEquals(true, repr.negative)
  }

  @Test
  fun `BigUint rejects negative BigInteger at the boundary`() {
    // Strict policy: silently coercing a negative input into a positive
    // BigUint (the previous behavior, mirrored from a too-permissive TS
    // helper) hides data corruption from callers. Encoding now throws —
    // see also `BigUint rejects negative scalar` below.
    assertThrows(CanonicalCborException::class.java) {
      BigIntCodec.normalize(BigInteger("-42"), Kind.BIG_UINT)
    }
  }

  // ---- toCbor: tagged output -------------------------------------------

  @Test
  fun `toCbor emits tag 2 for positive`() {
    val repr = BigIntRepr.Tagged(byteArrayOf(0x2a), negative = false)
    val cbor = BigIntCodec.toCbor(repr)
    assertEquals(CanonicalCbor.TAG_POSITIVE_BIGNUM, cbor.mostInnerTag.ToInt32Checked())
  }

  @Test
  fun `toCbor emits tag 3 for negative`() {
    val repr = BigIntRepr.Tagged(byteArrayOf(0x2a), negative = true)
    val cbor = BigIntCodec.toCbor(repr)
    assertEquals(CanonicalCbor.TAG_NEGATIVE_BIGNUM, cbor.mostInnerTag.ToInt32Checked())
  }

  @Test
  fun `encode returns null when value is zero`() {
    assertNull(BigIntCodec.encode(BigInteger.ZERO, Kind.BIG_UINT))
    assertNull(BigIntCodec.encode(0L, Kind.BIG_UINT))
    assertNull(BigIntCodec.encode(null, Kind.BIG_UINT))
  }

  @Test
  fun `encode wraps 10^18 into bytes with RFC 8949 tag 2 prefix c2 48`() {
    val cbor = BigIntCodec.encode(BigInteger("1000000000000000000"), Kind.BIG_UINT)!!
    val bytes = cbor.EncodeToBytes()
    // Expect: c2 (tag 2) 48 (bstr, 8 bytes) 0d e0 b6 b3 a7 64 00 00
    assertArrayEquals(
      byteArrayOf(
        0xc2.toByte(), 0x48,
        0x0d, 0xe0.toByte(), 0xb6.toByte(), 0xb3.toByte(),
        0xa7.toByte(), 0x64, 0x00, 0x00
      ),
      bytes
    )
  }

  // ---- invalid input handling ------------------------------------------

  @Test
  fun `invalid decimal string throws`() {
    val err = assertThrows(CanonicalCborException::class.java) {
      BigIntCodec.normalize("not a number", Kind.BIG_UINT)
    }
    assertTrue(err.message!!.contains("invalid decimal string"))
  }

  @Test
  fun `unsupported wrapper value type throws`() {
    assertThrows(CanonicalCborException::class.java) {
      BigIntCodec.normalize(mapOf("value" to 3.14), Kind.BIG_UINT)
    }
  }

  // ---- BigUint must reject negative input at the boundary --------------

  /**
   * Regression: a `{value, minus: true}` wrapper must not silently
   * encode as a positive BigUint — that would corrupt user data with
   * no error path. Same intent as the TS `normalizeBigIntWrapper`
   * strictness for unsigned wrappers.
   */
  @Test
  fun `BigUint wrapper rejects minus=true`() {
    assertThrows(CanonicalCborException::class.java) {
      BigIntCodec.normalize(
        mapOf("value" to byteArrayOf(0x01), "minus" to true),
        Kind.BIG_UINT
      )
    }
  }

  @Test
  fun `BigUint rejects negative BigInteger`() {
    assertThrows(CanonicalCborException::class.java) {
      BigIntCodec.normalize(java.math.BigInteger.valueOf(-1), Kind.BIG_UINT)
    }
  }

  @Test
  fun `BigUint rejects negative scalar`() {
    assertThrows(CanonicalCborException::class.java) {
      BigIntCodec.normalize(-42L, Kind.BIG_UINT)
    }
  }

  @Test
  fun `BigSint still accepts minus=true`() {
    val repr = BigIntCodec.normalize(
      mapOf("value" to byteArrayOf(0x01), "minus" to true),
      Kind.BIG_SINT
    )
    // Tagged with negative=true (i.e. CBOR tag 3 on emit).
    org.junit.Assert.assertTrue("BigSint minus=true should produce a Tagged.negative=true repr",
      repr is BigIntCodec.BigIntRepr.Tagged && repr.negative)
  }

  // ---- stripLeadingZeros -----------------------------------------------

  @Test
  fun `stripLeadingZeros removes leading zeros but preserves last byte`() {
    assertArrayEquals(
      byteArrayOf(0x12, 0x34),
      BigIntCodec.stripLeadingZeros(byteArrayOf(0, 0, 0x12, 0x34))
    )
    assertArrayEquals(
      byteArrayOf(0),
      BigIntCodec.stripLeadingZeros(byteArrayOf(0, 0, 0))
    )
    assertArrayEquals(
      byteArrayOf(0xff.toByte()),
      BigIntCodec.stripLeadingZeros(byteArrayOf(0xff.toByte()))
    )
  }

  // ---- decode round-trip -----------------------------------------------

  @Test
  fun `decode of tag 2 produces BigUint wrapper`() {
    val cbor = CBORObject.FromObjectAndTag(byteArrayOf(0x2a), 2)
    val decoded = BigIntCodec.decode(cbor, Kind.BIG_UINT) as Map<*, *>
    assertArrayEquals(byteArrayOf(0x2a), decoded["value"] as ByteArray)
    assertNull(decoded["minus"])
  }

  @Test
  fun `decode of tag 3 produces BigSint wrapper with minus=true`() {
    val cbor = CBORObject.FromObjectAndTag(byteArrayOf(0x2a), 3)
    val decoded = BigIntCodec.decode(cbor, Kind.BIG_SINT) as Map<*, *>
    assertEquals(true, decoded["minus"])
  }

  @Test
  fun `decode of untagged CBOR passes through unchanged`() {
    val cbor = CBORObject.FromObject(42L)
    assertSame(cbor, BigIntCodec.decode(cbor, Kind.BIG_UINT))
  }
}
