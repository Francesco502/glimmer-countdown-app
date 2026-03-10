# 拾光（Glimmer）

`v3.4` 的 Android 倒计时 / 纪念日应用，基于 Jetpack Compose + Material 3。

## 版本信息

- `versionName`: `3.4`
- `versionCode`: `8`
- 发布目标日期：`2026-03-10`

## 核心能力

- 倒计时、生日、纪念日等事件管理
- 公历与农历事件并存，支持农历每年重复计算
- 重复规则支持按日、周、月、半年、年循环
- 自定义提醒：支持“提前 N 天 + 指定时刻 + 自定义提醒语义”
- 通知点击可直达对应事件详情
- 系统日历 / 日程同步，含权限申请、失败提示与状态回写
- 首页支持搜索、Tag 过滤、排序组合与月历视图
- 表格模式支持固定分栏、长日期保护与倒计时字号自适应
- 桌面小组件支持尺寸分级展示与独立字号缩放
- App 全局字体支持基准字号缩放，并保留标题 / 正文字号层级
- 支持 JSON 导入 / 导出与基础数据恢复

## 3.4 当前重点更新

- 完成提醒调度链路修复，重复事件在非农历场景下不再因原始日期过期而失效
- 完成通知权限与系统日历权限链路补齐
- 完成首页搜索、Tag 过滤、月历视图与跨天自动刷新
- 完成宋式主题统一、全局字号与小组件字号能力
- 完成表格模式可读性重构：日期单独展示、左右分栏对齐、剩余时间完整显示
- 完成提醒 / 系统日历入口优化：编辑页始终可见，详情页可直接进入配置

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
