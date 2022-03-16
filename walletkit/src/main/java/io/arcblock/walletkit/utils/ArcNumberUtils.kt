package io.arcblock.walletkit.utils

import io.arcblock.walletkit.utils.ArcInputCheckUtils.getMinScale
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode.DOWN
import java.text.DecimalFormat
import kotlin.math.pow

/**
 * Created by Nate on 2018/12/5
 */
object ArcNumberUtils {

  fun formatNumberForTokenDisplay(number: String?, decimals: Int): String {
    val numberDecimal = string2BigDecimalWithScale(number ?: "0", decimals)
    return formatNumberForTokenDisplay(numberDecimal)
  }

  fun formatNumberForTokenDisplay(number: BigInteger, decimals: Int): String {
    val numberDecimal = string2BigDecimalWithScale(number, decimals)
    return formatNumberForTokenDisplay(numberDecimal)
  }

  @JvmStatic
  fun formatNumberForTokenDisplay(number: BigDecimal?): String {
    if (number == null) {
      return "0"
    }
    val nf = DecimalFormat("#,##0.000000")
    nf.roundingMode = DOWN
    return removeTheEndZero(nf.format(number))
  }
  /**
   * ***********************************************************
   */

  /**
   * ********** 格式化 token number 为法币展示用的样式 ******************
   */
  fun formatNumberForFiatDisplay(number: String): String {
    val numberBigDecimal = try {
      BigDecimal(number)
    } catch (e: Exception) {
      BigDecimal.ZERO
    }
    return formatNumberForFiatDisplay(numberBigDecimal)
  }

  fun formatNumberForFiatDisplay(number: BigDecimal): String {
    val format = "#,##0.00"
    val nf = DecimalFormat(format)
    nf.roundingMode = DOWN
    return nf.format(number)
  }
  /**
   * ***********************************************************
   */

  /**
   * ********** 格式化 token number 为输入框用的格式 ******************
   */
  fun formatNumberForSend(number: String, decimals: Int): String {
    val numberDecimal = string2BigDecimalWithScale(number, decimals)
    return formatNumberForSend(numberDecimal)
  }

  fun formatNumberForSend(number: BigDecimal, decimals: Int): String {
    val numberDecimal = string2BigDecimalWithScale(number, decimals)
    return formatNumberForSend(numberDecimal)
  }

  private fun formatNumberForSend(number: BigDecimal): String {
    val format = "##0.000000"
    val nf = DecimalFormat(format)
    nf.roundingMode = DOWN
    return removeTheEndZero(nf.format(number))
  }
  /**
   * ***********************************************************
   */

  /**
   * ********** String 类型的数字转换为去除精度的 BigDecimal ******************
   */
  fun string2BigDecimalWithOutScale(number: String, decimals: Int): BigDecimal {
    return try {
      BigDecimal(number).divide(BigDecimal.valueOf(10.0.pow(decimals.toDouble())))
    } catch (e: Exception) {
      BigDecimal.ZERO
    }
  }

  /**
   * ********** BigInteger 类型的数字转换为去除精度的 BigDecimal ******************
   */
  fun string2BigDecimalWithOutScale(number: BigInteger, decimals: Int): BigDecimal {
    return try {
      BigDecimal(number).divide(BigDecimal.valueOf(10.0.pow(decimals.toDouble())))
    } catch (e: Exception) {
      BigDecimal.ZERO
    }
  }

  /**
   * ********** String 类型的数字转为去除精度的 BigDecimal 并截取一定的小数点位数 ******************
   */
  @JvmStatic
  fun string2BigDecimalWithScale(number: String?, decimals: Int): BigDecimal {
    // 确保构建 BigDecimal 能成功，不成功，默认为 0
    val numberBigDecimal = try {
      BigDecimal(number)
    } catch (e: Exception) {
      BigDecimal.ZERO
    }
    return string2BigDecimalWithScale(numberBigDecimal, decimals)
  }

  /**
   * ********** BigInteger 类型的数字转为去除精度的 BigDecimal 并截取一定的小数点位数 ******************
   */
  fun string2BigDecimalWithScale(number: BigInteger?, decimals: Int): BigDecimal {
    // 确保构建 BigDecimal 能成功，不成功，默认为 0
    val numberBigDecimal = try {
      BigDecimal(number)
    } catch (e: Exception) {
      BigDecimal.ZERO
    }
    return string2BigDecimalWithScale(numberBigDecimal, decimals)
  }

  /**
   * ********** BigDecimal 去除精度并截取一定的小数点位数 ******************
   */
  fun string2BigDecimalWithScale(number: BigDecimal, decimals: Int): BigDecimal {
    return try {
      val bigDecimalNumber = number.divide(BigDecimal.valueOf(10.0.pow(decimals)))
      // 默认截取 6 位，如果 decimals 小于 6，则取 decimals
      bigDecimalNumber.setScale(getMinScale(decimals), BigDecimal.ROUND_DOWN)
    } catch (e: Exception) {
      BigDecimal.ZERO
    }
  }

  fun string2Int(number: String): Int {
    return if (isEmpty(number)) {
      0
    } else try {
      number.toInt()
    } catch (e: Exception) {
      0
    }
  }

  fun stringToBigDecimal(number: String?): BigDecimal {
    return if (isEmpty(number)) {
      BigDecimal.ZERO
    } else try {
      BigDecimal(number)
    } catch (e: Exception) {
      BigDecimal.ZERO
    }
  }

  //big integer to Uint64
  fun unsigned(b: BigInteger): BigInteger {
    if (b.signum() >= 0) {
      return b
    }
    val a1 = b.toByteArray()
    val a2 = ByteArray(a1.size + 1)
    a2[0] = 0
    System.arraycopy(a1, 0, a2, 1, a1.size)
    return BigInteger(a2)
  }

  private fun removeTheEndZero(input: String): String {
    var result = ""
    result = if (!isEmpty(input)) {
      input
    } else {
      "0"
    }
    while (result.endsWith("0")) {
      result = result.substring(0, result.length - 1)
    }
    if (result.endsWith(".")) {
      result = result.substring(0, result.length - 1)
    }
    return result
  }

  fun checkCanParseStringToBigDecimal(number: String?): Boolean {
    return try {
      BigDecimal(number)
      true
    } catch (e: Exception) {
      false
    }
  }

  fun checkCanParseStringToInt(number: String?): Boolean {
    return try {
      number!!.toInt()
      true
    } catch (e: Exception) {
      false
    }
  }

  fun isEmpty(str: CharSequence?): Boolean {
    return str == null || str.isEmpty()
  }
}
