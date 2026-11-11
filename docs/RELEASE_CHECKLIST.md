# 发布检查清单（v4.3.1）

**版本**：`4.3.1`（`versionCode=31`） **文档日期**：2026-11-11 **发布状态：已发布**

所有通过结果必须来自本版新鲜执行，并绑定源码 revision、构建、设备和原始报告。未知、未执行和失败不能勾选；APK 文件和设备安装包须互相对应。旧测试、旧截图和旧真机豁免均不延续到本版。

**唯一正式发布渠道：GitHub Release。** 唯一官方资产为 Direct APK `glimmer-countdown-4-3-1.apk`。Play flavor 仅保留用于兼容性与开发回归。

## 一、源码与构建

- [x] 版本更新为 4.3.1 / 31；升级验证脚本固定公开 4.3.0 / 30 的正式 APK 哈希与大小。
- [x] Direct / Play 单元测试通过（各 678 项），零失败、零错误、零跳过；两渠道设备测试编译通过。
- [x] Direct Debug、Direct Release、Play Release lint 零错误；release vital 分析通过。
- [x] Python 测试与 publisher 隔离 mock 全通过（CI `42984100115` 对应步骤），未访问真实发布 API。
- [x] 最终标签对应提交的 CI：API26/31/36 native、运行时 smoke、双渠道 JVM、lint 及 R8 构建（run `42984100115`，三条矩阵全部 success）。
- [x] 本轮过程记录写入 `docs/releases/v4.3.1-development.md`；本地候选与最终标签构建分开记录。

## 二、4.3.1 变更要求对照表

| 变更 | 自动回归 | 真机 / Launcher 验收 |
|---|---|---|
| 图片引用读取失败时重试并终止清理，禁止按空集合删除 | `EventImageRegistryTest.databaseReadFailurePreservesAllCandidates` 与清理路径契约 | 待检查：备份恢复后冷启动观察日志与图片 |
| 孤儿扫描前强制核对数据库引用（注册表丢失也不误删） | 单元测试 + 代码审查 | 待检查：纯恢复设备上已存事件配图仍显示 |
| 启动清理增加缓冲与重试，避开数据库初始化竞争 | 仪器测试与 runtime smoke（CI `42984100115`） | 待检查：恢复后立即启动、反复启动 |
| 版本号/发布脚本/QA 脚本同步到 4.3.1 / 31 | JVM 发布契约测试 + Python 测试 | — |

## 三、正式签名

- [x] 候选包新鲜构建并验签、核对版本、非 debug、16KiB 对齐。
- [x] 最终标签新鲜构建正式 APK 并独立验签：7,531,819 bytes，SHA-256 `430a3b35d06aaca88c3e7fcd142f36d5bc50a4d7983fcf22a210f1704192461d`。
- 正式证书 SHA-256：`3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`。
- [x] 升级标签（v4.3.1）为 annotated、不可移动（tag object `2abb7a68beb05bbcca310aa9543f6c996355edd5` → commit `b54b0d3cc43a3d5b5d615cef8ba2857d8c3e9fa8`），发布后不复用旧产物。

## 四、正式 APK 升级与运行

- [x] API26/31/36 从公开 4.3.0 / 30 覆盖升级：事件、配图、分类、排序、UID/首次安装时间保留（发布后 QA run `42987967595`，三条矩阵 success）。
- [ ] 恢复/升级后已存事件配图仍可显示，清理未删除仍被事件引用的图片。
- [x] AMS 草稿恢复、编辑保存、分享保存与系统图片选择器在 API36 实际执行（CI run `42984100115` 与 QA run `42987967595`）。
- [ ] 物理设备（至少一台）完成：覆盖安装、反复冷启动、备份恢复观察。
- [x] 未执行的设备矩阵保持未勾选，不以模拟器结果表述为真机通过。

## 五、发布凭据与说明

- [x] 发布说明区分本地检查、设备验收与历史结果；4.3.0 发布证据已归档。
- [x] 真机检查未执行；发布负责人于 2026-11-11 明确要求“推进直到完成发布”，真机项如实记录为未执行，不记为通过。
- [x] 发布凭据仅在子进程环境变量中传入，未写入仓库或命令行可见输出。

## 六、发布动作

- [x] 最终代码与发布准备文档提交，工作区干净；不可变 `v4.3.1` 标签的 HEAD（`b54b0d3`）与远端解引用一致。
- [x] 从最终标签 `--no-build-cache --rerun-tasks` 新鲜构建、验签后运行现有 publisher。
- [x] Release 仅含 `glimmer-countdown-4-3-1.apk`（Release `441413576`，asset `734755677`），公开正式版本，非 draft / 非 prerelease。
- [x] 公开匿名下载核对：7,531,819 bytes，digest 与本地一致，证书 `3b7cb426…`，16 KiB 对齐通过。
- [x] 公开包 API26/31/36 QA 与实际“检查更新”通过（run `42987967595`，三条矩阵 success）。
- [x] 发布脚本已清理 owned QA draft 与发布锁；无临时 tag 残留。

## 历史记录（非发布门）

[4.3.0 记录](releases/v4.3.0-development.md)、[4.2.4 历史清单](releases/v4.2.4-development.md)、[4.2.3 历史清单](releases/v4.2.3-checklist.md)、[4.2.2 历史清单](releases/v4.2.2-checklist.md)、[4.2.1 历史清单](releases/v4.2.1-checklist.md)、[4.2 历史清单](releases/v4.2-checklist.md)、[4.1 历史清单](releases/v4.1-checklist.md)与[4.0 历史清单](releases/v4.0-checklist.md)仅保留各版证据。
