package io.arcblock.canonical_cbor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class ScalarsTest {

  // ---- isDefaultScalar -------------------------------------------------

  @Test
  fun `integer zero across proto int types is default`() {
    for (t in listOf("int32", "uint32", "sint32", "int64", "uint64", "sint64")) {
      assertTrue("$t with 0 should be default", Scalars.isDefaultScalar(0, t))
      assertTrue("$t with 0L should be default", Scalars.isDefaultScalar(0L, t))
      assertTrue("$t with \"0\"", Scalars.isDefaultScalar("0", t))
      assertTrue("$t with \"\"", Scalars.isDefaultScalar("", t))
    }
  }

  @Test
  fun `non-zero integer is not default`() {
    assertFalse(Scalars.isDefaultScalar(1, "int32"))
    assertFalse(Scalars.isDefaultScalar(1L, "uint64"))
    assertFalse(Scalars.isDefaultScalar("1717171717171", "uint64"))
  }

  @Test
  fun `empty string is default for string type`() {
    assertTrue(Scalars.isDefaultScalar("", "string"))
    assertFalse(Scalars.isDefaultScalar("x", "string"))
  }

  @Test
  fun `empty bytes via any repr is default`() {
    assertTrue(Scalars.isDefaultScalar(ByteArray(0), "bytes"))
    assertTrue(Scalars.isDefaultScalar("", "bytes"))
    assertTrue(Scalars.isDefaultScalar(emptyList<Byte>(), "bytes"))
    assertFalse(Scalars.isDefaultScalar(byteArrayOf(1, 2, 3), "bytes"))
  }

  @Test
  fun `bool default is false`() {
    assertTrue(Scalars.isDefaultScalar(false, "bool"))
    assertFalse(Scalars.isDefaultScalar(true, "bool"))
  }

  @Test
  fun `null is default for any type`() {
    assertTrue(Scalars.isDefaultScalar(null, "string"))
    assertTrue(Scalars.isDefaultScalar(null, "int32"))
    assertTrue(Scalars.isDefaultScalar(null, "bytes"))
  }

  @Test
  fun `enum zero and empty string are default`() {
    // KeyType is an enum in the OCAP schema
    assertTrue(Scalars.isDefaultScalar(0, "KeyType"))
    assertTrue(Scalars.isDefaultScalar("", "KeyType"))
  }

  // ---- encodeInteger ---------------------------------------------------

  @Test
  fun `encodeInteger handles Long`() {
    val cbor = Scalars.encodeInteger(1717171717171L)
    assertEquals(1717171717171L, cbor.AsInt64Value())
  }

  @Test
  fun `encodeInteger handles Int`() {
    val cbor = Scalars.encodeInteger(42)
    assertEquals(42L, cbor.AsInt64Value())
  }

  @Test
  fun `encodeInteger handles BigInteger greater than 2^63`() {
    val big = BigInteger.ONE.shiftLeft(70)  // 2^70
    val cbor = Scalars.encodeInteger(big)
    // upokecenter automatically promotes BigInteger > 2^63 - 1 to a bignum
    // (CBOR tag 2). Verify the tag + byte contents rather than depending on
    // AsEIntegerValue which requires an untagged integer type.
    assertTrue("expected bignum tag 2", cbor.isTagged)
    assertEquals(2, cbor.mostInnerTag.ToInt32Checked())
    val bytes = cbor.Untag().GetByteString()
    assertEquals(big, BigInteger(1, bytes))
  }

  @Test
  fun `encodeInteger handles decimal string`() {
    val cbor = Scalars.encodeInteger("12345")
    assertEquals(12345L, cbor.AsInt64Value())
  }

  @Test
  fun `encodeInteger empty string maps to zero`() {
    val cbor = Scalars.encodeInteger("")
    assertEquals(0L, cbor.AsInt64Value())
  }

  @Test
  fun `encodeInteger rejects non-integer double`() {
    assertThrows(CanonicalCborException::class.java) {
      Scalars.encodeInteger(3.14)
    }
  }

  @Test
  fun `encodeInteger rejects non-numeric type`() {
    assertThrows(CanonicalCborException::class.java) {
      Scalars.encodeInteger(byteArrayOf(1, 2, 3))
    }
  }

  // ---- encodeTimestamp -------------------------------------------------

  @Test
  fun `timestamp empty input produces empty string`() {
    assertEquals("", Scalars.encodeTimestamp(null))
    assertEquals("", Scalars.encodeTimestamp(""))
  }

  @Test
  fun `timestamp ISO string round-trips`() {
    // 2026-01-01T00:00:00.000Z
    val out = Scalars.encodeTimestamp("2026-01-01T00:00:00Z")
    assertEquals("2026-01-01T00:00:00Z", out)
  }

  @Test
  fun `timestamp seconds+nanos truncates below millisecond`() {
    // 1 nanosecond should not inflate to 1 millisecond
    val out = Scalars.encodeTimestamp(mapOf("seconds" to 0L, "nanos" to 1))
    assertEquals("1970-01-01T00:00:00Z", out)
  }

  @Test
  fun `timestamp seconds+nanos produces correct ISO string`() {
    // 1,000,000 ns = 1 ms
    val out = Scalars.encodeTimestamp(mapOf("seconds" to 1_700_000_000L, "nanos" to 1_000_000))
    assertEquals("2023-11-14T22:13:20.001Z", out)
  }

  @Test
  fun `timestamp invalid string does not echo input`() {
    val err = assertThrows(CanonicalCborException::class.java) {
      Scalars.encodeTimestamp("definitely not a date")
    }
    // Do not leak user input — see canonical-cbor.ts line 162-164
    assertFalse(
      "error message should not contain raw user input",
      err.message!!.contains("definitely not")
    )
  }
}
