# 发布检查清单（v3.15）

**版本**：`3.15`（`versionCode=20`）  **发布日期**：`2026-07-02`

## 一、自动校验

- [x] `gradle.properties` 中 `VERSION_CODE=20`、`VERSION_NAME=3.15` 正确
- [x] `app/build.gradle.kts` 的版本读取、APK 重命名、Direct / Play flavor 与 BuildConfig 开关正常
- [x] `README.md`、`CHANGELOG.md` 与发布文档已切换到 `3.15`
- [x] `git diff --check` 未发现尾随空格或冲突标记
- [x] `./gradlew testDirectDebugUnitTest` 通过
- [x] `./gradlew testPlayDebugUnitTest` 通过
- [x] `./gradlew compileDirectDebugAndroidTestKotlin` 通过
- [x] `./gradlew lintDirectRelease lintPlayRelease` 通过
- [x] `./gradlew assembleDirectRelease assemblePlayRelease bundlePlayRelease` 通过
- [x] Direct APK 已输出到 `app/build/outputs/apk/direct/release/glimmer-countdown-3-15.apk`
- [x] Play AAB 已输出到 `app/build/outputs/bundle/playRelease/app-play-release.aab`
- [x] Direct / Play APK 签名验证通过
- [x] Direct APK 包含 `REQUEST_INSTALL_PACKAGES`，Play APK 不包含 `REQUEST_INSTALL_PACKAGES`
- [ ] GitHub Release 资产只包含 Direct APK，不上传 Play APK / AAB

## 二、发布前人工复核

- [ ] 首页卡片、列表、月历在浅色 / 深色主题下文字可读
- [x] 首页“今日 / 七日 / 本月 / 节点”近期摘要显示正确，点击摘要可聚焦对应事件，再次点击可取消聚焦
- [x] 首页浅色卡片为暖宣纸色，过期卡片为柔和旧纸色，边框和左侧色线不过曝
- [x] 首页“卡片 / 列表 / 月历”切换为轻量页签，面积不再接近大号分段按钮
- [ ] 列表模式为透明书目行、短印色标记和极细分隔线，与卡片模式有明确差异
- [x] 月历模式格子和选中日期事件行为轻量历书样式，不再像卡片列表
- [ ] 首页顶部搜索、筛选、排序、设置入口均可点击；搜索展开时替换标题区域且不会挤压内容
- [ ] 重大节点显示可读，例如百日、千日、一周年、半岁 / 周年节点优先于普通步进节点
- [x] 月历日格不再显示农历小字，选中日期区域可完整显示农历信息
- [x] 月历“本月重点”按生日、纪念日、倒数日和节点聚合，数量与列表一致
- [x] 详情页提醒可信度状态条可读，编辑、置顶、删除和提醒入口操作正常
- [x] 新建 / 编辑页实时预览卡会跟随标题、分类、日期、颜色和提醒状态变化
- [x] 生日、纪念日、倒数日模板会套用正确分类与重复规则
- [ ] 宋式命名色板在浅色 / 深色主题下均可读
- [ ] 设置首页分类入口、折叠分组、字体弹窗和小组件配置区显示正常
- [x] 设置页外观样张预览和系统日程健康状态显示正常
- [ ] 字体切换到 Noto Serif SC、ZCOOL XiaoWei、系统黑体、系统衬线、默认字体后无明显布局溢出
- [ ] 小组件添加配置页可保存 2x2、3x3、4x2 模板
- [ ] 小组件透明、半透明、宣纸、青瓷、朱印背景在桌面显示正常
- [ ] 小组件全透明和半透明玻璃模式在浅色 / 深色系统下文字都可读
- [ ] 小组件内容筛选、排序、密度、边框、圆角、文字对比和农历前缀配置生效
- [ ] 设置页默认小组件配置可以应用到已有小组件
- [ ] 新建 / 编辑 / 删除事件、提醒、系统日历同步和小组件刷新链路保持正常
- [ ] 启动页包含“拾光”品牌字标，动画不过长，普通图标与 Android themed icon 均可识别
- [ ] Play 渠道关于页不出现直接 APK 下载 / 安装入口
- [ ] Direct 渠道关于页检查更新仍可读取 GitHub Release

## 三、3.15 实测记录

- `2026-07-02` 使用 `Vivo_Fold3_pro` Android 模拟器安装 `glimmer-countdown-3-15.apk` 完成 UI QA。
- 首页：确认近期摘要在空状态和 1 条事件状态下均渲染正常；月历页确认 `本月重点`、选中日期事件、农历信息和浮动新增入口无遮挡。
- 新建页：确认实时预览卡、生日 / 纪念日 / 倒数日模板、底部保存按钮禁用 / 启用状态正常；通知和日历权限弹窗可触发。
- 保存链路：在模拟器无可写系统日历时，事件主体可保存，详情页和设置页会显示系统日程不可用状态。
- 详情页：确认提醒可信度状态条、`调整` 入口和底部 `提醒 / 置顶 / 编辑 / 删除` 工具栏无重叠。
- 设置页：确认设置总览、主题样张预览、字体 / 字号分组、里程碑与提醒、系统日程同步分组均渲染正常。
- logcat：未发现 `AndroidRuntime` 崩溃；系统日历无可写日历只记录为可预期的同步 warning。

## 四、发布动作

- [ ] 提交并 push 当前分支到 GitHub
- [ ] 创建或更新 `v3.15` 标签
- [ ] 在 GitHub Release 中发布 `glimmer-countdown-3-15.apk`
- [ ] 以 `CHANGELOG.md` 中的 `3.15` 小节作为 Release Notes
- [ ] 确认 GitHub Release 页面标题、标签、说明和资产均对应 `v3.15`
