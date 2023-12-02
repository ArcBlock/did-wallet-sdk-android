package io.arcblock.walletkit

import io.arcblock.walletkit.did.toHexString
import io.arcblock.walletkit.jwt.ArcJWT
import io.arcblock.walletkit.utils.Base58Btc
import okio.internal.commonToUtf8String
import org.junit.Test

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

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


  @Test
  fun testBase58() {
    println("z1ZidAgkXsgnaDprBer7CkVNWSYZjoEfydW".length)
    println("zfffffffffffffffffffffffffffffffff".length)
      val x = Base58Btc.decode("zfffffffffffffffffffffffffffffffff")
    println("decode: ${x.toHexString()}")
  }

  @Test
  fun testAESUtf8(){
    val tmpJWT = "z6imeZiXqQiqix82ix29VJHXLi9ED7qNNV336ZKuDuU3CY1DPMJPeJ6df4ch5YspweKoqPg4QW9SUV3XoFjTorRxC7SFF8ATSMDKHj5WHVGEjuvLb1xyTGEJLBRQnKacAhzVu9aHsCn5BoRRJTr5nhdbXWmDzHypX8ZV4qxigQLkTQRg9yXEK7sXtgAB5jBzXGM9rRNcsnxXkujd9Wuo99jyT43mzPGouaf2AtkHwoiHLKrpDGyyiHGY4V3sJMgdtGKqAmpi2UdUBJvv97eYSHQXvhRadtoXUzgE32mRGidByfLGBoEeQ71iwbKJkfZVUvKTYLfZUdGLC8Nfowm3bB8uqELGZWAAyDQLoBTZERKobogs1E4Bi99gAnrbSLPx4uQ1XQvgVBQadjpNSUAhqcCHz387Gj8dVqtxUKXE1VM3MgnS4SjdnwjN3jRCFqEEmKE49Dq9Bo3LKcBaotcvY9136zm2gNEBnCHzoteyf8sMZPEpRfu3BGZiqNCWgBfrktHxxiDhd8ndn4xVNCAG9z38jkLXrBN18tSA3m6tttQNo3win7MYtYhrjG9Wa4iBmFKqjEBNyEafbKpgsiRDu1L75dJUNN7FauRBUbSWh1BXPxg6oYY19McUpd8gTri9omHBHvGewsfVD4iLYgW6yZcYGLJHroNLq3iYjbyBD59YRBDw3qa"

    val x = ArcJWT.parseJWT(Base58Btc.decode(tmpJWT).commonToUtf8String())
    println(x)

  }
}
