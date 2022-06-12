package io.arcblock.walletkit.did

import com.google.common.base.Preconditions
import com.google.common.io.BaseEncoding
import io.arcblock.walletkit.bip44.Bip44Utils
import io.arcblock.walletkit.did.EncodingType.BASE16
import io.arcblock.walletkit.did.EncodingType.BASE58
import io.arcblock.walletkit.did.HashType.KECCAK
import io.arcblock.walletkit.did.HashType.SHA2
import io.arcblock.walletkit.did.HashType.SHA3
import io.arcblock.walletkit.did.KeyType.ED25519
import io.arcblock.walletkit.did.KeyType.ETHEREUM
import io.arcblock.walletkit.did.KeyType.SECP256K1
import io.arcblock.walletkit.did.RoleType.ACCOUNT
import io.arcblock.walletkit.did.RoleType.APPLICATION
import io.arcblock.walletkit.did.RoleType.DELEGATE
import io.arcblock.walletkit.did.RoleType.SWAP
import io.arcblock.walletkit.hash.Hasher
import io.arcblock.walletkit.solana.Account
import io.arcblock.walletkit.solana.TweetNaclFast
import io.arcblock.walletkit.solana.crypto.HmacSha512
import io.arcblock.walletkit.solana.crypto.HmacSha512.hmac512
import io.arcblock.walletkit.utils.address
import io.arcblock.walletkit.utils.decodeB16
import io.arcblock.walletkit.utils.decodeB58
import org.bitcoinj.core.Base58
import org.bitcoinj.core.Utils
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDDerivationException
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.HDUtils
import org.bitcoinj.crypto.MnemonicCode
import org.junit.Assert
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.Locale

/**
 * Author       :paperhuang
 * Time         :2019/2/15
 * Edited By    :
 * Edited Time  :
 */
class IdGeneratorTest {

  @Test
  fun preAppend() {
    val append = (APPLICATION.ordinal.shl(9).and(0b111111000000000)).toString(2)
    System.out.println(append)
    val r0 = IdGenerator.preAppend(ACCOUNT, ED25519, KECCAK)
    System.out.println(BaseEncoding.base16().encode(r0))
    val r1 = IdGenerator.preAppend(APPLICATION, ED25519, SHA3)
    System.out.println(BaseEncoding.base16().encode(r1))
    r1.forEach {
      System.out.println(it.toChar())
    }
  }

  @Test
  fun testSECP256K1() {
    val sk = "26954E19E8781905E2CF91A18AE4F36A954C142176EE1BC27C2635520C49BC55".decodeB16()
    println("pk:${BaseEncoding.base16().encode(IdGenerator.sk2pk(SECP256K1, sk))}")
    println("pk size:${IdGenerator.sk2pk(SECP256K1, sk).size}")
  }

