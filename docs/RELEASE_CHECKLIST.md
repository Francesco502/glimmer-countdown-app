# 发布检查清单（v4.2.1）

**版本**：`4.2.1`（`versionCode=26`）  **文档日期**：2026-10-15  **发布状态：已发布**

本版修复桌面小组件夜间模式的自动文字颜色。所有通过结果必须来自本版新鲜执行，并绑定源码 revision、构建、设备和原始报告；未知、未执行和失败不能勾选；APK 文件和设备安装包须互相对应。旧测试、旧截图和旧真机豁免均不延续到本版。

**唯一正式发布渠道：GitHub Release。** 唯一官方资产为 Direct APK `glimmer-countdown-4-2-1.apk`。Play flavor 仅保留用于兼容性与开发回归。

## 当前阶段与证据身份

| 项目 | 当前状态 |
|---|---|
| 源码 revision | `v4.2.1` → `e532c3333e10148c5771906fd2996adb53735104`；本地与远端解引用一致；tag 对象 `51440e1875824a6c8d95fdaeff8a0d2fd812d020` |
| 自动测试与 lint | 最终标签 CI [38735339024](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38735339024) 成功：Direct/Play 各638项、native55项、lint无错误、R8及运行时检查通过 |
| 正式 APK | 最终 tag CI 新鲜 R8 APK 本机正式签名；`com.example.timeapk` / 4.2.1 / 26，min26 / target36，非 debug；12,517,434 bytes |
| 正式签名 | 沿用正式证书 SHA-256 `3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`；最终 APK 已独立验签 |
| 升级基线 | 公开 v4.2：12,513,338 bytes，SHA-256 `7fe9673e4b3a85f4152c1e5786f8a221779a95fc56578f9c93b660b4e9019e74`；已实时核对公开资产 |
| 正式包模拟器 QA | [38740578879](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38740578879)：API26/31/36 升级、AMS 恢复通过；API36 五页与分享通过；公开包 QA [38742224120](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38742224120) 三平台成功，API36 实际显示“已是最新版本” |
| GitHub Release | 已于 2026-10-15 13:21:47 UTC 公开 [v4.2.1](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.2.1)；Release 413837859，唯一资产 643936757；未覆盖 v4.2 |
| 物理手机与 Launcher | API36 模拟器 Pixel Launcher 实际绑定列表与单日小组件，日间→夜间→日间通过；实体手机及其 Launcher 未执行 |

## 一、自动质量门

- [x] 版本号、Gradle fallback 与 exact APK 命名同步为 4.2.1 / 26
- [x] CHANGELOG 使用发布日期 2026-10-15，公开说明仅包含用户可见修复
- [x] v4.2 发布清单原样归档，旧截图保留原版本标识
- [x] `git diff --check`、Python AST 与工作流 shell 脚本语法检查
- [x] Direct / Play JVM 各638项通过、两渠道 AndroidTest 编译通过
- [x] `lintDirectDebug`、`lintDirectRelease`、`lintPlayRelease`、`lintVitalDirectRelease`、`lintVitalPlayRelease`
- [x] 本地与最终 tag CI publisher 状态机均10/10，IME只读查询恢复自检2/2
- [x] 最终 tag CI：新鲜 JVM / native / lint / R8 / runtime smoke 全部通过

## 二、4.2.1 变更要求对照表

| 要求 | 实现与必须验证的场景 | 本版结果 |
|---|---|---|
| 自动模式恢复 | 每次刷新写入文字颜色；复用视图时从手动浅字/深字切回自动 | 最终标签 native 回归通过 |
| 日夜切换 | Android12+ 在 host apply/reapply 时解析资源色或日夜颜色对；API26-30 在刷新时重新取色 | API36 native 与正式 APK 的模拟器 Launcher 切换通过；API26 刷新兼容路径8项通过、1项API31专用测试跳过 |
| 同一配色规则 | 列表行、空状态、单事件小组件共用颜色应用逻辑；系统 / 宣纸 / 青瓷 / 透明 / 磨砂背景 | 最终标签 native 与 JVM 源契约回归通过 |
| 手动文字颜色 | 强制浅字或深字不受 host 日夜模式影响 | 最终标签 native 回归通过 |

## 三、数据与核心功能

- [x] 正式签名包在 API26/31/36 由公开 v4.2 原地升级；首次安装时间、UID、标题、备注及日期保留
- [x] 正式包 AMS 后台终止后草稿恢复并保存（API26/31/36）
- [x] API36 五页、分享保存、分享选择器返回
- [x] 公开版本实际检查更新：发布后 QA 38742224120 成功，实际显示“已是最新版本”

数据库及配置保存格式未修改。事件、偏好和多实例配置的自动测试不替代实体手机验收。

## 四、UI、无障碍、小组件与性能

- [x] API36 全部55项原生回归（含新增4项文字颜色测试）
- [x] 最终标签新鲜 Debug 包：API26 小组件8项通过、1项Android12专用检查跳过；API36 原生日夜颜色路径通过。API31 正式包升级/恢复通过，未单独做该平台 Launcher 绑定
- [x] 本版正式包13张新鲜原图及来源、SHA-256已归档（10张公开包页面/分享/更新，3张模拟器Launcher）
- [ ] 实体手机安装/升级、真实 Launcher 绑定与日夜切换：未执行
- [ ] 物理通知送达、系统日历、完整字体/主题/旋转/TalkBack 与长期性能：未执行

