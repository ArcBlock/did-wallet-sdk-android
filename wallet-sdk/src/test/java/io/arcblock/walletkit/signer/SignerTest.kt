package io.arcblock.walletkit.signer

import com.google.common.io.BaseEncoding
import io.arcblock.walletkit.bip44.Bip44Utils
import io.arcblock.walletkit.did.*
import io.arcblock.walletkit.jwt.ArcJWT
import io.arcblock.walletkit.utils.*
import org.junit.Assert
import org.junit.Test
import org.web3j.crypto.ECKeyPair


/**
 * Author       :paperhuang
 * Time         :2019/3/4
 * Edited By    :
 * Edited Time  :
 **/
class SignerTest {

  @Test
  fun genDifferentSizePkAndSk() {
    val codes = Bip44Utils.genMnemonicCodes()
    val seed = Bip44Utils.genSeedByInsertMnemonics(codes)
    println("seed size:${seed.seedBytes!!.size}")
    val keyPair = IdGenerator.genAppKeyPair("", 0, seed.seedBytes!!, KeyType.ETHEREUM)
    println("sk size:${keyPair.privateKey.size}")
    println("sk hex:${ConvertUtils.bytes2hex(keyPair.privateKey)}")
    println("pk size:${keyPair.publicKey.size}")
    println("pk hex:${BaseEncoding.base16().encode(keyPair.publicKey)}")
  }

  @Test
  fun testSecp256() {
    val sk = BaseEncoding.base16()
      .decode("18E14A7B6A307F426A94F8114701E7C8E774E7F9A47E2C2035DB29A206321725")
    val pk =
      "0450863AD64A87AE8A2FE83C1AF1A8403CB53F53E486D8511DAD8A04887E5B23522CD470243453A299FA9E77237716103ABC11A1DF38855ED6F2EE187E9C582BA6"
    val kp = ECKeyPair.create(sk)
    Assert.assertEquals(BaseEncoding.base16().encode(kp.getFixedPK()), pk)
  }

  @Test
  fun testSecp256Sign() {
    var sk = BaseEncoding.base16()
      .decode("18E14A7B6A307F426A94F8114701E7C8E774E7F9A47E2C2035DB29A206321725")
    var pk = BaseEncoding.base16()
      .decode("0450863AD64A87AE8A2FE83C1AF1A8403CB53F53E486D8511DAD8A04887E5B23522CD470243453A299FA9E77237716103ABC11A1DF38855ED6F2EE187E9C582BA6")
    var kp = ECKeyPair.create(sk)
    println("pk:${kp.publicKey.toString(16)}")
    var msg = BaseEncoding.base16()
      .decode("15D0014A9CF581EC068B67500683A2784A15E1F68057E5E37AAF3A0F58F3C43F083D6A5630130399D4E5003EA191FDE30849")
    var sig = kp.sign(msg)
    println("sig:${sig.encodeToDER().toHexString()}")

    var sigWanted =
      "3045022100942F2DB25D6A0F6B01B195EDBAD8BB8F58F4EE85C7D5E1934649781D815F7ECE0220158DD32CB48D2A3A97267F4416A53692C51C72CD350F945D7BEA60376FD658D5"
    Assert.assertEquals(BaseEncoding.base16().encode(sig.encodeToDER()), sigWanted)

    assert(DidUtils.verify(msg, sig.encodeToDER(), pk))

    sk = BaseEncoding.base16()
      .decode("BA49D2E26C47CF1EED12DFF092C626E7B9F2F880921A20C4FB40606EEAEA8687")
    pk = BaseEncoding.base16()
      .decode("046FDB9C87D0BA520305D3A8D80F2DE13BA7406593B34AF140EEF0A47492893DEB1F999C1F05C6C71B55F769C725F3928FE8052C83D7DEDF7CC1F9187E9538CAFA")
    msg = BaseEncoding.base16()
      .decode("8C24B77F419D70BADD21A98137DD381BC2A3E7CC6ED1810E8BC3D612C350760B")
    kp = ECKeyPair.create(sk)
    println("pk:${kp.publicKey.toString(16)}")
    sig = kp.sign(msg)
    println("sig:${sig.encodeToDER().toHexString()}")

    assert(DidUtils.verify(msg, sig.encodeToDER(), pk))

    sk = BaseEncoding.base16()
      .decode("03A54A41458E83708251C5249662B0221F66B65D3371A7C525BB7A8DBF3241F7")
    pk = BaseEncoding.base16()
      .decode("049A82794A4D58D6EC56060FD162631E1C3F2BBB51F154D52190E428FDA6D2CDF3896570CECDA7136E259BE624AAA58AB9A8B758B30877BC72C009B1285A59D45D")
    msg = BaseEncoding.base16()
      .decode("D1977B932F99D1D1CD6C21FDAABDAC300F5F8D0F0E16D9B0F97DB59727668FAF")
    kp = ECKeyPair.create(sk)
    println("pk:${kp.publicKey.toString(16)}")
    sig = kp.sign(msg)
    println("sig:${sig.encodeToDER().toHexString()}")
    assert(DidUtils.verify(msg, sig.encodeToDER(), pk))
  }


