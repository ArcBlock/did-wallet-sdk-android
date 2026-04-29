# Manual Release Runbook — canonical-cbor + tx-codec (Sonatype Central Portal)

The active procedure for cutting a release of `sdk-canonical-cbor` and
`sdk-tx-codec` to Maven Central since `1.0.15` (April 2026). Walk this
list end-to-end when shipping a new version.

The two modules ship together as a coordinated release; their version
numbers stay in lockstep with the parent SDK's root `version` file.

## Status

- ✅ `canonical-cbor` module — 72 tests, byte-exact against 15 golden
  vectors plus full encode/decode round-trip
- ✅ `tx-codec` module — 31 tests, including 10 cross-encoder tests
  that verify bit-for-bit compatibility with abt-wallet's TypeScript
  pipeline
- ✅ Bytes-first public API for tx-codec (no Transaction class leaks
  across module boundary; safe to depend on alongside the existing
  javalite `sdk-protobuf` artifact)
- ✅ `1.0.15` published to Maven Central via Sonatype Central Portal
  (2026-04-29) — first release after Sonatype sunset the legacy
  OSSRH (`s01.oss.sonatype.org`) on 2025-06-30 and after the
  `io.arcblock.did` namespace was reclaimed under the org account.

## How publishing works (high level)

```
maintainer's workstation                                Sonatype                     Maven Central
─────────────────────────                               ────────                     ─────────────
gradle publishAndReleaseToMavenCentral
  ├─ build + sign each artifact (jar / sources / javadoc / pom / module)
  ├─ POST staging deployment ─────────────► Central Portal API
  ├─                                          ├─ validate signatures (resolves
  ├─                                          │   pubkey via keys.openpgp.org)
  ├─                                          ├─ validate POM completeness
  ├─                                          ├─ validate group ownership
  ├─                                          └─ promote → Maven Central ─────────► repo1.maven.org
  └─ exit 0                                                                          (5–30 min mirror)
```

CI auto-publish on tag push is **not** wired up yet. See issue #18 for the
plan to remove this single-point-of-failure.

## One-time publisher setup

These steps you do **once per maintainer machine**. Skip to "Pre-release
verification" if your `~/.gradle/gradle.properties` already has the
`mavenCentralUsername` / `mavenCentralPassword` / `signing.gnupg.keyName`
keys.

### 1.1 GPG signing key

Maven Central rejects unsigned artifacts. You need an RSA key the
publisher controls + the public half resolvable on a public keyserver
that Sonatype queries (`keys.openpgp.org`).

```bash
# Pinentry GUI dialog for passphrase entry (Terminal.app must be open)
brew install pinentry-mac
mkdir -p ~/.gnupg
echo "pinentry-program /opt/homebrew/bin/pinentry-mac" > ~/.gnupg/gpg-agent.conf
chmod 600 ~/.gnupg/gpg-agent.conf
gpgconf --kill gpg-agent && gpgconf --launch gpg-agent

# Generate the key (uses pinentry-mac for passphrase entry).
# Use your @arcblock.io email — NOT a personal Gmail; that was the
# paper-era mistake that cost us the namespace when he left.
gpg --quick-generate-key "Your Name <you@arcblock.io>" rsa4096 default 4y

# Note the long key id and fingerprint
gpg --list-secret-keys --keyid-format=long you@arcblock.io
```

Push the **public** key to keyservers + verify your email:

```bash
KEY=<your-long-key-id>
gpg --keyserver hkps://keys.openpgp.org    --send-keys $KEY
gpg --keyserver hkps://keyserver.ubuntu.com --send-keys $KEY
```

`keys.openpgp.org` will email you a verification link — **click it**.
Without that, the keyserver hides your email/name from public queries
and Sonatype can't resolve "this signature came from an authorized
publisher".

Verify propagation (wait 1–3 hours after upload):

```bash
gpg --keyserver hkps://keys.openpgp.org --recv-keys $KEY
# Expected: "imported: 1" with your name+email shown
```

### 1.2 Central Portal user token

