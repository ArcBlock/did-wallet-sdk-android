package io.arcblock.walletkit.utils

import org.junit.Assert
import org.junit.Test

class InputCheckUtilsTest {

  @Test
  fun testAmountCheck() {
    Assert.assertFalse(ArcInputCheckUtils.isAmountValid("", 18))
    Assert.assertFalse(ArcInputCheckUtils.isAmountValid(".", 18))
    Assert.assertTrue(ArcInputCheckUtils.isAmountValid("11.", 18))
    Assert.assertTrue(ArcInputCheckUtils.isAmountValid(".11", 18))
    Assert.assertTrue(ArcInputCheckUtils.isAmountValid("11.0", 18))
    Assert.assertTrue(ArcInputCheckUtils.isAmountValid("11", 18))
    Assert.assertTrue(ArcInputCheckUtils.isAmountValid("111.123456", 18))
    Assert.assertFalse(ArcInputCheckUtils.isAmountValid("111.1234567", 18))
    Assert.assertTrue(ArcInputCheckUtils.isAmountValid("12345.1234", 4))
    Assert.assertFalse(ArcInputCheckUtils.isAmountValid("12345.12345", 4))
  }

  @Test
  fun testGasPriceCheck() {
    Assert.assertFalse(ArcInputCheckUtils.isValidGasPrice(""))
    Assert.assertFalse(ArcInputCheckUtils.isValidGasPrice("1234.1234567891"))
    Assert.assertTrue(ArcInputCheckUtils.isValidGasPrice("111.123456789"))
  }

  @Test
  fun testGasLimitCheck() {
    Assert.assertFalse(ArcInputCheckUtils.isValidGasLimit(""))
    Assert.assertFalse(ArcInputCheckUtils.isValidGasLimit("0"))
    Assert.assertFalse(ArcInputCheckUtils.isValidGasLimit("25000.0"))
    Assert.assertFalse(ArcInputCheckUtils.isValidGasLimit("20000"))
    Assert.assertTrue(ArcInputCheckUtils.isValidGasLimit("80000"))
  }
}
