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
