# 拾光（Glimmer）`v3.16`

拾光（Glimmer）是一款 Android 倒数日 / 生日 / 纪念日应用，基于 Jetpack Compose + Material 3 构建。

## 版本信息

- `versionName`：`3.16`
- `versionCode`：`21`
- 发布日期：`2026-07-06`

## 核心能力

- 管理倒数日、生日、纪念日等普通事件
- 同时支持公历与农历事件
- 支持按天、周、月、半年、年重复
- 支持“提前 N 天 + 固定时间”的提醒配置
- 支持系统日历 / 日程同步、权限处理与同步状态反馈
- 首页支持搜索、筛选、置顶、自定义排序与月历视图
- 支持可配置桌面小组件，包含 2x2 / 3x3 / 4x2 模板、透明背景、筛选、排序与密度设置
- 支持 JSON 导入 / 导出，并可导入“记得日子” `.mdb` 备份文件

## 3.16 版本重点

- 首页近期入口收纳到右上操作菜单，减少常驻控件占位
- 列表视图改为更简洁的桌面小组件式书目行，保留标题与时间核心信息
- 卡片视图保留分类、重复、提醒等辅助行，兼顾扫描和上下文
- 月历移除“本月重点”，点击日期展示具体内容，点击顶部年月可切换月份
- 新建 / 编辑页合并模板与分类，日期前置，重复与提醒统一收口
- 详情页主卡轻量化，下方无框表格承载农历、重复、提醒、备注和警告
- 新增宋式分享卡，可保存图片或调起系统分享
- 设置页优化显示分组、默认提醒预设、自定义节点删除触控与开关无障碍语义

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

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-16.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 发布文档

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
