# 拾光（Glimmer）

`v3.8` 的 Android 倒数日 / 生日 / 纪念日应用，基于 Jetpack Compose + Material 3。

## 版本信息

- `versionName`: `3.8`
- `versionCode`: `12`
- 发布日期：`2026-04-03`

## 核心能力

- 管理倒数日、生日、纪念日等事件
- 同时支持公历与农历事件
- 支持按天、周、月、半年、年重复
- 支持“提前 N 天 + 固定时刻”的提醒配置
- 支持系统日历 / 日程同步、权限处理与同步状态反馈
- 首页支持搜索、筛选、置顶、自定义排序与月历视图
- 支持表格模式与桌面小组件
- 支持 JSON 导入 / 导出，用于备份与恢复

## 3.8 版本重点

- 小组件主题自适配增强：跟随系统深浅色、动态配色、字体缩放与整体圆角背景
- 调整小组件条目样式，去掉每条事件的独立外框，保持整体背景更克制
- 修复小组件点击链路，确保点击具体事件可正确带出 `open_event_id`
- 完善权限申请与降级保存流程，权限不足时可继续保存并直接返回首页
- 新增编辑页返回防丢机制，未保存修改时拦截返回并弹确认框
- 修复日期输入非法值校验、首页拖拽刷新中断、详情页与编辑页宽屏布局等问题
- 详情页操作区收敛为“提醒 / 置顶 / 编辑 / 删除”四按钮
- 设置首页改为细线分隔样式，减少圆角卡片感，更贴近宋式克制风格

## 构建与运行

```bash
# 直装渠道 Debug
./gradlew installDirectDebug

# Play 渠道 Debug
./gradlew installPlayDebug
```

```bash
# 直装渠道 Release APK
./gradlew assembleDirectRelease

# Play 渠道 Release AAB
./gradlew bundlePlayRelease
```

默认产物路径：

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-8.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 发布文档

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
