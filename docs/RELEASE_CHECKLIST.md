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
- [ ] 使用正式发布密钥重建并验证 Direct APK / Play APK / Play AAB 的签名与权限；临时签名配置及 Debug APK 验证不等于正式产物，正式发布密钥产物仍需最终重跑
- [x] Direct Debug APK 包含 `REQUEST_INSTALL_PACKAGES`，Play Debug APK 不包含该权限
- [ ] publisher 删除 owned draft 中的所有旧资产，且整个 Release 只保留唯一的 exact Direct APK；Play AAB 只交付 Play Console

## 二、数据与核心功能成熟度

- [ ] 新建、编辑、删除、撤销、搜索、筛选、置顶和三种首页排序均通过回归
- [ ] JSON / CSV 导入导出往返后关键字段不丢失，错误输入可解释且不会覆盖现有数据
- [ ] “记得日子” `.mdb` 导入、数据库迁移与重复数据处理通过回归
- [ ] 公历、农历、按天 / 周 / 月 / 半年 / 年重复在月末、闰年和跨年边界正确
- [ ] 通知提醒在当天、提前多天、设备重启和权限变化后仍按预期工作
- [ ] 系统日历存在可写账户时同步正确；无可写系统日历时提示清楚且事件主体仍安全保存
- [ ] 分享图片、系统分享面板、更新检查和 Direct APK 安装路径可用

## 三、首页与桌面小组件

- [x] 首页卡片、列表、月历在浅色 / 深色主题与 150% 系统字体下文字可读、布局不溢出
- [x] 首页按天数、按日期、自定义排序均保持置顶事件在前
- [x] 小组件“跟随首页”与首页顺序一致，3.17 导出的 22 条脱敏事件 fixture 回归通过
- [x] 首页选择按距离天数排列时，小组件置顶项在前，其余项目按相同天数规则排序
- [x] 小组件“置顶优先”和“最近优先”显式模式维持各自定义，不受“跟随首页”修复影响
- [ ] 小组件默认配置页显示 1-5 格“预览宽度 / 预览高度”，说明不会强制改变 Launcher 物理尺寸
- [ ] 添加、编辑、删除多个小组件时实例配置互不污染，事件和设置变化后可及时刷新
- [ ] 小组件内容筛选、排序、密度、边框、圆角、文字对比和农历前缀配置生效
- [ ] 透明、半透明、宣纸、青瓷、朱印背景在浅色 / 深色 Launcher 下均可读

## 四、体验、无障碍与性能

- [ ] 新建 / 编辑输入法、拼音组合态、硬件键盘、旋转和返回手势无数据丢失
- [ ] TalkBack 可识别主要按钮、开关、展开状态、列表项和日期选择器
- [x] 常用触控目标满足尺寸要求，颜色与文字对比通过项目守卫测试
- [ ] 冷启动、首页滚动、月历切换、详情与设置导航无明显卡顿或异常内存增长
- [x] Android 8、Android 12 和当前 target SDK 设备至少各完成一轮核心 smoke
- [x] Direct 与 Play 渠道关于页、权限和更新能力符合各自渠道约束

## 五、4.0 实测记录

