# GitHub 提交与发布流程（v3.13）

本文档用于当前 `3.13` 版本的代码提交、推送、标签与 GitHub Release 操作。

## 1. 本地提交

```bash
git status
git add app gradle.properties README.md CHANGELOG.md docs scripts .gitignore
git commit -m "release: ship v3.13"
```

说明：

- 只提交业务代码、版本元数据、脚本、字体 license 和发布文档
- 不提交本地缓存目录，例如 `.gradle-user-home`、`.cursor`、`build`

## 2. 推送代码

```bash
git push -u origin codex/widget-enhancement-3-13
```

如果最终发布分支是 `main`，应先完成合并或按仓库实际策略推送到目标分支。

## 3. 创建或更新 `v3.13` 标签

首次发布：

```bash
git tag -a v3.13 -m "Release v3.13"
git push origin v3.13
```

同版本重新发布：

```bash
git tag -fa v3.13 -m "Release v3.13"
git push origin v3.13 --force
```

## 4. 构建 Release 产物

```bash
./gradlew testDirectDebugUnitTest testPlayDebugUnitTest
./gradlew lintDirectRelease lintPlayRelease
./gradlew assembleDirectRelease assemblePlayRelease bundlePlayRelease
```

产物路径：

- GitHub Release：`app/build/outputs/apk/direct/release/glimmer-countdown-3-13.apk`
- Play Console：`app/build/outputs/bundle/playRelease/app-play-release.aab`

## 5. 创建或更新 GitHub Release

```powershell
$env:GITHUB_TOKEN = "your_token"
.\scripts\publish-release.ps1
```

脚本行为：

- 自动读取 `gradle.properties` 中的 `VERSION_NAME`
- 自动从 `CHANGELOG.md` 提取 `3.13` 小节作为 Release Notes
- 如果 Release 已存在，会自动更新说明
- 如果同名 Direct APK 资源已存在，会自动删除旧资源并上传新 APK
- 不上传 Play APK / AAB，避免 Direct 渠道应用内更新误下载 Play 包

## 6. 发布后核对

- Release 标题、标签与说明是否对应 `v3.13`
- 上传的 APK 文件名是否为 `glimmer-countdown-3-13.apk`
- Release 资产中没有 `app-play-release.apk` 或 `app-play-release.aab`
- Direct APK `versionName` 是否为 `3.13`
- Play APK / AAB `versionName` 是否为 `3.13-play`
- Play APK 是否不包含 `REQUEST_INSTALL_PACKAGES`
- 抽检设置页折叠分组、字体弹窗、小组件配置、透明背景和 Direct 渠道检查更新
