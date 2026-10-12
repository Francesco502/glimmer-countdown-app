# 发布检查清单（v4.1）

**版本**：`4.1`（`versionCode=24`）  **文档日期**：2026-10-10  **发布状态：已发布**

本清单记录 4.1 正式发布及其验证边界。所有通过结果必须来自本版新鲜执行，并绑定源码 revision、构建、设备和原始报告；未知、未执行和失败不能勾选。v4.0 的原始记录已完整归档到 [历史清单](releases/v4.0-checklist.md)，旧测试、旧截图和旧真机豁免均不延续到本版。

**唯一正式发布渠道：GitHub Release。** 唯一官方资产为 Direct APK `glimmer-countdown-4-1.apk`。Play flavor 仅保留用于兼容性与开发回归，不是正式发布工件或阻断项。最新公开版本为 [v4.1](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.1)。

## 当前阶段与证据身份

| 项目 | 当前状态 | 完成所需证据 |
|---|---|---|
| 源码 revision | 不可变v4.1 tag commit `6c52a3a0e7f495e015f12c035ea94bd614fc2813`，本地/远端解引用一致；app源码和APK不随后续文档/验证工具修复改变 | 原始tag对象 `e25f58372246d55c2d37fdf4d168adcccdb5e15e` 与最终CI source-revision |
| 自动测试与 lint | 最终标签CI37990007930：Direct/Play各575/575、native49/49、publisher10/10；三份lint各0 error/1个OldTargetApi，R8新鲜通过。发布工具a608b70真实apksigner格式修复另行10/10通过 | 最终标签原报告与发布工具单独revision/真实证书输出验证 |
| 模拟器运行与截图 | 最终标签debug回归及正式签名QA37992543286均通过；API26/31/36升级和AMS恢复、API36菜单/五页/分享返回完整，正式包截图已人工审阅 | 最终APK SHA8fbe…；签名QA原报告及本节记录，物理范围不扩大 |
| 物理真机 | 当前无实体手机；未执行，按现有授权记录剩余限制 | 用户已明确授权发布并说明无手机；安装/升级、提醒、日历、Launcher 与性能缺少物理设备证据，不能写成通过，也不继承 v4.0 豁免 |
| 正式签名 | 最终4.1与无认证公开下载均独立验签通过；单一签名者、v2/v3，复用v4.0证书 | SHA-256 `3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`；不记录密码/密钥/token |
| 正式 APK | 26,463,543 bytes，SHA-256 `8fbeee7590f89104995ecce98769842dc2a75cf84d8641efd6a0c7b67619e5b4` | `com.example.timeapk` /4.1/24、min26/target36、非debug、安装权限、16KiB zipalign通过 |
| GitHub 上传与公开复验 | Release408364983公开且非prerelease，唯一asset626232462；无认证下载字节/签名/身份通过，线上安装与更新QA37995185711三平台全成功 | APK源码6c52a3a、测试工具main6a1bdb5；首轮37994317407的API36旧版夹具导航失败原样保留 |

最终APK与无认证公开下载的证书 SHA-256 均为 `3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`，与线上v4.0一致。没有使用新建QA证书代替正式升级证书。

## 一、自动质量门

- [x] `gradle.properties` 的 `VERSION_NAME=4.1` / `VERSION_CODE=24`、Gradle 默认 fallback 和 APK 命名一致
- [x] README、CHANGELOG 和发布文档使用本版状态与 exact APK，历史证据可区分
- [x] `git diff --check` 无尾随空格或冲突标记
- [x] `./gradlew --no-daemon --no-parallel testDirectDebugUnitTest` 新鲜通过，记录完整测试结果
- [x] `./gradlew compileDirectDebugAndroidTestKotlin` 通过
- [x] `./gradlew lintDirectDebug lintDirectRelease lintVitalDirectRelease` 通过，无未解释 warning
- [x] publisher 隔离 PowerShell 状态机 10/10 通过，包括本地预检零 GitHub 请求、锁竞争、owned draft 恢复与全资产清理、失败清理、证书输出和临时资产 URL 场景
- [x] Debug APK 渠道身份与权限正确：Direct 包含 `REQUEST_INSTALL_PACKAGES`，Play 不包含；Play 开发回归单独记录
- [x] Direct Release 默认签名门保持；从最终 tag 完成 R8、资源压缩和正式签名，或按发布指引在 CI 生成未签名工件后在本机正式签名，未签名工件不得发布

## 二、4.1 修复要求对照表

