# 发布检查清单（v4.3.2）

**版本**：`4.3.2`（`versionCode=32`） **发布日期**：2026-11-17（香港时间） **发布状态：已发布**

所有通过结果必须来自本版新鲜执行，并绑定源码 revision、构建、设备和原始报告。未知、未执行和失败不能勾选，APK 文件和设备安装包须互相对应。旧测试、旧截图和旧真机豁免均不延续到本版。

**唯一正式发布渠道：GitHub Release。** 唯一官方资产为 Direct APK `glimmer-countdown-4-3-2.apk`。Play flavor 仅保留用于兼容性与开发回归。

## 一、源码与构建

- [x] 源码版本为 4.3.2 / 32，签名 QA 已固定公开 4.3.1 / 31 的 APK、哈希与大小。
- [x] Direct / Play 各 678 项单元测试与两套设备测试编译通过。三套 lint 零错误，两套 release vital、6 项 Python 测试和 10 个 publisher 模拟场景通过。
- [x] 最终源码通过 API 26、31、36 仪器测试、运行时测试和 R8 / 资源压缩构建。[Android CI 43905892490](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/43905892490)
- 测试记录见 [4.3.2 验证记录](releases/v4.3.2-development.md)。

## 二、4.3.2 变更要求对照表

| 变更 | 检查 |
|---|---|
| 白文和朱文印、明暗主题、纸纹与墨线 | 宋式组件用例和主题截图 |
| 首页纸笺卡片、倒数可读性、“最近”去重 | 大字体与窄窗用例、排序与无障碍移动用例、卡片截图 |
| 三栏账页、行高与缩进分隔线 | 首页列表和月历详情截图 |
| 历书月历、农历、今天、选中与重复日程 | 月历模型、短窗口和日程用例、月历截图 |
| 毛笔沙漏桌面图标 | 图标安全区和小尺寸测试，实体设备桌面待检查 |

## 三、正式签名

- [x] 从最终不可变标签新鲜构建，校验版本、唯一签名者、非调试状态、16 KiB 对齐和 APK SHA-256。
- 正式证书 SHA-256 为 `3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`。

## 四、正式 APK 升级与运行

- [x] API 26、31、36 从公开 4.3.1 / 31 覆盖升级。已核对首次安装时间、UID、测试事件标题、备注和日期。
- [x] API 26、31、36 在后台进程终止后恢复编辑草稿。API 36 检查六个应用页面、保存分享图和系统分享。[正式包送测 43910164100](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/43910164100)
- [ ] 实体设备覆盖安装、桌面图标、小组件绑定和系统备份恢复。

## 五、发布凭据与说明

- [x] 发布说明记录了版本变更，测试证据和设备范围记入本版验证记录。发布凭据局限于发布进程。

## 六、发布动作

- [x] 最终代码与发布准备文档已提交。正式构建使用干净的独立工作区，本地与远端的 annotated 标签指向 `4f3c88d`。
- [x] 使用已通过正式包 QA 的构建运行 publisher。
- [x] Release 仅含 `glimmer-countdown-4-3-2.apk`，公开、正式。[GitHub Release](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.3.2)
- [x] 匿名下载核对 SHA-256、大小、包身份、正式签名和 16 KiB 对齐。
- [x] 公开包通过 API 26、31、36 的覆盖升级、草稿恢复与运行回归。API 36 应用内检查更新显示“已是最新版本”。[公开包 QA 43912081493](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/43912081493)
- [x] 本轮测试草稿已清理。已确认远端没有临时标签和发布锁。

## 历史记录（非发布门）

[4.3.1 检查清单](releases/v4.3.1-checklist.md)、[4.3.0 验证记录](releases/v4.3.0-development.md)、[4.2.4 验证记录](releases/v4.2.4-development.md)、[4.2 历史清单](releases/v4.2-checklist.md)、[4.1 历史清单](releases/v4.1-checklist.md)、[4.0 历史清单](releases/v4.0-checklist.md)保留各版证据。
