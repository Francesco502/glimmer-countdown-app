# 发布检查清单（v3.11）

**版本**：`3.11`（`versionCode=16`）  **发布日期**：`2026-05-27`

## 一、已完成的自动校验

- [x] `gradle.properties` 中 `VERSION_CODE=16`、`VERSION_NAME=3.11` 正确
- [x] `app/build.gradle.kts` 的版本读取、APK 重命名与 Play AAB 构建逻辑正常
- [x] `README.md`、`CHANGELOG.md` 与发布文档已切换到 `3.11`
- [x] `./gradlew assembleDirectDebug` 通过
- [x] `./gradlew testDirectDebugUnitTest` 通过
- [x] `./gradlew lintDirectDebug` 通过
- [x] `./gradlew assembleDirectRelease` 通过
- [x] `./gradlew bundlePlayRelease` 通过
- [x] Direct APK 已输出到 `app/build/outputs/apk/direct/release/glimmer-countdown-3-11.apk`
- [x] Play AAB 已输出到 `app/build/outputs/bundle/playRelease/app-play-release.aab`
- [x] `git diff --check` 未发现尾随空格或冲突标记

## 二、发布前建议人工复核

- [ ] 每周/每月/每半年重复事件在非发生日显示的”已过去天数”不再为 0
- [ ] 暗色模式下首页列表项文字颜色可读性良好
- [ ] 编辑页快速切换权限对话框不再崩溃
- [ ] 导入格式错误的 JSON 文件时，能正常解析有效事件并提示错误数
- [ ] 删除事件后旋转屏幕，不会卡在已删除事件的详情页
- [ ] 详情页加载失败时显示返回按钮而非无限 spinner
- [ ] 里程碑模式下无即将到来节点时显示”无即将到来的节点”
- [ ] 触控目标（设置按钮、操作栏按钮、颜色选择器）在真机上易点击
- [ ] 系统通知设置中可分别配置”倒计时提醒”和”节点提醒”两个 channel
- [ ] 快速连续修改多项设置时，不会出现频繁的 reschedule 重启

## 三、发布动作

- [ ] 将当前代码提交并 push 到远端分支
- [ ] 创建或更新 `v3.11` 标签
- [ ] 在 GitHub Release 中上传最新 APK 与 AAB
- [ ] 以 `CHANGELOG.md` 中的 `3.11` 小节作为 Release Notes
