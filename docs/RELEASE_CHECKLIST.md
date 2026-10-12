# 发布检查清单（v4.2）

**版本**：`4.2`（`versionCode=25`）  **文档日期**：2026-10-12  **发布状态：已发布**

本清单记录 4.2 正式发布及其验证边界。所有通过结果必须来自本版新鲜执行，并绑定源码 revision、构建、设备和原始报告；未知、未执行和失败不能勾选；APK 文件和设备安装包须互相对应。v4.1 的原始记录已完整归档到 [v4.1 历史清单](releases/v4.1-checklist.md)，v4.0 的记录见 [v4.0 历史清单](releases/v4.0-checklist.md)；旧测试、旧截图和旧真机豁免均不延续到本版。

**唯一正式发布渠道：GitHub Release。** 唯一官方资产为 Direct APK `glimmer-countdown-4-2.apk`。Play flavor 仅保留用于兼容性与开发回归，不是正式发布工件或阻断项。最新公开版本为 [v4.2](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.2)。

设计、范围与评审决定见 [4.2 设计文档](superpowers/specs/2026-10-12-v4-2-experience-polish-design.md)。按评审决定 4，本版不安排实体手机验收。

## 当前阶段与证据身份

| 项目 | 当前状态 | 完成所需证据 |
|---|---|---|
| 源码 revision | 不可变 `v4.2` tag commit `f0937448efa02232d5c6b1d47c531d50cad0797e`（annotated tag 对象 `294035246e8a96072175710efbc4961088531df3`），本地/远端解引用一致；PR #2 快进合并到 `main`，提交 SHA 未改写 | tag CI 与签名 QA 的 source-revision 均为 `f093744` |
| 自动测试与 lint | 最终 tag CI [38235651872](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38235651872)：Direct/Play 各 85 份 suite / 638 tests，0 失败 0 跳过；API36 native 51/51；publisher 10/10；三份 lint 各 0 error / 1 个既有 `OldTargetApi` | artifact `android-verification-f0937448…`，本地 `.tmp/final-tag-ci-38235651872/` |
| 模拟器运行与截图 | 正式签名 QA [38238275647](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38238275647) 与公开 APK 复验 [38239618078](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38239618078) 均为 API26/31/36 全成功；API36 五页、排序菜单、相册保存与分享返回通过，公开包截图已人工审阅 | 原始报告 `.tmp/final-signed-qa-38238275647/`、`.tmp/public-qa-38239618078/`；截图见 [4.2 截图记录](screenshots/4.2/README.md) |
| 物理真机 | 按评审决定 4 不安排；未执行 | 安装/升级、提醒、精确提醒、日历、Launcher 小组件、相册选图与性能缺少物理设备证据，不能写成通过，也不继承历史豁免 |
| 正式签名 | 本机以既有正式密钥签名；单一签名者、v2/v3；公开下载独立验签一致 | 证书 SHA-256 `3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`，与线上 v4.0/v4.1 一致；不记录密码/密钥/token |
| 正式 APK | 12,513,338 bytes，SHA-256 `7fe9673e4b3a85f4152c1e5786f8a221779a95fc56578f9c93b660b4e9019e74`（4.1 为 26,463,543 bytes） | `com.example.timeapk` / 4.2 / 25、min26/target36、非 debug、`REQUEST_INSTALL_PACKAGES` 与 `SCHEDULE_EXACT_ALARM`、16KiB zipalign 通过 |
| GitHub 上传与公开复验 | Release `410506677` 公开且非 prerelease，唯一 asset `633513961`（`application/vnd.android.package-archive`）；无认证下载字节/签名/身份一致；公开 APK 三平台复验成功，API36 实际检查更新显示“已是最新版本” | `.tmp/public-v42-verified/` 与 `.tmp/public-qa-38239618078/`；发布锁与临时 QA draft 已清理 |

签名 QA 的升级基线固定为公开 v4.1 APK：26,463,543 bytes，SHA-256 `8fbeee7590f89104995ecce98769842dc2a75cf84d8641efd6a0c7b67619e5b4`。没有使用新建 QA 证书代替正式升级证书。

## 一、自动质量门

