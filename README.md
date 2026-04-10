# 拾光（Glimmer）
`v3.10` 的 Android 倒数日 / 生日 / 纪念日应用，基于 Jetpack Compose + Material 3。

## 版本信息

- `versionName`: `3.10`
- `versionCode`: `15`
- 发布日期：`2026-04-10`

## 核心能力

- 管理倒数日、生日、纪念日等事件
- 同时支持公历与农历事件
- 支持按天、周、月、半年、年重复
- 支持“提前 N 天 + 固定时刻”的提醒配置
- 支持系统日历 / 日程同步、权限处理与同步状态反馈
- 首页支持搜索、筛选、置顶、自定义排序与月历视图
- 支持表格模式与桌面小组件
- 支持 JSON 导入 / 导出，用于备份与恢复

## 3.10 版本重点

- 修复“提前 X 天提醒”同步到系统日程后，在兼容性回退链路下只会出现第 X 天单条提醒的问题
- 当系统日历 Provider 触发兼容性回退时，改为补写从第 X 天到当天的整段提醒系列，而不是单条 `Events + Reminders`
- 保持当天提醒的周 / 月 / 年重复事件继续使用 RRULE 单条同步，避免破坏既有重复日程表现
- 延续 3.9 的真实写入、Provider 兼容性回退和 logcat 定位能力

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

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-10.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 发布文档

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
