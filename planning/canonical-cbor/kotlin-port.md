# Kotlin Port — Implementation Guide

Companion to `spec.md`. This doc tells you how to actually write the code.

## 1. Module placement

Create a new Gradle module `canonical-cbor` alongside the existing `protobuf`
module in `did-wallet-sdk-android/`:

```
did-wallet-sdk-android/
├── protobuf/                   ← existing, generates Transaction / ItxTypes
├── canonical-cbor/             ← NEW, this module
│   ├── build.gradle
│   └── src/main/java/io/arcblock/canonical_cbor/
│       ├── CanonicalCbor.kt    ← encode + decode entry points
│       ├── FieldResolver.kt    ← proto schema → field id lookup
│       ├── BigIntCodec.kt      ← BigUint/BigSint tag 2/3
│       ├── AnyCodec.kt         ← Any unwrap + opaque payloads
│       └── internal/           ← package-private helpers
└── wallet-sdk/
```

Depend on `protobuf` (for generated `Type.Transaction` etc).

## 2. Dependency choice

**Recommended: `com.upokecenter:cbor:4.5.4`**.

Why:
- Pure Java, no native code. Works on Android min SDK 21+.
- Supports arbitrary tags via `CBORObject.FromObjectAndTag()`.
- `CBORObject.NewMap()` preserves insertion order (critical for canonical
  encoding).
- `EncodeOptions("ctap2canonical=true")` gets close to RFC 8949 §4.2.1 but
  **does NOT match cborg exactly** — see §5 below for the adjustments.
- Active upstream maintenance.

Alternatives considered:

| Library | Verdict |
|---|---|
| `com.fasterxml.jackson.dataformat:jackson-dataformat-cbor` | Rejected. Does not expose canonical mode; no fine-grained tag control. |
| `org.bouncycastle:bcpkix-jdk18on` (CBOR pieces) | Rejected. Too small a surface for our needs. |
| `com.augustcellars.cose:cose-java` | Rejected. Focused on COSE, not general CBOR. |
| `co.nstant.in:cbor:0.9` | Considered. Older, less active. upokecenter is newer and more featureful. |

Maven:

```gradle
dependencies {
  api "com.upokecenter:cbor:4.5.4"
  api project(":protobuf")
}
```

## 3. Public API surface

```kotlin
package io.arcblock.canonical_cbor

object CanonicalCbor {
  /** RFC 8949 self-describe tag prefix. Emitted at byte 0 of every canonical
   *  encoding. Receivers can use this to distinguish CBOR vs protobuf. */
  val SELF_DESCRIBE_PREFIX: ByteArray = byteArrayOf(0xd9.toByte(), 0xd9.toByte(), 0xf7.toByte())

  /**
   * Encode [data] as canonical CBOR bytes for the given message [type]
   * (e.g. "Transaction", "TransferV2Tx"). Throws [CanonicalCborException]
   * on unknown type, invalid BigInt, or missing typeUrl in Any fields.
   */
  fun canonicalBytes(type: String, data: Map<String, Any?>): ByteArray

  /**
   * Decode canonical CBOR bytes back to a plain map. Throws
   * [CanonicalCborException] if [bytes] do not start with the self-describe
   * prefix. Returns jspb-compatible keys (both `fieldName` and
   * `fieldNameList` for repeated fields).
   */
  fun parseCanonical(type: String, bytes: ByteArray): Map<String, Any?>
}

class CanonicalCborException(message: String, cause: Throwable? = null)
  : RuntimeException(message, cause)
```

Design notes:

- **Input is `Map<String, Any?>`**, not a typed Transaction builder. This
  matches the Web reference and lets the adapter layer (in arc-wallet-android)
  decide how to convert Kotlin's immutable Transaction proto to a map without
  forcing the SDK to depend on how callers want to model tx data.
- **Never leak `CBORObject` across the module boundary.** Internal only.
- **`Any?` not `Any`** — null is a valid value meaning "omit". Do not call
  `requireNotNull` on map values.

## 4. Line-by-line mapping from TypeScript

| TypeScript function | Kotlin equivalent | Notes |
|---|---|---|
| `canonicalBytes(type, data)` (line 409) | `CanonicalCbor.canonicalBytes(type, data)` | Same signature. Wraps body in `Tagged(55799, body)`. |
| `encodeMessageFields(type, data)` (line 361) | `internal.encodeMessageFields(type, data): CBORObject` | Returns a CBOR map (integer-keyed). |
| `encodeFieldValue(value, field)` (line 292) | `internal.encodeFieldValue(value, field): CBORObject?` | Returns null to signal "omit". |
| `normalizeBigIntWrapper(value, type)` (line 82) | `BigIntCodec.normalize(value, type): BigIntRepr?` | Returns null for zero/missing → omit. |
| `resolveAnyTypeUrl(value)` (line 177) | `AnyCodec.resolveTypeUrl(value): AnyDescriptor` | Keep the precise discriminant (§spec 7.1). |
| `encodeAnyValue(value)` (line 231) | `AnyCodec.encode(value): CBORObject` | Opaque payload branch + known-schema branch. |
| `parseCanonical(type, bytes)` (line 562) | `CanonicalCbor.parseCanonical(type, bytes)` | Validate prefix first. |
| `decodeMessageMap(type, payload)` (line 539) | `internal.decodeMessageMap(type, payload): Map<String, Any?>` | Dual-key emission (canonical + jspb alias). |
| `decodeAnyValue(payload)` (line 448) | `AnyCodec.decode(payload)` | Opaque payload branch + known-schema branch. |
| `mapsToPlainObjects(value)` (line 434) | `internal.mapsToPlainObjects(value): Any?` | Convert nested CBOR maps from opaque payloads. |

