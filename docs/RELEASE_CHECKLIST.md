# 发布检查清单（v4.3.4）

**版本** `4.3.4`（`versionCode=34`）。**发布状态：已发布**。发布日期为 2026-11-25（香港时间）。

所有通过结果必须来自本版新鲜执行，并绑定源码 revision、构建、设备和原始报告。未知、未执行和失败不能勾选，APK 文件和设备安装包须互相对应。旧测试、旧截图和旧真机豁免均不延续到本版。

**唯一正式发布渠道：GitHub Release。** 唯一官方资产为 Direct APK `glimmer-countdown-4-3-4.apk`。Play flavor 仅保留用于兼容性与开发回归。公开升级基线为 4.3.3 / 33。

## 一、源码与构建

- [x] 双渠道单元测试、设备测试编译、三个完整 lint、两项 release vital 和双渠道 Debug 构建。
- [x] 发布脚本隔离测试和运行检查脚本测试。
- [x] Direct Release 经 R8、资源压缩、正式签名与 16 KiB 对齐检查。
- [x] API 26、31、36 本地设备回归，API 36 Play 更新入口隔离通过。
- 结果和候选 APK 指纹见 [4.3.4 验证记录](releases/v4.3.4-development.md)。

## 二、4.3.4 变更要求对照表

| 范围 | 核验项 |
|---|---|
| 首页和详情 | 四种短眉题及当天印章、100% / 130% 字号、账页日期、分类和日期搜索、长按菜单与拖拽、月历无跳动、权限返回刷新 |
| 启动更新 | Direct 首帧后检查、24 小时节流、72 小时稍后、按版本忽略、下载进度和回前台安装。Play 由应用商店管理 |
| 分享与小组件 | 三套分享纸色、内置字体、农历和备注、单事件预览和独立配置、静态与动态快捷方式 |
| 数据 | Room 6→13 和 12→13、旧提醒、归档和恢复、农历闰月与小月、ZIP 配图及排序恢复 |

## 三、正式签名

- 正式证书 SHA-256 为 `3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`。
- [x] 审核后从不可变标签新鲜构建正式发布包，核对包名、版本、签名、对齐、元数据、大小与 SHA-256。

## 四、正式 APK 升级与运行

- [x] API 26、31、36 从公开 4.3.3 / 33 覆盖升级，验证版本、首次安装时间、UID、旧记录与草稿恢复。
- [x] API 36 正式签名候选通过六个页面、分享图片保存和系统分享回归。
- [ ] 实体设备覆盖安装、Launcher 快捷方式、小组件和系统权限验证。

## 五、用户审核

- [x] 用户确认推送、创建标签并发布 4.3.4。

## 六、发布动作

- [x] 最终源码与仓库 CI、正式 APK QA 绑定到同一版本。[源码 CI](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/45570907797) · [正式包 QA](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/45577771582)
- [x] Release 仅含 `glimmer-countdown-4-3-4.apk`。[GitHub Release](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.3.4)
- [x] 匿名下载 APK 并核对大小、SHA-256、包身份、正式签名和 16 KiB 对齐。
- [x] 公开包在 API 26、31、36 通过覆盖升级与草稿恢复。API 36 应用内检查显示「已是最新版本」。[公开包 QA](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/45580346008)
- [x] 本轮临时 QA 草稿已清理，发布锁已解除，远端标签指向最终提交。

## 历史记录（非发布门）

[4.3.3 验证记录](releases/v4.3.3-development.md)、[4.3.2 检查清单](releases/v4.3.2-checklist.md)、[4.3.1 检查清单](releases/v4.3.1-checklist.md)、[4.1 历史清单](releases/v4.1-checklist.md)、[4.0 历史清单](releases/v4.0-checklist.md)。
