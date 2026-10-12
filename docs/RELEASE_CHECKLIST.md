# 发布检查清单（v4.2）

**版本**：`4.2`（`versionCode=25`）  **文档日期**：2026-10-12  **发布状态：候选 / 未发布**

本清单记录 4.2 候选的发布门与验证边界。所有通过结果必须来自本版新鲜执行，并绑定源码 revision、构建、设备和原始报告；未知、未执行和失败不能勾选；APK 文件和设备安装包须互相对应。v4.1 的原始记录已完整归档到 [v4.1 历史清单](releases/v4.1-checklist.md)，v4.0 的记录见 [v4.0 历史清单](releases/v4.0-checklist.md)；旧测试、旧截图和旧真机豁免均不延续到本版。

**唯一正式发布渠道：GitHub Release。** 唯一官方资产为 Direct APK `glimmer-countdown-4-2.apk`。Play flavor 仅保留用于兼容性与开发回归，不是正式发布工件或阻断项。最新公开版本仍为 [v4.1](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.1)。

设计、范围与评审决定见 [4.2 设计文档](superpowers/specs/2026-10-12-v4-2-experience-polish-design.md)。按评审决定 4，本版不安排实体手机验收。

## 当前阶段与证据身份

| 项目 | 当前状态 | 完成所需证据 |
|---|---|---|
| 源码 revision | `feat/v4.2` 分支候选，尚未合并 `main`，尚未创建 `v4.2` tag | 不可变 `v4.2` tag commit，本地/远端解引用一致，与最终 CI source-revision 相同 |
| 自动测试与 lint | 本机工作区：Direct/Play JVM 各 637/637、三份 Direct lint 无问题、AndroidTest 编译通过（非发布门证据） | 最终 tag 的 CI 原始报告：JVM、native connected、publisher 10/10、lint、R8 打包 |
| 模拟器运行与截图 | 本机 API36 模拟器人工核对 Debug 与本机签名 Release 主要页面（非发布门证据） | tag 的 debug 回归与正式签名 QA（API26/31/36 升级 v4.1→v4.2 与 AMS 恢复、API36 五页/分享），截图人工审阅 |
| 物理真机 | 按评审决定 4 不安排；未执行 | 安装/升级、提醒、精确提醒、日历、Launcher 小组件、相册选图与性能缺少物理设备证据，不能写成通过，也不继承历史豁免 |
| 正式签名 | 未执行（最终 APK 须从 tag 新鲜签名） | 证书 SHA-256 须为 `3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`，与线上 v4.1 一致；不记录密码/密钥/token |
| 正式 APK | 未生成；本机候选签名包约 12.5MB（4.1 为 26,463,543 bytes），仅作包体参考 | `com.example.timeapk` / 4.2 / 25、min26/target36、非 debug、安装权限、16KiB zipalign，记录大小与 SHA-256 |
| GitHub 上传与公开复验 | 未执行 | 公开、非 prerelease 的 Release 仅含唯一 asset；无认证下载字节/签名/身份一致；公开 APK 安装与更新检查 QA |

签名 QA 的升级基线固定为公开 v4.1 APK：26,463,543 bytes，SHA-256 `8fbeee7590f89104995ecce98769842dc2a75cf84d8641efd6a0c7b67619e5b4`。没有使用新建 QA 证书代替正式升级证书。

## 一、自动质量门

- [x] `gradle.properties` 的 `VERSION_NAME=4.2` / `VERSION_CODE=25`、Gradle 默认 fallback 和 APK 命名一致
- [x] README、CHANGELOG 和发布文档使用本版候选状态与 exact APK，历史证据可区分
- [ ] `git diff --check` 无尾随空格或冲突标记（最终 tag commit）
- [ ] `./gradlew --no-daemon --no-parallel testDirectDebugUnitTest` 在最终 tag 的 CI 新鲜通过，记录完整测试结果
- [ ] `./gradlew compileDirectDebugAndroidTestKotlin` 通过，connected 套件（含 Room 10→11 迁移）在 API36 通过
- [ ] `./gradlew lintDirectDebug lintDirectRelease lintVitalDirectRelease` 通过，无未解释 warning
- [ ] publisher 隔离 PowerShell 状态机 10/10 通过，包括本地预检零 GitHub 请求、锁竞争、owned draft 恢复与全资产清理、失败清理、证书输出和临时资产 URL 场景
- [ ] Debug APK 渠道身份与权限正确：Direct 包含 `REQUEST_INSTALL_PACKAGES` 与 `SCHEDULE_EXACT_ALARM`，Play 均不包含；Play 开发回归单独记录
- [ ] Direct Release 默认签名门保持；从最终 tag 完成 R8、资源压缩和正式签名，或按发布指引在 CI 生成未签名工件后在本机正式签名，未签名工件不得发布

