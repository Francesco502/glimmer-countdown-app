# 拾光（Glimmer）
`v3.9` 的 Android 倒数日 / 生日 / 纪念日应用，基于 Jetpack Compose + Material 3。

## 版本信息

- `versionName`: `3.9`
- `versionCode`: `14`
- 发布日期：`2026-04-07`

## 核心能力

- 管理倒数日、生日、纪念日等事件
- 同时支持公历与农历事件
- 支持按天、周、月、半年、年重复
- 支持“提前 N 天 + 固定时刻”的提醒配置
- 支持系统日历 / 日程同步、权限处理与同步状态反馈
- 首页支持搜索、筛选、置顶、自定义排序与月历视图
- 支持表格模式与桌面小组件
- 支持 JSON 导入 / 导出，用于备份与恢复

## 3.9 版本重点

- 将系统日程同步回退到更贴近 `3.6 / 3.7` 的真实写入策略，不再依赖保存前的“标准可写日历账户”前置拦截
- 当新版系列同步链路与部分厂商日历 Provider 不兼容时，自动回退到旧版单条 `Events + Reminders` 写入，降低误报“部分提醒/同步操作失败”的概率
- 将旧提醒 / 旧里程碑日程的清理查询改为安全失败，只记日志，不再因为 Provider 查询兼容性把保存误判成部分失败
- 为保存链路补充 logcat 定位信息，后续能直接区分提醒调度、日历同步、里程碑同步和小组件刷新异常
- 延续 3.8 的小组件主题、权限降级保存、编辑页返回防丢等改进

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

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-9.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 发布文档

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
