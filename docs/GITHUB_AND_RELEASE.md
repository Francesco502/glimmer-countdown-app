# GitHub 提交与发布流程（v3.7）

本文档用于当前 `3.7` 版本的代码提交、推送与 GitHub Release 操作。

## 1. 本地提交

```bash
git status
git add app gradle.properties README.md CHANGELOG.md docs scripts .gitignore
git commit -m "release: refresh v3.7 docs and widget fixes"
```

说明：

- 只提交业务代码、版本元数据、发布脚本和发布文档。
- 不提交本地缓存目录，例如 `.gradle-user-home`、`.cursor`、`build`、`.tmp`。

## 2. 推送代码

```bash
git push origin main
```

## 3. 创建或更新 `v3.7` 标签

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

说明：

- 如果 `3.7` 已经发布过，而代码又修复了问题但仍保持同版本号，就需要把 `v3.7` 标签移动到最新提交。

## 4. 构建 Release APK

```bash
./gradlew assembleDirectRelease
```

产物路径：

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-7.apk`

## 5. 创建或更新 GitHub Release

```powershell
$env:GITHUB_TOKEN = "your_token"
.\scripts\publish-release.ps1
```

脚本行为：

- 自动读取 `gradle.properties` 中的 `VERSION_NAME`
- 自动从 `CHANGELOG.md` 提取 `3.7` 小节作为 Release Notes
- 如果 Release 已存在，会自动更新说明
- 如果同名 APK 资产已存在，会自动删除旧资产并上传新 APK

## 6. 发布后核对

- Release 标题、标签与说明是否对应 `v3.7`
- 上传的 APK 文件名是否为 `glimmer-countdown-3-7.apk`
- “设置 > 关于”中版本是否显示为 `3.7`
- 抽检应用主题切换、小组件主题跟随、小组件全量事件显示和更新检查流程
