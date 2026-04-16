# 拾光（Glimmer）`v3.11`

拾光（Glimmer）是一款 Android 倒数日 / 生日 / 纪念日应用，基于 Jetpack Compose + Material 3 构建。

## 版本信息

- `versionName`：`3.11`
- `versionCode`：`16`
- 发布日期：`2026-04-16`

## 核心能力

- 管理倒数日、生日、纪念日等普通事件
- 同时支持公历与农历事件
- 支持按天、周、月、半年、年重复
- 支持“提前 N 天 + 固定时间”的提醒配置
- 支持系统日历 / 日程同步、权限处理与同步状态反馈
- 首页支持搜索、筛选、置顶、自定义排序与月历视图
- 支持表格式桌面小组件
- 支持 JSON 导入 / 导出，用于备份与恢复

## 3.11 版本重点

- 新增“新建事件默认提醒”设置，用户可自行配置默认提醒开关、提前天数和提醒时间
- 新建普通事件时，默认按用户设置初始化提醒；编辑已有事件时保持原有提醒数据不变
- 初始默认值调整为“提醒开启、提前 7 天、10:00 提醒”
- 设置项放入现有“里程碑与提醒”页面，沿用当前主题与滚轮交互，不影响既有提醒、同步和里程碑能力

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
