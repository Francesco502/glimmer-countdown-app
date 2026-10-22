# 发布检查清单（v4.2.4）

**版本**：`4.2.4`（`versionCode=29`） **文档日期**：2026-10-22 **发布状态：已发布**

所有通过结果必须来自本版新鲜执行，并绑定源码 revision、构建、设备和原始报告。未知、未执行和失败不能勾选；APK 文件和设备安装包须互相对应。旧测试、旧截图和旧真机豁免均不延续到本版。

**唯一正式发布渠道：GitHub Release。** 唯一官方资产为 Direct APK `glimmer-countdown-4-2-4.apk`。Play flavor 仅保留用于兼容性与开发回归。

## 一、源码与构建

- [x] 版本更新为 4.2.4 / 29；升级验证脚本固定公开 4.2.3 / 28 的正式 APK 哈希与大小。
- [x] 本地 Direct / Play 各 673 项单元测试通过，零失败、零错误、零跳过；两渠道设备测试编译通过。
- [x] Direct Debug、Direct Release、Play Release lint 零问题；两项 release vital 分析通过，无问题报告。
- [x] 本版 Python 测试 5/5、publisher 隔离 mock 10/10。
- [x] 最终标签对应提交的 CI：API26/31/36 native、运行时 smoke、双渠道 JVM、lint 及 R8 构建；[运行 39756383558](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/39756383558)。云端 lint 零错误，各报告保留 targetSdk 36 的 `OldTargetApi` 警告。
- 本轮过程记录见 [4.2.4 修复记录](releases/v4.2.4-development.md)。本地工作区候选的检查不等于最终标签 CI 通过。

## 二、4.2.4 变更要求对照表

| 修复 | 自动回归 | 真机 / Launcher 验收（未执行） |
|---|---|---|
| 背景实际不透明度与百分比不符 | 资源合成透明度、0–100 渲染策略与像素测试 | 待检查桌面背景 0 / 1 / 25 / 37 / 50 / 68 / 75 / 99 / 100% |
| 五档选项改为连续滑杆 | 任意整数配置往返、连续 Slider 语义及端点保存 | 待检查拖动、松手更新、重新进入后保留 |
| 半透明样式端点与预览、桌面不一致 | 列表、单事件、预览共用渲染，视图复用回归 | 待检查两类小组件与应用内预览 |
| 默认自适应及朱印手动深色文字 | 默认/缺省值自适应、手动值保留、朱印深浅色回归 | 待检查日夜切换、自动和手动文字颜色 |
| 4.2.3→4.2.4 升级 | smoke 版本与公开基线约束 | 待验证事件、旧配置与桌面绑定保留 |

## 三、正式签名

- [x] 本地 4.2.4 候选包新鲜构建与验签、版本、非 debug、16KiB 对齐检查通过。
- [x] 最终标签新鲜构建正式 APK 并独立验签；49 项任务实际执行，正式 APK 哈希另行记录，本地早期候选未用于发布。
- 正式证书 SHA-256：`3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`。

## 四、正式 APK 升级与运行

- [x] API26/31/36 从公开 4.2.3 / 28 覆盖升级，事件、UID/首次安装时间保留及 AMS 草稿恢复；[正式签名包 QA](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/39760546811)。
- [ ] vivo X Fold3 Pro / Android 16 / OriginOS 6：覆盖安装、已有及新增小组件、透明度、日夜文字颜色。
- [x] 原生新增像素测试与滑杆测试在 API26/31/36 均实际通过，包含 0–100 所有整数、日夜模式、列表 / 单事件 / 预览与设置保存。
- [ ] 折叠屏切换、其他手机、完整日期/时区桌面矩阵及系统图片备份恢复。

## 五、发布凭据与说明

- [x] 发布说明区分本地检查、设备验收与历史结果；4.2.3 发布证据已归档。
- [x] 2026-10-22 用户明确授权自动化全部通过后正式发布；真机检查如实记为未执行，本版不以真机检查作为发布前置条件。
- [x] 最终正式候选 QA 通过后准备有效发布凭据，且仅在子进程环境使用。

## 六、发布动作

- [x] 最终代码与发布准备文档提交，发布 worktree 干净；不可变 v4.2.4 标签的 HEAD、本地与远端解引用均为 `ec3c4885fd5b051bef103e32c233426d95ec4207`。
- [x] 从最终标签新鲜构建、验签并完成正式包 QA 后运行现有 publisher。
- [x] Release 仅含 `glimmer-countdown-4-2-4.apk`，公开正式版本，非 prerelease；见[发布页](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.2.4)。
- [x] 公开匿名下载核对哈希、大小、签名与包身份。
- [x] [公开包 API26/31/36 QA](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/39762032271) 和 API36 实际更新检查通过，显示已是最新版本。
- [x] 核实并删除本轮 owned QA draft，确认临时 tag 与发布锁均不存在；补记证据，不移动标签。

## 构建与发布证据

本地正式签名候选为 12,526,481 bytes，SHA-256 `5045ad44c5c5c4f3426d5f1031413fd68e884748684675940da08db591e091cd`。候选副本与原始报告保存在 `.tmp/v424-candidate/`（本机、未入 Git），APK 为 `glimmer-countdown-4-2-4.apk`。

完整构建使用 `--no-build-cache --rerun-tasks`，151 项任务执行，结果 `BUILD SUCCESSFUL`。源码基于 `082aff936dac3f48f473cacd1f9c7e80e50e52bf` 加本轮工作区修改；源码清单 SHA-256 为 `a0e9493e26ee3a1152d10d82b854ecce17ce92f137d77fdd536723d2cc5f8ba4`。

详见 [4.2.4 修复与验证记录](releases/v4.2.4-development.md)及[机器可读验证记录](releases/v4.2.4-verification.json)。本机无连接设备，模拟器缺 hypervisor driver；后续原生执行已在云端完成。最终标签正式 APK 为 12,526,321 bytes，SHA-256 `5aa396a3028ca1dd75e75d6f847b2017a81520f700d8ea8227135596d7a6ea53`，副本与报告保存在 `.tmp/v424-release/`。Release ID 为 `420573534`，唯一 APK asset ID 为 `665465531`，于 2026-10-22 10:17 UTC / 18:17 香港时间正式公开。尚未执行的检查保持未勾选。

## 历史记录（非发布门）

[4.2.3 历史清单](releases/v4.2.3-checklist.md)、[4.2.2 历史清单](releases/v4.2.2-checklist.md)、[4.2.1 历史清单](releases/v4.2.1-checklist.md)、[4.2 历史清单](releases/v4.2-checklist.md)、[4.1 历史清单](releases/v4.1-checklist.md)与[4.0 历史清单](releases/v4.0-checklist.md)仅保留各版证据。
