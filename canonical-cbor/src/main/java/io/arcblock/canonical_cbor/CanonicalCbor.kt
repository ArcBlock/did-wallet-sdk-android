package io.arcblock.canonical_cbor

/**
 * Canonical CBOR Transaction encoder/decoder for OCAP.
 *
 * Port of `blockchain/core/message/src/canonical-cbor.ts`. Wire format spec is
 * documented in `planning/canonical-cbor/spec.md` next to this module.
 *
 * This file is a scaffold — actual implementations land in subsequent
 * commits (see kotlin-port.md §7 Implementation checklist).
 */
object CanonicalCbor {

  /** RFC 8949 self-describe tag 55799 prefix, emitted at byte 0 of every
   *  canonical CBOR encoding. Receivers can use this to distinguish CBOR
   *  from protobuf without a try/catch. */
  @JvmField
  val SELF_DESCRIBE_PREFIX: ByteArray = byteArrayOf(
    0xd9.toByte(), 0xd9.toByte(), 0xf7.toByte()
  )

  /** CBOR tag 2 — positive bignum (RFC 8949 §3.4.3). */
  const val TAG_POSITIVE_BIGNUM: Int = 2

  /** CBOR tag 3 — negative bignum (RFC 8949 §3.4.3). */
  const val TAG_NEGATIVE_BIGNUM: Int = 3

  /** CBOR tag 55799 — self-describe (RFC 8949 §3.4.6). */
  const val TAG_SELF_DESCRIBE: Int = 55799

  /**
   * Encode [data] as canonical CBOR bytes for the given OCAP message [type].
   *
   * @param type  e.g. "Transaction", "TransferV2Tx"
   * @param data  field name → value map
   * @return      canonical CBOR bytes including the self-describe tag prefix
   * @throws CanonicalCborException on unknown [type], invalid BigInt input,
   *         or missing typeUrl in Any fields
   */
  @JvmStatic
  fun canonicalBytes(type: String, data: Map<String, Any?>): ByteArray {
    throw NotImplementedError(
      "Implementation pending — see Phase 1.3/1.4/1.5 in kotlin-port.md"
    )
  }

  /**
   * Decode canonical CBOR [bytes] back to a plain map for the given OCAP
   * message [type]. Validates the self-describe tag prefix.
   *
   * @throws CanonicalCborException on missing tag 55799 prefix or malformed
   *         body
   */
  @JvmStatic
  fun parseCanonical(type: String, bytes: ByteArray): Map<String, Any?> {
    throw NotImplementedError(
      "Implementation pending — see Phase 1.6 in kotlin-port.md"
    )
  }
}
