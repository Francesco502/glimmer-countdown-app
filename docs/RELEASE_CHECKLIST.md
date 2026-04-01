# 发布检查清单（v3.7）

**版本**：`3.7`（`versionCode=11`）
**发布日期**：`2026-04-01`

## 一、版本与文档

- [ ] `gradle.properties` 中 `VERSION_CODE=11`、`VERSION_NAME=3.7` 正确
- [ ] `app/build.gradle.kts` 的版本读取、APK 重命名与 Play AAB 构建逻辑正常
- [ ] `README.md`、`CHANGELOG.md` 与发布文档内容和当前实现一致
- [ ] GitHub Release 标签与标题使用 `v3.7`

## 二、功能回归

- [ ] 应用跟随系统浅色 / 深色模式时表现正确
- [ ] 应用手动切换浅色 / 深色模式时表现正确
- [ ] 小组件在系统浅色 / 深色切换时会正确更新，不停留在旧主题
- [ ] 小组件在应用内切换主题后会正确刷新，不出现空白
- [ ] 小组件能显示全部事件，不再只显示固定前几条
- [ ] 重复提醒在日 / 周 / 月 / 半年 / 年场景下均能正确调度
- [ ] “提前 N 天”提醒会按预期写入系统日历 / 日程
- [ ] 开机、时区变更、手动改时间、冷启动后的统一重建正常
- [ ] 通知点击仍能打开正确事件详情
- [ ] 系统日历权限、写入、更新与删除链路保持一致

## 三、界面与可读性

- [ ] 设置页结构、命名与主题视觉一致
- [ ] 表格模式仍能正确显示中文相对时间表达
- [ ] 小组件关键数字与时间文案不会被截断
- [ ] 小 / 中 / 大尺寸小组件下的展示策略正常
- [ ] 深浅主题下关键信息可读

## 四、构建与产物

- [ ] `./gradlew test` 通过
- [ ] `./gradlew assembleDirectRelease` 成功
- [ ] `./gradlew bundlePlayRelease` 成功
- [ ] APK 输出路径为 `app/build/outputs/apk/direct/release/glimmer-countdown-3-7.apk`
- [ ] AAB 输出路径为 `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 五、发布动作

- [ ] 代码已提交并 push 到 `origin/main`
- [ ] `v3.7` 标签已创建或已更新到最新提交
- [ ] GitHub Release 已更新，并上传新的 APK 与 `3.7` 版本说明