## 5. Traps and adjustments over stock libraries

### 5.1 `upokecenter/cbor` map ordering is not cborg ordering

Out of the box, `CBORObject.NewMap()` uses insertion order. Cborg's
`rfc8949EncodeOptions` **also** uses insertion order, BUT the TypeScript
encoder explicitly builds a `Map` in ascending key order. So you must sort
**before** inserting.

```kotlin
val sortedFields = fields.entries.sortedBy { it.value.id }
for ((name, spec) in sortedFields) {
  // insert into CBORObject
}
```

Do NOT use `CBORObject.NewOrderedMap()` — it does nothing useful for integer
keys. Do NOT rely on `ctap2canonical` encode option — it re-sorts by byte
length of keys, which is **wrong** for our integer-keyed maps (CTAP2 canonical
is length-first, OCAP canonical is value-ascending).

### 5.2 `undefined` vs `null` vs missing

Kotlin has no `undefined`. The `Map<String, Any?>` input uses three states:

- key missing → field not provided → omit
- key present, value = null → explicit "unset" → omit
- key present, value = zero/empty → scalar default → omit (per §spec 4)

All three produce **identical** encoded bytes. Test vectors must cover all
three paths.

### 5.3 BigInt representation

- `java.math.BigInteger` for arbitrary precision.
- CBOR tag 2/3 via `CBORObject.FromObjectAndTag(byteArray, 2)`.
- **Strip leading zeros** from magnitude before tagging (mirror of line 67).
- Zero magnitude → return null from `BigIntCodec.normalize` → caller omits
  the field.

### 5.4 `ByteArray` vs `com.google.protobuf.ByteString`

Input can come as either. `encodeFieldValue` for `bytes` must accept both:

```kotlin
fun bytesOf(value: Any?): ByteArray? = when (value) {
  null -> null
  is ByteArray -> value
  is ByteString -> value.toByteArray()
  is String -> hexOrBase64Decode(value)  // match toUint8Array()
  else -> throw CanonicalCborException("expected bytes, got ${value::class}")
}
```

### 5.5 Float encoding

Kotlin `Float` (32-bit) and `Double` (64-bit) are different. Always encode as
64-bit double — `CBORObject.FromObject(value.toDouble())` — to match the
reference implementation's `Number(value)`.

### 5.6 Enum-as-string vs enum-as-number

Input may be either. `encodeFieldValue` for enum type:

```kotlin
fun encodeEnum(value: Any?, type: String): CBORObject = when (value) {
  is String -> CBORObject.FromObject(lookupEnum(type, value))
  is Number -> CBORObject.FromObject(value.toInt())
  else -> throw CanonicalCborException("unknown enum repr for $type")
}
```

`lookupEnum` reads from the protobuf descriptor. You'll need a
`FieldResolver` module that wraps protoc-generated descriptors — see §6.

## 6. Schema lookup — `FieldResolver`

The encoder needs to answer two questions for any message type:

1. "What are the proto fields of `TransferV2Tx`? Give me `{name → {id, type, rule}}`."
2. "Is `WalletType` an enum type? If so what's the numeric value of
   `ACCOUNT_MIGRATE`?"

On the TypeScript side this is `provider.ts` which reads from a pbjs schema
JSON. On Android, you have two options:

### Option A: Use protoc-generated descriptors (recommended)

`Type.Transaction.getDescriptor()` returns `Descriptors.Descriptor` with
`getFields()` giving you `FieldDescriptor` (number, name, type, repeated).
Enum types via `Descriptors.EnumDescriptor`.

Pros: no duplication of schema info. Always in sync with `.proto` files.
Cons: protoc-gen-javalite **strips descriptors** by default. You need the
full Java runtime or a different generator option.

Check `did-wallet-sdk-android/protobuf/build.gradle` — the current setup uses
javalite which drops descriptors. Two paths forward:

1. Switch to the full Java generator for this module only. Bundle size
   increases ≈500KB.
2. Add a second `.pb` file with descriptors and load at runtime via
   `FileDescriptorProto`.

### Option B: Ship a schema JSON next to the code

Generate a JSON blob with `{messageName: {fields: {...}}, enums: {...}}`
from the `.proto` files at build time. Load at SDK init.

Pros: small, predictable.
Cons: duplicates schema. Drift risk if `.proto` changes and JSON isn't
regenerated.

