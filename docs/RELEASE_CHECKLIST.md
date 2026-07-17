# 发布检查清单（v4.0）

**版本**：`4.0`（`versionCode=23`）  **发布日期**：待定  **发布状态：待验证**

4.0 的目标是成为可长期使用、可公开分发的成熟产品版本。4.0 尚未发布，最新公开版本仍为 3.17。只有获得本版本的新鲜证据后才可勾选项目；3.17 的历史验证记录不能直接沿用。

## 一、自动质量门

- [x] `gradle.properties` 中 `VERSION_CODE=23`、`VERSION_NAME=4.0` 正确
- [x] `app/build.gradle.kts` 的版本读取、APK 重命名、Direct / Play flavor 与 BuildConfig 开关正常
- [x] `README.md`、`CHANGELOG.md` 与发布文档均明确 4.0 尚未发布
- [x] `git diff --check` 未发现尾随空格或冲突标记
- [x] `./gradlew testDirectDebugUnitTest testPlayDebugUnitTest` 通过
- [x] `./gradlew compileDirectDebugAndroidTestKotlin` 通过
- [x] `./gradlew lintDirectDebug lintDirectRelease lintPlayRelease lintVitalDirectRelease lintVitalPlayRelease` 通过且无未解释 warning
- [x] 使用隔离的临时签名配置执行 `assembleDirectRelease assemblePlayRelease bundlePlayRelease` 通过，证明构建与签名门可工作
- [x] 临时签名构建生成 exact Direct APK `app/build/outputs/apk/direct/release/glimmer-countdown-4-0.apk` 与 Play AAB `app/build/outputs/bundle/playRelease/app-play-release.aab`
- [x] 候选提交使用与线上 v3.17 相同的正式发布证书重建并验证 Direct APK / Play APK / Play AAB 的签名、权限、大小与 SHA-256；最终仍须从不可变 `v4.0` tag 再次新鲜构建
- [x] Direct Debug APK 包含 `REQUEST_INSTALL_PACKAGES`，Play Debug APK 不包含该权限
- [x] publisher 隔离 PowerShell 状态机验证会删除 owned draft 中的所有旧资产，且整个 Release 只保留唯一的 exact Direct APK；Play AAB 只交付 Play Console

## 二、数据与核心功能成熟度

- [x] 新建、编辑、删除、撤销、搜索、筛选、置顶和三种首页排序均通过回归
- [x] JSON 导出/导入往返后关键字段不丢失，CSV 导出字段与转义正确；错误输入可解释且不会覆盖现有数据
- [x] “记得日子” `.mdb` 导入、数据库迁移与重复数据处理通过回归
- [x] 公历、农历、按天 / 周 / 月 / 半年 / 年重复在月末、闰年和跨年边界正确
- [x] 通知提醒在当天、提前多天、设备重启和权限变化后仍按预期工作
- [x] 系统日历存在可写账户时同步正确；无可写系统日历时提示清楚且事件主体仍安全保存
- [x] 分享图片、系统分享面板、更新检查和 Direct APK 安装路径可用

## 三、首页与桌面小组件

- [x] 首页卡片、列表、月历在浅色 / 深色主题与 150% 系统字体下文字可读、布局不溢出
- [x] 完全空首页只有一个 48dp 以上的中央“记录第一个日期”入口；搜索 / 筛选无匹配时可一键清除条件、保留底部新建入口且没有重复无障碍操作
- [x] 首页按天数、按日期、自定义排序均保持置顶事件在前
- [x] 小组件“跟随首页”与首页顺序一致，3.17 导出的 22 条脱敏事件 fixture 回归通过
- [x] 首页选择按距离天数排列时，小组件置顶项在前，其余项目按相同天数规则排序
- [x] 小组件“置顶优先”和“最近优先”显式模式维持各自定义，不受“跟随首页”修复影响
- [x] 小组件默认配置页显示 1-5 格“预览宽度 / 预览高度”，并说明这些选项只改变预览比例；Launcher 实际尺寸仍在桌面拖动边框调整
- [x] 添加、编辑、删除多个小组件时实例配置互不污染，事件和设置变化后可及时刷新
- [x] 小组件内容筛选、排序、密度、边框、圆角、文字对比和农历前缀配置生效
- [x] 透明、半透明、宣纸、青瓷、朱印背景在浅色 / 深色 Launcher 下均可读

