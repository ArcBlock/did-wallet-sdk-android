package io.arcblock.canonical_cbor

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the module scaffold wires up correctly — constants exported,
 * unimplemented methods throw NotImplementedError as expected. Real
 * behavior tests land alongside each implementation phase.
 */
class SmokeTest {

  @Test
  fun `self-describe prefix is the RFC 8949 tag 55799 bytes`() {
    assertArrayEquals(
      byteArrayOf(0xd9.toByte(), 0xd9.toByte(), 0xf7.toByte()),
      CanonicalCbor.SELF_DESCRIBE_PREFIX
    )
  }

  @Test
  fun `tag constants match RFC 8949`() {
    assertEquals(2, CanonicalCbor.TAG_POSITIVE_BIGNUM)
    assertEquals(3, CanonicalCbor.TAG_NEGATIVE_BIGNUM)
    assertEquals(55799, CanonicalCbor.TAG_SELF_DESCRIBE)
  }

  @Test
  fun `canonicalBytes of empty Transaction returns 4-byte tagged empty map`() {
    val out = CanonicalCbor.canonicalBytes("Transaction", emptyMap())
    // d9 d9 f7 a0 = tag 55799 + empty map
    assertArrayEquals(
      byteArrayOf(0xd9.toByte(), 0xd9.toByte(), 0xf7.toByte(), 0xa0.toByte()),
      out
    )
  }

  @Test
  fun `parseCanonical rejects input without tag 55799 prefix`() {
    val err = assertThrows(CanonicalCborException::class.java) {
      CanonicalCbor.parseCanonical("Transaction", byteArrayOf(0xa0.toByte()))
    }
    assertTrue(err.message!!.contains("self-describe tag 55799"))
  }

  @Test
  fun `parseCanonical of empty tagged map returns empty fields`() {
    val bytes = byteArrayOf(0xd9.toByte(), 0xd9.toByte(), 0xf7.toByte(), 0xa0.toByte())
    val decoded = CanonicalCbor.parseCanonical("Transaction", bytes)
    assertTrue(decoded.isEmpty())
  }
}
