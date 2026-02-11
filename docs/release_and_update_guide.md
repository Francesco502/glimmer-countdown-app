# TimeAPK 发布与更新指南

本文档说明：**距离发布并输出可安装 APK 还差哪些步骤**、**如何预留更新渠道**，以及**建议的优化项**。

---

## 一、当前状态简要

| 项目           | 状态 |
|----------------|------|
| 应用 ID / 版本 | ✅ 已配置 `applicationId`、`versionCode`、`versionName`，支持 gradle.properties 覆盖 |
| 最低/目标 SDK  | ✅ minSdk 26, targetSdk 34 |
| Release 构建   | ✅ 有 `release` buildType，未开启混淆（可按需开启） |
| **签名配置**   | ✅ **已预留**：存在 `keystore.properties` 时自动为 release 签名 |
| .gitignore     | ✅ 已添加 `keystore.properties`、`*.keystore`，避免密钥进库 |
| 设置页「检查更新」 | ✅ 已增加入口，当前提示「已是最新版本」，便于后续接入真实 API |
| 关于/版本展示   | ✅ 设置页展示当前 `versionName`（BuildConfig.VERSION_NAME） |
| 更新服务接口   | ✅ 已预留 `UpdateChecker`、`CheckUpdateResult`、`StubUpdateChecker` |
| 渠道包         | ✅ 已配置 `direct` / `play` 两渠道（productFlavors） |
| 更新机制（真实接口） | ⏳ 未实现（无下载/安装流程），接口已预留 |

---

## 二、发布 APK 还差的步骤

### 1. 配置签名（必须）

Release 包必须签名后用户才能安装。

**步骤：**

1. **生成签名密钥库（仅做一次）**
   - 若系统未将 `keytool` 加入 PATH，请用 JDK 完整路径运行（Windows 下常见路径）：
   ```powershell
   & "C:\Program Files\Java\jdk-17.0.2\bin\keytool.exe" -genkey -v -keystore timeapk-release.keystore -alias timeapk -keyalg RSA -keysize 2048 -validity 10000
   ```
   - 在项目根目录（如 `D:\LLT\Code\TimeAPK`）打开 PowerShell 或 CMD 执行上述命令；若 JDK 路径不同，请把 `jdk-17.0.2` 改成你本机的 JDK 目录名。
   - 按提示输入密钥库密码（至少 6 位）、姓名、组织等并牢记密码与 alias。

2. **在 `app/build.gradle.kts` 中配置 signingConfigs**
   - 使用 `keystore.properties`（不提交）存放路径与密码，或在 CI 中用环境变量。
   - 在 `buildTypes.release` 中引用该 signingConfig。
   - 具体写法见下文「Gradle 预留配置」小节。

3. **安全注意**
   - 将 `keystore.properties`、`*.keystore` 加入 `.gitignore`。
   - 正式环境用 CI 变量注入密码，避免写死在仓库。

**生成 keystore 后的具体操作：**

1. 确认 `timeapk-release.keystore` 在**项目根目录**（与 `keystore.properties` 同级）。
2. 编辑项目根目录的 **`keystore.properties`**，把占位符换成你生成 keystore 时设置的密码：
   - `storePassword=` 后面填**密钥库密码**
   - `keyPassword=` 后面填**密钥密码**（若当时回车沿用密钥库密码，则填同一密码）
   - `storeFile=timeapk-release.keystore` 保持不变（若 keystore 在根目录）
   - `keyAlias=timeapk` 保持不变
3. 保存后执行下面「完成上述后，执行」中的构建命令即可打出已签名的 release APK。

完成上述后，执行：

```bash
# 直装渠道（默认）
./gradlew assembleDirectRelease

# 或 Play 渠道
./gradlew assemblePlayRelease
```

产物路径：`app/build/outputs/apk/direct/release/` 或 `app/build/outputs/apk/play/release/`，APK 可直接发给用户安装（或上架应用商店）。

### 2. 可选：启用代码与资源压缩（推荐）

- 在 `app/build.gradle.kts` 的 `buildTypes.release` 中：
  - `isMinifyEnabled = true`
  - `isShrinkResources = true`
- 确保 `proguard-rules.pro` 存在并保留 Room、Compose、反射等必要规则，避免 release 运行异常。

### 3. 可选：发布为 AAB（Google Play）

- 若上架 Google Play，应使用 App Bundle：
  ```bash
  ./gradlew bundleRelease
  ```
- 产物：`app/build/outputs/bundle/release/app-release.aab`，上传 Play 后台即可。

### 4. 发布前检查清单

- [ ] 已配置 release 签名（在项目根目录添加 `keystore.properties` 并填写密钥信息）并成功 `assembleRelease` 或 `assembleDirectRelease` / `assemblePlayRelease`
- [ ] 版本号：每次发布递增 `versionCode`，并更新 `versionName`（如 1.0 → 1.1）
- [ ] 应用名称、图标、权限与隐私说明与实际上架渠道一致
- [ ] 如需要：隐私政策链接、应用内展示的版本号与 `versionName` 一致

---

## 三、预留“以后更新”的渠道