- [x] `gradle.properties` 的 `VERSION_NAME=4.2` / `VERSION_CODE=25`、Gradle 默认 fallback 和 APK 命名一致
- [x] README、CHANGELOG 和发布文档使用本版状态与 exact APK，历史证据可区分
- [ ] `git diff --check` 无尾随空格或冲突标记：未通过。`v4.1..v4.2` 有三处 strings.xml 文件末尾多余空行，以及 `res/raw/ofl_zcool_xiaowei.txt` 一处行尾空格（上游许可原文，按原样保留）；无冲突标记，不影响构建与资源解析，留待下一版清理
- [x] `testDirectDebugUnitTest` 与 `testPlayDebugUnitTest` 在最终 tag CI 新鲜通过（各 638/638）
- [x] `compileDirectDebugAndroidTestKotlin` 通过；connected 套件在 API36 51/51，含 `migrate10To11_addsCategoriesTableAndImageColumnWithoutTouchingRows`
- [x] `lintDirectDebug`、`lintDirectRelease`、`lintPlayRelease`、`lintVitalDirectRelease`、`lintVitalPlayRelease` 通过；唯一 warning 为既有 `OldTargetApi`（targetSdk 36，沿用 4.1 结论）
- [x] publisher 隔离 PowerShell 状态机 10/10 通过（tag CI 日志：All publisher mock scenarios passed (10/10)）
- [x] Debug APK 渠道身份与权限正确：Direct `com.example.timeapk` / 4.2 / 25 含 `REQUEST_INSTALL_PACKAGES` 与 `SCHEDULE_EXACT_ALARM`；Play `com.example.timeapk.play` / 4.2 / 25 均不含；两者均为 debug，Play 仅开发回归
- [x] Direct Release 默认签名门保持；tag CI 在无密钥环境完成 R8 / 资源压缩的未签名工件，本机 zipalign 后正式签名

## 二、4.2 变更要求对照表

| 要求 | 当前实现方向 | 必须验证的场景 | 当前结果 |
|---|---|---|---|
| 字体子集与包体 | Glimmer Serif 可变子集（完整 GB2312）+ Glimmer Xiaowei 一级字子集，SIL OFL 改名 | 常用字/人名字覆盖、粗细可变、正式 APK ≤13MB | tag CI JVM 字体 cmap 覆盖与 ≤8,000,000 bytes 预算通过；正式 APK 12,513,338 bytes |
| 数据库 10→11 | 新增 `categories` 表与 `events.imagePath`，迁移只加法 | 4.1 数据原地升级后事件、偏好、置顶与排序保留；新字段为空 | tag CI 迁移测试通过；正式包 API26/31/36 由 v4.1 覆盖升级，首次安装时间、UID、标题、备注与日期保留 |
| 自定义分类 | 分类表 + 名称/颜色导出，按名称合并导入 | 新建/改名/改色/删除后事件归“其他”；导出导入往返；筛选、月历色点、详情、分享卡一致 | JVM 导入合并与筛选映射通过；候选阶段模拟器人工核对分类管理与导入；正式包未单独运行该场景 |
| 事件配图 | 系统照片选择器、私有目录压缩存储、孤儿清理 | 选图、详情题图、卡片缩略图、分享底纹；删除事件后清理；不随 JSON 导出 | 未在设备上实际选图；按现有范围记录为未执行 |
| 单事件小组件 | 独立 provider、配置页、选定事件或最近事件 | 添加/配置/删除实例、尺寸变化、跨日刷新、点击打开详情 | JVM 选择与尺寸策略通过；真实 Launcher 未执行 |
| 首页与月历 | “最近”横幅（默认开）、朱印“今天”、置顶标记、顶栏搜索、长按菜单；农历/节气格、今天与选中区分、滑动换月 | 浅/深色、窄屏、跨 0 点“今天”刷新、筛选一致 | 模型测试、connected 首页搜索/拖拽/面板返回通过；正式包首页与月历截图人工审阅；跨 0 点刷新为单元测试 |
| 新建页 | 日期在标题下、快捷提醒、常用重复、分类与配图、实心保存 | 草稿恢复、保存不重复、预填月历日期 | connected SavedState / 重建 / IME 用例通过；正式包 API26/31/36 AMS 后台终止后草稿恢复并保存通过 |
| 通知与精确提醒 | 事件名标题、正文“还有 N 天 · 日期”、查看按钮；Direct 可选精确闹钟 | 通知送达、改名后标题更新、系统撤销精确闹钟权限后回退 WorkManager；Play 无该权限 | JVM 文案与回退策略通过；Direct/Play 权限差异经 aapt 核对；实际送达未执行 |
| 更新检查 | 预发布后缀解析、下载进度、未知来源权限引导 | 已是最新版本、新版本、失败三态；安装权限引导 | JVM 版本解析通过；公开 APK 在 API36 实际检查更新显示“已是最新版本”；新版本下载与安装引导未在设备执行 |
| 宽屏与动态取色 | ≥840dp 列表/详情双栏；Android 12+ 跟随壁纸 | 平板/横屏布局、深浅色动态色对比 | 候选阶段模拟器宽屏双栏人工核对；动态取色未执行 |
| 文案 | 功能性文案直白，展示位保留宋韵 | 中文/英文资源一致、无遗留未用 key | i18n 测试与 lint 在 tag CI 通过 |

