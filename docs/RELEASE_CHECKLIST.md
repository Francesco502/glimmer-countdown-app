# 发布检查清单（v3.11）

**版本**：`3.11`（`versionCode=16`）  **发布日期**：`2026-04-16`

## 一、自动校验

- [x] `gradle.properties` 中 `VERSION_CODE=16`、`VERSION_NAME=3.11` 正确
- [x] `app/build.gradle.kts` 的版本读取、APK 重命名与 Play AAB 构建逻辑正常
- [x] `README.md`、`CHANGELOG.md` 与发布文档已切换到 `3.11`
- [x] `./gradlew testPlayDebugUnitTest --tests "com.example.timeapk.ui.event.EventEntryValidationTest" --tests "com.example.timeapk.notifications.ReminderDateCalculatorEdgeTest" --tests "com.example.timeapk.notifications.ScheduleSyncManagerTest"` 通过
- [x] `./gradlew assembleDirectRelease` 通过
- [x] `./gradlew bundlePlayRelease` 通过
- [x] Direct APK 已输出到 `app/build/outputs/apk/direct/release/glimmer-countdown-3-11.apk`
- [x] Play AAB 已输出到 `app/build/outputs/bundle/playRelease/app-play-release.aab`
- [x] `git diff --check` 未发现尾随空格或冲突标记

## 二、发布前人工复核

- [ ] 新建普通事件时，默认提醒开关为开启，默认提前天数为 7 天，默认提醒时间为 10:00
- [ ] 在设置中修改“新建事件默认提醒”后，新建事件会使用新默认值
- [ ] 编辑已有事件时，不会被新的默认提醒设置覆盖
- [ ] 里程碑提醒、系统日历同步、小组件与现有提醒链路保持正常
- [ ] “里程碑与提醒”页在浅色 / 深色主题下显示正常，滚轮与开关样式一致

## 三、发布动作

- [ ] 提交并 push 当前代码到远端分支
- [ ] 创建或更新 `v3.11` 标签
- [ ] 在 GitHub Release 中上传最新 APK 与 AAB
- [ ] 以 `CHANGELOG.md` 中的 `3.11` 小节作为 Release Notes
