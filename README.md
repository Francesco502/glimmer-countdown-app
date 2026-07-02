# 拾光（Glimmer）`v3.15`

拾光（Glimmer）是一款 Android 倒数日 / 生日 / 纪念日应用，基于 Jetpack Compose + Material 3 构建。

## 版本信息

- `versionName`：`3.15`
- `versionCode`：`20`
- 发布日期：`2026-07-02`

## 核心能力

- 管理倒数日、生日、纪念日等普通事件
- 同时支持公历与农历事件
- 支持按天、周、月、半年、年重复
- 支持“提前 N 天 + 固定时间”的提醒配置
- 支持系统日历 / 日程同步、权限处理与同步状态反馈
- 首页支持搜索、筛选、置顶、自定义排序与月历视图
- 支持可配置桌面小组件，包含 2x2 / 3x3 / 4x2 模板、透明背景、筛选、排序与密度设置
- 支持 JSON 导入 / 导出，并可导入“记得日子” `.mdb` 备份文件

## 3.15 版本重点

- 首页新增“今日 / 七日 / 本月 / 节点”近期摘要，帮助用户快速定位最近重要事件
- 月历新增“本月重点”，按生日、纪念日、倒数日和重大节点聚合当前月份事件
- 详情页新增提醒可信度状态条，并将底部操作切换为统一宋式工具栏
- 新增 / 编辑事件页加入实时预览卡、生日 / 纪念日 / 倒数日模板和命名宋式色板
- 设置页增加外观样张预览，并在系统日程同步区展示提醒与日程健康状态
- 启动页缩短动画节奏，加入“拾光”品牌字标，并继续提供 Android themed icon 单色资源

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

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-15.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 发布文档

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
