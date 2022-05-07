package io.arcblock.walletkit.utils

import org.junit.Assert
import org.junit.Test
import java.math.BigDecimal

class NumberUtilsTest {

  @Test
  fun testTokenNumberDisplay() {
    /**
     * token 展示测试用例
     * 规则:
     * 1. 保留 Min(decimals, 6) 位小数，直接截断，不做四舍五入
     * 2. 末位的 0 做截取
     */
    Assert.assertEquals("0", ArcNumberUtils.formatNumberForTokenDisplay("abcd", 18))

    Assert.assertEquals("1.123456", ArcNumberUtils.formatNumberForTokenDisplay("1123456789000000000", 18))
    Assert.assertEquals("1.1234", ArcNumberUtils.formatNumberForTokenDisplay("1123400000000000000", 18))
    Assert.assertEquals("1", ArcNumberUtils.formatNumberForTokenDisplay("1000000000000000000", 18))
    Assert.assertEquals("1,000", ArcNumberUtils.formatNumberForTokenDisplay("1000000000000000000000", 18))
    Assert.assertEquals("1.23456", ArcNumberUtils.formatNumberForTokenDisplay("123456", 5))
    Assert.assertEquals("1,000.23456", ArcNumberUtils.formatNumberForTokenDisplay("100023456", 5))
    Assert.assertEquals("1,000.2345", ArcNumberUtils.formatNumberForTokenDisplay("100023450", 5))
    Assert.assertEquals("0", ArcNumberUtils.formatNumberForTokenDisplay("", 5))
  }

  @Test
  fun testFiatNumberDisplay() {
    /**
     * 法币数字展示测试用例
     * 规则:
     * 1. 保留 2 位小数，直接截断，不做四舍五入
     * 2. 末位的 0 保留
     */
    Assert.assertEquals("0.00", ArcNumberUtils.formatNumberForFiatDisplay("acb"))

    Assert.assertEquals("12.00", ArcNumberUtils.formatNumberForFiatDisplay("12"))
    Assert.assertEquals("12.12", ArcNumberUtils.formatNumberForFiatDisplay("12.12345"))
    Assert.assertEquals("12.99", ArcNumberUtils.formatNumberForFiatDisplay("12.99999"))
    Assert.assertEquals("0.00", ArcNumberUtils.formatNumberForFiatDisplay("0"))
    Assert.assertEquals("0.09", ArcNumberUtils.formatNumberForFiatDisplay("0.0999"))
    Assert.assertEquals("0.00", ArcNumberUtils.formatNumberForFiatDisplay(""))
  }


  @Test
  fun testString2Num(){
    Assert.assertEquals(
      "1.2345678901".toBigDecimal(),
      ArcNumberUtils.string2BigDecimalWithOutScale("12345678901", 10))

    Assert.assertEquals(
      "1.2345678901".toBigDecimal(),
      ArcNumberUtils.string2BigDecimalWithOutScale("12345678901".toBigInteger(), 10))

    Assert.assertEquals(123, ArcNumberUtils.string2Int("123"))
    Assert.assertEquals("123".toBigDecimal(), ArcNumberUtils.stringToBigDecimal("123"))
    Assert.assertEquals("1.123456", ArcNumberUtils.formatNumberForSend("1123456789", 9))

    Assert.assertEquals("1.123456", ArcNumberUtils.formatNumberForSend("1123456789".toBigDecimal(), 9))
    Assert.assertEquals("1,123.456789", ArcNumberUtils.formatNumberForTokenDisplay("1123456789001", 9))
    Assert.assertEquals("1.12", ArcNumberUtils.formatNumberForTokenDisplay("1120000000".toBigInteger(), 9))
    Assert.assertEquals("0", ArcNumberUtils.formatNumberForTokenDisplay("0".toBigInteger(), 9))


    Assert.assertTrue(ArcNumberUtils.checkCanParseStringToBigDecimal("1.11"))
    Assert.assertTrue(!ArcNumberUtils.checkCanParseStringToBigDecimal("1.11.11"))


    Assert.assertTrue(ArcNumberUtils.checkCanParseStringToInt("111"))
    Assert.assertTrue(!ArcNumberUtils.checkCanParseStringToInt("11.1"))

    Assert.assertTrue(ArcNumberUtils.stringToBigDecimal(null) == BigDecimal.ZERO)
    Assert.assertTrue(ArcNumberUtils.stringToBigDecimal("abc") == BigDecimal.ZERO)
    println(ArcNumberUtils.unsigned("-10000".toBigInteger()).toString())
  }

}