  @Test
  fun getDidTypeByAddress() {
    // get did type from eth address
    val type1 = DidType.getDidTypeByAddress("0xFCAd0B19bB29D4674531d6f115237E16AfCE377c")
    Assert.assertEquals(RoleType.ACCOUNT, type1.roleType)
    Assert.assertEquals(KeyType.ETHEREUM, type1.keyType)
    Assert.assertEquals(HashType.KECCAK, type1.hashType)
    Assert.assertEquals(BASE16, type1.encodingType)

    // get did type from empty string
    val type2 = DidType.getDidTypeByAddress("")
    Assert.assertEquals(RoleType.ACCOUNT, type2.roleType)
    Assert.assertEquals(KeyType.ED25519, type2.keyType)
    Assert.assertEquals(HashType.SHA3, type2.hashType)
    Assert.assertEquals(BASE58, type2.encodingType)

    // get did type from a error string
    val type3 = DidType.getDidTypeByAddress("123")
    Assert.assertEquals(RoleType.ACCOUNT, type3.roleType)
    Assert.assertEquals(KeyType.ED25519, type3.keyType)
    Assert.assertEquals(HashType.SHA3, type3.hashType)
    Assert.assertEquals(BASE58, type3.encodingType)

    // get did type from a did address - ed25519
    val type4 = DidType.getDidTypeByAddress("z1n9fgDEKMWdnEJ46Uftg732hVvVx3gs9yu")
    Assert.assertEquals(RoleType.ACCOUNT, type4.roleType)
    Assert.assertEquals(KeyType.ED25519, type4.keyType)
    Assert.assertEquals(HashType.SHA3, type4.hashType)
    Assert.assertEquals(BASE58, type4.encodingType)

    // get did type from a did address - SECP256K1
    val type5 = DidType.getDidTypeByAddress("z1Ee1H8g248HqroacmEnZzMYgbhjz1Z2WSvv")
    Assert.assertEquals(RoleType.ACCOUNT, type5.roleType)
    Assert.assertEquals(KeyType.SECP256K1, type5.keyType)
    Assert.assertEquals(HashType.SHA3, type5.hashType)
    Assert.assertEquals(BASE58, type5.encodingType)

    // get did type from a did address - swap
    val type6 = DidType.getDidTypeByAddress("z2UHsX5Gzj24oT81Kis6fekS1xTRvdejNqM88")
    Assert.assertEquals(SWAP, type6.roleType)
    Assert.assertEquals(KeyType.ED25519, type6.keyType)
    Assert.assertEquals(SHA2, type6.hashType)
    Assert.assertEquals(BASE58, type6.encodingType)

    // get did type from a did address - DELEGATE
    val type7 = DidType.getDidTypeByAddress("z2bN1iucQC2obei6B2cJrtp7d9zbVCKoceKEo")
    Assert.assertEquals(DELEGATE, type7.roleType)
    Assert.assertEquals(KeyType.ED25519, type7.keyType)
    Assert.assertEquals(HashType.SHA3, type7.hashType)
    Assert.assertEquals(BASE58, type7.encodingType)
  }

  @Test
  fun hash() {
    val out = Hasher.hash(SHA3, "123".toByteArray())
    System.out.println(out.toHexString())
    System.out.println((out.sliceArray(0..1).toHexString()))
  }

  @Test
  fun testGenDIDFromSeed() {
    val seed = BaseEncoding.base16().decode(
        "07abfceff5cdfb0cd164d2da98099c15b7223fc5a1b8c02c2cf1f74670c72aac27e1d28ed47cf4f2c4330a6e6e1dc0724721e80fa56177fdba926937a253fe7e".toUpperCase(
          Locale.ROOT))
    val kp = IdGenerator.genAppKeyPair("", 0, seed, ED25519)
    val did = IdGenerator.sk2did(kp.privateKey)
    println("did:$did")
    var sk1 = BaseEncoding.base16()
      .decode("91F9EEA50EBCE155E6F1BAB1EB9C6D74AF3DB1D2C20A951F790E9AA28285DEF28270BBEABBB608361CBD3610A8220B66CBC8D5F82B7DDA6EB3F880D9FB2460EC")
    Assert.assertEquals("z1k1i9VcGPNYH5Fx1ufnrtwo5Lk7onMTn7R", IdGenerator.sk2did(sk1))
    sk1 = BaseEncoding.base16()
      .decode("87F591D4E5E09DD59E4D520F80CE004577D53C348493C296E49CDD729FB4FA5FA483C813A355A0671009EC75349BAF3322FFAEE628515F0BBCEA6563A6A1445E")
    Assert.assertEquals("z1n9fgDEKMWdnEJ46Uftg732hVvVx3gs9yu", IdGenerator.sk2did(sk1))
  }

