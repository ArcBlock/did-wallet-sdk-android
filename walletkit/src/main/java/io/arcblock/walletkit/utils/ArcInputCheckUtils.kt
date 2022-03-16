package io.arcblock.walletkit.utils

import java.math.BigDecimal
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.min

/**
 * Created by Nate on 11/10/21
 */
object ArcInputCheckUtils {

  const val DEFAULT_DECIMALS = 6

  @JvmStatic
  fun getMinScale(scale: Int): Int {
    return min(DEFAULT_DECIMALS, scale)
  }

  private fun genRegex(digitsAfterZero: Int = DEFAULT_DECIMALS): Pattern {
    val regex = String.format("^[0-9]{0,18}+(\\.[0-9]{0,%d})?", digitsAfterZero)
    return Pattern.compile(regex)
  }

  /**
   * Given a string, check if it's a valid decimal number
   *
   * @param input the string to be validated.
   * @param digitsAfterZero The number of digits after the decimal point.
   * @return Nothing.
   */
  private fun isValidDigital(input: String, digitsAfterZero: Int = DEFAULT_DECIMALS): Boolean {
    val mPattern = genRegex(digitsAfterZero)
    val matcher: Matcher = mPattern.matcher(input)
    val isMatch = matcher.matches()
    // 确保输入的 string 最终能够正确的转换成 BigDecimal 对象
    val canBeDecimal = try {
      BigDecimal(input)
      true
    } catch (e: Exception) {
      false
    }
    return isMatch && canBeDecimal
  }

  fun isAmountValid(input: String, digitsAfterZero: Int = DEFAULT_DECIMALS): Boolean {
    val realDecimal = getMinScale(digitsAfterZero)
    return isValidDigital(input, realDecimal)
  }

  fun isValidGasPrice(input: String): Boolean {
    return isValidDigital(input, digitsAfterZero = 9)
  }

  fun isValidGasLimit(input: String): Boolean {
    return input.toIntOrNull() != null && input.toInt() >= 21000
  }

}
