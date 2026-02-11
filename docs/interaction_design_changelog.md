# 交互设计优化说明 (Interaction Design)

基于 interaction-design 技能，对 TimeAPK 全应用做了统一的动效与反馈优化。

## 1. 动效规范 (AnimationSpecs)

在 `ui/theme/AnimationSpecs.kt` 中集中定义：

- **时长**：微反馈 150ms、小过渡 250ms、页面/内容 350ms
- **缓动**：入场用 EaseOut，出场用 EaseIn
- **弹簧**：按钮/FAB/卡片用 `springButton`，列表项位置用 `springItemPlacement`（IntOffset）

便于全应用统一节奏，并方便后续支持「减少动效」等无障碍。

## 2. 导航 (TimeApp)

- **过渡**：页面切换使用「Fade Through」—— 淡入淡出 + 轻微上下位移（约 1/4 屏），时长 350ms
- **效果**：符合设计稿中的「胶片切换」感，避免生硬滑入滑出

## 3. 首页 (HomeScreen)

- **FAB**：按下时缩放到 0.94，释放后执行导航；空列表时仍保留 1.08 的强调缩放
- **事件卡片 (EventCard)**：按下缩放到 0.98，弹簧动画，无额外水波纹（避免与缩放重复）
- **列表**：有/无数据切换用 `AnimatedContent` 做淡入 + 上滑；单条用 `AnimatedVisibility` 淡入 + 轻滑；`animateItemPlacement` 在排序/过滤变化时平滑移动
- **空状态**：从「空 ↔ 有列表」有明确过渡，避免闪跳

## 4. 详情页 (DetailScreen)

- **加载**：`eventState == null` 时显示 `CircularProgressIndicator`，不再只显示标题文字
- **内容入场**：主内容用 `AnimatedVisibility` 淡入 + 轻微上滑（1/8 屏）
- **操作按钮**：编辑 / 删除 / 分享 三个按钮按下缩放到 0.96，弹簧反馈

## 5. 事件编辑页 (EventEntryScreen)

- **保存按钮**：按下缩放到 0.98，弹簧动画
- **颜色色块 (ColorChip)**：每个色块按下缩放到 0.9，选中态保留边框

## 6. 设置页 (SettingsScreen)

- **可点击行**：导出 JSON/CSV/纯文本、导入等行，按下整行缩放到 0.98，释放后执行操作

## 7. 原则对照

| 原则           | 实现说明 |
|----------------|----------|
| 有目的的动效   | 按压/入场/页面切换都有明确反馈或引导 |
| 时长分级       | 微 150ms、小 250ms、中 350ms 集中管理 |
| 弹性/自然      | 按钮与卡片用 spring，避免线性生硬 |
| 性能           | 使用 `graphicsLayer` 做 scale，保证 60fps |
| 一致性         | 全应用通过 `AnimationSpecs` 统一时长与曲线 |

后续若需支持「减少动效」（如 `prefers-reduced-motion`），可在 `AnimationSpecs` 中根据系统/设置将时长缩到近 0 或跳过动画。
