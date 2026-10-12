# GitHub 提交与发布流程（v4.2）

本文档记录 4.2 的正式发布流程与当前结果（2026-10-12）。4.2 已公开，最终正式签名 APK 已独立验签，与线上 v4.0 / v4.1 同证。不可变 `v4.2` tag 对应 APK 源码 commit 为 `f0937448efa02232d5c6b1d47c531d50cad0797e`（annotated tag 对象 `294035246e8a96072175710efbc4961088531df3`）；最终 [TAG CI 38235651872](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38235651872) 与 [正式 APK QA 38238275647](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38238275647) 均通过，后者覆盖 API26/31/36 由公开 v4.1 保留数据覆盖升级与 AMS 草稿恢复，以及 API36 五页和分享验证。

公开 Release ID 为 `410506677`，唯一资产 ID 为 `633513961`，大小 `12513338` 字节，SHA-256 为 `7fe9673e4b3a85f4152c1e5786f8a221779a95fc56578f9c93b660b4e9019e74`。公开无鉴权下载的哈希、证书及真实包身份 `com.example.timeapk` / `4.2` / `25` 已复核；[公开 APK 安装与在线更新复验 38239618078](https://github.com/Francesco502/glimmer-countdown-app/actions/runs/38239618078) 三平台 job 均成功，API36 实际检查更新显示“已是最新版本”。原始结果见 [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md)。按评审决定，本版不安排实体手机验收，物理设备相关项保持未执行，不继承历史豁免。

**唯一正式发布渠道：GitHub Release。** 唯一官方资产为 `glimmer-countdown-4-2.apk`。Play flavor 仅保留用于兼容性与开发回归，不是 4.2 正式发布工件或阻断项。

最新公开版本为 [v4.1](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.1)。v4.1 与 [v4.0](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.0) 的测试、豁免与发布结果只保留在 [v4.1 清单](releases/v4.1-checklist.md) 与 [v4.0 清单](releases/v4.0-checklist.md)，不得迁移为 4.2 通过项。

## 1. 本地提交

```bash
git status
git add app gradle.properties README.md CHANGELOG.md docs scripts .gitignore
git commit -m "release: ship v4.2"
```

说明：

- 只提交业务代码、版本元数据、脚本、字体 license 和发布文档
- 不提交本地缓存目录，例如 `.gradle-user-home`、`.cursor`、`build`

## 2. 推送代码

```bash
git push origin main
```

如果最终发布分支是 `main`，应先完成合并或按仓库实际策略推送到目标分支。

## 3. 在最终发布 commit 上创建 `v4.2` 标签

发布动作的固定顺序是：最终代码与发布文档已提交，且工作区干净 → 创建并推送不可变的 exact tag → 从该 tag 对应 commit 的工作树重新正式签名构建（可按下述等价路径分离 CI 打包与本机签名） → 验证签名、精确证书指纹与 SHA-256 → 准备安全凭据环境 → 运行发布脚本。

先确认待发布分支已经合并、`git status --short` 无输出，且 `HEAD` 就是最终发布 commit，再首次创建并推送标签：

```bash
git tag -a v4.2 -m "Release v4.2"
git push origin v4.2
```

`v4.2` 是不可变的发布身份。禁止强制移动、覆盖或复用已推送的 `v4.2` tag，也禁止覆盖已发布 Release；若最终 commit 改变，应在发布前删除尚未推送的本地错误标签并重新创建。标签一旦推送或 Release 一旦发布，发现问题应停止发布、调查影响并使用新的版本号修复。

## 4. 构建 Release 产物

推送标签后，核对 `git rev-parse HEAD` 与 `git rev-parse v4.2^{commit}` 完全一致，再从该提交执行新构建。不得复用旧构建产物；可先执行 `./gradlew clean` 清理 Gradle 输出，但不要使用会删除未跟踪文件的 `git clean`。正式签名完成后记录 Direct APK 的 SHA-256，并验证正式签名、精确证书指纹和 Direct 安装权限。

```bash
./gradlew testDirectDebugUnitTest compileDirectDebugAndroidTestKotlin
./gradlew lintDirectDebug lintDirectRelease lintVitalDirectRelease
./gradlew assembleDirectRelease
```

也可按[签名指引](release_and_update_guide.md#三构建命令)从最终 tag commit 在 CI 执行 `packageDirectRelease -x validateReleaseSigning`，保留未签名 R8 工件的源码 revision、原始 SHA-256 和 metadata；本机先 zipalign，再以环境密码输入调用原生 apksigner 正式签名，密钥不出本机。未签名工件不能称正式签名完成或发布；Gradle 默认签名门不修改。发布用 metadata 仅将唯一 artifact 的 `outputFile` 改为 exact APK 名称，版本、包名、variant 等保持原值，最终由 publisher 独立核验。

产物路径：

- GitHub Release 唯一资产：`app/build/outputs/apk/direct/release/glimmer-countdown-4-2.apk`

如果需要兼容性回归，可单独运行 Play flavor 的 Debug 测试或构建；其结果不延迟、替代或扩展本页的正式发布步骤。

## 5. 创建 GitHub Release

发布脚本只接受已经完成正式签名的 exact Direct APK。运行前必须准备：

- `app/build/outputs/apk/direct/release/glimmer-countdown-4-2.apk`，且由正式发布证书签名；
- `ANDROID_HOME`，其中至少有一个稳定版本的 `build-tools/apksigner` 与 `build-tools/aapt`；
- `GLIMMER_RELEASE_CERT_SHA256`，内容为正式证书的 SHA-256 指纹；
- 本地先运行 `gh auth login` 完成交互式登录，由脚本内部调用 `gh auth token`；对应凭据对仓库具有 `GitHub Contents: write` 权限；
- CI 才通过仓库 secret 将 `GITHUB_TOKEN` 注入进程环境，且不得打印其值；
- 本地与远端均已有 `v4.2` tag，且 tag 位于最终发布 commit。

正式证书须与已发布 v4.0 / v4.1 兼容。已取得密钥的证书 SHA-256 核验为 `3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`，与线上 v4.1 APK 一致；该结果不替代最终 4.2 APK 的独立验签，不得用临时 QA 证书替代。

发布工具沿用 v4.1 已验证的 `scripts/publish-release.ps1`（兼容正式 apksigner 的 `Signer #1 certificate SHA-256 digest:` 输出和旧 `V2 Signer:` 格式），4.2 只更新版本相关文案，隔离回归须在本版 CI 重新 10/10 通过。

本地推荐使用 GitHub CLI 的凭据存储，避免把 token 明文写进命令。证书指纹和 SDK 路径按受控环境的实际方式注入；Shell 历史策略因环境而异，不作绝对安全承诺。PowerShell 运行示例：

```powershell
$env:GLIMMER_RELEASE_CERT_SHA256 = "your_release_certificate_sha256"
$env:ANDROID_HOME = "your_android_sdk"
gh auth login
.\scripts\publish-release.ps1
```

脚本行为：

- 自动读取 `gradle.properties` 中的 `VERSION_NAME` 与 `VERSION_CODE`
- 自动从 `CHANGELOG.md` 提取 `4.2` 小节作为 Release Notes
- 在任何 GitHub 请求前拒绝 tracked / untracked 脏工作区，并要求当前 `HEAD` 等于 exact 本地 tag commit；首次远端写入前再次复核，避免预检后源码或 tag 被替换
- 严格校验 `output-metadata.json` 只描述一个 `directRelease` / `com.example.timeapk` / `4.2` / `25` 的 exact APK，并使用稳定版 `aapt` 验证 APK 的真实包名、版本、非调试状态和 `REQUEST_INSTALL_PACKAGES`
- 在任何远端写操作前验证 APK 签名，并验证本地与远端 tag 解引用后的 commit 完全一致
- 通过 `refs/heads/release-locks/v4.2` Git ref 锁阻止两个合规脚本并发发布
- 新建带本次 `ownership marker` 的 draft；仅恢复带脚本自身旧 `ownership marker` 的 draft，拒绝 published Release、prerelease 与人工创建的 draft
- 删除 owned draft 中的所有旧资产后上传 exact Direct APK；按上传响应及重新读取结果绑定 asset id、size、digest、content type 与下载 URL
- 上传后要求整个 Release 只保留唯一的 exact Direct APK，拒绝任何其他附件夹带
- 发布前反复校验 draft 身份和 ownership marker，发布后以最终 GET 验证公开 Release 及唯一 APK

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

必须看到 10/10 通过：新发布成功、锁竞争拒绝、owned draft 恢复、失败后锁清理、清理失败保留残留锁，以及脏工作区、未跟踪文件、`HEAD` / tag 不一致、输出元数据错误和 APK 身份错误五类本地预检拒绝。该回归验证脚本控制流与安全不变量，不替代最终 tag、正式签名产物、真实 GitHub 发布和发布后安装复验。

## 6. 发布后核对

- Release 标题、标签与说明是否对应 `v4.2`
- 上传的 APK 文件名是否为 `glimmer-countdown-4-2.apk`
- 整个 Release 只保留唯一的 exact Direct APK，没有任何其他资产
- GitHub API 最终 GET 返回公开、非 prerelease 的 `v4.2` Release，且唯一资产的 id、size、digest、下载 URL 与本地产物一致
- Direct APK `versionName` 是否为 `4.2`
- 抽检首页“最近”横幅、朱印“今天”与置顶标记、月历农历/节气与今天/选中区分、详情配图与分享弹窗、新建页（日期在标题下、快捷提醒、自定义分类）、设置当前值、小组件设置（外观 / 内容）、单事件小组件、系统日历无可写提示和 Direct 渠道检查更新
- 物理手机安装 / 升级、关键链路与性能 smoke：本版按用户决定不安排（无实体手机），记录为未执行；公开 APK 模拟器复验 38239618078 三平台 job 均成功，原始结果见本版检查清单，不能写成物理验收通过
