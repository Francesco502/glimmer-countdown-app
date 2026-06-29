# 发布检查清单（v3.13）

**版本**：`3.13`（`versionCode=18`）  **发布日期**：`2026-06-29`

## 一、自动校验

- [ ] `gradle.properties` 中 `VERSION_CODE=18`、`VERSION_NAME=3.13` 正确
- [ ] `app/build.gradle.kts` 的版本读取、APK 重命名、Direct / Play flavor 与 BuildConfig 开关正常
- [ ] `README.md`、`CHANGELOG.md` 与发布文档已切换到 `3.13`
- [ ] `git diff --check` 未发现尾随空格或冲突标记
- [ ] `./gradlew testDirectDebugUnitTest` 通过
- [ ] `./gradlew testPlayDebugUnitTest` 通过
- [ ] `./gradlew compileDirectDebugAndroidTestKotlin` 通过
- [ ] `./gradlew lintDirectRelease lintPlayRelease` 通过
- [ ] `./gradlew assembleDirectRelease assemblePlayRelease bundlePlayRelease` 通过
- [ ] Direct APK 已输出到 `app/build/outputs/apk/direct/release/glimmer-countdown-3-13.apk`
- [ ] Play AAB 已输出到 `app/build/outputs/bundle/playRelease/app-play-release.aab`
- [ ] Direct / Play APK 签名验证通过
- [ ] Direct APK 包含 `REQUEST_INSTALL_PACKAGES`，Play APK 不包含 `REQUEST_INSTALL_PACKAGES`
- [ ] GitHub Release 资产只包含 Direct APK，不上传 Play APK / AAB

## 二、发布前人工复核

- [ ] 首页卡片、列表、月历在浅色 / 深色主题下文字可读
- [ ] 设置首页分类入口、折叠分组、字体弹窗和小组件配置区显示正常
- [ ] 字体切换到 Noto Serif SC、ZCOOL XiaoWei、系统黑体、系统衬线、默认字体后无明显布局溢出
- [ ] 小组件添加配置页可保存 2x2、3x3、4x2 模板
- [ ] 小组件透明、半透明、宣纸、青瓷、朱印背景在桌面显示正常
- [ ] 小组件内容筛选、排序、密度、边框、圆角、文字对比和农历前缀配置生效
- [ ] 设置页默认小组件配置可以应用到已有小组件
- [ ] 新建 / 编辑 / 删除事件、提醒、系统日历同步和小组件刷新链路保持正常
- [ ] Play 渠道关于页不出现直接 APK 下载 / 安装入口
- [ ] Direct 渠道关于页检查更新仍可读取 GitHub Release

## 三、发布动作

- [ ] 提交并 push 当前分支到 GitHub
- [ ] 创建或更新 `v3.13` 标签
- [ ] 在 GitHub Release 中发布 `glimmer-countdown-3-13.apk`
- [ ] 以 `CHANGELOG.md` 中的 `3.13` 小节作为 Release Notes
- [ ] 确认 GitHub Release 页面标题、标签、说明和资产均对应 `v3.13`