| 要求 | 当前实现方向 | 必须验证的场景 | 当前结果 |
|---|---|---|---|
| 普通事件日历清理 | 共享事件入口判断所有权 | 无日历权限的普通事件：导入、撤销删除、提醒送达后无虚假同步错误且可删除 | main CI 无ownership免权限/删除策略与清理调用契约通过（内存策略/源码契约）；原生双回调导入无虚假错误通过。实际撤销、后台提醒送达链路未设备执行 |
| 真实日历记录保护 | 精确/写入中所有权、历史错误保留；删除统一检查 | 仅注册表有所有权也不能跳过；撤权、部分失败、恢复权限、重试及无关日历不误删 | main CI API36真实 CalendarProvider 临时本地日历的发现/创建/更新/关闭同步/清理与ownership通过；注册表、pending、部分删除失败和重试为 fake provider/内存测试。实际拒权限故障、用户日历及云端同步未证明 |
| 草稿与保存可靠性 | SavedStateHandle 与 ViewModel 保存任务 | 系统回收进程后的新建/编辑草稿恢复；保存中旋转/返回不重复落库；强制停止不冒充系统回收验证 | main CI 六项 SavedState、三项 IME 及编辑重建/返回确认/保存通过；实际 AMS 小量新建草稿同 task78、PID5601→5943、stopped=false，保存重开两字段精确通过。大草稿/编辑草稿为受控 Parcel 恢复；自然 LMK、保存中旋转/返回尚无独立设备证据 |
| 导入可靠性 | 进行中保护、输入边界、后台解析和重复识别 | 连续点击、页面重建、超限/畸形文件、重复数据、取消及失败后的再次导入 | main CI 原生双回调入口与 Room 三个事件各插入一次通过；JVM 上限/超限/畸形、重复识别与功能字段往返通过。系统文件选择器、导入中页面重建/取消/失败后重试未独立运行 |
| 小时显示 | 24 小时内显示已有小时数据 | 开关生效、当天/过去/超过 24 小时、卡片/列表及日期模式一致 | main CI JVM 未来不足24小时向上取整、当天/过去/远期及 DST 通过；320dp/font1.6 组件卡片13小时→1天切换与操作通过。实际列表与全部日期模式的设备矩阵未执行 |
| 纪念节点含义 | 固定天数使用天数文案 | 跨闰年 365 天明确显示天数，不误称日历一周年；节点显示和提醒一致 | main CI 公历/农历周年、闰日锚点、366天一周年及提醒计划回归通过；固定天数与周年资源文案分开。实际 WorkManager 周年通知送达未执行 |
| 详情状态 | 复用首页时间与偏好 | 跨午夜、回到前台、智能节点关闭、自定义节点与详情更新 | main CI 智能节点开关模型、原生时间切换/长备注操作及实际唯一事件详情通过；跨午夜、回前台、自定义节点实时更新未单独设备执行 |
| 日历状态与通知频道 | 本地化资源与普通提醒频道检查 | 中文/英文权限、无可写系统日历、provider 失败；相关频道关闭不能显示普通提醒 ready | main CI JVM 分类本地化、日历失败保留及相关频道关闭/未创建判断通过；系统设置内实际关闭频道与定时触发、通知最终送达未验证 |
| 分享一致性 | 预览与导出采用一致样式 | 浅色/深色、自定义字体、长标题、预览与 PNG 对照、保存和系统分享 | CI13 深色组件预览/导出、长文本与四项mock失败测试通过。签名预验收 API36 实际相册PNG、系统chooser打开/原生Back返回同详情已通过；接收方读取/发送及完整字体/主题矩阵未执行 |
| 首页交互与适配 | 筛选摘要、面板关闭、排序操作、短高度月历 | 系统返回/点外部关闭、清除筛选、长列表排序与无障碍移动、横屏六行月份可访问列表 | CI13 筛选子集真实拖动、边缘自动滚动（1996ms）、隐藏位置与落盘断言、无障碍移动及 Popup 原生返回通过；实际菜单触摸通过。320dp/font1.6 窄卡片、360×240 六周月份为已审阅组件证据，实际旋转/200% 字体/TalkBack 服务未执行 |
| 小组件配置 | 恢复草稿、保存中保护和错误反馈 | 旋转、保存失败/重试、多实例、实际 Launcher 背景/圆角/密度/文字和独立配置 | CI13 draft saver 受控恢复、多实例仓库/默认值与 RemoteViews 布局测试通过；实际应用内默认预览已审阅。WidgetConfigActivity 真实旋转、保存失败重试及 Launcher 绑定/缩放未执行 |
| 更新安装 | 对象级互斥、临时文件、长度校验和原子替换 | 重复下载、空/截断/超长响应、失败/取消保留完整 APK、安装权限与系统签名拒绝路径 | main CI 六项 JVM 下载及17项 GitHub版本/唯一资产解析通过，失败/取消保留完整APK；API26/31正式签名候选覆盖升级已通过。API36线上公开最新版本检查已通过；实际应用内安装权限/签名拒绝路径未执行 |

## 三、数据与核心功能

- [ ] 新建、编辑、删除、撤销、搜索、筛选、置顶和三种排序完整回归
- [ ] JSON 导出/导入完整字段往返、重复识别与 CSV 转义正确；失败不覆盖现有数据
- [ ] “记得日子” `.mdb` 导入和既有数据库升级保留事件及偏好
- [ ] 公历/农历、按天/周/月/半年/年重复覆盖月末、闰年和跨年
- [ ] 当天/提前多天提醒、设备重启、权限变化、通知频道关闭和后台省电路径有设备记录
- [ ] 可写日历同步与无可写日历提示、撤权清理和重试经过实际 provider 验证
- [ ] 分享保存、系统分享、GitHub 更新检查和 Direct APK 安装路径可用

提醒使用 WorkManager；延迟任务受系统调度与省电影响，本版不据此承诺分钟级准点。后台提醒需记录实际环境与到达时间，不能用单元测试替代设备证据。

## 四、UI、无障碍、小组件与性能

- [ ] 卡片、列表、月历、详情、新建/编辑和设置在浅色/深色下可读，采用同一候选新鲜截图
- [ ] 320dp 窄窗口、横屏/短高度、100%/150%/200% 字体检查内容与操作可达性
- [ ] 输入法、拼音组合态、软硬键盘、旋转和返回手势不造成未预期数据丢失
- [ ] TalkBack 主入口、开关、展开状态、日期选择器、自定义排序上移/下移可操作
- [ ] 筛选反馈、无匹配清除操作、面板关闭和触控目标实际可用
- [ ] 分享预览与导出图片、主题及长文本对照通过
- [ ] 小组件“跟随首页”与置顶/排序一致，多实例配置互不污染
- [ ] 小组件 1-5 格“预览宽度 / 预览高度”与 Launcher 实际尺寸区别清楚
- [ ] 真实 Launcher 的背景、边框、圆角、密度、农历前缀和文字对比验证完成
- [ ] 冷启动、首页滚动、月历切换、详情与设置导航的帧和内存记录无未解释退化
- [ ] Android 8 / API 26、Android 12 及当前 target SDK 环境完成核心 smoke
- [ ] 物理手机安装/升级、通知、日历、Launcher 与性能验收：未执行（当前无手机），按现有发布授权记录剩余限制

## 五、本版证据记录

