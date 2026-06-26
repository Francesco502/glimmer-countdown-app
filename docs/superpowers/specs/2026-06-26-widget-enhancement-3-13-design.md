# 3.13 桌面小组件增强设计

## 背景

拾光 3.12 已具备表格式桌面小组件，当前实现基于 Android 原生 `AppWidgetProvider`、`RemoteViewsService` 与 XML 布局。已有能力包括事件列表展示、点击打开详情、滚动列表、主题跟随系统、动态配色、圆角背景、字号缩放、置顶顺序和数据变更后的主动刷新。

3.13 的目标是把小组件从“统一样式的信息列表”升级为“可独立配置的桌面看板”。用户可以同时放置多个小组件，例如一个 2x2 透明小组件只显示置顶事件，一个 3x3 标准小组件显示全部未来事件，一个 4x2 横向小组件用于横屏或平板桌面。

## 目标

- 支持全局默认小组件设置，并允许每个桌面小组件独立覆盖。
- 支持透明、半透明、纯色、跟随系统和玻璃感等外观预设。
- 支持背景透明度、边框、圆角、显示密度、内容范围和排序配置。
- 支持 2x2、3x3、4x2 等尺寸模板，并在用户手动缩放后根据实际宽高自适应。
- 保持现有小组件刷新、列表点击、空状态和系统主题切换的稳定性。
- 提供清晰的测试边界，避免 RemoteViews 缓存、尺寸变化和多小组件配置互相污染。

## 非目标

- 不实现真正读取桌面壁纸并实时模糊的毛玻璃效果。Android 原生小组件无法稳定获得宿主壁纸内容，3.13 的“玻璃感”定义为半透明背景、高光层和低透明描边。
- 不引入 Glance 或 Compose 小组件重写。3.13 延续当前 RemoteViews 架构，降低回归风险。
- 不在 3.13 实现无限制自定义 CSS 式样式。配置项保持离散枚举和有限档位，便于测试和兼容。
- 不承诺所有 Launcher 都精确以相同 dp 尺寸渲染 2x2、3x3、4x2。应用提供默认模板和自适应策略，最终尺寸仍由系统桌面宿主管理。

## 用户体验

### 添加小组件

用户从系统桌面添加拾光小组件时，进入小组件配置页。配置页展示：

- 尺寸模板：2x2 紧凑、3x3 标准、4x2 横向。
- 外观预设：跟随系统、纯色、半透明、全透明、玻璃感。
- 内容范围：全部事件、仅置顶、仅未来、仅生日纪念日。
- 排序方式：跟随首页、置顶优先、最近优先。
- 显示密度：紧凑、标准、宽松。
- 预览区域：用应用内 Compose 模拟小组件效果。

保存后，该配置绑定到当前 `appWidgetId`。如果用户取消配置，则不创建小组件或使用系统默认取消流程。

接入方式是在 `countdown_widget_info.xml` 和 `xml-v31/countdown_widget_info.xml` 中声明 `android:configure`，指向小组件配置 Activity。配置 Activity 从 `AppWidgetManager.EXTRA_APPWIDGET_ID` 读取实例 ID，保存配置后设置 `RESULT_OK` 并触发该小组件刷新。

### 设置页

设置页保留“全局小组件默认设置”。这些默认值用于新添加的小组件，不强制覆盖已经放在桌面的旧小组件。

设置页还提供“应用到全部已有小组件”操作。该操作需要二次确认，因为它会覆盖每个 `appWidgetId` 的独立配置。

### 已有小组件编辑

点击小组件空白区域仍默认打开应用首页。小组件配置入口放在应用设置中，以“管理桌面小组件”进入，列出当前活跃小组件：

- 小组件 ID 或显示名称，例如“小组件 1 / 2x2 / 仅置顶”。
- 当前模板、外观和内容范围摘要。
- 编辑、复制为默认、恢复默认、清理实例配置。

如果系统 Launcher 不支持从小组件直接进入配置页，应用内管理页仍可覆盖主要编辑场景。

