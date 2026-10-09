# 拾光（Glimmer）4.1 开发候选

拾光是一款面向 Android 的倒数日、生日与纪念日应用。它把日子整理成安静的纸笺、月历和桌面小组件，让重要时刻能被看见，也能被系统提醒与日历同步照顾到。

当前工作区为 `4.1` 开发候选（`versionCode=24`，文档日期 2026-10-09），尚未发布。修复范围与待完成的验证门见 [4.1 发布检查清单](docs/RELEASE_CHECKLIST.md)。v4.0 的发布证据保存在 [历史清单](docs/releases/v4.0-checklist.md)，不作为 4.1 的通过记录或豁免。

**唯一正式发布渠道：GitHub Release。** 4.1 的唯一官方资产为 Direct APK `glimmer-countdown-4-1.apk`，须在正式发布门完成后生成并上传。Play flavor 仅保留用于兼容性与开发回归，不是正式发布工件或阻断项。

最新公开版本为 `4.0`：[下载 v4.0 APK](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.0)

## 历史界面预览（v4.0）

以下 `docs/screenshots/4.0` 图片是 v4.0 历史截图，本轮不将这些截图视为发布证据。4.1 各页面新鲜截图尚未完成审阅，候选模拟器启动截图不能替代完整 UI 验收；后续须使用同一最终候选与新鲜构建验证首页、月历、详情、设置和小组件，不能把历史图片标为 4.1 界面。

| 首页纸笺 | 月历视图 |
|---|---|
| <img src="docs/screenshots/4.0/home-card.png" width="260" alt="拾光 4.0 首页纸笺视图"> | <img src="docs/screenshots/4.0/month-calendar.png" width="260" alt="拾光 4.0 月历视图"> |

| 设置入口 | 小组件设置 |
|---|---|
| <img src="docs/screenshots/4.0/settings.png" width="260" alt="拾光 4.0 设置页"> | <img src="docs/screenshots/4.0/widget-settings.png" width="260" alt="拾光 4.0 小组件设置页"> |

## 4.1 候选修复范围

- 修复普通事件产生虚假日历权限错误的清理路径，同时保留真实受管日历记录的删除保护。
- 完善编辑草稿恢复、保存任务生命周期、连续输入、导入防重复执行和更新下载完整性。
- 接入 24 小时内小时显示，统一详情页的时间与偏好，明确固定天数纪念节点和周年的区别。
- 改善筛选条件反馈、返回关闭工具面板、排序无障碍、短窗口月历和分享图片的一致性。
- 保留桌面小组件独立配置与 1-5 格“预览宽度 / 预览高度”，复核配置恢复、保存反馈和 Launcher 实际表现。

以上为开发候选范围，不能据此视为验证通过。用户已明确授权发布并说明无实体手机；物理手机验收未执行，按现有授权记录剩余限制，不继承 v4.0 豁免。既有正式签名配置与 keystore 已取得，配置路径、私钥可用性和证书已核验，证书与线上 v4.0 APK 一致。本机中间候选已验签，但未包含后续源码更改且不是最终 tag 工件；最终 APK、上传和公开下载复验仍待完成。

## 核心能力

- 记录倒数日、生日、纪念日和普通事件。
- 同时支持公历与农历日期。
- 支持按天、周、月、半年、年重复。
- 支持“提前 N 天 + 固定时间”的提醒配置。
- 支持系统通知、系统日历同步、权限处理和同步状态反馈。
- 首页提供卡片、列表、月历三种浏览方式，支持搜索、筛选、置顶和自定义排序。
- 桌面小组件支持透明 / 半透明 / 宣纸 / 青瓷 / 朱印等视觉方案，并可配置内容范围、排序、密度、边框、圆角和文字模式。
- 支持 JSON 导入 / 导出，并可导入“记得日子” `.mdb` 备份文件。

## 版本信息

- `versionName`: `4.1`
- `versionCode`: `24`
- 发布状态：开发候选 / 未发布（2026-10-09）
- Direct APK 目标文件名：`glimmer-countdown-4-1.apk`

## 构建与运行

```bash
# Direct 渠道 Debug
./gradlew installDirectDebug

# Play 渠道 Debug
./gradlew installPlayDebug
```

```bash
# Direct 渠道 Release APK
./gradlew assembleDirectRelease

# Play flavor 开发回归（非正式发布门）
./gradlew testPlayDebugUnitTest assemblePlayDebug
```

默认产物路径：

- `app/build/outputs/apk/direct/release/glimmer-countdown-4-1.apk`

## 发布与验证

4.1 正式发布要求包含，实际结果以当前清单为准：

- `testDirectDebugUnitTest`
- `compileDirectDebugAndroidTestKotlin`
- `lintDirectDebug lintDirectRelease lintVitalDirectRelease`
- `assembleDirectRelease`，或按[发布指引](docs/release_and_update_guide.md#三构建命令)从最终 tag 的 CI 未签名 Release 工件在本机完成正式签名
- Direct release APK 正式证书、精确证书指纹与 SHA-256 验证
- Direct release APK 的模拟器安装 / 升级、性能与更新 smoke；物理手机安装、提醒、日历与 Launcher 验收当前未执行，按现有发布授权记录剩余限制
- GitHub Release 只保留 `glimmer-countdown-4-1.apk`，并完成公开下载复验、线上重装、更新检查与关键链路 smoke

2026-10-09 [CI11](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/37924741446) 实际执行 PR merge `7ef83906e5cda51a00a197b22587d6adf7e1ea87`：Direct/Play JVM 各 575/575，publisher 10/10，三份 lint 各 0 error / 1 个 `OldTargetApi` warning，未签名 R8 构建通过。API36 connected 为 46/49，新增快速输入及页面重建回归通过；三项排序测试仍停在准备阶段。runtime 已实际记录新进程、同一任务草稿恢复与保存，但最终重开备注时字段定位错误，整条流程仍失败。后续须修复定位并独立检查实际菜单触摸；完整页面截图、最终正式 APK 与公开下载仍待完成，详见[清单](docs/RELEASE_CHECKLIST.md)。

publisher 会拒绝脏工作区或未指向 exact tag 的 `HEAD`，并核对输出元数据与 APK 的真实包名、版本、权限和非调试状态。发布流程禁止移动已推送的 tag 或覆盖已发布 Release，GitHub Release 仅上传 exact Direct APK。

正式发布必须在代码与文档提交且工作区干净后推送不可变 tag，再从该 tag commit 新鲜构建和验证签名、证书指纹、SHA-256；不得复用旧产物。publisher 会删除 owned draft 中的所有旧资产，并要求整个 Release 只保留唯一的 exact Direct APK。本地认证使用 `gh auth login` / 脚本内部 `gh auth token`，CI 才从 secret 注入 `GITHUB_TOKEN`，且不得打印凭据。现有本地认证状态不作为结论；最终发布时按此流程重新取得并验证有效的写入权限。

更多发布记录：

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
