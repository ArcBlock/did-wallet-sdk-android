package io.arcblock.walletkit.did

import com.google.common.io.BaseEncoding
import io.arcblock.walletkit.did.RoleType.ERROR
import io.arcblock.walletkit.utils.Base58Btc
import io.arcblock.walletkit.utils.address
import io.arcblock.walletkit.utils.decodeB58
import io.arcblock.walletkit.utils.hash
import org.bitcoinj.core.ECKey.CURVE
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERSequenceGenerator
import org.bouncycastle.asn1.DLSequence
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.web3j.crypto.ECDSASignature
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.WalletUtils
import org.web3j.utils.Numeric
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*


/**
 * Author       :paperhuang
 * Time         :2019/2/14
 * Edited By    :
 * Edited Time  :
 **/

object DidUtils {
  fun decodeFromDER(bytes: ByteArray): ECDSASignature {
    var decoder: ASN1InputStream? = null
    try {
      decoder = ASN1InputStream(bytes)
      val seq = decoder.readObject() as DLSequence
      val r: ASN1Integer
      val s: ASN1Integer
      try {
        r = seq.getObjectAt(0) as ASN1Integer
        s = seq.getObjectAt(1) as ASN1Integer
      } catch (e: ClassCastException) {
        throw IllegalArgumentException(e)
      }

      // OpenSSL deviates from the DER spec by interpreting these values as unsigned, though they should not be
      // Thus, we always use the positive versions. See: http://r6.ca/blog/20111119T211504Z.html
      return ECDSASignature(r.positiveValue, s.positiveValue)
    } catch (e: IOException) {
      throw RuntimeException(e)
    } finally {
      if (decoder != null) try {
        decoder.close()
      } catch (x: IOException) {
      }

    }
  }

  fun verify(data: ByteArray, signature: ByteArray, pk: ByteArray): Boolean {
    val signer = ECDSASigner()
    val params = ECPublicKeyParameters(CURVE.curve.decodePoint(pk), CURVE)
    signer.init(false, params)
    val sig = DidUtils.decodeFromDER(signature)
    return signer.verifySignature(data, sig.r, sig.s)
  }

  fun formatHexString(hexString: String): String {
    return (hexString.removePrefix("0x")).toUpperCase(Locale.ROOT)
  }

  /**
   * decodeDidRoleType
   * @param did DID address no matter did:abt:xxx or zXXXXXX
   * @return RoleType
   */
  fun decodeDidRoleType(did: String): RoleType {
    val address = did.address()

    if (did.isEmpty()) {
      return RoleType.ERROR
    }

    if (isETH(address)) {
      return RoleType.ACCOUNT
    }
    try {
      return when (decodeDidEncodingType(address)) {
        EncodingType.BASE16 -> decodeDidRoleType(
          BaseEncoding.base16().decode(formatHexString(address))
        )
        EncodingType.BASE58 -> decodeDidRoleType(Base58Btc.decode(address))
      }
    }catch (e: Exception) {
      return RoleType.ERROR
    }
  }

  /**
   * get HashType from your DID address
   * @param did DID address no matter did:abt:xxx or zXXXXXX
   */
  fun decodeDidHashType(did: String): HashType {
    val address = did.address()

    if (did.isEmpty()) {
      return HashType.SHA3
    }

    if (isETH(address)) {
      return HashType.KECCAK
    }

    return when (decodeDidEncodingType(address)) {
      EncodingType.BASE16 -> decodeDidHashType(
        BaseEncoding.base16().decode(formatHexString(address))
      )
      EncodingType.BASE58 -> decodeDidHashType(Base58Btc.decode(address))
    }
  }

  /**
   * get SignatureType from your DID address
   * @param did DID address no matter did:abt:xxx or zXXXXXX
   */
  fun decodeDidSignType(did: String): KeyType {
    val address = did.address()

    if (did.isEmpty()) {
      return KeyType.ED25519
    }

    if (isETH(address)) {
      return KeyType.ETHEREUM
    }

    return when (decodeDidEncodingType(address)) {
      EncodingType.BASE16 -> decodeDidSignType(
        BaseEncoding.base16().decode(formatHexString(address))
      )
      EncodingType.BASE58 -> decodeDidSignType(Base58Btc.decode(address))
    }
  }