## 四、体验、无障碍与性能

- [x] 新建 / 编辑输入法、拼音组合态、硬件键盘、旋转和返回手势无数据丢失
- [x] TalkBack 可识别主要按钮、开关、展开状态、列表项和日期选择器
- [x] 常用触控目标满足尺寸要求，颜色与文字对比通过项目守卫测试
- [ ] 冷启动、首页滚动、月历切换、详情与设置导航无明显卡顿或异常内存增长
- [x] Android 8、Android 12 和当前 target SDK 设备至少各完成一轮核心 smoke
- [x] Direct 与 Play 渠道关于页、权限和更新能力符合各自渠道约束
- [x] 缺少已保存偏好时新事件默认关闭提醒；显式开启 / 关闭偏好继续保留，预览与实际状态一致，默认保存普通事件不请求通知权限

## 五、4.0 实测记录

- JVM 回归（2026-07-16）：Direct / Play 各 410 项通过，0 failures / 0 errors / 0 skipped；覆盖农历重复、导入校验、数据库恢复、首页排序、小组件解析、无障碍架构、当前界面语言解析及渠道更新契约等确定性行为。`compileDirectDebugAndroidTestKotlin`、`assembleDirectDebug`、`assemblePlayDebug` 同轮通过。
- 数据库迁移（2026-07-16）：从已发布 v3.4 的真实 Room v6 schema 启动，依次执行生产迁移 6→7→8→9→10；API 37 上 2 项 connected migration 测试通过，验证删除字段、新增字段、核心数据保留与 v10 schema 严格校验。
- UI / 无障碍（2026-07-16）：API 37 验证首页列表与卡片深色模式、月历 150% 系统字体、日期滚轮与设置单选组。修复事件文字和选中日期对比、48dp 触控目标、滚轮可调语义、装饰性空状态按钮重复朗读及设置单选行合并语义；镜像内置 TalkBack 已以触摸探索模式绑定，首页视图标签取得真实读屏焦点，UI 语义树能识别月历入口、月份切换、事件和添加按钮的中文标签。首轮自动注入手势未能可靠完成开关、展开区与日期选择器的顺序遍历，后续以真实 TalkBack 操作结合确定性语义回归补齐，详见本节 2026-07-17 复测记录。
- 性能（2026-07-17）：分享卡 1080×1350 渲染移至 Default dispatcher，PNG 写入移至 IO；Debug 首页滚动 Perfetto trace 无丢样。临时测试签名 Direct Release 在 API 37 模拟器执行 10 次强制停止启动，其中 9 次 COLD 为 488–827ms、中位数 719ms，另 1 次被系统标记为 WARM（551ms）。纠正“系统返回可能只关闭输入法”的旧测法后，以页面顶部返回键完成预热及 100 次真实“添加事件 / 返回首页”循环；三次采样的稳定值从基线到第 100 次为 TOTAL PSS 144,127KB→151,654KB，其中第 25→100 次仅增加 2,169KB，Java Heap 40,208KB→40,240KB、Native Heap 16,816KB→17,376KB，且 `Activities=1`、`ViewRootImpl=1`、`AppContexts=6` 始终不变。第 100 次循环后触发完整堆转储与 GC，Local Binder 降至 23；Shark 2.14 分析当前堆与 2026-07-16 的旧 Release 堆均为 0 application leaks、0 library leaks、0 unreachable objects，未发现确定的持续引用链。另以开启系统动画的临时测试签名、混淆 Direct Release 执行月历前后翻页、详情 / 设置往返及三种首页视图切换：282 帧中位数 17ms、P90 29ms、P95 34ms，29 帧（10.28%）超过截止时间；ADB 输入注入产生 325 次高输入延迟，模拟器结果不作为真机流畅度结论。堆与帧数据保存在 `/tmp/timeapk-v4-perf-20260717-d0501c4/`；因真机长时与导航流畅度验证仍未完成，性能总项继续保留未勾选。
- 工程门（2026-07-16）：五项 lint / vital lint 均通过，DirectDebug、DirectRelease、PlayRelease 报告均为 `No issues found.`；新增 Android pull request CI，执行双渠道 JVM、AndroidTest 编译、五项 lint / vital lint 与双渠道 Debug 构建。
- 渠道验收（2026-07-16）：API 37 `emulator-5554` 安装 Direct / Play Debug APK；Direct 关于页显示 `4.0` 和“探寻新章”，Play 关于页显示 `4.0-play` 和“更新由应用商店管理”，且不暴露 Direct 下载或安装入口。`aapt` 验证 Direct Debug APK 包含 `REQUEST_INSTALL_PACKAGES`、Play Debug APK 不包含；后续正式签名 Release 产物复测结果见 2026-07-17 记录。
- 模拟器（2026-07-16）：API 37 `sdk_gphone16k_arm64`，以 22 条脱敏事件完成 Custom / ByDays / ByDate 首页排序，并由两个真实 Pixel Launcher 小组件实例验证“跟随首页”和独立模式；置顶 `Event 06`、`Event 03` 始终在前。
- 系统兼容矩阵（2026-07-16）：Android 8.0 / API 26、Android 12 / API 31 与当前 API 37 均完成 Direct Debug 冷启动、新建事件、首页持久化、主要导航和 4.0 关于页 smoke，未发现崩溃；Android 12 无可写系统日历路径还验证事件主体安全保存。实测发现保存提示曾错误使用 Application 的英文环境，现改由当前界面 Context 解析资源并在同一路径复测为中文。
- Release 构建（2026-07-16）：以隔离的临时签名配置完成 Direct / Play APK 与 Play AAB，exact Direct 文件名、APK v2 签名及 AAB JAR 签名验证通过；该轮临时证书产物不用于发布，正式证书复测结果见 2026-07-17 记录。
- 真机：未检查；待记录设备、系统版本、Launcher、小组件、通知、日历账户与安装升级验证。
- PowerShell 脚本运行（2026-07-17）：微软官方 PowerShell 7.5 Ubuntu 容器在 `--network none` 下执行仓库内模拟器，成功、锁竞争、owned draft 恢复、失败清理和残留锁 5/5 场景通过；该结果不等于真实 GitHub mutation。
- 真实 GitHub mutation：未检查；公开只读查询确认 v3.17 为非草稿、非预发布且只有一个 Direct APK，下载资产的 SHA-256 与 GitHub digest 一致。当前 `gh` 本地令牌已失效，最终发布前必须重新执行 `gh auth login`；本轮未创建 Git ref 锁、draft、asset 或正式 Release。
- Backup / restore smoke（2026-07-16）：Android 8 / API 26 启用系统 LocalTransport 后完成 `backupnow`、`pm clear` 与指定 token restore；事件数据库、用户偏好和小组件偏好均恢复，两个 DataStore 文件恢复前后 SHA-256 完全一致，恢复后的事件可在界面读取。
- 输入 / 旋转（2026-07-17）：API 37 新建事件输入标题后旋转到横屏，标题与未保存状态保留，返回仍出现放弃修改确认；编辑链 connected 回归继续验证编辑标题跨 Activity recreate 保留、返回“留在此页”不丢内容、保存落库并返回首页。新增 `EventEntryImeInputTest` 直接经过 Android `InputConnection`：`setComposingText("pin")` 后提交“拼”，再注入硬件数字键得到“拼1”，Activity recreate 与返回确认后内容仍完整；另一条用例确认 IME 已交付的 composing 文本在重建后仍作为草稿保留。运行态将 Gboard 切换到已启用的简体中文拼音子类型，通过系统硬件按键输入 `pin`，真实显示“品 / 拼 / 频”等候选并用空格提交“品”；标题框和顶部摘要均只出现“品”，竖屏→横屏、系统返回、选择“留在此页”及恢复竖屏后内容均未丢失。测试结束恢复英文 Gboard、自动旋转并清除应用数据；完整 connected 套件 17/17 通过。
- 小组件多实例（2026-07-16）：在两个真实 Pixel Launcher 小组件实例运行验证基础上，新增 API 37 DataStore connected 回归；两个实例分别保存不同外观、内容范围和排序，更新默认配置不会覆盖已有实例，删除单个实例配置后仅该实例回退最新默认，另一个实例保持不变。
- 筛选后的真实拖拽（2026-07-16）：API 37 connected test 从“按天数”切换“自定义排序”，搜索得到 3 个可见事件并隐藏 1 个事件，通过 48dp 拖动把手移动中间项；界面换位与 DataStore 全局顺序均成功，隐藏事件保持原槽位。包含编辑恢复与小组件多实例回归在内的完整 connected 套件 10/10 通过；另以 ADB 在三张匿名卡片的可见把手上拖动 `QA_B`，UI 顺序从 C/B/A 变为 C/A/B，复核把手未遮挡卡片内容。
- 小组件设置说明（2026-07-17）：API 37 `emulator-5554` 的 Direct Debug 在 100% / 150% 系统字体下验证默认配置页；1-5 格预览宽高、完整 Launcher 尺寸说明及后续显示密度均可读且可滚动到达，系统字体已恢复 100%。同轮资源与架构契约覆盖默认中文、简体中文、英文文案及说明节点位置。
- 核心交互补充（2026-07-17）：API 37 Direct Debug 验证新建事件在拒绝通知与日历权限后主体仍可保存；详情页删除后 Snackbar 撤销成功恢复事件；置顶语义更新为“取消置顶”；搜索无结果时可清除搜索，生日与其他分类筛选结果正确。测试事件均已清理。全部重复周期和设备重启提醒仍未完整覆盖，对应综合清单项继续保留未勾选。
- 分享链路补充（2026-07-17）：详情分享预览成功，MediaStore 保存 `image/png` 的 1080×1350 图片，并真实打开 Android 系统分享选择器；测试图片和事件均已清理。后续正式签名 Direct APK 已完成 v3.17→4.0 原地升级及线上更新检查，分享与更新综合项据此关闭。
- 导入链路补充（2026-07-17）：无效文本 `notjson` 返回可解释错误且未改变现有事件；通过系统文件选择器导入脱敏 `.mdb` fixture，首次识别 2 条并导入 2 条，第二次识别 2 条重复且禁用导入，0 条解析错误。导入事件与下载目录 fixture 均已清理。
- 数据往返补充（2026-07-17）：API 37 Direct Debug 通过系统文件选择器导入 2 条包含农历、半年/年度重复、颜色、提醒时间以及逗号/引号/换行备注的 JSON fixture；导出 JSON 忽略数据库 ID 后逐字段与原始 fixture 完全一致，CSV 由标准解析器还原为 12 列、2 条记录且复杂备注无损。再次选择最新 JSON 导出文件时显示“识别2，可入0，重事2，未解0”，导入按钮禁用；新增 JVM 回归固定 JSON 往返和 CSV 转义行为。
- 重复日期边界补充（2026-07-17）：聚焦 JVM 用例先复现 2020-02-29 年度事件在 2025-02-27 被错误反推为 2024-02-28；修复后覆盖公历按天 / 周 / 月 / 半年 / 年跨年、31 日短月裁剪后恢复、闰日闰年与非闰年，以及农历春节前、当天发生、发生后跨公历年搜索，确认所有周期保留原始日期锚点。
- 通知与重启补充（2026-07-17）：API 37 Direct Debug 导入“当天提醒”和“提前 1 天提醒”两条匿名事件，在拒绝 `POST_NOTIFICATIONS` 时执行真实模拟器重启并收到 `BOOT_COMPLETED`；重启后两条 `ReminderWorker` 均重新入队，`RescheduleAllWorker` 成功结束。运行态继续验证“拒绝 → 授予 → 再次拒绝”权限切换，两条提醒始终保留且每次统一重排均成功。测试同时发现从未同步过系统日历的普通提醒会被错误执行日历清理，并在无日历权限时进入永久重试；现仅对仍有日历 provider ownership 或既有同步错误的事件执行清理，干净数据复测不再产生虚假日历错误或重试循环。
- 系统日历正向同步补充（2026-07-17）：API 37 Direct Debug 在设置中显式选择独立的 `TimeAPK_v4_QA` 可写本地日历；从 UI 新建当天提醒后，Room 保存 `scheduleEventId=251`、`targetCalendarId=6` 且同步错误为空，CalendarProvider 对应记录位于日历 6。编辑标题时原地复用事件 ID 251 且无重复；关闭同步后 provider 记录消失并清空本地 ownership 字段；重新开启同步生成活动记录 252，从详情删除后 CalendarProvider 与 Room 均无残留。新增 connected 回归会自行创建唯一的本地日历，覆盖新增、原地更新、提醒记录、关闭同步和活动记录清理，并在 `finally` 删除临时账户；手工 QA 日历、测试事件和 Direct 测试应用均已清理。Android 12 无可写日历与 API 37 撤权恢复证据继续覆盖负向路径。
- 小组件外观补充（2026-07-17）：API 37 Pixel Launcher 真实实例验证小 / 大圆角与系统宣纸背景有 / 无边框的视觉差异。进一步复现应用进程退出后 Launcher 将背景切到夜间资源、但旧 RemoteViews 写死文字色导致“深底深字”；现由主题自适应布局管理自动文字色，在进程退出状态下从深色切回浅色，实测分别为深底浅字与浅底深字。配置预览同步反映圆角、农历前缀及紧凑 / 标准 / 宽松密度。最终人工矩阵覆盖透明、半透明、宣纸、青瓷、朱印五种背景的浅色 / 深色 Launcher：全部可读；同一圆角内连续执行墨线→透明、青瓷→朱印等切换时不再残留旧背景。期间发现 Android 12+ 仅可靠识别顶层 `@android:id/background`，现已同步基础与 v31 布局，并让配置保存等待 RemoteViews 刷新完成后再关闭。
- 最终质量门补充（2026-07-17）：当前候选代码强制重跑 Direct / Play JVM 各 425 项，均为 0 failures / 0 errors / 0 skipped；API 37 connected 19/19 通过，包含完整 6→10 迁移、编辑恢复、拼音组合态与硬件键盘输入、输入框标签语义、筛选拖拽、小组件多实例、CalendarProvider 正向同步、小组件根背景结构、透明专用布局、圆角 RemoteViews，以及开关、展开区与日期滚轮 Compose 语义回归。`compileDirectDebugAndroidTestKotlin`、Direct / Play Debug 构建与五项 lint / vital lint 同轮成功，DirectDebug、DirectRelease、PlayRelease 报告均为 `No issues found.`
- 输入框无障碍补充（2026-07-17）：API 37 新鲜 UI 树复现标题编辑框缺少字段名称并被标记为 `NAF=true`；根因是自定义输入框把“标题 / 备注”渲染为独立视觉文本，却未把标签写入编辑框语义。新增失败测试后为复用组件补齐标签语义，运行时 UI 树不再出现 NAF，旋转回竖屏后标题节点明确为 `content-desc="标题"`、备注节点为 `content-desc="备注"`；聚焦 JVM 6/6 与编辑 connected 2/2 通过。
- 输入 / TalkBack 复测补充（2026-07-17）：API 37 在 150% 系统字体与真实 Gboard 下输入匿名标题，关闭 / 重开键盘、旋转横屏并旋回后草稿仍保留，顶部栏与表单稳定布局正常；首次改字体时 Gboard 自身的“Keyboard font size updated”横幅会短暂改变输入法高度，横幅关闭后不再复现。内置 TalkBack 17.0.0 已绑定，`touchExplorationEnabled=true`；首页“卡片”入口真实朗读“已选择、单选按钮、第 1 个，共 3 个”，匿名事件列表项和设置按钮均可通过触摸探索激活，详情页与设置页返回按钮取得绿色读屏焦点。设置页开关经 TalkBack 激活后从“开”变为“关”，展开区从“收起设置分组”变为“展开设置分组”；日期对话框取得真实绿色焦点，语义树将年、月、日暴露为可调节点。新增 connected 回归确定性验证开关角色及“开 / 关”、展开区“已折叠 / 已展开”，并通过无障碍 `SetProgress` 将日期滚轮从 2026-07-17 调至 2027-08-18 后确认回调。Shell 连续滑动注入仍不够稳定，不作为顺序遍历证据；综合真实 TalkBack 激活、焦点与朗读证据及 19/19 语义回归后关闭 TalkBack 检查项。测试结束已恢复 100% 字体、自动旋转、原输入法子类型和关闭 TalkBack，测试包与数据均已清理。
- 正式签名与升级补充（2026-07-17）：从干净候选提交 `19ec656` 注入仓库外正式签名配置，`validateReleaseSigning` 与 `clean assembleDirectRelease assemblePlayRelease bundlePlayRelease` 成功。Direct APK、Play APK 与 Play AAB 的证书 SHA-256 均为 `3B:7C:B4:26:A8:26:64:F8:91:C6:95:11:CC:25:05:B6:71:28:C8:50:36:64:63:9F:29:72:91:DA:4E:A9:03:CA`，与 GitHub v3.17 唯一 APK 完全一致；两个 APK 均通过 v2 签名验证，AAB 的 `jarsigner -verify` 返回 `jar verified.`，官方 bundletool 1.18.3 的 `validate`、`build-apks` 与模拟器 `install-apks` 全部成功。Direct APK 为 `26,393,406` bytes / `143d816f33148e1a403c7a47b7fce0a0edafea6382668967ec88b6a4314880d3`，Play APK 为 `26,389,242` bytes / `8e5f0fbeebe3e41a69796ce1a172196808c9eeb5127ab61a925b6f12cacdcffd`，Play AAB 为 `38,745,732` bytes / `42b7e5b3a94b443a2a501fc9df534f05a0b1e7aa125e506d4c8107cf25400e40`。Direct / Play 包名分别为 `com.example.timeapk` / `com.example.timeapk.play`，只有 Direct 含 `REQUEST_INSTALL_PACKAGES`；AAB manifest 同样确认 Play 包名且无该权限。
- 正式升级链路补充（2026-07-17）：下载的线上 v3.17 APK 哈希与 GitHub digest `3319513689f7178306d593c90dd6ce16bb50533d495c0fbab9e2f755ec589c5c` 一致。在 API 37 安装 v3.17、通过 UI 创建匿名事件后，以正式签名 4.0 Direct APK 执行 `adb install -r`；`versionCode` 从 22 升至 23、`firstInstallTime` 保持不变、通知授权与日历拒绝状态保留，首页和详情均能读取原事件。关于页显示“版本 4.0”，真实 GitHub 更新检查返回“已是最新版本”，不会向 3.17 降级。正式 Play APK 可与 Direct 共存，关于页显示“版本 4.0-play / 更新由应用商店管理”；AAB 生成的测试 APK 集也可冷启动。全程 crash buffer 为空，最后卸载两个包、清理匿名数据和设备临时文件，并确认字体、旋转、输入法与 TalkBack 状态恢复。该候选产物不替代最终 `v4.0` tag 新鲜构建与线上安装复验。
- PowerShell publisher 隔离补充（2026-07-17）：新增 `scripts/tests/publish-release-mock-harness.ps1`，每个场景将发布脚本、版本、变更日志和匿名假 APK 复制到独立临时仓库，用假 `git` / `apksigner` 与内存 GitHub REST 状态机执行真实 `publish-release.ps1`；测试容器使用微软官方 PowerShell 7.5.0、只读输入和 `--network none`。5/5 场景通过：新发布最终只含 exact Direct APK 并清理锁；并发锁 422 在创建 Release 前失败；带旧 ownership marker 的 draft 删除两个旧资产后恢复发布；上传失败保留 draft 但清理锁；上传与清理同时失败时保留残留锁阻止重试。未读取真实凭据、未访问网络、未创建远端 ref / draft / asset / Release。
- 首页空状态与提醒默认值补充（2026-07-17）：候选提交 `1b2ec22`、`2ed017e`、`732b8a9` 以测试先行完成唯一中央空状态 CTA、无匹配清除条件、月历筛选结果保持、缺省提醒关闭、显式偏好保留和语言镜像 KTX 写入。API 37 运行态确认完全空首页仅有一个可点击“记录第一个日期”入口且隐藏底部添加按钮；搜索 `NOMATCH999` 后显示“清除搜索与筛选”并保留底部“添加事件”；新建页默认显示“未设置提醒”，保存普通事件未出现通知权限对话框。最新完整 connected 套件 20/20 通过，0 skipped / 0 failed。
- 语言冷启动补充（2026-07-17）：API 37 通过应用界面完成中文→英文→中文切换；英文与中文各强制停止冷启动一次，日志均只有一次 MainActivity START / Displayed 且无额外重建，界面语言正确、crash buffer 为空。该 Debug 启动计时不作为 Release 性能结论。
- 4.0 截图补充（2026-07-17）：重新导入 3.17 导出的 22 条脱敏事件并制作首页纸笺、月历、设置与小组件设置四张当前候选截图；逐张确认仅含 `Event 01`–`Event 22` 脱敏标题，README 已切换至 `docs/screenshots/4.0`。
- 日历清理可靠性补充（2026-07-17）：提交 `f2617c6`–`a9c0db6` 修复此前被忽略的 `CalendarCleanupResult`，清理失败不再继续重建或持久化成功指纹；活动 / 写入中 ownership 登记、v3.17 旧数据首次扫描、事件级互斥、插入中断恢复与同步 `SharedPreferences.commit()` 失败回滚均有 JVM 回归。提醒保存触发的修复会强制全量重排，通知 Worker 不再通过自取消重试制造重复通知。独立代码复核结论为通过，未增加外部 API、未修改 Room schema，也未迁移既有事件字段。
- 最新自动质量门补充（2026-07-17）：在最终生产代码 `a9c0db6` 上新鲜运行 Direct / Play JVM 各 487 项，均为 0 failures / 0 errors / 0 skipped；完整 API 37 connected 套件 20/20、`compileDirectDebugAndroidTestKotlin` 及五项 lint / vital lint 均通过，DirectDebug、DirectRelease、PlayRelease 三份报告均为 `No issues found.`。`LocalePreferenceMirror` 的 KTX 写入不再产生 `UseKtx` issue；日历 ownership 登记因必须检查同步 `commit()` 返回值，仅在最小作用域保留有理由的 suppression。仓库外临时 QA 证书构建只用于验证打包路径，不得发布，也不替代最终 `v4.0` tag 的正式证书新鲜构建。