  // should match elixir did test case
  @Test
  fun sk2didWithDifferentType() {
    val sk1 =
      "3E0F9A313300226D51E33D5D98A126E86396956122E97E32D31CEE2277380B83FF47B3022FA503EAA1E9FA4B20FA8B16694EA56096F3A2E9109714062B3486D9"
    val pk = IdGenerator.sk2pk(ED25519, BaseEncoding.base16().decode(sk1))
    println("sk:${BaseEncoding.base16().encode(BaseEncoding.base16().decode(sk1))}")
    println("pk:${BaseEncoding.base16().encode(pk)}")
//    Assert.assertEquals(
//      "z1ioGHFYiEemfLa3hQjk4JTwWTQPu1g2YxP",
//      IdGenerator.sk2did(BaseEncoding.base16().decode(sk1)).address()
//    )

    val pk1 = "FF47B3022FA503EAA1E9FA4B20FA8B16694EA56096F3A2E9109714062B3486D9"
    Assert.assertEquals(
      "z1ioGHFYiEemfLa3hQjk4JTwWTQPu1g2YxP",
      IdGenerator.pk2did(BaseEncoding.base16().decode(pk1)).address())

    val sk = "26954E19E8781905E2CF91A18AE4F36A954C142176EE1BC27C2635520C49BC55"
    Assert.assertEquals(
      "z1Ee1H8g248HqroacmEnZzMYgbhjz1Z2WSvv", IdGenerator.sk2did(
        BaseEncoding.base16().decode(sk), DidType(ACCOUNT, SECP256K1, SHA3, BASE58)).address())

    Assert.assertEquals(
      "0x0021E4B8F62674897ED75DF0F7356E82C6F9A64A5C13F3CC0CD3", IdGenerator.sk2did(
        BaseEncoding.base16().decode(sk), DidType(ACCOUNT, SECP256K1, SHA3, BASE16)).address())

    Assert.assertEquals(
      "z1Ee1H8g248HqroacmEnZzMYgbhjz1Z2WSvv", IdGenerator.pk2did(
        IdGenerator.sk2pk(
          SECP256K1,
          BaseEncoding.base16()
            .decode("26954E19E8781905E2CF91A18AE4F36A954C142176EE1BC27C2635520C49BC55")),
        DidType(ACCOUNT, SECP256K1, SHA3, BASE58)).address())

  }

  @Test
  fun seedToDid() {
    val mnemonicCodes = Bip44Utils.genMnemonicCodes()
    val seed = Bip44Utils.genSeedByInsertMnemonics(mnemonicCodes).seedBytes!!
    val kp = IdGenerator.genAppKeyPair(
      "did:abt:zZeZVr12oY4hEevye8yo8BbRE75CuWBogHYNVbaceKkCw5wUYuNissZx3JgGk", 0, seed, ED25519)
    val did = IdGenerator.sk2did(kp.privateKey)
    val address = IdGenerator.pk2Address(kp.publicKey)
    val address2 = IdGenerator.pk2did(kp.publicKey)
    println("did:$did")
    println("address:$address")
    println("address2:$address2")
    Assert.assertTrue(
      DidUtils.isFromPublicKey(
        address2.address(), kp.publicKey))
  }

  @Test
  fun isFromPublicKey() {
    val address = "did:abt:0x71d75889cbe882cFa747B4BBE9a349163991AE45"
    val pk =
      "z3DmL11f5TiTNmRDVCSMdsoPYWdM8N5b9rf8K3fi9sQuHnX84yBpfxj4B8m7VTcAeiRqWBLSwAyvruZiNxZqLaGyt".decodeB58()
    val result = DidUtils.isFromPublicKey(address, pk)
    assert(result)

    val address2 = "0x39875F135Ddca2e18e3d1679bCa6059198b00aA2"
    val pk2 =
      "z3vgraBB9AfCjCexFWY8oztu6rdu3Wa9gpVQxJVzoLLo35FZaD9RkBcSp958iqxpFugiVcWeFe6Jf9U8p8GAh3gpT".decodeB58()
    println("pk2:${BaseEncoding.base16().encode(pk2)}")
    assert(DidUtils.isFromPublicKey(address2, pk2))
  }

