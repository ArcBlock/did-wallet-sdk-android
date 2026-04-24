package io.arcblock.canonical_cbor

import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORType
import io.arcblock.canonical_cbor.BigIntCodec.Kind
import io.arcblock.canonical_cbor.FieldResolver.ProtoField

/**
 * Canonical CBOR decoder for OCAP messages.
 *
 * Mirrors the decoder half of canonical-cbor.ts:
 *  - parseCanonical      (line 562-584)
 *  - decodeMessageMap    (line 539-560)
 *  - decodeFieldValue    (line 482-537)
 *  - decodeAnyValue      (line 448-480)
 *  - mapsToPlainObjects  (line 434-446)
 *
 * Package-internal. Public entry points live on [CanonicalCbor].
 *
 * Two quirks to be aware of (both mirror the TypeScript reference):
 *
 * 1. **Dual key emission.** For repeated fields the decoder produces both
 *    `fieldName` and `fieldNameList`; for map fields `fieldName` and
 *    `fieldNameMap`. Existing downstream code reads either name, so the
 *    CBOR-decoded map is drop-in compatible with the jspb `.toObject()`
 *    surface. See canonical-cbor.ts line 546-557.
 *
 * 2. **Opaque map flattening.** For json/vc/fg:x:address payloads, nested
 *    CBOR `Map` entries are recursively converted to Kotlin `Map<String,
 *    Any?>` so downstream JSON serialization doesn't silently lose data.
 *    See canonical-cbor.ts line 434-446.
 */
internal object Decoder {

  private val BIGINT_WRAPPER_TYPES = setOf("BigUint", "BigSint")
  private val TIMESTAMP_TYPES = setOf("google.protobuf.Timestamp", "Timestamp")
  private val ANY_TYPES = setOf("google.protobuf.Any", "Any")
  private val OPAQUE_TYPE_URLS = setOf("json", "vc", "fg:x:address")

  /**
   * Top-level entry: decode canonical CBOR [bytes] as a message of the given
   * [type]. Validates the self-describe tag 55799 prefix; anything else is
   * rejected as non-canonical input.
   */
  internal fun parseCanonical(type: String, bytes: ByteArray): Map<String, Any?> {
    if (bytes.size < CanonicalCbor.SELF_DESCRIBE_PREFIX.size ||
      bytes[0] != CanonicalCbor.SELF_DESCRIBE_PREFIX[0] ||
      bytes[1] != CanonicalCbor.SELF_DESCRIBE_PREFIX[1] ||
      bytes[2] != CanonicalCbor.SELF_DESCRIBE_PREFIX[2]
    ) {
      throw CanonicalCborException(
        "canonical-cbor: missing self-describe tag 55799 prefix"
      )
    }

    val cbor = try {
      CBORObject.DecodeFromBytes(bytes)
    } catch (err: Exception) {
      throw CanonicalCborException("canonical-cbor: malformed CBOR input", err)
    }

    val body = cbor.Untag()
    if (body.type != CBORType.Map) {
      throw CanonicalCborException("canonical-cbor: payload is not a CBOR map")
    }
    return decodeMessageMap(type, body)
  }

  /**
   * Decode a CBOR map representing a message of the given [type]. Emits both
   * the canonical proto name AND the jspb-alias name for repeated / map
   * fields per §spec 10.1.
   */
  internal fun decodeMessageMap(type: String, cborMap: CBORObject): Map<String, Any?> {
    // Top-level BigUint / BigSint special case.
    if (BIGINT_WRAPPER_TYPES.contains(type)) {
      return decodeBigIntTopLevel(type, cborMap)
    }

    val fields = FieldResolver.getFields(type)
      ?: throw CanonicalCborException("canonical-cbor: unknown message type \"$type\"")

    // Proto field id -> descriptor lookup (single pass).
    val idToField = HashMap<Int, ProtoField>(fields.size)
    for (spec in fields.values) idToField[spec.id] = spec

    val out = LinkedHashMap<String, Any?>()
    for (keyObj in cborMap.keys) {
      if (keyObj.type != CBORType.Integer) continue
      val id = keyObj.AsInt32()
      val spec = idToField[id] ?: continue
      val rawValue = cborMap[keyObj]
      val decoded = decodeFieldValue(rawValue, spec)
      out[spec.name] = decoded
      // Dual-key emission (§spec 10.1).
      when {
        spec.rule == "repeated" -> out["${spec.name}List"] = decoded
        spec.keyType != null -> out["${spec.name}Map"] = decoded
      }
    }
    return out
  }

