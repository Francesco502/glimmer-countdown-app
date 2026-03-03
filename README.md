# 拾光 (Glimmer)

> 白驹过隙，拾光留痕。  
> *Time flies, casting shadows.*

**v3.2** · 一款基于 **Jetpack Compose** 与 **Material 3** 的 Android 倒计时 / 纪念日应用，支持多语言（中/英）、主题切换、桌面小部件、应用内检查更新与提醒通知，并提供完整的农历生日 / 纪念日支持。

## 主要特性

- **农历事件全链路支持**：支持「按农历每年重复」的生日与纪念日，基于 `cn.6tail:lunar` 计算下次发生日与已历年数，并在详情页以「岁次 甲申 腊月 初八」等格式展示。
- **宋代美学主题**：默认配色与排版遵循「宋代工笔画」与绢本设色风格，提供瘦金体等字体预设。
- **灵活的首页视图**：按「全部 / 生日 / 纪念日 / 其他」分类筛选，支持按剩余天数、目标日期与创建时间排序。
- **桌面小组件与提醒**：倒计时小组件实时刷新，支持按事件设置提醒，并在更新版本时通过 GitHub Release 自动检查更新。

## 技术栈

| 类别     | 技术 |
|----------|------|
| 语言     | Kotlin |
| UI       | Jetpack Compose、Material 3 |
| 架构     | MVVM |
| 本地存储 | Room |
| 导航     | Navigation Compose |
| 后台任务 | WorkManager（提醒） |
| 偏好     | DataStore |

## 环境要求

- **Android Studio**  Ladybug (2024.2.1) 或更高（推荐）
- **JDK** 17
- **minSdk** 26 · **targetSdk** 35

## 快速开始

### 克隆与打开

```bash
git clone https://github.com/Francesco502/glimmer-countdown-app.git
cd glimmer-countdown-app
```

用 Android Studio 打开项目根目录即可。

### 运行 Debug 包

```bash
# 直装渠道（默认）
./gradlew installDirectDebug

# 或 Play 渠道
./gradlew installPlayDebug
```

或在 Android Studio 中直接 Run。

### 打包 Release APK（需先配置签名）

1. 在项目根目录生成签名密钥库（仅做一次）：
   ```powershell
   .\gen-keystore.ps1
   ```
   或使用 [release_and_update_guide.md](docs/release_and_update_guide.md) 中的 `keytool` 命令。

2. 在项目根目录创建 `keystore.properties`（不要提交到 Git），填写：
   ```properties
   storeFile=timeapk-release.keystore
   storePassword=你的密钥库密码
   keyAlias=timeapk
   keyPassword=你的密钥密码
   ```

3. 执行构建：
   ```bash
   ./gradlew assembleDirectRelease
   ```
   产出路径：`app/build/outputs/apk/direct/release/glimmer-countdown-3-2.apk`（版本号随 gradle.properties 中 VERSION_NAME 变化）

更多发布与更新说明见 **[docs/release_and_update_guide.md](docs/release_and_update_guide.md)**。  
上传 GitHub 与打包发布完整步骤见 **[docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)**。

## 项目结构概览

```
app/src/main/
├── java/com/example/timeapk/
│   ├── data/           # Room、Repository
│   ├── ui/              # Compose 界面与 ViewModel
│   ├── notifications/   # 提醒调度
│   ├── update/          # 应用内检查更新（GitHub Release）
│   ├── widget/          # 桌面小部件
│   ├── MainActivity.kt
│   ├── TimeApplication.kt
│   └── TimeApp.kt       # 导航根 Composable
└── res/                 # 资源与多语言
```

## 文档

| 文档 | 说明 |
|------|------|
| [CHANGELOG.md](CHANGELOG.md) | 版本更新日志（含 v1.0 → v3.2 等） |
| [release_and_update_guide.md](docs/release_and_update_guide.md) | 发布 APK、签名、版本与更新渠道说明 |
| [GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md) | 上传 GitHub 与打包发布操作步骤 |
| [RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md) | 发布前检查清单 |
| [LUNAR_IMPLEMENTATION_PLAN.md](docs/LUNAR_IMPLEMENTATION_PLAN.md) | 农历事件全链路支持实施方案与实现说明（3.1 已全部落地） |

## 许可证

请根据实际情况添加 License 文件或说明。
