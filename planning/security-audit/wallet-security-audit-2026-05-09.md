# did-wallet-sdk-android 钱包安全审计报告

- 审计日期：2026-05-09
- 审计对象：`/Users/nategu/work/arcblock/did-wallet/did-wallet-sdk-android`
- 审计方式：使用 `audit-wallet-security` skill 进行只读审查，包括仓库库存、静态源码审阅、Gradle/CI/供应链配置检查、关键单测执行和公开 advisory 抽样。
- 验证命令：`./gradlew --no-daemon :wallet-sdk:testDebugUnitTest`
- 验证结果：通过，`BUILD SUCCESSFUL`

## 仓库状态

审计期间没有修改源码。审计前已存在如下未提交删除，均为 `.idea/*` 文件，本报告未处理这些变更：

```text
 D .idea/.gitignore
 D .idea/androidTestResultsUserPreferences.xml
 D .idea/appInsightsSettings.xml
 D .idea/compiler.xml
 D .idea/deploymentTargetDropDown.xml
 D .idea/deploymentTargetSelector.xml
 D .idea/graphql-settings.xml
 D .idea/kotlinc.xml
 D .idea/misc.xml
 D .idea/render.experimental.xml
```

## 结论摘要

本轮发现的最高风险问题是 DID/JWT issuer 与公钥绑定判断写反，属于认证逻辑级漏洞。供应链方面，仓库存在 HTTP Maven 仓库、默认 `mavenLocal()`、未 pin GitHub Actions SHA、发布链路偏人工本机化、依赖版本陈旧且缺少锁定和校验。密码学工具层面，存在 ECB AES、弱 KDF、1024-bit RSA 默认值、PASSKEY 未实现但返回空签名/零公钥等不适合钱包 SDK 的 fail-open/误用风险。

优先修复顺序建议：

1. 修复 DID/JWT issuer 与公钥绑定判断，并补测试。
2. 统一 JWT 时间单位，补边界测试。
3. 移除 HTTP Maven 和默认 `mavenLocal()`，启用依赖锁定与校验。
4. 废弃或替换不安全 crypto helper。
5. 收紧 CI/release 权限和 action pinning。
6. 系统升级依赖并接入 OSV/dependency-check 类扫描。

## 发现详情

### Critical：DID/JWT issuer 与公钥绑定判断写反

证据：

- `wallet-sdk/src/main/java/io/arcblock/walletkit/bean/DIDTokenResponse.kt:32`
- `wallet-sdk/src/main/java/io/arcblock/walletkit/bean/DIDTokenBody.kt:31`

当前逻辑：

```kotlin
return (IdGenerator.pk2did(
  pk, DidType.getDidTypeByAddress(iss)
) != iss) && DidAuthUtils.verifyJWTSig(token, pk, DidUtils.decodeSignTypeByPk(pk).name)
```

问题：

这里使用了 `!=`，导致合法的 `pk -> did == iss` 反而被拒绝；同时，只要签名能用传入 `pk` 验过，并且该 `pk` 派生 DID 不等于 token body 中的 `iss`，DID 绑定条件就为真。如果调用方把 `pk` 从 token、响应或非可信远端数据中取得，攻击者可能使用自己的 key 签署一个声称来自受害 DID/app DID 的 token。

影响：

- 破坏 DID/JWT issuer 与公钥之间的认证绑定。
- 正常 token 可能被错误拒绝。
- 错误调用模式下可能接受 issuer/key 不匹配的 token。

建议：

- 将 `!= iss` 改为 `== iss`。
- 添加测试：
  - issuer 与 public key 匹配且签名正确时必须返回 `true`。
  - issuer 与 public key 不匹配时必须返回 `false`。
  - 签名正确但 `iss` 被篡改时必须返回 `false`。
- 验证流程应从可信 DID 文档解析 issuer public key，或至少先验证 `pk2did(pk) == iss` 再验签。

### High：JWT 过期校验时间单位不一致

证据：

- `wallet-sdk/src/main/java/io/arcblock/walletkit/bean/DIDTokenResponse.kt:40`
- `wallet-sdk/src/main/java/io/arcblock/walletkit/bean/DIDTokenBody.kt:39`
- `wallet-sdk/src/main/java/io/arcblock/walletkit/did/DidAuthUtils.kt:38`

