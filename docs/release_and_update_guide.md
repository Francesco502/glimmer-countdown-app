# TimeAPK 发布与更新指引

本文档记录 4.2 的正式签名、构建、验证与更新流程（2026-10-12）。**4.2 当前为候选 / 未发布**：代码与发布文档在 `feat/v4.2` 分支，尚未创建 `v4.2` tag，也没有正式签名的最终 APK。最新公开版本仍为 [v4.1](https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.1)，其 tag commit、CI、正式 APK QA、Release/资产 ID 与公开复验记录归档在 [v4.1 清单](releases/v4.1-checklist.md)，不作为 4.2 的通过证据。

4.2 正式签名须继续使用线上 v4.0 / v4.1 的同一证书（SHA-256 `3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`），以便覆盖升级。签名 QA 以公开 v4.1 APK（26,463,543 bytes，SHA-256 `8fbeee7590f89104995ecce98769842dc2a75cf84d8641efd6a0c7b67619e5b4`）为固定升级基线。按用户决定，本版不安排实体手机验收，物理设备相关项保持未执行并如实记录。

**唯一正式发布渠道：GitHub Release。** 唯一官方资产为 Direct APK `glimmer-countdown-4-2.apk`。Play flavor 仅保留用于兼容性与开发回归，不是 4.2 正式发布工件或阻断项。

## 一、当前状态

| 项目 | 状态 |
|------|------|
| `applicationId` / 版本号 | Direct：`com.example.timeapk` / `4.2`；Play：`com.example.timeapk.play` / `4.2`（仅开发回归） |
| 最低 / 目标 SDK | `minSdk 26` / `targetSdk 36` |
| Release 构建 | 已启用 `release` buildType，并开启 `minify` 与 `shrinkResources` |
| Release 签名 | 既有正式密钥已核验（与线上 v4.0 / v4.1 同证）；最终 4.2 APK 尚未从 tag 正式签名 |
| Direct APK 命名 | 输出为 `glimmer-countdown-4-2.apk` |
| 正式渠道 | GitHub Release，只上传 Direct APK |
| Play flavor | 保留用于兼容性与开发回归；不产生正式资产或发布门 |

## 二、发布前准备

### 1. 签名配置

工作区中的既有正式密钥与配置已通过 Java Properties / JCA 加载核验。正式构建可使用 `TIMEAPK_KEYSTORE_PROPERTIES` 指定配置文件，或读取仓库根目录配置；不能通过新建证书绕过升级签名要求。配置字段示例如下：

```properties
storeFile=timeapk-release.keystore
storePassword=xxx
keyAlias=timeapk
keyPassword=xxx
```

上面仅为字段示例，不要复制真实值到终端输出、截图、issue 或提交。正式发布还需以安全环境变量提供正式证书指纹：

```text
GLIMMER_RELEASE_CERT_SHA256=<64位SHA-256证书指纹>
```

已取得密钥及线上 v4.1 APK 的证书 SHA-256 均独立核验为 `3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca`；最终 4.2 APK 须单独验签并一致。私钥、密码、配置内容和 GitHub token 不进入文档或日志。

确保以下文件不进入仓库：

- `keystore.properties`
- `*.keystore`

### 2. 版本确认

当前版本值：

- `VERSION_NAME=4.2`
- `VERSION_CODE=25`

当前 `4.2` / `25` 为候选 / 未发布，实际证据按本版 [发布检查清单](RELEASE_CHECKLIST.md) 记录。按用户决定本版不安排实体手机验收，物理设备项保持未执行，不继承 v4.0 豁免。

继续发布新版本时，应同步递增 `versionCode`，并更新 `versionName`、`README.md`、`CHANGELOG.md` 与发布文档。

## 三、构建命令

```bash
./gradlew testDirectDebugUnitTest
./gradlew compileDirectDebugAndroidTestKotlin
./gradlew lintDirectDebug lintDirectRelease lintVitalDirectRelease
./gradlew assembleDirectRelease
```

