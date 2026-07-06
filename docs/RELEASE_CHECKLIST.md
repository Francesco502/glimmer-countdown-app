# 发布检查清单（v3.16）

**版本**：`3.16`（`versionCode=21`）  **发布日期**：`2026-07-06`

## 一、自动校验

- [x] `gradle.properties` 中 `VERSION_CODE=21`、`VERSION_NAME=3.16` 正确
- [x] `app/build.gradle.kts` 的版本读取、APK 重命名、Direct / Play flavor 与 BuildConfig 开关正常
- [x] `README.md`、`CHANGELOG.md` 与发布文档已切换到 `3.16`
- [x] `git diff --check` 未发现尾随空格或冲突标记
- [x] `./gradlew testDirectDebugUnitTest` 通过
- [x] `./gradlew testPlayDebugUnitTest` 通过
- [x] `./gradlew compileDirectDebugAndroidTestKotlin` 通过
- [x] `./gradlew lintDirectDebug lintDirectRelease lintPlayRelease lintVitalDirectRelease lintVitalPlayRelease` 通过且无未解释 warning
- [x] `./gradlew assembleDirectRelease assemblePlayRelease bundlePlayRelease` 通过
- [x] Direct APK 已输出到 `app/build/outputs/apk/direct/release/glimmer-countdown-3-16.apk`
- [x] Play AAB 已输出到 `app/build/outputs/bundle/playRelease/app-play-release.aab`
- [x] Direct / Play APK 签名验证通过
- [x] Direct APK 包含 `REQUEST_INSTALL_PACKAGES`，Play APK 不包含 `REQUEST_INSTALL_PACKAGES`
- [x] GitHub Release 资产只包含 Direct APK，不上传 Play APK / AAB

## 二、发布前人工复核

- [ ] 首页卡片、列表、月历在浅色 / 深色主题下文字可读
- [x] 首页右上近期入口可呼出“今日 / 七日 / 本月 / 节点”，点击后可聚焦对应事件，再次点击可取消聚焦
- [x] 首页列表模式为桌面小组件式紧凑行，只保留标题与时间核心信息
- [x] 首页卡片模式显示分类、重复、提醒等辅助行，信息层级清楚
- [ ] 首页顶部搜索和右上操作入口均可点击；搜索展开时替换标题区域且不会挤压内容
- [x] 月历模式不再显示“本月重点”，点击日期后展示该日期事件、农历和相对时间
- [x] 月历顶部年月可打开日期选择器并切换到对应月份
- [x] 月历选中日期事件列表在模拟器常规字体下不被底部区域挤压
- [x] 详情页提醒状态条可读，权限类 CTA 会进入系统设置，编辑类 CTA 会进入编辑页
- [x] 详情页轻量主卡、无框表格详情、置顶 / 编辑 / 分享 / 删除四等分底部操作显示正常
- [x] 详情页分享预览、保存图片与系统分享面板可用
- [ ] 新建 / 编辑页实时预览卡会跟随标题、分类、日期、颜色和提醒状态变化
- [x] 新建 / 编辑页提醒提前天数为常用纸签加自定义输入，不再使用 0..3650 大滚轮
- [x] 生日、纪念日、倒数日模板会套用正确分类与重复规则
- [ ] 宋式命名色板在浅色 / 深色主题下均可读
- [x] 设置首页分类入口、折叠分组、字体弹窗和小组件配置区显示正常
- [ ] 设置页默认提醒纸签、自定义节点删除按钮、日程健康状态和开关无障碍语义正常
- [ ] 字体切换到 Noto Serif SC、ZCOOL XiaoWei、系统黑体、系统衬线、默认字体后无明显布局溢出
- [ ] 小组件添加配置页可保存 2x2、3x3、4x2 模板
- [ ] 小组件透明、半透明、宣纸、青瓷、朱印背景在桌面显示正常
- [ ] 小组件全透明和半透明玻璃模式在浅色 / 深色系统下文字都可读
- [ ] 小组件内容筛选、排序、密度、边框、圆角、文字对比和农历前缀配置生效
- [ ] 设置页默认小组件配置可以应用到已有小组件
- [ ] 新建 / 编辑 / 删除事件、提醒、系统日历同步和小组件刷新链路保持正常
- [ ] 启动页包含“拾光”品牌字标，动画不过长，普通图标与 Android themed icon 均可识别
- [ ] 通知深链在 Home、设置页、编辑页状态下都能回到 Home 并展示对应详情
- [ ] Play 渠道关于页不出现直接 APK 下载 / 安装入口
- [ ] Direct 渠道关于页检查更新仍可读取 GitHub Release

## 三、3.16 实测记录

- 已执行：在 `sdk_gphone16k_arm64` 模拟器安装 `glimmer-countdown-3-16.apk`，系统版本 Android `17` / API `37`。
- 已确认：安装包为 `versionName=3.16`、`versionCode=21`；首页、右上近期入口、设置页、新建事件日期 / 提醒区、权限弹窗、详情轻量主卡、分享预览、保存图片、系统分享面板、月历年月选择器均完成 smoke test。
- 已确认：Direct APK 包含 `REQUEST_INSTALL_PACKAGES`，Play APK 不包含 `REQUEST_INSTALL_PACKAGES`；`adb logcat -b crash -d` 未发现崩溃输出。
- GitHub Release：`https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v3.16`
- Release 资产：仅包含 `glimmer-countdown-3-16.apk`，大小 `26368278` bytes，GitHub 返回 digest `sha256:38c387dbdb02a239530cc77902b88c1d22ea761e9f9c7a3fa5eb87f0102f1d33`。
- 截图与 UI tree 暂存：`/tmp/timeapk-316-readiness/`。
- 未覆盖：真机、完整深浅主题矩阵、完整字体矩阵、真实定时通知触发、真实可写日历账户同步、桌面实际添加小组件并切换全部模板、Play 渠道关于页、Direct 渠道 GitHub 更新下载链路。

## 四、发布动作

- [x] 提交并 push 当前分支到 GitHub
- [x] 创建或更新 `v3.16` 标签
- [x] 在 GitHub Release 中发布 `glimmer-countdown-3-16.apk`
- [x] 以 `CHANGELOG.md` 中的 `3.16` 小节作为 Release Notes
- [x] 确认 GitHub Release 页面标题、标签、说明和资产均对应 `v3.16`
