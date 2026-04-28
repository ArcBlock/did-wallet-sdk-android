package io.arcblock.tx_codec.internal

import com.google.protobuf.Any as ProtoAny
import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.Message
import io.arcblock.canonical_cbor.CanonicalCbor
import io.arcblock.canonical_cbor.CanonicalCborException
import io.arcblock.tx_codec.generated.Type.Transaction
import java.math.BigInteger

/**
 * Build a protobuf [Transaction] from the plain-map shape produced by
 * `CanonicalCbor.parseCanonical("Transaction", bytes)`.
 *
 * Reflection-based: uses each message's [Descriptor] to drive field
 * population, so we don't need a switch per itx typeUrl. The only hard
 * work is:
 *   - `google.protobuf.Any` → repack the flat `{typeUrl, ...fields}`
 *     map into `Any { type_url, value: <inner-proto-bytes> }`
 *   - BigUint / BigSint wrappers → build a nested Message with the
 *     `value` / `minus` fields populated from the CBOR-side wrapper
 */
internal object MapToTransaction {

  internal fun build(map: Map<String, Any?>): Transaction {
    val b = Transaction.newBuilder()
    populate(b, Transaction.getDescriptor(), map)
    return b.build()
  }

  // ---------------------------------------------------------------------
  // Core recursion
  // ---------------------------------------------------------------------

  private fun populate(builder: Message.Builder, desc: Descriptor, data: Map<String, Any?>) {
    for (field in desc.fields) {
      val value = readField(data, field) ?: continue
      setFieldValue(builder, field, value)
    }
  }

  /** Read a field value honoring the jspb alias (e.g. `signaturesList`
   *  vs `signatures`) so either shape round-trips. Prefers `jsonName`
   *  (camelCase) which matches the canonical-cbor / pbjs schema keys;
   *  also accepts the proto snake_case name and the jspb list/map
   *  suffixes as fallbacks. */
  private fun readField(data: Map<String, Any?>, field: FieldDescriptor): Any? {
    val camel = field.jsonName       // "chainId"
    val protoName = field.name       // "chain_id"
    val aliasList = if (field.isRepeated) "${camel}List" else null
    val aliasMap = if (field.isMapField) "${camel}Map" else null

    val candidates = listOfNotNull(camel, protoName, aliasList, aliasMap)
    for (key in candidates) {
      if (data.containsKey(key)) return data[key]
    }
    return null
  }

  private fun setFieldValue(builder: Message.Builder, field: FieldDescriptor, value: Any?) {
    if (value == null) return

    if (field.isRepeated && !field.isMapField) {
      val list = value as? List<*> ?: return
      for (item in list) {
        val encoded = coerceField(field, item) ?: continue
        builder.addRepeatedField(field, encoded)
      }
      return
    }

    val encoded = coerceField(field, value) ?: return
    builder.setField(field, encoded)
  }

  private fun coerceField(field: FieldDescriptor, value: Any?): Any? {
    if (value == null) return null

    return when (field.type) {
      FieldDescriptor.Type.MESSAGE, FieldDescriptor.Type.GROUP -> {
        val nested = value as? Map<*, *>
          ?: throw CanonicalCborException("tx-codec: expected map for ${field.name}")
        @Suppress("UNCHECKED_CAST")
        coerceMessage(field.messageType, nested as Map<String, Any?>)
      }
      FieldDescriptor.Type.ENUM -> coerceEnum(field, value)
      FieldDescriptor.Type.BYTES -> coerceBytes(value)
      FieldDescriptor.Type.STRING -> value.toString()
      FieldDescriptor.Type.BOOL -> value == true
      FieldDescriptor.Type.DOUBLE -> (value as Number).toDouble()
      FieldDescriptor.Type.FLOAT -> (value as Number).toFloat()
      FieldDescriptor.Type.INT32,
      FieldDescriptor.Type.SINT32,
      FieldDescriptor.Type.SFIXED32,
      FieldDescriptor.Type.UINT32,
      FieldDescriptor.Type.FIXED32 -> coerceInt(value, 32)
      FieldDescriptor.Type.INT64,
      FieldDescriptor.Type.SINT64,
      FieldDescriptor.Type.SFIXED64,
      FieldDescriptor.Type.UINT64,
      FieldDescriptor.Type.FIXED64 -> coerceInt(value, 64)
    }
  }

  private fun coerceInt(value: Any?, bits: Int): Any {
    val long = when (value) {
      is Long -> value
      is Int -> value.toLong()
      is Short -> value.toLong()
      is Byte -> value.toLong()
      is BigInteger -> value.toLong()
      is String -> if (value.isEmpty()) 0L else value.toLong()
      else -> throw CanonicalCborException(
        "tx-codec: cannot coerce ${value!!::class.java.simpleName} to int"
      )
    }
    // protobuf setField for 32-bit types wants Int, 64-bit wants Long
    return if (bits == 32) long.toInt() else long
  }

  private fun coerceEnum(field: FieldDescriptor, value: Any?): Any {
    val enumDesc = field.enumType
    return when (value) {
      is Number -> enumDesc.findValueByNumber(value.toInt())
        ?: enumDesc.findValueByNumber(0) // default fallback
        ?: throw CanonicalCborException("tx-codec: unknown enum value ${value}")
      is String -> enumDesc.findValueByName(value)
        ?: throw CanonicalCborException("tx-codec: unknown enum name")
      else -> throw CanonicalCborException(
        "tx-codec: invalid enum input for ${field.name}"
      )
    }
  }