当前注释说明 `currentTimestamp` 的单位是毫秒，但 token 生成时 `exp`、`iat`、`nbf` 使用的是 `currentTimestamp / 1000`，即秒。

影响：

- 调用方按注释传入 `System.currentTimeMillis()` 时，token 几乎都会被判过期。
- 调用方若传秒才能通过，又与 API 注释冲突。
- 可能造成认证 DoS，或诱导上层实现失败后的不安全降级逻辑。

建议：

- 统一使用 epoch seconds，或在校验函数内部将毫秒显式转换为秒。
- 重命名参数或修正注释，避免调用方误用。
- 添加 `nbf`、`exp`、当前时间边界测试。

### High：默认供应链解析允许 HTTP Maven 仓库和 mavenLocal

证据：

- `settings.gradle:13`
- `settings.gradle:17`

当前配置包含：

```gradle
maven {
  allowInsecureProtocol(true)
  url "http://android-docs.arcblock.io/release"
}
mavenLocal()
```

问题：

- HTTP Maven 仓库允许中间人替换 artifact。
- `mavenLocal()` 让本机缓存或被污染 artifact 参与默认构建，降低 CI 与开发机的一致性。
- 对钱包 SDK 来说，默认构建链路必须假设 artifact 解析是高价值攻击面。

建议：

- 删除 HTTP 仓库，或改成 HTTPS 并校验 artifact 签名/checksum。
- 默认移除 `mavenLocal()`，仅在显式 opt-in profile 中启用。
- 启用 Gradle dependency verification 和 dependency locking。

### Medium：AES helper 使用 ECB 且 password KDF 过弱

证据：

- `wallet-sdk/src/main/java/io/arcblock/walletkit/utils/AESEcbUtil.java:21`
- `wallet-sdk/src/main/java/io/arcblock/walletkit/utils/AESEcbUtil.java:24`

当前实现使用：

```java
private static final String CipherMode = "AES/ECB/PKCS5Padding";
SecretKeySpec keySpec = new SecretKeySpec(HashUtil.SHA3(password.getBytes()), "AES");
```

问题：

- ECB 会泄露明文结构。
- 无认证标签，无法抵抗密文篡改。
- 直接 hash password，没有 salt 和 work factor，不适合作为密码派生。
- 异常路径中存在 `printStackTrace`/日志输出。

影响：

即使当前生产源码中未发现直接调用，该类是 SDK public helper，容易被下游用于钱包密钥、助记词或敏感数据加密。

建议：

- 标记 deprecated，并在后续版本移除。
- 替换为 AES-GCM 或 Tink AEAD。
- 如果必须支持 password-based encryption，使用 Argon2id/scrypt/PBKDF2WithHmacSHA256，并包含 salt、版本号、迭代参数。
- 不要吞异常返回 `null`，应返回明确错误。

### Medium：RSA helper 默认 1024-bit，OAEP 参数不明确

证据：

- `wallet-sdk/src/main/java/io/arcblock/walletkit/utils/RSAUtil.java:26`
- `wallet-sdk/src/main/java/io/arcblock/walletkit/utils/RSAUtil.java:85`

问题：

- `generateKey()` 默认使用 1024-bit RSA，不适合现代钱包安全基线。
- `RSA/ECB/OAEPPadding` 未显式指定 OAEP hash/MGF1 参数，在 Android/JCE 环境下容易落到 SHA-1 默认或产生跨端不一致。
- 注释仍提到 PKCS1Padding，和实际代码不一致，会误导调用方。

建议：

- 默认改为 3072-bit 或 4096-bit。
- 显式使用 `RSA/ECB/OAEPWithSHA-256AndMGF1Padding` 和 `OAEPParameterSpec`。
- 或移除 public RSA helper，改用 Tink HybridEncrypt/HybridDecrypt。

### Medium：PASSKEY 被建模但实现退化为空签名/零公钥

证据：

- `wallet-sdk/src/main/java/io/arcblock/walletkit/did/signer/Signer.kt:39`
- `wallet-sdk/src/main/java/io/arcblock/walletkit/did/signer/Signer.kt:75`
- `wallet-sdk/src/main/java/io/arcblock/walletkit/did/IdGenerator.kt:284`
- `wallet-sdk/src/main/java/io/arcblock/walletkit/did/DidKeyPair.kt:18`

