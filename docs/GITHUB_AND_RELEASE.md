# GitHub 提交与发布流程（v4.2.2）

本文记录 4.2.2 / versionCode=27 的发布流程。已完成正式发布，实际结果见 [发布检查清单](RELEASE_CHECKLIST.md)；[v4.2 历史清单](releases/v4.2-checklist.md) 保留旧版已公开证据。

**唯一正式发布渠道：GitHub Release。** 唯一官方资产为 Direct APK `glimmer-countdown-4-2-2.apk`。Play flavor 仅保留用于兼容性与开发回归。

## 1. 提交、标签与新鲜构建

固定顺序：最终代码与发布文档已提交，且工作区干净 → 创建并推送不可变的 exact tag → 从该 tag 对应 commit 的工作树重新正式签名构建 → 验证签名、精确证书指纹与 SHA-256 → 准备安全凭据环境 → 运行发布脚本。

```bash
git status --short
git add <已核对的本版文件>
git commit -m "release: fix birthday widget display in v4.2.2"
git push origin main
git tag -a v4.2.2 -m "Release v4.2.2"
git push origin v4.2.2
```

标签只能创建于最终发布 commit；核对本地与远端 tag 解引用后的 commit 完全一致。不得移动已推送 tag、覆盖已发布 Release 或复用旧版 APK。不得复用旧构建产物；允许 `gradlew clean` 清理构建输出，禁止用 `git clean` 删除未跟踪内容。

## 2. 验证与正式签名

```bash
./gradlew testDirectDebugUnitTest testPlayDebugUnitTest compileDirectDebugAndroidTestKotlin compilePlayDebugAndroidTestKotlin
./gradlew lintDirectDebug lintDirectRelease lintPlayRelease lintVitalDirectRelease lintVitalPlayRelease
./gradlew assembleDirectRelease
```

最终 tag CI 还须通过 API26/31/36 全套 native、runtime smoke、publisher 隔离状态机 10/10，以及 R8 / 资源压缩构建。正式包须以公开 v4.2.1 / 26 为基线，在 API26/31/36 验证保留数据覆盖升级与 AMS 草稿恢复。

密钥只留本机时，沿用 CI 未签名构建、本机签名路径：从最终 tag 在 CI 执行 `packageDirectRelease -x validateReleaseSigning`，保留 source-revision、原始 SHA-256 与 `output-metadata.json`；本机先 zipalign，再用正式密钥与原生 apksigner 签名。密码只通过子进程环境输入，密钥、密码及 token 不进入日志。默认 Gradle 签名门保持；未签名工件不得发布。

正式证书 SHA-256 为 `3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`。签名后用 `aapt` 核对包名 `com.example.timeapk`、4.2.2 / 27、非 debug、Direct 安装权限；zipalign 16KiB 检查通过。发布用 metadata 仅将唯一 artifact 的 outputFile 改为 `glimmer-countdown-4-2-2.apk`。

本地也可用只读挂载、无网络的容器隔离运行 publisher 状态机；测试使用假 APK 与内存 GitHub API，不读取正式签名或凭据：

```bash
docker run --rm --network none --platform linux/amd64 \
  -v "$PWD:/workspace:ro" -w /workspace \
  mcr.microsoft.com/powershell:7.5-ubuntu-24.04 \
  pwsh -NoProfile -File scripts/tests/publish-release-mock-harness.ps1 -Scenario all
```

## 3. 通过正式包 QA 后发布

正式签名候选可通过本轮临时 owned QA draft 提供给 `android-apk-qa.yml`；它不创建正式 tag，不替代 publisher，也不直接公开。API26/31/36 QA 成功后才运行正式 publisher。

本地先用 `gh auth login` 登录，publisher 内部通过 `gh auth token` 读取 GitHub Contents: write 凭据；也可使用 Git Credential Manager 登录后仅在发布子进程环境提供凭据。CI 使用仓库 secret `GITHUB_TOKEN`。`ANDROID_HOME` 定位稳定版 apksigner / aapt，`GLIMMER_RELEASE_CERT_SHA256` 设置为正式证书指纹，不能打印 token。

```powershell
.\scripts\publish-release.ps1 -Tag v4.2.2 -ReleaseName v4.2.2
```

publisher 的门与现有流程一致：

- 任何 GitHub 请求前拒绝脏工作区，要求 HEAD 精确位于本地 tag，首次远端写入前再次复核。
- 创建 Git ref 锁 `refs/heads/release-locks/v4.2.2`；只新建或恢复带自身 ownership marker 的 owned draft，拒绝 published Release 与人工 draft。
- 删除 owned draft 中的所有旧资产，只上传 exact Direct APK，按 asset id、size、digest、下载 URL 和 content type 绑定核对。
- 整个 Release 只保留唯一的 exact Direct APK，拒绝任何其他附件夹带。
- 发布前复核 draft 身份，发布后以最终 GET 验证公开非 prerelease 的 Release 和唯一 APK。

残留锁须先调查发布进程和远端对象；不能随意删除活跃锁。publisher 在 finally 校验 ownership 后清理锁，失败保留原始证据。

## 4. 发布后复验

公开无认证下载唯一 APK，独立核对 SHA-256、大小、正式签名、真实包身份与权限；再次执行三平台正式包 QA，API36 实际检查更新显示已是最新版本。最后清理本轮 owned QA draft，补记清单并提交发布后文档，不移动最终 tag。

小组件保留“预览宽度 / 预览高度”和无可写系统日历等原有行为。真实 Launcher 旧绑定迁移、完整日期/时区桌面矩阵、系统图片备份恢复及实体手机仍未执行，不得用组件测试或普通事件升级代替，公开说明须明确保留这些限制。