  @Test
  fun randomSecp256Sign() {
    val sk = Bip44Utils.genSeed(
      System.currentTimeMillis().toString(),
      "abc",
      ""
    ).seedBytes?.sliceArray(0..31)
    println("sk:${sk?.toHexString()}")
    var kp = ECKeyPair.create(sk)
    var pk = kp.getFixedPK()
    var msg = BaseEncoding.base16()
      .decode("D1977B932F99D1D1CD6C21FDAABDAC300F5F8D0F0E16D9B0F97DB59727668FAF")
    var sig = kp.sign(msg)
    println("pk:${kp.getFixedPK().encodeB16()}")
    println("msg:D1977B932F99D1D1CD6C21FDAABDAC300F5F8D0F0E16D9B0F97DB59727668FAF")
    println("sig:${sig.encodeToDER().toHexString()}")
    assert(DidUtils.verify(msg, sig.encodeToDER(), pk))
  }

  @Test
  fun testEd25519() {
    val d =
      "zbFNtuU1rp9DvvWSG4G2k6P7ND6kzQWkZgPwqbbyHLPkgAiq9YXwScx5jkPqhbyE4qp12NohwDQzjHVhRwnJZijP7KePywbFioCAzVdSWoktVVWUY55HJGTBPu8emUhKUwWtdTyjffHne5WhySnuke4EWJrARr".decodeB58()
    var sig =
      "z31C4D3hfpLPUeEi6eXyZq6uxTXusD74doKA9wLHPGrrw19vxsm2FsxPw9XDtZBTzdfuaVDNtY5T68LNtxGXNV3qn".decodeB58()
    val pk = "zBQbgFnZsUEJeAfjbHNMEiRjCAxjbJu7hJtzNcaFZNX5f".decodeB58()
    assert(Signer.verify(KeyType.ED25519, d, pk, sig))
  }

