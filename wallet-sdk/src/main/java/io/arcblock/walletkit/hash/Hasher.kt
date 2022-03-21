package io.arcblock.walletkit.hash

import io.arcblock.walletkit.did.HashType
import io.arcblock.walletkit.did.HashUtils

/**
 *  Calculate Hash
 */
object Hasher {

  /**
   * Calculate Hash
   * @param hashType enum class HashType
   * @param contents hash content
   * @return result
   */
  fun hash(hashType: HashType, contents: ByteArray, round: Int?= null): ByteArray {
    return when (hashType) {
      HashType.SHA3 -> {
        ArcSha3Hasher.sha256(contents, 1)
      }
      HashType.KECCAK -> {
        ArcKeccakf1600Hasher.sha256(contents,round ?:1)
      }
      HashType.KECCAK_384 -> {
        ArcKeccakf1600Hasher.sha384(contents,round ?:1)
      }
      HashType.KECCAK_512 -> {
        ArcKeccakf1600Hasher.sha512(contents,round?:1)
      }
      HashType.SHA3_384 -> {
        ArcKeccakf1600Hasher.sha384(contents ,round?:1)
      }
      HashType.SHA3_512 -> {
        ArcKeccakf1600Hasher.sha512(contents,round?:1)
      }
      HashType.SHA2 ->{
        ArcSha2Hasher.sha256(contents,round ?:1)
      }
      HashType.NONE -> {
        contents
      }
    }
  }
}
