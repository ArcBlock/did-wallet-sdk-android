package io.arcblock.canonical_cbor

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
  fun `canonicalBytes is a scaffold (throws until Phase 1_3+)`() {
    assertThrows(NotImplementedError::class.java) {
      CanonicalCbor.canonicalBytes("Transaction", emptyMap())
    }
  }

  @Test
  fun `parseCanonical is a scaffold (throws until Phase 1_6)`() {
    assertThrows(NotImplementedError::class.java) {
      CanonicalCbor.parseCanonical("Transaction", byteArrayOf())
    }
  }
}
