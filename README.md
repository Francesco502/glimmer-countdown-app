# 拾光（Glimmer）

`v3.7` 的 Android 倒计时 / 生日 / 纪念日应用，基于 Jetpack Compose + Material 3。

## 版本信息

- `versionName`: `3.7`
- `versionCode`: `11`
- 发布日期：`2026-04-01`

## 核心能力

- 管理倒计时、生日、纪念日等事件
- 同时支持公历与农历事件
- 支持按天、周、月、半年、年重复
- 支持“提前 N 天 + 固定时刻”的提醒配置
- 支持系统日历 / 日程同步、权限处理与同步状态反馈
- 首页支持搜索、筛选、置顶、自定义排序与月历视图
- 表格模式和小组件支持更完整的中文时间表达
- 支持 JSON 导入 / 导出，用于备份与恢复

## 3.7 版本重点

- 修复系统日夜模式切换后，小组件偶发停留在旧主题的问题。
- 修复应用内主题切换过程中，小组件偶发空白或不显示内容的问题。
- 恢复小组件可滚动列表能力，重新支持显示全部事件，不再只显示固定前几条。
- 统一应用主题设置、小组件主题解析和刷新触发链路。
- 同步更新版本元数据、APK 命名、发布脚本和发布文档到 `3.7`。

## 构建与运行

```bash
# 直装渠道 Debug
./gradlew installDirectDebug

# Play 渠道 Debug
./gradlew installPlayDebug
```

```bash
# 直装渠道 Release APK
./gradlew assembleDirectRelease

# Play 渠道 Release AAB
./gradlew bundlePlayRelease
```

默认产物路径：

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-7.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 发布文档

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