## 五、本版证据记录

修复阶段的旧版本工作区验证保留在本地 `.tmp/widget-theme-qa/`：原实现 3 项失败；修复后 API36 小组件 9 项通过，API26 8 项通过且 1 项 Android12 专用检查不适用；Direct/Play JVM 各638项、Debug/Release lint无错误。这些结果绑定改版前 4.2 元数据，不能当作最终 4.2.1 工件的通过证明。

最终 tag CI、原始 metadata、未签名哈希与正式签名 receipt 已保存；三平台候选 QA 和公开下载复验通过。公开包三平台 QA [38742224120](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38742224120) 已全部成功，原始报告独立核验通过。

标签前 CI [38728734235](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38728734235) 的 JVM、lint、R8 与 API36 原生55项全部通过；后续 runtime smoke 在只读 `dumpsys input_method` 时发生 ADB transport 断线（返回255，随后暂时 offline 并恢复），整体 run 保留为失败。现已为这项只读查询增加三次有界重试，2项标准库自检验证重连及持续失败仍会报错；完整源码复验 [38734341326](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38734341326) 和最终标签 CI 均成功。输入、保存与字段断言未放宽。

## 六、发布动作

用户已于 2026-10-15 授权更新版本、完成现有验证流程并发布 GitHub Release。

- [x] 最终代码与版本文档已提交，发布前工作区干净且 HEAD 精确位于标签
- [x] 创建并推送不可变的 exact `v4.2.1` tag
- [x] 从该 tag commit 新鲜构建并正式签名，记录 SHA-256 / 大小 / 身份 / 权限与正式证书
- [x] 正式包 API26/31/36 升级与恢复 QA 通过；保留失败证据
- [x] 运行发布脚本；验证 ownership marker、Git ref 锁与唯一资产
- [x] 最终 GET 验证公开非 prerelease，Release 仅含 `glimmer-countdown-4-2-1.apk`
- [x] 公开无认证下载 HTTP200，独立验签、哈希、包身份和16KiB对齐核对
- [x] 公开包三平台复验：38742224120 成功，升级与恢复通过，API36页面/分享/实际更新检查通过
- [x] 发布锁已确认不存在，本轮 owned QA draft 删除后GET为404；据实际结果更新文档，不移动 tag

## 本版工件与 QA 驱动记录

- APK SHA-256：`21563bc13a72be41d0f6cb21e8db136bcafd20bcf7c94a6390fd53e085cd896c`；原始未签名 SHA-256：`6377766b1c3be84d7cf3a7ee55de8ae089f72eb4e7bf08f518955bbb8c2e78e7`。公开无认证下载与正式候选逐字节一致，正式证书、单一签名者、非 debug、Direct 安装权限均已独立核验。
- APK 源码为标签 `e532c3333e10148c5771906fd2996adb53735104`；签名包 QA 驱动为 `d3c5ff9b1f196f4598c27816d61414eb05fe58ce`，两者分别记录。后者仅把升级夹具固定标签缩短：49→30字符，12位随机标识、逐字符完整前缀核对、字段/日期/UID/首次安装时间断言和全部超时保持原样。发布标签未移动、APK 未被重建替换。
- 旧驱动签名包 QA [38737659244](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38737659244) 的API26/31成功；API36两次均在4分钟外层进程预算收尾时超时，保持失败。原始日志、截图与两次artifact已分别下载保存。新驱动完整三平台 QA 38740578879 成功。
- 本地 API36 正式包使用原标签驱动补跑完整升级、AMS恢复、五页与分享，全部通过；49字符的升级自动化流程含逐字符 UI 查询耗时303.6秒，不是APK安装耗时或性能基准。
- 标签前 CI [38731226578](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38731226578) 有一项既有导入预览等待节点超时，保持失败；同一测试在独立模拟器3次通过，未改其断言，后续完整源码和标签CI均通过。
- 本地原始记录：`.tmp/finaltag-ci-38735339024/`、`.tmp/finaltag-v421-signed-38735339024/`、`.tmp/candidate-qa-38740578879/`、`.tmp/public-v421-verified/`、`.tmp/finaltag-launcher-v421/`、`.tmp/qa-v421-api26-final/`。公开复验报告位于 `.tmp/public-qa-38742224120/`；[13张原图记录](screenshots/4.2.1/README.md) 与 [结构化验证记录](releases/v4.2.1-verification.json) 已归档。

## 历史记录（非发布门）

[v4.2 原始清单](releases/v4.2-checklist.md)、[v4.1 原始清单](releases/v4.1-checklist.md) 与 [v4.0 原始清单](releases/v4.0-checklist.md) 保留各自的源码、APK、验证范围和历史决定。v4.2 唯一资产为 `glimmer-countdown-4-2.apk`；其测试及图片不证明本版通过。
