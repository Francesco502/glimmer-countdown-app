# TimeAPK 发布与更新指引

本文档说明 `4.0` 成熟版候选如何完成签名、构建、验证与 GitHub Release。4.0 检查清单完成前保持未发布状态，最新公开版本仍为 3.17。

## 一、当前状态

| 项目 | 状态 |
|------|------|
| `applicationId` / 版本号 | Direct：`com.example.timeapk` / `4.0`；Play：`com.example.timeapk.play` / `4.0-play` |
| 最低 / 目标 SDK | `minSdk 26` / `targetSdk 36` |
| Release 构建 | 已启用 `release` buildType，并开启 `minify` 与 `shrinkResources` |
| Release 签名 | 从 `keystore.properties` 读取 |
| Direct APK 命名 | 输出为 `glimmer-countdown-4-0.apk` |
| 渠道 | 支持 `direct` / `play` flavor |
| 应用内更新入口 | Direct 使用 GitHub Release；Play 使用占位更新器，不提供直接 APK 安装 |
| GitHub Release 资产 | 只上传 Direct APK |

## 二、发布前准备

### 1. 签名配置

在仓库根目录准备：

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

确保以下文件不进入仓库：

- `keystore.properties`
- `*.keystore`

### 2. 版本确认

当前版本值：

- `VERSION_NAME=4.0`
- `VERSION_CODE=23`

继续发布新版本时，应同步递增 `versionCode`，并更新 `versionName`、`README.md`、`CHANGELOG.md` 与发布文档。

## 三、构建命令

```bash
./gradlew testDirectDebugUnitTest
./gradlew testPlayDebugUnitTest
./gradlew compileDirectDebugAndroidTestKotlin
./gradlew lintDirectDebug lintDirectRelease lintPlayRelease lintVitalDirectRelease lintVitalPlayRelease
./gradlew assembleDirectRelease assemblePlayRelease bundlePlayRelease
```

产物路径：

- `app/build/outputs/apk/direct/release/glimmer-countdown-4-0.apk`
- `app/build/outputs/apk/play/release/app-play-release.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 四、GitHub 发布

### 1. 提交与推送

```bash
git add app gradle.properties README.md CHANGELOG.md docs scripts .gitignore
git commit -m "release: ship v4.0"
git push -u origin codex/release-4-0-widget-sort
```

### 2. 标签

标签必须在代码、文档和检查结果全部确定后，创建于最终发布 commit：

```bash
git tag -a v4.0 -m "Release v4.0"
git push origin v4.0
```

脚本会分别解引用 annotated/lightweight tag，并要求本地与远端 tag 解引用后的 commit 精确一致。禁止 force-push、移动或复用已推送 tag，禁止覆盖已发布 Release；已发布后出现问题必须递增版本号。

### 3. Release

前置条件：正式签名 exact Direct APK 已生成；`ANDROID_HOME` 可定位稳定版 `apksigner`；`GLIMMER_RELEASE_CERT_SHA256` 已安全注入；`GITHUB_TOKEN`（或 `gh auth token`）具有 `GitHub Contents: write` 权限；本地和远端 `v4.0` tag 已指向最终发布 commit。

```powershell
$env:GITHUB_TOKEN = "your_token"
$env:GLIMMER_RELEASE_CERT_SHA256 = "your_release_certificate_sha256"
$env:ANDROID_HOME = "your_android_sdk"
.\scripts\publish-release.ps1
```

脚本会：

- 读取当前版本号
- 提取 `CHANGELOG.md` 中 `4.0` 小节作为 Release Notes
- 校验正式证书指纹及本地/远端 tag commit 后创建 `refs/heads/release-locks/v4.0` Git ref 锁
- 创建带 `ownership marker` 的 draft；仅恢复带脚本自身 marker 的 draft，拒绝 published Release 和人工 draft
- 只上传 exact Direct APK，并将响应及重新读取结果绑定到 asset id、size、digest、content type 和下载 URL
- 在发布前重新核对 ownership marker，并以最终 GET 验证公开 Release 与唯一 APK
- Play AAB 不上传 GitHub Release，只交付 Play Console

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
- Play 渠道不包含 `REQUEST_INSTALL_PACKAGES`，且不展示直接 APK 安装入口