## 二、4.2 变更要求对照表

| 要求 | 当前实现方向 | 必须验证的场景 | 当前结果 |
|---|---|---|---|
| 字体子集与包体 | Glimmer Serif 可变子集（完整 GB2312）+ Glimmer Xiaowei 一级字子集，SIL OFL 改名 | 常用字/人名字覆盖、粗细可变、正式 APK ≤13MB | JVM 字体 cmap 覆盖与 ≤8,000,000 bytes 预算测试本机通过；正式 APK 待 tag 构建 |
| 数据库 10→11 | 新增 `categories` 表与 `events.imagePath`，迁移只加法 | 4.1 数据原地升级后事件、偏好、置顶与排序保留；新字段为空 | androidTest 迁移用例已编写，待 CI connected；正式包升级待签名 QA |
| 自定义分类 | 分类表 + 名称/颜色导出，按名称合并导入 | 新建/改名/改色/删除后事件归“其他”；导出导入往返；筛选、月历色点、详情、分享卡一致 | JVM 导入合并与筛选映射通过；设备上分类管理与导入人工核对通过（模拟器） |
| 事件配图 | 系统照片选择器、私有目录压缩存储、孤儿清理 | 选图、详情题图、卡片缩略图、分享底纹；删除事件后清理；不随 JSON 导出 | 未在设备上实际选图（非发布门证据亦缺）；须在 QA 中记录 |
| 单事件小组件 | 独立 provider、配置页、选定事件或最近事件 | 添加/配置/删除实例、尺寸变化、跨日刷新、点击打开详情 | JVM 选择与尺寸策略通过；真实 Launcher 未执行 |
| 首页与月历 | “最近”横幅（默认开）、朱印“今天”、置顶标记、顶栏搜索、长按菜单；农历/节气格、今天与选中区分、滑动换月 | 浅/深色、窄屏、跨 0 点“今天”刷新、筛选一致 | 模型测试与模拟器人工核对通过；跨 0 点刷新为单元测试 |
| 新建页 | 日期在标题下、快捷提醒、常用重复、分类与配图、实心保存 | 草稿恢复、保存不重复、预填月历日期 | SavedState/重建 connected 用例待 CI；模拟器人工核对通过 |
| 通知与精确提醒 | 事件名标题、正文“还有 N 天 · 日期”、查看按钮；Direct 可选精确闹钟 | 通知送达、改名后标题更新、系统撤销精确闹钟权限后回退 WorkManager；Play 无该权限 | JVM 文案与回退策略通过；实际送达未执行 |
| 更新检查 | 预发布后缀解析、下载进度、未知来源权限引导 | 已是最新版本、新版本、失败三态；安装权限引导 | JVM 版本解析通过；公开 Release 更新检查待发布后 QA |
| 宽屏与动态取色 | ≥840dp 列表/详情双栏；Android 12+ 跟随壁纸 | 平板/横屏布局、深浅色动态色对比 | 模拟器宽屏双栏人工核对通过；动态取色未执行 |
| 文案 | 功能性文案直白，展示位保留宋韵 | 中文/英文资源一致、无遗留未用 key | i18n 与 lint 本机通过 |

## 三、数据与核心功能

- [ ] 新建、编辑、删除、撤销、搜索、筛选、置顶和三种排序完整回归
- [ ] JSON 导出/导入完整字段往返（含自定义分类）、重复识别与 CSV 转义正确；失败不覆盖现有数据
- [ ] “记得日子” `.mdb` 导入和 v4.1 数据库原地升级保留事件及偏好
- [ ] 公历/农历、按天/周/月/半年/年重复覆盖月末、闰年和跨年
- [ ] 当天/提前多天提醒、精确提醒开关、设备重启、权限变化、通知频道关闭和后台省电路径有设备记录
- [ ] 可写日历同步与无可写系统日历提示、撤权清理和重试经过实际 provider 验证
- [ ] 分享保存、系统分享、GitHub 更新检查和 Direct APK 安装路径可用

提醒默认使用 WorkManager；只有 Direct 渠道在用户开启且系统允许精确闹钟时按设定时间触发。后台提醒需记录实际环境与到达时间，不能用单元测试替代设备证据。

## 四、UI、无障碍、小组件与性能

