# TimeAPK 发布与更新指南

本文档说明当前 `3.8` 版本如何完成签名、构建、发布，以及如何更新 GitHub Release。

## 一、当前状态

| 项目 | 状态 |
|------|------|
| `applicationId` / 版本号 | 已配置，可通过 `gradle.properties` 覆盖 |
| 最低 / 目标 SDK | `minSdk 26` / `targetSdk 36` |
| Release 构建 | 已启用 `release` buildType，并开启 `minify` 与 `shrinkResources` |
| Release 签名 | 从 `keystore.properties` 读取 |
| Direct APK 命名 | 输出为 `glimmer-countdown-3-8.apk` |
| 渠道 | 支持 `direct` / `play` flavor |
| 应用内更新入口 | 已具备 GitHub Release 检查与设置页入口 |

## 二、发布前准备

### 1. 签名配置

在仓库根目录准备：

```properties
storeFile=timeapk-release.keystore
storePassword=xxx
keyAlias=timeapk
keyPassword=xxx
```

确保以下文件不进入仓库：

- `keystore.properties`
- `*.keystore`

### 2. 版本确认

当前版本值：

- `VERSION_NAME=3.8`
- `VERSION_CODE=12`

若后续继续发布新版本，应同步递增 `versionCode`，并更新 `versionName`、README 与 CHANGELOG。

## 三、构建命令

```bash
./gradlew assembleDirectDebug
./gradlew testDirectDebugUnitTest
./gradlew lintDirectDebug
./gradlew assembleDirectRelease
./gradlew bundlePlayRelease
```

产物路径：

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-8.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 四、GitHub 发布

### 1. 提交与推送

```bash
git add app gradle.properties README.md CHANGELOG.md docs scripts .gitignore
git commit -m "release: ship v3.8"
git push origin main
```

### 2. 标签

```bash
git tag -a v3.8 -m "Release v3.8"
git push origin v3.8
```

### 3. Release

```powershell
$env:GITHUB_TOKEN = "your_token"
.\scripts\publish-release.ps1
```

脚本会：

- 读取当前版本号
- 提取 `CHANGELOG.md` 中 `3.8` 小节作为 Release Notes
- 自动更新已存在的 GitHub Release
- 自动替换同名 APK 资产

## 五、建议抽检

- 桌面小组件的深浅色切换与点击打开链路
- 权限不足时的降级保存与返回首页行为
- 编辑页未保存内容时的返回确认
- 设置首页细线分隔样式的真机效果
