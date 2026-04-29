package io.arcblock.canonical_cbor

/**
 * Thrown by [CanonicalCbor] on any failure to encode or decode a canonical
 * CBOR message. Error messages deliberately avoid echoing user-supplied field
 * content to prevent leaking payloads in logs.
 */
class CanonicalCborException(
  message: String,
  cause: Throwable? = null
) : RuntimeException(message, cause)
