# 拾光（Glimmer）4.2

拾光是一款面向 Android 的倒数日、生日与纪念日应用。它把日子整理成安静的纸笺、月历和桌面小组件，让重要时刻能被看见，也能被系统提醒与日历同步照顾到。

`4.2` 已发布（`versionCode=25`，2026-10-12）。发布门、实际验证与剩余限制见 [4.2 发布检查清单](docs/RELEASE_CHECKLIST.md)；设计与实施记录见 [4.2 设计文档](docs/superpowers/specs/2026-10-12-v4-2-experience-polish-design.md)。v4.1 与 v4.0 的发布证据保存在 [v4.1 历史清单](docs/releases/v4.1-checklist.md) 与 [v4.0 历史清单](docs/releases/v4.0-checklist.md)，不作为 4.2 的通过记录或豁免。

**唯一正式发布渠道：GitHub Release。** 4.2 的唯一官方资产为 Direct APK `glimmer-countdown-4-2.apk`，复用 v4.0 / v4.1 正式签名，可覆盖升级。Play flavor 仅保留用于兼容性与开发回归，不是正式发布工件或阻断项。

最新公开版本为 `4.2`：[下载 v4.2 APK](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.2)

## 4.2 模拟器界面

以下截图来自线上正式 APK，按同一最终候选与新鲜构建核验，绑定不可变 `v4.2` 标签。环境为 API36、1080×1920、字体1.0；事件为 QA 夹具，小组件截图是应用内预览。原图来源、哈希与验证范围见[截图记录](docs/screenshots/4.2/README.md)。

| 首页 | 月历 |
|---|---|
| <img src="docs/screenshots/4.2/home.png" width="260" alt="拾光4.2正式APK首页"> | <img src="docs/screenshots/4.2/calendar.png" width="260" alt="拾光4.2正式APK月历"> |

| 详情 | 设置 |
|---|---|
| <img src="docs/screenshots/4.2/detail.png" width="260" alt="拾光4.2正式APK事件详情"> | <img src="docs/screenshots/4.2/settings.png" width="260" alt="拾光4.2正式APK设置"> |

| 应用内小组件预览 | 分享预览 |
|---|---|
| <img src="docs/screenshots/4.2/widget-preview.png" width="260" alt="拾光4.2应用内小组件预览"> | <img src="docs/screenshots/4.2/share-preview.png" width="260" alt="拾光4.2实际分享图片预览"> |

## 历史界面预览（v4.1）

以下 `docs/screenshots/4.1` 图片是 v4.1 历史截图，来自线上 v4.1 正式 APK，按当时同一最终候选与新鲜构建核验，绑定不可变 `v4.1` 标签；本轮不将这些截图视为 4.2 发布证据，4.2 截图见上一节。环境为 API36、411dp、字体1.0；事件为 QA 夹具，小组件截图是应用内预览。原图来源、哈希与验证范围见[截图记录](docs/screenshots/4.1/README.md)。

| 首页 | 月历 |
|---|---|
| <img src="docs/screenshots/4.1/home.png" width="260" alt="拾光4.1正式APK首页"> | <img src="docs/screenshots/4.1/calendar.png" width="260" alt="拾光4.1正式APK月历"> |

| 详情 | 设置 |
|---|---|
| <img src="docs/screenshots/4.1/detail.png" width="260" alt="拾光4.1正式APK事件详情"> | <img src="docs/screenshots/4.1/settings.png" width="260" alt="拾光4.1正式APK设置"> |

| 应用内小组件预览 | 分享预览 |
|---|---|
| <img src="docs/screenshots/4.1/widget-preview.png" width="260" alt="拾光4.1应用内小组件预览"> | <img src="docs/screenshots/4.1/share-preview.png" width="260" alt="拾光4.1实际分享图片预览"> |

## 历史界面预览（v4.0）

以下 `docs/screenshots/4.0` 图片是 v4.0 历史截图，本轮不将这些截图视为发布证据，也不把历史图片标为 4.2 界面。

| 首页纸笺 | 月历视图 |
|---|---|
| <img src="docs/screenshots/4.0/home-card.png" width="260" alt="拾光 4.0 首页纸笺视图"> | <img src="docs/screenshots/4.0/month-calendar.png" width="260" alt="拾光 4.0 月历视图"> |

| 设置入口 | 小组件设置 |
|---|---|
| <img src="docs/screenshots/4.0/settings.png" width="260" alt="拾光 4.0 设置页"> | <img src="docs/screenshots/4.0/widget-settings.png" width="260" alt="拾光 4.0 小组件设置页"> |

## 4.2 体验更新

- 自定义分类（新建、改名、改色、删除后事件归“其他”，随 JSON 导出导入）与事件配图（系统照片选择器，私有存储，不随 JSON 导出）。
- 单事件大数字桌面小组件；列表小组件预览与桌面一致，设置分“外观 / 内容”两页，保留 1-5 格“预览宽度 / 预览高度”，覆盖已有配置前先确认；深色模式下磨砂玻璃转为深色。
- 首页“最近”横幅（默认开启）、放大的倒数数字、朱印“今天”、置顶标记、顶栏搜索与长按菜单；月历显示农历日、节日与节气，区分今天与选中日，可左右滑动换月。
- 新建页日期紧随标题，提供快捷提醒与常用重复；设置首页每行显示当前值；功能性文案改为直白表述，分享卡与空状态保留宋韵措辞。
- 宽屏（≥840dp）列表与详情双栏、Android 12+ 跟随壁纸取色、Direct 渠道可选精确提醒、更新下载进度与安装权限引导。
- 内置字体裁剪为子集并按 SIL OFL 1.1 改名为 Glimmer Serif / Glimmer Xiaowei，字体资源约 31MB → 7.8MB。

