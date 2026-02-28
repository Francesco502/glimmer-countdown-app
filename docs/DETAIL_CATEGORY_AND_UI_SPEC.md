# 启动页、分类与详情模板改动实施文档

## 1. 修改目标总览

| 序号 | 目标 | 说明 |
|------|------|------|
| 1 | 启动页文案位置 | 将启动页文字置于屏幕靠下部分 |
| 2 | 恢复事件分类选项 | 新增/编辑时可选：生日、纪念日、其他；详情按分类展示不同模板 |
| 3 | 详情卡片提醒标识 | 在详细卡片中增加小图标，提示当前事件是否设置了提醒 |

---

## 2. 目标一：启动页文字靠下

### 2.1 现状

- `SplashScreen.kt` 使用 `Box(contentAlignment = Alignment.Center)`，图标与中英文两行文案整体垂直居中。
- 文案为：「在时间的深海里，拾起一缕微光。」与 "Captured light in the deep ocean of time."

### 2.2 目标

- 将**文字部分**置于屏幕**靠下**区域，图标可保持偏上或整体重新排版，使文案在视觉上位于屏幕下半部分（例如图标在上、文案在下方固定间距或距底部一定 padding）。

### 2.3 实施要点

- 修改 `SplashScreen` 布局：由单列居中改为例如 `Column` 上部分留空/放 Logo，下部分 `Spacer` + 文案，或使用 `Alignment.BottomCenter` 放置文案区域并加 `padding(bottom = 48.dp)` 等。
- 不改变文案内容与淡入淡出逻辑，仅调整布局约束与对齐方式。

### 2.4 涉及文件

- `app/src/main/java/com/example/timeapk/ui/splash/SplashScreen.kt`

---

## 3. 目标二：恢复分类选项与按分类的详情模板

### 3.1 现状

- `Event` 已有 `category: String` 字段；新增/编辑页**未**展示分类选择（此前已移除）。
- 详情页未按 `category` 区分展示，仅按 `repeatType` 显示「缘起｜已历｜静候」六行（重复事件时）。

### 3.2 目标

- **新增/编辑事件**：恢复分类选择，可选 **生日、纪念日、其他** 三类（与现有 `category` 字段对应，建议常量如 `CATEGORY_BIRTHDAY` / `CATEGORY_ANNIVERSARY` / `CATEGORY_OTHER`）。
- **详情页**：在保留当前「剩余/已经」日期主模块的前提下，按分类展示不同**模板**：
  - **纪念日**：对应现有「缘起｜已历｜静候」六行区块（与重复事件逻辑可共存：纪念日且重复时展示该区块）。
  - **生日**：根据生日日期展示：农历日期、岁数、属相、生辰八字、八字对应五行属性、星座。
  - **其他**：仅显示公历日期及对应的农历日期。

### 3.3 实施要点

#### 3.3.1 编辑页恢复分类

- 在 `EventEntryScreen` / `EventInputForm` 中增加分类选择 UI（如单选：生日 / 纪念日 / 其他）。
- `EventDetails` / `EventEntryViewModel` 中确保 `category` 被正确读写；新建事件默认值建议为「其他」或「纪念日」。
- 若已有 `category_birthday`、`category_anniversary`、`category_other` 等 string，可直接使用；否则在 values/values-zh/values-en 中补充。

#### 3.3.2 详情页按分类的模板

- **公共部分**（所有分类保留）：顶部日期、标题、当前「剩余/已经 X 天」或「X年X月X天」主模块（含点击切换）、下方备注等。
- **纪念日**：在公共部分之下展示「缘起｜已历｜静候」六行区块（可与 `repeatType != REPEAT_NONE` 组合：例如纪念日且为重复事件时显示；若希望“纪念日”即显示则按 category 判断）。
- **生日**：在公共部分之下增加生日专属区块，包含：
  - 农历日期（可由现有 `RepeatDetailHelper` 或 lunar 库提供）
  - 岁数（根据生日与今日计算周岁）
  - 属相（由农历年生肖）
  - 生辰八字（年柱、月柱、日柱、时柱；若未存时间则仅年月日三柱或提示“未设置时辰”）
  - 八字对应的五行属性（如金木水火土分布）
  - 星座（由公历月日计算）
- **其他**：在公共部分之下仅显示一行或两行：公历日期 + 对应农历日期（无缘起已历静候、无生日扩展信息）。

#### 3.3.3 数据与依赖

