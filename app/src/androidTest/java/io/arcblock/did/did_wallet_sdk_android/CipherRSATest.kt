package io.arcblock.did.did_wallet_sdk_android

import android.util.Log
import io.arcblock.walletkit.did.toHexString
import io.arcblock.walletkit.utils.AESEcbUtil
import io.arcblock.walletkit.utils.RSAUtil
import io.arcblock.walletkit.utils.decodeB58
import io.arcblock.walletkit.utils.decodeB64
import io.arcblock.walletkit.utils.encodeB58
import io.arcblock.walletkit.utils.encodeB64
import okio.internal.commonToUtf8String
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.security.PrivateKey
import java.security.PublicKey

class CipherTest {

  lateinit var pkFromJs: PublicKey
  lateinit var skFromJs: PrivateKey


  @Before
  fun setup(){
    val  pkPEM = """MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApk/Ovd2m0IyMGwSBB2I5mMRD
      soYRBdGDtyW+eF2a/qDa8JVekmsAYA
      +DofkkFTAu1oJZ
      s1afXALpNMcEMYXhxFCdPgOSpal2cL6dc7jYQkm1VopGvg9oIA5IJrwLHSqqkh0V+YDdepU7OStzH5n8RLh/Thb8od+JtYQkAuy9CbifU+A5CBe7FCBvzMqGEil3oucVZ7t01vLVRJkAVWaCzQgwMvd/8HTPv0ebVeLndF6NfOHOoooLw2C5rtaoTLcC9eJBQabx/f4PTF37QDM0W7/Rv5fk0yWq3cB3xmZN4teEOQTuSe5XF1lBa6E8Vv+TqNI0A6eeUHLbSAV2U04YeQIDAQAB
    """.replace("\\s".toRegex(), "").trim();
    println("")
    val skPEM = """
    MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCmT8693abQjIwbBIEHYjmYxEOyhhEF0YO3Jb54XZr+oNrwlV6SawBgD4Oh+SQVMC7WglmzVp9cAuk0xwQxheHEUJ0+A5KlqXZwvp1zuNhCSbVWika+D2ggDkgmvAsdKqqSHRX5gN16lTs5K3MfmfxEuH9OFvyh34m1hCQC7L0JuJ9T4DkIF7sUIG/MyoYSKXei5xVnu3TW8tVEmQBVZoLNCDAy93/wdM+/R5tV4ud0Xo184c6iigvDYLmu1qhMtwL14kFBpvH9/g9MXftAMzRbv9G/l+TTJardwHfGZk3i14Q5BO5J7lcXWUFroTxW/5Oo0jQDp55QcttIBXZTThh5AgMBAAECggEAA9t2ABFT/SJFXZsNIw60J0bmCw3w9yGU3HqToFcLcTxp14qfVuYEbDXv56HPpG4pp+/+BJrNt2SZ5A95mWxxgAwemYGbtRvpE1RYcoam/WKYQhmS6nWRBK1QHxXdbB/BNQJXsCG9AUrUxM6tLN51a2KcEUOXOGnm177o1uiGueL0vMRavTAG7/8IIraGBB4h0GBgwaMhGo1JtzjkeXjJqA6AidsDSHa4mj3dfH1zcubLigaSz/azbmMNEQ08h0oyviz+4B06NbMXVbW92GQT/CKyMGpxtU+bj1Jh8ybaMIsT5NjWXcw4HLuztqwNnX4uQrhC269RqK6cL7sAm+D9wQKBgQDO4jrHVsoBDf09VM9N/irRB4a7mDj5d+PpD8y2gFksRh1MIjInXqz6BH4BIJootriYiT1p8Q5Ao6lNIOp57dUZlElZimZDZ2v9MmLv4+S+UYjDR+EgMt05z0vNzNXw3uv8uoic+xYfIF2mqxj0BC32mgWgToxkdZHch6zKY/7igwKBgQDNy7xuvPbM4hm+TRdkGK+BIb55ztryVIzbRzjNeeRj28ZpZQ6V9n7lvGSYds9+Afxzd1QVcecC20GfEDq8besAOThkKRaFrJVD0bcTwmHvRF/CfauhosGO4P3hodmRqhtJhx7O19c5Jth/Z6Uz2kOB9XK2Ew4zHjO2Rd9Z7wo4UwKBgCdUdM4unqqCqVD+jYaLOkKQxrllH/e1JhvJiCZt0gYLskgl/Bjl88Z4EihOtV/mFMPS210HmakKNAZYqprRbwC04xjlqblIsQvqh0qJrZPM1k4hnRfM86eo1AVk2os3Je/e2lfVmAgE1Cj6P/0ryj0mXMl0BVaXz0n4dQ3o4qzXAoGAYaL4in1ihj/7QLsojtfbZGOTEA1g+Tm9/kbjHzFmdy4NC3Hjoqho+iwQeflcZgchM9L4dJgupr9JeeLkSwPHS7raE0MfKVqBEsULm/dMKY2B9S9UX4JtXJFIQmVcaOyQt6jAqBflR3szmfadfWVfQ+gkfVe7E+uPUzoBRpTPf3sCgYEAl/gAzMCHw9LpexmTitmmm7EK4K9XFLQ/m7QXHLJ9XAohdr6CqfLwscQ9BSuEf79E6jAiQLhpc9qJ13cG9gr79F0FKqnRlXTVs7gr7lQolzO+jwY8hD4qv3/b/717rDzAArjvw/GDKmdtIh/n2FlQY4wPaNrWxbU/fGexmhsn2d0=
    """.trim();
    pkFromJs = RSAUtil.getPublicKey(pkPEM.decodeB64())
    skFromJs = RSAUtil.getPrivateKey(skPEM.decodeB64())
  }


