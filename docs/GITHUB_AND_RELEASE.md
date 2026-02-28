# 上传 GitHub 与打包发布操作指南

本文说明如何将 TimeAPK 上传到 GitHub，并完成签名打包与发布。

---

## 一、上传到 GitHub

### 1. 在 GitHub 上创建仓库

1. 登录 [GitHub](https://github.com)，点击右上角 **+** → **New repository**。
2. 填写：
   - **Repository name**：如 `TimeAPK` 或 `ShiGuang`
   - **Description**：可选，如「Android 倒计时 / 纪念日应用」
   - **Public**，**不要**勾选 “Add a README”（本地已有项目）
3. 点击 **Create repository**，记下仓库地址，例如：  
   `https://github.com/你的用户名/TimeAPK.git`

### 2. 本地初始化 Git 并推送（若尚未初始化）

在项目根目录（`d:\LLT\Code\TimeAPK`）打开 PowerShell 或 CMD：

```powershell
# 若还未初始化 Git
git init

# 添加远程仓库（把下面的 URL 换成你的仓库地址）
git remote add origin https://github.com/你的用户名/TimeAPK.git

# 添加所有文件（.gitignore 会排除 build、.idea、keystore 等）
git add .

# 首次提交
git commit -m "Initial commit: 拾光 TimeAPK 倒计时应用"

# 推送到 GitHub（主分支名可能是 main 或 master）
git branch -M main
git push -u origin main
```

若本地已有 Git 但未添加远程：

```powershell
git remote add origin https://github.com/你的用户名/TimeAPK.git
git push -u origin main
```

### 3. 确认不要提交的内容

以下内容已在 `.gitignore` 中，**不应**出现在仓库里：

- `keystore.properties`、`*.keystore`、`*.jks`（签名密钥）
- `local.properties`
- `build/`、`app/build/`、`.gradle/`
- `.idea/`、`.trae/`

推送前可执行 `git status` 确认没有上述敏感或生成目录被加入。

---

## 二、打包发布（Release APK / AAB）

### 1. 配置签名（必须，仅做一次）

Release 包必须签名后才能安装或上架。

**方式 A：使用项目自带脚本（推荐）**

在项目根目录执行：

```powershell
.\gen-keystore.ps1
```

按提示输入密钥库密码、姓名、组织等，并**牢记密码与 alias（默认 `timeapk`）**。

**方式 B：手动使用 keytool**

```powershell
keytool -genkey -v -keystore timeapk-release.keystore -alias timeapk -keyalg RSA -keysize 2048 -validity 10000
```

将生成的 `timeapk-release.keystore` 放在**项目根目录**（与 `build.gradle.kts` 同级）。

**创建 keystore.properties**

在项目根目录新建 `keystore.properties`（不要提交到 Git），内容示例：

```properties
storeFile=timeapk-release.keystore
storePassword=你的密钥库密码
keyAlias=timeapk
keyPassword=你的密钥密码
```

若 keystore 与 `keystore.properties` 同在根目录，`storeFile` 写文件名即可。

### 2. 构建 Release APK

```powershell
# 直装渠道（默认 applicationId）
.\gradlew assembleDirectRelease

# 或 Play 渠道（applicationId 带 .play 后缀）
.\gradlew assemblePlayRelease
```

产出路径：

- 直装：`app\build\outputs\apk\direct\release\app-direct-release.apk`
- Play：`app\build\outputs\apk\play\release\app-play-release.apk`

可将该 APK 直接发给用户安装，或用于应用商店上架。

### 3. 构建 AAB（Google Play 上架用）

若上架 Google Play，应使用 App Bundle：

```powershell
.\gradlew bundleRelease
```

产出：`app\build\outputs\bundle\release\app-release.aab`，上传到 Play 后台即可。

### 4. 发布前检查

- [ ] 已配置 `keystore.properties` 且能成功执行 `assembleDirectRelease` 或 `assemblePlayRelease`
- [ ] 每次发布前在 `app/build.gradle.kts` 中递增 `versionCode` 并更新 `versionName`（如 1.0 → 1.1）
- [ ] 应用名称、图标、权限与隐私说明与上架渠道一致

---

## 三、在 GitHub 上创建 Release（必做：支持应用内更新）

应用内「检查更新」会请求 GitHub API 获取**最新 Release** 及其中的 **.apk 附件**。因此每次发布新版本时都要在 GitHub 创建 Release 并上传 APK。

### 步骤

1. 打开仓库：`https://github.com/Francesco502/glimmer-countdown-app` → **Releases** → **Create a new release**。
2. **Choose a tag**：新建标签，**必须与当前版本号一致**，例如当前 `VERSION_NAME=2.0` 则填 **`v2.0`**（带前缀 `v`）。选 “Create new tag” 后发布。
3. **Release title**：如 `v2.0`。
4. **Describe**：写更新说明（会显示在应用内更新弹窗的「更新说明」中）。可使用 **[CHANGELOG.md](../CHANGELOG.md)** 中对应版本的条目；或使用 `.\scripts\publish-release.ps1` 自动发布时，脚本会从 CHANGELOG 读取。
5. **Attach binaries**：将本地构建好的 APK 拖拽上传。  
   - 路径示例：`app\build\outputs\apk\direct\release\glimmer-countdown-2-0.apk`（随 VERSION_NAME 变化）  
   - 附件名需为 **.apk 结尾**，应用只会识别带 `.apk` 的附件作为下载链接。
6. 点击 **Publish release**。

完成后：

- 用户可在 GitHub Releases 页面直接下载 APK 安装。
- 已安装用户可在应用内 **设置 → 检查更新** 收到新版本提示，并选择「下载并安装」或「在浏览器中打开」完成更新。

---

## 四、简要流程小结

| 步骤 | 操作 |
|------|------|
| 1 | 在 GitHub 创建新仓库，不勾选 README |
| 2 | 本地 `git init`（若无）→ `git add .` → `git commit` → `git remote add origin <URL>` → `git push -u origin main` |
| 3 | 运行 `gen-keystore.ps1` 生成 keystore，在根目录创建 `keystore.properties` |
| 4 | 执行 `.\gradlew assembleDirectRelease` 得到签名 APK |
| 5 | （可选）在 GitHub Releases 新建版本并上传 APK |

更细的签名、渠道、版本号与更新机制说明见 **[release_and_update_guide.md](release_and_update_guide.md)**。