  @Test
  fun testVerify() {
    val content =
      "eyJhbGciOiJFZDI1NTE5IiwidHlwZSI6IkpXVCJ9.eyJhZ2VudERpZCI6ImRpZDphYnQ6ek5LZFFzQkVQOXpKeXNleEpUcWtEQ3N1WG95dHpDQTY1UnA4IiwiZXhwIjoxNjA1MzEzMzU0LCJpYXQiOjE1NzM3NzczNTQsImlzcyI6ImRpZDphYnQ6ek5LZGVpaVo1Z2pNcWdLTEptb1hUdmhtNkVuSGZERnREb1FnIiwibmJmIjoxNTczNzc3MzU0LCJvcHMiOnsicHJvZmlsZSI6WyJmdWxsTmFtZSIsIm1vYmlsZVBob25lIiwibWFpbGluZ0FkZHJlc3MiXX0sInZlcnNpb24iOiIxLjAifQ".toByteArray()
    val sig =
      "K9Syv2CrhcZ2cdizfXggDMnUkP6BxE2lMfP2ORvE5wZkPeHKsdbNhf6WaAe22eBqIODlxUp1a9WwxFYdeZ7kAA".decodeB64Url()
    val pk = "zE6zBsjDMmYyFhHMegsLFk6WXRqhS4beoj8aa78uEC1te".decodeB58()
    assert(Signer.verify(KeyType.ED25519, pk = pk, content = content, signature = sig))

    assert(
      ArcJWT.verifyJWT(
        "eyJhbGciOiJFZDI1NTE5IiwidHlwZSI6IkpXVCJ9.eyJhZ2VudERpZCI6ImRpZDphYnQ6ek5LZFFzQkVQOXpKeXNleEpUcWtEQ3N1WG95dHpDQTY1UnA4IiwiZXhwIjoxNjA1MzEzMzU0LCJpYXQiOjE1NzM3NzczNTQsImlzcyI6ImRpZDphYnQ6ek5LZGVpaVo1Z2pNcWdLTEptb1hUdmhtNkVuSGZERnREb1FnIiwibmJmIjoxNTczNzc3MzU0LCJvcHMiOnsicHJvZmlsZSI6WyJmdWxsTmFtZSIsIm1vYmlsZVBob25lIiwibWFpbGluZ0FkZHJlc3MiXX0sInZlcnNpb24iOiIxLjAifQ.K9Syv2CrhcZ2cdizfXggDMnUkP6BxE2lMfP2ORvE5wZkPeHKsdbNhf6WaAe22eBqIODlxUp1a9WwxFYdeZ7kAA",
        pk,
        KeyType.ED25519,
        ArcJWT.DEFAULT_JWT_VERSION
      )
    )

    val token1 =
      ".eyJhY3Rpb24iOiJyZXNwb25zZUF1dGgiLCJhcHBJbmZvIjp7ImRlc2NyaXB0aW9uIjoiQXJjQmxvY2sgUmV2ZXJzZSBUb2tlbiBTd2FwIFNlcnZpY2UiLCJpY29uIjoiaHR0cDovLzE5Mi4xNjguMi40OjMwMDAvaW1hZ2VzL2xvZ28ucG5nIiwibGluayI6Imh0dHA6Ly8xOTIuMTY4LjIuNDozMDAwIiwibmFtZSI6IlJldmVyc2UgVG9rZW4gU3dhcCIsInBhdGgiOiJodHRwczovL2FidHdhbGxldC5pby9pLyIsInB1Ymxpc2hlciI6ImRpZDphYnQ6MHg3MWQ3NTg4OWNiZTg4MmNGYTc0N0I0QkJFOWEzNDkxNjM5OTFBRTQ1In0sImNoYWluSW5mbyI6eyJob3N0IjoiaHR0cHM6Ly9zdGFnaW5nLmFidC5hYnRuZXR3b3JrLmlvIiwiaWQiOiJ4ZW5vbi0yMDIwLTAxLTE1In0sImNoYWxsZW5nZSI6IjRGODNFM0IwQkU5OUQxQzE4NjVEOTMxNDIwOTc0Mjg5IiwiZXhwIjoiMTYxNTQ4MTYzNCIsImlhdCI6IjE2MTU0ODEzMzQiLCJpc3MiOiJkaWQ6YWJ0OjB4NzFkNzU4ODljYmU4ODJjRmE3NDdCNEJCRTlhMzQ5MTYzOTkxQUU0NSIsIm5iZiI6IjE2MTU0ODEzMzQiLCJyZXF1ZXN0ZWRDbGFpbXMiOlt7ImRlc2NyaXB0aW9uIjoiUGxlYXNlIHNlbGVjdCBhdXRoZW50aWNhdGlvbiBwcmluY2lwYWwiLCJtZXRhIjp7fSwidGFyZ2V0IjoiIiwidHlwZSI6ImF1dGhQcmluY2lwYWwifV0sInVybCI6Imh0dHA6Ly8xOTIuMTY4LjIuNDozMDAwL2FwaS9kaWQvbG9naW4vYXV0aD9fdF89NGQxNDc1ODAiLCJ2ZXJzaW9uIjoiMS4wIn0.MEUCIQCNcw_gV11kjVqBRcQNmKBGbPz9Lde7WrrOhpFIXYGyDwIgGJezOmfFali940l3yx0lVdAKFrGkGw3s-LueAmSc-8c"
    val appPk1 =
      "z3DmL11f5TiTNmRDVCSMdsoPYWdM8N5b9rf8K3fi9sQuHnX84yBpfxj4B8m7VTcAeiRqWBLSwAyvruZiNxZqLaGyt".decodeB58()

    assert(
      ArcJWT.verifyJWT(
        token1,
        appPk1,
        KeyType.ETHEREUM,
        ArcJWT.DEFAULT_JWT_VERSION
      )
    )
  }

  @Test
  fun testVerifyETHFromJS() {
    val messageUtf8 = "abt to the moon! haha"
    val pk =
      "50863AD64A87AE8A2FE83C1AF1A8403CB53F53E486D8511DAD8A04887E5B23522CD470243453A299FA9E77237716103ABC11A1DF38855ED6F2EE187E9C582BA6"
    val signatureUtf8 =
      "3045022100B09D7A812785CBAFE3B1C4EE1F9675F5DE8A6D6C167B31F0EF4DFAC16AC2B35802203E5E9CE3221EFB3079EACC8E9FC3BFE6F78A274242D557669AB4EC767572F588"

    val sk = "18E14A7B6A307F426A94F8114701E7C8E774E7F9A47E2C2035DB29A206321725"
    val pkFromSK = BaseEncoding.base16()
      .encode(IdGenerator.sk2pk(KeyType.ETHEREUM, BaseEncoding.base16().decode(sk)))
    Assert.assertEquals(pk, pkFromSK)

    val sigFromSk = BaseEncoding.base16().encode(
      Signer.sign(
        KeyType.ETHEREUM,
        messageUtf8.toByteArray(),
        BaseEncoding.base16().decode(sk)
      )
    )
    Assert.assertEquals(signatureUtf8, sigFromSk)
    val result = Signer.verify(
      KeyType.ETHEREUM,
      messageUtf8.toByteArray(),
      BaseEncoding.base16().decode(pk),
      BaseEncoding.base16().decode(signatureUtf8)
    )
    assert(result)
  }