  /**
   * get RoleType from your DID address
   * @param did DID binary :base58Btc decode from zXXXXXX
   */
  private fun decodeDidRoleType(did: ByteArray): RoleType {
    if (did.size < 2) return ERROR
    val type = did.sliceArray(0..1)
    val x = type[0].toInt().shl(8) + type[1].toInt()
    return RoleType.values()[x.and(0b1111110000000000).shr(10)]
  }

  /**
   * get Hash type from your DID address
   * @param did DID binary :base58Btc decode from zXXXXXX
   *
   */
  private fun decodeDidHashType(did: ByteArray): HashType {
    val type = did.sliceArray(0..1)
    val x = type[0].toInt().shl(8) + type[1].toInt()
    return HashType.values()[x.and(0b11111)]
  }

  /**
   * get SignatureType from your DID address
   * @param did DID binary :base58Btc decode from zXXXXXX
   *
   */
  private fun decodeDidSignType(did: ByteArray): KeyType {
    val type = did.sliceArray(0..1)
    val x = type[0].toInt().shl(8) + type[1].toInt()
    return KeyType.values()[x.and(0b0000001111100000).shr(5)]
  }

  /**
   * Given a public key, return the type of the key
   *
   * @param pk the public key of the signer
   * @return The KeyType of the public key.
   */
  fun decodeSignTypeByPk(pk: ByteArray): KeyType {
    return decodeDidSignType(pk)
  }

  /**
   * get EncodingType from your DID address
   * @param did DID string: zXXXXXX
   *
   */
  fun decodeDidEncodingType(did: String): EncodingType {
    val address = did.address()

    if (did.isEmpty()) {
      return EncodingType.BASE58
    }

    return if (address.startsWith("z")) {
      EncodingType.BASE58
    } else {
      EncodingType.BASE16
    }
  }


  /**
   * It checks if the address is a valid Ethereum address.
   *
   * @param address The address to check.
   * @return Nothing.
   */
  fun isETH(address: String): Boolean {
    try {
      if (address.isEmpty()) {
        return false
      }
      if (WalletUtils.isValidAddress(Keys.toChecksumAddress(address.address()))) {
        return true
      }
      return false
    } catch (e: Exception) {
      return false
    }
  }

  /**
   * `return isETH(address) || address.isValidDID()`
   *
   * The `||` operator is a short-circuit operator. It only evaluates the second argument if the first argument is `false`
   *
   * @param address The address to check.
   * @return Nothing.
   */
  private fun isValid(address: String): Boolean {
    return isETH(address) || address.isValidDID()
  }

  /**
   * Given a DID and a public key, return true if the public key is the public key of the DID
   *
   * @param did The DID to check.
   * @param pk The public key of the DID.
   * @return Boolean
   */
  fun isFromPublicKey(did: String, pk: ByteArray): Boolean {
    val address = did.address()
    if (!isValid(address)) {
      return false
    }
    val newAddress = IdGenerator.pk2Address(pk, DidType.getDidTypeByAddress(address))
    if (isETH(address)) {
      return newAddress == Keys.toChecksumAddress(address)
    } else {
      return newAddress == address.address()
    }
  }
}

fun ByteArray.toHexString(): String {
  return Numeric.toHexStringNoPrefix(this)
}

fun ECDSASignature.encodeToDER(): ByteArray {
  val bos = ByteArrayOutputStream(72)
  val seq = DERSequenceGenerator(bos)
  seq.addObject(ASN1Integer(r))
  seq.addObject(ASN1Integer(s))
  seq.close()
  return bos.toByteArray()
}

//compress 66 uncompressed 130
fun ECKeyPair.getFixedPK(): ByteArray {
  var ret = publicKey.toByteArray()
  if (ret.size == 64) {
    // 64 -> add a 4 element before the byte array
    ret = byteArrayOf(4).plus(ret)
  } else {
    // 65 -> just replace the first element
    ret[0] = 4
  }
  return ret
}

private val CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1")
val CURVE = ECDomainParameters(CURVE_PARAMS.curve, CURVE_PARAMS.g, CURVE_PARAMS.n, CURVE_PARAMS.h)

fun String.isValidDID(): Boolean {
  return try {
    val hashType = DidUtils.decodeDidHashType(this)
    val hashContent = this.address().decodeB58().sliceArray(0..21)
    val check = this.address().decodeB58().sliceArray(22..25)
    check.contentEquals(hashContent.hash(hashType).sliceArray(0..3))
  } catch (e: Exception) {
    false
  }
}



