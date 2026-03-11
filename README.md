# 拾光（Glimmer）

`v3.5` 的 Android 倒计时 / 生日 / 纪念日应用，基于 Jetpack Compose + Material 3。

## 版本信息

- `versionName`: `3.5`
- `versionCode`: `9`
- 计划发布日期：`2026-03-11`

## 核心能力

- 倒计时、生日、纪念日等事件管理
- 公历与农历事件并存，支持农历重复计算
- 重复规则支持按天、周、月、半年、年循环
- 自定义提醒：支持“提前 N 天 + 指定时刻”
- 系统日历 / 日程同步，包含权限申请、失败提示与同步状态回写
- 首页支持搜索、分类筛选、排序组合与月历视图
- 表格模式与桌面小组件支持更完整的中文时间表达
- 支持 JSON 导入 / 导出与基础数据恢复

## 3.5 当前重点更新

- 完成提醒、里程碑、系统日历与小组件的统一重建链路，覆盖开机、时区变更、手动改时间和冷启动
- 完成提醒时间与提前天数的吸附式滚轮选择，提前 N 天会逐天写入系统日历 / 日程
- 完成设置页重构，统一提醒、系统日历同步、数据管理入口与宋式主题视觉
- 完成首页搜索、分类筛选、月历视图与跨天自动刷新
- 完成表格模式与小组件显示优化，支持“已经 / 还有 / X年X月X日”等更完整的中文表达
- 完成生日详情收敛：仅保留农历、岁数、属相、星座，移除出生时辰与八字五行
- 完成标签模型、数据库字段与导入导出支持下线，筛选语义统一为分类

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

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-5.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 发布文档

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