## 六、发布动作

- [ ] 合并经过复核的 4.0 发布分支
- [ ] 确认最终代码与发布文档已提交，且工作区干净
- [ ] 创建并推送不可变的 exact `v4.0` tag
- [ ] 从该 tag 对应 commit 的工作树重新正式签名构建；不得复用旧构建产物
- [ ] 验证签名、渠道权限与 SHA-256
- [ ] 准备安全凭据环境：本地使用 `gh auth login` / `gh auth token`；CI 才注入 secret 且不打印
- [ ] 以 `CHANGELOG.md` 中的 4.0 小节作为 Release Notes
- [ ] 运行发布脚本，发布 `glimmer-countdown-4-0.apk`，确认不存在 Play APK / AAB 或任何其他资产
- [ ] 发布后安装线上 APK 并完成更新检查与关键链路 smoke

## Release / Update Task 4 验证（2026-07-16）

- [x] `ReleaseReadinessTest` 与 `ReleasePublicationContractTest` 覆盖：严格版本/tag、正式签名指纹门、exact Direct APK、published/manual draft 拒绝、publisher ownership marker、Git ref 锁、owned draft 全资产清理、Release 唯一资产集合、size/digest/URL 绑定和最终 GET 验证。
- [x] 未签名的最终 package 图会先进入签名校验并失败；release lint / compile 不因缺少本地密钥而读取秘密。
- [x] Play 关于页只显示商店托管更新说明，不暴露 Direct APK 检查或安装入口。
- [x] Release / update 子系统验收：Direct / Play JVM 各 410 项通过，`compileDirectDebugAndroidTestKotlin` 通过，两个渠道 Debug APK 均成功构建并安装到 API 37 `emulator-5554`；关于页运行时文案与 Debug APK 权限符合渠道约束。
- [x] 使用正式发布证书重复完整构建、签名、权限、文件大小与 SHA-256 记录；证书与线上 v3.17 一致，正式 Direct APK 已完成保留数据原地升级，AAB 已通过 bundletool 生成与安装测试。
- [x] 在隔离测试仓库运行 PowerShell publisher 的成功、并发锁、owned draft 恢复、残留锁与失败清理场景；无网络 PowerShell 7.5 容器 5/5 通过。

