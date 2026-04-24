package io.arcblock.canonical_cbor

import org.junit.Assert.assertArrayEquals
import org.junit.Test

/**
 * Byte-exact encode verification against the 15 golden fixtures. Any
 * mismatch here means the Kotlin encoder diverges from the TypeScript
 * reference at some byte position -> signature verification will break
 * in production.
 *
 * Debugging tips for a failing vector:
 *  - xxd the .cbor.bin to see expected layout
 *  - print the actual bytes as hex and diff manually
 *  - Most likely causes (ordered): map key order (sort by id!),
 *    BigUint leading-zero stripping, default-value folding edge case,
 *    Any wrapper type-field stripping.
 */
class GoldenVectorTest {

  private fun verify(name: String) {
    val fx = FixtureLoader.load(name)
    val actual = CanonicalCbor.canonicalBytes(fx.type, fx.data)
    assertArrayEquals(
      "$name: encoded bytes differ from golden vector",
      fx.cborBytes,
      actual
    )
  }

  // ---- 5 upstream blockchain fixtures ----------------------------------

  @Test fun transfer_v2() = verify("transfer_v2")
  @Test fun declare_tx() = verify("declare_tx")
  @Test fun consume_asset() = verify("consume_asset")
  @Test fun acquire_asset_v2() = verify("acquire_asset_v2")
  @Test fun transaction_full() = verify("transaction_full")

  // ---- 10 wallet-specific fixtures -------------------------------------

  @Test fun wallet_transfer_v2() = verify("wallet_transfer_v2")
  @Test fun wallet_transfer_v2_signed() = verify("wallet_transfer_v2_signed")
  @Test fun wallet_transfer_v3_single_input() = verify("wallet_transfer_v3_single_input")
  @Test fun wallet_transfer_v3_multi_input() = verify("wallet_transfer_v3_multi_input")
  @Test fun wallet_acquire_asset_v3() = verify("wallet_acquire_asset_v3")
  @Test fun wallet_exchange_v2_multisig() = verify("wallet_exchange_v2_multisig")
  @Test fun wallet_stake_tx() = verify("wallet_stake_tx")
  @Test fun wallet_delegate_tx() = verify("wallet_delegate_tx")
  @Test fun wallet_revoke_delegate_tx() = verify("wallet_revoke_delegate_tx")
  @Test fun wallet_account_migrate_tx() = verify("wallet_account_migrate_tx")
}