### 最终标签、正式签名与公开发布

应用源码固定为 `6c52a3a0e7f495e015f12c035ea94bd614fc2813`，annotated tag 对象为 `e25f58372246d55c2d37fdf4d168adcccdb5e15e`。后续 publisher / QA 工具和文档提交不改变 APK 生产源码，也不移动 `v4.1`。

| 证据 | 实际结果 | 原始来源与范围 |
|---|---|---|
| 最终标签完整 CI | [37990007930](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37990007930)：Direct/Play 各76份 suite /575 tests，failures/errors/skipped 均0；API36 native49/49；publisher10/10；三份完整lint各0 error/1 OldTargetApi | `android-verification-6c52a3a0e7f495e015f12c035ea94bd614fc2813`，artifact11645024228；原报告 `.tmp/final-tag-ci-37990007930/`；vital汇总跳过与Analyze实际执行分别保留 |
| 新鲜 R8 APK | 未签名26,443,962 bytes，SHA-256 `baf96d2d0aa4b7ac920e4ed131eafa8e467b664e88fff586f1e707496f968770`；R8/资源压缩/package实际成功，source-revision与原metadata均为tag6c /directRelease/4.1/24 | 同一最终CI的 `outputs/apk/direct/release/` 与 `reports/unsigned-direct-release/`；保留原文件后本机正式签名 |
| Debug 渠道身份 | Direct `com.example.timeapk` /4.1/24、有REQUEST_INSTALL_PACKAGES；Play `com.example.timeapk.play` /4.1/24、无该权限；均为debug | 对上述CI的两个实际APK运行aapt；Direct SHA `f5d273794ecab783882b9f4613dcd25380bae89e93057940b226659f07a376ea`，Play SHA `89410c14271258a4d24fb70775ef1df88a47c82e09bd2acbb087ce72130cfb6f`；Play仅开发回归 |
| 正式签名与身份 | `glimmer-countdown-4-1.apk` /26,463,543 bytes /SHA-256 `8fbeee7590f89104995ecce98769842dc2a75cf84d8641efd6a0c7b67619e5b4`；单签名者、v2/v3、证书与v4.0相同；16KiB zipalign通过 | `.tmp/candidates/final-tag-37990007930/` 的receipt、独立apksigner与aapt；com.example.timeapk/4.1/24/min26/target36/非debug/安装权限 |
| 最终正式包三平台 QA | [37992543286](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37992543286)：API26/31/36全成功，4.0/23覆盖升级到4.1/24；首次安装时间、UID、标题、备注和日期保留，upgrade/runtime passed=true | `.tmp/final-signed-qa-37992543286/`；26：21:19:35/UID10077/AMS6481→7199/task7；31：21:19:25/UID10145/8434→9150/task14；36：21:19:12/UID10216/6458→7171/task8，均2026-10-09、stopped=false。实际后台AMS终止，非自然LMK |
| 正式包五页与分享 | API36实际MainActivity五页、菜单触摸、相册MediaStore保存、系统chooser与原生Back返回同详情通过；原PNG1080×1350 /59,828 bytes /SHA `a1aef10d1e8f5188298aa2acd7441e8832cb96d9e1ace5f5ca250f1e4790f930` | 上述签名QA的原始JSON/XML/PNG/Activity与share返回核对；这是发布前正式包证据，公开包新截图另行归档。不证明接收方投递或Launcher绑定 |
| 发布工具修复 | commit `a608b70d843b766e5b352f680e42a2aa1382993c` 正确解析真实 `Signer #1 certificate SHA-256 digest:`；10/10隔离回归与实际最终证书输出匹配通过 | 原tag工具首次在网络请求前拒绝合法格式，未发布任何错误资产；使用已验证同内容的工具副本在tag HEAD发布，不修改tag或APK。`.tmp/publisher-utility-v41-receipt.json` |
| 正式公开与无认证下载 | [v4.1](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.1) Release408364983，公开且非prerelease，唯一asset626232462，MIME application/vnd.android.package-archive；下载大小/SHA/签名/版本与最终APK完全一致 | `.tmp/public-v41-verified/receipt.json` 与无认证下载APK/验签/包身份；发布锁及两个owned临时QA draft已按ownership清理 |
| 公开 APK 复验 | [37995185711](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37995185711) API26/31/36全成功；固定公开asset626232462/hash8fbe…、APK生产源码6c52a3a；upgrade/runtime均passed，API36 update_check=true/result=update_latest（Already up to date）、share_check=true、实际五页5/5 | 原始报告 `.tmp/public-qa-37995185711/`；QA工具revision `6a1bdb5d73a5e6228303d631e29007a5ad32ca91` /main，与APK源码分开。生产源码diff为0；本轮线上包原图与文件哈希见 [截图记录](screenshots/4.1/README.md) |

公开复验的三平台首次安装时间/UID与AMS记录（2026-10-09）：API26 `21:45:53 /10077 /6503→7171 /task7`；API31 `21:45:34 /10145 /8049→8780 /task14`；API36 `21:47:55 /10216 /6447→7187 /task8`。三者均保留标题/备注/日期与身份，`stopped=false`。API36原始 `73-update-03-already-current.xml` 实际观察“已是最新版本”；相册PNG为57,772 bytes /1080×1350，MediaStore row20已发布，系统chooser原生Back后回到精确标题/备注所在详情及MainActivity。原始PNG及其SHA-256在本轮[截图记录](screenshots/4.1/README.md)中保留。

首次公开QA [37994317407](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37994317407) 整体失败：API26/31完成升级与AMS恢复，API36在旧v4.0基线创建页等待期间失败，未执行4.1运行/更新/五页。原始记录证明单次点击已进入新建页，但runner在过渡期没有滚动容器时立即报错；`6a1bdb5` 将共享查找函数改为在原有有限重试预算内等待，不重点击、不放宽字段/签名/数据断言。同一个公开APK使用新工具复验；失败原报告保留在 `.tmp/public-qa-37994317407/`，不作最终通过证据。

