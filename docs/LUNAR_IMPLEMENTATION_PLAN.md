# 拾光 (Glimmer) 农历事件全链路支持实施方案

> 说明：本方案已在 **拾光 3.1 (versionCode 5)** 中全部落地，当前文件作为设计与实现说明，便于后续维护与回归测试使用。

**目标**：全面支持「每年按农历重复」的事件（如农历生日、传统节日），并提供优雅的公农历切换交互体验。

本方案旨在确保数据向后兼容、计算绝对准确，并且 UI 交互符合直觉。为了安全落地，实施过程分为四个阶段进行，并已全部完成。

---

## 阶段一：数据层与底层支撑改造

这是整个功能的地基，必须首先完成且保证不出错。

### 1. 数据库升级 (`Event` 实体与 Room 配置)
- **修改 `Event` 实体类** (`app/src/main/java/com/example/timeapk/data/Event.kt`)
  - 新增字段：`val isLunar: Boolean = false`。
  - *说明*：为了统一底层存储基准，无论是否农历，`date: Long` 始终存储公历 UTC 午夜毫秒数。`isLunar` 仅作为“该事件在重复或展示时应按农历逻辑解析”的标志。
- **配置 Room Migration** (`app/src/main/java/com/example/timeapk/data/AppDatabase.kt`)
  - 增加数据库版本号（如：从 1 升至 2 或在当前基础上 +1）。
  - 编写 `Migration` 逻辑：`database.execSQL("ALTER TABLE events ADD COLUMN isLunar INTEGER NOT NULL DEFAULT 0")`，确保老用户平滑升级不丢数据。

### 2. 构建核心农历计算工具类 (`LunarEventUtils.kt`)
- 依赖 `cn.6tail.lunar`（项目中已引入）。
- **编写功能函数**：
  - `getNextLunarOccurrence(originSolarDate: LocalDate, today: LocalDate): LocalDate`
    - *逻辑*：将原始公历 `originSolarDate` 转为 `Lunar`，提取其农历月、日（及是否为闰月）。然后构造今年的同月同日 `Lunar` 并转回公历 `Solar`。若该公历日已早于 `today`，则推算明年的该农历日。
  - `getLunarElapsedPeriod(originSolarDate: LocalDate, today: LocalDate): Period`
    - *逻辑*：专用于农历事件“已历多少年”的推算，避免简单使用公历差值导致的年份边界偏差。
  - `formatLunarDateString(solarDate: LocalDate): String`
    - *逻辑*：将底层公历时间渲染为符合中国人阅读习惯的字符串，例如“岁次 甲申 腊月 初八”，统一用于事件录入与详情页展示。

---

## 阶段二：核心计算层与 UI 状态层改造

使应用的 ViewModel 和全局状态感知并正确计算农历。

### 3. 改造 ViewModel 倒计时逻辑 (`HomeViewModel.kt`)
- 修改 `toEventUiState()` 扩展函数中的时间推算逻辑。
- **判断逻辑**：
  - 如果 `event.isLunar == true` 且 `event.repeatType == REPEAT_YEARLY`：
    - 不再调用现有的公历 `nextOccurrenceDate`，而是调用阶段一写的 `getNextLunarOccurrence` 算出下一个准确的公历日期。
    - 用新算出的公历日期减去 `LocalDate.now()` 计算出 `daysRemaining`（剩余天数）。
    - 针对已过去时间的计算（`daysPassed` 等），切换为农历年份差值逻辑。

### 4. 改进展示文案工具 (`DisplayModeUtils.kt` & `RepeatDetailHelper.kt`)
- **倒计时标签** (`getUntilLabel`):
  - 若为农历重复事件，需要在标签上明确标识，如返回 `context.getString(R.string.until_lunar_birthday_label, years)`（显示“距离 农历X岁生日”）。
- **详情页六行展示** (`formatLunarLine` / `formatDateWithWeekday`):
  - 若 `isLunar == true`，将首行“缘起”显示为大字号的农历日期，公历作为副标题或隐藏，强化农历属性。

---

## 阶段三：UI 与交互改造（重点突破）

这一阶段直接面向用户，特别是日期选择器的重构复杂度较高。

### 5. 重构 `BottomSheetDatePicker` 支持双模
- **UI 结构调整**：
  - 顶部操作区下方，增加一个 `SegmentedButton`（分段按钮）或自定义 Tab，显示【公历】与【农历】。
- **状态流转**：
  - 内部维护状态 `isLunarMode: Boolean`。
  - 切换模式时，利用 `Lunar` 库将当前的 `selectedDate` 在公农历之间无缝换算，保持选择器日期一致。
- **农历模式的滚轮与输入框**：
  - **年滚轮**：依然是数字（1900-2100）。
  - **月滚轮**：替换为中文字符串数组 `["正月", "二月", ..., "腊月"]`。
    - *难点预警*：由于农历存在闰月（如“闰四月”），当滚轮滚动到某一年时，必须动态计算该年是否有闰月，并重新生成月份数组（长度变为 13）。
  - **日滚轮**：替换为 `["初一", "初二", ..., "三十"]`（当月仅29天时自动裁剪）。
- **回调接口升级**：
  - `onConfirm: (millis: Long, isLunar: Boolean) -> Unit`。

### 6. 改造新建/编辑页面 (`EventEntryScreen.kt`)
- 接收并存储 `isLunar` 状态到 `EventDetails` 承载类。
- 日期输入框的显示逻辑：
  - `if (eventDetails.isLunar) formatLunarDateString(...) else formatSolarDateString(...)`。
- 点击保存时，确保 `isLunar` 字段一并写入 ViewModel 和数据库。

---

## 阶段四：边缘功能与测试回归

### 7. 桌面小组件适配 (`CountdownWidgetService.kt`)
- 依赖于阶段二的 `EventUiState`，由于倒计时天数已被正确计算并传递给小组件，小组件在此只需要进行**UI层面的微调**。
- 如果事件判定为 `isLunar`，在事件标题旁或下方标签中加上“农历”标识符（`[农]`），以防用户混淆。

### 8. 提醒与通知服务刷新 (`NotificationWorker.kt`)
- 原有的通知往往基于公历固定时间差注册。因为农历对应的公历日期每年都在变化，所以在触发提醒后，不能简单地 `+ 1年` 来复用通知。
- **改造点**：每次计算下一次提醒时间时，如果判定为农历事件，必须调用 `getNextLunarOccurrence` 推算出下一年的确切公历日期，再向 `WorkManager` 或 `AlarmManager` 注册新的延时任务。

### 9. 数据导入导出兼容 (`JsonExportImport.kt`)
- 修改 JSON 序列化逻辑，确保 `isLunar` 属性被正确导出（向后兼容：读取旧版本 JSON 时如果缺失该字段，默认视作 `false`）。

---

## 实施建议与检查清单

在正式编写代码前，建议按以下顺序建立分支并提交：
1. **[底层]** 提交：完成 Room 升级与 `LunarEventUtils` 工具类，并编写针对闰年闰月的 Unit Test。
2. **[中层]** 提交：完成 ViewModel 与 `DisplayModeUtils` 改造，确保现有公历事件不受影响，硬编码一个农历事件确保倒计时正确。
3. **[UI层]** 提交：完成 `BottomSheetDatePicker` 的双模切换与滚轮动态刷新（最容易出 Bug 的地方，尤其是闰月映射）。
4. **[收尾]** 提交：完成 Widget、通知调度与导出导入的适配，进行全量回归测试。