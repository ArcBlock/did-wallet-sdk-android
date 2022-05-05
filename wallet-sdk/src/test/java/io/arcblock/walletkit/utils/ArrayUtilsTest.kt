package io.arcblock.walletkit.utils

import org.junit.Assert
import org.junit.Test

class ArrayUtilsTest {

  @Test fun testArrayCopy() {
    val byteArray = byteArrayOf(0x1, 0x2, 0x3, 0x4)
    val intArray = intArrayOf(1, 2, 3, 4)
    val longArray = longArrayOf(1L, 2L, 3L, 4L)
    val destBytes = ByteArray(4)
    val destInt = IntArray(4)
    val destLong = LongArray(4)
    ArrayUtils.arrayCopy(intArray, 0, destInt, 0, 0)
    Assert.assertNotEquals(destInt[1], 2)

    ArrayUtils.arrayCopy(intArray, 0, destInt, 0, 4)
    Assert.assertArrayEquals(destInt, intArray)

    ArrayUtils.arrayCopy(longArray, 0, destLong, 0, 0)
    Assert.assertNotEquals(destLong[1], 2L)

    ArrayUtils.arrayCopy(longArray, 0, destLong, 0, 4)
    Assert.assertArrayEquals(destLong, longArray)

    ArrayUtils.arrayCopy(byteArray, 0, destBytes, 0, 0)
    Assert.assertNotEquals(destBytes[2], 0x3)

    ArrayUtils.arrayCopy(byteArray, 0, destBytes, 0, 4)
    Assert.assertArrayEquals(destBytes, byteArray)

  }

  @Test fun testFill() {
    val destBytes = ByteArray(4)
    val destInt = IntArray(4)
    val destLong = LongArray(4)
    ArrayUtils.fill(destInt, 4)
    ArrayUtils.fill(destBytes, 0x11.toByte())
    ArrayUtils.fill(destLong, 10L)

    Assert.assertEquals(destInt[3], 4)
    Assert.assertEquals(destBytes[3], 0x11.toByte())

    Assert.assertEquals(destLong[3], 10L)
  }
}