## 配置模型

新增 `WidgetConfig` 模型，分为全局默认配置和实例配置。

```kotlin
data class WidgetConfig(
    val version: Int = 1,
    val sizeTemplate: Int = SIZE_TEMPLATE_2X2,
    val appearancePreset: Int = APPEARANCE_SYSTEM,
    val backgroundOpacityPercent: Int = 75,
    val borderMode: Int = BORDER_AUTO,
    val cornerMode: Int = CORNER_SYSTEM,
    val densityMode: Int = DENSITY_STANDARD,
    val contentScope: Int = CONTENT_ALL,
    val sortMode: Int = SORT_HOME,
    val showLunarPrefix: Boolean = true,
    val contrastMode: Int = CONTRAST_AUTO,
    val fontScale: Float = 1.0f
)
```

建议常量：

- `SIZE_TEMPLATE_2X2`、`SIZE_TEMPLATE_3X3`、`SIZE_TEMPLATE_4X2`
- `APPEARANCE_SYSTEM`、`APPEARANCE_SOLID`、`APPEARANCE_TRANSLUCENT`、`APPEARANCE_TRANSPARENT`、`APPEARANCE_GLASS`
- `BORDER_AUTO`、`BORDER_ON`、`BORDER_OFF`
- `CORNER_SYSTEM`、`CORNER_SMALL`、`CORNER_MEDIUM`、`CORNER_LARGE`
- `DENSITY_COMPACT`、`DENSITY_STANDARD`、`DENSITY_COMFORTABLE`
- `CONTENT_ALL`、`CONTENT_PINNED`、`CONTENT_FUTURE`、`CONTENT_BIRTHDAY`
- `SORT_HOME`、`SORT_PINNED_FIRST`、`SORT_NEAREST_FIRST`
- `CONTRAST_AUTO`、`CONTRAST_LIGHT_TEXT`、`CONTRAST_DARK_TEXT`

### 存储

继续使用 DataStore Preferences，不做数据库迁移。

新增键：

- `widget_default_config_json`
- `widget_instance_configs_json`

`widget_instance_configs_json` 存储为 JSON 对象，key 为 `appWidgetId` 字符串，value 为 `WidgetConfig` JSON。

读取逻辑：

1. 先读取 `widget_instance_configs_json[appWidgetId]`。
2. 如果不存在，读取 `widget_default_config_json`。
3. 如果默认配置不存在或解析失败，使用内置默认配置。
4. 如果配置版本旧于当前版本，按字段级默认值补齐。

写入逻辑：

- 新增小组件时写入该 `appWidgetId` 的实例配置。
- 修改全局默认设置时只更新默认配置。
- 用户选择“应用到全部已有小组件”时批量覆盖实例配置。
- `onDeleted` 收到系统删除事件时清理对应 `appWidgetId` 配置。

## 渲染架构

### 当前结构延续

保留现有模块职责：

- `CountdownAppWidgetProvider`：接收更新、尺寸变化、删除事件，创建根 RemoteViews。
- `CountdownWidgetService`：提供列表行 RemoteViews。
- `WidgetContentResolver`：读取事件、偏好和配置，生成小组件条目。
- `WidgetStylePolicy`：按尺寸、密度和字号生成文字与间距策略。
- `WidgetThemeResolver`：解析系统深浅色、动态配色和小组件配置后的颜色快照。

### 新增模块

- `WidgetConfigRepository`：封装 DataStore 读写、JSON 解析、默认值和清理。
- `WidgetConfigModels`：集中定义配置 data class、常量和 sanitize 逻辑。
- `WidgetRenderPolicy`：把 `WidgetConfig + WidgetSizeClass + WidgetThemeSnapshot` 解析成背景、文字、边框、圆角和列表样式。
- `WidgetConfigActivity` 或现有 `MainActivity` 深链入口：承载添加小组件配置页。
- `WidgetManagementScreen`：应用内管理已有小组件配置。

