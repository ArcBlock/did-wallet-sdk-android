package io.arcblock.canonical_cbor

import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBOREncodeOptions

/**
 * Canonical CBOR Transaction encoder/decoder for OCAP.
 *
 * Port of `blockchain/core/message/src/canonical-cbor.ts`. Wire format spec is
 * documented in `planning/canonical-cbor/spec.md` next to this module.
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

  /** typeUrls whose payload is treated as opaque CBOR (no schema-driven
   *  encoding). Mirror of the constant in canonical-cbor.ts. */
  @JvmField
  val OPAQUE_TYPE_URLS: Set<String> = setOf("json", "vc", "fg:x:address")

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
    val body = Encoder.encodeMessageFields(type, data)
    val tagged = CBORObject.FromObjectAndTag(body, TAG_SELF_DESCRIBE)
    return tagged.EncodeToBytes()
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
    return Decoder.parseCanonical(type, bytes)
  }

  /**
   * Encode an opaque payload (Any payload for json / vc / fg:x:address
   * typeUrls) to raw CBOR bytes — no self-describe tag, no schema lookup.
   * Used by tx-codec to round-trip opaque Any.value through protobuf
   * without losing information.
   */
  @JvmStatic
  fun encodeOpaque(value: Any?): ByteArray =
    Encoder.opaqueToCbor(Encoder.normalizeOpaquePayload(value)).EncodeToBytes()

  /**
   * Decode raw CBOR [bytes] (produced by [encodeOpaque]) back to a plain
   * Kotlin object tree.
   *
   * **Note on tagging**: the input must NOT carry the self-describe tag
   * 55799 prefix (`0xd9 0xd9 0xf7`). [encodeOpaque] intentionally emits
   * payload-only CBOR so the bytes can be embedded directly inside
   * another CBOR structure (e.g. as a nested map value) without
   * double-tagging. If you have a self-describe-tagged buffer use
   * [parseCanonical] instead.
   *
   * **Untrusted input**: opaque payloads are dapp-controlled; downstream
   * UI rendering should treat the returned tree as untrusted (no
   * automatic markup execution, sane size limits, etc).
   */
  @JvmStatic
  fun decodeOpaque(bytes: ByteArray): Any? =
    Decoder.cborObjectToPlainObject(CBORObject.DecodeFromBytes(bytes))
}
