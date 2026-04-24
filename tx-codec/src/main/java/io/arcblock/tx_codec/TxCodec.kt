package io.arcblock.tx_codec

import io.arcblock.canonical_cbor.CanonicalCbor
import io.arcblock.tx_codec.generated.Type.Transaction
import io.arcblock.tx_codec.internal.MapToTransaction
import io.arcblock.tx_codec.internal.TransactionToMap

/**
 * OCAP Transaction codec with dual-encoding support.
 *
 * Public API is **bytes-first**: the caller's Transaction representation
 * (javalite, full-java, even a custom wire-compatible class) does not
 * interact with tx-codec's internal types. This avoids duplicate-class
 * issues on Android where the app usually uses javalite but tx-codec's
 * reflection internals require the full runtime.
 *
 * Typical use from a signing flow:
 *
 * ```kotlin
 * val oriBytes = Base58Btc.decode(claim.partialTx)
 * val inboundEncoding = TxCodec.detectEncoding(oriBytes)
 *
 * // Normalize to protobuf for the existing javalite-based signing path.
 * val protoBytes = TxCodec.toProtobuf(oriBytes)
 * val tx = Transaction.parseFrom(protoBytes)   // app's javalite class
 *
 * val signed = signTx(tx)                      // existing code
 *
 * // Re-encode back to the inbound encoding so the DApp's signature
 * // verification hash matches.
 * val finalBytes = TxCodec.toEncoding(signed.toByteArray(), inboundEncoding)
 * claim.finalTx = Base58Btc.encode(finalBytes)
 * ```
 *
 * Mirrors the responsibilities of `abt-wallet/src/libs/chain/tx-codec.ts`.
 */
object TxCodec {

  private val CBOR_PREFIX: ByteArray = CanonicalCbor.SELF_DESCRIBE_PREFIX

  /** Detect inbound encoding by inspecting the first 3 bytes. */
  @JvmStatic
  fun detectEncoding(bytes: ByteArray): Encoding =
    if (bytes.size >= 3 &&
      bytes[0] == CBOR_PREFIX[0] &&
      bytes[1] == CBOR_PREFIX[1] &&
      bytes[2] == CBOR_PREFIX[2]
    ) Encoding.CBOR else Encoding.PROTOBUF

  /**
   * Convert Transaction wire bytes between CBOR and protobuf encodings.
   * When [from] equals [to] the input is returned unchanged (no
   * allocation). Otherwise the bytes round-trip through an internal
   * protobuf Transaction representation.
   */
  @JvmStatic
  fun convert(bytes: ByteArray, from: Encoding, to: Encoding): ByteArray {
    if (from == to) return bytes
    val tx: Transaction = when (from) {
      Encoding.CBOR -> {
        val map = CanonicalCbor.parseCanonical("Transaction", bytes)
        MapToTransaction.build(map)
      }
      Encoding.PROTOBUF -> Transaction.parseFrom(bytes)
    }
    return when (to) {
      Encoding.CBOR -> {
        val map = TransactionToMap.convert(tx)
        CanonicalCbor.canonicalBytes("Transaction", map)
      }
      Encoding.PROTOBUF -> tx.toByteArray()
    }
  }

  /**
   * Shortcut: normalize any inbound Transaction bytes to protobuf. Handy
   * when the signing path is hard-wired to javalite or full-java
   * protobuf APIs.
   */
  @JvmStatic
  fun toProtobuf(bytes: ByteArray): ByteArray =
    convert(bytes, detectEncoding(bytes), Encoding.PROTOBUF)

  /**
   * Shortcut: Transaction protobuf bytes → the specified outbound encoding.
   * Use this when writing `finalTx` to return to a DApp whose `partialTx`
   * was known to be [encoding].
   */
  @JvmStatic
  fun toEncoding(protoBytes: ByteArray, encoding: Encoding): ByteArray =
    convert(protoBytes, Encoding.PROTOBUF, encoding)
}