若正式密钥只留在本机，可采用等价路径完成签名，不修改 Gradle 的默认签名门：

1. 从最终不可变 `v4.2` tag 对应 commit 在无密钥的 CI 执行 `./gradlew --no-daemon --no-parallel packageDirectRelease -x validateReleaseSigning`，生成 Release R8 / 资源压缩的未签名工件。此处排除签名校验仅用于未签名打包，不能称正式签名完成，也不能直接发布。
2. 保存该 run 的源码 revision、未签名 APK 原始 SHA-256 和原始 `output-metadata.json`；下载后核对哈希及 `directRelease` / `com.example.timeapk` / `4.2` / `25` 的唯一无过滤 artifact。不得用 CI3 或其他旧候选工件替代最终 tag 工件。
3. 在本机先 zipalign，再用原生 apksigner 和已核验的正式 keystore 签名。密钥不出本机，密码通过进程环境输入，不写入命令或日志。下例 `unsigned.apk` 指原始 metadata 引用的未签名 APK，SDK Build Tools 路径按本机环境解析；环境变量须事先安全注入。

   ```bash
   zipalign -P 16 -f 4 unsigned.apk aligned.apk
   apksigner sign --ks local-release.keystore --ks-key-alias release-alias \
     --ks-pass env:TIMEAPK_STORE_PASSWORD --key-pass env:TIMEAPK_KEY_PASSWORD \
     --out glimmer-countdown-4-2.apk aligned.apk
   apksigner verify --print-certs glimmer-countdown-4-2.apk
   zipalign -c -P 16 4 glimmer-countdown-4-2.apk
   ```

   zipalign 必须在 apksigner 签名前完成；密码的 `env:` 输入格式见 [apksigner](https://developer.android.com/tools/apksigner)，对齐顺序见 [zipalign](https://developer.android.com/tools/zipalign)。签名后不得再改 APK 内容。
4. 记录正式 APK 的大小、SHA-256、证书及真实包身份，将最终 APK 与 metadata 放入下述输出目录；保留原始 metadata，只将发布用副本的唯一 artifact `outputFile` 改为 `glimmer-countdown-4-2.apk`，不改版本、包名、variant、类型或 filters。publisher 独立验签和核对 APK/metadata；只有全部校验成功的最终 APK 才可发布。

产物路径：

- `app/build/outputs/apk/direct/release/glimmer-countdown-4-2.apk`

Play flavor 的 Debug 测试、编译或安装仅用于开发回归，必须单独记录，且不是本页正式发布的前置条件。

## 四、GitHub 发布

固定发布顺序：最终代码与发布文档已提交，且工作区干净 → 创建并推送不可变的 exact tag → 从该 tag 对应 commit 的工作树重新正式签名构建（可按上述等价路径分离 CI 打包与本机签名） → 验证签名、精确证书指纹与 SHA-256 → 准备安全凭据环境 → 运行发布脚本。

### 1. 提交与推送

```bash
git add app gradle.properties README.md CHANGELOG.md docs scripts .gitignore
git commit -m "release: ship v4.2"
git push origin main
```

### 2. 标签

标签必须在代码、文档和检查结果全部确定后，创建于最终发布 commit：

```bash
git tag -a v4.2 -m "Release v4.2"
git push origin v4.2
```

脚本会分别解引用 annotated/lightweight tag，并要求本地与远端 tag 解引用后的 commit 精确一致。禁止 force-push、移动或复用已推送 tag，禁止覆盖已发布 Release；已发布后出现问题必须递增版本号。

标签推送后先核对 `git rev-parse HEAD` 与 `git rev-parse v4.2^{commit}` 一致，再从该提交执行本页“构建命令”或 CI 未签名构建、本机正式签名路径。可用 `./gradlew clean` 清理构建输出，不得复用旧构建产物，也不要使用可能删除未跟踪文件的 `git clean`。用正式密钥完成 Direct APK 后，记录 SHA-256，并验证签名、精确证书指纹与安装权限。

### 3. Release

前置条件：正式签名 exact Direct APK 已从 tag commit 新鲜生成；`ANDROID_HOME` 可定位稳定版 `apksigner` 与 `aapt`；`GLIMMER_RELEASE_CERT_SHA256` 已安全注入；本地运行 `gh auth login` 后脚本可通过 `gh auth token` 取得具备 `GitHub Contents: write` 的凭据；本地和远端 `v4.2` tag 已指向最终发布 commit。CI 才通过仓库 secret 注入 `GITHUB_TOKEN`，且不得打印其值。

```powershell
$env:GLIMMER_RELEASE_CERT_SHA256 = "your_release_certificate_sha256"
$env:ANDROID_HOME = "your_android_sdk"
gh auth login
.\scripts\publish-release.ps1
```

不要在命令行中直接书写 token。GitHub CLI 可减少明文凭据暴露，但命令历史和凭据存储安全仍取决于本机配置，不能作绝对保证。

发布工具沿用 v4.1 已验证的 `scripts/publish-release.ps1`（兼容正式 apksigner 的 `Signer #1 certificate SHA-256 digest:` 输出及旧 `V2 Signer:` 格式），隔离回归须在本版 CI 重新 10/10 通过。

脚本会：

- 读取当前版本号；工作区存在 tracked / untracked 改动时拒绝发布
- 提取 `CHANGELOG.md` 中 `4.2` 小节作为 Release Notes
- 要求当前 `HEAD` 等于 exact 本地 tag commit，并在首次远端写入前再次复核
- 校验 `output-metadata.json` 的唯一 Direct artifact，再用 `aapt` 验证 APK 的真实包名、版本、非调试状态和安装包权限
- 校验正式证书指纹及本地/远端 tag commit 后创建 `refs/heads/release-locks/v4.2` Git ref 锁
- 创建带 `ownership marker` 的 draft；仅恢复带脚本自身 marker 的 draft，拒绝 published Release 和人工 draft
- 删除 owned draft 中的所有旧资产，再上传 exact Direct APK，并将响应及重新读取结果绑定到 asset id、size、digest、content type 和下载 URL
- 要求整个 Release 只保留唯一的 exact Direct APK，发现任何其他资产即拒绝发布
- 在发布前重新核对 ownership marker，并以最终 GET 验证公开 Release 与唯一 APK

发布进程并发或发现残留锁时脚本会拒绝继续。先调查是否仍有活跃发布进程、owned draft 或已发生的远端 mutation；不要随意删除活跃锁。只有确认是崩溃遗留且没有活跃发布者后，维护者才可记录原因并人工清理。

## 五、建议抽检

- 首页“最近”横幅、放大的倒数数字与上方“已经 / 还有”、朱印“今天”、置顶标记、顶栏搜索与长按菜单
- 月历格内农历日 / 节日 / 节气、分类色点、今天与选中日区分、左右滑动换月与“今天”按钮；新建按钮预填所选日期
- 详情配图与分享弹窗（不透明、预览完整、弹窗内保存反馈）；新建页日期紧随标题、快捷提醒、自定义分类与配图
- 设置页当前值、宋韵方形开关、分类管理、跟随壁纸取色、精确提醒（Direct）
- 小组件设置“外观 / 内容”分页、1-5 格“预览宽度 / 预览高度”、透明度项按背景显隐、覆盖确认；单事件大数字小组件
- 宽屏（≥840dp）列表与详情双栏；深色模式下的磨砂玻璃与自定义颜色
- 新建 / 编辑 / 删除事件、提醒、系统日历同步和小组件刷新链路是否正常
- Direct 渠道检查更新是否能读取 GitHub Release，下载进度与安装权限引导
- 物理手机 Direct APK 安装 / 升级、通知、日历、Launcher 小组件与性能 smoke：本版按用户决定不安排，记录为未执行，不写成通过
- 从公开 GitHub Release 在线重新安装唯一 APK，并完成更新检查与关键链路 smoke：发布后执行，结果写入本版检查清单
