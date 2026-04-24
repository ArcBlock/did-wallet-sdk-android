package io.arcblock.tx_codec

import ocap.Type.Transaction
import ocap.Tx.TransferV2Tx
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

  // ---- protobuf round-trip ---------------------------------------------

  @Test
  fun `protobuf bytes round-trip through decode then encode`() {
    val tx = buildSampleTransferV2()
    val original = tx.toByteArray()
    val decoded = TxCodec.decode(original)
    assertEquals(Encoding.PROTOBUF, decoded.encoding)
    val reEncoded = TxCodec.encode(decoded.tx, Encoding.PROTOBUF)
    assertArrayEquals(original, reEncoded)
  }

  // ---- CBOR round-trip -------------------------------------------------

  @Test
  fun `CBOR bytes round-trip through decode then encode`() {
    val tx = buildSampleTransferV2()
    val cborBytes = TxCodec.encode(tx, Encoding.CBOR)
    // Self-describe tag 55799 prefix present
    assertEquals(Encoding.CBOR, TxCodec.detectEncoding(cborBytes))
    val decoded = TxCodec.decode(cborBytes)
    assertEquals(Encoding.CBOR, decoded.encoding)
    // Re-encode produces identical bytes (canonical form is deterministic)
    val reEncoded = TxCodec.encode(decoded, decoded.tx)
    assertArrayEquals(cborBytes, reEncoded)
  }

  @Test
  fun `CBOR decode preserves transaction top-level fields`() {
    val tx = buildSampleTransferV2()
    val cbor = TxCodec.encode(tx, Encoding.CBOR)
    val decoded = TxCodec.decode(cbor)
    assertEquals(tx.from, decoded.tx.from)
    assertEquals(tx.nonce, decoded.tx.nonce)
    assertEquals(tx.chainId, decoded.tx.chainId)
    assertEquals(tx.pk, decoded.tx.pk)
    assertEquals(tx.itx.typeUrl, decoded.tx.itx.typeUrl)
  }

  @Test
  fun `CBOR decode preserves the transfer inner tx value`() {
    val tx = buildSampleTransferV2()
    val cbor = TxCodec.encode(tx, Encoding.CBOR)
    val decoded = TxCodec.decode(cbor)
    val innerBefore = TransferV2Tx.parseFrom(tx.itx.value)
    val innerAfter = TransferV2Tx.parseFrom(decoded.tx.itx.value)
    assertEquals(innerBefore.to, innerAfter.to)
    assertEquals(innerBefore.value.value, innerAfter.value.value)
  }

  // ---- helpers ---------------------------------------------------------

  private fun buildSampleTransferV2(): Transaction {
    val itxInner = TransferV2Tx.newBuilder()
      .setTo("z1djzQ7tYaSC2E183dxFMFScriZgvsrhQD1")
      .setValue(
        ocap.Type.BigUint.newBuilder()
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