目标：**不改变现有逻辑即可在将来接入任意一种更新方式**。

### 1. 版本信息统一管理（已具备，需坚持）

- 在 `app/build.gradle.kts` 的 `defaultConfig` 中集中维护：
  - `versionCode`：整数，每次发布递增（用于比较新旧版本）。
  - `versionName`：用户可见，如 `"1.0"`、`"1.1.0"`。
- 建议：后续可将 `versionName` 抽到 `gradle.properties` 或单独 `version.gradle.kts`，便于 CI 或脚本统一改版。

### 2. 预留“更新渠道”的几种方式

| 方式 | 说明 | 何时选用 |
|------|------|----------|
| **Google Play 应用内更新** | 使用 Play Core 的 In-App Update API，由 Play 负责下载与安装 | 仅上架 Google Play 时 |
| **自建/第三方更新** | 自己或第三方（如 蒲公英、fir.im）提供“当前最新版本号 + APK 下载链接”，应用内检查版本并下载 APK、引导安装 | 官网/自有服务器分发、国内渠道 |
| **渠道包（Product Flavors）** | 同一套代码打出多个渠道包（如 `play`、`direct`、`huawei`），便于统计与分渠道更新策略 | 需要区分渠道统计或不同商店/渠道时 |

**已完成的预留：**

1. **在设置页留入口** ✅
   - 已增加「检查更新」入口，点击后显示 Snackbar「已是最新版本」，后续可接入真实检查逻辑。

2. **抽象“更新服务”接口** ✅
   - 已定义 `UpdateChecker`、`CheckUpdateResult`（`update` 包），以及 `StubUpdateChecker` 占位实现。
   - 后续实现 `PlayStoreUpdateChecker` 或 `CustomServerUpdateChecker` 并注入到设置页即可。

3. **渠道包** ✅
   - 已在 `app/build.gradle.kts` 中配置 `flavorDimensions = "channel"`，`productFlavors`：`direct`（默认）、`play`（applicationIdSuffix = ".play"）。
   - 构建：`assembleDirectRelease`、`assemblePlayRelease`。

### 3. 后续实现“检查更新 + 安装”时需注意

- **Android 8+ 安装 APK**：需要 `REQUEST_INSTALL_PACKAGES` 权限，并使用 `PackageInstaller` 或 `Intent(ACTION_VIEW)` 打开下载好的 APK。
- **下载方式**：可用 `DownloadManager` 或 OkHttp 等下载到 app 私有目录，再触发安装。
- **HTTPS**：下载链接必须使用 HTTPS，避免被系统或安全策略拦截。

---

## 四、建议的优化（与发布/更新相关）

### 1. 与发布直接相关

- **签名与安全**
  - 完成上述签名配置，并确保密钥与密码不进仓库、用 CI/本地安全存储。
- **版本号**
  - 建立规则：每次发布前在 `build.gradle.kts`（或集中版本文件）中递增 `versionCode` 并更新 `versionName`。
- **Release 体积与稳定性**
  - 开启 `isMinifyEnabled` / `isShrinkResources`，并完善 ProGuard 规则；发布前用 release 包做一次完整测试。

### 2. 与“更新渠道”相关

- **设置页** ✅ 已增加「检查更新」入口，当前提示“已是最新版本”；可后续接入 `UpdateChecker` 实现。
- **关于/版本** ✅ 设置页已展示当前 `versionName`（`BuildConfig.VERSION_NAME`），便于用户反馈时说明版本。

### 3. 与产品体验相关（来自《优化建议》）

- 继续推进《优化建议.md》中的项：空状态、卡片状态（已过期）、编辑/删除、筛选排序、主题与无障碍等，会在发布后显著提升口碑与留存。

---

## 五、Gradle 配置说明（已就绪）

- **签名**：根目录存在 `keystore.properties` 时，`app/build.gradle.kts` 会自动读取并为 release 配置签名。格式示例：
  ```properties
  storeFile=timeapk-release.keystore
  storePassword=xxx
  keyAlias=timeapk
  keyPassword=xxx
  ```
- **版本**：支持通过 `gradle.properties` 覆盖 `VERSION_CODE`、`VERSION_NAME`，便于 CI/脚本统一改版。
- **渠道**：已配置 `direct`、`play` 两个 productFlavors，可按需执行 `assembleDirectRelease` / `assemblePlayRelease`。

---

## 六、总结

| 类别     | 状态与建议 |
|----------|------------|
| **发布 APK** | 在根目录配置 `keystore.properties` 后执行 `assembleDirectRelease` 或 `assemblePlayRelease` 即可产出可安装 APK；可选开启混淆与 AAB。 |
| **更新渠道** | 已预留：设置页「检查更新」、关于/版本展示、`UpdateChecker` 接口与渠道包（direct/play）。后续接入真实更新 API 即可。 |
| **优化**   | 签名与版本流程已规范化；可按需开启 minify/shrink 并测试；继续按《优化建议》完善体验。 |

当前工程已完成文档中列出的预留项，配置好签名即可发布；后续实现真实“检查更新 + 下载安装”时只需实现并注入 `UpdateChecker` 即可。