  @Test
  fun testVerifyETHFromAndroid() {
    val messageUtf8 = "abt to the moon! haha"
    // sk - 32
    val sk = "4A44A535370B47518ED67E0E77C7C7EB968E5DE3BB00370B5E99921E506F2CC3".decodeB16()
    // pk - 64
    val pk =
      "7D6F080329CAB372A8008B19B32D0E5D2138B6E46161EECFDC439D9A2BDBE6EF386017FE07A55D9AA501F0A525AD02AF14E0C09EF8375404B5B69A7C70A99972".decodeB16()
    val sig = Signer.sign(KeyType.ETHEREUM, messageUtf8.toByteArray(), sk)
    assert(Signer.verify(KeyType.ETHEREUM, messageUtf8.toByteArray(), pk, sig))

    val sk2 = "6235C0D56B8E979640CC96629CB3E5B8C4D25C42E13FC92097F1AEE2FB118606".decodeB16()
    val pk2 =
      "00FA17A7CE0401F031CBD87FA824FF3FD6B63034951DDBD2D1536AEC3BF7382A3D328993ED68C9B5C5D2858C0558330711E0CCBCCAA318F8E1E494DCF397905D13".decodeB16()
    val sig2 = Signer.sign(KeyType.ETHEREUM, messageUtf8.toByteArray(), sk2)
    assert(Signer.verify(KeyType.ETHEREUM, messageUtf8.toByteArray(), pk2, sig2))
  }

  @Test
  fun test(){
    val msg = "65794A68624763694F694A4656456846556B565654534973496E523563434936496B705856434A392E65794A6C654841694F6949784E6A45314E5455794E7A59354969776961574630496A6F694D5459784E5455314D6A51324F534973496D6C7A63794936496A42344D7A6B344E7A56474D544D315247526A59544A6C4D54686C4D3251784E6A6335596B4E684E6A41314F5445354F4749774D4746424D694973496D35695A694936496A45324D5455314E5449304E546B694C434A795A5846315A584E305A5752446247467062584D694F6C74644C434A7A5A584E7A61573975535751694F6949696651".decodeB16()
    val pk = "z3vgraBB9AfCjCexFWY8oztu6rdu3Wa9gpVQxJVzoLLo35FZaD9RkBcSp958iqxpFugiVcWeFe6Jf9U8p8GAh3gpT".decodeB58()
    val sig = "490D2A5EB22C744877042DFC9159B60A2E30AC0CEE22E5787C653A05F63C05A74CA2A1DD8313EA1021FBC58BCECEE5D0D8754CBD9CD8235282BDDF07A8156F08".decodeB16()
    val result = Signer.verify(KeyType.ETHEREUM, msg, pk, sig)
    println("result:$result")
  }

  @Test
  fun testVerifyED25519() {
    val messageUtf8 = "this is a message !587++sdF"
    val signatureUtf8 =
      "4C6CC2FEFEB4CE33FA88684AACEDF653A4F33621890055EAACA82C5952C407CF26EB786B6330AFE7A09FA725B7DFEBF792B24AC93A8E1A447BE029E45762390F"
    val pk =
      "E4852B7091317E3622068E62A5127D1FB0D4AE2FC50213295E10652D2F0ABFC7"

    val result = Signer.verify(
      KeyType.ED25519,
      messageUtf8.toByteArray(),
      BaseEncoding.base16().decode(pk),
      BaseEncoding.base16().decode(signatureUtf8)
    )

    assert(result)
  }

  @Test
  fun testVerifySECP256K1() {
    val messageUtf8 = "abt to the moon! haha"
    val signatureUtf8 =
      "3045022100B09D7A812785CBAFE3B1C4EE1F9675F5DE8A6D6C167B31F0EF4DFAC16AC2B35802203E5E9CE3221EFB3079EACC8E9FC3BFE6F78A274242D557669AB4EC767572F588"
    val pk =
      "0450863AD64A87AE8A2FE83C1AF1A8403CB53F53E486D8511DAD8A04887E5B23522CD470243453A299FA9E77237716103ABC11A1DF38855ED6F2EE187E9C582BA6"
    val result = Signer.verify(
      KeyType.SECP256K1,
      messageUtf8.toByteArray(),
      BaseEncoding.base16().decode(pk),
      BaseEncoding.base16().decode(signatureUtf8)
    )
    assert(result)
  }

  @Test
  fun testDigest() {
    val digest = "z9RimajgDBmU7PRbqH8ztAwmcpBLAgcZLXDpTdMN9Rtc3".decodeB58().toHexString()
    println("digest:$digest")
  }
}