物理设备、自然LMK、完整主题/字体/旋转/TalkBack、实际后台通知送达、用户日历云同步、Launcher绑定与接收方读取/发送仍未执行。meminfo/gfxinfo仅为诊断快照，不代表性能基准通过。发布后文档采用既有 ReleaseReadiness 六项文档方法的153条静态谓词及UTF-8/diff检查，不因文档和原图归档重复构建正式APK。

### 历史候选 CI3（非最终发布证据）

2026-10-09 [CI3 run 37899670865](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37899670865) 的分支 head 为 `544a6a7`，实际执行源码为 PR merge `1ffc458552067ac431e7095432f09e639f1ff4bd`。以下结果仅绑定该提交；后续修复待 CI4，不能迁移为最终 tag 已通过的结论。报告位于该 run 的 `android-verification-1ffc458552067ac431e7095432f09e639f1ff4bd` artifact。

| 证据 | revision / 工件 | 环境 / 命令 | 结果与原始报告 |
|---|---|---|---|
| JVM 测试 | CI3 PR merge（见上） | `testDirectDebugUnitTest testPlayDebugUnitTest` | 各 575 项，failures/errors/skipped 均为 0；`test-results/testDirectDebugUnitTest/` 与 `test-results/testPlayDebugUnitTest/` XML |
| AndroidTest 编译与 connected | CI3 Direct debug APK | API36 Google APIs x86_64 / KVM；`compileDirectDebugAndroidTestKotlin connectedDirectDebugAndroidTest` | 编译通过；46 tests / 4 failures / 0 errors / 0 skipped（42 通过）；`outputs/androidTest-results/connected/debug/flavors/direct/TEST-emulator-5554 - 16-_app-direct.xml`，修复后待 CI4 |
| lint / vital lint | CI3 PR merge（见上） | `lintDirectDebug lintDirectRelease lintPlayRelease lintVitalDirectRelease lintVitalPlayRelease` | 三份完整报告各 0 error、2 warning：`OldTargetApi` / `UnusedQuantity`；两个 vital 汇总任务 Skipped，对应 `lintVitalAnalyzeDirectRelease` / `lintVitalAnalyzePlayRelease` 已执行；`reports/lint-results-{directDebug,directRelease,playRelease}.xml` 与 run 任务日志 |
| publisher 隔离回归 | CI3 PR merge（见上） | `pwsh -NoProfile -File scripts/tests/publish-release-mock-harness.ps1 -Scenario all` | 10/10；见 run 的 publisher 步骤日志，属于受控状态机证据 |
| 模拟器 UI、恢复、日历与更新 | CI3 Direct debug APK | API36 connected 与 `reports/native-emulator/` | SavedState 六项通过；整个 connected 套件未通过，UI 截图审阅、实际后台进程恢复和修复后结果待 CI4 |
| 物理手机与 Launcher | 本版无实体手机 | 未执行 | 按现有发布授权记录剩余限制，不写成通过，不继承 v4.0 豁免 |
| 候选期正式签名构建 | CI3时尚无最终tag | 正式密钥与配置已核验 | 本行仅保留CI3阶段状态；最终构建与验签见上节 |
| 候选期GitHub发布与公开下载 | CI3时未执行 | CI3阶段 | 最终公开结果见上节；不将候选结果迁移为最终tag证明 |

本机中间候选（不可发布）：`assembleDirectRelease` 用时 30m13s，R8、资源压缩、vital、package、rename 实际执行；v2 验签通过，证书与线上 v4.0 一致。真实包为 `com.example.timeapk` / `4.1` / `24`、非 debug，含 `REQUEST_INSTALL_PACKAGES`；26,449,922 bytes，SHA-256 `c9aa55acd32d84777c4b67ae24683f6540a182c1fd481c7bd36f9071b1157bbd`。主要生产源码为 `544a6a7` 时的状态，未包含后续 Theme/Home 更改，也未绑定最终 tag；仅证明本机签名打包路径可用，不替代最终新鲜构建。本地私有日志为 `.tmp/v41-signed-candidate-build.log`，不提交日志内容。

不得将临时 QA 签名产物、旧 dist、旧截图或旧测试写入本版正式产物栏。最终 tag、APK 文件和设备安装包须互相对应；发生源码变化后重新执行受影响的验证。

### CI6 实际候选记录

2026-10-09 [CI6 run 37908996800](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37908996800) 的分支 head 为 `d46169761a09213fc28b3518264abe8b55673caa`，实际执行源码及 `reports/unsigned-direct-release/source-revision.txt` 为 PR merge `2481068d533cb2ad5307969246d25e59914cb3d8`。run 整体为 failure，以下局部通过不能写成整个套件或最终 tag 已通过。

artifact 为 `11607262690` / `android-verification-2481068d533cb2ad5307969246d25e59914cb3d8`，GitHub 记录大小 106,569,511 bytes、archive digest `7739ee6801cef350e04dd16693c7653c784a642ec88c75f1c34de43b20cf82c6`。原始报告已下载到被忽略的 `.tmp/ci6-37908996800/`；下表路径均相对此目录。archive digest 来自 GitHub 上传记录，APK 原始哈希已在本机重新计算对照。

