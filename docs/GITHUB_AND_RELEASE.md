# GitHub 提交与发布流程（v3.11）

本文档用于当前 `3.11` 版本的代码提交、推送、标签与 GitHub Release 操作。

## 1. 本地提交

```bash
git status
git add app gradle.properties README.md CHANGELOG.md docs scripts .gitignore
git commit -m "release: ship v3.11"
```

说明：

- 只提交业务代码、版本元数据、脚本和发布文档
- 不提交本地缓存目录，例如 `.gradle-user-home`、`.cursor`、`build`

## 2. 推送代码

```bash
git push origin main
```

## 3. 创建或更新 `v3.11` 标签

首次发布：

```bash
git tag -a v3.11 -m "Release v3.11"
git push origin v3.11
```

同版本重新发布：

```bash
git tag -fa v3.11 -m "Release v3.11"
git push origin v3.11 --force
```

## 4. 构建 Release 产物

```bash
./gradlew assembleDirectRelease
./gradlew bundlePlayRelease
```

产物路径：

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-11.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 5. 创建或更新 GitHub Release

```powershell
$env:GITHUB_TOKEN = "your_token"
.\scripts\publish-release.ps1
```

脚本行为：

- 自动读取 `gradle.properties` 中的 `VERSION_NAME`
- 自动从 `CHANGELOG.md` 提取 `3.11` 小节作为 Release Notes
- 如果 Release 已存在，会自动更新说明
- 如果同名 APK / AAB 资源已存在，会自动删除旧资源并上传新 APK / AAB

## 6. 发布后核对

- Release 标题、标签与说明是否对应 `v3.11`
- 上传的 APK 文件名是否为 `glimmer-countdown-3-11.apk`
- 上传的 AAB 文件名是否为 `app-play-release.aab`
- “设置 > 关于”中版本是否显示为 `3.11`
- 抽检权限保存流程、系统日历同步与小组件主题切换
- 抽检新建事件默认提醒设置、系统日历同步与提醒计算是否正常
