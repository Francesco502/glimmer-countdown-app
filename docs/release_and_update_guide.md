# TimeAPK 发布与更新指引（4.3.4）

4.3.4 / versionCode=34 已于 2026-11-25 发布，唯一官方资产为 Direct APK `glimmer-countdown-4-3-4.apk`，升级基线为公开 4.3.3 / 33。**唯一正式发布渠道：GitHub Release。** Play flavor 仅保留用于兼容性与开发回归。[发布检查清单](RELEASE_CHECKLIST.md)

## 一、发布身份与凭据

Direct 包名为 `com.example.timeapk`，minSdk 26，targetSdk 36。正式签名须与公开 v4.3.3 兼容。正式证书 SHA-256 为 `3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`。

既有 keystore 和配置只在本机使用，不进入 Git、日志或远端 runner。可用 `TIMEAPK_KEYSTORE_PROPERTIES` 指定配置；不要新建证书代替正式升级签名。

本地使用 `gh auth login`，由 publisher 内部通过 `gh auth token` 取得 GitHub Contents: write 凭据。也可经 Git Credential Manager 登录，在发布子进程环境提供凭据。CI 使用 secret `GITHUB_TOKEN`；不能输出 token。`ANDROID_HOME` 须可找到稳定版 apksigner 和 `aapt`；`GLIMMER_RELEASE_CERT_SHA256` 为正式证书指纹。

## 二、固定顺序

最终代码与发布文档已提交，且工作区干净 → 创建并推送不可变的 exact tag → 从该 tag 对应 commit 的工作树重新正式签名构建 → 验证签名、精确证书指纹与 SHA-256 → 准备安全凭据环境 → 运行发布脚本。

使用最终发布 commit 创建 annotated `v4.3.4` tag。本地与远端 tag 解引用后的 commit 必须一致。禁止移动已推送 tag 或覆盖 published Release。不得复用旧构建产物，使用新构建目录或 `gradlew clean`，不能使用 `git clean`。

## 三、构建与 QA

```bash
./gradlew testDirectDebugUnitTest testPlayDebugUnitTest compileDirectDebugAndroidTestKotlin compilePlayDebugAndroidTestKotlin
./gradlew lintDirectDebug lintDirectRelease lintPlayRelease lintVitalDirectRelease lintVitalPlayRelease
./gradlew assembleDirectRelease
```

最终 tag 对应提交的 CI 须通过 JVM、API26/31/36 native、runtime smoke、publisher 隔离 10/10 以及 R8 / shrinkResources。main 的 CI 通过后，将标签固定到同一提交，再从干净工作树构建正式 APK。也可以下载同一提交的未签名 APK，在本机验包并签名。

1. 在无密钥 CI 从最终 tag 执行 `packageDirectRelease -x validateReleaseSigning`，保留 revision、原始未签名 APK 哈希与原始 `output-metadata.json`。
2. 下载后核对原始哈希和唯一 directRelease / 4.3.4 / 34 元数据。本机先 zipalign，再由 apksigner 使用环境变量中的密码完成正式签名。
3. 独立验证正式证书、单一签名者、签名方案、16KiB 对齐、非 debug、包名和 Direct 安装权限；记录正式 APK 大小 / SHA-256。
4. 将发布元数据中唯一工件的 outputFile 改为正式文件名，与 `app/build/outputs/apk/direct/release/glimmer-countdown-4-3-4.apk` 一起准备。
5. 通过临时 owned QA draft 运行 `android-apk-qa.yml`。API 26、31、36 从公开 4.3.3 / 33 覆盖升级到 4.3.4 / 34，核对首次安装时间、UID、事件标题、备注和日期，并检查进程终止后的草稿恢复。API 36 还检查页面和分享。

升级基线固定为 `glimmer-countdown-4-3-3.apk`，7,531,711 bytes，SHA-256 `2681b4e8d34e5a873eb3a4580b853ff68ab082e5fad5c84eff1c25a35568e820`。

## 四、GitHub Release

工作区存在 tracked / untracked 改动时拒绝发布；HEAD 须精确等于最终 tag。签名与正式包 QA 通过后执行：

```powershell
.\scripts\publish-release.ps1 -Tag v4.3.4 -ReleaseName v4.3.4
```

脚本从 CHANGELOG 的 4.3.4 小节取公开说明，通过 Git ref 锁串行发布。删除 owned draft 中的所有旧资产，再上传唯一 exact Direct APK。整个 Release 只保留唯一的 exact Direct APK。脚本以 asset id、size、digest、下载 URL 和 MIME 绑定校验，并用最终 GET 验证正式公开状态。

存在残留锁时先调查活跃发布者和远端 mutation，不随意删除。若超时造成结果未知，先读取权威状态，不盲目重复发布或覆盖已有 Release。

## 五、公开复验与记录

无认证重新下载公开 APK，独立验签、哈希和包身份核对。运行公开包 API26/31/36 QA，API36 实际检查更新、分享保存与返回；清理本轮 owned QA draft 和发布锁，按结果补记 README / 检查清单，不移动最终 tag。

## 六、证据与未执行项

本版结果见 [4.3.4 验证记录](releases/v4.3.4-development.md)。实体设备的覆盖安装、桌面呈现、小组件绑定和系统备份恢复按实际执行情况记录。
