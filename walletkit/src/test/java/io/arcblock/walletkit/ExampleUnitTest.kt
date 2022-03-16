package io.arcblock.walletkit

import io.arcblock.walletkit.jwt.ArcJWT
import org.junit.Test

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

import org.junit.Assert.assertEquals

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
  internal var format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")


  @Test
  fun addition_isCorrect() {
    assertEquals(4, (2 + 2).toLong())
  }

  @Test
  fun testSha224() {
    val date = isoToUtc("2001-01-01 01:00:00")

    val cal = Calendar.getInstance()
    cal.time = date
    val tz = cal.timeZone
    println("Time zone=" + tz.displayName)
    println("Time zone=$cal")
  }

  private fun isoToUtc(iso: String): Date {
    return try {
      format.parse(iso.replace("T", " ").replace("Z", " "))!!
    } catch (e: ParseException) {
      println(" err:" + e.message)
      Date()
    }

  }
}
