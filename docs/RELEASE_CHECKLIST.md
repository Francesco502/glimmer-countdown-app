# 发布检查清单（v4.1）

**版本**：`4.1`（`versionCode=24`）  **文档日期**：2026-10-09  **发布状态：候选 / 未发布**

本清单只记录 4.1 当前候选。所有通过结果必须来自本版新鲜执行，并绑定源码 revision、构建、设备和原始报告；未知、未执行和失败不能勾选。v4.0 的原始记录已完整归档到 [历史清单](releases/v4.0-checklist.md)，旧测试、旧截图和旧真机豁免均不延续到本版。

**唯一正式发布渠道：GitHub Release。** 唯一官方资产为 Direct APK `glimmer-countdown-4-1.apk`。Play flavor 仅保留用于兼容性与开发回归，不是正式发布工件或阻断项。最新公开版本仍为 [v4.0](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.0)。

## 当前阶段与证据身份

| 项目 | 当前状态 | 完成所需证据 |
|---|---|---|
| 源码 revision | 开发中，最终 commit 未冻结 | 最终 commit、干净工作区和本地/远端 exact tag 解引用结果 |
| 自动测试与 lint | CI5 JVM 与 publisher 通过；完整 lint 仅剩 `OldTargetApi` warning；未签名 R8 构建通过 | 本节后续记录实际命令、测试数量、报告路径与对应 revision |
| 模拟器运行与截图 | CI5 API36 48 项中 3 项失败；修复待 CI6，OS smoke 尚未运行 | APK 哈希、系统镜像、操作路径、新鲜截图、UI 树和崩溃日志 |
| 物理真机 | 当前无实体手机；未执行，按现有授权记录剩余限制 | 用户已明确授权发布并说明无手机；安装/升级、提醒、日历、Launcher 与性能缺少物理设备证据，不能写成通过，也不继承 v4.0 豁免 |
| 正式签名 | 既有配置与 keystore 已取得；配置路径一致、私钥可用，证书与线上 v4.0 APK 一致 | 已完成 Java Properties / JCA 加载及证书 SHA-256 核验；最终 4.1 APK 仍须独立验签，不记录密码、密钥内容或 token |
| 正式 APK | 本机中间候选已验签，非最终 tag 工件不得发布；最终 APK 尚待完成 | 从最终不可变 tag 新鲜构建的 exact Direct APK、大小、SHA-256、签名与真实包身份 |
| GitHub 上传与公开复验 | 尚未执行 | publisher 日志、Release/asset 身份、唯一资产、公开下载哈希与安装复验 |

已取得密钥的证书 SHA-256 为 `3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`，与下载核验的线上 v4.0 APK 一致。密钥与配置核验完成不代表 4.1 正式 APK 已构建或验签通过；不能用新建 QA 证书代替正式升级证书。

## 一、自动质量门

- [ ] `gradle.properties` 的 `VERSION_NAME=4.1` / `VERSION_CODE=24`、Gradle 默认 fallback 和 APK 命名一致
- [ ] README、CHANGELOG 和发布文档使用本版状态与 exact APK，历史证据可区分
- [ ] `git diff --check` 无尾随空格或冲突标记
- [ ] `./gradlew --no-daemon --no-parallel testDirectDebugUnitTest` 新鲜通过，记录完整测试结果
- [ ] `./gradlew compileDirectDebugAndroidTestKotlin` 通过
- [ ] `./gradlew lintDirectDebug lintDirectRelease lintVitalDirectRelease` 通过，无未解释 warning
- [ ] publisher 隔离 PowerShell 状态机 10/10 通过，包括本地预检零 GitHub 请求、锁竞争、owned draft 恢复与全资产清理、失败清理、证书输出和临时资产 URL 场景
- [ ] Debug APK 渠道身份与权限正确：Direct 包含 `REQUEST_INSTALL_PACKAGES`，Play 不包含；Play 开发回归单独记录
- [ ] Direct Release 默认签名门保持；从最终 tag 完成 R8、资源压缩和正式签名，或按发布指引在 CI 生成未签名工件后在本机正式签名，未签名工件不得发布

## 二、4.1 修复要求对照表

