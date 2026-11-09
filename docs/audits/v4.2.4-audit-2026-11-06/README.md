# 拾光 Glimmer 4.2.4 版本健康度审计

- **审计对象**：`com.example.timeapk`，`versionName=4.2.4` / `versionCode=29`（2026-10-22 发布，当前 GitHub 最新正式版，公开资产 `glimmer-countdown-4-2-4.apk`，12,526,321 bytes，SHA-256 `5aa396a3…ea53`）
- **审计日期**：2026-11-06
- **技术基线**：AGP 9.1.0 / Gradle 9.3.1 / Kotlin 2.2.10 / KSP 2.3.9，compileSdk=targetSdk=36，minSdk=26，Compose + Material3，Room 2.8.4
- **方法**：三个并行只读静态审计（平台能力层 / 界面层 / 数据层）+ 依赖版本联网核对（Google Maven 元数据、GitHub Release API）+ 对本机发布证据（`.tmp/`、`docs/releases/`）的交叉核对；未修改任何源码，未执行构建。
- **边界**：本机 shell 因工作区 Windows 文件权限（沙箱 ACL）不可用，因此**未重跑 gradle / lint / 仪器测试**；文中「确定」= 源码可证，「待验证」= 需真机或运行期确认。明细报告：`.tmp/audit-v424/{platform,ui,data}.md`（本机，未入 Git）。

---

## 一、总体判断

**没有 P0（无启动崩溃、无大面积数据损坏、无阻断发布的缺陷）。** 4.2.4 的工程质量明显高于同类个人项目：673×2 单元测试、API 26/31/36 三平台仪器回归、lint 零问题、R8 + 资源压缩、正式签名与哈希校验、16 KiB 对齐、迁移链 1→11 完整且有严格测试。这些都已记录在 `docs/releases/v4.2.4-development.md` 与公开 Release 中，与本轮审计结论一致。

需要修的问题集中在三条主线：

1. **平台边界行为**：小组件跨天刷新不可靠、自更新下载/安装链路可静默失败；
2. **数据迁移语义**：JSON「导出 / 导入」不含配图且界面无提示、编辑保存整行覆盖后台写入；
3. **技术栈老化**：Compose 等 AndroidX 依赖停留在 2024 年初，与 2026 年的工具链和 targetSdk 36 形成约 2.5 年落差。

问题计数（去重合并后）：**P1 共 7 项、P2 约 30 项**，全部可用应用层补丁或计划内重构解决，**数据库无需升级**。

---

## 二、P1：建议在下一个版本修复

