# GitHub 提交与发布流程（v3.4）

本文档用于当前 `3.4` 版本的代码提交、推送与 GitHub Release 操作。

## 1. 本地提交

```bash
git status
git add app gradle.properties README.md CHANGELOG.md docs
git commit -m "docs(release): refresh v3.4 release notes and checklist"
```

说明：

- 仅提交业务代码和文档，不提交本地缓存目录（如 `.gradle-user-home`、`.cursor`、`build`）。

## 2. 推送到远端

```bash
git push origin main
```

## 3. 打标签（可选但推荐）

```bash
git tag -a v3.4 -m "Release v3.4"
git push origin v3.4
```

## 4. 创建 GitHub Release

1. 打开仓库 `Releases` 页面，点击 `Draft a new release`。
2. 选择标签 `v3.4`（不存在则新建同名标签）。
3. 标题填写：`v3.4`。
4. 描述正文建议直接使用 `CHANGELOG.md` 的 `3.4` 小节。
5. 上传 APK：
   - `app/build/outputs/apk/direct/release/glimmer-countdown-3-4.apk`
6. 发布 Release。

## 5. 发布后核对

- 在 Release 页面检查版本号、说明、APK 文件名是否一致。
- 在手机安装 APK，确认“设置-关于”版本显示为 `3.4`。
- 抽检通知提醒、系统日历同步、首页筛选/搜索/月历、小组件字号功能。