  @Test
  fun testAESEncrypt() {
    val codedText = AESEcbUtil.encryptString2Byte("abcd", "123456")
    println("codedText: ${codedText.toHexString()}")
    Log.d("test AES", "codedText: ${codedText.toHexString()}")
    Assert.assertEquals("1c72c78500c351aefddf48ffa193e71a", codedText.toHexString().lowercase())
  }



  @Test
  fun testRSAEmpty() {
    val cryptedTxt = RSAUtil.encrypt("".toByteArray(), pkFromJs)
    Assert.assertEquals("", RSAUtil.decrypt(cryptedTxt, skFromJs).commonToUtf8String())
    // Client should be able to decrypt follow and get empty string
    val cryptedFromWeb = "z49E3pw1L8b4UP9L5gtNZ9QYyGcqaCJCLECryRgboFZTWSj46nZEEPSTdVvpctvyw2ij1JsCTiL12VPx7mNoQRfnDCwmnjBP8ggDQ4UgJz3shNF8rRSHMPvKsznXPNWHZix9DVytjPFAowZnkeyAwUWj16oZg5FPB71dPMxJrTWH7c62iELunJkKScaj5v4iUyQ8SBtatNxE7fR4cPzZTysnvgeaqhgK14rKqZuncyMjcwzLAtH1dzpDwCChipSCJdY3w84CTMarLtyKnRkbaxhBG9G3ZiXsM7i2bzpWGNK5QVndnKr9n7N5jg2vBb3mAxk5HnrDVkKDMfkMKDUUj7JTMo6ZA3q"

    Assert.assertEquals("", RSAUtil.decrypt(cryptedFromWeb.decodeB58(), skFromJs).commonToUtf8String())

  }

  @Test
  fun testRSAEncrypt() {
// key-pair from browser
    val encryptRst = RSAUtil.encrypt("abcd".toByteArray(), pkFromJs)
    val rst = RSAUtil.decrypt(encryptRst, skFromJs).commonToUtf8String()
    Log.d("test RSA", "rst: $rst")
    Assert.assertEquals("abcd", rst)
    val jsDecrypt = RSAUtil.decrypt("z76MRS8iyHWJLMroRtpdZ9gzGKsWV7yx2qEkn3cnzbREjjp79iiuQymuyT74GNpjcHxVF5QZgW7d2E2CZb93sM6w8QBeZyWq1bst4KHVMzpG7pgwvvTwBnBh1957vAyafnivxG3EXAQevUnD5wrjUExTibT68GjxmrCtTyEZEyBZHpPx65j5eJfMgCmNRMsKJrYA4HRFrqiVQ6g7o56N1uyuhv7nvvn93AFgD7HdP3tzuw22ZqmWcyequVxwNAmoHzb2bF36ikCrwEKN6bioag4MJvt1SMPMYX9zAjnCCEwYDtdkTvPStvTqzvP1JfzwTjEBDiUybgFpzyjJG29AfNszLNKve2X".decodeB58(), skFromJs).commonToUtf8String()
    Log.d("test RSA", "rst: $jsDecrypt")
    Assert.assertEquals("abcd", jsDecrypt)

    val androidEncoded = RSAUtil.encrypt("android".toByteArray(), pkFromJs)
    Log.d("RSA android encoded", "android rst: ${androidEncoded.encodeB58()}")


  }




