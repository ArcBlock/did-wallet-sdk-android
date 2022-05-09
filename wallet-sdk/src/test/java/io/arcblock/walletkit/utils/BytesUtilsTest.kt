package io.arcblock.walletkit.utils

import org.junit.Assert
import org.junit.Test

class BytesUtilsTest {

  @Test fun testB2iLittle() {

    val bytesArray = byteArrayOf(0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8)
    val outArray = IntArray(1)
    BytesUtils.b2iLittle(bytesArray, 0, outArray, 0, 0)
    Assert.assertNotEquals(outArray[0].shr(16).and(0xff), bytesArray[2].toInt())

    BytesUtils.b2iLittle(bytesArray, 0, outArray, 0, 4)
    val empty = ""
    val dot = ","
    println("in: ${bytesArray.foldRight(empty) { a, acc -> a.toString().plus(dot).plus(acc) }} out: ${outArray.foldRight("") { a, acc -> "$a,$acc" }}")
    Assert.assertEquals(outArray[0].shr(16).and(0xff), bytesArray[2].toInt())

    val outLong = LongArray(1)
    BytesUtils.b2iLittle(bytesArray, 0, outLong, 0, 0)
    Assert.assertNotEquals(outLong[0].shr(16).and(0xff), bytesArray[2].toLong())
    BytesUtils.b2iLittle(bytesArray, 0, outLong, 0, 8)
    Assert.assertEquals(outLong[0].shr(16).and(0xff), bytesArray[2].toLong())

  }

  @Test fun testB2iBig() {
    val bytesArray = byteArrayOf(0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8)
    val outArray = IntArray(1)
    BytesUtils.b2iBig(bytesArray, 0, outArray, 0, 0)
    Assert.assertNotEquals(outArray[0].shr(8).and(0xff), bytesArray[2].toInt())

    BytesUtils.b2iBig(bytesArray, 0, outArray, 0, 4)
    val empty = ""
    val dot = ","
    println("in: ${bytesArray.foldRight(empty) { a, acc -> a.toString().plus(dot).plus(acc) }} out: ${outArray.foldRight("") { a, acc -> "$a,$acc" }}")
    Assert.assertEquals(outArray[0].shr(8).and(0xff), bytesArray[2].toInt())

    val outLong = LongArray(1)
    BytesUtils.b2iBig(bytesArray, 0, outLong, 0, 0)
    Assert.assertNotEquals(outLong[0].shr(16).and(0xff), bytesArray[5].toLong())
    BytesUtils.b2iBig(bytesArray, 0, outLong, 0, 8)
    println("out Long: ${outLong[0]}")
    Assert.assertEquals(outLong[0].shr(16).and(0xff), bytesArray[5].toLong())

  }

  @Test fun i2bLittle() {
    val inputInt = intArrayOf(16909060)
    val outInt = ByteArray(8)
    BytesUtils.i2bLittle(inputInt, 0, outInt, 0, 0)
    Assert.assertNotEquals(inputInt[0].shr(16).and(0xff), outInt[2].toInt())
    BytesUtils.i2bLittle(inputInt, 0, outInt, 0, 4)
    Assert.assertEquals(inputInt[0].shr(16).and(0xff), outInt[2].toInt())

    val inputLong = longArrayOf(72623859790382856L)
    BytesUtils.i2bLittle(inputLong, 0, outInt, 0, 0)
    Assert.assertNotEquals(inputLong[0].shr(16).and(0xff), outInt[2].toLong())
    BytesUtils.i2bLittle(inputLong, 0, outInt, 0, 8)
    Assert.assertEquals(inputLong[0].shr(16).and(0xff), outInt[2].toLong())

  }

  @Test fun testI2bLittle() {}
  @Test fun i2bLittle4() {}
  @Test fun testB2iLittle1() {}
  @Test fun testB2iLittle2() {}
  @Test fun testB2iBig1() {}
  @Test fun testB2iBig2() {}
  @Test fun testI2bLittle1() {}
  @Test fun testI2bLittle2() {}
  @Test fun testI2bLittle4() {}
  @Test fun b2iLittle64() {}
  @Test fun i2bBig() {
    val inputInt = intArrayOf(16909060)
    val outInt = ByteArray(8)
    BytesUtils.i2bBig(inputInt, 0, outInt, 0, 0)
    Assert.assertNotEquals(inputInt[0].shr(16).and(0xff), outInt[1].toInt())
    BytesUtils.i2bBig(inputInt, 0, outInt, 0, 4)
    Assert.assertEquals(inputInt[0].shr(16).and(0xff), outInt[1].toInt())

    val inputLong = longArrayOf(72623859790382856L)
    BytesUtils.i2bBig(inputLong, 0, outInt, 0, 0)
    Assert.assertNotEquals(inputLong[0].shr(16).and(0xff), outInt[5].toLong())
    BytesUtils.i2bBig(inputLong, 0, outInt, 0, 8)
    Assert.assertEquals(inputLong[0].shr(16).and(0xff), outInt[5].toLong())

  }

  @Test fun testI2bBig() {}
  @Test fun i2bBig4() {}
  @Test fun b2iBig64() {}
  @Test fun b2iBig128() {}
}