  @Test
  fun testETH() {
    val testSKs = arrayOf(
      "4646464646464646464646464646464646464646464646464646464646464646",
      "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
    val testPKs = arrayOf(
      "4bc2a31265153f07e70e0bab08724e6b85e217f8cd628ceb62974247bb493382ce28cab79ad7119ee1ad3ebcdb98a16805211530ecc6cfefa1b88e6dff99232a",
      "4646ae5047316b4230d0086c8acec687f00b1cd9d1dc634f6cb358ac0a9a8ffffe77b4dd0a4bfb95851f3b7355c781dd60f8418fc8a65d14907aff47c903a559")
    val testAddresses = arrayOf(
      "0x9d8A62f656a8d1615C1294fd71e9CFb3E4855A4F", "0xFCAd0B19bB29D4674531d6f115237E16AfCE377c")

    testSKs.forEachIndexed { index, sk ->
      val sk1 = sk.toUpperCase(Locale.ROOT)
      val pk1 = IdGenerator.sk2pk(ETHEREUM, BaseEncoding.base16().decode(sk1))
      // should get the same pk from the sk
      Assert.assertEquals(
        testPKs[index].toUpperCase(Locale.ROOT), BaseEncoding.base16().encode(pk1))
      val addressFromIdGen1 = IdGenerator.pk2Address(
        pk1, DidType.DID_TYPE_ETHEREUM)
      // should generate eth address from publicKey as expected
      Assert.assertEquals(testAddresses[index], addressFromIdGen1)
      // should generate eth address from privateKey as expected
      Assert.assertEquals(
        testAddresses[index],
        IdGenerator.sk2did(BaseEncoding.base16().decode(sk1), DidType.DID_TYPE_ETHEREUM))
      // should validate eth address from publicKey as expected
      Assert.assertTrue(
        DidUtils.isFromPublicKey(
          testAddresses[index],
          BaseEncoding.base16().decode(testPKs[index].toUpperCase(Locale.ROOT))))
    }
  }

  @Test
  fun testGenDelegateAddress() {
    val address = IdGenerator.genDelegateAddress(
      "z1ewYeWM7cLamiB6qy6mDHnzw1U5wEZCoj7", "z1T6maYajgDLjhVErT71WEbqaxaWHs9nqpZ")
    Assert.assertEquals("z2bN1iucQC2obei6B2cJrtp7d9zbVCKoceKEo", address)
  }

  @Throws(HDDerivationException::class)
  fun createMasterPrivateKey(seed: ByteArray): DeterministicKey {
    Preconditions.checkArgument(seed.size > 8, "Seed is too short and could be brute forced")
    // Calculate I = HMAC-SHA512(key="Bitcoin seed", msg=S)
    val i = hmac512(seed, "ed25519 seed".toByteArray(StandardCharsets.UTF_8))
    // Split I into two 32-byte sequences, Il and Ir.
    // Use Il as master secret key, and Ir as master chain code.
    Preconditions.checkState(i.size == 64, i.size)
    val il = Arrays.copyOfRange(i, 0, 32)
    val ir = Arrays.copyOfRange(i, 32, 64)
    Arrays.fill(i, 0.toByte())
    val masterPrivKey = HDKeyDerivation.createMasterPrivKeyFromBytes(il, ir)
    Arrays.fill(il, 0.toByte())
    Arrays.fill(ir, 0.toByte())
    // Child deterministic keys will chain up to their parents to find the keys.
    masterPrivKey.creationTimeSeconds = Utils.currentTimeSeconds()
    return masterPrivKey
  }

  @Test
  fun testGenSolanaAddress() {
    val codes =
      "burger brass envelope shock plate arrive umbrella goat kite measure urge apple".split(" ")
    val account = Account.fromMnemonic(codes, "")
    println(Base58.encode(account.secretKey))
    println(account.publicKey.toString())

    val seeds = MnemonicCode.toSeed(codes, "")
    val pathArray = "m/44'/501'/0'/0'".split("/".toRegex()).toTypedArray()
    var dkKey = createMasterPrivateKey(seeds)

    println("m sk=>${BaseEncoding.base16().encode(dkKey.privKeyBytes)}")

    for (i in 1 until pathArray.size) {
      val childNumber = if (pathArray[i].endsWith("'")) {
        val number = pathArray[i].substring(0, pathArray[i].length - 1).toInt()
        ChildNumber(number, true)
      } else {
        val number = pathArray[i].toInt()
        ChildNumber(number, false)
      }
      dkKey = HDKeyDerivation.deriveChildKey(dkKey, childNumber)
    }

    println("sdk sk=>${BaseEncoding.base16().encode(dkKey.privKeyBytes)}")
    val keypair = TweetNaclFast.Signature.keyPair_fromSeed(dkKey.privKeyBytes)

    val account1 = Account(keypair)

    println(Base58.encode(account1.secretKey))
    println(account1.publicKey.toString())
  }

}
