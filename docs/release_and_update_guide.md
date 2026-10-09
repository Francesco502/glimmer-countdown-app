# TimeAPK 发布与更新指引

本文档是 4.1 开发候选的签名、构建、验证与更新指引（2026-10-09）。4.1 尚未发布，最新公开版本仍为 v4.0；本版测试、设备、正式签名和上传结果以 RELEASE_CHECKLIST.md 为准。既有正式签名配置与 keystore 已取得且通过加载和证书核验，4.1 正式 APK 尚未构建；真机门未执行且未获本版豁免。

**唯一正式发布渠道：GitHub Release。** 唯一官方资产为 Direct APK `glimmer-countdown-4-1.apk`。Play flavor 仅保留用于兼容性与开发回归，不是 4.1 正式发布工件或阻断项。

## 一、当前状态

| 项目 | 状态 |
|------|------|
| `applicationId` / 版本号 | Direct：`com.example.timeapk` / `4.1`；Play：`com.example.timeapk.play` / `4.1`（仅开发回归） |
| 最低 / 目标 SDK | `minSdk 26` / `targetSdk 36` |
| Release 构建 | 已启用 `release` buildType，并开启 `minify` 与 `shrinkResources` |
| Release 签名 | 根目录 `keystore.properties` 与既有 `timeapk-release.keystore` 已取得；配置路径、私钥可用性及证书已核验；4.1 正式 APK 尚未构建 |
| Direct APK 命名 | 输出为 `glimmer-countdown-4-1.apk` |
| 正式渠道 | GitHub Release，只上传 Direct APK |
| Play flavor | 保留用于兼容性与开发回归；不产生正式资产或发布门 |

## 二、发布前准备

### 1. 签名配置

工作区中的既有正式密钥与配置已通过 Java Properties / JCA 加载核验。正式构建可使用 `TIMEAPK_KEYSTORE_PROPERTIES` 指定配置文件，或读取仓库根目录配置；不能通过新建证书绕过升级签名要求。配置字段示例如下：

```properties
storeFile=timeapk-release.keystore
storePassword=xxx
keyAlias=timeapk
keyPassword=xxx
```

上面仅为字段示例，不要复制真实值到终端输出、截图、issue 或提交。正式发布还需以安全环境变量提供正式证书指纹：

```text
GLIMMER_RELEASE_CERT_SHA256=<64位SHA-256证书指纹>
```

已取得密钥的正式证书 SHA-256 核验为 `3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`，与下载核验的线上 v4.0 APK 一致。4.1 正式 APK 的独立验签尚未执行；私钥、密码、配置内容和 GitHub token 不进入文档或日志。

确保以下文件不进入仓库：

- `keystore.properties`
- `*.keystore`

### 2. 版本确认

当前版本值：

- `VERSION_NAME=4.1`
- `VERSION_CODE=24`

这是开发候选版本，不代表本版已构建、已签名或已发布。正式发布前完成本版 [发布检查清单](RELEASE_CHECKLIST.md)，物理真机验证或针对本版的明确豁免不能继承 v4.0。

继续发布新版本时，应同步递增 `versionCode`，并更新 `versionName`、`README.md`、`CHANGELOG.md` 与发布文档。

## 三、构建命令

```bash
./gradlew testDirectDebugUnitTest
./gradlew compileDirectDebugAndroidTestKotlin
./gradlew lintDirectDebug lintDirectRelease lintVitalDirectRelease
./gradlew assembleDirectRelease
```

产物路径：

- `app/build/outputs/apk/direct/release/glimmer-countdown-4-1.apk`

Play flavor 的 Debug 测试、编译或安装仅用于开发回归，必须单独记录，且不是本页正式发布的前置条件。

## 四、GitHub 发布

固定发布顺序：最终代码与发布文档已提交，且工作区干净 → 创建并推送不可变的 exact tag → 从该 tag 对应 commit 的工作树重新正式签名构建 → 验证签名、精确证书指纹与 SHA-256 → 准备安全凭据环境 → 运行发布脚本。

### 1. 提交与推送

```bash
git add app gradle.properties README.md CHANGELOG.md docs scripts .gitignore
git commit -m "release: ship v4.1"
git push origin main
```

### 2. 标签