| # | 问题 | 证据 | 用户影响 | 建议（工作量） |
|---|---|---|---|---|
| P1-1 | **小组件跨天刷新不可靠**：日期边界只用非精确闹钟 `setAndAllowWhileIdle`，Doze / App Standby / 受限桶下可推迟数十分钟到数小时；`DATE_CHANGED` 分支只刷列表组件，单事件组件不刷；`WidgetRefreshReceiver` 在「没有列表组件」时提前 `return`，单事件组件的异步刷新拿不到 `goAsync()` 生命周期 | `widget/WidgetDateBoundaryScheduler.kt:35-40`；`widget/WidgetRefreshReceiver.kt:23-33`；`widget/CountdownAppWidgetProvider.kt:192-207`；`widget/SingleEventWidgetProvider`（无 DATE_CHANGED 过滤项） | 跨天后桌面仍显示「昨天」的天数——小组件是核心卖点，夜间息屏跨天最容易命中 | 有精确闹钟权限时用 `setExactAndAllowWhileIdle`，否则保留非精确 + 6h 周期 WorkManager 自愈；`onResume` 强制刷一次；单事件组件纳入同一刷新与 `goAsync`（S–M） |
| P1-2 | **自更新链路无完整性校验**：下载只比对 `Content-Length`，忽略 GitHub Release API 已返回的 `assets[].digest`（本次核对确认公开资产含 `sha256:5aa396a3…`）；资产 URL 无 scheme/域名白名单 | `update/GitHubReleaseUpdateChecker.kt:149-160`；`update/DirectReleaseAssetPolicy.kt:41-47`；`update/UpdateInstaller.kt:136-142,171-180` | App 无法区分「下载损坏」与「被投毒」；对唯一分发渠道（GitHub APK）而言，这是自更新的地基 | 解析 `digest` 并在 `Files.move` 前校验 SHA-256；`browser_download_url` 强制 `https://github.com/` 前缀（M） |
| P1-3 | **安装阶段可能静默失败**：`startActivity(ACTION_VIEW)` 在 App 已被切到后台时会被后台启动限制（BAL）拦截，但函数仍返回 `true`；下载协程绑定 `rememberCoroutineScope()`，用户离开「关于」页即被取消且无提示 | `update/UpdateInstaller.kt:68-71,82-90`；`ui/settings/SettingsSubScreens.kt:2018,2079-2090` | 用户点「下载并安装」→ 切出去看了一眼消息 → 回来什么都没发生、也没报错，只能重来 | 下载放 `TimeApplication` 级 scope；安装前判前台；改用 `PackageInstaller` 会话并回传结果；失败给可重试提示（M） |
| P1-4 | **JSON 导出静默不含配图**：导出字段无 `imagePath`，导出/导入界面无任何说明 | `data/EventJson.kt:9-37`；`res/values/strings.xml:122-141`；`CHANGELOG.md:57` 已承认该设计 | 用户把「导出 / 导入」当作换机备份，迁移后全部照片丢失且无预警——数据信任问题 | 入口加一行说明（「配图不随 JSON 导出，换机请用系统备份」）；4.3 可评估含图 ZIP（S） |
| P1-5 | **详情页深浅色绕过应用内主题**：用 `isSystemInDarkTheme()` 而不是主题解析结果，强制浅/深色时详情卡配色走错分支 | `ui/detail/DetailScreen.kt:199-204` vs `ui/theme/Theme.kt:125-137` | 设置里「强制深色/浅色」对详情页不生效，可能出现对比度不足或色块割裂 | 改用 `MaterialTheme.colorScheme` 亮度判断，或由主题暴露 `resolvedDark`（S） |
| P1-6 | **编辑保存整行覆盖后台 CAS 写入**：保存时用打开编辑页的旧快照做 `@Update`，会回滚后台写入的 `scheduleEventId` / `targetCalendarId` / `lastScheduleSync*`；`@Update` 返回 `Unit`，0 行更新也算成功 | `ui/event/EventEntryViewModel.kt:449-459`；`data/EventDao.kt:36-37`；后台写侧为 CAS：`RescheduleAllWorker.kt:140-149`、`ReminderWorker.kt:83` | 时间窗口命中时可能出现系统日历条目重复/失去所有权；行已被删时编辑静默丢弃 | 保存也走 CAS（或只更新用户可改列），`@Update` 返回 `Int` 并断言 `==1`（M） |
| P1-7 | **无障碍语义缺陷**：① 详情页是叠在首页上的浮层，首页仍在语义树内，TalkBack 可聚焦并激活被盖住的条目；② 卡片外层 `mergeDescendants=true` + `contentDescription` 吞掉内层「切换时间显示」按钮 | `TimeApp.kt:207-236`（全量 grep 无 `invisibleToUser/focusGroup`）；`ui/home/HomeEventCards.kt:602-608` + `:666-674`、`:854-860` + `:936-945` | 读屏用户会「点到看不见的内容」，且无法单独操作切换按钮 | 背景加 `invisibleToUser()`/`clearAndSetSemantics`，内层按钮改为独立可聚焦节点或自定义动作（S–M） |

> 另有一项**战略级 P1**（技术债，非即时缺陷）：依赖栈老化，见第四节。

---

## 三、P2：建议按主题批量处理

### 3.1 数据与隐私