| 证据 | CI6 实际结果 | 原始报告 / 范围 |
|---|---|---|
| JVM、编译与 debug 构建 | Direct/Play 各 575/575；各 76 个 XML suite，failures/errors/skipped 均为 0；主程序、两渠道 AndroidTest 编译与 debug APK 构建通过 | `test-results/test{Direct,Play}DebugUnitTest/` 与 run 综合步骤日志；仍为 PR merge 候选 |
| publisher 隔离状态机 | 10/10 | run publisher 步骤日志；未创建或发布正式 Release |
| lint / vital lint | 三份完整报告各 0 error、1 个 `OldTargetApi` warning（`app/build.gradle.kts:114`）；R8 阶段 Direct vital 实际执行，Play vital 汇总任务跳过 | `reports/lint-results-{directDebug,directRelease,playRelease}.xml` 与 run 任务日志；目标 SDK 保持 36 |
| API36 connected | 48 tests / 1 failure / 0 errors / 0 skipped（47 通过）；唯一失败为 `holdingADragNearTheListEdgeScrollsBeyondTheInitialViewport` 的 5 秒条件超时，`HomeFilteredReorderGestureTest.kt:166` | `outputs/androidTest-results/connected/debug/flavors/direct/TEST-emulator-5554 - 16-_app-direct.xml`；EventEdit IME/重建、筛选子集拖动、Popup 返回和系统栏两项回归通过，套件整体未通过 |
| 未签名 Direct R8 APK | 26,442,138 bytes；SHA-256 `7b452efdeb9c3c7fdff3ac28898cea58d34b6852547db077848bc817e3eecdb1`，本机计算值与 CI 记录一致；`minifyDirectReleaseWithR8` / `packageDirectRelease` 实际执行并成功 | `outputs/apk/direct/release/app-direct-release-unsigned.apk`；`reports/unsigned-direct-release/` 的 revision、原始 metadata、哈希与 Gradle 日志；metadata 为 `directRelease` / `com.example.timeapk` / `4.1` / `24` / `SINGLE` |
| 组件原始证据 | 16 项文件：5 张 device PNG、5 份 Compose tree、5 份 context、1 张分享导出 PNG；tar 内文件与解出文件逐字节一致，device PNG 为 1080×1920，export 为 1080×1350 | `reports/native-emulator/qa-evidence/` 与 `component-evidence.tar`；仅为 Direct debug 的 synthetic 组件场景，截图人工审阅另行记录，不能替代实际 MainActivity 五页或最终正式签名 APK QA |
| 实际 AMS 恢复与 MainActivity 五页 | 未执行；connected 失败中断后续 native script，没有 `runtime-smoke/result.json` | 仅 `cold-start.png` / `final-state.png` 不能证明 OS 恢复或完整五页；不能将已收集的组件截图计入实际页面证据 |

CI6 的目标 APK 保留及组件采集已实际成功；CI5 的 `run-as: unknown package` / 无效 tar 问题不再出现。后续未提交的综合步骤与 R8 `--no-build-cache --rerun-tasks` 调整不属于 CI6，最终 tag 必须按届时提交的 workflow 新鲜执行并记录原始报告。此处未签名 APK 不可直接发布，也不能替代最终 tag 工件。

### 后续候选复验

