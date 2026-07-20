# GitHub 提交与发布流程（v4.0）

本文档用于 `4.0` 成熟版候选的代码提交、推送、标签与 GitHub Release 操作。4.0 尚未发布，最新公开版本仍为 3.17；发布检查清单未完成前不得创建正式 Release。

## 1. 本地提交

```bash
git status
git add app gradle.properties README.md CHANGELOG.md docs scripts .gitignore
git commit -m "release: ship v4.0"
```

说明：

- 只提交业务代码、版本元数据、脚本、字体 license 和发布文档
- 不提交本地缓存目录，例如 `.gradle-user-home`、`.cursor`、`build`

## 2. 推送代码

```bash
git push -u origin codex/release-4-0-widget-sort
```

如果最终发布分支是 `main`，应先完成合并或按仓库实际策略推送到目标分支。

## 3. 在最终发布 commit 上创建 `v4.0` 标签

发布动作的固定顺序是：最终代码与发布文档已提交，且工作区干净 → 创建并推送不可变的 exact tag → 从该 tag 对应 commit 的工作树重新正式签名构建 → 验证签名、渠道权限与 SHA-256 → 准备安全凭据环境 → 运行发布脚本。

先确认待发布分支已经合并、`git status --short` 无输出，且 `HEAD` 就是最终发布 commit，再首次创建并推送标签：

```bash
git tag -a v4.0 -m "Release v4.0"
git push origin v4.0
```

`v4.0` 是不可变的发布身份。禁止强制移动、覆盖或复用已推送的 `v4.0` tag，也禁止覆盖已发布 Release；若最终 commit 改变，应在发布前删除尚未推送的本地错误标签并重新创建。标签一旦推送或 Release 一旦发布，发现问题应停止发布、调查影响并使用新的版本号修复。

## 4. 构建 Release 产物

推送标签后，核对 `git rev-parse HEAD` 与 `git rev-parse v4.0^{commit}` 完全一致，再在这个工作树中执行新构建。不得复用旧构建产物；可先执行 `./gradlew clean` 清理 Gradle 输出，但不要使用会删除未跟踪文件的 `git clean`。正式构建完成后记录 Direct APK 与 Play AAB 的 SHA-256，并验证签名和渠道权限。

```bash
./gradlew testDirectDebugUnitTest testPlayDebugUnitTest compileDirectDebugAndroidTestKotlin
./gradlew lintDirectDebug lintDirectRelease lintPlayRelease lintVitalDirectRelease lintVitalPlayRelease
./gradlew assembleDirectRelease assemblePlayRelease bundlePlayRelease
```

产物路径：

- GitHub Release：`app/build/outputs/apk/direct/release/glimmer-countdown-4-0.apk`
- Play Console：`app/build/outputs/bundle/playRelease/app-play-release.aab`

## 5. 创建 GitHub Release

发布脚本只接受已经完成正式签名的 exact Direct APK。运行前必须准备：

- `app/build/outputs/apk/direct/release/glimmer-countdown-4-0.apk`，且由正式发布证书签名；
- `ANDROID_HOME`，其中至少有一个稳定版本的 `build-tools/apksigner`；
- `GLIMMER_RELEASE_CERT_SHA256`，内容为正式证书的 SHA-256 指纹；
- 本地先运行 `gh auth login` 完成交互式登录，由脚本内部调用 `gh auth token`；对应凭据对仓库具有 `GitHub Contents: write` 权限；
- CI 才通过仓库 secret 将 `GITHUB_TOKEN` 注入进程环境，且不得打印其值；
- 本地与远端均已有 `v4.0` tag，且 tag 位于最终发布 commit。

本地推荐使用 GitHub CLI 的凭据存储，避免把 token 明文写进命令。证书指纹和 SDK 路径按受控环境的实际方式注入；Shell 历史策略因环境而异，不作绝对安全承诺。PowerShell 运行示例：

```powershell
$env:GLIMMER_RELEASE_CERT_SHA256 = "your_release_certificate_sha256"
$env:ANDROID_HOME = "your_android_sdk"
gh auth login
.\scripts\publish-release.ps1
```

脚本行为：

- 自动读取 `gradle.properties` 中的 `VERSION_NAME`
- 自动从 `CHANGELOG.md` 提取 `4.0` 小节作为 Release Notes
- 在任何远端写操作前验证 APK 签名，并验证本地与远端 tag 解引用后的 commit 完全一致
- 通过 `refs/heads/release-locks/v4.0` Git ref 锁阻止两个合规脚本并发发布
- 新建带本次 `ownership marker` 的 draft；仅恢复带脚本自身旧 `ownership marker` 的 draft，拒绝 published Release、prerelease 与人工创建的 draft
- 删除 owned draft 中的所有旧资产后上传 exact Direct APK；按上传响应及重新读取结果绑定 asset id、size、digest、content type 与下载 URL
- 上传后要求整个 Release 只保留唯一的 exact Direct APK，拒绝 AAB、Play APK 或其他附件夹带
- 发布前反复校验 draft 身份和 ownership marker，发布后以最终 GET 验证公开 Release 及唯一 APK
- Play AAB 不上传 GitHub Release；Play AAB 只交付 Play Console

锁由脚本在 `finally` 中校验后清理。若进程崩溃或清理失败留下残留锁，下一次发布会安全拒绝继续；先调查是否仍有发布进程、draft 和远端变更，不要随意删除活跃锁。确认没有活跃发布者且记录好调查结论后，才可由有权限的维护者人工清理残留锁。

脚本不会覆盖已发布 Release，也不会接管没有 ownership marker 的人工 draft。需要重新发布内容时递增版本号，重新走完整检查清单。

### 隔离 publisher 回归

以下命令只读挂载仓库并禁用容器网络。测试器在容器临时目录创建匿名假 APK，以内存 GitHub REST 状态机运行真实发布脚本；不会读取本机 GitHub 凭据，也不会创建远端 ref、draft、asset 或 Release。

```bash
docker run --rm --network none --platform linux/amd64 \
  -v "$PWD:/workspace:ro" -w /workspace \
  mcr.microsoft.com/powershell:7.5-ubuntu-24.04 \
  pwsh -NoProfile -File scripts/tests/publish-release-mock-harness.ps1 -Scenario all
```

必须看到 5/5 通过：新发布成功、锁竞争拒绝、owned draft 恢复、失败后锁清理、清理失败保留残留锁。该回归验证脚本控制流与安全不变量，不替代最终 tag、正式签名产物、真实 GitHub 发布和发布后安装复验。

## 6. 发布后核对

- Release 标题、标签与说明是否对应 `v4.0`
- 上传的 APK 文件名是否为 `glimmer-countdown-4-0.apk`
- 整个 Release 只保留唯一的 exact Direct APK，没有 `app-play-release.apk`、`app-play-release.aab` 或其他资产
- GitHub API 最终 GET 返回公开、非 prerelease 的 `v4.0` Release，且唯一资产的 id、size、digest、下载 URL 与本地产物一致
- Direct APK `versionName` 是否为 `4.0`
- Play APK / AAB `versionName` 是否为 `4.0`
- Play APK 是否不包含 `REQUEST_INSTALL_PACKAGES`
- 抽检首页右上近期入口、月历选中日期内容与年月选择、详情轻量主卡与分享卡、新建 / 编辑标题输入与提醒滚轮、设置页样张、小组件配置、启动页、系统日历无可写提示和 Direct 渠道检查更新
