package io.arcblock.walletkit.bip44

import io.arcblock.walletkit.did.DidType
import io.arcblock.walletkit.did.IdGenerator
import io.arcblock.walletkit.did.KeyType
import io.arcblock.walletkit.utils.encodeB64
import io.arcblock.walletkit.utils.encodeB64Url
import org.bitcoinj.crypto.MnemonicCode
import org.junit.Assert
import org.junit.Test
import org.web3j.crypto.Keys

/**
 * Author       :paperhuang
 * Time         :2019/2/18
 * Edited By    :
 * Edited Time  :
 */
class Bip44UtilsTest {
  @Test
  fun bip44Gen() {
    val apk = "zAaanxNCcsmymDfn8ZfMNX7sdP1tni4Kr5gEg28Di9Qus"
    val seed = Bip44Utils.genSeed("123", "123", "").seedBytes ?: ByteArray(0)
    val keyPair = IdGenerator.genAppKeyPair(apk, 0, seed, KeyType.ED25519)
    val sk = keyPair.privateKey.encodeB64()
    val pk = keyPair.publicKey.encodeB64()
    println("sk:$sk")
    println("pk:$pk")
  }

  // test eth 12 words gen seed and recover
  @Test
  fun mnemonicCode() {
    val mnemonicCodes = Bip44Utils.genMnemonicCodes()
    println("mnemonicCodes:${mnemonicCodes.joinToString(",")}")
    var shouldCheckPass = false
    try {
      MnemonicCode.INSTANCE.check(mnemonicCodes)
      shouldCheckPass = true
    } catch (e: Exception) {
      println("check word list fail: $e")
    }
    println("checkPass:$shouldCheckPass")
    Assert.assertTrue(shouldCheckPass)

    var shouldCheckFail = false
    try {
      val errorCodes = ArrayList<String>()
      errorCodes.addAll(mnemonicCodes)
      errorCodes[11] = "add123456"
      MnemonicCode.INSTANCE.check(errorCodes)
    } catch (e: Exception) {
      println("check word list fail: $e")
      shouldCheckFail = true
    }
    println("shouldCheckFail:$shouldCheckFail")
    Assert.assertTrue(shouldCheckFail)

    val seed = Bip44Utils.genSeedByInsertMnemonics(mnemonicCodes)
    println("seed:${seed.seedBytes?.encodeB64Url()}")
  }

  @Test
  fun genETHAddressByMnemonicCode() {
    val codes =
      "virus million sister elite junior comfort since grain train artefact lunch fever".split(" ")
    val seed = Bip44Utils.genSeedByInsertMnemonics(codes)
    println("seed:${seed.seedBytes?.encodeB64Url()}")

    // DID 派生规则生成的以太坊地址
    val keyPair = IdGenerator.genAppKeyPair("", 0, seed.seedBytes!!, KeyType.ETHEREUM)
    val addressFromIdGen1 = IdGenerator.pk2Address(
      keyPair.publicKey, DidType.DID_TYPE_ETHEREUM
    )
    println("addressFromIdGen1:${addressFromIdGen1}")
    Assert.assertEquals(addressFromIdGen1, "0xBA6f19e811A21a18F31D1320C18842b77f6D3afD")

    // 以太坊钱包默认派生规则生成的以太坊地址
    val keyPair2 = Bip44Utils.genKeyPair(seed.seedBytes)

    val addressFromIdGen2 = IdGenerator.pk2Address(
      keyPair2.publicKey.toByteArray(), DidType.DID_TYPE_ETHEREUM
    )
    println("addressFromIdGen2:${addressFromIdGen2}")
    Assert.assertEquals(addressFromIdGen2, "0xd89972DC4247fdf76570116A1CF643135a062916")
  }

  // 这个测试的目的是测试 ECKeypair 转 DIDKeypair 之后会不会影响地址的生成
  @Test
  fun testGenAccountAddress() {
    var count = 0
    while (count < 1000) {
      println("=====count:$count=====")
      val mnemonicCodes = Bip44Utils.genMnemonicCodes()
      println("mnemonicCodes:${mnemonicCodes.joinToString(",")}")
      val seed = Bip44Utils.genSeedByInsertMnemonics(mnemonicCodes)
      val keyPair = Bip44Utils.genKeyPair(seed.seedBytes)
      println("origin sk length:${keyPair.privateKey.toByteArray().size}")
      println("origin pk length:${keyPair.publicKey.toByteArray().size}")
      val originAddress = Keys.toChecksumAddress(Keys.getAddress(keyPair.publicKey))
      println("origin address :$originAddress")
      val didKeyPair = IdGenerator.genETHDIDKeyPair(seed.seedBytes ?: byteArrayOf())
      println("format sk length:${didKeyPair.privateKey.size}")
      println("format pk length:${didKeyPair.publicKey.size}")
      val sk2Address = IdGenerator.sk2did(didKeyPair.privateKey, DidType.DID_TYPE_ETHEREUM)
      val pk2Address = IdGenerator.pk2Address(didKeyPair.publicKey, DidType.DID_TYPE_ETHEREUM)
      println("sk2Address address :$sk2Address")
      println("pk2Address address :$pk2Address")
      Assert.assertEquals(originAddress, sk2Address)
      Assert.assertEquals(originAddress, pk2Address)
      count++
    }
  }

}
