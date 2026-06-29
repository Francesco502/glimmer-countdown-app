# 拾光（Glimmer）`v3.13`

拾光（Glimmer）是一款 Android 倒数日 / 生日 / 纪念日应用，基于 Jetpack Compose + Material 3 构建。

## 版本信息

- `versionName`：`3.13`
- `versionCode`：`18`
- 发布日期：`2026-06-29`

## 核心能力

- 管理倒数日、生日、纪念日等普通事件
- 同时支持公历与农历事件
- 支持按天、周、月、半年、年重复
- 支持“提前 N 天 + 固定时间”的提醒配置
- 支持系统日历 / 日程同步、权限处理与同步状态反馈
- 首页支持搜索、筛选、置顶、自定义排序与月历视图
- 支持可配置桌面小组件，包含 2x2 / 3x3 / 4x2 模板、透明背景、筛选、排序与密度设置
- 支持 JSON 导入 / 导出，并可导入“记得日子” `.mdb` 备份文件

## 3.13 版本重点

- 桌面小组件新增尺寸模板、透明 / 半透明 / 青瓷 / 朱印等背景风格、内容筛选、排序、密度、边框、圆角和文字对比配置
- 新增小组件配置 Activity，添加小组件时可直接配置；设置页可管理默认小组件和已有小组件实例
- 继续推进宋式美学：统一设置页折叠分组、内置 Noto Serif SC 与 ZCOOL XiaoWei 字体、保留低体积系统字体选项
- Play 渠道移除直接 APK 安装权限，GitHub Release 只作为 Direct 渠道应用内更新来源
- 设置页减少平铺信息，细节配置收纳到折叠区或弹窗；日程同步状态改为展开后再刷新
- Release APK 体积从约 36M 收口到约 25M，同时保持真实宋体主视觉

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

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-13.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 发布文档

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
