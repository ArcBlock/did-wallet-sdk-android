package io.arcblock.walletkit.bean

import io.arcblock.walletkit.did.*
import io.arcblock.walletkit.utils.Base58Btc
import io.arcblock.walletkit.utils.address

class WalletInfo(var address: String,val pk: ByteArray) {

  constructor(sk: ByteArray):this(IdGenerator.sk2did(sk).address(),IdGenerator.sk2pk(KeyType.ED25519,sk)){
    this.sk = sk
  }

  var sk: ByteArray? = null
  var token: String? = null

  fun getAccountType(): RoleType {
    return DidUtils.decodeDidRoleType(address)
  }

  fun getHashType(): HashType {
    return DidUtils.decodeDidHashType(address)
  }

  fun getSignType(): KeyType {
    return DidUtils.decodeDidSignType(address)
  }

  fun pkBase58(): String = Base58Btc.encode(pk)

}