The `io.arcblock.did` namespace lives at
[https://central.sonatype.com](https://central.sonatype.com) under the
ArcBlock org account. To publish, get added as a publisher to that
namespace and generate a user token:

1. Sign in to [central.sonatype.com](https://central.sonatype.com) with
   the account that has write access to the `io.arcblock.did` namespace
2. Account → **View Account** → **Generate User Token**
3. The page shows an XML snippet — copy the `<username>` and
   `<password>` strings (you cannot retrieve them again; revoke + regenerate
   if lost)

### 1.3 `~/.gradle/gradle.properties`

```bash
chmod 600 ~/.gradle/gradle.properties   # if creating new, run after writing
```

Append (replace placeholders):

```
# ArcBlock SDK Maven Central publishing
mavenCentralUsername=<central-portal-user-token-name>
mavenCentralPassword=<central-portal-user-token-secret>
signing.gnupg.keyName=<last-8-hex-of-your-gpg-long-key-id>
```

`signing.gnupg.keyName` tells the Gradle signing plugin which key to use.
The last 8 hex chars of the long key id is enough.

**Do not commit this file to any repo.** It contains live publish
credentials.

## Pre-release verification

```bash
cd did-wallet-sdk-android

cd canonical-cbor && gradle clean test && cd ..
cd tx-codec      && gradle clean test && cd ..
```

Both should report `BUILD SUCCESSFUL` with all tests green:

- `canonical-cbor`: 72 tests
- `tx-codec`: 31 tests

If either fails, fix before publishing.

## 2. Bump version

```bash
cd did-wallet-sdk-android
echo "1.0.16" > version           # or whatever the next version is
git diff version                  # confirm one-line bump
```

This single file is read by every module's `build.gradle` to derive
the Maven `version` field. No per-module version edits required.

Open a small PR for this commit — branch protection on `main` blocks
direct pushes. Merge before going to step 4.

## 3. Local Maven publish (smoke test)

```bash
cd did-wallet-sdk-android/canonical-cbor
gradle clean publishToMavenLocal

cd ../tx-codec
gradle clean publishToMavenLocal
```

Verify the artifacts landed:

```bash
ls ~/.m2/repository/io/arcblock/did/sdk-canonical-cbor/<VERSION>/
ls ~/.m2/repository/io/arcblock/did/sdk-tx-codec/<VERSION>/
```

Each should contain 5 files + 5 detached signatures:

```
sdk-canonical-cbor-<v>.jar              + .jar.asc
sdk-canonical-cbor-<v>-sources.jar      + .jar.asc
sdk-canonical-cbor-<v>-javadoc.jar      + .jar.asc
sdk-canonical-cbor-<v>.pom              + .pom.asc
sdk-canonical-cbor-<v>.module           + .module.asc
```

Spot-check a signature:

```bash
gpg --verify ~/.m2/repository/io/arcblock/did/sdk-canonical-cbor/<v>/sdk-canonical-cbor-<v>.jar.asc \
            ~/.m2/repository/io/arcblock/did/sdk-canonical-cbor/<v>/sdk-canonical-cbor-<v>.jar
# Expected: "Good signature from 'Your Name <you@arcblock.io>'"
```

If signing fails: pinentry-mac dialog never popped up (gpg-agent stuck);
restart with `gpgconf --kill gpg-agent && gpgconf --launch gpg-agent`.

## 4. Verify arc-wallet-android can resolve mavenLocal copies

`arc-wallet-android/config.gradle` declares
`io.arcblock.did:sdk-canonical-cbor:<version>` and
`io.arcblock.did:sdk-tx-codec:<version>`. The wallet build already has
`mavenLocal()` first in the resolution order (see `build.gradle`
`allprojects.repositories`), so the artifacts you just published locally
will win over Maven Central.

```bash
cd arc-wallet-android
./gradlew :AppCommonSDK:compileDebugKotlin
```

Should compile cleanly. If a class fails to resolve, the artifact's
contents are bad — fix and re-publish locally before going to step 5.

## 5. Real publish to Sonatype Central Portal

Bump `version` (step 2) must be merged on `main` before this. Run from
the SDK repo root:

```bash
cd did-wallet-sdk-android
git checkout main && git pull origin main
make maven-cbor
```

What this does (per module):

- builds artifacts
- signs them via local `gpg` (pinentry-mac may pop up once for passphrase
  on first sign per gpg-agent session)
- POSTs a staging deployment to the Central Portal API
- because `publishToMavenCentral('CENTRAL_PORTAL', true)` in build.gradle
  passes `automaticRelease = true`, Sonatype auto-validates and
  auto-promotes once validation passes — no UI click needed

Expected output: each module ends with `BUILD SUCCESSFUL`. Total wall
clock ≈ 3 minutes per module.

If signing fails:

- `error receiving key from agent: No passphrase given` → pinentry-mac
  didn't show. Run `gpgconf --kill gpg-agent && gpgconf --launch gpg-agent`
  and retry.

If upload fails:

- `401 Unauthorized` → `mavenCentralUsername` / `mavenCentralPassword`
  in `gradle.properties` are wrong. Generate a fresh token in the Portal.
- `403 Forbidden ... namespace` → your token is for a different namespace.
  Confirm the namespace UUID at
  [central.sonatype.com/publishing/namespaces](https://central.sonatype.com/publishing/namespaces).

## 6. Verify Sonatype validation + Maven Central sync

Open [central.sonatype.com](https://central.sonatype.com) → **Deployments**.
Two staging deployments should appear:

- `io.arcblock.did:sdk-canonical-cbor:<v>`
- `io.arcblock.did:sdk-tx-codec:<v>`

State progression: `PENDING → VALIDATING → VALIDATED → PUBLISHING → PUBLISHED`.
Total ≈ 5–15 minutes. Then 5–30 more minutes for Maven Central mirror sync.

If you see `FAILED`, click into the deployment to see why. Most common
reasons:

- Signature couldn't be validated (public key unreachable on
  keys.openpgp.org — email verification not done?)
- POM missing required field (shouldn't happen if you didn't touch
  the pom blocks in `build.gradle`)

Verify Maven Central availability:

```bash
for art in sdk-canonical-cbor sdk-tx-codec; do
  for ext in .pom .jar -sources.jar -javadoc.jar; do
    url="https://repo1.maven.org/maven2/io/arcblock/did/${art}/<v>/${art}-<v>${ext}"
    code=$(curl -sI "$url" -o /dev/null -w "%{http_code}")
    echo "  ${code} ${url}"
  done
done
```

All 8 should return `200`. If still `404` after 30 minutes, check the
Sonatype deployment status — it may be stuck.

## 7. Tag the release

After all 8 artifacts are confirmed `200` on Maven Central:

```bash
cd did-wallet-sdk-android
git tag -a v<VERSION> -m "Release <VERSION>: <one-line summary>"
git push origin v<VERSION>
```

Tag push triggers `.github/workflows/release.yml`, which:

- Skips the (currently disabled) auto-publish step — see issue #18
- Creates a GitHub Release for the tag
- Sends a Slack notification to the clients channel

Verify the workflow turned green at
[github.com/ArcBlock/did-wallet-sdk-android/actions](https://github.com/ArcBlock/did-wallet-sdk-android/actions).

## 8. Bump downstream consumers

`arc-wallet-android/config.gradle` pins coordinates per module:

```groovy
"canonical-cbor": "io.arcblock.did:sdk-canonical-cbor:<v>"
"tx-codec":       "io.arcblock.did:sdk-tx-codec:<v>"
```

Open a PR there to bump both lines. The wallet's `build.gradle` already
has `mavenLocal()` + Maven Central in the resolution order, so the
new version will be picked up cleanly once Maven Central has the
artifacts.

If the wallet PR was on `WIP:` because it was waiting for the SDK
release, also drop the `WIP:` prefix and take the PR out of draft.

## 9. Notify cross-team

For releases that include CBOR / DID Connect protocol changes (every
release after this checklist applied), notify the payment-kit team —
they need to know which Android SDK version is the staging-handshake
floor. See
`arc-wallet-android/planning/cbor-support/staging-integration-checklist.md`.

## Rollback

Sonatype Central Portal releases are **immutable** — there is no delete
button. To "fix" a bad version:

- Publish a higher version with the fix
- Yank the previous version (the Portal supports a "Republish" /
  "Withdraw" action under certain conditions, but assume immutability
  for safety)

For local developer machines: `rm -rf ~/.m2/repository/io/arcblock/did/sdk-{canonical-cbor,tx-codec}/<v>/`
to force re-resolve from Maven Central.

`arc-wallet-android` consumers can pin to the previous version (or omit
the dep) until the fix lands.

## See also

- `kotlin-port.md` — implementation guide
- `spec.md` — wire format spec (the protocol contract)
- `arc-wallet-android/planning/cbor-support/staging-integration-checklist.md` — next phase
- Issue #18 — wire CI auto-publish to remove the single-point-of-failure
