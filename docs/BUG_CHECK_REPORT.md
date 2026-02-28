# 功能与 UI 二次检查报告

## 本次已修复

### 1. 编辑页加载失败无反馈
- **问题**：从详情/Widget 进入编辑时，若事件已被删除，`loadEvent` 得到 `null`，界面仍显示空表单，用户易误以为在新建。
- **修复**：在 `EventEntryUiState` 中增加 `loadError`；加载失败时置为 `true`，`EventEntryScreen` 内用 `LaunchedEffect(loadError)` 弹出 Snackbar「事件不存在或已删除」并约 1.2 秒后自动返回上一页。

### 2. 文件导入 MIME 类型过严
- **问题**：导入从文件选择改为仅 `application/json` 后，部分导出为「纯文本」的 .txt 文件无法在文件选择器中看到。
- **修复**：将启动文件选择器的 MIME 改为 `text/*`，既保留 JSON，也支持 .txt 等文本文件；导入逻辑仍按 JSON 解析，无效内容会提示「导入失败」。

---

## 可选优化（已全部实现）

### 1. 通知小图标 ✅
- **实现**：新增仅白/透明通道的 `res/drawable/ic_notification_small.xml`（沙漏轮廓），`MilestoneReminderWorker` 改为使用 `R.drawable.ic_notification_small`，状态栏显示更稳定。

### 2. 详情页 / 小组件使用自定义里程碑 ✅
- **实现**：`TimeApp.kt` 的 Detail 路由中通过 `app.userPrefs.customMilestonesFlow.collectAsState(initial = DEFAULT_MILESTONE_DAYS)` 获取里程碑并传入 `event?.toEventUiState(milestones)`；`CountdownWidgetService.onDataSetChanged()` 内用 `app.userPrefs.customMilestonesFlow.first()` 取里程碑并传入 `toEventUiState(milestones)`，与首页一致。

### 3. 未来非重复事件的里程碑语义 ✅
- **实现**：在 `HomeViewModel` 中新增 `computeNextCountdownMilestone(milestones, daysRemaining)`，对「未来 + 非重复」事件按剩余天数倒计时：取「≤ 当前剩余天数」的最大里程碑为下一节点，`nextMilestoneDays = daysRemaining - 该节点`，语义为「还有 X 天到达 Y 天倒计时节点」。

### 4. 首页 `today` 回到前台时刷新 ✅
- **实现**：在 `HomeScreen` 中维护 `var today by remember { mutableStateOf(LocalDate.now()) }`，通过 `LocalLifecycleOwner` + `DisposableEffect` 监听 `Lifecycle.Event.ON_START`，在回到前台时执行 `today = LocalDate.now()`；将 `today` 作为参数传入 `EventCard` 与 `EventListItem`，跨午夜后从后台切回时天数会更新。

---

## 检查结论

- **核心功能**：事件增删改、重复规则、纪念日/生日展示、里程碑提醒调度、撤销恢复、导入导出、设置与语言切换等逻辑正确，无新发现的功能性 Bug。
- **UI 行为**：删除确认、编辑页校验与错误提示、启动页与设置页多语言、列表/卡片高度与溢出处理已按预期工作。
- **本次修改**：修复「编辑页加载失败无反馈」和「文件导入 MIME 过严」两处；其余为可选优化，不影响主流程。
