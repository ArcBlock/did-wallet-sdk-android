package io.arcblock.walletkit.signer

import com.google.common.io.BaseEncoding
import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.Ed25519Verify
import io.arcblock.walletkit.did.DidUtils
import io.arcblock.walletkit.did.KeyType
import io.arcblock.walletkit.did.KeyType.*
import io.arcblock.walletkit.did.encodeToDER
import org.web3j.crypto.ECKeyPair

/**
 * Author       :paperhuang
 * Time         :2019/2/19
 * Edited By    :
 * Edited Time  :
 **/
object Signer {

  /**
   * It takes a key type and a message, and returns a signature
   *
   * @param keyType The type of key to use for signing.
   * @param content The data to be signed.
   * @param sk the private key
   * @return The signature of the content.
   */
  fun sign(keyType: KeyType, content: ByteArray, sk: ByteArray): ByteArray {
    return when (keyType) {
      ED25519 -> {
        Ed25519Sign(sk.sliceArray(0..31)).sign(content)
      }
      SECP256K1 -> {
        ECKeyPair.create(sk).sign(content).encodeToDER()
      }
      ETHEREUM -> {
        ECKeyPair.create(sk).sign(content).encodeToDER()
      }
      PASSKEY -> {
        // not support
        ByteArray(0)
      }
    }
  }

  /**
   * It verifies the signature of the content using the public key.
   *
   * @param keyType The type of key to use for signing.
   * @param content The data to be signed.
   * @param pk The public key of the signer.
   * @param signature The signature to verify.
   * @return Boolean
   */
  fun verify(keyType: KeyType, content: ByteArray, pk: ByteArray, signature: ByteArray): Boolean {
    try {
      return when (keyType) {
        ED25519 -> {
          Ed25519Verify(pk).verify(signature, content)
          true
        }
        SECP256K1 -> {
          return DidUtils.verify(content, signature, pk)
        }
        ETHEREUM -> {
          return if (pk.size == 64) {
            DidUtils.verify(content, signature, byteArrayOf(4).plus(pk))
          } else {
            DidUtils.verify(content, signature, pk.let {
              it[0] = 4
              it
            })
          }
        }
        PASSKEY -> {
          return false
        }
      }
    } catch (e: Exception) {
      return false
    }
  }
}