| 要求 | 当前实现方向 | 必须验证的场景 | 当前结果 |
|---|---|---|---|
| 普通事件日历清理 | 共享事件入口判断所有权 | 无日历权限的普通事件：导入、撤销删除、提醒送达后无虚假同步错误且可删除 | 待验证 |
| 真实日历记录保护 | 精确/写入中所有权、历史错误保留；删除统一检查 | 仅注册表有所有权也不能跳过；撤权、部分失败、恢复权限、重试及无关日历不误删 | 待验证 |
| 草稿与保存可靠性 | SavedStateHandle 与 ViewModel 保存任务 | 系统回收进程后的新建/编辑草稿恢复；保存中旋转/返回不重复落库；强制停止不冒充系统回收验证 | CI5 六项 SavedState 测试通过；编辑标题重建后已恢复，但关闭 IME 操作超时，后续路径待 CI6；实际进程恢复未执行 |
| 导入可靠性 | 进行中保护、输入边界、后台解析和重复识别 | 连续点击、页面重建、超限/畸形文件、重复数据、取消及失败后的再次导入 | 待验证 |
| 小时显示 | 24 小时内显示已有小时数据 | 开关生效、当天/过去/超过 24 小时、卡片/列表及日期模式一致 | 待验证 |
| 纪念节点含义 | 固定天数使用天数文案 | 跨闰年 365 天明确显示天数，不误称日历一周年；节点显示和提醒一致 | 待验证 |
| 详情状态 | 复用首页时间与偏好 | 跨午夜、回到前台、智能节点关闭、自定义节点与详情更新 | 待验证 |
| 日历状态与通知频道 | 本地化资源与普通提醒频道检查 | 中文/英文权限、无可写系统日历、provider 失败；相关频道关闭不能显示普通提醒 ready | 待验证 |
| 分享一致性 | 预览与导出采用一致样式 | 浅色/深色、自定义字体、长标题、预览与 PNG 对照、保存和系统分享 | 待验证 |
| 首页交互与适配 | 筛选摘要、面板关闭、排序操作、短高度月历 | 系统返回/点外部关闭、清除筛选、长列表排序与无障碍移动、横屏六行月份可访问列表 | 待验证 |
| 小组件配置 | 恢复草稿、保存中保护和错误反馈 | 旋转、保存失败/重试、多实例、实际 Launcher 背景/圆角/密度/文字和独立配置 | 待验证 |
| 更新安装 | 对象级互斥、临时文件、长度校验和原子替换 | 重复下载、空/截断/超长响应、失败/取消保留完整 APK、安装权限与系统签名拒绝路径 | 待验证 |

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

2026-10-09 [CI3 run 37899670865](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37899670865) 的分支 head 为 `544a6a7`，实际执行源码为 PR merge `1ffc458552067ac431e7095432f09e639f1ff4bd`。以下结果仅绑定该提交；后续修复待 CI4，不能迁移为最终 tag 已通过的结论。报告位于该 run 的 `android-verification-1ffc458552067ac431e7095432f09e639f1ff4bd` artifact。

| 证据 | revision / 工件 | 环境 / 命令 | 结果与原始报告 |
|---|---|---|---|
| JVM 测试 | CI3 PR merge（见上） | `testDirectDebugUnitTest testPlayDebugUnitTest` | 各 575 项，failures/errors/skipped 均为 0；`test-results/testDirectDebugUnitTest/` 与 `test-results/testPlayDebugUnitTest/` XML |
| AndroidTest 编译与 connected | CI3 Direct debug APK | API36 Google APIs x86_64 / KVM；`compileDirectDebugAndroidTestKotlin connectedDirectDebugAndroidTest` | 编译通过；46 tests / 4 failures / 0 errors / 0 skipped（42 通过）；`outputs/androidTest-results/connected/debug/flavors/direct/TEST-emulator-5554 - 16-_app-direct.xml`，修复后待 CI4 |
| lint / vital lint | CI3 PR merge（见上） | `lintDirectDebug lintDirectRelease lintPlayRelease lintVitalDirectRelease lintVitalPlayRelease` | 三份完整报告各 0 error、2 warning：`OldTargetApi` / `UnusedQuantity`；两个 vital 汇总任务 Skipped，对应 `lintVitalAnalyzeDirectRelease` / `lintVitalAnalyzePlayRelease` 已执行；`reports/lint-results-{directDebug,directRelease,playRelease}.xml` 与 run 任务日志 |
| publisher 隔离回归 | CI3 PR merge（见上） | `pwsh -NoProfile -File scripts/tests/publish-release-mock-harness.ps1 -Scenario all` | 10/10；见 run 的 publisher 步骤日志，属于受控状态机证据 |
| 模拟器 UI、恢复、日历与更新 | CI3 Direct debug APK | API36 connected 与 `reports/native-emulator/` | SavedState 六项通过；整个 connected 套件未通过，UI 截图审阅、实际后台进程恢复和修复后结果待 CI4 |
| 物理手机与 Launcher | 本版无实体手机 | 未执行 | 按现有发布授权记录剩余限制，不写成通过，不继承 v4.0 豁免 |
| 最终正式签名构建 | 最终 tag / APK 待记录 | 正式密钥与配置已核验 | 与线上 v4.0 同证；最终 4.1 正式签名 APK 构建与独立验签尚未完成 |
| GitHub 发布与公开下载 | 待填写 | 待填写 | 未执行 |

本机中间候选（不可发布）：`assembleDirectRelease` 用时 30m13s，R8、资源压缩、vital、package、rename 实际执行；v2 验签通过，证书与线上 v4.0 一致。真实包为 `com.example.timeapk` / `4.1` / `24`、非 debug，含 `REQUEST_INSTALL_PACKAGES`；26,449,922 bytes，SHA-256 `c9aa55acd32d84777c4b67ae24683f6540a182c1fd481c7bd36f9071b1157bbd`。主要生产源码为 `544a6a7` 时的状态，未包含后续 Theme/Home 更改，也未绑定最终 tag；仅证明本机签名打包路径可用，不替代最终新鲜构建。本地私有日志为 `.tmp/v41-signed-candidate-build.log`，不提交日志内容。

