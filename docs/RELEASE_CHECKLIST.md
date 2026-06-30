# 发布检查清单（v3.14）

**版本**：`3.14`（`versionCode=19`）  **发布日期**：`2026-06-30`

## 一、自动校验

- [ ] `gradle.properties` 中 `VERSION_CODE=19`、`VERSION_NAME=3.14` 正确
- [ ] `app/build.gradle.kts` 的版本读取、APK 重命名、Direct / Play flavor 与 BuildConfig 开关正常
- [ ] `README.md`、`CHANGELOG.md` 与发布文档已切换到 `3.14`
- [ ] `git diff --check` 未发现尾随空格或冲突标记
- [ ] `./gradlew testDirectDebugUnitTest` 通过
- [ ] `./gradlew testPlayDebugUnitTest` 通过
- [ ] `./gradlew compileDirectDebugAndroidTestKotlin` 通过
- [ ] `./gradlew lintDirectRelease lintPlayRelease` 通过
- [ ] `./gradlew assembleDirectRelease assemblePlayRelease bundlePlayRelease` 通过
- [ ] Direct APK 已输出到 `app/build/outputs/apk/direct/release/glimmer-countdown-3-14.apk`
- [ ] Play AAB 已输出到 `app/build/outputs/bundle/playRelease/app-play-release.aab`
- [ ] Direct / Play APK 签名验证通过
- [ ] Direct APK 包含 `REQUEST_INSTALL_PACKAGES`，Play APK 不包含 `REQUEST_INSTALL_PACKAGES`
- [ ] GitHub Release 资产只包含 Direct APK，不上传 Play APK / AAB

## 二、发布前人工复核

- [ ] 首页卡片、列表、月历在浅色 / 深色主题下文字可读
- [ ] 首页浅色卡片为暖宣纸色，过期卡片为柔和旧纸色，边框和左侧色线不过曝
- [ ] 首页“卡片 / 列表 / 月历”切换为轻量页签，面积不再接近大号分段按钮
- [ ] 列表模式为透明书目行、短印色标记和极细分隔线，与卡片模式有明确差异
- [ ] 月历模式格子和选中日期事件行为轻量历书样式，不再像卡片列表
- [ ] 首页顶部搜索、筛选、排序、设置入口均可点击；搜索展开时替换标题区域且不会挤压内容
- [ ] 重大节点显示可读，例如百日、千日、一周年、半岁 / 周年节点优先于普通步进节点
- [ ] 月历日格不再显示农历小字，选中日期区域可完整显示农历信息
- [ ] 设置首页分类入口、折叠分组、字体弹窗和小组件配置区显示正常
- [ ] 字体切换到 Noto Serif SC、ZCOOL XiaoWei、系统黑体、系统衬线、默认字体后无明显布局溢出
- [ ] 小组件添加配置页可保存 2x2、3x3、4x2 模板
- [ ] 小组件透明、半透明、宣纸、青瓷、朱印背景在桌面显示正常
- [ ] 小组件全透明和半透明玻璃模式在浅色 / 深色系统下文字都可读
- [ ] 小组件内容筛选、排序、密度、边框、圆角、文字对比和农历前缀配置生效
- [ ] 设置页默认小组件配置可以应用到已有小组件
- [ ] 新建 / 编辑 / 删除事件、提醒、系统日历同步和小组件刷新链路保持正常
- [ ] Play 渠道关于页不出现直接 APK 下载 / 安装入口
- [ ] Direct 渠道关于页检查更新仍可读取 GitHub Release

## 三、发布动作

- [ ] 提交并 push 当前分支到 GitHub
- [ ] 创建或更新 `v3.14` 标签
- [ ] 在 GitHub Release 中发布 `glimmer-countdown-3-14.apk`
- [ ] 以 `CHANGELOG.md` 中的 `3.14` 小节作为 Release Notes
- [ ] 确认 GitHub Release 页面标题、标签、说明和资产均对应 `v3.14`