- 生辰八字、五行、属相、星座可依赖现有 `cn.6tail:lunar` 或扩展工具类（如 `LunarHelper` / `RepeatDetailHelper` 的扩展）。若库不支持八字，需查阅 6tail 文档或引入/实现简易八字换算。
- 岁数 = 当前年份 - 出生年份（必要时按是否过生日调整周岁）。

### 3.4 涉及文件

- `app/src/main/java/com/example/timeapk/ui/event/EventEntryScreen.kt`（表单中增加分类选择）
- `app/src/main/java/com/example/timeapk/ui/event/EventEntryViewModel.kt`（category 的默认值与保存）
- `app/src/main/java/com/example/timeapk/ui/detail/DetailScreen.kt`（按 `event.category` 分支展示不同模板）
- `app/src/main/java/com/example/timeapk/ui/utils/RepeatDetailHelper.kt` 或新建生日/八字相关 Helper（农历、属相、八字、五行、星座）
- `app/src/main/res/values/strings.xml`、`values-zh`、`values-en`（分类标签、生日区块标签如“农历”“岁数”“属相”“八字”“五行”“星座”等）

### 3.5 分类常量建议

- 在 `Event.kt` 或单独 constants 文件中定义：
  - `CATEGORY_BIRTHDAY = "birthday"`
  - `CATEGORY_ANNIVERSARY = "anniversary"`
  - `CATEGORY_OTHER = "other"`
- 与现有 `Event.category` 持久化一致；旧数据若为空或未知值，详情页可回退为「其他」模板。

---

## 4. 目标三：详情卡片提醒图标

### 4.1 目标

- 在**详细卡片**（详情页的主卡片区域）中增加一个**小图标**，用于提示当前事件**是否已设置提醒**（即 `event.remindEnabled == true`）。
- 不改变现有提醒逻辑，仅增加视觉标识（如铃铛图标：开启提醒显示高亮/实心，未开启显示灰色或不出镜，由产品决定）。

### 4.2 实施要点

- 在 `DetailScreen.kt` 中，在卡片内部合适位置（例如顶部日期行右侧、或标题行右侧）增加一个小图标（如 `Icons.Default.Notifications` / `NotificationsActive`）。
- 根据 `eventState.event.remindEnabled` 控制图标显示与样式（颜色/透明度/描边与填充）。
- 可选：为图标提供 contentDescription（如“已设置提醒”/“未设置提醒”），便于无障碍。

### 4.3 涉及文件

- `app/src/main/java/com/example/timeapk/ui/detail/DetailScreen.kt`
- 若使用 Material Icons，通常无需新增资源；如需本地图标，可在 `res/drawable` 增加。

---

## 5. 实施优先级建议（已实施）

| 优先级 | 项目 | 依赖 | 说明 | 状态 |
|--------|------|------|------|------|
| P0 | 启动页文案靠下 | 无 | 改动集中、风险低 | ✅ |
| P0 | 详情卡片提醒图标 | 无 | 仅读 `remindEnabled`，逻辑简单 | ✅ |
| P1 | 编辑页恢复分类选择 | 无 | 恢复 UI + 写回 `category` | ✅ |
| P2 | 详情「其他」模板 | 无 | 仅公历+农历一行，可复用现有农历工具 | ✅ |
| P2 | 详情「纪念日」模板 | 无 | 与现有缘起已历静候区块对接，按 category 显示 | ✅ |
| P3 | 详情「生日」模板 | 农历/八字/星座工具 | 需农历、属相、八字、五行、星座计算与展示 | ✅ |

---

## 6. 验收要点

- **启动页**：文案在屏幕靠下位置，动画与跳转逻辑不变。
- **编辑页**：可选生日、纪念日、其他；保存后 `Event.category` 正确持久化。
- **详情页**：  
  - 纪念日：有「缘起｜已历｜静候」六行区块（与现有逻辑一致或按 category 触发）。  
  - 生日：展示农历、岁数、属相、八字、五行、星座。  
  - 其他：仅公历 + 农历。  
  - 所有分类均保留「剩余/已经」主模块。  
- **提醒图标**：详情页卡片上可见提醒状态图标，且与 `remindEnabled` 一致。

---

## 7. 附录：现有相关资源

- 事件模型：`Event.kt`（含 `category`, `remindEnabled`）。
- 详情「缘起｜已历｜静候」：`DetailScreen.kt` 中 `isRepeating` 区块；`RepeatDetailHelper.kt` 提供农历、干支、下一发生日等。
- 农历/八字：已引入 `cn.6tail:lunar`；可查 6tail 文档获取生肖、八字、五行等 API（若支持）。
- 字符串：`category_birthday`、`category_anniversary`、`category_other` 已存在于 values/values-zh/values-en。