问题：

- `Signer.sign(PASSKEY)` 返回空数组。
- `Signer.verify(PASSKEY)` 返回 `false`。
- `IdGenerator.getPkByType(PASSKEY)` 返回全零 32 字节。
- `DidKeyPair` 对 `PASSKEY` 没有分支，编译测试时也出现 non-exhaustive warning。

影响：

公开枚举和 DidType 暗示 PASSKEY 可用，但实际行为可能导致确定性地址、空签名、未初始化公钥或异常路径。安全能力未实现时应 fail-closed，而不是返回伪值。

建议：

- 未实现前直接抛 `UnsupportedOperationException("PASSKEY is not supported")`。
- 不要返回空签名或零公钥。
- 添加 PASSKEY 路径测试，确保所有入口 fail-closed。

### Medium：CI/release 供应链控制不足

证据：

- `.github/workflows/coverage.yml:12`
- `.github/workflows/coverage.yml:14`
- `.github/workflows/coverage.yml:26`
- `.github/workflows/release.yml:13`
- `.github/workflows/release.yml:18`
- `.github/workflows/release.yml:26`
- `.github/workflows/release.yml:30`
- `.github/workflows/release.yml:60`
- `.github/workflows/start.yml:13`
- `.github/workflows/start.yml:18`
- `Makefile:55`

问题：

- GitHub Actions 使用 tag 版本，例如 `actions/checkout@v2`、`actions/setup-java@v1`、`madrapps/jacoco-report@v1.2`，未 pin 到完整 commit SHA。
- workflow 未显式配置最小化 `permissions:`。
- `start.yml` 使用 `secrets.PERSONAL_ACCESS_TOKEN` checkout。
- release workflow 中自动 Maven 发布被注释/禁用，实际发布依赖 maintainer workstation 和 Makefile，本机发布链路缺少 CI provenance、SBOM、attestation 和环境审批。

建议：

- 第三方 action pin 到完整 commit SHA。
- 为每个 workflow/job 配置最小 `permissions:`。
- 尽量使用 `GITHUB_TOKEN` 和 fine-grained permissions，避免 PAT。
- 将 Maven Central 发布迁移到受保护 CI 环境，使用 OIDC/环境审批/短期凭据/签名 attestation。

### Medium：依赖陈旧，缺少 lock/verification

证据：

- `config.gradle`
- `wallet-sdk/build.gradle`
- `protobuf/build.gradle`

观察到的高风险旧依赖包括：

- `com.google.code.gson:gson:2.8.2`
- `com.google.protobuf:protobuf-java:3.6.1`
- `org.bouncycastle:bcprov-jdk15to18:1.69`
- `com.google.crypto.tink:tink-android:1.6.1`
- `org.web3j:core:4.6.0-android`
- `org.bitcoinj:bitcoinj-core:0.16.1`

同时未发现：

- `gradle.lockfile`
- `gradle/verification-metadata.xml`
- Dependabot/Renovate 配置
- OSV/dependency-check 配置

公开 advisory 抽样：

- protobuf-java：<https://osv.dev/vulnerability/GHSA-g5ww-5jh7-63cx>
- Gson：<https://osv.dev/vulnerability/GHSA-4jrv-ppp4-jm57>
- BouncyCastle：<https://osv.dev/vulnerability/GHSA-4h8f-2wvx-gg5w>

说明：

本轮没有运行完整依赖漏洞扫描器，以上是静态版本检查加公开 advisory 抽样。应使用 OSV Scanner、OWASP Dependency-Check 或同等级工具做完整确认。

建议：

- 引入依赖锁定和 Gradle verification metadata。
- 接入 Dependabot/Renovate。
- 在 CI 中加入 OSV Scanner 或 dependency-check。
- 优先升级 protobuf、Gson、BouncyCastle、Tink、web3j、bitcoinj 等安全敏感依赖。

### Medium/Low：sample app 允许备份

证据：

- `app/src/main/AndroidManifest.xml:6`

当前配置：

```xml
android:allowBackup="true"
```

影响：

如果 sample app 存储测试钱包、私钥、助记词、token 或其他敏感数据，adb/cloud backup 可能带走 app 数据。作为 SDK 示例，该配置也容易被下游复制。

