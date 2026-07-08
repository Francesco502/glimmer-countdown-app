# 拾光（Glimmer）`v3.17`

拾光（Glimmer）是一款 Android 倒数日 / 生日 / 纪念日应用，基于 Jetpack Compose + Material 3 构建。

## 版本信息

- `versionName`：`3.17`
- `versionCode`：`22`
- 发布日期：`2026-07-08`

## 核心能力

- 管理倒数日、生日、纪念日等普通事件
- 同时支持公历与农历事件
- 支持按天、周、月、半年、年重复
- 支持“提前 N 天 + 固定时间”的提醒配置
- 支持系统日历 / 日程同步、权限处理与同步状态反馈
- 首页支持搜索、筛选、置顶、自定义排序与月历视图
- 支持可配置桌面小组件，包含 1-5 格预览宽高、透明背景、筛选、排序与密度设置
- 支持 JSON 导入 / 导出，并可导入“记得日子” `.mdb` 备份文件

## 3.17 版本重点

- 设置页新增独立“小组件设置”入口，不再把小组件配置塞在主题页内
- 主题页只保留主题、颜色、字体和应用字号，页面更短、更聚焦
- 已有桌面小组件的编辑入口复用新增小组件配置页，避免在列表里展开长表单
- 小组件字号、默认配置、已有小组件管理统一收敛到独立设置页
- 小组件玻璃背景、圆角、边线和文字颜色向系统原生小组件视觉靠拢
- 小组件宽高设置改为“预览宽度 / 预览高度”，避免误解为能改写 Launcher 桌面物理尺寸
- 默认小组件配置变更后会刷新桌面小组件，并可一键应用到已有小组件
- 新建事件标题输入修复拼音组合态和焦点问题；系统日历无可写日历时只提示，不自动创建本地日历

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

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-17.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 发布文档

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
