# 拾光 (TimeAPK)

一款基于 **Jetpack Compose** 与 **Material 3** 的 Android 倒计时 / 纪念日应用，支持多语言（中/英）、主题切换、桌面小部件与提醒通知。

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
- **minSdk** 26 · **targetSdk** 34

## 快速开始

### 克隆与打开

```bash
git clone https://github.com/<你的用户名>/TimeAPK.git
cd TimeAPK
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
   产出路径：`app/build/outputs/apk/direct/release/`

更多发布与更新说明见 **[docs/release_and_update_guide.md](docs/release_and_update_guide.md)**。  
上传 GitHub 与打包发布完整步骤见 **[docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)**。

## 项目结构概览

```
app/src/main/
├── java/com/example/timeapk/
│   ├── data/           # Room、Repository
│   ├── ui/              # Compose 界面与 ViewModel
│   ├── notifications/   # 提醒调度
│   ├── update/          # 更新检查接口（预留）
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
| [design_plan_hk_retro.md](docs/design_plan_hk_retro.md) | 港式复古 UI 设计规范 |
| [canvas_design_philosophy.md](docs/canvas_design_philosophy.md) | Harbor Glow 视觉哲学 |
| [icon_assets.md](docs/icon_assets.md) | 图标规范与 AI 生图 Prompt |
| [interaction_design_changelog.md](docs/interaction_design_changelog.md) | 交互与动效说明 |
| [优化建议.md](docs/优化建议.md) | 功能与体验优化规划 |

## 许可证

请根据实际情况添加 License 文件或说明。