- JVM 回归（2026-07-16）：Direct / Play 各 409 项通过，0 failures / 0 errors / 0 skipped；覆盖农历重复、导入校验、数据库恢复、首页排序、小组件解析、无障碍架构、当前界面语言解析及渠道更新契约等确定性行为。`compileDirectDebugAndroidTestKotlin`、`assembleDirectDebug`、`assemblePlayDebug` 同轮通过。
- 数据库迁移（2026-07-16）：从已发布 v3.4 的真实 Room v6 schema 启动，依次执行生产迁移 6→7→8→9→10；API 37 上 2 项 connected migration 测试通过，验证删除字段、新增字段、核心数据保留与 v10 schema 严格校验。
- UI / 无障碍（2026-07-16）：API 37 验证首页列表与卡片深色模式、月历 150% 系统字体、日期滚轮与设置单选组。修复事件文字和选中日期对比、48dp 触控目标、滚轮可调语义、装饰性空状态按钮重复朗读及设置单选行合并语义；镜像内置 TalkBack 已以触摸探索模式绑定，首页视图标签取得真实读屏焦点，UI 语义树能识别月历入口、月份切换、事件和添加按钮的中文标签。自动注入手势未能可靠完成开关、展开区与日期选择器的端到端顺序遍历，因此总项仍保留未勾选。
- 性能（2026-07-16）：分享卡 1080×1350 渲染移至 Default dispatcher，PNG 写入移至 IO；Debug 冷启动 5 次中位数约 1630ms，首页滚动 Perfetto trace 无丢样。Release 性能画像与真机长时内存观察仍未完成。
- 工程门（2026-07-16）：五项 lint / vital lint 均通过，DirectDebug、DirectRelease、PlayRelease 报告均为 `No issues found.`；新增 Android pull request CI，执行双渠道 JVM、AndroidTest 编译、五项 lint / vital lint 与双渠道 Debug 构建。
- 渠道验收（2026-07-16）：API 37 `emulator-5554` 安装 Direct / Play Debug APK；Direct 关于页显示 `4.0` 和“探寻新章”，Play 关于页显示 `4.0-play` 和“更新由应用商店管理”，且不暴露 Direct 下载或安装入口。`aapt` 验证 Direct Debug APK 包含 `REQUEST_INSTALL_PACKAGES`、Play Debug APK 不包含；正式发布密钥 Release 产物的最终权限与签名检查仍未执行。
- 模拟器（2026-07-16）：API 37 `sdk_gphone16k_arm64`，以 22 条脱敏事件完成 Custom / ByDays / ByDate 首页排序，并由两个真实 Pixel Launcher 小组件实例验证“跟随首页”和独立模式；置顶 `Event 06`、`Event 03` 始终在前。
- 系统兼容矩阵（2026-07-16）：Android 8.0 / API 26、Android 12 / API 31 与当前 API 37 均完成 Direct Debug 冷启动、新建事件、首页持久化、主要导航和 4.0 关于页 smoke，未发现崩溃；Android 12 无可写系统日历路径还验证事件主体安全保存。实测发现保存提示曾错误使用 Application 的英文环境，现改由当前界面 Context 解析资源并在同一路径复测为中文。
- Release 构建（2026-07-16）：以隔离的临时签名配置完成 Direct / Play APK 与 Play AAB，exact Direct 文件名、APK v2 签名及 AAB JAR 签名验证通过；正式发布密钥产物仍需最终重跑，不能把临时证书产物用于发布。
- 真机：未检查；待记录设备、系统版本、Launcher、小组件、通知、日历账户与安装升级验证。
- PowerShell 脚本运行：未检查；当前环境没有 `pwsh` / Windows PowerShell，未执行解析或 mocked dry run。
- 真实 GitHub mutation：未检查；未创建 Git ref 锁、draft、asset 或正式 Release，避免在未完成检查清单时改变远端发布状态。
- Backup / restore smoke（2026-07-16）：Android 8 / API 26 启用系统 LocalTransport 后完成 `backupnow`、`pm clear` 与指定 token restore；事件数据库、用户偏好和小组件偏好均恢复，两个 DataStore 文件恢复前后 SHA-256 完全一致，恢复后的事件可在界面读取。
- 筛选后的真实长按拖拽：未检查；ADB 手势未触发 Compose reorder 回调，仅有确定性合并、生命周期与 wiring 测试证据。

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
- [x] Release / update 子系统验收：Direct / Play JVM 各 409 项通过，`compileDirectDebugAndroidTestKotlin` 通过，两个渠道 Debug APK 均成功构建并安装到 API 37 `emulator-5554`；关于页运行时文案与 Debug APK 权限符合渠道约束。
- [ ] 使用正式发布证书重复完整构建、签名、权限、文件大小与 SHA-256 记录。
- [ ] 在隔离测试仓库运行 PowerShell publisher 的成功、并发锁、owned draft 恢复、残留锁与失败清理场景。

## Data Task 6 恢复验证（2026-07-16）

- [x] 农历重复、导入校验与重复数据回归：Direct / Play JVM 各 409 项通过；本次会话工作报告为 `.superpowers/sdd/data-task-6-report.md`，不作为长期发布附件。
- [x] 日历权限撤销恢复 smoke：`emulator-5554` / API 37；撤权后保留 provider ownership 与可重试错误、阻止删除，恢复权限后由应用清理 CalendarProvider 并成功删除 Room 事件；`/tmp/timeapk-data-task6-2026-07-16/rerun-682e004/` 仅为本机临时证据目录，不作为长期发布附件。
- [x] Backup / restore smoke：Android 8 / API 26 的 LocalTransport 完成事件、用户偏好和小组件配置的备份、`pm clear` 与恢复；恢复后 DataStore 哈希一致，事件数据库和界面内容可读取。

## Home / Widget Task 6 验证（2026-07-16）

- [x] 3.17 导出的 22 条脱敏事件 fixture；首页 Custom / ByDays / ByDate 与真实 Pixel Launcher `SORT_HOME` 小组件顺序一致，置顶 `Event 06`、`Event 03` 始终在前。
- [x] 两个真实小组件实例分别保持“全部事件 / 跟随首页”和“仅置顶 / 最近优先”配置；显式日期边界广播刷新两个 RemoteViews，下一次本地午夜 alarm 已布置。
- [ ] 筛选后的真实长按拖拽：ADB 三种手势均未触发 Compose reorder 回调，因此保持未验证；隐藏槽位合并规则由确定性单元测试与架构测试覆盖，不声称真实手势通过。
- 本次会话工作报告为 `.superpowers/sdd/home-task-6-report.md`；`/tmp/timeapk-home-widget-task6-2026-07-16-final/` 仅为本机匿名临时证据目录，二者均不作为长期发布附件。
