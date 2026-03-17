# 拾光（Glimmer）

`v3.6` 的 Android 倒计时 / 生日 / 纪念日应用，基于 Jetpack Compose + Material 3。

## 版本信息

- `versionName`: `3.6`
- `versionCode`: `10`
- 计划发布日期：`2026-03-17`

## 核心能力

- 倒计时、生日、纪念日等事件管理
- 公历与农历事件并存，支持农历单次与农历每年重复计算
- 重复规则支持按天、周、月、半年、年循环
- 自定义提醒：支持“提前 N 天 + 指定时刻”
- 系统日历 / 日程同步，包含权限申请、失败提示与同步状态回写
- 首页支持搜索、分类筛选、自定义排序（默认）、按剩余天数 / 目标日期排序切换、置顶与月历视图
- 表格模式与桌面小组件支持更完整的中文时间表达
- 支持 JSON 导入 / 导出与基础数据恢复

## 3.6 当前重点更新

- 修复系统日历普通提醒同步误删里程碑日历项的问题
- 修复未来事件里程碑提醒不能提前建档、导致漏提醒的问题
- 修复更新检查失败时误报“已是最新版本”的反馈问题
- 限制农历事件仅支持“不重复 / 每年重复”，使编辑页与实际提醒/展示逻辑保持一致
- 首页排序改为默认“自定义排序”，仅该模式支持长按拖拽；首页长按编辑下线，置顶在三种排序下都生效；旧用户升级后会一次性重置到默认自定义排序
- 同步升级默认版本号、APK 命名与发布脚本到 `3.6`

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

# Release AAB（Play 渠道）
./gradlew bundlePlayRelease
```

默认产物路径：

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-6.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 发布文档

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