标签必须在代码、文档和检查结果全部确定后，创建于最终发布 commit：

```bash
git tag -a v4.1 -m "Release v4.1"
git push origin v4.1
```

脚本会分别解引用 annotated/lightweight tag，并要求本地与远端 tag 解引用后的 commit 精确一致。禁止 force-push、移动或复用已推送 tag，禁止覆盖已发布 Release；已发布后出现问题必须递增版本号。

标签推送后先核对 `git rev-parse HEAD` 与 `git rev-parse v4.1^{commit}` 一致，然后在该工作树执行 `./gradlew clean` 和本页“构建命令”。不得复用旧构建产物，也不要使用可能删除未跟踪文件的 `git clean`。用正式密钥完成 Direct APK 后，记录 SHA-256，并验证签名、精确证书指纹与安装权限。

### 3. Release

前置条件：正式签名 exact Direct APK 已从 tag commit 新鲜生成；`ANDROID_HOME` 可定位稳定版 `apksigner` 与 `aapt`；`GLIMMER_RELEASE_CERT_SHA256` 已安全注入；本地运行 `gh auth login` 后脚本可通过 `gh auth token` 取得具备 `GitHub Contents: write` 的凭据；本地和远端 `v4.1` tag 已指向最终发布 commit。CI 才通过仓库 secret 注入 `GITHUB_TOKEN`，且不得打印其值。

```powershell
$env:GLIMMER_RELEASE_CERT_SHA256 = "your_release_certificate_sha256"
$env:ANDROID_HOME = "your_android_sdk"
gh auth login
.\scripts\publish-release.ps1
```

不要在命令行中直接书写 token。GitHub CLI 可减少明文凭据暴露，但命令历史和凭据存储安全仍取决于本机配置，不能作绝对保证。

脚本会：

- 读取当前版本号；工作区存在 tracked / untracked 改动时拒绝发布
- 提取 `CHANGELOG.md` 中 `4.1` 小节作为 Release Notes
- 要求当前 `HEAD` 等于 exact 本地 tag commit，并在首次远端写入前再次复核
- 校验 `output-metadata.json` 的唯一 Direct artifact，再用 `aapt` 验证 APK 的真实包名、版本、非调试状态和安装包权限
- 校验正式证书指纹及本地/远端 tag commit 后创建 `refs/heads/release-locks/v4.1` Git ref 锁
- 创建带 `ownership marker` 的 draft；仅恢复带脚本自身 marker 的 draft，拒绝 published Release 和人工 draft
- 删除 owned draft 中的所有旧资产，再上传 exact Direct APK，并将响应及重新读取结果绑定到 asset id、size、digest、content type 和下载 URL
- 要求整个 Release 只保留唯一的 exact Direct APK，发现任何其他资产即拒绝发布
- 在发布前重新核对 ownership marker，并以最终 GET 验证公开 Release 与唯一 APK

发布进程并发或发现残留锁时脚本会拒绝继续。先调查是否仍有活跃发布进程、owned draft 或已发生的远端 mutation；不要随意删除活跃锁。只有确认是崩溃遗留且没有活跃发布者后，维护者才可记录原因并人工清理。

## 五、建议抽检

- 首页右上近期入口、轻量页签、册页卡片、透明书目列表和历书月历是否层级清楚
- 首页月历日格是否只显示日期、今日标记和事件点/数量，选中日期区域是否显示完整农历信息，顶部年月是否可切换月份
- 月历选中日期内容、详情轻量主卡与分享卡、新建 / 编辑日期卡片和提醒滚轮是否正常
- 设置页外观样张与系统日程健康状态是否正常
- 小组件 1-5 格“预览宽度 / 预览高度”、折叠摘要、默认配置应用入口与透明 / 半透明背景是否正常
- 小组件内容筛选、排序、密度、边框、圆角、文字对比和农历前缀是否生效
- 新建 / 编辑 / 删除事件、提醒、系统日历同步和小组件刷新链路是否正常
- Direct 渠道检查更新是否能读取 GitHub Release
- 在至少一台物理手机完成 Direct APK 安装 / 升级、通知、日历、Launcher 小组件与性能 smoke
- 从公开 GitHub Release 在线重新安装唯一 APK，并完成更新检查与关键链路 smoke