- [签名预验收 run 37988739107](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37988739107) source 为 `c8223320e22044e9fd044fbb92d8e6bb5bff41c4`，API26/31/36 全部成功。沿用下述 CI13 正式候选（26,463,543 bytes / `b05c6329218a93f693e3367067f6f8658ded5d82d340be2775dbdb7cfda13c00`），不能代替最终标签工件。API36 覆盖升级保留 firstInstallTime `2026-10-09 20:43:54`、userId `10216`、精确标题/备注/日期；AMS PID6513→7233/task8/stopped=false，runtime passed=true。真实相册PNG为1080×1350 /62,686 bytes /SHA-256 `3f7f6c1a6c31d03b7579a7eefc34f649f5c36144fd72d24b39b223848ec0e9a8`，share_check passed=true，系统chooser截图/XML、原生Back返回同详情Activity记录完整，实际五页5/5、菜单touch通过。基线IME就绪与ChooserActivityLauncher识别修复已实际复验；接收方读取/发送、实体手机、自然LMK与Launcher绑定未执行。原始报告 `.tmp/pretag-qa-37988739107/`，最终不可变标签仍须新鲜完整CI、正式验签及三平台/公开下载QA。
- [签名预验收 run 37988494418](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37988494418) source 为 `f6b3c6f2edcfdfae583743cbe635d53a4f99f6a8`，原生验证进行中主动取消，不记为通过。检查实际 API26/31 的 BACK→IME隐藏→字段仍focused日志后发现新增就绪等待与入口 hide_keyboard 冲突；基线入口改为保留键盘状态，仍在每个字段录入结束隐藏键盘，未聚焦字段依旧实际点击。4.1快速录入及严格字段断言未改变，完整修正后新鲜复验。
- [签名预验收 run 37987793650](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37987793650) source 为 `ab6e87750b8276721a3d8aa5f40c1cc1b2000172`。API36 在 v4.0 基线第一字符录入失败（预期 `U`、实际 `UU`），尚未执行候选安装、恢复或分享。原 commands 只有一次 `input text U`、没有重复点击；logcat 记录首次输入法尚未 onShown 时 U DOWN 处理超时（2500ms）。脚本将仅基线录入增加实际 IME 就绪等待，仍严格验证每个前缀、完整字段、日期与升级身份，4.1快速输入路径不变。不能以此前升级通过替代本轮失败；原始报告 `.tmp/pretag-qa-37987793650/api36/`。
- [签名预验收 run 37986450767](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37986450767) source 为 `1a4845a271d6fc25839aa8a9b54e975b89ce68b0`，仍使用同一 CI13 正式候选。API26/31 整体通过；API36 的 v4.0→v4.1 覆盖升级、身份/标题/备注/日期保留和 AMS 恢复通过（PID6399→7100/task8/stopped=false），已解决旧版输入法重复点击误触。API36 实际相册保存成功，反馈被捕捉；新 MediaStore row 为 image/png、Pictures/TimeAPK/、is_pending=0，PNG 1080×1350 / 59,328 bytes / SHA-256 `c3a4b4f7df7835e3532cdd17148b266fb75f8891f53d2f7a494e2e7b3c49924b`。选择器检查误拒绝系统 `com.android.intentresolver/.ChooserActivityLauncher` 别名：实际 ActivityRecord 已有 CHOOSER 动作与 ChooserActivity component，但旧正则要求名称立即结束。脚本只补充 Launcher 别名并保持原断言与20秒预算；本轮未执行返回详情，五页仅3/5，整体仍失败，须新run证明完整分享返回路径。原始报告 `.tmp/pretag-qa-37986450767/signed-apk-qa-api{26,31,36}-1a4845a271d6fc25839aa8a9b54e975b89ce68b0/`。
- [签名预验收 run 37984408468](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37984408468) 实际 source 为 `63ebf13b5e592943c8d8870aa07f942dad4ba188`，候选正式 APK 仍为下述 CI13 签名工件。三平台下载、大小、哈希及证书均通过，已解决私有草稿下载 403。API26/31 完成 v4.0/23→v4.1/24 覆盖升级，保留 firstInstallTime、应用 UID、精确标题/备注/日期；AMS 小草稿恢复分别 PID6557→7269/task7、7922→8680/task14，stopped=false。API36 在 v4.0 fixture 第一字符输入期失败（预期 `U`、实际 `yU`），未执行升级/恢复/五页；整体矩阵失败。原 XML 显示字段已聚焦，输入法随后弹出使额外点击旧坐标命中键盘；验证脚本仅跳过基线已聚焦字段的重复点击，不放宽逐字/完整字段与身份断言，4.1 快速输入路径不变。原始报告 `.tmp/pretag-qa-37984408468/api{26,31,36}/`；修复后须新 run 复验。
- [main CI run 37937662644](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37937662644) source 为 `bbaee240e7e0097f1290b376fc710f02e31ea218`：Direct/Play JVM 各575/575、publisher10/10、native49/49；三份 lint 各0 error/1个已说明 warning。实际 AMS 恢复 PID5645→5980/task78/stopped=false、两字段保存重开、排序菜单触摸和五页5/5通过。未签名 APK 为26,443,962 bytes，SHA-256 `94ffc349a118fafffcbd6752a79280f69aae0974f000f7d5890916bb3b0fd58a`；原始报告 `.tmp/main-ci-37937662644/`。上述对照表使用该主分支候选与CI13明确范围，均不替代最终tag。
- [CI13 run 37932788084](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37932788084) 实际 PR merge 为 `7113b1a27c314671f66b9470c1a7de503e83dbe4`，与已合并 main `a1b207d6587b8fe1a666e72129c68a79d23a2357` 的 tree 完全相同。JVM 各 575/575、publisher 10/10，三份 lint 各 0 error / 1 个 `OldTargetApi` warning；综合 174/174、R8 46/46 tasks 均新鲜执行。未签名 R8 APK 为 26,443,962 bytes，SHA-256 `72f10f89a0a5439023a75873740dc0cc1e466974a13b206df806a49ec9a99882`。connected 49/49、零失败/错误/跳过；真实边缘拖动到达 M6，`composeElapsedMs=1996`、`scrollRange=0.0→1819.0`，原有五秒预算、隐藏位置和落盘断言均通过。runtime `passed=true`，同 task `78` 的 AMS 后台进程终止后 PID `5325→5785`、`package_stopped_after_kill=false`，草稿、保存及重开两字段精确一致。排序菜单真实触摸、原生返回、五页截图 5/5 均完成；首页、月历、详情、设置、应用内小组件默认预览已人工审阅，未见明显溢出/遮挡/系统栏对比问题。artifact `11618491923`，109,126,504 bytes，archive digest `af0c1f3bd4344c872e9ef18f07d9ad70b5bcdb2f83598bb67e97778ac825d81d`；原始报告在忽略目录 `.tmp/ci13-37932788084/`，组件 16 项文件完整。范围是 API36 模拟器 Direct debug、字体 1.0；不能扩大为最终正式签名包、自然 LMK、实体手机或 Launcher 绑定验收。
- 标签冻结前已正式签名 CI13 候选：26,463,543 bytes，SHA-256 `b05c6329218a93f693e3367067f6f8658ded5d82d340be2775dbdb7cfda13c00`；独立验签、16KiB zipalign、包名/版本/SDK 核对通过，正式证书与 v4.0 相同。仅用于预验收，不能发布此候选代替最终 tag 工件。首次 [签名 QA run 37936902473](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37936902473) 的 API26/31/36 均在下载临时私有草稿资产时返回 `Resource not accessible by integration (HTTP 403)`；安装、升级、恢复和截图均未执行。原始报告在 `.tmp/pretag-qa-37936902473/`；用户于 2026-10-10 明确批准仅签名 QA 作业取得 `contents: write`，用于草稿下载，其余 CI 保持只读。权限调整后的升级验证仍须重新执行，不将本轮失败标为通过。
- [CI12 run 37928780065](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37928780065) 实际 PR merge 为 `b2e7b921c4c5032fdfc6a74d340089d10a3d220a`。JVM 各 575/575、publisher 10/10，三份 lint 各 0 error / 1 个 `OldTargetApi` warning；综合 174/174、R8 46/46 tasks 均新鲜执行。未签名 R8 APK 为 26,443,962 bytes，SHA-256 `fa43e1347d921d8f6b50e757be5a1a19e9ea2b52162a3134ef8031efd1e82ac5`。connected 46/49，三项 Home 均在排序 fixture 滚动后的 `assertIsDisplayed`（第 237 行）失败，尚未进入真实拖动；固定版本 Compose 的滚动 action 接受后异步执行，后续需等待实际可见。runtime 已完成 PID `5709→6077`、同一 task `78`、`package_stopped_after_kill=false` 的草稿恢复、保存与备注精确重开；实际菜单触摸两种排序、重新打开选中状态及原生返回均通过。五页截图仅 2/5，日历事件卡片父中心坐标位于裁剪 viewport 下方，点击后仍在月历，`pages-03-event-detail` 失败；不放宽详情页标记。artifact `11616270934`，108,882,377 bytes，archive digest `6ce1cc34644371cf1110da9e0b3b09e2453eadf972e0e0dd55755a308902e27d`；原始报告在忽略目录 `.tmp/ci12-37928780065/`，组件 16 项文件完整。本轮仍是候选 PR 证据，不是最终 tag 或正式签名包验收。
- [CI11 run 37924741446](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37924741446) 实际 PR merge 为 `7ef83906e5cda51a00a197b22587d6adf7e1ea87`。JVM 各 575/575、publisher 10/10，三份 lint 各 0 error / 1 个 `OldTargetApi` warning；未签名 R8 APK 为 26,443,962 bytes，SHA-256 `fed94c615610e6c4d32ceb95c74fc3a39e1af29b4c87e5959921632064f76a11`。connected 46/49，新整串快速输入及页面重建用例实际通过（7.466s）；三项 Home 用例仍在排序准备等待失败，尚未进入拖动。runtime 记录 PID `5626→5967`、同一 task `78`、`package_stopped_after_kill=false`，并通过保存前的草稿与返回确认；最后 `11-reopen-event` 将视口内唯一标题框误匹配为备注框，整体 `passed=false`。原始详情 XML 中保存后的标题和备注均完整；字段查找须在局部容器关联标签，并滚动找到实际备注输入框后重新精确核验。artifact `11613754347`，108,346,395 bytes，archive digest `7b30328c9ca6acd060ec3e834addb7c61e5d1fbe2bdd6230ba5f3a3242ddbd75`；原始报告在忽略目录 `.tmp/ci11-37924741446/`，组件 16 项文件完整。后续排序 fixture 使用实际 UI 的语义点击回调，核心拖动保持真实触摸；物理菜单点击另由实际 MainActivity 脚本核对，不将 fixture 语义动作标为菜单触摸证据。
- [CI10 run 37923334942](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37923334942) 实际 PR merge 为 `7b55529ef9edc98153784ac11a9deca47a475340`。publisher 10/10、主程序与两渠道 AndroidTest 编译通过，Direct JVM 574/575；`WidgetContentResolverTest.filterAndSortStates_appliesContentScopes` 期望 `[1,2]`，实际 `[2,1]`。Play 测试、完整 lint、R8、connected 与 runtime 本轮未执行。artifact `11613327172`，205,786 bytes，archive digest `aaa7755f6ea70d6f928aa05fcde44b48a78fe40683daabf833a7cb8de5b79bd8`；原始失败 XML 在忽略目录 `.tmp/ci10-37923334942/`。后续 CI 独立尝试 Release 构建及原生验证，任何前置质量门失败仍使整个 job 失败，不放宽发布门。
- [CI9 run 37920691029](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37920691029) 实际 PR merge 为 `e94281a160d4e4bbde907dcbeb15ffa507a6fa24`。JVM 各 575/575、publisher 10/10，三份 lint 各 0 error / 1 个 `OldTargetApi` warning；未签名 R8 APK 为 26,444,546 bytes，SHA-256 `e6067974b5d472c6a75675dfab45a5e61dd5ca1ec97b793edd5b23c85a53bff5`。connected 45/48，三项均在排序偏好等待超时，未进入拖动；实际 runtime smoke 的 `02-enter-draft` 标题精确核验发现丢字符，未进入 AMS 回收、恢复或五页截图。artifact `11612487455`，105,276,471 bytes，archive digest `29f30f2799206f3f03f84ff7e51a5f26075d283f8e75d85cf0c593a8cf10131f`；原始 XML、输入命令、result.json 与截图在忽略目录 `.tmp/ci9-37920691029/`。
- [CI8 run 37917421040](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37917421040) 实际 PR merge 为 `e6006d487330d5dcdd0c35c42a31a39f86a19603`。JVM 各 575/575、publisher 10/10，三份 lint 各 0 error / 1 个 `OldTargetApi` warning；未签名 R8 APK 为 26,444,546 bytes，SHA-256 `1d5232bba59eebe4f950370712dc86c6398d875f13f00422c77abafcbb118636`。connected 45/48，三项排序测试均在准备函数找不到 `E2EDrag-A`，未进入拖动断言；runtime smoke 未执行。artifact `11611366183`，106,618,633 bytes，archive digest `94d13556871ab6345c6e4ce7f6f729fbcbd61617e3fa59ab895e5eb01978197f`，原始报告位于忽略目录 `.tmp/ci8-37917421040/`；组件 16 项文件完整。后续 CI 保留 connected 的失败退出码并独立执行 runtime smoke，分别判断两个结果，不将失败改为通过。
- [CI7 run 37914565927](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37914565927) 实际 PR merge 为 `4a620779f7d0c4805b4ee9af5382e261e021be87`，Direct/Play JVM 各 575/575，publisher 10/10，三份完整 lint 各 0 error / 1 个 `OldTargetApi` warning；综合与 R8 均实际使用 `--no-build-cache --rerun-tasks`。未签名 R8 APK 为 26,444,546 bytes，SHA-256 `69ba67326130b3d881023ae7b2a4787d63de9fa321be41ee42a110a9ba817f5d`，本机重算匹配，仍不是最终 tag 的正式 APK。
- CI7 connected 为 47/48，唯一 edge case 在五秒 Compose 帧预算结束时失败：`composeElapsedMs=5004`、`scrollRange=0.0 -> 0.0`，拖动卡片越过 viewport 底部。实际字节码核对定位到排序库启动协程后才赋值任务字段，与测试框架默认 `UnconfinedTestDispatcher` 提前执行的初始化竞态；改用既有 `StandardTestDispatcher` 的排队调度后仍须重新执行，不能以诊断代替通过。
- CI7 artifact `11609189493`，106,658,769 bytes，archive digest `ee4a11458f0801e9dbdd30273e15c667102b8bca38687aaf00557d40857a7efe`；原始报告保存在忽略目录 `.tmp/ci7-37914565927/`。组件 16 项文件完整且与原始 tar 逐字节相同；取图同步后初始月历已有内容，英文 `13 hours` / `1 day`、窄卡片和长备注底部四项操作已实看。这些均为 synthetic ComponentActivity / Direct debug API36 证据。connected 失败使实际 AMS 恢复脚本及 MainActivity 五页未执行。

