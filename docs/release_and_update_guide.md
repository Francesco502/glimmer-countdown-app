# TimeAPK 发布与更新指引

本文档说明当前 `3.12` 版本如何完成签名、构建、发布，以及如何更新 GitHub Release。

## 一、当前状态

| 项目 | 状态 |
|------|------|
| `applicationId` / 版本号 | 已配置，可通过 `gradle.properties` 覆盖 |
| 最低 / 目标 SDK | `minSdk 26` / `targetSdk 36` |
| Release 构建 | 已启用 `release` buildType，并开启 `minify` 与 `shrinkResources` |
| Release 签名 | 从 `keystore.properties` 读取 |
| Direct APK 命名 | 输出为 `glimmer-countdown-3-12.apk` |
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

- `VERSION_NAME=3.12`
- `VERSION_CODE=17`

后续继续发布新版本时，应同步递增 `versionCode`，并更新 `versionName`、`README.md`、`CHANGELOG.md` 与发布文档。

## 三、构建命令

```bash
./gradlew assembleDirectDebug
./gradlew testPlayDebugUnitTest --tests "com.example.timeapk.ui.event.EventEntryValidationTest" --tests "com.example.timeapk.notifications.ReminderDateCalculatorEdgeTest" --tests "com.example.timeapk.notifications.ScheduleSyncManagerTest"
./gradlew assembleDirectRelease
./gradlew bundlePlayRelease
```

产物路径：

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-12.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 四、GitHub 发布

### 1. 提交与推送

```bash
git add app gradle.properties README.md CHANGELOG.md docs scripts .gitignore
git commit -m "release: ship v3.12"
git push origin main
```

### 2. 标签

```bash
git tag -a v3.12 -m "Release v3.12"
git push origin v3.12
```

### 3. Release

```powershell
$env:GITHUB_TOKEN = "your_token"
.\scripts\publish-release.ps1
```

脚本会：

- 读取当前版本号
- 提取 `CHANGELOG.md` 中 `3.12` 小节作为 Release Notes
- 自动更新已存在的 GitHub Release
- 自动替换同名 APK / AAB 资产

## 五、建议抽检

- 新建事件默认提醒是否按“开启 / 7 天 / 10:00”初始化
- 修改默认提醒设置后，新建事件是否使用新的默认值
- 里程碑提醒、系统日历同步和原有提醒计算链路是否正常
- 浅色 / 深色主题下设置页与事件编辑页显示是否正常