完整变更见 [CHANGELOG.md](CHANGELOG.md)。本版按评审决定不安排实体手机验收；通知实际到达、精确提醒、物理设备日历、Launcher 小组件、相册选图与长期性能的物理验收记录为未执行。

## v4.1 修复与优化（历史）

- 修复普通事件产生虚假日历权限错误的清理路径，同时保留真实受管日历记录的删除保护。
- 完善编辑草稿恢复、保存任务生命周期、连续输入、导入防重复执行和更新下载完整性。
- 接入 24 小时内小时显示，统一详情页的时间与偏好，明确固定天数纪念节点和周年的区别。
- 改善筛选条件反馈、返回关闭工具面板、排序无障碍、短窗口月历和分享图片的一致性。

## 核心能力

- 记录倒数日、生日、纪念日和普通事件，可自定义分类并添加配图。
- 同时支持公历与农历日期。
- 支持按天、周、月、半年、年重复。
- 支持“提前 N 天 + 固定时间”的提醒配置。
- 支持系统通知（Direct 渠道可选精确提醒）、系统日历同步、权限处理和同步状态反馈。
- 首页提供卡片、列表、月历三种浏览方式，支持搜索、筛选、置顶和自定义排序。
- 桌面列表小组件与单事件大数字小组件；列表小组件支持透明 / 半透明 / 宣纸 / 青瓷 / 朱印等视觉方案，并可配置内容范围、排序、密度、边框、圆角和文字模式。
- 支持 JSON 导入 / 导出，并可导入“记得日子” `.mdb` 备份文件。

## 版本信息

- `versionName`: `4.2`
- `versionCode`: `25`
- 发布状态：已发布（2026-10-12）
- Direct APK 目标文件名：`glimmer-countdown-4-2.apk`

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

- `app/build/outputs/apk/direct/release/glimmer-countdown-4-2.apk`

## 发布与验证

4.2 正式发布要求包含，实际结果以当前清单为准：

- `testDirectDebugUnitTest`
- `compileDirectDebugAndroidTestKotlin`
- `lintDirectDebug lintDirectRelease lintVitalDirectRelease`
- `assembleDirectRelease`，或按[发布指引](docs/release_and_update_guide.md#三构建命令)从最终 tag 的 CI 未签名 Release 工件在本机完成正式签名
- Direct release APK 正式证书、精确证书指纹与 SHA-256 验证
- Direct release APK 的模拟器安装 / v4.1→v4.2 升级、性能与更新 smoke；物理手机安装、提醒、日历与 Launcher 验收按评审决定不安排，记录为未执行
- GitHub Release 只保留 `glimmer-countdown-4-2.apk`，并完成公开下载复验、线上重装、更新检查与关键链路 smoke

2026-10-12 [最终标签 CI](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38235651872)：Direct/Play JVM各638/638、原生51/51、publisher10/10、lint无error，R8新鲜构建通过。[最终正式签名QA](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38238275647) 在API26/31/36全部通过v4.1→v4.2覆盖升级、精确数据保留与进程恢复；API36五页、真实相册保存和分享选择器返回通过。正式APK为12,513,338 bytes，SHA-256 `7fe9673e4b3a85f4152c1e5786f8a221779a95fc56578f9c93b660b4e9019e74`，公开下载独立验签通过。[线上安装与更新复验](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38239618078) 三平台全部成功，API36实际更新检查显示已是最新版本；原始范围见[清单](docs/RELEASE_CHECKLIST.md)。APK源码与QA工具均为不可变标签 `f0937448efa02232d5c6b1d47c531d50cad0797e`。

publisher 会拒绝脏工作区或未指向 exact tag 的 `HEAD`，并核对输出元数据与 APK 的真实包名、版本、权限和非调试状态。发布流程禁止移动已推送的 tag 或覆盖已发布 Release，GitHub Release 仅上传 exact Direct APK。

正式发布必须在代码与文档提交且工作区干净后推送不可变 tag，再从该 tag commit 新鲜构建和验证签名、证书指纹、SHA-256；不得复用旧产物。publisher 会删除 owned draft 中的所有旧资产，并要求整个 Release 只保留唯一的 exact Direct APK。本地认证使用 `gh auth login` / 脚本内部 `gh auth token`，CI 才从 secret 注入 `GITHUB_TOKEN`，且不得打印凭据。现有本地认证状态不作为结论；最终发布时按此流程重新取得并验证有效的写入权限。

更多发布记录：

- [CHANGELOG.md](CHANGELOG.md)
- [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md)
- [docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)
- [docs/release_and_update_guide.md](docs/release_and_update_guide.md)
