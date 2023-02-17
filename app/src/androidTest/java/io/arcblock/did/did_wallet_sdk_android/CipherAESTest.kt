package io.arcblock.did.did_wallet_sdk_android

import android.util.Log
import io.arcblock.walletkit.did.toHexString
import io.arcblock.walletkit.jwt.ArcJWT
import io.arcblock.walletkit.utils.AESEcbUtil
import io.arcblock.walletkit.utils.Base58Btc
import okio.internal.commonToUtf8String
import org.junit.Assert
import org.junit.Test

class CipherAESTest {

  @Test
  fun testAESEncrypt() {
    val codedText = AESEcbUtil.encryptString2Byte("abcd", "123456")
    println("codedText: ${codedText.toHexString()}")
    Log.d("test AES", "codedText: ${codedText.toHexString()}")
    Assert.assertEquals("1c72c78500c351aefddf48ffa193e71a", codedText.toHexString() .lowercase())
    Assert.assertEquals("abcd", AESEcbUtil.decryptByte2Byte(codedText, "123456").commonToUtf8String())
    Assert.assertEquals("abcd", AESEcbUtil.decryptByte2String(codedText, "123456"))
  }

  @Test
  fun testAESEmpty() {
    val codedText = AESEcbUtil.encryptString2Byte("", "123456")
    Assert.assertEquals("", AESEcbUtil.decryptByte2Byte(codedText, "123456").commonToUtf8String())
    Assert.assertEquals("", AESEcbUtil.decryptByte2String(codedText, "123456"))
  }


}