## 尺寸策略

### 模板与系统尺寸

Android 小组件尺寸由桌面宿主控制。3.13 提供模板作为默认体验：

- 2x2 紧凑：默认入口，适合只显示 2-4 条事件。
- 3x3 标准：适合显示 5-8 条事件和更完整文案。
- 4x2 横向：适合横向桌面区域，强化标题和值的并排阅读。

`countdown_widget_info.xml` 继续保留 `resizeMode="horizontal|vertical"`，`xml-v31` 提供默认 `targetCellWidth="2"`、`targetCellHeight="2"`。如需在系统小组件选择器里出现多个尺寸入口，可新增多个 `appwidget-provider` 与 receiver；3.13 首版建议先用一个 provider 加配置页模板，避免 Manifest 和刷新链路成倍增加。

### 宽高双维度分档

当前 `WidgetSizeBucket.resolve` 主要看宽度。3.13 改为宽高双维度：

- `COMPACT_SQUARE`：接近 2x2。
- `STANDARD_SQUARE`：接近 3x3。
- `WIDE_SHORT`：接近 4x2。
- `TALL`：竖向拉高的小组件。

输入来自：

- `OPTION_APPWIDGET_MIN_WIDTH`
- `OPTION_APPWIDGET_MAX_WIDTH`
- `OPTION_APPWIDGET_MIN_HEIGHT`
- `OPTION_APPWIDGET_MAX_HEIGHT`

样式策略基于尺寸分档、模板和密度共同决定：

- 字号。
- 每行 padding。
- divider 高度。
- 最大显示文案长度。
- 空状态字号。
- 是否使用更短语义文案。

## 外观策略

### 背景

背景仍使用 XML drawable 和 RemoteViews 支持的 API。推荐做法是准备有限背景资源，并由 provider 根据配置选择：

- 系统样式：沿用当前主题属性和动态配色。
- 纯色：使用应用主题衍生色，透明度 100%。
- 半透明：使用 50% 或 75% alpha 的浅/深色容器。
- 全透明：背景透明，保留可选文字对比保护。
- 玻璃感：半透明容器 + 顶部高光渐变 + 自动描边。

由于 RemoteViews 对运行时动态 drawable 的支持有限，透明度优先采用离散档位：

- 0%
- 25%
- 50%
- 75%
- 100%

### 边框与圆角

边框：

- 自动：透明度低于 75% 时显示低透明描边；纯色或系统背景下按主题决定。
- 开：始终显示描边。
- 关：不显示描边。

圆角：

- 跟随系统：Android 12+ 使用系统小组件圆角，旧版本使用 `widget_background_radius`。
- 小：12dp。
- 中：20dp。
- 大：28dp。

### 文字对比

透明背景无法知道桌面壁纸真实颜色，因此对比保护采用保守策略：

- 透明度 0% 或 25%：默认高对比文字，允许用户手动选择浅色或深色。
- 透明度 50%：根据系统深浅色选择文字，并增加 accent 色限制。
- 透明度 75% 或 100%：沿用主题文字色。

`CONTRAST_AUTO` 需要避免使用过浅 accent 色显示天数。对于浅色背景使用深色 accent，对于暗色背景使用明亮但不刺眼的 accent。

## 内容策略

### 内容范围

`WidgetContentResolver` 在读取事件后应用内容范围：

- 全部事件：保持当前行为。
- 仅置顶：只显示 pinned 事件；无置顶时显示空状态。
- 仅未来：过滤已过去且非今天事件。
- 仅生日纪念日：根据现有事件类型或分类字段过滤；如果当前数据模型没有稳定分类字段，则先支持生日事件，普通纪念日维持在全部事件中。

### 排序

- 跟随首页：复用首页自定义顺序和置顶顺序；未出现在自定义顺序中的事件按小组件默认排序补齐。
- 置顶优先：置顶事件按用户置顶顺序，其余按当前小组件默认排序。
- 最近优先：未来事件按剩余天数升序，过去事件放后。

