# TimeAPK 发布与更新指引

本文档说明当前 `3.4` 版本如何完成签名、构建、发布，以及如何复用 GitHub Release 作为后续更新源。

## 一、当前状态

| 项目 | 状态 |
|------|------|
| `applicationId` / 版本号 | 已配置，支持从 `gradle.properties` 覆盖 |
| 最低 / 目标 SDK | `minSdk 26` / `targetSdk 36` |
| Release 构建 | 已启用 `release` buildType，开启 `minify` 与 `shrinkResources` |
| Release 签名 | 已支持从 `keystore.properties` 读取 |
| APK 命名 | 已自动重命名为 `glimmer-countdown-3-4.apk` |
| 更新检查预留 | 已有 GitHub Release 检查器与设置入口 |
| 渠道包 | 已支持 `direct` / `play` flavor |

## 二、发布前准备

### 1. 签名配置

根目录准备：

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

当前版本：

- `VERSION_NAME=3.4`
- `VERSION_CODE=8`

如果仅进行 `3.4` 的最终发布，不需要改动版本号；如果后续继续发修订包，应先递增 `versionCode`。

## 三、构建命令

```bash
./gradlew test
./gradlew lintDirectDebug
./gradlew assembleDirectRelease
```

APK 路径：

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-4.apk`

## 四、GitHub 发布

### 1. 推送代码

```bash
git add app gradle.properties README.md CHANGELOG.md docs
git commit -m "release: finalize v3.4 notes and fixes"
git push origin main
```

### 2. 推送标签

```bash
git tag -a v3.4 -m "Release v3.4"
git push origin v3.4
```

### 3. 创建 Release

仓库内脚本：

```powershell
$env:GITHUB_TOKEN = "your_token"
.\scripts\publish-release.ps1
```

脚本会：

- 读取当前版本号
- 提取 `CHANGELOG.md` 中 `3.4` 小节作为说明
- 调用 GitHub Releases API
- 上传 Release APK

## 五、与应用内更新的关系

当前工程已具备：

- GitHub Release 更新检查能力
- 设置页中的“检查更新”入口
- 渠道区分与版本展示能力

后续如果要接入真实更新下载与安装，只需要继续扩展现有更新模块，不需要重做发布链路。
