# 钱包安全审计报告：did-wallet-sdk-android

## 执行摘要

- **审计范围**：/Users/nategu/work/arcblock/did-wallet/did-wallet-sdk-android
- **审计日期**：2026-05-11（基于 2026-05-09 报告更新）
- **审计版本**：did-wallet-sdk-android (master 分支)
- **总体风险**：中高
- **核心问题**：JWT 验证逻辑缺陷、不安全的依赖解析。
- **高优先级修复项**：
    1. 修正 JWT 时间戳单位不一致的问题。
    2. 禁用不安全的 HTTP Maven 协议。
    3. 为 PASSKEY 实现“Fail-Closed（失败即关闭）”逻辑。

## 审计方法

- **静态审查**：验证了前次审计报告（2026-05-09）中的发现。对手动审阅 `DIDTokenResponse.kt`、`DidAuthUtils.kt` 和 `settings.gradle`。
- **工具使用**：`audit-wallet-security` 参考审计流程。
- **审计限制**：仅限静态分析。

## 发现汇总

| 编号 | 严重级别 | 状态 | 领域 | 标题 | 影响仓库 |
| --- | --- | --- | --- | --- | --- |
| SDK-AND-001 | 高 (High) | 已确认 | 认证安全 | JWT 时间戳单位不一致（秒 vs 毫秒） | did-wallet-sdk-android |
| SDK-AND-002 | 高 (High) | 已确认 | 供应链 | 允许使用不安全的 HTTP Maven 协议 | did-wallet-sdk-android |
| SDK-AND-003 | 中 (Medium) | 已确认 | 密钥类型 | PASSKEY 失败时仍开放（返回空签名） | did-wallet-sdk-android |

## 详细发现

### SDK-AND-001：JWT 时间戳单位不一致

- **严重级别**：高 (High)
- **状态**：已确认
- **影响文件**：`wallet-sdk/src/main/java/io/arcblock/walletkit/bean/DIDTokenResponse.kt`、`wallet-sdk/src/main/java/io/arcblock/walletkit/did/DidAuthUtils.kt`
- **资产影响**：认证失败或拒绝服务攻击。
- **攻击场景**：`createDidAuthToken` 使用秒作为 `exp` 和 `nbf` 的单位，但 `verifyJWTExpired` 在调用方传入 `System.currentTimeMillis()` 时期望的是毫秒。这导致合法的 Token 也会因为被判定为过期而被拒绝。
- **证据**：`DidAuthUtils.kt:32` 使用了 `currentTimestamp/1000`，而 `DIDTokenResponse.kt:40` 直接将 `currentTimestamp`（毫秒）与 `exp`（秒）进行比较。
- **根本原因**：SDK 内部时间单位使用不统一。
- **修复建议**：在所有 JWT 时间戳字段和校验逻辑中统一使用秒（Epoch Seconds）。

### SDK-AND-002：允许使用不安全的 HTTP Maven 协议

- **严重级别**：高 (High)
- **状态**：已确认
- **影响文件**：`settings.gradle`
- **资产影响**：可能通过中间人攻击（MITM）导致供应链被污染。
- **攻击场景**：允许在 Maven 仓库中使用不安全的 HTTP 协议，使得攻击者可以进行中间人攻击，将合法的依赖包替换为恶意包。
- **证据**：`settings.gradle` 中包含针对 HTTP URL 的 `allowInsecureProtocol(true)` 配置。
- **修复建议**：对所有 Maven 仓库强制执行 HTTPS，并禁用不安全协议。

### SDK-AND-003：PASSKEY 失败时仍开放（Fail-Open）

- **严重级别**：中 (Medium)
- **状态**：已确认
- **影响文件**：`wallet-sdk/src/main/java/io/arcblock/walletkit/did/signer/Signer.kt`
- **资产影响**：可能绕过签名验证。
- **攻击场景**：对于 `PASSKEY` 密钥类型，签名器返回的是空数组而不是抛出错误。如果调用方未正确检查签名是否为空，这种“失败即开放”的行为可能导致严重的安全漏洞。
- **证据**：`Signer.kt` 在 `sign` 方法中针对 `PASSKEY` 类型返回了 `ByteArray(0)`。
- **根本原因**：新密钥类型仅实现了部分功能。
- **修复建议**：对于尚未实现的密钥类型（如 `PASSKEY`），应抛出 `UnsupportedOperationException`。

## 正向控制措施

- **TDD 开发模式**：仓库拥有完善的单元测试套件。
- **关键 Bug 已修复**：之前报告的 `verifyJWTDID` 中将 `==` 写成 `!=` 的严重问题在当前版本中已得到修复。

## 建议后续步骤

1. 统一 JWT 时间戳单位 (SDK-AND-001)。
2. 加固 Maven 仓库配置 (SDK-AND-002)。
3. 升级 Gson、Protobuf 和 BouncyCastle 等依赖项版本。
