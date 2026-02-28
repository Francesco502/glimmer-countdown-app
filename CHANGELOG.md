# 更新日志

本文档记录拾光 (Glimmer) 各版本的更新内容。应用内「检查更新」弹窗中的「更新说明」来自 GitHub Release 的 body，可与本文件对应版本一致。

---

## [3.0] - 2025-02-28

### 重要修复与发布就绪

- **版本**：versionCode 4，versionName 3.0；ProGuard 规则补全（Room Entity/Dao、WorkManager、OkHttp、农历库等），Release 构建可安全发布
- **日期时区**：统一事件日期解析为 UTC，任意时区下日期显示正确；提醒调度（ReminderScheduler / MilestoneReminderScheduler）同步修正
- **通知**：普通提醒与里程碑提醒均使用应用图标 `ic_notification_small`，状态栏显示一致
- **编辑页**：保存按钮防重复点击；`saveEvent()` 异常捕获并返回失败状态，避免崩溃且失败时保留在编辑页
- **小组件**：深浅模式切换时自动刷新；列表项默认文字颜色使用主题属性，深色模式下无黑底黑字；删除未使用 PendingIntent；`widgetCategory="home_screen"`
- **详情页**：`isToday` / `isShowUntil` 与首页逻辑统一；已历文学化文案（`formatElapsedLiterary`）支持英文
- **首页**：`EventCard` / `EventListItem` 的「今天」判断统一为 `daysRemaining == 0 && !isPast`；移除 `EventListItem` 未用参数
- **其他**：OkHttp Response 使用 `use{}` 管理；`RECEIVE_BOOT_COMPLETED` 权限；GitHub README Slogan 更新为启动页「白驹过隙，拾光留痕」

---

## [2.1] - 2025-02-28

### 改进

- **宋代美学主题**：首页卡片默认配色改为淡雅绢本设色风格（秋香、退红、雨过天青、绢色、蟹壳青、月白、藕荷、淡墨、檀色）
- **启动页 Slogan**：增加了符合宋代美学的启动页 Slogan：“在时间的深海里，拾起一缕微光。”
- **卡片颜色自定义**：新建/编辑事件时支持自定义十六进制颜色，可通过调色板按钮输入任意颜色
- **绢本质感优化**：卡片透明度调整，更贴合绢本设色质感

---

## [2.0] - 2025-02-27

### 新增

- **应用内检查更新**：支持从 GitHub Release 检测新版本并下载安装
- **桌面小组件优化**：
  - 事件增删改后即时刷新
  - 已过 / 临近（7 天内）标签提示
  - 列表项圆角背景
  - 按天轮询（原为每小时）

### 改进

- **图标**：沙漏整体 0.7 倍缩放，顶底弧线修平为接近平直线
- **字体**：补全 14 个 Typography 样式，所有界面统一跟随设置中的字体（默认 / 衬线 / 手写 / 等宽 / 瘦金体）
- **顶部栏**：「拾光」标题随字体设置展示（如瘦金体）
- **小组件点击**：应用已打开时从小组件点击事件可正确跳转详情（`singleTop` + `onNewIntent`）

### 技术

- targetSdk 35
- 多渠道：direct（直装）、play（Play 商店）

---

## [1.0] - 初始版本

- 倒计时 / 纪念日管理
- 多语言（中 / 英）
- 主题切换（浅色 / 深色 / 跟随系统）
- 桌面小组件
- 提醒通知
- 导出 / 导入 JSON
