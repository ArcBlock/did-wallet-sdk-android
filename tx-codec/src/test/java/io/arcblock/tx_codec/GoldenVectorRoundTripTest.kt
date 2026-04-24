package io.arcblock.tx_codec

import org.junit.Assert.assertArrayEquals
import org.junit.Test

/**
 * Verifies the reflection-based Map ↔ Transaction bridge produces a
 * bit-perfect round-trip for every canonical CBOR golden vector.
 *
 * Stronger than the canonical-cbor module's own byte-exact encode test
 * because it exercises the additional step of reconstructing a
 * protobuf Transaction from the decoded map, and then disassembling it
 * back into the same map shape. Any asymmetry in field naming, default
 * folding, Any unwrap, or BigInt coercion would surface here.
 */
class GoldenVectorRoundTripTest {

  private fun verify(name: String) {
    val stream = javaClass.getResourceAsStream("/vectors/$name.cbor.bin")
      ?: error("missing /vectors/$name.cbor.bin")
    val original = stream.use { it.readBytes() }

    assertArrayEquals(
      "$name: self-describe tag 55799 prefix missing on golden input",
      io.arcblock.canonical_cbor.CanonicalCbor.SELF_DESCRIBE_PREFIX,
      original.copyOf(3)
    )

    // CBOR -> protobuf -> CBOR round-trip exercises both reflection bridges.
    val protoBytes = TxCodec.toProtobuf(original)
    val reEncoded = TxCodec.toEncoding(protoBytes, Encoding.CBOR)
    assertArrayEquals(
      "$name: TxCodec round-trip differs from golden bytes",
      original,
      reEncoded
    )
  }

  // Note: blockchain baselines `transfer_v2`, `declare_tx`, `consume_asset`,
  // `acquire_asset_v2` are NOT Transaction-level payloads — they are the
  // inner tx types directly (TransferV2Tx, DeclareTx, etc.). TxCodec
  // expects Transaction bytes, so those fixtures belong to
  // canonical-cbor's GoldenVectorTest, not here.
  @Test fun transaction_full() = verify("transaction_full")

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
