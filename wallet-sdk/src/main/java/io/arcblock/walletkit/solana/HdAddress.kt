package io.arcblock.walletkit.solana

import io.arcblock.walletkit.solana.key.HdPrivateKey
import io.arcblock.walletkit.solana.key.HdPublicKey

/**
 * An HD pub/private key
 */
class HdAddress(
    val privateKey: HdPrivateKey,
    val publicKey: HdPublicKey,
    val coinType: SolanaCoin,
    val path: String
)