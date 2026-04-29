# Release Checklist — canonical-cbor + tx-codec

Walk this list when promoting the two new modules to a real Maven
artifact that arc-wallet-android (and any other consumer) can depend on.

The two modules ship together as a coordinated release. Their version
numbers stay in lockstep with the parent SDK's `version` file
(currently `1.0.14` — bump to `1.0.15` for this release).

## Status before release

- ✅ `canonical-cbor` module — 72 tests, byte-exact against 15 golden
  vectors plus full encode/decode round-trip
- ✅ `tx-codec` module — 31 tests, including 10 cross-encoder tests
  that verify bit-for-bit compatibility with abt-wallet's TypeScript
  pipeline
- ✅ Bytes-first public API for tx-codec (no Transaction class leaks
  across module boundary; safe to depend on alongside the existing
  javalite `sdk-protobuf` artifact)
- ⏸ Not yet integrated into the SDK root build (root references
  obsolete jcenter() — see step 4 below)

## Pre-release verification

```bash
cd did-wallet-sdk-android

# canonical-cbor
cd canonical-cbor && gradle clean test && cd ..

# tx-codec (depends on canonical-cbor via composite build)
cd tx-codec && gradle clean test && cd ..
```

Both should report `BUILD SUCCESSFUL` with all tests green:
- `canonical-cbor`: 72 tests
- `tx-codec`: 31 tests

If either fails, fix before publishing.

## 1. Bump version

```bash
cd did-wallet-sdk-android
echo "1.0.15" > version
git diff version           # confirm one-line bump
```

This single file is read by every module's `build.gradle` to derive
the Maven `version` field. No per-module version edits required.

## 2. Local Maven publish (smoke test)

```bash
cd did-wallet-sdk-android/canonical-cbor
gradle publishToMavenLocal

cd ../tx-codec
gradle publishToMavenLocal
```

Verify the artifacts landed:

```bash
ls ~/.m2/repository/io/arcblock/did/sdk-canonical-cbor/1.0.15/
ls ~/.m2/repository/io/arcblock/did/sdk-tx-codec/1.0.15/
```

Each should contain `.jar`, `-sources.jar`, `-javadoc.jar`, `.pom`,
and `.module`.

## 3. Verify arc-wallet-android can resolve the artifacts

`arc-wallet-android/config.gradle` already declares the coordinates at
`io.arcblock.did:sdk-canonical-cbor:1.0.15` and
`io.arcblock.did:sdk-tx-codec:1.0.15`. Add `mavenLocal()` to the root
repositories block (likely `build.gradle` or `settings.gradle`) so the
just-published artifacts are picked up:

```groovy
repositories {
  mavenLocal()      // ← add at top
  google()
  mavenCentral()
}
```

Then trigger a build of `AppCommonSDK` only (avoids the broader
jcenter problem):

```bash
cd arc-wallet-android
./gradlew :AppCommonSDK:compileDebugKotlin
```

Note: at the time of writing the root build also references the
shut-down `jcenter()` repo, so a full `./gradlew build` will fail on
unrelated dependencies (TensorFlow, Apollo). That is a separate
modernization task — see `arc-wallet-android/planning/cbor-support/
README.md` Phase 3 caveats. The point of this step is just to confirm
that `AppCommonSDK`'s new `tx-codec` import resolves and compiles.

## 4. (Optional) Move the modules into the SDK root build

Both modules currently ship with their own `settings.gradle` because
the root `did-wallet-sdk-android/build.gradle` still uses
`jcenter()`. If/when the root is modernized:

1. Remove `did-wallet-sdk-android/canonical-cbor/settings.gradle`
2. Remove `did-wallet-sdk-android/tx-codec/settings.gradle`
3. In root `settings.gradle` uncomment the line:
   ```
   include ':canonical-cbor'
   ```
   and add:
   ```
   include ':tx-codec'
   ```
4. Rewrite both modules' `build.gradle` to use the legacy
   `apply plugin: 'kotlin'` style (per `:protobuf`) instead of the
   modern `plugins { … }` block — the root buildscript classpath
   pins ancient versions.

This step is **not** a prerequisite for releasing: the artifacts
publish fine from the standalone subprojects.

## 5. Sonatype staging publish

Configure Maven publish credentials in `~/.gradle/gradle.properties`:

```
NEXUS_USERNAME=<sonatype-jira-id>
NEXUS_PASSWORD=<sonatype-jira-pw>
signing.keyId=<gpg-key-id>
signing.password=<gpg-key-passphrase>
signing.secretKeyRingFile=/Users/<you>/.gnupg/secring.gpg
```

(Same secrets the existing `:protobuf` and `:wallet-sdk` modules use.)

Then publish:

```bash
cd did-wallet-sdk-android/canonical-cbor
gradle publish

cd ../tx-codec
gradle publish
```

Each module publishes to
`https://s01.oss.sonatype.org/service/local/staging/deploy/maven2`
configured in its `build.gradle`.

## 6. Promote staging → release

Log into https://s01.oss.sonatype.org with the Sonatype JIRA account.

For each module:
1. Open "Staging Repositories"
2. Find the latest closed staging repo (look for `iotxxx`)
3. Click "Close"
4. Wait for validation (≈5 min)
5. Click "Release"

After ≈30 min the artifacts are mirrored to Maven Central.

## 7. Post-release housekeeping

- [ ] Tag the SDK repo: `git tag -a sdk-canonical-cbor-1.0.15 -m "..."`
      and `git tag -a sdk-tx-codec-1.0.15 -m "..."`. Push tags
      (`git push --tags`).
- [ ] Update `arc-wallet-android/config.gradle` version pins to match
      whatever was actually published, if it differs from `1.0.15`.
- [ ] Open the consumer PR in arc-wallet-android (`docs/cbor-android-
      port-plan` branch holds the integration commits).
- [ ] Notify payment-kit team that Android is ready for staging
      handshake — see
      `arc-wallet-android/planning/cbor-support/staging-integration-
      checklist.md`.

## Rollback

If a problem surfaces after Sonatype release:

- Sonatype releases are **immutable** — no delete. You must publish
  a higher version number with the fix.
- Locally on a developer machine: `rm -rf ~/.m2/repository/io/arcblock/
  did/sdk-{canonical-cbor,tx-codec}/1.0.15/` to force re-resolve.
- arc-wallet-android consumer can pin to the previous version
  (or omit the dep) until the fix lands.

## See also

- `kotlin-port.md` — implementation guide
- `spec.md` — wire format spec (the protocol contract)
- `arc-wallet-android/planning/cbor-support/staging-integration-
  checklist.md` — next phase
