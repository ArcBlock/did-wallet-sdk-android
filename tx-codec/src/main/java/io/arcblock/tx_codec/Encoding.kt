package io.arcblock.tx_codec

/**
 * Wire format of an OCAP Transaction.
 *
 * Chain nodes at or above 1.30.4 accept both formats and auto-detect via
 * RFC 8949 tag 55799 (0xd9 0xd9 0xf7). Wallet flows that interact with
 * DApps must round-trip in the SAME encoding the DApp sent, else
 * signature verification on the DApp side fails.
 */
enum class Encoding { CBOR, PROTOBUF }
