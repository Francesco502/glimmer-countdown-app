# 发布检查清单（v3.8）

**版本**：`3.8`（`versionCode=12`）
**发布日期**：`2026-04-03`

## 一、已完成的自动校验

- [x] `gradle.properties` 中 `VERSION_CODE=12`、`VERSION_NAME=3.8` 正确
- [x] `app/build.gradle.kts` 的版本读取、APK 重命名与 Play AAB 构建逻辑正常
- [x] `README.md`、`CHANGELOG.md` 与发布文档已切换到 `3.8`
- [x] `./gradlew assembleDirectDebug` 通过
- [x] `./gradlew testDirectDebugUnitTest` 通过
- [x] `./gradlew lintDirectDebug` 通过
- [x] `./gradlew assembleDirectRelease` 通过
- [x] `./gradlew bundlePlayRelease` 通过
- [x] Direct APK 已输出到 `app/build/outputs/apk/direct/release/glimmer-countdown-3-8.apk`
- [x] Play AAB 已输出到 `app/build/outputs/bundle/playRelease/app-play-release.aab`
- [x] `git diff --check` 未发现尾随空格或冲突标记

## 二、发布前建议人工复核

- [ ] 小组件在系统浅色 / 深色切换时表现正确
- [ ] 小组件点击具体事件后能打开对应详情，而不是只进入首页
- [ ] 权限不足时，选择降级保存后会直接返回首页
- [ ] 编辑页未保存内容时，返回会弹出“放弃修改”确认框
- [ ] 详情页底部只保留“提醒 / 置顶 / 编辑 / 删除”四按钮，且小屏下不拥挤
- [ ] 设置首页的细线分隔样式在真机上视觉正常

## 三、发布动作

- [ ] 将当前代码提交并 push 到远端分支
- [ ] 创建或更新 `v3.8` 标签
- [ ] 在 GitHub Release 中上传最新 APK / AAB
- [ ] 以 `CHANGELOG.md` 中的 `3.8` 小节作为 Release Notes
