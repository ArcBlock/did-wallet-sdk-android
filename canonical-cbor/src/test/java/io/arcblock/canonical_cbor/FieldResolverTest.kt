package io.arcblock.canonical_cbor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldResolverTest {

  @Test
  fun `Transaction fields resolve with expected ids`() {
    val fields = FieldResolver.getFields("Transaction")
    assertNotNull(fields)
    // Canonical proto field ids (see blockchain/core/proto/src/type.proto)
    assertEquals(1, fields!!["from"]?.id)
    assertEquals("string", fields["from"]?.type)
    assertEquals(2, fields["nonce"]?.id)
    assertEquals("uint64", fields["nonce"]?.type)
    assertEquals(4, fields["pk"]?.id)
    assertEquals("bytes", fields["pk"]?.type)
    assertEquals("google.protobuf.Any", fields["itx"]?.type)
  }

  @Test
  fun `TransferV2Tx fields have BigUint value and repeated tokens`() {
    val fields = FieldResolver.getFields("TransferV2Tx")!!
    assertEquals("string", fields["to"]?.type)
    assertEquals("BigUint", fields["value"]?.type)
    assertEquals("repeated", fields["tokens"]?.rule)
    assertEquals("TokenInput", fields["tokens"]?.type)
  }

  @Test
  fun `unknown type returns null from getFields`() {
    assertNull(FieldResolver.getFields("NoSuchMessageType"))
  }

  @Test
  fun `typeUrl maps TransferV2Tx to fg_t_transfer_v2`() {
    assertEquals("fg:t:transfer_v2", FieldResolver.toTypeUrl("TransferV2Tx"))
  }

  @Test
  fun `typeUrl round-trips for common wallet tx types`() {
    val pairs = listOf(
      "TransferV2Tx" to "fg:t:transfer_v2",
      "TransferV3Tx" to "fg:t:transfer_v3",
      "AcquireAssetV3Tx" to "fg:t:acquire_asset_v3",
      "ExchangeV2Tx" to "fg:t:exchange_v2",
      "StakeTx" to "fg:t:stake",
      "DelegateTx" to "fg:t:delegate",
      "RevokeDelegateTx" to "fg:t:revoke_delegate",
      "AccountMigrateTx" to "fg:t:account_migrate",
      "WithdrawTokenV2Tx" to "fg:t:withdraw_token_v2"
    )
    for ((name, url) in pairs) {
      assertEquals("toTypeUrl($name)", url, FieldResolver.toTypeUrl(name))
      assertEquals("fromTypeUrl($url)", name, FieldResolver.fromTypeUrl(url))
    }
  }

  @Test
  fun `State types map to fg_s`() {
    assertEquals("fg:s:account", FieldResolver.toTypeUrl("AccountState"))
    assertEquals("fg:s:asset", FieldResolver.toTypeUrl("AssetState"))
  }

  @Test
  fun `explicit typeUrl overrides are respected`() {
    assertEquals("fg:x:asset_factory", FieldResolver.toTypeUrl("AssetFactory"))
    assertEquals("fg:s:asset_factory_state",
      FieldResolver.toTypeUrl("AssetFactoryState"))
    assertEquals("fg:x:address", FieldResolver.toTypeUrl("DummyCodec"))
    assertEquals("fg:x:transaction_info",
      FieldResolver.toTypeUrl("TransactionInfo"))
  }

  @Test
  fun `unknown typeUrl falls through unchanged`() {
    assertEquals("mime:unknown", FieldResolver.fromTypeUrl("mime:unknown"))
  }

  @Test
  fun `enum lookup returns numeric value`() {
    // KeyType / HashType / EncodingType exist in @ocap/proto/schema enums
    assertTrue(FieldResolver.isEnumType("KeyType"))
    // ED25519 = 0 per blockchain/core/proto/src/enum.proto
    assertEquals(0, FieldResolver.getEnumValue("KeyType", "ED25519"))
  }

  @Test
  fun `non-enum type returns false from isEnumType`() {
    assertFalse(FieldResolver.isEnumType("Transaction"))
    assertFalse(FieldResolver.isEnumType("NoSuchEnumHere"))
  }
}