## 三、数据与核心功能

- [ ] 新建、编辑、删除、撤销、搜索、筛选、置顶和三种排序完整回归（自动测试与正式包排序菜单覆盖部分，完整人工回归未执行）
- [ ] JSON 导出/导入完整字段往返（含自定义分类）、重复识别与 CSV 转义正确；失败不覆盖现有数据（JVM 覆盖，设备文件选择器未执行）
- [x] v4.1 数据库原地升级保留事件及偏好（正式包 API26/31/36 覆盖升级）；“记得日子” `.mdb` 导入仅有 JVM 回归
- [ ] 公历/农历、按天/周/月/半年/年重复覆盖月末、闰年和跨年（JVM 覆盖，设备矩阵未执行）
- [ ] 当天/提前多天提醒、精确提醒开关、设备重启、权限变化、通知频道关闭和后台省电路径有设备记录
- [ ] 可写日历同步与无可写系统日历提示、撤权清理和重试经过实际 provider 验证
- [x] 分享保存、系统分享和 GitHub 更新检查可用（公开 APK API36 实际执行）；Direct APK 应用内下载安装路径未在设备执行

提醒默认使用 WorkManager；只有 Direct 渠道在用户开启且系统允许精确闹钟时按设定时间触发。后台提醒需记录实际环境与到达时间，不能用单元测试替代设备证据。

## 四、UI、无障碍、小组件与性能

- [ ] 卡片、列表、月历、详情、新建/编辑和设置在浅色/深色下可读，采用同一候选新鲜截图（正式包仅浅色五页）
- [ ] 320dp 窄窗口、≥840dp 宽屏、横屏/短高度、100%/150%/200% 字体检查内容与操作可达性
- [ ] 输入法、拼音组合态、软硬键盘、旋转和返回手势（含预测式返回）不造成未预期数据丢失
- [ ] TalkBack 主入口、开关、展开状态、日期选择器、自定义排序上移/下移、长按菜单可操作（connected 语义测试覆盖部分）
- [x] 分享弹窗不透明、预览完整、保存结果在弹窗内提示；导出图片 1080×1350 与预览一致（正式包 API36，未覆盖配图与长文本）
- [ ] 小组件“跟随首页”与置顶/排序一致，多实例配置互不污染；单事件小组件跟随所选事件
- [ ] 小组件 1-5 格“预览宽度 / 预览高度”与 Launcher 实际尺寸区别清楚；不支持透明度的背景不显示该项
- [ ] 真实 Launcher 的背景（含深色磨砂玻璃）、边框、圆角、密度、农历前缀和文字对比验证完成
- [ ] 冷启动、首页滚动、月历切换、详情与设置导航的帧和内存记录无未解释退化（仅有诊断快照）
- [x] Android 8 / API 26、Android 12 / API 31 及当前 target SDK API 36 完成核心 smoke（覆盖升级、数据保留、AMS 恢复）
- [ ] 物理手机安装/升级、通知、日历、Launcher 与性能验收：按评审决定 4 不安排，记录为未执行

## 五、本版证据记录

### 最终标签、正式签名与公开发布

