package io.arcblock.walletkit.did

/**
 * Author       :paperhuang
 * Time         :2019/2/15
 * Edited By    :
 * Edited Time  :
 **/
enum class KeyType(val value: Int) {
  ED25519(0), SECP256K1(1), ETHEREUM(2), PASSKEY(3)
}