  private fun coerceBytes(value: Any?): ByteString {
    return when (value) {
      is ByteArray -> ByteString.copyFrom(value)
      is ByteString -> value
      is String -> {
        if (value.isEmpty()) ByteString.EMPTY
        else {
          // hex by default (matches toUint8Array heuristic)
          ByteString.copyFrom(hexToBytes(value))
        }
      }
      else -> throw CanonicalCborException(
        "tx-codec: cannot coerce ${value!!::class.java.simpleName} to bytes"
      )
    }
  }

  private fun coerceMessage(desc: Descriptor, data: Map<String, Any?>): Message {
    // google.protobuf.Any — must repack the flat inner-fields shape
    if (desc.fullName == "google.protobuf.Any") {
      return buildAny(data)
    }

    // BigUint / BigSint wrappers — canonical-cbor decoder produced the
    // {value: bytes, minus?: Boolean} shape; translate to proto builder.
    if (desc.name == "BigUint" || desc.name == "BigSint") {
      return buildBigIntMessage(desc, data)
    }

    // Generic nested message — use DynamicMessage since we may be many
    // levels deep and don't want to resolve concrete Java classes.
    val b = DynamicMessage.newBuilder(desc)
    populate(b, desc, data)
    return b.build()
  }

  private fun buildAny(data: Map<String, Any?>): ProtoAny {
    val typeUrl = (data["typeUrl"] ?: data["type_url"])?.toString()
      ?: throw CanonicalCborException(
        "tx-codec: Any field missing typeUrl"
      )
    // Opaque payload (json / vc / fg:x:address): canonical-cbor stores
    // the value as raw CBOR at Any key 1 — no schema-driven encoding. We
    // serialize it back to raw CBOR bytes and stash in ProtoAny.value so
    // the round-trip through protobuf preserves the payload byte-for-byte.
    if (CanonicalCbor.OPAQUE_TYPE_URLS.contains(typeUrl)) {
      val payload = data["value"]
      val cborBytes = CanonicalCbor.encodeOpaque(payload)
      return ProtoAny.newBuilder()
        .setTypeUrl(typeUrl)
        .setValue(ByteString.copyFrom(cborBytes))
        .build()
    }
    // Resolve inner message descriptor by stripping the typeUrl prefix.
    val innerName = typeUrlToMessageName(typeUrl)
    val innerDesc = DescriptorRegistry.lookup(innerName)
    // Strip wrapper keys before populating the inner message.
    val inner = LinkedHashMap(data)
    inner.remove("typeUrl")
    inner.remove("type_url")
    val innerBuilder = DynamicMessage.newBuilder(innerDesc)
    populate(innerBuilder, innerDesc, inner)
    return ProtoAny.newBuilder()
      .setTypeUrl(typeUrl)
      .setValue(innerBuilder.build().toByteString())
      .build()
  }

  private fun buildBigIntMessage(desc: Descriptor, data: Map<String, Any?>): Message {
    val b = DynamicMessage.newBuilder(desc)
    val valueField = desc.findFieldByName("value")
    val minusField = desc.findFieldByName("minus")
    val rawValue = data["value"]
    if (rawValue != null) b.setField(valueField, coerceBytes(rawValue))
    if (minusField != null) {
      val minus = data["minus"] == true
      if (minus) b.setField(minusField, true)
    }
    return b.build()
  }

  // ---------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------

  private fun typeUrlToMessageName(typeUrl: String): String {
    // Convert "fg:t:transfer_v2" -> "TransferV2Tx", "fg:s:account" -> "AccountState", etc.
    // Mirrors blockchain/core/proto/lib/schema.js createTypeUrls inverse.
    if (!typeUrl.contains(':')) return typeUrl
    val parts = typeUrl.split(':')
    if (parts.size < 3 || parts[0] != "fg") return typeUrl
    val suffix = upperCamelCase(parts[2])
    return when (parts[1]) {
      "t" -> "${suffix}Tx"
      "s" -> "${suffix}State"
      "x" -> handleExtendedTypeUrl(parts[2], suffix)
      else -> suffix
    }
  }

  private fun handleExtendedTypeUrl(raw: String, suffix: String): String {
    // fg:x:stake_<snake> -> StakeFor<camel>
    return if (raw.startsWith("stake_")) {
      "StakeFor${upperCamelCase(raw.removePrefix("stake_"))}"
    } else when (raw) {
      "asset_factory" -> "AssetFactory"
      "transaction_info" -> "TransactionInfo"
      "address" -> "DummyCodec"
      else -> suffix
    }
  }

  private fun upperCamelCase(snake: String): String {
    val sb = StringBuilder(snake.length)
    var upper = true
    for (c in snake) {
      if (c == '_') upper = true
      else {
        sb.append(if (upper) c.uppercaseChar() else c)
        upper = false
      }
    }
    return sb.toString()
  }

  private fun hexToBytes(hex: String): ByteArray {
    val s = if (hex.startsWith("0x") || hex.startsWith("0X")) hex.substring(2) else hex
    val clean = if (s.length % 2 == 1) "0$s" else s
    val out = ByteArray(clean.length / 2)
    for (i in out.indices) {
      val hi = Character.digit(clean[i * 2], 16)
      val lo = Character.digit(clean[i * 2 + 1], 16)
      if (hi < 0 || lo < 0) {
        throw CanonicalCborException("tx-codec: invalid hex in bytes field")
      }
      out[i] = ((hi shl 4) or lo).toByte()
    }
    return out
  }
}