| 项 | 证据 | 建议 |
|---|---|---|
| DataStore 无损坏恢复：未配 `corruptionHandler`，所有偏好流裸 `map` 无 `catch`，文件损坏后设置全失且无法自愈 | `data/UserPreferencesRepository.kt:22,146-250` | `ReplaceFileCorruptionHandler { emptyPreferences() }`（S，性价比高） |
| 导入非事务：逐条 insert、分类先落库；进程被杀留下部分事件 + 孤儿分类（重复导入有去重兜底） | `ui/settings/SettingsSubScreens.kt:1533-1598,1881-1884` | `withTransaction` 包住「分类 + 事件」，日历/通知副作用移出事务（S–M） |
| 云备份收集备注/生日/配图且 `disableIfNoEncryptionCapabilities="false"`；备份同时含 db/wal/shm，恢复一致性宜实测 | `AndroidManifest.xml:16-19`；`res/xml/data_extraction_rules.xml:3-9`；`res/xml/backup_rules.xml:3-7` | 改 `true`；设置页说明备份范围；真机做一次云端恢复验证（S） |
| 图片孤儿：`draft-*` owner 仅 `onCleared` 释放，registry 在 `noBackupFilesDir`（不随备份迁移）→ 换机后旧图永不清理、无容量上限 | `ui/event/EventEntryViewModel.kt:171-174,314-327`；`ui/common/EventImages.kt:49-50`；`EventImageRegistry.kt:55-75` | 给 draft owner 加 TTL；对「DB 未引用 + mtime > 7d」做兜底清扫（M） |
| EXIF 只处理旋转，未处理镜像类方向（2/4/5/7） | `ui/common/EventImages.kt:172-177` | 按 9 种方向构造 Matrix（S） |
| 导入内存/耗时：8 MiB 二进制先整体转 UTF-8 String 再逐字节扫，OOM 未捕获；title/note 无长度上限；去重键含提醒开关，改过提醒再导入会产生重复 | `data/BackupImportParser.kt:82,309-320,160-172`；`EventJson.kt:62-63,79` | 先按首字节判格式；截断超长字段；去重键降为（title,date,category）；catch OOM（M） |
| 冷启动主线程 I/O：`Application.onCreate` 读/写 properties；图片清理持锁 `runBlocking` 等 DB | `TimeApplication.kt:57`；`ui/common/EventImages.kt:68-74` | 移出主线程；锁外取快照（S） |
| 分享图缓存不清理、固定 100% PNG | `ui/detail/ShareImageStore.kt:23-43` | 下次进入清理非当前文件或用 JPEG（S） |
| 事件表无索引，DAO 全表查询 + 排序 | `app/schemas/…/11.json`；`data/EventDao.kt:13-14,23-31` | 数据量增长前加 `Index(date)`；需 DB v12 + 迁移（放 4.3） |

### 3.2 平台与系统行为

| 项 | 证据 | 建议 |
|---|---|---|
| 通知频道首次发通知才创建，渠道名/重要性对老用户永不更新；频道 id 字面量在两处重复 | `notifications/ReminderNotificationDelivery.kt:157-159,227-233`；`permissions/PermissionSupport.kt:37` | 在 `Application.onCreate` 统一建渠道并提常量（S） |
| 冷启动 `REPLACE` 全量重排提醒/日历同步，会打断进行中的同步（已有指纹增量，但每次仍全表快照） | `TimeApplication.kt:54`；`RescheduleAllWorker.kt:382-393,296-307` | 改 `KEEP` 或加 15 分钟节流；开关变化走脏标记（S–M） |
| 同步到系统日历时额外写 `MINUTES=0` 日历提醒 → 同一时刻 App 通知 + 日历通知两次 | `notifications/ScheduleSyncManager.kt:1430-1442` | 默认不写 Reminders 行（S） |
| edge-to-edge：全工程无 `enableEdgeToEdge()`，`statusBarColor/navigationBarColor` 在 API 35+ 已无效 | `MainActivity.kt`；`ui/theme/Theme.kt:91-92`；`res/values/themes.xml:2` | 接 `enableEdgeToEdge()` + `WindowInsets`，删无效赋值（M，随依赖升级一起做） |
| **待验证**：`CountdownWidgetService` 声明 `exported="false"` + `BIND_REMOTEVIEWS`，个别启动器可能拒绝绑定 → 列表型小组件空白 | `AndroidManifest.xml:108-111` | 真机放一次列表组件确认；必要时改 `exported="true"` 保留权限（S） |
| 小组件字号只跟应用内 `fontScale`，不看系统 `fontScale`；SP 被系统再放大一次 → 超大字号截断 | `widget/WidgetStylePolicy.kt:21-67`；`CountdownAppWidgetProvider.kt:114` | 读取系统 fontScale 并设上限（S） |
| 重排/刷新路径 `ConcurrentHashMap<Int, Mutex>` 只增不减；精确提醒与 WorkManager 双路径靠 SharedPreferences 镜像手工同步 | `notifications/ScheduleEventCoordinator.kt:8`；`ExactReminderAlarms.kt:60-90` | 锁表加回收时机；镜像纳入单一写入口（S） |

### 3.3 界面 / 无障碍 / 国际化

