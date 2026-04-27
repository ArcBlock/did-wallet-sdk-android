package io.arcblock.tx_codec

import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Test

/**
 * Cross-encoder verification: feed the **JS-side protobuf bytes**
 * (captured by abt-wallet's `tools/cbor-fixture-generate.js` into each
 * fixture's `.meta.json`) into the Kotlin TxCodec and confirm the
 * resulting CBOR matches the JS-side `.cbor.bin` byte-for-byte.
 *
 * This is the strongest cross-language correctness signal we can get
 * without an actual on-the-wire test:
 *
 *   abt-wallet  (TypeScript createMessage)   →   protobufHex
 *                                                       ↓ (this test)
 *               Kotlin TxCodec.toEncoding(.., CBOR)     ↓
 *                                                       ↓
 *   abt-wallet  (TypeScript canonicalBytes)  →   cbor.bin   == ?
 *
 * If any byte differs, the JS protobuf encoder, the JS canonical CBOR
 * encoder, the Kotlin protobuf parser, the Kotlin Map↔Transaction
 * bridge, OR the Kotlin canonical CBOR encoder is misaligned. All three
 * pieces are independently exercised here in a single assertion per
 * fixture.
 */
class MetaCrossEncoderTest {

  private fun verify(name: String) {
    val metaText = javaClass.getResourceAsStream("/vectors/$name.meta.json")
      ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
      ?: error("missing /vectors/$name.meta.json")
    val meta = JSONObject(metaText)
    val protobuf = meta.optJSONObject("protobuf")
      ?: error("$name: meta.json missing `protobuf` block — fixture script may not have been re-run")
    if (protobuf.has("error")) {
      // protobuf encoding intentionally not produced for this fixture
      return
    }
    val protobufHex = protobuf.getString("hex")
    val protobufBytes = hexToBytes(protobufHex)

    val cborGoldenStream = javaClass.getResourceAsStream("/vectors/$name.cbor.bin")
      ?: error("missing /vectors/$name.cbor.bin")
    val cborGolden = cborGoldenStream.use { it.readBytes() }

    val cborFromKotlin = TxCodec.toEncoding(protobufBytes, Encoding.CBOR)
    assertArrayEquals(
      "$name: Kotlin CBOR encoding of JS-side protobuf bytes differs from JS golden",
      cborGolden,
      cborFromKotlin
    )
  }

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

  private fun hexToBytes(hex: String): ByteArray {
    val out = ByteArray(hex.length / 2)
    for (i in out.indices) {
      val hi = Character.digit(hex[i * 2], 16)
      val lo = Character.digit(hex[i * 2 + 1], 16)
      out[i] = ((hi shl 4) or lo).toByte()
    }
    return out
  }
}