## Data Task 6 恢复验证（2026-07-16）

- [x] 农历重复、导入校验与重复数据回归：Direct / Play JVM 各 410 项通过；本次会话工作报告为 `.superpowers/sdd/data-task-6-report.md`，不作为长期发布附件。
- [x] 日历权限撤销恢复 smoke：`emulator-5554` / API 37；撤权后保留 provider ownership 与可重试错误、阻止删除，恢复权限后由应用清理 CalendarProvider 并成功删除 Room 事件；`/tmp/timeapk-data-task6-2026-07-16/rerun-682e004/` 仅为本机临时证据目录，不作为长期发布附件。
- [x] Backup / restore smoke：Android 8 / API 26 的 LocalTransport 完成事件、用户偏好和小组件配置的备份、`pm clear` 与恢复；恢复后 DataStore 哈希一致，事件数据库和界面内容可读取。

## Home / Widget Task 6 验证（2026-07-16）

- [x] 3.17 导出的 22 条脱敏事件 fixture；首页 Custom / ByDays / ByDate 与真实 Pixel Launcher `SORT_HOME` 小组件顺序一致，置顶 `Event 06`、`Event 03` 始终在前。
- [x] 两个真实小组件实例分别保持“全部事件 / 跟随首页”和“仅置顶 / 最近优先”配置；显式日期边界广播刷新两个 RemoteViews，下一次本地午夜 alarm 已布置。
- [x] API 37 connected 回归验证两个实例配置独立持久化、默认配置只作用于未配置实例、删除单个实例配置不会污染另一实例。
- [x] 圆角配置通过 connected RemoteViews 布局断言与 Pixel Launcher 实例复测；系统宣纸关闭边框时不再残留描边，应用进程退出后的深浅主题切换仍保持文字可读。
- [x] 筛选后的真实拖拽：API 37 connected test 使用独立 48dp 把手移动搜索子集中的中间项，界面换位、DataStore 持久化和隐藏全局槽位全部通过；ADB 可见把手拖拽复测顺序同样更新。
- 本次会话工作报告为 `.superpowers/sdd/home-task-6-report.md`；`/tmp/timeapk-home-widget-task6-2026-07-16-final/` 仅为本机匿名临时证据目录，二者均不作为长期发布附件。
