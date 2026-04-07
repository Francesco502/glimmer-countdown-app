# 发布检查清单（v3.9）

**版本**：`3.9`（`versionCode=14`）  **发布日期**：`2026-04-07`

## 一、已完成的自动校验

- [x] `gradle.properties` 中 `VERSION_CODE=14`、`VERSION_NAME=3.9` 正确
- [x] `app/build.gradle.kts` 的版本读取、APK 重命名与 Play AAB 构建逻辑正常
- [x] `README.md`、`CHANGELOG.md` 与发布文档已切换到 `3.9`
- [x] `./gradlew assembleDirectDebug` 通过
- [x] `./gradlew testDirectDebugUnitTest` 通过
- [x] `./gradlew lintDirectDebug` 通过
- [x] `./gradlew assembleDirectRelease` 通过
- [x] `./gradlew bundlePlayRelease` 通过
- [x] Direct APK 已输出到 `app/build/outputs/apk/direct/release/glimmer-countdown-3-9.apk`
- [x] Play AAB 已输出到 `app/build/outputs/bundle/playRelease/app-play-release.aab`
- [x] `git diff --check` 未发现尾随空格或冲突标记

## 二、发布前建议人工复核

- [ ] 在授予通知 / 日历权限的真机上，新增事件后不再误报“事件已保存，但部分提醒/同步操作失败”
- [ ] 对新版系列同步不兼容的设备，确认 logcat 出现 legacy fallback 成功日志，且系统日历中能看到新增事件
- [ ] 在存在标准可写日历账户的设备上，提醒与系统日程同步可正常写入系统日历
- [ ] 小组件在系统浅色 / 深色切换时表现正确
- [ ] 编辑页未保存内容时，返回会弹出“放弃修改”确认框
- [ ] 详情页底部只保留“提醒 / 置顶 / 编辑 / 删除”四按钮，且小屏下不拥挤

## 三、发布动作

- [ ] 将当前代码提交并 push 到远端分支
- [ ] 创建或更新 `v3.9` 标签
- [ ] 在 GitHub Release 中上传最新 APK 与 AAB
- [ ] 以 `CHANGELOG.md` 中的 `3.9` 小节作为 Release Notes