  @Test
  fun testGenRSAEncrypt() {
    val keyPair = RSAUtil.generateKey()
    val  pkPEM = """MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApk/Ovd2m0IyMGwSBB2I5mMRDsoYRBdGDtyW+eF2a/qDa8JVekmsAYA+DofkkFTAu1oJZs1afXALpNMcEMYXhxFCdPgOSpal2cL6dc7jYQkm1VopGvg9oIA5IJrwLHSqqkh0V+YDdepU7OStzH5n8RLh/Thb8od+JtYQkAuy9CbifU+A5CBe7FCBvzMqGEil3oucVZ7t01vLVRJkAVWaCzQgwMvd/8HTPv0ebVeLndF6NfOHOoooLw2C5rtaoTLcC9eJBQabx/f4PTF37QDM0W7/Rv5fk0yWq3cB3xmZN4teEOQTuSe5XF1lBa6E8Vv+TqNI0A6eeUHLbSAV2U04YeQIDAQAB
    """.trim()
    Log.d("test RSA", RSAUtil.PrivatePKCS1ToPEM(RSAUtil.toPKCS1PrivateKey(keyPair)))
    Log.d("test RSA", (RSAUtil.toPKCS1PrivateKey(keyPair)).encodeB64())
    Log.d("test RSA",  RSAUtil.PublicPKCS1ToPEM(RSAUtil.toPKCS1PublicKey(keyPair)))

//    Log.d("test RSA", "sk length: ${pkPEM.length}  ${keyPair.private.encoded.encodeB64().length}")
    val rst = RSAUtil.encrypt("arcblock".toByteArray(), keyPair.public)
    Log.d("test RSA", "rst: ${rst.encodeB58()}")

    val decryptTxt = RSAUtil.decrypt(rst,keyPair.private).commonToUtf8String()
    Assert.assertEquals("arcblock", decryptTxt)



  }