不得将临时 QA 签名产物、旧 dist、旧截图或旧测试写入本版正式产物栏。最终 tag、APK 文件和设备安装包须互相对应；发生源码变化后重新执行受影响的验证。

### 后续候选复验

- [CI4 run 37903771924](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37903771924)，实际 PR merge `76ed7c368fd21e706e2e82689d743040924fc246`：Direct JVM 574/575，唯一失败为文档旧阶段措辞断言；后续 Play、lint、R8 和 native 运行未执行。此断言已改为检查阶段一致性与新鲜证据规则，未放宽正式签名或标签约束。
- [CI5 run 37906555695](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37906555695)，实际 PR merge `8ad78c206f02369e904a9fad3e20a7b059b90f51`：Direct/Play JVM 各 575/575，0 failures/errors/skipped；publisher 10/10；三份完整 lint 各 0 error、1 个 `OldTargetApi` warning（目标 SDK 保持 36）。R8 阶段 Direct vital 实际执行，Play vital 汇总任务跳过。
- CI5 API36 connected 为 48 tests / 3 failures / 0 errors / 0 skipped。Popup 系统返回与两项系统栏对比度回归通过；失败为编辑页关闭 IME 回调超时，以及两项真实拖动排序未完成。修复后须 CI6 复验，不把 45 项通过写为整个套件通过。
- CI5 未签名 Direct R8 APK 为 26,442,154 bytes，SHA-256 `f26251ff3570ba498aaee2685a19b17c7c4744c5ffe3c905d5bebeb30b1c380c`；原始 metadata 为 `directRelease` / `com.example.timeapk` / `4.1` / `24` / `SINGLE`。该工件绑定候选 PR merge，不能替代最终 tag 构建，也不能直接发布。
- CI5 artifact `11604884376`（`android-verification-8ad78c206f02369e904a9fad3e20a7b059b90f51`）保留 XML、R8、lint 与系统日志。connected 失败后 OS smoke 未执行；Gradle 在测试结束时卸载目标包，组件私有 cache 随包删除，故没有组件 PNG 或五页截图证据。下一轮使用当前 AGP 9.1 的原生 `android.injected.androidTest.leaveApksInstalledAfterRun=true`，显式检查组件文件后再运行真实 AMS 恢复与五页截图。

## 六、发布动作

以下项目只有各自事实成立后才能勾选。用户已允许修复、验证、提交、推送和发布 4.1，并说明当前无手机；按现有授权继续发布工作，如实保留物理验收未执行的限制，不另设二次审批门。授权不替代最终 APK、签名及公开下载的实际证据。

- [ ] 本版可执行的功能、UI、数据、恢复、无障碍和性能检查已完成，失败已修复并复验；物理设备缺项保持未执行并记录剩余限制
- [ ] 最终代码与发布文档已提交，且发布前工作区干净
- [ ] 创建并推送不可变的 exact `v4.1` tag；本地与远端解引用后 commit 一致
- [ ] 从该 tag commit 新鲜构建并正式签名，未复用旧产物；CI 未签名工件的 revision、原始哈希与 metadata 在本机签名前保留
- [ ] 验证签名、精确证书指纹与 SHA-256，并记录 exact APK 大小、包名 `com.example.timeapk`、`4.1` / `24`、非调试状态和安装权限
- [ ] 准备安全凭据环境：本地用 `gh auth login` / `gh auth token`；CI 才注入 secret，不打印凭据
- [ ] 从 CHANGELOG 的 4.1 小节准备可公开 Release Notes，候选状态与剩余限制按实际结果修订
- [ ] 运行发布脚本；只创建/恢复带 ownership marker 的 owned draft，不覆盖 published Release 或接管人工 draft
- [ ] 删除 owned draft 中的所有旧资产，并验证整个 Release 只保留唯一的 exact Direct APK
- [ ] 最终 GET 核对公开、非 prerelease 的 Release 身份与 asset id、size、digest、MIME、下载 URL；Release 仅含 `glimmer-countdown-4-1.apk`
- [ ] 发布后重新下载并安装线上 APK，核对大小/SHA-256/签名/版本，复测冷启动、更新检查和关键链路
- [ ] 发布锁已按 ownership 验证清理，最后根据实际公开结果更新 README 与本版记录

固定顺序：最终代码与发布文档已提交，且工作区干净 → 创建并推送不可变的 exact tag → 从该 tag 对应 commit 的工作树重新正式签名构建 → 验证签名、精确证书指纹与 SHA-256 → 准备安全凭据环境 → 运行发布脚本。禁止移动已推送 tag、覆盖已发布 Release 或在缺失验证门时直接公开。

## 历史记录（非发布门）

[v4.0 原始清单](releases/v4.0-checklist.md) 保留其发布日期、源码/产物身份、验证结果和负责人当时的豁免。该归档未经改写，不证明 4.1 在同样环境下通过；README 中 v4.0 截图也仅是历史展示。
