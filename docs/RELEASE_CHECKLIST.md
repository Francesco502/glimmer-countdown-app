# 发布检查清单（v3.17）

**版本**：`3.17`（`versionCode=22`）  **发布日期**：`2026-07-08`

## 一、自动校验

- [x] `gradle.properties` 中 `VERSION_CODE=22`、`VERSION_NAME=3.17` 正确
- [x] `app/build.gradle.kts` 的版本读取、APK 重命名、Direct / Play flavor 与 BuildConfig 开关正常
- [x] `README.md`、`CHANGELOG.md` 与发布文档已切换到 `3.17`
- [x] `git diff --check` 未发现尾随空格或冲突标记
- [x] `./gradlew testDirectDebugUnitTest` 通过
- [x] `./gradlew testPlayDebugUnitTest` 通过
- [x] `./gradlew compileDirectDebugAndroidTestKotlin` 通过
- [x] `./gradlew lintDirectDebug lintDirectRelease lintPlayRelease lintVitalDirectRelease lintVitalPlayRelease` 通过且无未解释 warning
- [x] `./gradlew assembleDirectRelease assemblePlayRelease bundlePlayRelease` 通过
- [x] Direct APK 已输出到 `app/build/outputs/apk/direct/release/glimmer-countdown-3-17.apk`
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
- [x] 设置首页分类入口、折叠分组和独立小组件设置入口显示正常
- [ ] 字体弹窗显示正常
- [ ] 设置页默认提醒纸签、自定义节点删除按钮、日程健康状态和开关无障碍语义正常
- [ ] 字体切换到 Noto Serif SC、ZCOOL XiaoWei、系统黑体、系统衬线、默认字体后无明显布局溢出
- [x] 小组件默认配置页显示 1-5 格“预览宽度 / 预览高度”，折叠摘要包含尺寸、透明度、文字模式和密度
- [ ] 小组件添加配置页可保存多组 1-5 格预览宽高配置
- [ ] 小组件透明、半透明、宣纸、青瓷、朱印背景在桌面显示正常
- [ ] 小组件全透明和半透明玻璃模式在浅色 / 深色系统下文字都可读
- [ ] 小组件 25% 玻璃背景在实机桌面上接近系统原生乳白磨砂效果，圆角、边线和文字颜色不过硬
- [ ] 小组件内容筛选、排序、密度、边框、圆角、文字对比和农历前缀配置生效
- [x] 设置页默认小组件配置的“应用到已有小组件”入口位于默认配置顶部
- [ ] 已有小组件编辑会打开与新增小组件一致的配置页，不在列表内展开长表单
- [x] 新建事件标题可输入，允许通知和日历权限后，无可写系统日历时只提示且不会创建本地日历，事件保存后回到首页
- [ ] 编辑 / 删除事件、真实提醒触发、真实系统日历账户同步和小组件刷新链路保持正常
- [ ] 启动页包含“拾光”品牌字标，动画不过长，普通图标与 Android themed icon 均可识别
- [ ] 通知深链在 Home、设置页、编辑页状态下都能回到 Home 并展示对应详情
- [ ] Play 渠道关于页不出现直接 APK 下载 / 安装入口
- [x] Direct 渠道关于页检查更新仍可读取 GitHub Release，当前版本返回“已是最新版本”

## 三、3.17 实测记录

- 已执行：`testDirectDebugUnitTest`、`testPlayDebugUnitTest`、`compileDirectDebugAndroidTestKotlin`。
- 已执行：`lintDirectDebug lintDirectRelease lintPlayRelease lintVitalDirectRelease lintVitalPlayRelease`。
- 已执行：`assembleDirectRelease assemblePlayRelease bundlePlayRelease`。
- 已执行：`apksigner verify --verbose --print-certs` 检查 Direct / Play APK，均通过 v2 签名验证。
- 已执行：`aapt dump badging` 确认 Direct APK 为 `com.example.timeapk` / `versionCode=22` / `versionName=3.17`。
- 已执行：`aapt dump permissions` 确认 Direct APK 包含 `REQUEST_INSTALL_PACKAGES`，Play APK 不包含该权限。
- 已执行：安装 `glimmer-countdown-3-17.apk` 到模拟器后 smoke test：首页启动、设置入口、小组件设置、默认配置摘要、预览宽高文案、选项单选语义、新建标题输入、通知 / 日历权限请求、无可写系统日历提示、保存回首页。
- 已执行：Direct 渠道关于页“探寻新章”读取 GitHub Release 成功，当前 `3.17` 返回“已是最新版本”。
- GitHub Release：`https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v3.17`
- Release 资产：仅包含 `glimmer-countdown-3-17.apk`，大小 `26370758` bytes，GitHub 返回 digest `sha256:3319513689f7178306d593c90dd6ce16bb50533d495c0fbab9e2f755ec589c5c`。
- 截图与 UI tree 暂存：`/tmp/timeapk-release-home.png`、`/tmp/timeapk-release-widget-settings.png`、`/tmp/timeapk-release-widget-expanded.xml`。
- 未覆盖：真机、完整深浅主题矩阵、完整字体矩阵、真实定时通知触发、真实可写日历账户同步、桌面实际添加小组件并切换全部 1-5 格组合、Play 渠道关于页。

## 四、发布动作

- [x] 提交并 push 当前分支到 GitHub
- [x] 创建或更新 `v3.17` 标签
- [x] 在 GitHub Release 中发布 `glimmer-countdown-3-17.apk`
- [x] 以 `CHANGELOG.md` 中的 `3.17` 小节作为 Release Notes
- [x] 确认 GitHub Release 页面标题、标签、说明和资产均对应 `v3.17`

## Data Task 6 恢复验证（2026-07-16）

- [x] 农历重复、导入校验与重复数据回归：Direct / Play JVM 各 325 项通过；报告 `.superpowers/sdd/data-task-6-report.md`。
- [x] 日历权限撤销恢复 smoke：`emulator-5554` / API 37；撤权后保留 provider ownership 与可重试错误、阻止删除，恢复权限后由应用清理 CalendarProvider 并成功删除 Room 事件；证据 `/tmp/timeapk-data-task6-2026-07-16/rerun-682e004/`。
- [ ] Backup / restore smoke：模拟器 Backup Manager disabled，`Ancestral=0`、`Current=0`、`Ever backed up=0`，且没有真实 TimeAPK widget instance；未执行 `pm clear` / restore，不声称覆盖。

## Home / Widget Task 6 验证（2026-07-16）

- [x] 3.17 导出的 22 条脱敏事件 fixture；首页 Custom / ByDays / ByDate 与真实 Pixel Launcher `SORT_HOME` 小组件顺序一致，置顶 `Event 06`、`Event 03` 始终在前。
- [x] 两个真实小组件实例分别保持“全部事件 / 跟随首页”和“仅置顶 / 最近优先”配置；显式日期边界广播刷新两个 RemoteViews，下一次本地午夜 alarm 已布置。
- [ ] 筛选后的真实长按拖拽：ADB 三种手势均未触发 Compose reorder 回调，因此保持未验证；隐藏槽位合并规则由确定性单元测试与架构测试覆盖，不声称真实手势通过。
- 运行时报告：`.superpowers/sdd/home-task-6-report.md`；匿名证据：`/tmp/timeapk-home-widget-task6-2026-07-16-final/`。
