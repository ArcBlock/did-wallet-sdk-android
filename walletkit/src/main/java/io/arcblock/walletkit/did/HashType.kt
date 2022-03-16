package io.arcblock.walletkit.did

enum class HashType(val value: Int) {
  KECCAK(0),
  SHA3(1),
  KECCAK_384(2),
  SHA3_384(3),
  KECCAK_512(4),
  SHA3_512(5),
  SHA2(6),
  NONE(999)
}