  /**
   * Decode a top-level BigUint / BigSint CBOR map `{1: tagged-bytes, 2?: true}`
   * back to the OCAP wrapper `{value: bytes, minus?: Boolean}` shape.
   */
  private fun decodeBigIntTopLevel(type: String, cborMap: CBORObject): Map<String, Any?> {
    val valueEntry = cborMap[CBORObject.FromObject(1)]
    val out = LinkedHashMap<String, Any?>()
    if (valueEntry == null) return out
    if (!valueEntry.isTagged) {
      // Value was encoded without a bignum tag — pass through as-is.
      out["value"] = cborObjectToAny(valueEntry)
      return out
    }
    val tag = valueEntry.mostInnerTag.ToInt32Checked()
    if (tag != CanonicalCbor.TAG_POSITIVE_BIGNUM && tag != CanonicalCbor.TAG_NEGATIVE_BIGNUM) {
      throw CanonicalCborException(
        "canonical-cbor: $type wrapper expects tag 2/3, got $tag"
      )
    }
    out["value"] = valueEntry.Untag().GetByteString()
    if (type == "BigSint") {
      val minusEntry = cborMap[CBORObject.FromObject(2)]
      out["minus"] = minusEntry?.AsBoolean() == true || tag == CanonicalCbor.TAG_NEGATIVE_BIGNUM
    }
    return out
  }

  /**
   * Decode a single CBOR value [raw] into the Kotlin-side representation of
   * a field of the given proto [spec]. Mirrors canonical-cbor.ts line
   * 482-537.
   */
  internal fun decodeFieldValue(raw: CBORObject?, spec: ProtoField): Any? {
    if (raw == null) return null

    if (spec.rule == "repeated") {
      if (raw.type != CBORType.Array) return cborObjectToAny(raw)
      val itemSpec = spec.copy(rule = null)
      val out = ArrayList<Any?>(raw.size())
      for (i in 0 until raw.size()) out.add(decodeFieldValue(raw[i], itemSpec))
      return out
    }

    val type = spec.type

    if (BIGINT_WRAPPER_TYPES.contains(type)) {
      val kind = if (type == "BigSint") Kind.BIG_SINT else Kind.BIG_UINT
      return BigIntCodec.decode(raw, kind)
    }

    if (TIMESTAMP_TYPES.contains(type)) {
      if (raw.type != CBORType.TextString) {
        throw CanonicalCborException("canonical-cbor: Timestamp expected string")
      }
      return raw.AsString()
    }

    if (ANY_TYPES.contains(type)) {
      if (raw.type != CBORType.Map) {
        throw CanonicalCborException("canonical-cbor: Any expected nested map")
      }
      return decodeAnyValue(raw)
    }

    if (FieldResolver.isEnumType(type)) {
      if (raw.type == CBORType.Integer) return raw.AsInt32()
      return cborObjectToAny(raw)
    }

    if (Scalars.isScalarInt(type)) {
      return when (raw.type) {
        CBORType.Integer -> {
          // Prefer Long so the caller gets 64-bit precision; fall back to
          // BigInteger only if the value exceeds Long range.
          if (raw.CanValueFitInInt64()) raw.AsInt64Value()
          else java.math.BigInteger(raw.AsEIntegerValue().toString())
        }
        else -> cborObjectToAny(raw)
      }
    }

    if (Scalars.isScalarFloat(type)) return raw.AsDoubleValue()
    if (type == "bool") return raw.AsBoolean()
    if (type == "string") return raw.AsString()
    if (type == "bytes") {
      return if (raw.type == CBORType.ByteString) raw.GetByteString() else cborObjectToAny(raw)
    }

    // Nested message
    if (raw.type == CBORType.Map) return decodeMessageMap(type, raw)
    return cborObjectToAny(raw)
  }

