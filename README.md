# 拾光 (Glimmer)

`v3.4` 的 Android 倒计时 / 纪念日应用，基于 Jetpack Compose + Material 3。

## 版本信息

- `versionName`: `3.4`
- `versionCode`: `8`

## 核心能力

- 倒计时、生日、纪念日等事件管理。
- 公历与农历事件并存，支持农历重复计算。
- 重复规则支持按日、周、月、半年、年等周期。
- 提醒支持“提前 N 天 + 指定时刻 + 自定义提醒文案（任意输入）”。
- 通知点击可直达对应事件详情。
- 可同步提醒到系统日程/日历（包含权限与失败提示链路）。
- 首页支持搜索、Tag 过滤、组合筛选与月历视图。
- 桌面小组件支持按尺寸自适应展示策略，支持独立字号缩放。
- 应用内全局字号支持基准缩放（保持标题/正文层级比例）。
- 支持 JSON 导入/导出与基础数据恢复。

## 构建与运行

```bash
# Debug（直装渠道）
./gradlew installDirectDebug

# Debug（Play 渠道）
./gradlew installPlayDebug
```

```bash
# Release APK（直装渠道）
./gradlew assembleDirectRelease
```

默认输出路径：

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-4.apk`

## 发布文档

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
