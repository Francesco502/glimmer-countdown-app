# 发布检查清单（v4.2.1）

**版本**：`4.2.1`（`versionCode=26`）  **文档日期**：2026-10-15  **发布状态：候选 / 未发布**

本版修复桌面小组件夜间模式的自动文字颜色。所有通过结果必须来自本版新鲜执行，并绑定源码 revision、构建、设备和原始报告；未知、未执行和失败不能勾选；APK 文件和设备安装包须互相对应。旧测试、旧截图和旧真机豁免均不延续到本版。

**唯一正式发布渠道：GitHub Release。** 唯一官方资产为 Direct APK `glimmer-countdown-4-2-1.apk`。Play flavor 仅保留用于兼容性与开发回归。

## 当前阶段与证据身份

| 项目 | 当前状态 |
|---|---|
| 源码 revision | 待最终发布 commit 与不可变 `v4.2.1` tag；本地与远端 tag 解引用后必须一致 |
| 自动测试与 lint | 待 4.2.1 新鲜执行；此前修复阶段结果不替代本版 tag CI |
| 正式 APK | 待最终 tag 新鲜 R8 构建与正式签名；`com.example.timeapk` / 4.2.1 / 26，min26 / target36，非 debug |
| 正式签名 | 沿用正式证书 SHA-256 `3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`；最终 APK 须独立验签 |
| 升级基线 | 公开 v4.2：12,513,338 bytes，SHA-256 `7fe9673e4b3a85f4152c1e5786f8a221779a95fc56578f9c93b660b4e9019e74`；已实时核对公开资产 |
| 正式包模拟器 QA | 待 API26/31/36 的 v4.2→v4.2.1 保留数据覆盖升级、AMS 恢复；API36 页面、分享与公开更新检查 |
| GitHub Release | 待正式包 QA 通过后用 publisher 发布；不覆盖 v4.2 |
| 物理手机与 Launcher | 本轮未执行实体手机桌面验收；原生视图回归与应用内预览不等同于真实 Launcher 绑定 |

## 一、自动质量门

- [x] 版本号、Gradle fallback 与 exact APK 命名同步为 4.2.1 / 26
- [x] CHANGELOG 使用发布日期 2026-10-15，公开说明仅包含用户可见修复
- [x] v4.2 发布清单原样归档，旧截图保留原版本标识
- [x] `git diff --check`、Python AST 与工作流 shell 脚本语法检查
- [ ] Direct / Play JVM 完整测试、AndroidTest 编译
- [ ] `lintDirectDebug`、`lintDirectRelease`、`lintPlayRelease`、`lintVitalDirectRelease`、`lintVitalPlayRelease`
- [x] 本地 publisher 状态机 10/10；最终 tag CI 仍须重新执行
- [ ] 最终 tag CI：新鲜 JVM / native / lint / R8 / runtime smoke

## 二、4.2.1 变更要求对照表

| 要求 | 实现与必须验证的场景 | 本版结果 |
|---|---|---|
| 自动模式恢复 | 每次刷新写入文字颜色；复用视图时从手动浅字/深字切回自动 | 待最终 tag native 回归 |
| 日夜切换 | Android12+ 在 host apply/reapply 时解析资源色或日夜颜色对；API26-30 在刷新时重新取色 | 待本版新鲜回归 |
| 同一配色规则 | 列表行、空状态、单事件小组件共用颜色应用逻辑；系统 / 宣纸 / 青瓷 / 透明 / 磨砂背景 | 待最终 tag native 与源契约回归 |
| 手动文字颜色 | 强制浅字或深字不受 host 日夜模式影响 | 待最终 tag native 回归 |

## 三、数据与核心功能

- [ ] 正式签名包在 API26/31/36 由公开 v4.2 原地升级；首次安装时间、UID、标题、备注及日期保留
- [ ] 正式包 AMS 后台终止后草稿恢复并保存
- [ ] API36 五页、分享保存、分享选择器返回；公开版本实际检查更新

数据库及配置保存格式未修改。事件、偏好和多实例配置的自动测试不替代实体手机验收。

## 四、UI、无障碍、小组件与性能

- [ ] API36 全部原生回归（含新增 4 项文字颜色测试）
- [ ] API26 兼容路径与 API31/36 原生日夜颜色路径
- [ ] 本版正式包新鲜截图，逐项标注实际来源和范围
- [ ] 实体手机安装/升级、真实 Launcher 绑定与日夜切换：未执行
- [ ] 物理通知送达、系统日历、完整字体/主题/旋转/TalkBack 与长期性能：未执行

## 五、本版证据记录

修复阶段的旧版本工作区验证保留在本地 `.tmp/widget-theme-qa/`：原实现 3 项失败；修复后 API36 小组件 9 项通过，API26 8 项通过且 1 项 Android12 专用检查不适用；Direct/Play JVM 各638项、Debug/Release lint无错误。这些结果绑定改版前 4.2 元数据，不能当作最终 4.2.1 工件的通过证明。

最终 tag CI、未签名原始 metadata / 哈希、正式签名 receipt、三平台 QA、公开下载复验将在完成后按实际结果补记。

## 六、发布动作

用户已于 2026-10-15 授权更新版本、完成现有验证流程并发布 GitHub Release。

- [ ] 最终代码与发布文档已提交，且发布前工作区干净
- [ ] 创建并推送不可变的 exact `v4.2.1` tag
- [ ] 从该 tag commit 新鲜构建并正式签名，记录 SHA-256 / 大小 / 身份 / 权限与正式证书
- [ ] 正式包 API26/31/36 升级与恢复 QA 通过；保留失败证据
- [ ] 运行发布脚本；验证 ownership marker、Git ref 锁与唯一资产
- [ ] 最终 GET 验证公开非 prerelease，Release 仅含 `glimmer-countdown-4-2-1.apk`
- [ ] 公开无认证下载独立验签、哈希与包身份核对；公开包三平台复验
- [ ] 清理发布锁和本轮 owned QA draft；据实际结果更新文档，不移动 tag

## 历史记录（非发布门）

[v4.2 原始清单](releases/v4.2-checklist.md)、[v4.1 原始清单](releases/v4.1-checklist.md) 与 [v4.0 原始清单](releases/v4.0-checklist.md) 保留各自的源码、APK、验证范围和历史决定。v4.2 唯一资产为 `glimmer-countdown-4-2.apk`；其测试及图片不证明本版通过。
