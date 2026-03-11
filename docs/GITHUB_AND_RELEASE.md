# GitHub 提交与发布流程（v3.5）

本文档用于当前 `3.5` 版本的代码提交、推送与 GitHub Release 操作。

## 1. 本地提交

```bash
git status
git add app gradle.properties README.md CHANGELOG.md docs
git commit -m "release: finalize v3.5 notes and fixes"
```

说明：

- 仅提交业务代码与文档
- 不提交本地缓存目录，如 `.gradle-user-home`、`.cursor`、`build`

## 2. 推送到远端

```bash
git push origin main
```

## 3. 打标签

```bash
git tag -a v3.5 -m "Release v3.5"
git push origin v3.5
```

## 4. 构建 Release APK

```bash
./gradlew assembleDirectRelease
```

产物路径：

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-5.apk`

## 5. 创建 GitHub Release

当前仓库已提供脚本：

```powershell
$env:GITHUB_TOKEN = "your_token"
.\scripts\publish-release.ps1
```

脚本行为：

- 自动读取 `gradle.properties` 的 `VERSION_NAME`
- 自动从 `CHANGELOG.md` 提取 `3.5` 小节作为 Release Notes
- 自动创建或复用 `v3.5` Release
- 自动上传 APK 资产

## 6. 发布后核对

- Release 标题、标签与说明是否对应 `v3.5`
- 上传的 APK 文件名是否为 `glimmer-countdown-3-5.apk`
- 手机安装后，“设置 > 关于”中版本显示为 `3.5`
- 抽检提醒、系统日历同步、首页表格模式、小组件字号与月历视图