### 显示密度

显示密度影响行距、padding 和可读文案长度，不直接改变用户设置的小组件字号基准。字号由 `fontScale` 决定，密度只改变空间分配。

### 小尺寸文案

紧凑尺寸优先使用短文案：

- 今天
- +12天
- -8天
- 还有3月
- 已2年

标准和大尺寸继续显示完整语义文案：

- 还有 12 天
- 已经 8 天
- 还有 1 年 2 个月

## 交互与刷新

### PendingIntent

列表项点击继续打开事件详情。根区域点击打开应用首页。配置页入口不放在小组件内部，避免和列表点击冲突，也避免占用有限空间。

### RemoteViews 缓存 key

`createRemoteAdapterIntent` 的 data URI 必须包含配置摘要：

- `appWidgetId`
- 宽高尺寸分档
- 外观配置版本或 hash
- 内容范围
- 排序方式
- 字号和密度
- 系统主题 key

这样可以避免多个小组件使用同一个 RemoteViews 工厂缓存，导致样式或内容互相串扰。

### 刷新触发

保持现有触发点：

- 事件新增、编辑、删除。
- 置顶或排序变化。
- 小组件尺寸变化。
- 系统深浅色变化。
- 应用主题或字体设置变化。

新增触发点：

- 实例配置保存。
- 全局默认配置应用到全部实例。
- 小组件删除后的配置清理。

## 兼容性

- Android 12+ 使用系统小组件圆角和动态颜色。
- Android 12 以下使用应用内定义的颜色、圆角和透明资源。
- 部分 Launcher 对透明小组件的背景处理不同，测试以“不崩溃、不空白、文字可读”为底线。
- 如果小组件配置丢失或 JSON 损坏，回退到默认配置并刷新，不阻断小组件显示。

## 测试计划

### 单元测试

新增或扩展：

- `WidgetConfigRepositoryTest`：默认配置、实例覆盖、坏 JSON 回退、删除清理。
- `WidgetConfigModelsTest`：枚举 sanitize、透明度档位、版本补齐。
- `WidgetSizeBucketTest`：宽高双维度分档，覆盖 2x2、3x3、4x2、竖向拉高。
- `WidgetStylePolicyTest`：尺寸 + 密度 + 字号组合不越界。
- `WidgetThemeResolverTest`：透明度、预设、边框、对比模式解析。
- `WidgetContentResolverTest`：内容范围、排序、置顶过滤、空状态。

### 手动回归

- 添加 2x2、3x3、4x2 小组件并保存不同配置。
- 同时存在多个小组件时，外观和内容互不影响。
- 修改事件后所有小组件刷新。
- 修改某一个小组件配置后只影响该实例。
- 系统深浅色切换后小组件文字和背景可读。
- 桌面手动拉伸后布局不重叠、不严重裁切。
- 删除小组件后配置清理，重新添加不会误用旧 `appWidgetId` 配置。
- 无事件、无置顶、仅未来无结果等空状态正常。

## 发布拆分

3.13 建议分四个实现阶段，每个阶段都可独立验证：

1. 配置基础设施：新增 `WidgetConfig`、DataStore 存储、默认与实例读取、删除清理、缓存 key。
2. 外观渲染：背景预设、透明度、边框、圆角、对比保护。
3. 内容与尺寸：内容范围、排序、显示密度、宽高分档、2x2/3x3/4x2 模板。
4. 配置 UI：添加小组件配置页、设置页默认配置、已有小组件管理页、预览与文案。

如果工期需要压缩，3.13 最小可发布范围为：

- 每个小组件独立配置。
- 透明/半透明/系统三种外观。
- 2x2、3x3、4x2 尺寸策略。
- 内容范围支持全部、仅置顶、仅未来。
- 显示密度支持紧凑、标准、宽松。

玻璃感、已有小组件管理页、应用到全部已有小组件可作为 3.13.x 增量。
