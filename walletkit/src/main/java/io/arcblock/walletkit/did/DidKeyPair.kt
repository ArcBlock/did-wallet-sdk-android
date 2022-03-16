package io.arcblock.walletkit.did

import io.arcblock.walletkit.did.KeyType.*
import org.web3j.crypto.ECKeyPair
import org.web3j.utils.Numeric

/**
 * Author       :paperhuang
 * Time         :2019/2/19
 * Edited By    :
 * Edited Time  :
 **/
class DidKeyPair(keyType: KeyType, var privateKey: ByteArray) {

  var publicKey: ByteArray

  init {
    when (keyType) {
      ED25519 -> {
        // the sk first part is the seed, and the sk2pk only use (0,32) of the sk, so just pass the seed here
        publicKey = IdGenerator.sk2pk(keyType, privateKey)
      }
      SECP256K1 -> {
        ECKeyPair.create(privateKey).let {
          privateKey = Numeric.toBytesPadded(it.privateKey, 32)
          publicKey = IdGenerator.sk2pk(keyType, privateKey)
        }
      }
      ETHEREUM -> {
        ECKeyPair.create(privateKey).let {
          privateKey = Numeric.toBytesPadded(it.privateKey, 32)
          publicKey = Numeric.toBytesPadded(it.publicKey, 64)
        }
      }
    }
  }
}
