# 拾光（Glimmer）`v3.14`

拾光（Glimmer）是一款 Android 倒数日 / 生日 / 纪念日应用，基于 Jetpack Compose + Material 3 构建。

## 版本信息

- `versionName`：`3.14`
- `versionCode`：`19`
- 发布日期：`2026-06-30`

## 核心能力

- 管理倒数日、生日、纪念日等普通事件
- 同时支持公历与农历事件
- 支持按天、周、月、半年、年重复
- 支持“提前 N 天 + 固定时间”的提醒配置
- 支持系统日历 / 日程同步、权限处理与同步状态反馈
- 首页支持搜索、筛选、置顶、自定义排序与月历视图
- 支持可配置桌面小组件，包含 2x2 / 3x3 / 4x2 模板、透明背景、筛选、排序与密度设置
- 支持 JSON 导入 / 导出，并可导入“记得日子” `.mdb` 备份文件

## 3.14 版本重点

- 优化首页月历日格：移除窄格内的农历小字，避免实机显示为“四...”等截断残片
- 月历日格改为仅显示公历日期、今日标记和事件点/数量，完整事件标题保留在选中日期列表
- 选中日期区域新增农历副标题，在更宽的详情区展示农历信息
- 重大节点改为按生日、纪念日、普通倒计时分别评分，优先展示百日、周年、半岁和倒计时阈值等语义节点
- 首页顶部整合搜索、筛选、排序和设置入口，搜索展开时直接替换标题区域
- “卡片 / 列表 / 月历”切换改为更轻的宋式页签，减少首屏控制区占位
- 浅色卡片改为暖宣纸色和旧纸色层级，列表改为透明书目行，月历改为更轻的历书格子

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

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-14.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 发布文档

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
