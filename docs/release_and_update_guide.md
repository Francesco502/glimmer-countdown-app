# TimeAPK 发布与更新指引（4.2.1）

当前正式版本为 4.2.1 / versionCode=26，已于 2026-10-15 发布，唯一官方资产为 Direct APK `glimmer-countdown-4-2-1.apk`。**唯一正式发布渠道：GitHub Release。** Play flavor 仅保留用于兼容性与开发回归。实际验证状态见 [发布检查清单](RELEASE_CHECKLIST.md)，上一公开版见 [v4.2 历史清单](releases/v4.2-checklist.md)。

## 一、发布身份与凭据

Direct 包名为 `com.example.timeapk`，minSdk26 / targetSdk36；正式签名须与公开 v4.2 兼容。正式证书 SHA-256：`3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`。

既有 keystore 和配置只在本机使用，不进入 Git、日志或远端 runner。可用 `TIMEAPK_KEYSTORE_PROPERTIES` 指定配置；不要新建证书代替正式升级签名。

本地使用 `gh auth login`，由 publisher 内部通过 `gh auth token` 取得 GitHub Contents: write 凭据。CI 才使用 secret `GITHUB_TOKEN`；不能输出 token。`ANDROID_HOME` 须可找到稳定版 apksigner 和 `aapt`；`GLIMMER_RELEASE_CERT_SHA256` 为正式证书指纹。

## 二、固定顺序

最终代码与发布文档已提交，且工作区干净 → 创建并推送不可变的 exact tag → 从该 tag 对应 commit 的工作树重新正式签名构建 → 验证签名、精确证书指纹与 SHA-256 → 准备安全凭据环境 → 运行发布脚本。

使用最终发布 commit 创建 annotated `v4.2.1` tag；本地与远端 tag 解引用后的 commit 必须一致。禁止移动已推送 tag 或覆盖 published Release。不得复用旧构建产物，使用新构建目录或 `gradlew clean`，不能使用 `git clean`。

## 三、构建与 QA

```bash
./gradlew testDirectDebugUnitTest testPlayDebugUnitTest compileDirectDebugAndroidTestKotlin compilePlayDebugAndroidTestKotlin
./gradlew lintDirectDebug lintDirectRelease lintPlayRelease lintVitalDirectRelease lintVitalPlayRelease
./gradlew assembleDirectRelease
```

最终 tag CI 须新鲜运行 JVM、API36 native、runtime smoke、publisher 隔离 10/10 以及 R8 / shrinkResources。正式签名可采用现有等价路径：

1. 在无密钥 CI 从最终 tag 执行 `packageDirectRelease -x validateReleaseSigning`，保留 revision、原始未签名 APK 哈希与原始 `output-metadata.json`。
2. 下载后核对原始哈希和唯一 directRelease / 4.2.1 / 26 元数据。本机先 zipalign，再以环境密码输入调用原生 apksigner 正式签名；默认签名门不改，未签名文件不得发布。
3. 独立验证正式证书、单一签名者、签名方案、16KiB 对齐、非 debug、包名和 Direct 安装权限；记录正式 APK 大小 / SHA-256。
4. 发布用 metadata 只将唯一 artifact 的 outputFile 改为 exact 名称；放入 `app/build/outputs/apk/direct/release/glimmer-countdown-4-2-1.apk`。
5. 通过临时 owned QA draft 运行 `android-apk-qa.yml`：API26/31/36 从公开 v4.2 / 25 覆盖升级到 4.2.1 / 26，核对首次安装时间、UID、事件标题/备注/日期保留与 AMS 草稿恢复；API36 还检查页面和分享。

升级基线固定为 `glimmer-countdown-4-2.apk`，12,513,338 bytes，SHA-256 `7fe9673e4b3a85f4152c1e5786f8a221779a95fc56578f9c93b660b4e9019e74`。不能用 debug 或临时 QA 证书替代正式候选。

## 四、GitHub Release

工作区存在 tracked / untracked 改动时拒绝发布；HEAD 须精确等于最终 tag。签名与正式包 QA 通过后执行：

```powershell
.\scripts\publish-release.ps1 -Tag v4.2.1 -ReleaseName v4.2.1
```

脚本从 CHANGELOG 的 4.2.1 小节取公开说明，通过 Git ref 锁串行发布；只创建/恢复带自身 ownership marker 的 draft。删除 owned draft 中的所有旧资产，再上传唯一 exact Direct APK。整个 Release 只保留唯一的 exact Direct APK，拒绝任何其他附件夹带；以 asset id、size、digest、下载 URL 和 MIME 绑定校验，并用最终 GET 验证公开非 prerelease 状态。

存在残留锁时先调查活跃发布者和远端 mutation，不随意删除。若超时造成结果未知，先读取权威状态，不盲目重复发布或覆盖已有 Release。

## 五、公开复验与记录

无认证重新下载公开 APK，独立验签、哈希和包身份核对。运行公开包 API26/31/36 QA，API36 实际检查更新、分享保存与返回；清理本轮 owned QA draft 和发布锁，按结果补记 README / 检查清单，不移动最终 tag。

实体手机及其 Launcher、完整主题/字体/旋转/TalkBack、通知送达、系统日历与长期性能没有本轮通过证据。小组件原生视图回归与应用内预览只证明各自范围。

## 六、本版 APK 与 QA 驱动身份

正式 APK 来自不可变 `v4.2.1` → `e532c3333e10148c5771906fd2996adb53735104` 的成功标签 CI [38735339024](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38735339024)。签名包 QA 使用驱动提交 `d3c5ff9b1f196f4598c27816d61414eb05fe58ce`：仅将升级夹具固定标签缩短，仍保留12位随机标识、逐字符核对、全部字段/安装身份断言与原超时；应用源码和发布标签未改变。

旧驱动 API36 的两次4分钟进程超时保留为失败；本地原驱动完整升级与恢复通过。新驱动三平台正式包 QA [38740578879](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38740578879) 全部通过。公开包复验和原始证据见当前发布清单。API36 模拟器 Pixel Launcher 两种小组件的真实绑定、日夜双向切换已核验；这不代表实体手机验收。

公开包 QA [38742224120](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38742224120) 三平台全部成功，API36 实际显示“已是最新版本”。公开匿名下载、验签、资产哈希与身份一致；本轮 owned QA draft 和发布锁已独立确认清理。详见 [结构化记录](releases/v4.2.1-verification.json) 与 [原图记录](screenshots/4.2.1/README.md)。
