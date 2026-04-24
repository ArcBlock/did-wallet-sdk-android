package io.arcblock.tx_codec

import com.google.protobuf.ByteString
import io.arcblock.canonical_cbor.CanonicalCbor
import io.arcblock.tx_codec.internal.MapToTransaction
import io.arcblock.tx_codec.internal.TransactionToMap
import ocap.Type.Transaction

/**
 * OCAP Transaction codec with dual-encoding support.
 *
 * Decodes inbound tx bytes (CBOR or protobuf), exposes the result as a
 * [Transaction] so existing code paths remain unchanged, and re-encodes
 * back to the original wire format for outbound responses. The
 * [DecodedTx.encoding] field threads the inbound format through the
 * wallet's signing pipeline so `finalTx` bytes match what the DApp
 * expects to verify.
 *
 * Mirrors the responsibilities of `abt-wallet/src/libs/chain/tx-codec.ts`
 * for the Android client.
 */
object TxCodec {

  /**
   * RFC 8949 self-describe tag 55799. First 3 bytes of every canonical
   * CBOR Transaction. Used to distinguish CBOR from protobuf on inbound
   * bytes.
   */
  private val CBOR_PREFIX: ByteArray = CanonicalCbor.SELF_DESCRIBE_PREFIX

  /** Detect the inbound encoding by inspecting the first 3 bytes. */
  @JvmStatic
  fun detectEncoding(bytes: ByteArray): Encoding =
    if (bytes.size >= 3 &&
      bytes[0] == CBOR_PREFIX[0] &&
      bytes[1] == CBOR_PREFIX[1] &&
      bytes[2] == CBOR_PREFIX[2]
    ) Encoding.CBOR else Encoding.PROTOBUF

  /**
   * Decode inbound tx [bytes]. Auto-detects encoding. The returned
   * [Transaction] is a standard protobuf instance regardless of the
   * inbound encoding, so downstream access (`tx.from`, `tx.itx.typeUrl`,
   * `tx.signaturesList`, etc.) works without branching.
   */
  @JvmStatic
  fun decode(bytes: ByteArray): DecodedTx {
    val encoding = detectEncoding(bytes)
    val tx = when (encoding) {
      Encoding.CBOR -> {
        val map = CanonicalCbor.parseCanonical("Transaction", bytes)
        MapToTransaction.build(map)
      }
      Encoding.PROTOBUF -> Transaction.parseFrom(bytes)
    }
    return DecodedTx(tx, encoding)
  }

  /**
   * Encode [tx] to wire bytes in the specified [encoding]. Callers should
   * use the same encoding that came in (from [decode]'s result).
   */
  @JvmStatic
  fun encode(tx: Transaction, encoding: Encoding): ByteArray =
    when (encoding) {
      Encoding.CBOR -> {
        val map = TransactionToMap.convert(tx)
        CanonicalCbor.canonicalBytes("Transaction", map)
      }
      Encoding.PROTOBUF -> tx.toByteArray()
    }

  /**
   * Convenience: encode using the same encoding the [DecodedTx] was
   * produced with. Use this in call sites where you already have a
   * [DecodedTx] but mutated the inner [Transaction] before re-encoding.
   */
  @JvmStatic
  fun encode(decoded: DecodedTx, tx: Transaction = decoded.tx): ByteArray =
    encode(tx, decoded.encoding)

  // Package-internal re-export of canonical-cbor's self-describe prefix
  // for tests that want to inspect the byte layout.
  internal val SELF_DESCRIBE_PREFIX: ByteArray get() = CBOR_PREFIX

  // Prevent accidental javac complaints about the unused import on some
  // toolchains that strip it when Kotlin-only code compiles.
  @Suppress("unused")
  private val byteStringMarker: Class<*> = ByteString::class.java
}