- [ ] 卡片、列表、月历、详情、新建/编辑和设置在浅色/深色下可读，采用同一候选新鲜截图
- [ ] 320dp 窄窗口、≥840dp 宽屏、横屏/短高度、100%/150%/200% 字体检查内容与操作可达性
- [ ] 输入法、拼音组合态、软硬键盘、旋转和返回手势（含预测式返回）不造成未预期数据丢失
- [ ] TalkBack 主入口、开关、展开状态、日期选择器、自定义排序上移/下移、长按菜单可操作
- [ ] 分享弹窗不透明、预览完整、保存结果在弹窗内提示；导出图片与主题、配图及长文本对照通过
- [ ] 小组件“跟随首页”与置顶/排序一致，多实例配置互不污染；单事件小组件跟随所选事件
- [ ] 小组件 1-5 格“预览宽度 / 预览高度”与 Launcher 实际尺寸区别清楚；不支持透明度的背景不显示该项
- [ ] 真实 Launcher 的背景（含深色磨砂玻璃）、边框、圆角、密度、农历前缀和文字对比验证完成
- [ ] 冷启动、首页滚动、月历切换、详情与设置导航的帧和内存记录无未解释退化
- [ ] Android 8 / API 26、Android 12 及当前 target SDK 环境完成核心 smoke
- [ ] 物理手机安装/升级、通知、日历、Launcher 与性能验收：按评审决定 4 不安排，记录为未执行

## 五、本版证据记录

### 本机候选核对（2026-10-12，非发布门证据）

来自 `feat/v4.2` 工作区，未绑定最终 tag：JVM Direct/Play 各 637/637；`lintDirectDebug`、`lintDirectRelease`、`lintVitalDirectRelease` 均无问题；AndroidTest 编译通过；本机签名 `assembleDirectRelease` 产物 12,503,873 bytes。API36（1080×1920）模拟器安装 Debug 与本机签名 Release，导入 6 条中文样例（含自定义分类），人工核对首页卡片/横幅/朱印“今天”/置顶、月历农历与节气、详情、分享预览、新建页、小组件设置、深色模式、宽屏双栏与长按菜单，Release 包无崩溃。核对中发现并修复：宽屏双栏不出现、新建页选项被截断、浅色主题下强制浅色字的磨砂玻璃缺少文字阴影。

未覆盖：真实 Launcher 小组件与选择器预览、通知送达与精确提醒、相册选图与配图显示、动态取色、实体手机。

### 最终标签、正式签名与公开发布

待执行。

## 六、发布动作

以下项目只有各自事实成立后才能勾选。创建并推送 `v4.2` tag 与公开 Release 前须再次取得发布负责人确认；授权不替代最终 APK、签名及公开下载的实际证据。

- [ ] 本版发布门覆盖的回归、正式包 v4.1→v4.2 升级/恢复与 API36 实际页面/分享验证已完成；失败及修复复验分别记录，未执行的广泛矩阵与物理项保留在本清单
- [ ] 最终代码与发布文档已提交，且发布前工作区干净；公开复验结果与原图在发布后单独文档提交，不移动 tag
- [ ] 创建并推送不可变的 exact `v4.2` tag；本地与远端解引用后 commit 一致
- [ ] 从该 tag commit 新鲜构建并正式签名，未复用旧产物；CI 未签名工件的 revision、原始哈希与 metadata 在本机签名前保留
- [ ] 验证签名、精确证书指纹与 SHA-256，并记录 exact APK 大小、包名 `com.example.timeapk`、`4.2` / `25`、非调试状态和安装权限
- [ ] 准备安全凭据环境：本地用 `gh auth login` / `gh auth token`；CI 才注入 secret，不打印凭据
- [ ] 将 CHANGELOG 的 `## [4.2] - 未发布` 改为发布日期，并据此准备可公开 Release Notes，剩余限制按实际结果修订
- [ ] 运行发布脚本；只创建/恢复带 ownership marker 的 owned draft，不覆盖 published Release 或接管人工 draft
- [ ] 删除 owned draft 中的所有旧资产，并验证整个 Release 只保留唯一的 exact Direct APK
- [ ] 最终 GET 核对公开、非 prerelease 的 Release 身份与 asset id、size、digest、MIME、下载 URL；Release 仅含 `glimmer-countdown-4-2.apk`
- [ ] 发布后重新下载并安装线上 APK，核对大小/SHA-256/签名/版本，复测冷启动、更新检查和关键链路（模拟器范围；不是实体手机或应用内安装器 UI 验收）
- [ ] 发布锁已按 ownership 验证清理，最后根据实际公开结果更新 README 与本版记录

固定顺序：最终代码与发布文档已提交，且工作区干净 → 创建并推送不可变的 exact tag → 从该 tag 对应 commit 的工作树重新正式签名构建 → 验证签名、精确证书指纹与 SHA-256 → 准备安全凭据环境 → 运行发布脚本。禁止移动已推送 tag、覆盖已发布 Release 或在缺失验证门时直接公开。

## 历史记录（非发布门）

[v4.1 原始清单](releases/v4.1-checklist.md) 与 [v4.0 原始清单](releases/v4.0-checklist.md) 保留各自的发布日期、源码/产物身份、验证结果和当时的豁免。归档未经改写，不证明 4.2 在同样环境下通过；README 中 v4.1 截图也仅是历史展示。
