# 拾光（Glimmer）
`v3.8.1` 的 Android 倒数日 / 生日 / 纪念日应用，基于 Jetpack Compose + Material 3。

## 版本信息

- `versionName`: `3.8.1`
- `versionCode`: `13`
- 发布日期：`2026-04-03`

## 核心能力

- 管理倒数日、生日、纪念日等事件
- 同时支持公历与农历事件
- 支持按天、周、月、半年、年重复
- 支持“提前 N 天 + 固定时刻”的提醒配置
- 支持系统日历 / 日程同步、权限处理与同步状态反馈
- 首页支持搜索、筛选、置顶、自定义排序与月历视图
- 支持表格模式与桌面小组件
- 支持 JSON 导入 / 导出，用于备份与恢复

## 3.8.1 版本重点

- 修复“通知和日历权限都已授予，但系统中没有可写日历”时，保存仍误报“部分提醒/同步操作失败”的问题
- 新增保存前、权限回调后和同步开关开启时的可写日历检查
- 无可写日历时，改为明确提示用户前往系统同步设置，或以“不同步日历”的方式继续保存
- 保留 3.8 的小组件主题、权限降级保存、编辑页返回防丢等改进

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

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-8-1.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 发布文档

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