| 证据 | 实际结果 | 原始来源与范围 |
|---|---|---|
| PR CI | [PR #2](https://github.com/Francesco502/glimmer-countdown-app/pull/2) 最终 run [38232942181](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38232942181) 通过；此前 3 次失败（38214187285、38225569827、38229711541）原样保留，另有 2 次被新提交取消 | 失败原因依次为：测试仍按 4.1 首页结构查找（“最近”横幅重复标题、搜索移至顶栏）及深色自定义背景的图标期望（7 项）；分享预览位图在 dispose 时回收，RenderThread 绘制已回收位图（真实缺陷，已修复）；边缘拖拽测试的 Compose 线程竞争（测试侧修复）。smoke 设置页标记由本机预演发现并修复 |
| 最终 tag CI | [38235651872](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38235651872)：JVM 638/638 ×2、native 51/51、publisher 10/10、lint 0 error、runtime smoke 通过 | source-revision `f093744`；`.tmp/final-tag-ci-38235651872/` |
| 未签名 R8 APK | 12,495,689 bytes，SHA-256 `0d199990e4a1c002848aba63d8993362626224d558bf3bba4185e8f19cb14ff0`；原 metadata 为唯一 `directRelease` / 4.2 / 25 | 同一 tag CI 的 `outputs/apk/direct/release/` 与 `reports/unsigned-direct-release/`；保留原文件后本机正式签名，发布用 metadata 仅改 `outputFile` |
| 正式签名与身份 | `glimmer-countdown-4-2.apk` / 12,513,338 bytes / SHA-256 `7fe9673e4b3a85f4152c1e5786f8a221779a95fc56578f9c93b660b4e9019e74`；单签名者、v2/v3、证书与 v4.0/v4.1 相同；16KiB zipalign 通过 | `.tmp/candidates/final-tag-38235651872/` 的 receipt、apksigner 与 aapt 输出 |
| 正式包三平台 QA | [38238275647](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38238275647)：API26/31/36 全成功，4.1/24 覆盖升级到 4.2/25；首次安装时间、UID（10077/10145/10216）、标题、备注和日期保留；AMS 后台终止后草稿恢复 | `.tmp/final-signed-qa-38238275647/`；候选通过临时 owned draft `410498170` 提供，QA 后按 ownership marker 删除，无残留 ref |
| 正式公开与无认证下载 | [v4.2](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.2) Release `410506677`，2026-10-12T10:57:18Z 公开，非 prerelease，唯一 asset `633513961`；下载大小/SHA/签名/版本与最终 APK 完全一致 | `.tmp/public-v42-verified/`；发布锁 `refs/heads/release-locks/v4.2` 已清理 |
| 公开 APK 复验 | [38239618078](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38239618078)：API26/31/36 全成功；固定公开 asset `633513961`；API36 `update_check` 为 `update_latest`，五页 5/5、分享保存与返回通过 | `.tmp/public-qa-38239618078/`；截图与哈希见 [4.2 截图记录](screenshots/4.2/README.md) |

物理设备、自然 LMK、完整主题/字体/旋转/TalkBack、实际后台通知与精确提醒送达、用户日历云同步、Launcher 绑定（含单事件小组件）、相册选图与配图显示、动态取色和接收方读取/发送仍未执行。meminfo/gfxinfo 仅为诊断快照，不代表性能基准通过。

Release Notes 取自 CHANGELOG 的 4.2 小节，其中“4.2 设计文档”为仓库相对链接，在 Release 页面上不可直接打开；APK 与校验不受影响。

## 六、发布动作

以下项目只有各自事实成立后才能勾选。用户已授权在 CI 通过并合并后创建 tag 并发布 4.2；授权不替代最终 APK、签名及公开下载的实际证据。

- [x] 本版发布门覆盖的回归、正式包 v4.1→v4.2 升级/恢复与 API36 实际页面/分享验证已完成；失败及修复复验分别记录，未执行的广泛矩阵与物理项保留在本清单
- [x] 最终代码与发布文档已提交，且发布前工作区干净；公开复验结果与原图在发布后单独文档提交，不移动 tag
- [x] 创建并推送不可变的 exact `v4.2` tag；本地与远端解引用后 commit 一致
- [x] 从该 tag commit 新鲜构建并正式签名，未复用旧产物；CI 未签名工件的 revision、原始哈希与 metadata 在本机签名前保留
- [x] 验证签名、精确证书指纹与 SHA-256，并记录 exact APK 大小、包名 `com.example.timeapk`、`4.2` / `25`、非调试状态和安装权限
- [x] 准备安全凭据环境：本地由 publisher 通过 `gh auth token` 取得已登录凭据；未注入或打印 token
- [x] CHANGELOG 的 4.2 小节已写入发布日期 `2026-10-12`，作为可公开 Release Notes；剩余限制按实际结果修订
- [x] 运行发布脚本；只创建/恢复带 ownership marker 的 owned draft，不覆盖 published Release 或接管人工 draft
- [x] 删除 owned draft 中的所有旧资产，并验证整个 Release 只保留唯一的 exact Direct APK
- [x] 最终 GET 核对公开、非 prerelease 的 Release 身份与 asset id、size、digest、MIME、下载 URL；Release 仅含 `glimmer-countdown-4-2.apk`
- [x] 发布后重新下载并安装线上 APK，核对大小/SHA-256/签名/版本，复测冷启动、更新检查和关键链路（上述 API26/31/36 范围；不是实体手机或应用内安装器 UI 验收）
- [x] 发布锁已按 ownership 验证清理，最后根据实际公开结果更新 README 与本版记录

固定顺序：最终代码与发布文档已提交，且工作区干净 → 创建并推送不可变的 exact tag → 从该 tag 对应 commit 的工作树重新正式签名构建 → 验证签名、精确证书指纹与 SHA-256 → 准备安全凭据环境 → 运行发布脚本。禁止移动已推送 tag、覆盖已发布 Release 或在缺失验证门时直接公开。

## 历史记录（非发布门）

[v4.1 原始清单](releases/v4.1-checklist.md) 与 [v4.0 原始清单](releases/v4.0-checklist.md) 保留各自的发布日期、源码/产物身份、验证结果和当时的豁免。归档未经改写，不证明 4.2 在同样环境下通过；README 中 v4.1 / v4.0 截图也仅是历史展示。
