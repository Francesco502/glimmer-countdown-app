# 拾光（Glimmer）`v3.12`

拾光（Glimmer）是一款 Android 倒数日 / 生日 / 纪念日应用，基于 Jetpack Compose + Material 3 构建。

## 版本信息

- `versionName`：`3.12`
- `versionCode`：`17`
- 发布日期：`2026-06-24`

## 核心能力

- 管理倒数日、生日、纪念日等普通事件
- 同时支持公历与农历事件
- 支持按天、周、月、半年、年重复
- 支持“提前 N 天 + 固定时间”的提醒配置
- 支持系统日历 / 日程同步、权限处理与同步状态反馈
- 首页支持搜索、筛选、置顶、自定义排序与月历视图
- 支持表格式桌面小组件
- 支持 JSON 导入 / 导出，并可导入“记得日子” `.mdb` 备份文件

## 3.12 版本重点

- 导入入口新增“记得日子” `.mdb` 备份适配，可从旧备份中恢复事件标题、日期、备注与创建时间
- 导入前会先展示预览统计，确认后才写入事件
- 旧 `.mdb` 备份导入会按源 `oid` 去重，并跳过应用内已存在的同名同日同分类事件
- JSON 文件导入继续走原有逐条容错逻辑，文本粘贴导入保持兼容

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

- `app/build/outputs/apk/direct/release/glimmer-countdown-3-12.apk`
- `app/build/outputs/bundle/playRelease/app-play-release.aab`

## 发布文档

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