| 项 | 证据 | 建议 |
|---|---|---|
| 深色冷启动白闪：`splash_bg` 无 night 变体 | `res/values/colors.xml:4`；`values-night/colors.xml`；`themes.xml:2-5`；`values-v31/themes.xml:7-12` | 加 `values-night` 的 `splash_bg`（S，纯资源） |
| 触控目标 < 48dp + 固定高度：时间切换区 42dp、筛选 chip 32dp、色块 36dp、搜索框固定 48dp（200% 字号裁剪） | `HomeEventCards.kt:930-934`；`SongComponents.kt:311-313,358-366`；`HomeScreen.kt:995-1011` | `minimumInteractiveComponentSize` / `heightIn(min=…)`（S–M） |
| 无 `android:localeConfig`，未接 Android 13 per-app language；小组件配置页不跟随应用内语言，而组件正文跟随 | `AndroidManifest.xml:14-25`；`MainActivity.kt:30-33`；`LocaleUtils.kt:10-20`；`WidgetConfigActivity.kt:64-105` vs `WidgetLanguage.kt:9-18` | 加 `res/xml/locales_config.xml`；API 33+ 用平台 `LocaleManager`，低版本保留手工 wrap；配置页同样 wrap（M） |
| 首页 1348 行巨型 Composable，一次订阅 18 个 `StateFlow`（11 个 DataStore），组合期写 `mutableStateList` | `ui/home/HomeScreen.kt:105-127,204-207` | 拆 Toolbar/List/Calendar/ToolsPanel；订阅下沉（L，放 4.3） |
| 每分钟在主线程全量重算事件 UI 状态（含农历转换），月历按月展开也在组合期 | `ui/home/HomeViewModel.kt:727-736,491-518`；`HomeMonthCalendar.kt:98-101` | `flowOn(Default)`；只在跨天/跨小时重算（M） |
| 89 处 `collectAsState`、0 处 `collectAsStateWithLifecycle`，且未引入 `lifecycle-runtime-compose` → 后台仍在收集 Flow（分钟 ticker、Room 查询） | 全量 grep；`MainActivity.kt:43-51`；`ui/home/HomeScreen.kt:105-127` | 引入 `lifecycle-runtime-compose`，改 `collectAsStateWithLifecycle`（M，机械改动） |
| 事件自定义色作小字时亮色分支缺对比度兜底；选中态只靠颜色、部分按钮无 `role`/`stateDescription`；月历固定周一为首日且转场不过「减弱动效」通道 | `HomeEventCards.kt:542-552,646-654`；`HomeScreen.kt:1199-1247`；`HomeMonthCalendar.kt:182-199,201-228` | 走 `ensureTextContrast`；统一 `selectable`/`stateDescription`；`WeekFields` 取首日（S–M） |
| 分享卡用 `Typeface.SERIF` 而非应用字体；预览与导出各持一张 1080×1350 位图（峰值约 17 MB） | `EventShareImageRenderer.kt:34`；`EventShareCard.kt:61-65`；`DetailScreen.kt:211-213,468-482` | 用 `ResourcesCompat.getFont`；预览降采样（S–M） |
| `HomeScreen.kt:542` 编译器恒真条件提示（已在 4.2.4 记录中挂账） | `HomeScreen.kt:542` | 顺手清理（S） |

### 3.4 工程化与性能

| 项 | 证据 | 建议 |
|---|---|---|
| R8 keep 规则过宽，实质关闭优化：`-keep class androidx.compose.** { *; }`、`okhttp3.**`、`okio.**`、`datastore.**`，Room 规则与 AAR consumer rules 重复 | `app/proguard-rules.pro:8-32` | 只保留反射必需项，用「同版本 APK 体积 + 启动耗时」对比验证（S，收益可量化） |
| `material-icons-extended` 声明但**零使用**（全工程无 `Icons.*`，图标均为自绘 `SongLineIcons`） | `app/build.gradle.kts:239` | 删除该依赖（S，明显缩短 debug 构建/安装时间） |
| 无 Baseline Profile / `profileinstaller`；无 Macrobenchmark 模块 | 全工程 grep 无命中 | 生成 baseline profile，改善冷启动与首页滚动（M） |
| 无依赖更新自动化、无漏洞扫描；无崩溃/异常上报（GitHub 渠道下问题只能靠用户反馈） | 无 `dependabot.yml` / `renovate` / osv-scanner；`build.gradle.kts` 无 crash SDK | 加 Dependabot（或 Renovate）+ CI osv-scanner；评估可选的匿名崩溃上报（S/M） |
| lint 未 `warningsAsErrors`、无 detekt/ktlint/覆盖率门禁；CI 显式 `--no-build-cache`（发布可复现正确，但 PR 可开缓存） | `.github/workflows/android-ci.yml`；`gradle.properties` | 增加 `lint { warningsAsErrors = true }`、静态分析门禁、PR 开启 build cache（S） |
| 版本号分散：`gradle.properties`、`build.gradle.kts:115-116,193` 回退字面量、`scripts/android-runtime-smoke.py:25`、`scripts/tests/test_android_runtime_smoke.py:65-79`、README/CHANGELOG/docs | 同上 | 单一版本源（读 `gradle.properties`），smoke 脚本从属性读取（S） |
| `CountdownWidgetService.onDataSetChanged` 在主线程回调里 `runBlocking(IO)` 读库 + 偏好 | `widget/CountdownWidgetService.kt:46-61` | provider 侧预取快照或异步等待（M） |