- [CI4 run 37903771924](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37903771924)，实际 PR merge `76ed7c368fd21e706e2e82689d743040924fc246`：Direct JVM 574/575，唯一失败为文档旧阶段措辞断言；后续 Play、lint、R8 和 native 运行未执行。此断言已改为检查阶段一致性与新鲜证据规则，未放宽正式签名或标签约束。
- [CI5 run 37906555695](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37906555695)，实际 PR merge `8ad78c206f02369e904a9fad3e20a7b059b90f51`：Direct/Play JVM 各 575/575，0 failures/errors/skipped；publisher 10/10；三份完整 lint 各 0 error、1 个 `OldTargetApi` warning（目标 SDK 保持 36）。R8 阶段 Direct vital 实际执行，Play vital 汇总任务跳过。
- CI5 API36 connected 为 48 tests / 3 failures / 0 errors / 0 skipped。Popup 系统返回与两项系统栏对比度回归通过；失败为编辑页关闭 IME 回调超时，以及两项真实拖动排序未完成。修复后须 CI6 复验，不把 45 项通过写为整个套件通过。
- CI5 未签名 Direct R8 APK 为 26,442,154 bytes，SHA-256 `f26251ff3570ba498aaee2685a19b17c7c4744c5ffe3c905d5bebeb30b1c380c`；原始 metadata 为 `directRelease` / `com.example.timeapk` / `4.1` / `24` / `SINGLE`。该工件绑定候选 PR merge，不能替代最终 tag 构建，也不能直接发布。
- CI5 artifact `11604884376`（`android-verification-8ad78c206f02369e904a9fad3e20a7b059b90f51`）保留 XML、R8、lint 与系统日志。connected 失败后 OS smoke 未执行；Gradle 在测试结束时卸载目标包，组件私有 cache 随包删除，故没有组件 PNG 或五页截图证据。下一轮使用当前 AGP 9.1 的原生 `android.injected.androidTest.leaveApksInstalledAfterRun=true`，显式检查组件文件后再运行真实 AMS 恢复与五页截图。


