# 安全修复计划：did-wallet-sdk-android

本计划整合了 2026-05-09 (Codex) 和 2026-05-11 (Gemini) 的审计发现，并根据风险等级梳理了分阶段的修复路径。

## 1. 审计报告整合

### 1.1 2026-05-11 审计报告 (Gemini)
> [!IMPORTANT]
> 完整内容详见：`./wallet-security-audit-2026-05-11.md`

**核心发现：**
- **SDK-AND-001 (High)**: JWT 时间戳单位不一致（秒 vs 毫秒）。
- **SDK-AND-002 (High)**: 允许使用不安全的 HTTP Maven 协议。
- **SDK-AND-003 (Medium)**: PASSKEY 失败路径未实现 Fail-Closed（返回空签名）。

---

### 1.2 2026-05-09 审计报告 (Codex)
> [!IMPORTANT]
> 完整内容详见：`./wallet-security-audit-2026-05-09.md`

**核心发现：**
- **Critical**: DID/JWT issuer 与公钥绑定判断写反 (`!=` vs `==`)。
- **High**: JWT 过期校验时间单位不一致。
- **High**: 默认供应链配置允许不安全的 HTTP Maven 仓库。
- **Medium**: AES helper 使用 ECB 模式且 KDF 过弱。
- **Medium**: RSA helper 默认 1024-bit。
- **Medium**: PASSKEY 被建模但实现退化为空签名。
- **Medium**: CI/Release 供应链控制不足（Action Pinning, Permissions）。

## 2. 审计对比分析

- **一致性**：两份报告均确认了时间戳单位错误、不安全的 Maven 协议以及 Passkey 的 Fail-Open 问题。
- **重大更新**：Codex 报告中列为 **Critical** 的 `verifyJWTDID` 逻辑错误（`!= iss`）在 05-11 的审计中已被确认为**已修复**。
- **状态验证**：当前代码已使用 `== iss`，说明之前的关键漏洞已被修补。

## 3. 分阶段执行计划

### 阶段 1：认证逻辑与配置加固 (High Priority)
- [ ] **统一时间单位 (SDK-AND-001)**：
    - 在 `DIDTokenResponse.kt` 和 `DidAuthUtils.kt` 中统一使用秒 (Epoch Seconds)。
    - 修正 `verifyJWTExpired` 逻辑。
- [ ] **禁用不安全协议 (SDK-AND-002)**：
    - 修改 `settings.gradle`，移除 `allowInsecureProtocol(true)`。
    - 强制所有 Maven 仓库使用 HTTPS。

### 阶段 2：密码学与 API 安全 (Medium Priority)
- [ ] **加固 Passkey 路径 (SDK-AND-003)**：
    - 将 Passkey 路径的返回空数组逻辑改为抛出 `UnsupportedOperationException`。
- [ ] **弃用不安全工具**：
    - 标记 `AESEcbUtil` 为 Deprecated。
    - 提升 `RSAUtil` 默认密钥长度。

### 阶段 3：工程化与供应链 (Governance)
- [ ] **依赖升级与锁定**：
    - 升级 Gson, Protobuf, BouncyCastle, Tink 等关键依赖。
    - 启用 Gradle dependency locking 和 verification metadata。
- [ ] **CI 加固**：
    - 将 GitHub Action pin 到 Commit SHA。
    - 配置显式的 `permissions` 权限。
- [ ] **清理测试日志**：移除测试代码中打印私钥、Seed 和签名的 `println` 语句。

## 4. 后续跟进
- 增加针对 JWT 过期（边界值）和 Passkey 异常路径的单元测试。
- 在 CI 中集成 `osv-scanner` 进行自动化漏洞扫描。
