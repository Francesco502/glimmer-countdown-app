# TimeAPK 发布与更新指引（4.2.2）

本次候选版本为 4.2.2 / versionCode=27，尚未公开，唯一官方资产为 Direct APK `glimmer-countdown-4-2-2.apk`。**唯一正式发布渠道：GitHub Release。** Play flavor 仅保留用于兼容性与开发回归。实际验证状态见 [发布检查清单](RELEASE_CHECKLIST.md)，上一公开版见 [v4.2.1 历史清单](releases/v4.2.1-checklist.md)。

## 一、发布身份与凭据

Direct 包名为 `com.example.timeapk`，minSdk26 / targetSdk36；正式签名须与公开 v4.2.1 兼容。正式证书 SHA-256：`3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`。

既有 keystore 和配置只在本机使用，不进入 Git、日志或远端 runner。可用 `TIMEAPK_KEYSTORE_PROPERTIES` 指定配置；不要新建证书代替正式升级签名。

本地使用 `gh auth login`，由 publisher 内部通过 `gh auth token` 取得 GitHub Contents: write 凭据。也可经 Git Credential Manager 登录，在发布子进程环境提供凭据。CI 使用 secret `GITHUB_TOKEN`；不能输出 token。`ANDROID_HOME` 须可找到稳定版 apksigner 和 `aapt`；`GLIMMER_RELEASE_CERT_SHA256` 为正式证书指纹。

## 二、固定顺序

最终代码与发布文档已提交，且工作区干净 → 创建并推送不可变的 exact tag → 从该 tag 对应 commit 的工作树重新正式签名构建 → 验证签名、精确证书指纹与 SHA-256 → 准备安全凭据环境 → 运行发布脚本。

使用最终发布 commit 创建 annotated `v4.2.2` tag；本地与远端 tag 解引用后的 commit 必须一致。禁止移动已推送 tag 或覆盖 published Release。不得复用旧构建产物，使用新构建目录或 `gradlew clean`，不能使用 `git clean`。

## 三、构建与 QA

```bash
./gradlew testDirectDebugUnitTest testPlayDebugUnitTest compileDirectDebugAndroidTestKotlin compilePlayDebugAndroidTestKotlin
./gradlew lintDirectDebug lintDirectRelease lintPlayRelease lintVitalDirectRelease lintVitalPlayRelease
./gradlew assembleDirectRelease
```

最终 tag CI 须新鲜运行 JVM、API26/31/36 native、runtime smoke、publisher 隔离 10/10 以及 R8 / shrinkResources。正式签名可采用现有等价路径：

1. 在无密钥 CI 从最终 tag 执行 `packageDirectRelease -x validateReleaseSigning`，保留 revision、原始未签名 APK 哈希与原始 `output-metadata.json`。
2. 下载后核对原始哈希和唯一 directRelease / 4.2.2 / 27 元数据。本机先 zipalign，再以环境密码输入调用原生 apksigner 正式签名；默认签名门不改，未签名文件不得发布。
3. 独立验证正式证书、单一签名者、签名方案、16KiB 对齐、非 debug、包名和 Direct 安装权限；记录正式 APK 大小 / SHA-256。
4. 发布用 metadata 只将唯一 artifact 的 outputFile 改为 exact 名称；放入 `app/build/outputs/apk/direct/release/glimmer-countdown-4-2-2.apk`。
5. 通过临时 owned QA draft 运行 `android-apk-qa.yml`：API26/31/36 从公开 v4.2.1 / 26 覆盖升级到 4.2.2 / 27，核对首次安装时间、UID、事件标题/备注/日期保留与 AMS 草稿恢复；API36 还检查页面和分享。

升级基线固定为 `glimmer-countdown-4-2-1.apk`，12,517,434 bytes，SHA-256 `21563bc13a72be41d0f6cb21e8db136bcafd20bcf7c94a6390fd53e085cd896c`。不能用 debug 或临时 QA 证书替代正式候选。

## 四、GitHub Release

工作区存在 tracked / untracked 改动时拒绝发布；HEAD 须精确等于最终 tag。签名与正式包 QA 通过后执行：

```powershell
.\scripts\publish-release.ps1 -Tag v4.2.2 -ReleaseName v4.2.2
```

脚本从 CHANGELOG 的 4.2.2 小节取公开说明，通过 Git ref 锁串行发布；只创建/恢复带自身 ownership marker 的 draft。删除 owned draft 中的所有旧资产，再上传唯一 exact Direct APK。整个 Release 只保留唯一的 exact Direct APK，拒绝任何其他附件夹带；以 asset id、size、digest、下载 URL 和 MIME 绑定校验，并用最终 GET 验证公开非 prerelease 状态。

存在残留锁时先调查活跃发布者和远端 mutation，不随意删除。若超时造成结果未知，先读取权威状态，不盲目重复发布或覆盖已有 Release。

## 五、公开复验与记录

无认证重新下载公开 APK，独立验签、哈希和包身份核对。运行公开包 API26/31/36 QA，API36 实际检查更新、分享保存与返回；清理本轮 owned QA draft 和发布锁，按结果补记 README / 检查清单，不移动最终 tag。

实体手机及其 Launcher、完整主题/字体/旋转/TalkBack、通知送达、系统日历与长期性能没有本轮通过证据。小组件原生视图回归与应用内预览只证明各自范围。

## 六、证据与未执行项

本版证据以 [4.2.2 验证记录](releases/v4.2.2-verification.json) 和[升级验收](releases/v4.2.2-upgrade-acceptance.md)为准。真实 Launcher 旧绑定迁移及系统图片备份恢复仍未执行，发布说明必须保留该限制。

[4.2.1 历史清单](releases/v4.2.1-checklist.md)保留旧标签、签名包和公开 QA 证据，不作为 4.2.2 通过记录。
