package io.arcblock.tx_codec.internal

import com.google.protobuf.Descriptors.Descriptor
import io.arcblock.canonical_cbor.CanonicalCborException
import ocap.Tx
import ocap.Type

/**
 * Maps OCAP message simple names (e.g. `TransferV2Tx`) to the generated
 * protobuf [Descriptor]. Used by [MapToTransaction] and [TransactionToMap]
 * to look up the schema for arbitrary Any-inner types without hardcoding
 * a switch per typeUrl.
 *
 * Descriptors come from the outer Java classes generated for tx.proto /
 * type.proto / enum.proto / vendor.proto — every inner message exposes a
 * static `getDescriptor()` method.
 */
internal object DescriptorRegistry {

  private val byName: Map<String, Descriptor> by lazy { buildIndex() }

  /** Look up a [Descriptor] by its simple message name. */
  internal fun lookup(name: String): Descriptor =
    byName[name]
      ?: throw CanonicalCborException("tx-codec: no descriptor for message \"$name\"")

  /** True when [name] is a known OCAP message (safe to attempt lookup). */
  internal fun contains(name: String): Boolean = byName.containsKey(name)

  private fun buildIndex(): Map<String, Descriptor> {
    val out = HashMap<String, Descriptor>()
    // Every type registered via FileDescriptor.getMessageTypes() plus
    // recursive nested types. Walking Tx / Type gives us everything
    // currently reachable from tx.proto + type.proto (Transaction,
    // TransferV2Tx, TokenInput, BigUint, Multisig, …).
    for (fd in listOf(Tx.getDescriptor(), Type.getDescriptor())) {
      for (msg in fd.messageTypes) registerRecursively(msg, out)
    }
    return out
  }

  private fun registerRecursively(msg: Descriptor, out: MutableMap<String, Descriptor>) {
    out[msg.name] = msg
    for (nested in msg.nestedTypes) registerRecursively(nested, out)
  }
}