**Recommendation**: Option A with javalite switched to full Java. Tx-level
code is not size-critical and the compile-time safety is worth it.

## 7. Implementation checklist

Rough order — each step verified against the golden vectors before the next:

- [ ] Gradle module scaffold + dependencies
- [ ] `FieldResolver` reading protoc descriptors (or Option B)
- [ ] Constants (`SELF_DESCRIBE_PREFIX`, tags)
- [ ] `encodeScalarInteger`, `encodeScalarFloat`, `encodeScalarString`,
      `encodeScalarBytes`, `encodeScalarBool`
- [ ] `isDefaultScalar` — exactly match §spec 4
- [ ] `BigIntCodec.normalize` + encode to tagged bytes
- [ ] `encodeTimestampValue` (ISO-8601 string output)
- [ ] `AnyCodec.resolveTypeUrl` — keep the precise discriminant
- [ ] `AnyCodec.encode` — opaque + known-schema branches
- [ ] `encodeFieldValue` dispatcher
- [ ] `encodeMessageFields` with integer-keyed sorted map
- [ ] `canonicalBytes(type, data)` — wrap with tag 55799
- [ ] **Verify against `transfer_v2` golden vector** (byte exact)
- [ ] **Verify against `declare_tx`** (tests string-only fields)
- [ ] **Verify against `acquire_asset_v2`** (tests repeated fields)
- [ ] **Verify against `consume_asset`** (tests empty-array omission)
- [ ] **Verify against `transaction_full`** (tests nested Any)
- [ ] Decoder path: `parseCanonical`, `decodeMessageMap`, `decodeFieldValue`,
      `decodeAnyValue`, `mapsToPlainObjects`
- [ ] Round-trip test on all 5 vectors: `parse(encode(parse(bytes))) == parse(bytes)`
- [ ] Edge cases: zero BigUint, null signature, AccountMigrateTx.type field

## 8. Testing strategy

### 8.1 Byte-exact golden vector tests

```kotlin
@Test fun `transfer_v2 vector`() {
  val input = Json.parse(loadResource("vectors/transfer_v2.input.json"))
  val expected = loadResource("vectors/transfer_v2.cbor.bin")
  val actual = CanonicalCbor.canonicalBytes("Transaction", input)
  assertArrayEquals(expected, actual)
}
```

Copy vectors from `/arc-wallet-android/planning/cbor-support/vectors/` into
`canonical-cbor/src/test/resources/vectors/`.

### 8.2 Round-trip tests

```kotlin
@Test fun `roundtrip transaction_full`() {
  val bytes = loadResource("vectors/transaction_full.cbor.bin")
  val parsed1 = CanonicalCbor.parseCanonical("Transaction", bytes)
  val reEncoded = CanonicalCbor.canonicalBytes("Transaction", parsed1)
  val parsed2 = CanonicalCbor.parseCanonical("Transaction", reEncoded)
  assertEquals(parsed1, parsed2)
}
```

Note: `parsed1 == parsed2`, not `bytes == reEncoded` — the first parse drops
wrapper aliases (the dual-key emission) that the second encode wouldn't
re-emit. Byte-exact round-trip is **not** guaranteed if jspb alias keys are
present in the input; semantic round-trip is.

### 8.3 Property-based tests (nice-to-have)

Use jqwik or Kotlin's built-in `Random` to generate random valid Transactions
and verify `parse(encode(x))` equals a normalized form of `x`. Specifically
useful for catching off-by-one in BigInt byte length.

### 8.4 Cross-client fixture generation

Before trusting the Kotlin implementation, generate **at least 10 new
fixtures** from the Web reference covering Transaction types not in the 5
golden vectors:

- TransferV3Tx (inputs[] with multiple owners)
- AcquireAssetV3Tx
- WithdrawTokenV2Tx (Rollup)
- ExchangeV2Tx (multi-sig payment-kit)
- StakeTx
- DelegateTx (with rules)
- Subscription (opaque vc payload)
- AccountMigrateTx (to test the `type` enum field false-positive regression)

See `arc-wallet-android/planning/cbor-support/golden-vectors.md` for how to
generate these.

## 9. Performance notes

Not a hot path for wallet use (1 encode + 1 hash per sign). Don't
micro-optimize. Correctness > speed.

But:

- Do NOT rebuild the `FieldResolver` on every call. Cache by message type.
- CBORObject allocation is fine; don't bother with buffer reuse.

## 10. Open questions for reviewer

- [ ] Should we expose `parseCanonical` with jspb-alias emission on by
      default, or gate behind a flag? (TypeScript does it unconditionally.)
- [ ] Should the module bundle the proto descriptors or rely on the caller's
      protobuf module? (Current plan: rely on caller via
      `dependencies { api project(":protobuf") }`.)
- [ ] CBOR library: any binary-size or ProGuard concerns with upokecenter?
      Need to run a size diff.

## 11. Status

First draft. No code written yet. Ready for review before starting the
checklist in §7.