  @Test
  fun testKeyPairFromiOS(){
    val iosPk = RSAUtil.getPublicKey("""
      MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDH5iXDs89VN+t03UTuMu5BvXHRK0yEchPfuQs6aFYaxJvGRrMLgsE4wCacfWVuyPH1Z+PcyHlbR4Gg3bv6xpT3u/xLz0SycWLVPUs+6Dg0Lkp8pe6Soms3x+rV2Utza5wTCMW95pTsiS4FOlVFxv8KfHV7WtB/G1oM0xvij4TfNwIDAQAB
    """.replace("\\s".toRegex(), "").trimIndent().trim().decodeB64())
    val iosSk = RSAUtil.getPrivateKey("""
      MIICXAIBAAKBgQDH5iXDs89VN+t03UTuMu5BvXHRK0yEchPfuQs6aFYaxJvGRrMLgsE4wCacfWVuyPH1Z+PcyHlbR4Gg3bv6xpT3u/xLz0SycWLVPUs+6Dg0Lkp8pe6Soms3x+rV2Utza5wTCMW95pTsiS4FOlVFxv8KfHV7WtB/G1oM0xvij4TfNwIDAQABAoGAJDMNS69YMHp77SHUxb37X3RLFDHfHWz7JFyCKAWU8iwAeZt7+O2ox/okA9rBb5p8FpDFtsmnEFyWoOcGj5c+invw+XJAVaGzxFdHfFu9BOquA3qN0wbnwqaMV1f1Lc1sFPZb1jn9TUIkERLm2fUODhRWxzmKnCKvLJpz+flxg2ECQQD5hIcEkp0qb2ycGTrPcijSvFwMR3UmVlBdYy7EmbmNry+hHvzlEvqGYe8E/AvngtpaQ2hmj8iXAeJt5EgOvhClAkEAzReewgdrQb5jIM+TV6TPvjL3bmWt0clJeC8sHzWdKj6h8z03xwSt8jPQr98IFwz9Fsmug7Nus7p06IbNNNvtqwJAOc5RNWb+EEm2IMfbjxnEiWXn8VRQuKwAfFIxEI0IpuFyKGK0diGadq2ToVT/MQhoq8a4FiKzdwCRw6HCa9/unQJBALYIGOiuHQylJuagVm6b4ac3JVXe+YAR4BvEAq1QsMYmASgbVb5W8OzgAIGYu116MchgCXzrbX4oKEuaGdiouzECQG+fgEOUDOjmQWuqbhLX5HDT2LXDoFCyXW0hHapxvEjZMQult8leT3f/xyYPkjCr5rEv4gSI+HkXMJH0kU7wfu4=
    """.replace("\\s".toRegex(), "").trimIndent().trim().decodeB64())
    val cryptedTxt = "zKgVhi46bxDns2THiapPHx7apBqXuufKntQhxQQKVk2q2BDnmryHcJT4zjL9qrF2qnddiV5NkZLFNXsLPeZZ1XQQsVPrhT853DUtzHaiSu7uJEHTxxEDqZ12cHvNNdgFociBEwQkDh7PC72KDRzE9QUiMaW6dkSQSJfAegxxMYDQ8sJB".decodeB58()
    val decrytedTxt = RSAUtil.decrypt(cryptedTxt, iosSk).commonToUtf8String()
    Assert.assertEquals("abcd", decrytedTxt)
    val emptyDecryptedTxt = "zH4eVVTCLMLjPJJW5f9mfscCbXmLrVkxEUAz6bqBDSgcE2k5WxhAmhkXGmtborHGtruCpM1Gd1KgfxWR2ewAsPLaTVxL59BnWb4vUJnvCbyVneiaMQjTBEmRUVbAv1pFDLBnUgy9YicB3hNW4EJoFWG9iCB44h6R33XbLayqcepYFjTu".decodeB58()
    Assert.assertEquals("", RSAUtil.decrypt(emptyDecryptedTxt, iosSk ).commonToUtf8String())



    val iosEncryptedStr = "zKA9JrsBLVFA77ny6rhxCPp9h4YcbXykHdYYYtQ3mniRwrTA2Ch13DXV3NkY7C48oibR2vGtL8b8c83aaCrwhjjbpXctpLiADaBw2Ce5QcwGUAjt9gUqBk8FJdwJCaRu7wsR4eapM4t6E8tkQhTPhpasLd58xjA2qs8BcnpsDYTa5kCE"
    val skAndroidString= """
      MIICXAIBAAKBgQC1V+zpcci400n07h9IJap/W20YtfPY6BfmLastQu76HAPqrtBP
        yyj5YsTZ9ivucJPC7VcO4ZzB+Pq0fauZJOcbs7cNi4hRJe9FbrKu0uvHYuXDGuUg
        wy+NBnjSw9o2W/3LHMocz+LJsSB43CHp2fUXBzn+JdC1n4z8BAQl9q5mWwIDAQAB
        AoGAKfFaNGxC1qzX8DSbO56qnqZQx2ReMA8OaAisDN3sVCDirwcb2zjME1JK4XbU
        lmOnaXBnsGNyVFL3+YMPi25DnXoYFubTAhTFbUoTGyQpaUr0N5tyj6QTdwh/gMOL
        Pbvg7FvFEZaFXu95HtRMwj4ScpuY4FrfFdYU24bGZQslZvECQQD2HmHuEKHYr6Da
        zqS/bFK0EAKdJLgdlooQpFwstbs1IhVlUhnintVXTHITjaFAoTyGR/ue/d23+Rmy
        YE3DzQ9dAkEAvJ/H1nOmY2GWmiquczbPO//llanLEFmPAKnZNAFdV1FjBH3Ds8KH
        M46tFISiq2uwYeDz+Qjxa7Ql8qAH3drJFwJAHsK3XKjJgaqZwR84qhAg2g5yNS/E
        rzYEdYYFWzUve7mR0QMM5y0Q3wNX8qet8sT0KphOk5WJI5hHpOqybXlwpQJBALl8
        qqixu7rZGZ9rP3ffOzU2dM+TVDQ0zdKKNCTW/rJCP4wIHK4mKoxBzuRxdgH6eU4X
        R/PqnnYahoKsam/5mWsCQGIWJsaaUQqNaY0c5Pes1a4Jrrxd/W9fUVuOkgQQAFus
        GBRxjxzifqzn5G/CM6vX/dddr392ed2mqu/9cgD/qJM=
    """.replace("\\s".toRegex(), "").trim().trimIndent()
    val skAnd = RSAUtil.getPrivateKey(skAndroidString.decodeB64())
    Assert.assertEquals("abcd", RSAUtil.decrypt(iosEncryptedStr.decodeB58(), skAnd).commonToUtf8String())
  }
}