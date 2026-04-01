# TimeAPK 发布与更新指引

本文档说明当前 `3.7` 版本如何完成签名、构建、发布，以及如何在保持同版本号时重新发布。

## 一、当前状态

| 项目 | 状态 |
|------|------|
| `applicationId` / 版本号 | 已配置，可通过 `gradle.properties` 覆盖 |
| 最低 / 目标 SDK | `minSdk 26` / `targetSdk 36` |
| Release 构建 | 已启用 `release` buildType，并开启 `minify` 与 `shrinkResources` |
| Release 签名 | 从 `keystore.properties` 读取 |
| APK 命名 | 输出为 `glimmer-countdown-3-7.apk` |
| 更新检查 | 已具备 GitHub Release 检查器与应用内入口 |
| 渠道 | 支持 `direct` / `play` flavor |

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

- `VERSION_NAME=3.7`
- `VERSION_CODE=11`

如果只是修复 `3.7` 中的小问题并重新发布，可以保持 `3.7 / 11` 不变，但需要更新 `v3.7` 标签和 GitHub Release 资产。如果后续要正式发新版本，再递增 `versionCode` 与 `versionName`。

## 三、构建命令

```bash
./gradlew test
./gradlew assembleDirectRelease
./gradlew bundlePlayRelease
```

APK 路径：

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-7.apk`

## 四、GitHub 发布

### 1. 推送代码

```bash
git add app gradle.properties README.md CHANGELOG.md docs scripts .gitignore
git commit -m "release: refresh v3.7 docs and widget fixes"
git push origin main
```

### 2. 更新标签

首次发布：

```bash
git tag -a v3.7 -m "Release v3.7"
git push origin v3.7
```

同版本重新发布：

```bash
git tag -fa v3.7 -m "Release v3.7"
git push origin v3.7 --force
```

### 3. 更新 Release

```powershell
$env:GITHUB_TOKEN = "your_token"
.\scripts\publish-release.ps1
```

脚本会：

- 读取当前版本号
- 提取 `CHANGELOG.md` 中 `3.7` 小节作为 Release 说明
- 自动更新已存在的 Release 说明
- 自动替换同名 APK 资产

## 五、与应用内更新的关系

当前工程已具备：

- GitHub Release 更新检查能力
- 设置页中的“检查更新”入口
- 渠道区分与版本展示能力

后续若要接入完整下载与安装流程，只需要继续扩展现有更新模块，无需重做发布链路。