---

## 四、依赖与工具链评估（与当前最新稳定版对照，2026-11-06 核对 Google Maven）

| 依赖 | 当前 | 最新稳定版 | 落差 |
|---|---|---|---|
| `androidx.compose:compose-bom` | **2024.02.00** | **2026.09.00** | ≈31 个月 |
| `compose.material3`（随 BOM） | 1.2.0 | 1.4.0（1.5.0-rc01 在途） | 2 个大版本 |
| `core-ktx` | **1.12.0** | **1.19.1** | 7 个 minor |
| `activity-compose` | **1.8.2** | **1.13.0** | 5 个 minor |
| `lifecycle-runtime-ktx` | **2.7.0** | **2.11.0**（且缺 `lifecycle-runtime-compose`） | 4 个 minor |
| `navigation-compose` | **2.7.5** | **2.10.2** | 3 个 minor |
| `work-runtime-ktx` | **2.9.0** | **2.12.0** | 3 个 minor |
| `datastore-preferences` | **1.0.0** | **1.3.0** | 3 个 minor |
| `exifinterface` | **1.3.7**（2023） | **1.4.2**（1.5.0-beta01 在途） | 2 个 minor |
| `room` | 2.8.4 | 2.8.5 | 基本同步 ✅ |
| `okhttp` | 5.3.2 | 5.x | 同步 ✅ |
| `cn.6tail:lunar` | 1.7.7 | — | 未核对 |
| 工具链 | AGP 9.1.0 / Gradle 9.3.1 / Kotlin 2.2.10 | 同期最新 | 同步 ✅ |

**判断**：工具链是 2026 年的，UI 依赖是 2024 年初的——这是当前最大的一笔技术债。

- **已付出的代价**：三处手工兼容层（`LocalOverscrollConfiguration`、`animateItemPlacement`、`Popup` 自写返回处理）与一处已失效 API（`window.statusBarColor/navigationBarColor`）；拿不到 Compose 1.7–1.10 的性能与稳定性修复、Material3 的组件修复、`collectAsStateWithLifecycle` 等现成能力；targetSdk 36 的边缘行为只能靠自己兜底。
- **升级风险与工作量**：1.6 → 1.10 跨 4 个 minor，上述兼容层会一次性暴露，Material3 视觉/行为有变化，需要截图基线与 API 26/31/36 回归。建议**独立分支、独立发版**，不要与热水修复混在一起（约 3–5 人日 + 5 人日回归）。

---

## 五、验证缺口（不是代码缺陷，但决定发版信心）

发布文档已如实记录，以下项目**在本机与云端都没有通过证据**：

- 实体手机（4.2.4）、折叠屏切换、完整桌面日期/时区矩阵、真实 Launcher 旧绑定迁移；
- 系统备份/换机恢复（含配图）、JSON 完整字段往返；
- `fontScale 2.0`、TalkBack 全流程、RTL、Android 15/16 边到边；
- 批量导入中途杀进程、配图替换/删除后的清理周期。

4.2.4 的发布依据是「自动化全绿 + 用户授权真机不阻断」。对于小组件跨天（P1-1）、自更新安装（P1-2/3）这类**只在真机环境暴露**的问题，建议下一版至少补一台实体机 + 一个原生 Launcher 的验证，否则修复也无法被证明有效。

---

## 六、结论：有没有必要提升版本？

**有必要，但不是「紧急热修」，而是「计划内的维护版 + 技术升级版」两步走。**

理由：