建议：

- 钱包相关 app 默认设置 `android:allowBackup="false"`。
- 如必须允许备份，应通过 `dataExtractionRules` 或 `fullBackupContent` 明确排除敏感数据。

### Low：测试日志打印 seed/private key/signature 材料

证据：

- `wallet-sdk/src/test/java/io/arcblock/walletkit/SignerTest.kt`
- `wallet-sdk/src/test/java/io/arcblock/walletkit/IdGeneratorTest.kt`
- `wallet-sdk/src/androidTest/java/io/arcblock/walletkit/CipherRSATest.kt`

问题：

多个测试会打印 seed、private key、public key 或签名材料。当前多为测试 fixture，但 CI 日志通常长期留存，未来若测试材料变成真实敏感数据会形成泄露路径。

建议：

- 移除私钥、seed、助记词、签名材料的日志输出。
- 使用固定、明确标注的非敏感 fixture。
- CI 中对测试日志做敏感信息扫描。

## 需要进一步确认的问题

### ECDSA 签名输入是否已经做 domain separation 和 hashing

证据：

- `wallet-sdk/src/main/java/io/arcblock/walletkit/did/signer/Signer.kt:33`
- `wallet-sdk/src/main/java/io/arcblock/walletkit/did/DidUtils.kt:64`

`SECP256K1`/`ETHEREUM` 签名路径看起来直接对传入 `content` 调用 web3j `ECKeyPair.sign(content)`，verify 路径也直接验证 `data`。如果调用方传入的是已经 domain-separated 的 digest，则可能符合预期；如果调用方直接传 UI 字符串或未加域分隔的业务 payload，则存在跨协议重放和签名语义混淆风险。

建议：

- 明确 SDK API 合约：传入原文还是 digest。
- 对钱包签名类 API 强制 domain separation。
- 对链上签名路径确认低 S 规范和 canonical signature 要求。

### ArcJWT.verifyJWT 是签名验证，不是完整 issuer 认证

证据：

- `wallet-sdk/src/main/java/io/arcblock/walletkit/jwt/ArcJWT.kt:79`

`ArcJWT.verifyJWT` 只使用调用方传入的 `pk` 和 `version` 验证签名，没有验证 token `iss` 与 `pk` 的 DID 绑定。作为底层 primitive 可以接受，但 public API 名称容易让调用方误以为完成了 JWT 身份认证。

建议：

- 文档中明确该函数只验证签名。
- 提供高级 API：解析 token、验证 alg、验证 `iss -> DID document -> pk`、验签、校验 `nbf/exp/aud/nonce`。

## 当前测试覆盖缺口

`./gradlew --no-daemon :wallet-sdk:testDebugUnitTest` 通过，但未覆盖本轮最关键的认证绑定错误。建议新增以下测试集合：

1. `verifyJWTDID_validIssuerAndMatchingPk_returnsTrue`
2. `verifyJWTDID_validSignatureButMismatchedIssuer_returnsFalse`
3. `verifyJWTDID_tamperedIssuer_returnsFalse`
4. `verifyExpireTime_acceptsEpochSecondsWithinWindow`
5. `verifyExpireTime_rejectsExpiredToken`
6. `passkeySignAndPkGeneration_throwUnsupported`
7. `aesEcbUtil_isDeprecatedOrBlockedForWalletSecretUse`

## 建议整改路线

### P0

- 修复 `verifyJWTDID` 的 `!=` 为 `==`。
- 补认证绑定测试。
- 统一 JWT 时间单位。

### P1

- 删除 HTTP Maven 仓库和默认 `mavenLocal()`。
- 开启 Gradle dependency locking/verification。
- pin GitHub Actions 到完整 commit SHA。
- 为 workflow 配置最小化 `permissions:`。

### P2

- 废弃 `AESEcbUtil`。
- 提升 `RSAUtil` 默认安全基线或移除该 helper。
- PASSKEY 未实现路径全部改为明确 fail-closed。

### P3

- 全量升级依赖并接入自动 advisory 扫描。
- 将 Maven 发布迁移到受保护 CI 环境。
- 移除测试日志里的私钥/seed/signature 输出。
- sample app 关闭备份或增加明确数据排除规则。
