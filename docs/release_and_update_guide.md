# TimeAPK 发布与更新指引

本文档说明当前 `3.16` 版本如何完成签名、构建、发布，以及如何更新 GitHub Release。

## 一、当前状态

| 项目 | 状态 |
|------|------|
| `applicationId` / 版本号 | Direct：`com.example.timeapk` / `3.16`；Play：`com.example.timeapk.play` / `3.16-play` |
| 最低 / 目标 SDK | `minSdk 26` / `targetSdk 36` |
| Release 构建 | 已启用 `release` buildType，并开启 `minify` 与 `shrinkResources` |
| Release 签名 | 从 `keystore.properties` 读取 |
| Direct APK 命名 | 输出为 `glimmer-countdown-3-16.apk` |
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

确保以下文件不进入仓库：

- `keystore.properties`
- `*.keystore`

### 2. 版本确认

当前版本值：

- `VERSION_NAME=3.16`
- `VERSION_CODE=21`

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

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-16.apk`
- `app/build/outputs/apk/play/release/app-play-release.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 四、GitHub 发布

### 1. 提交与推送

```bash
git add app gradle.properties README.md CHANGELOG.md docs scripts .gitignore
git commit -m "release: ship v3.16"
git push -u origin codex/detail-share-card-316
```

### 2. 标签

```bash
git tag -a v3.16 -m "Release v3.16"
git push origin v3.16
```

### 3. Release

```powershell
$env:GITHUB_TOKEN = "your_token"
.\scripts\publish-release.ps1
```

脚本会：

- 读取当前版本号
- 提取 `CHANGELOG.md` 中 `3.16` 小节作为 Release Notes
- 自动更新已存在的 GitHub Release
- 自动替换同名 Direct APK 资产
- 不上传 Play APK / AAB，避免 Direct 渠道应用内更新误下载 Play 包

## 五、建议抽检

- 首页右上近期入口、轻量页签、册页卡片、透明书目列表和历书月历是否层级清楚
- 首页月历日格是否只显示日期、今日标记和事件点/数量，选中日期区域是否显示完整农历信息，顶部年月是否可切换月份
- 月历选中日期内容、详情轻量主卡与分享卡、新建 / 编辑日期卡片和提醒滚轮是否正常
- 设置页外观样张与系统日程健康状态是否正常
- 小组件 2x2、3x3、4x2 模板与透明 / 半透明背景是否正常
- 小组件内容筛选、排序、密度、边框、圆角、文字对比和农历前缀是否生效
- 新建 / 编辑 / 删除事件、提醒、系统日历同步和小组件刷新链路是否正常
- Direct 渠道检查更新是否能读取 GitHub Release
- Play 渠道不包含 `REQUEST_INSTALL_PACKAGES`，且不展示直接 APK 安装入口
