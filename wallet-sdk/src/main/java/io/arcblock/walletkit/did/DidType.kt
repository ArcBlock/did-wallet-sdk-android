package io.arcblock.walletkit.did

/**
 * Created by Nate on 3/10/21
 */
data class DidType(
  var roleType: RoleType = RoleType.ACCOUNT,
  var keyType: KeyType = KeyType.ED25519,
  var hashType: HashType = HashType.SHA3,
  var encodingType: EncodingType = EncodingType.BASE58
) {

  override fun toString(): String {
    return "RoleType: ${roleType.name}\nKeyType: ${keyType.name}\nHashType: ${hashType.name}\nEncodingType: ${encodingType.name}"
  }

  companion object {
    val DID_TYPE_FORGE =
      DidType(RoleType.ACCOUNT, KeyType.ED25519, HashType.SHA3, EncodingType.BASE58)
    val DID_TYPE_FORGE_DELEGATE =
      DidType(RoleType.DELEGATE, KeyType.ED25519, HashType.SHA3, EncodingType.BASE58)
    val DID_TYPE_FORGE_TETHER =
      DidType(RoleType.TETHER, KeyType.ED25519, HashType.SHA2, EncodingType.BASE58)
    val DID_TYPE_FORGE_VALIDATOR =
      DidType(RoleType.VALIDATOR, KeyType.ED25519, HashType.SHA2, EncodingType.BASE58)
    val DID_TYPE_FORGE_NODE =
      DidType(RoleType.NODE, KeyType.ED25519, HashType.SHA2, EncodingType.BASE58)
    val DID_TYPE_FORGE_SWAP =
      DidType(RoleType.SWAP, KeyType.ED25519, HashType.SHA2, EncodingType.BASE58)
    val DID_TYPE_FORGE_STAKE =
      DidType(RoleType.STAKE, KeyType.ED25519, HashType.SHA3, EncodingType.BASE58)
    val DID_TYPE_FORGE_TX =
      DidType(RoleType.TX, KeyType.ED25519, HashType.SHA3, EncodingType.BASE58)
    val DID_TYPE_ETHEREUM =
      DidType(RoleType.ACCOUNT, KeyType.ETHEREUM, HashType.KECCAK, EncodingType.BASE16)

    /**
     * Given a DID, return the type of the DID
     *
     * @param did The DID to be parsed.
     * @return The return type is a `DidType` object.
     */
    fun getDidTypeByAddress(did: String): DidType {
      if (did.isEmpty()) {
        return DID_TYPE_FORGE
      }
      return try {
        if (DidUtils.isETH(did)) {
          DID_TYPE_ETHEREUM
        } else {
          DidType(
            DidUtils.decodeDidRoleType(did),
            DidUtils.decodeDidSignType(did),
            DidUtils.decodeDidHashType(did),
            DidUtils.decodeDidEncodingType(did)
          )
        }
      } catch (e: Exception) {
        DID_TYPE_FORGE
      }
    }
  }
}