  /**
   * Decode a google.protobuf.Any CBOR map back to the wallet-internal flat
   * form `{typeUrl, ...innerFields}`. For opaque typeUrls (json / vc /
   * fg:x:address), the payload at key 1 is preserved verbatim through
   * [cborObjectToPlainObject].
   */
  internal fun decodeAnyValue(cborMap: CBORObject): Map<String, Any?> {
    val typeUrlEntry = cborMap[CBORObject.FromObject(0)]
      ?: throw CanonicalCborException(
        "canonical-cbor: Any payload missing typeUrl at key 0"
      )
    if (typeUrlEntry.type != CBORType.TextString) {
      throw CanonicalCborException("canonical-cbor: Any typeUrl must be a string")
    }
    val typeUrl = typeUrlEntry.AsString()

    if (OPAQUE_TYPE_URLS.contains(typeUrl)) {
      val payload = cborMap[CBORObject.FromObject(1)]
      return mapOf(
        "typeUrl" to typeUrl,
        "value" to (payload?.let { cborObjectToPlainObject(it) })
      )
    }

    val messageName = FieldResolver.fromTypeUrl(typeUrl)
    val innerFields = FieldResolver.getFields(messageName)
    if (innerFields == null) {
      // Unknown inner type — preserve raw payload bytes if present.
      val payload = cborMap[CBORObject.FromObject(1)]
      return if (payload != null) mapOf("typeUrl" to typeUrl, "value" to cborObjectToAny(payload))
      else mapOf("typeUrl" to typeUrl)
    }

    // Known inner type: expand fields.
    val idToField = HashMap<Int, ProtoField>(innerFields.size)
    for (spec in innerFields.values) idToField[spec.id] = spec

    val out = LinkedHashMap<String, Any?>()
    out["typeUrl"] = typeUrl
    for (keyObj in cborMap.keys) {
      if (keyObj.type != CBORType.Integer) continue
      val id = keyObj.AsInt32()
      if (id == 0) continue
      val spec = idToField[id] ?: continue
      val decoded = decodeFieldValue(cborMap[keyObj], spec)
      out[spec.name] = decoded
      when {
        spec.rule == "repeated" -> out["${spec.name}List"] = decoded
        spec.keyType != null -> out["${spec.name}Map"] = decoded
      }
    }
    return out
  }

  // ---------------------------------------------------------------------
  // CBOR -> Kotlin object translation (for opaque / unknown-type paths)
  // ---------------------------------------------------------------------

  /**
   * Recursively convert a CBORObject to plain Kotlin types. Nested CBOR maps
   * become `LinkedHashMap<String, Any?>` (keys coerced to String), arrays
   * become `List<Any?>`, primitives become their natural JVM counterparts.
   *
   * Equivalent to canonical-cbor.ts `mapsToPlainObjects` — prevents silent
   * data loss when downstream code `JSON.stringify`s the result.
   */
  internal fun cborObjectToPlainObject(value: CBORObject): Any? {
    return when (value.type) {
      CBORType.Map -> {
        val out = LinkedHashMap<String, Any?>(value.size())
        for (k in value.keys) {
          val keyStr = when (k.type) {
            CBORType.TextString -> k.AsString()
            CBORType.Integer -> k.AsInt64Value().toString()
            else -> k.ToJSONString() // fallback for uncommon key types
          }
          out[keyStr] = cborObjectToPlainObject(value[k])
        }
        out
      }
      CBORType.Array -> {
        val arr = ArrayList<Any?>(value.size())
        for (i in 0 until value.size()) arr.add(cborObjectToPlainObject(value[i]))
        arr
      }
      else -> cborObjectToAny(value)
    }
  }

  /**
   * Convert a single (leaf or opaque) CBOR value to a natural Kotlin type.
   * Used as the bottom of the decode recursion and for fall-through cases
   * where field type information is not available.
   */
  private fun cborObjectToAny(value: CBORObject): Any? {
    if (value.isNull) return null
    if (value.isTrue) return true
    if (value.isFalse) return false

    // Tagged bignums — pass through as BigInteger for convenience.
    if (value.isTagged) {
      val tag = value.mostInnerTag.ToInt32Checked()
      if (tag == CanonicalCbor.TAG_POSITIVE_BIGNUM || tag == CanonicalCbor.TAG_NEGATIVE_BIGNUM) {
        val bytes = value.Untag().GetByteString()
        val bi = java.math.BigInteger(1, bytes)
        return if (tag == CanonicalCbor.TAG_NEGATIVE_BIGNUM) bi.negate() else bi
      }
      return cborObjectToAny(value.Untag())
    }

    return when (value.type) {
      CBORType.Integer -> if (value.CanValueFitInInt64()) value.AsInt64Value()
                          else java.math.BigInteger(value.AsEIntegerValue().toString())
      CBORType.FloatingPoint -> value.AsDoubleValue()
      CBORType.TextString -> value.AsString()
      CBORType.ByteString -> value.GetByteString()
      CBORType.Array -> {
        val out = ArrayList<Any?>(value.size())
        for (i in 0 until value.size()) out.add(cborObjectToAny(value[i]))
        out
      }
      CBORType.Map -> {
        val out = LinkedHashMap<Any?, Any?>(value.size())
        for (k in value.keys) out[cborObjectToAny(k)] = cborObjectToAny(value[k])
        out
      }
      else -> value.ToJSONString()
    }
  }
}
