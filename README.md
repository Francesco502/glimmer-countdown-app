# 拾光（Glimmer）
`v3.11` 的 Android 倒数日 / 生日 / 纪念日应用，基于 Jetpack Compose + Material 3。

## 版本信息

- `versionName`: `3.11`
- `versionCode`: `16`
- 发布日期：`2026-05-27`

## 核心能力

- 管理倒数日、生日、纪念日等事件
- 同时支持公历与农历事件
- 支持按天、周、月、半年、年重复
- 支持“提前 N 天 + 固定时刻”的提醒配置
- 支持系统日历 / 日程同步、权限处理与同步状态反馈
- 首页支持搜索、筛选、置顶、自定义排序与月历视图
- 支持表格模式与桌面小组件
- 支持 JSON 导入 / 导出，用于备份与恢复

## 3.11 版本重点

- 修复重复事件“已过去天数”、暗色模式列表文字、详情页删除/加载失败等显示问题
- JSON 导入支持逐条容错，格式错误时保留有效事件并报告失败数
- 补齐 CSV 导出字段，优化今天事件的纯文本导出显示
- 倒计时提醒与里程碑提醒拆分通知 channel，并降低连续设置变更触发的重复重排程
- 修复更新检查器客户端复用与空响应处理问题

## 构建与运行

```bash
# 直营渠道 Debug
./gradlew installDirectDebug

# Play 渠道 Debug
./gradlew installPlayDebug
```

```bash
# 直营渠道 Release APK
./gradlew assembleDirectRelease

# Play 渠道 Release AAB
./gradlew bundlePlayRelease
```

默认产物路径：

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-11.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 发布文档

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
