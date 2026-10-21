# 发布检查清单（v4.2.3）

**版本**：`4.2.3`（`versionCode=28`） **文档日期**：2026-10-21 **发布状态：已发布**

所有通过结果必须来自本版新鲜执行，并绑定源码 revision、构建、设备和原始报告。未知、未执行和失败不能勾选；APK 文件和设备安装包须互相对应。旧测试、旧截图和旧真机豁免均不延续到本版。

**唯一正式发布渠道：GitHub Release。** 唯一官方资产为 Direct APK `glimmer-countdown-4-2-3.apk`。Play flavor 仅保留用于兼容性与开发回归。

## 一、源码与构建

- [x] 版本为 4.2.3 / 28；本地修复测试 Direct / Play 各 668 项、三项 lint、设备测试编译通过。
- [x] 本地 Python 5/5、publisher 隔离 mock 10/10 通过。
- [x] 最终标签源码 CI：API26/31/36 native、运行时 smoke、双渠道 JVM、lint 及 R8 构建。
- 本轮过程记录见 [4.2.3 修复记录](releases/v4.2.3-development.md)。

## 二、4.2.3 变更要求对照表

| 修复 | 自动回归 | 实体手机反馈 |
|---|---|---|
| 发布刷新与列表回调互相等待 | WidgetCollectionRefreshTest；刷新顺序与锁边界检查 | 用户确认覆盖安装后小组件显示正常 |
| 新增小组件保存等待 | 配置保存与同步回调回归 | 用户确认新增保存正常 |

## 三、正式签名

- [x] 用户测试包已验签，包名、4.2.3 / 28、非 debug、16KiB 对齐通过。
- [x] 从最终标签新鲜构建正式 APK 并独立验签。
- 正式证书 SHA-256：`3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`。

## 四、正式 APK 升级与运行

- [x] 用户反馈：vivo X Fold3 Pro / Android 16 / OriginOS 6，覆盖安装测试通过，小组件显示及新增保存正常。用户测试包 SHA-256：`6fa54d07012ef8a5d8430190210447c35dc526173b8550f3300dec48b80d52df`。
- [x] 最终签名包 API26/31/36 从公开 4.2.2 / 27 覆盖升级、事件与 UID/首次安装时间保留、AMS 草稿恢复。
- [ ] 折叠屏切换、其他手机、完整日期/时区桌面矩阵及系统图片备份恢复：未执行。

## 五、发布凭据与说明

- [x] 用户确认手机测试通过并明确授权发布 4.2.3。
- [x] 正式候选 QA 通过后已运行现有 publisher；凭据仅在子进程环境中使用。
- [x] 说明区分用户手机反馈、自动化验证和未执行项目。

## 六、发布动作

- [x] 干净独立工作区，HEAD、本地标签及远端标签解引用一致。
- [x] Release 仅含 `glimmer-countdown-4-2-3.apk`，公开正式版本，非 prerelease。
- [x] 公开匿名下载核对哈希、大小、签名与包身份。
- [x] 公开包 API26/31/36 QA 和 API36 更新检查。
- [x] 核实清理本轮 owned QA draft 与发布锁，补记发布证据，不移动标签。

## 本次发布证据

最终标签 `v4.2.3` 对应源码 `bb159e277d1b550a384ed343236cc85181a133a9`；[标签 CI](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/39536372310)、[正式候选升级 QA](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/39536669661)、[公开 APK QA](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/39538592041) 全部通过。最终正式 APK 为 12,521,441 bytes，SHA-256 `0b9bfdb6258dfddd60c2e1453401b9bf7b52742de8cbd6d999c94c91dcf8c01b`。

最终标签 CI：Direct / Play 各 668 项单元测试通过；API26 原生报告 67 项、1 项因系统版本跳过，API31 / API36 各 71 项，均零失败、零错误。三个系统的新增锁等待回归均通过。三份云端 lint 各 1 条 OldTargetApi 警告、0 错误；保留 targetSdk 36。

公开匿名下载已核对哈希、大小、正式签名、包身份及 16KiB 对齐；公开包 API26/31/36 覆盖升级与草稿恢复通过，API36 应用内检查更新确认已是最新版本。

用户测试包与最终标签包的 DEX、Manifest 和二进制界面资源逐项相同；APK 差异仅为构建来源元数据和两份字体许可文本换行。用户反馈的真机范围为 vivo X Fold3 Pro / Android 16 / OriginOS 6 覆盖安装、小组件显示与新增保存，未扩大至折叠切换或其他设备。

## 历史记录（非发布门）

[4.2.2 历史清单](releases/v4.2.2-checklist.md)、[4.2.1 历史清单](releases/v4.2.1-checklist.md)、[4.2 历史清单](releases/v4.2-checklist.md)、[4.1 历史清单](releases/v4.1-checklist.md)与[4.0 历史清单](releases/v4.0-checklist.md)仅保留各版证据。
