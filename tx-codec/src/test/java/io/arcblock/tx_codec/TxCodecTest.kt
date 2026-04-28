package io.arcblock.tx_codec

import io.arcblock.tx_codec.generated.Type.Transaction
import io.arcblock.tx_codec.generated.Tx.TransferV2Tx
import com.google.protobuf.ByteString
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class TxCodecTest {

  // ---- encoding detection ----------------------------------------------

  @Test
  fun `detectEncoding spots CBOR tag 55799 prefix`() {
    val cbor = byteArrayOf(0xd9.toByte(), 0xd9.toByte(), 0xf7.toByte(), 0xa0.toByte())
    assertEquals(Encoding.CBOR, TxCodec.detectEncoding(cbor))
  }

  @Test
  fun `detectEncoding falls back to PROTOBUF otherwise`() {
    val pb = byteArrayOf(0x08, 0x01)
    assertEquals(Encoding.PROTOBUF, TxCodec.detectEncoding(pb))
  }

  @Test
  fun `detectEncoding returns PROTOBUF for short input`() {
    assertEquals(Encoding.PROTOBUF, TxCodec.detectEncoding(byteArrayOf()))
    assertEquals(Encoding.PROTOBUF, TxCodec.detectEncoding(byteArrayOf(0xd9.toByte())))
  }

  // ---- identity paths --------------------------------------------------

  @Test
  fun `convert with same encoding returns input unchanged`() {
    val bytes = byteArrayOf(0x08, 0x01)
    assertEquals(bytes, TxCodec.convert(bytes, Encoding.PROTOBUF, Encoding.PROTOBUF))
  }

  // ---- protobuf round-trip ---------------------------------------------

  @Test
  fun `protobuf bytes survive a CBOR round-trip`() {
    val tx = buildSampleTransferV2()
    val original = tx.toByteArray()
    val cbor = TxCodec.convert(original, Encoding.PROTOBUF, Encoding.CBOR)
    assertEquals(Encoding.CBOR, TxCodec.detectEncoding(cbor))
    val back = TxCodec.convert(cbor, Encoding.CBOR, Encoding.PROTOBUF)
    assertArrayEquals(original, back)
  }

  // ---- toProtobuf / toEncoding shortcuts -------------------------------

  @Test
  fun `toProtobuf auto-detects CBOR and converts`() {
    val tx = buildSampleTransferV2()
    val cbor = TxCodec.convert(tx.toByteArray(), Encoding.PROTOBUF, Encoding.CBOR)
    val proto = TxCodec.toProtobuf(cbor)
    val roundTripped = Transaction.parseFrom(proto)
    assertEquals(tx.from, roundTripped.from)
    assertEquals(tx.itx.typeUrl, roundTripped.itx.typeUrl)
  }

  @Test
  fun `toProtobuf leaves protobuf input unchanged`() {
    val proto = buildSampleTransferV2().toByteArray()
    assertArrayEquals(proto, TxCodec.toProtobuf(proto))
  }

  @Test
  fun `toEncoding re-encodes to the requested outbound format`() {
    val proto = buildSampleTransferV2().toByteArray()
    val cbor = TxCodec.toEncoding(proto, Encoding.CBOR)
    assertEquals(Encoding.CBOR, TxCodec.detectEncoding(cbor))
    // Converting back yields the original bytes
    val back = TxCodec.toEncoding(TxCodec.toProtobuf(cbor), Encoding.PROTOBUF)
    assertArrayEquals(proto, back)
  }

  // ---- field preservation ----------------------------------------------

  @Test
  fun `CBOR round-trip preserves transaction top-level fields`() {
    val tx = buildSampleTransferV2()
    val cbor = TxCodec.toEncoding(tx.toByteArray(), Encoding.CBOR)
    val decoded = Transaction.parseFrom(TxCodec.toProtobuf(cbor))
    assertEquals(tx.from, decoded.from)
    assertEquals(tx.nonce, decoded.nonce)
    assertEquals(tx.chainId, decoded.chainId)
    assertEquals(tx.pk, decoded.pk)
    assertEquals(tx.itx.typeUrl, decoded.itx.typeUrl)
  }

  @Test
  fun `CBOR round-trip preserves the transfer inner tx value`() {
    val tx = buildSampleTransferV2()
    val cbor = TxCodec.toEncoding(tx.toByteArray(), Encoding.CBOR)
    val decoded = Transaction.parseFrom(TxCodec.toProtobuf(cbor))
    val innerBefore = TransferV2Tx.parseFrom(tx.itx.value)
    val innerAfter = TransferV2Tx.parseFrom(decoded.itx.value)
    assertEquals(innerBefore.to, innerAfter.to)
    assertEquals(innerBefore.value.value, innerAfter.value.value)
  }

  // ---- OPAQUE Any payload (json/vc/fg:x:address) -----------------------

  /**
   * payment-kit subscriptions ship `itx.data` as
   * `Any{typeUrl='json', value=<arbitrary CBOR map>}`. Confirms the
   * wallet's CBOR → protobuf → CBOR round-trip preserves every byte;
   * any drift here would silently break signature verification on the
   * dapp side because SHA3(finalTx) wouldn't match.
   */
  @Test
  fun `OPAQUE Any payload (typeUrl=json) survives full CBOR round-trip`() {
    val tx = mapOf(
      "from" to "z1WfGZHaLkv16upggvqBhPAT1UKZZvdKe1L",
      "nonce" to 1_717_171_717_171L,
      "chainId" to "beta",
      "pk" to hexToBytes(
        "1f3da92f9443ad4c789310c88d42e68f5439b3d86187de5de8ec90100614dff1"
      ),
      "itx" to mapOf(
        "typeUrl" to "fg:t:delegate",
        "address" to "z1DelegateAddressExample00000000000000",
        "to" to "z1cRPzp7te3W9tTMLrKJs4ss5U3JQ1TmPs3",
        "ops" to listOf(
          mapOf(
            "typeUrl" to "fg:t:transfer_v3",
            "rules" to listOf("itx.to == \"z1djzQ7tYaSC2E183dxFMFScriZgvsrhQD1\"")
          )
        ),
        "data" to mapOf(
          "typeUrl" to "json",
          "value" to mapOf(
            // Order matters: canonical-cbor sorts map keys length-then-lex,
            // so "limit" < "method" < "currency" by length, then
            // "currency" < "method" lexicographically. Building in this
            // order keeps the input intent obvious; the encoder will
            // re-sort canonically regardless.
            "limit" to 10_000L,
            "method" to "card",
            "currency" to "USD"
          )
        )
      )
    )
    val cbor = io.arcblock.canonical_cbor.CanonicalCbor.canonicalBytes(
      "Transaction", tx
    )
    val proto = TxCodec.toProtobuf(cbor)
    val backToCbor = TxCodec.toEncoding(proto, Encoding.CBOR)
    assertArrayEquals(
      "OPAQUE Any payload (typeUrl='json') didn't round-trip byte-exact",
      cbor, backToCbor
    )
  }

  /**
   * Forward-compatibility: an itx typeUrl this build doesn't recognise
   * must NOT crash on encode. MapToTransaction.buildAny falls back to
   * the same opaque-bytes carrier as TransactionToMap.anyToMap, so the
   * round-trip survives intact even when the dapp ships a new itx type.
   */
  @Test
  fun `unknown typeUrl falls back to opaque-bytes round-trip`() {
    val tx = mapOf(
      "from" to "z1WfGZHaLkv16upggvqBhPAT1UKZZvdKe1L",
      "nonce" to 1_717_171_717_171L,
      "chainId" to "beta",
      "pk" to hexToBytes(
        "1f3da92f9443ad4c789310c88d42e68f5439b3d86187de5de8ec90100614dff1"
      ),
      "itx" to mapOf(
        // A typeUrl this SDK build has no descriptor for. The Decoder
        // will preserve the inner CBOR bytes under "value"; buildAny
        // must accept them without throwing.
        "typeUrl" to "fg:t:hypothetical_future_op",
        "value" to byteArrayOf(0x01, 0x02, 0x03, 0x04)
      )
    )
    val cbor = io.arcblock.canonical_cbor.CanonicalCbor.canonicalBytes(
      "Transaction", tx
    )
    val proto = TxCodec.toProtobuf(cbor)
    val backToCbor = TxCodec.toEncoding(proto, Encoding.CBOR)
    assertArrayEquals(
      "Unknown typeUrl didn't round-trip byte-exact",
      cbor, backToCbor
    )
  }

  // ---- helpers ---------------------------------------------------------

  private fun buildSampleTransferV2(): Transaction {
    val itxInner = TransferV2Tx.newBuilder()
      .setTo("z1djzQ7tYaSC2E183dxFMFScriZgvsrhQD1")
      .setValue(
        io.arcblock.tx_codec.generated.Type.BigUint.newBuilder()
          .setValue(ByteString.copyFrom(hexToBytes("0de0b6b3a7640000"))) // 10^18
      )
      .build()
    val itxAny = com.google.protobuf.Any.newBuilder()
      .setTypeUrl("fg:t:transfer_v2")
      .setValue(itxInner.toByteString())
      .build()
    return Transaction.newBuilder()
      .setFrom("z1WfGZHaLkv16upggvqBhPAT1UKZZvdKe1L")
      .setNonce(1_717_171_717_171L)
      .setChainId("beta")
      .setPk(ByteString.copyFrom(
        hexToBytes("1f3da92f9443ad4c789310c88d42e68f5439b3d86187de5de8ec90100614dff1")
      ))
      .setItx(itxAny)
      .build()
  }

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