- [CI6 run 37908996800](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37908996800) 的完整结果见上节：JVM、lint、R8 与组件采集完成，connected 仍为 47/48；修复唯一边缘拖动失败后重新取得受影响的 connected 结果，不沿用旧失败或局部通过作为整套通过。
- CI13 已实际执行 `scripts/android-runtime-smoke.py`，取得草稿恢复、任务保留、保存重开和实际 MainActivity 五页证据；这些是候选 debug 结果，最终不可变 tag 的正式签名 APK 仍须新鲜执行并记录，不能提前勾选最终产物验收门。
- 组件截图人工审阅、后续候选的实际页面截图，以及最终不可变 tag 正式签名 APK 的五页 QA 分别绑定各自源码与 APK；源码变化后的旧 debug 结果不能移作最终产物证据。

## 六、发布动作

以下项目只有各自事实成立后才能勾选。用户已允许修复、验证、提交、推送和发布 4.1，并说明当前无手机；按现有授权继续发布工作，如实保留物理验收未执行的限制，不另设二次审批门。授权不替代最终 APK、签名及公开下载的实际证据。

- [x] 本版发布门覆盖的修复回归、正式包升级/恢复与API36实际页面/分享验证已完成；失败及修复复验分别记录，未执行的广泛矩阵与物理项保留在本清单
- [x] 最终代码与发布文档已提交，且发布前工作区干净；公开复验结果与原图在发布后单独文档提交，不移动tag
- [x] 创建并推送不可变的 exact `v4.1` tag；本地与远端解引用后 commit 一致
- [x] 从该 tag commit 新鲜构建并正式签名，未复用旧产物；CI 未签名工件的 revision、原始哈希与 metadata 在本机签名前保留
- [x] 验证签名、精确证书指纹与 SHA-256，并记录 exact APK 大小、包名 `com.example.timeapk`、`4.1` / `24`、非调试状态和安装权限
- [x] 准备安全凭据环境：本地用 `gh auth login` / `gh auth token`；CI 才注入 secret，不打印凭据
- [x] 从 CHANGELOG 的 4.1 小节准备可公开 Release Notes，候选状态与剩余限制按实际结果修订
- [x] 运行发布脚本；只创建/恢复带 ownership marker 的 owned draft，不覆盖 published Release 或接管人工 draft
- [x] 删除 owned draft 中的所有旧资产，并验证整个 Release 只保留唯一的 exact Direct APK
- [x] 最终 GET 核对公开、非 prerelease 的 Release 身份与 asset id、size、digest、MIME、下载 URL；Release 仅含 `glimmer-countdown-4-1.apk`
- [x] 发布后重新下载并安装线上 APK，核对大小/SHA-256/签名/版本，复测冷启动、更新检查和关键链路（上述API26/31/36范围；不是实体手机或应用内安装器UI验收）
- [x] 发布锁已按 ownership 验证清理，最后根据实际公开结果更新 README 与本版记录

固定顺序：最终代码与发布文档已提交，且工作区干净 → 创建并推送不可变的 exact tag → 从该 tag 对应 commit 的工作树重新正式签名构建 → 验证签名、精确证书指纹与 SHA-256 → 准备安全凭据环境 → 运行发布脚本。禁止移动已推送 tag、覆盖已发布 Release 或在缺失验证门时直接公开。

## 历史记录（非发布门）

[v4.0 原始清单](releases/v4.0-checklist.md) 保留其发布日期、源码/产物身份、验证结果和负责人当时的豁免。该归档未经改写，不证明 4.1 在同样环境下通过；README 中 v4.0 截图也仅是历史展示。
