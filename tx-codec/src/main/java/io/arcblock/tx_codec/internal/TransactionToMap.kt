package io.arcblock.tx_codec.internal

import com.google.protobuf.Any as ProtoAny
import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumValueDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Message
import io.arcblock.canonical_cbor.CanonicalCbor
import io.arcblock.canonical_cbor.CanonicalCborException
import io.arcblock.tx_codec.generated.Type.Transaction

/**
 * Serialize a [Transaction] (or any nested message) to the plain-map
 * shape `CanonicalCbor.canonicalBytes("Transaction", map)` expects.
 *
 * Reflection-based inverse of [MapToTransaction]. Handles Any unwrap
 * (flat `{typeUrl, ...innerFields}` form), BigUint/BigSint wrapper
 * conversion, and repeated / map / scalar coercion.
 */
internal object TransactionToMap {

  internal fun convert(tx: Transaction): Map<String, Any?> =
    messageToMap(tx)

  // ---------------------------------------------------------------------

  internal fun messageToMap(msg: Message): Map<String, Any?> {
    val desc = msg.descriptorForType
    val out = LinkedHashMap<String, Any?>()

    // BigUint / BigSint — emit the wrapper shape canonical-cbor expects.
    if (desc.name == "BigUint" || desc.name == "BigSint") {
      val valueField = desc.findFieldByName("value")
      if (valueField != null && msg.hasField(valueField)) {
        out["value"] = (msg.getField(valueField) as ByteString).toByteArray()
      }
      val minusField = desc.findFieldByName("minus")
      if (minusField != null && msg.hasField(minusField)) {
        val minus = msg.getField(minusField) as Boolean
        if (minus) out["minus"] = true
      }
      return out
    }

    for (field in desc.fields) {
      if (!hasPopulatedField(msg, field)) continue
      val raw = msg.getField(field)
      val converted = convertFieldValue(field, raw) ?: continue
      // Use jsonName (camelCase) to match the pbjs schema keys that
      // canonical-cbor's FieldResolver consumes. The protobuf `field.name`
      // property returns snake_case (`chain_id`), which canonical-cbor
      // would silently drop.
      out[field.jsonName] = converted
    }
    return out
  }

  private fun hasPopulatedField(msg: Message, field: FieldDescriptor): Boolean {
    return if (field.isRepeated) msg.getRepeatedFieldCount(field) > 0
    else msg.hasField(field) || field.type != FieldDescriptor.Type.MESSAGE &&
      !isScalarDefault(msg.getField(field), field)
  }

  private fun isScalarDefault(value: Any?, field: FieldDescriptor): Boolean {
    if (value == null) return true
    return when (field.type) {
      FieldDescriptor.Type.STRING -> value == ""
      FieldDescriptor.Type.BOOL -> value == false
      FieldDescriptor.Type.INT32,
      FieldDescriptor.Type.SINT32,
      FieldDescriptor.Type.SFIXED32,
      FieldDescriptor.Type.UINT32,
      FieldDescriptor.Type.FIXED32 -> value == 0
      FieldDescriptor.Type.INT64,
      FieldDescriptor.Type.SINT64,
      FieldDescriptor.Type.SFIXED64,
      FieldDescriptor.Type.UINT64,
      FieldDescriptor.Type.FIXED64 -> value == 0L
      FieldDescriptor.Type.BYTES -> (value as ByteString).isEmpty
      FieldDescriptor.Type.FLOAT -> value == 0f
      FieldDescriptor.Type.DOUBLE -> value == 0.0
      FieldDescriptor.Type.ENUM -> (value as EnumValueDescriptor).number == 0
      else -> false
    }
  }

  private fun convertFieldValue(field: FieldDescriptor, raw: Any?): Any? {
    if (raw == null) return null

    if (field.isRepeated && !field.isMapField) {
      val list = raw as List<*>
      if (list.isEmpty()) return null
      val converted = list.mapNotNull { convertScalar(field, it) }
      return converted
    }

    return convertScalar(field, raw)
  }

