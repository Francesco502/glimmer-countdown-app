# 拾光 (Glimmer)

> "Captured light in the deep ocean of time."
> "在时间的深海里，拾起一缕微光。"

**v2.0** · 一款基于 **Jetpack Compose** 与 **Material 3** 的 Android 倒计时 / 纪念日应用，支持多语言（中/英）、主题切换、桌面小部件、应用内检查更新与提醒通知。

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
   产出路径：`app/build/outputs/apk/direct/release/glimmer-countdown-2-0.apk`（版本号随 gradle.properties 中 VERSION_NAME 变化）

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
| [release_and_update_guide.md](docs/release_and_update_guide.md) | 发布 APK、签名、版本与更新渠道说明 |
| [GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md) | 上传 GitHub 与打包发布操作步骤 |
| [RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md) | 发布前检查清单 |

## 许可证

请根据实际情况添加 License 文件或说明。
