# Canonical CBOR for Android

Port of the canonical CBOR Transaction encoder from the blockchain repo to
Kotlin so Android wallets can sign / verify / broadcast transactions in the
same wire format as the Web extension wallet and chain nodes ≥ 1.30.4.

## Why this lives in `did-wallet-sdk-android`

The Web reference implementation splits responsibilities across two repos:

```
blockchain/core/message/canonical-cbor.ts   ← the spec (≈600 lines)
                          ↓ import
abt-wallet/src/libs/chain/tx-codec.ts       ← wallet-internal adapter (≈200 lines)
```

Same split applies on Android. This module (the SDK) corresponds to the
blockchain side: it owns the wire format and nothing else. It does NOT know
about wallets, keys, app state, Client pools, or DID Connect. Everything
app-specific lives in `arc-wallet-android/AppCommonSDK/.../tx/TxCodec.kt` and
its callers (see `arc-wallet-android/planning/cbor-support/`).

Two good reasons to keep the split:

1. **Reusability.** iOS and other future clients can share a single spec
   doc + golden vectors. App-layer code is platform-specific; spec code is not.
2. **Review surface.** The encoder is a protocol contract with blockchain —
   changes here must be reviewed together with the chain side. Mixing it with
   wallet UX code makes that review harder.

## Files in this dir

| File | Purpose |
|---|---|
| `spec.md` | Full canonical CBOR Transaction wire format rules. Derived from `blockchain/core/message/src/canonical-cbor.ts` with the rules enumerated explicitly. If the Web reference changes, update this doc first, then code. |
| `kotlin-port.md` | Android implementation guide — API surface, dependency choice, known traps, line-by-line mapping from the TypeScript source. |

## Source of truth

The TypeScript reference implementation:

```
/Users/zac/work/arcblock/blockchain/core/message/src/canonical-cbor.ts
```

Golden test vectors (5 fixtures with `.input.json` + `.cbor.bin` pairs):

```
/Users/zac/work/arcblock/blockchain/core/message/tests/canonical-cbor-vectors/
```

Copies are in `/Users/zac/work/arcblock/arc-wallet-android/planning/cbor-support/vectors/`
so the app team has them next to their tests.

## Status

- Spec doc: first draft, matches `canonical-cbor.ts` as of blockchain
  @ 1.30.9.
- Kotlin implementation: not started. See `kotlin-port.md` §"Implementation
  checklist" for the entry point.