  private fun convertScalar(field: FieldDescriptor, value: Any?): Any? {
    if (value == null) return null

    return when (field.type) {
      FieldDescriptor.Type.MESSAGE, FieldDescriptor.Type.GROUP -> {
        val inner = value as Message
        when (field.messageType.fullName) {
          "google.protobuf.Any" -> {
            // Reflection-based message traversal can hand us a DynamicMessage
            // for Any fields (especially nested Any inside another DynamicMessage,
            // or under protobuf-java runtimes that don't auto-resolve well-known
            // types). Re-parse from wire bytes to land a concrete ProtoAny.
            val any = if (inner is ProtoAny) inner else ProtoAny.parseFrom(inner.toByteArray())
            anyToMap(any)
          }
          // canonical-cbor emits Timestamp as ISO-8601 string, not as a
          // {seconds, nanos} sub-map. Mirror it here so any itx that
          // contains a Timestamp field (e.g. `expiredAt`) round-trips.
          "google.protobuf.Timestamp" -> timestampToIso(inner)
          else -> messageToMap(inner)
        }
      }
      FieldDescriptor.Type.ENUM -> (value as EnumValueDescriptor).number
      FieldDescriptor.Type.BYTES -> (value as ByteString).toByteArray()
      FieldDescriptor.Type.STRING -> value.toString()
      FieldDescriptor.Type.BOOL -> value == true
      FieldDescriptor.Type.DOUBLE -> (value as Number).toDouble()
      FieldDescriptor.Type.FLOAT -> (value as Number).toFloat()
      FieldDescriptor.Type.INT32,
      FieldDescriptor.Type.SINT32,
      FieldDescriptor.Type.SFIXED32,
      FieldDescriptor.Type.UINT32,
      FieldDescriptor.Type.FIXED32 -> value as Int
      FieldDescriptor.Type.INT64,
      FieldDescriptor.Type.SINT64,
      FieldDescriptor.Type.SFIXED64 -> value as Long
      // UINT64 / FIXED64: protobuf-java returns the wire bits as a signed
      // Long. Values > Long.MAX_VALUE come out negative and would be
      // emitted as a CBOR negative integer, drifting from the TS encoder
      // which uses BigInt. Lift to BigInteger via the unsigned bit pattern
      // so canonical-cbor emits a positive integer (or tagged bignum
      // beyond 2^64 — caps out at the same code path).
      FieldDescriptor.Type.UINT64,
      FieldDescriptor.Type.FIXED64 -> {
        val signed = value as Long
        if (signed >= 0L) signed
        else java.math.BigInteger(java.lang.Long.toUnsignedString(signed))
      }
    }
  }

  /** Convert a `google.protobuf.Timestamp` message back to ISO-8601 (the
   *  shape `Encoder.encodeFieldValue` expects for Timestamp fields).
   *  Inverse of `MapToTransaction.buildTimestamp`.
   *  internal for unit-test access. */
  internal fun timestampToIso(msg: Message): String {
    val desc = msg.descriptorForType
    val seconds = (msg.getField(desc.findFieldByName("seconds")) as? Long) ?: 0L
    val nanos = (msg.getField(desc.findFieldByName("nanos")) as? Int) ?: 0
    return java.time.Instant.ofEpochSecond(seconds, nanos.toLong()).toString()
  }

  /**
   * Unpack a google.protobuf.Any into the flat form canonical-cbor
   * consumes: `{typeUrl, ...innerFields}`. The inner bytes are parsed
   * against the descriptor resolved from typeUrl; on unknown typeUrls the
   * raw bytes are preserved under `value`.
   */
  private fun anyToMap(any: ProtoAny): Map<String, Any?> {
    val typeUrl = any.typeUrl
    val bytes = any.value
    val out = LinkedHashMap<String, Any?>()
    out["typeUrl"] = typeUrl

    // Opaque payload (json / vc / fg:x:address): the value bytes are raw
    // CBOR (stashed by MapToTransaction.buildAny). Decode back to a plain
    // Kotlin object so canonical-cbor's Encoder.encodeAnyValue picks it up
    // unchanged via its own opaque branch.
    if (CanonicalCbor.OPAQUE_TYPE_URLS.contains(typeUrl)) {
      out["value"] = if (bytes.isEmpty) null else CanonicalCbor.decodeOpaque(bytes.toByteArray())
      return out
    }

    val innerName = typeUrlToMessageName(typeUrl)
    if (!DescriptorRegistry.contains(innerName)) {
      out["value"] = bytes.toByteArray()
      return out
    }
    val innerDesc = DescriptorRegistry.lookup(innerName)
    val innerMsg = try {
      DynamicMessage.parseFrom(innerDesc, bytes)
    } catch (err: InvalidProtocolBufferException) {
      throw CanonicalCborException("tx-codec: Any inner bytes do not parse as $innerName", err)
    }
    // Merge inner fields flat (excluding typeUrl which is already set).
    val inner = messageToMap(innerMsg)
    for ((k, v) in inner) if (k != "typeUrl" && v != null) out[k] = v
    return out
  }

  // Same helpers as MapToTransaction — kept local to avoid a public API.
  private fun typeUrlToMessageName(typeUrl: String): String {
    if (!typeUrl.contains(':')) return typeUrl
    val parts = typeUrl.split(':')
    if (parts.size < 3 || parts[0] != "fg") return typeUrl
    val suffix = upperCamelCase(parts[2])
    return when (parts[1]) {
      "t" -> "${suffix}Tx"
      "s" -> "${suffix}State"
      "x" -> when {
        parts[2].startsWith("stake_") ->
          "StakeFor${upperCamelCase(parts[2].removePrefix("stake_"))}"
        parts[2] == "asset_factory" -> "AssetFactory"
        parts[2] == "transaction_info" -> "TransactionInfo"
        parts[2] == "address" -> "DummyCodec"
        else -> suffix
      }
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
}
