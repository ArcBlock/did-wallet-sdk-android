package io.arcblock.tx_codec

import ocap.Type.Transaction

/**
 * A decoded Transaction plus the wire encoding it came in as. Callers
 * should pass the same [encoding] back to [TxCodec.encode] when writing
 * `finalTx` so the outbound bytes match the DApp's expected hash.
 */
data class DecodedTx(
  val tx: Transaction,
  val encoding: Encoding
)