1. **修复必须通过新版本交付**：自更新比较的是版本名（`remoteVersion <= installedVersion` 即视为最新），覆盖安装要求 `versionCode` 递增。不升版就无法把修复送到 4.2.4 用户手上。
2. **P1 里有 3 类是「当前版本用户能直接踩到」的**：小组件跨天显示旧天数、更新安装静默失败、换机导出丢照片。它们不构成发布阻断，但会在日常使用中持续损耗信任。
3. **技术债已经进入「越晚越贵」区间**：Compose 落后 2.5 年，且已出现手工兼容层。再拖一个版本，升级成本只会继续上升。
4. **维护版成本很低**：4.2.4 之后没有 schema 变更、没有新依赖，P1 修复全部是应用层代码；按现有测试基建（673×2 单测 + 三平台仪器）回归可控。

### 建议路线

**4.2.5 / versionCode 30 —— 维护版（建议 2–4 周内，工作量约 5–7 人日）**

- 平台：P1-1 小组件刷新（精确闹钟可用时 + WorkManager 自愈 + `onResume` + 单事件组件纳入 `goAsync`）；P1-2/3 更新 digest 校验 + URL 白名单 + 前台判定/`PackageInstaller` + 下载移出页面 scope；冷启动重排节流；通知频道集中创建；
- 数据：P1-4 导出提示；P1-6 CAS 保存 + 0 行断言；DataStore corruptionHandler；导入事务；备份加密策略；
- UI：P1-5 详情页主题；P1-7 无障碍语义；深色 splash；48dp 与搜索框高度；`localeConfig` + 组件配置页语言；对比度兜底；
- 工程：删除未使用的 `material-icons-extended`；收敛 R8 keep 规则（对比 APK 体积/启动）；版本号单一来源；CI 加依赖漏洞扫描。
- **零 schema、零大版本依赖升级**；发布前补一台真机的「小组件跨天 + 覆盖安装 + 换机恢复」三项验证。

**4.3.0 / versionCode 31 —— 技术升级版（独立分支，建议 1–2 个月，工作量约 3–4 周）**

- Compose BOM 2024.02 → 2026.09（连带 activity / lifecycle / navigation / work / datastore / exifinterface 升级），迁移三处兼容层，接 `enableEdgeToEdge()`；
- 引入 `lifecycle-runtime-compose`，全量改 `collectAsStateWithLifecycle`；
- Baseline Profile + Macrobenchmark；
- 详情页改为导航目的地（含平板/折叠 hinge 断点）、首页拆分、主线程全量重算下沉；
- Room v12：`events(date)` / `events(category)` 索引 + 迁移测试；
- 可选：含配图的 ZIP 备份、`allowBackup` 隐私开关。

**不建议做的**：为单个 P2 发版；把依赖大升级与热水修复混在同一次发布；为「看起来更低」的版本号重置（会破坏升级链）；在没有真机验证的情况下宣称已修复小组件/更新相关问题。

---

## 七、审计明细

| 报告 | 覆盖范围 | 结论 |
|---|---|---|
| `.tmp/audit-v424/platform.md` | 通知/提醒/精确闹钟/日历同步/小组件/自更新/权限/Manifest | 无 P0，4 个 P1，11 个 P2；含 Android 15/16 适配逐项核对 |
| `.tmp/audit-v424/ui.md` | Compose 界面/主题/响应式/无障碍/i18n/性能 | 无 P0，3 个 P1，12 个 P2；含 12 条「已核对通过」以区分误报 |
| `.tmp/audit-v424/data.md` | Room/迁移/DAO/导入导出/图片存储/DataStore/备份/隐私 | 无 P0，4 个 P1，11 个 P2；迁移链与 schema 一致，无需升 DB |

**本轮已核对、确认无问题的关键点**（避免下一版重复排查）：迁移链 1→11 完整且无破坏性回退；精确闹钟三级降级与 Play 渠道移除正确；POST_NOTIFICATIONS 请求与投递前双重校验；开机/时区/改时/覆盖安装重排；日历写入所有权注册表与清理；备份白名单正确（含配图、不含 `updates/`）；组件暴露面最小；PendingIntent 可变性正确；无 `.so`（16 KiB 页对齐不适用）；图片解码在 IO 线程且有 LruCache；分享渲染在后台线程；列表 key 稳定；「减弱动效」有统一通道；主题自定义色有 WCAG 门槛；FileProvider 暴露面窄；无敏感日志。
