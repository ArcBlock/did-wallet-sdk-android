package io.arcblock.walletkit.utils

import com.google.common.io.BaseEncoding
import io.arcblock.walletkit.did.KeyType.ED25519
import io.arcblock.walletkit.did.toHexString
import io.arcblock.walletkit.signer.Signer
import org.junit.Assert
import org.junit.Test
import java.nio.charset.Charset

class StringExtTest {
  @Test
  fun testStringExt() {
    val input = byteArrayOf(97, 98,99, 100, 101)
    Assert.assertEquals("abcde", input.toByteString().toString(Charsets.UTF_8))
    val sk = "18E14A7B6A307F426A94F8114701E7C8E774E7F9A47E2C2035DB29A206321725"

    val pk =
      "50863AD64A87AE8A2FE83C1AF1A8403CB53F53E486D8511DAD8A04887E5B23522CD470243453A299FA9E77237716103ABC11A1DF38855ED6F2EE187E9C582BA6"
    val skByte = BaseEncoding.base16().decode(sk)
    Assert.assertEquals(input.sign(skByte).toHexString(), Signer.sign(ED25519, input, skByte).toHexString())
    Assert.assertEquals(input.sign(skByte, ED25519).toHexString(), Signer.sign(ED25519, input, skByte).toHexString())



  }
}