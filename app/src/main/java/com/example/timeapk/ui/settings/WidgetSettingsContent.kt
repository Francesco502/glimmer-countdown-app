package com.example.timeapk.ui.settings

import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.timeapk.ui.components.SongConfirmDialog
import com.example.timeapk.ui.theme.SongModeTabRow
import com.example.timeapk.widget.SingleEventWidgetConfigActivity
import com.example.timeapk.widget.SingleEventWidgetProvider
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongPalette
import com.example.timeapk.widget.APPEARANCE_CELADON
import com.example.timeapk.widget.APPEARANCE_SEAL
import com.example.timeapk.widget.APPEARANCE_SOLID
import com.example.timeapk.widget.APPEARANCE_SYSTEM
import com.example.timeapk.widget.APPEARANCE_TRANSLUCENT
import com.example.timeapk.widget.APPEARANCE_TRANSPARENT
import com.example.timeapk.widget.BORDER_AUTO
import com.example.timeapk.widget.BORDER_OFF
import com.example.timeapk.widget.BORDER_ON
import com.example.timeapk.widget.CONTENT_ALL
import com.example.timeapk.widget.CONTENT_BIRTHDAY
import com.example.timeapk.widget.CONTENT_FUTURE
import com.example.timeapk.widget.CONTENT_PINNED
import com.example.timeapk.widget.CONTRAST_AUTO
import com.example.timeapk.widget.CONTRAST_DARK_TEXT
import com.example.timeapk.widget.CONTRAST_LIGHT_TEXT
import com.example.timeapk.widget.CORNER_LARGE
import com.example.timeapk.widget.CORNER_MEDIUM
import com.example.timeapk.widget.CORNER_SMALL
import com.example.timeapk.widget.CORNER_SYSTEM
import com.example.timeapk.widget.DENSITY_COMFORTABLE
import com.example.timeapk.widget.DENSITY_COMPACT
import com.example.timeapk.widget.DENSITY_STANDARD
import com.example.timeapk.widget.CountdownAppWidgetProvider
import com.example.timeapk.widget.SORT_HOME
import com.example.timeapk.widget.SORT_NEAREST_FIRST
import com.example.timeapk.widget.SORT_PINNED_FIRST
import com.example.timeapk.widget.WidgetConfigActivity
import com.example.timeapk.widget.WidgetConfig
import com.example.timeapk.widget.WidgetConfigRepository
import com.example.timeapk.widget.WidgetRenderPolicy
import com.example.timeapk.widget.WidgetThemeSnapshot
import com.example.timeapk.widget.WidgetUpdater
import kotlin.math.roundToInt

@Composable
fun WidgetSettingsContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimeApplication ?: return
    val widgetConfigRepository = remember(app) { WidgetConfigRepository(app) }
    val defaultWidgetConfig by widgetConfigRepository.defaultConfigFlow.collectAsState(initial = WidgetConfig.default())
    val widgetInstanceConfigs by widgetConfigRepository.instanceConfigsFlow.collectAsState(initial = emptyMap())
    val appWidgetIds = CountdownAppWidgetProvider.getAppWidgetIds(app).toList()
    val singleEventWidgetIds = SingleEventWidgetProvider.getAppWidgetIds(app).toList()
    var confirmApplyToAll by remember { mutableStateOf(false) }
    var pendingResetWidgetId by remember { mutableStateOf<Int?>(null) }

    fun launchWidgetSettingsUpdate(update: suspend () -> Unit) {
        app.launchAppTask {
            update()
            WidgetUpdater.refreshCountdownWidgets(app)
        }
    }

    fun openWidgetConfig(appWidgetId: Int) {
        context.startActivity(
            Intent(context, WidgetConfigActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )
    }

    fun openSingleEventWidgetConfig(appWidgetId: Int) {
        context.startActivity(
            Intent(context, SingleEventWidgetConfigActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )
    }

    if (confirmApplyToAll) {
        SongConfirmDialog(
            title = stringResource(R.string.widget_config_apply_to_all),
            message = stringResource(R.string.widget_config_apply_to_all_confirm),
            confirmText = stringResource(R.string.action_apply),
            dismissText = stringResource(R.string.delete_confirm_cancel),
            onConfirm = {
                confirmApplyToAll = false
                launchWidgetSettingsUpdate {
                    val ids = CountdownAppWidgetProvider.getAppWidgetIds(app)
                    val latestDefault = widgetConfigRepository.getDefaultConfig()
                    widgetConfigRepository.setAllInstanceConfigs(
                        ids.associateWith { latestDefault }
                    )
                }
            },
            onDismiss = { confirmApplyToAll = false }
        )
    }
    pendingResetWidgetId?.let { resetId ->
        SongConfirmDialog(
            title = stringResource(R.string.widget_config_reset_instance),
            message = stringResource(R.string.widget_config_reset_confirm),
            confirmText = stringResource(R.string.widget_config_reset_instance),
            dismissText = stringResource(R.string.delete_confirm_cancel),
            onConfirm = {
                pendingResetWidgetId = null
                launchWidgetSettingsUpdate {
                    widgetConfigRepository.removeConfigForWidget(resetId)
                }
            },
            onDismiss = { pendingResetWidgetId = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SettingsExpandableSection(
            title = stringResource(R.string.widget_config_defaults_title),
            summary = widgetConfigSummary(defaultWidgetConfig)
        ) {
            WidgetConfigEditor(
                config = defaultWidgetConfig,
                onConfigChange = { next ->
                    launchWidgetSettingsUpdate {
                        widgetConfigRepository.setDefaultConfig(next)
                    }
                },
                modifier = Modifier.padding(top = 4.dp)
            )
            if (appWidgetIds.isNotEmpty()) {
                TextButton(
                    onClick = { confirmApplyToAll = true },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.widget_config_apply_to_all))
                }
            }
        }

        SettingsExpandableSection(
            title = stringResource(R.string.widget_config_existing_widgets),
            summary = if (appWidgetIds.isEmpty() && singleEventWidgetIds.isEmpty()) {
                stringResource(R.string.settings_widget_summary_none)
            } else {
                pluralStringResource(
                    R.plurals.settings_widget_summary_count,
                    appWidgetIds.size + singleEventWidgetIds.size,
                    appWidgetIds.size + singleEventWidgetIds.size
                )
            }
        ) {
            WidgetInstanceManager(
                appWidgetIds = appWidgetIds,
                instanceConfigs = widgetInstanceConfigs,
                defaultConfig = defaultWidgetConfig,
                onEditWidget = ::openWidgetConfig,
                onResetWidget = { appWidgetId -> pendingResetWidgetId = appWidgetId },
                singleEventWidgetIds = singleEventWidgetIds,
                onEditSingleEventWidget = ::openSingleEventWidgetConfig,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun WidgetConfigEditor(
    config: WidgetConfig,
    onConfigChange: (WidgetConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    Column(modifier = modifier.fillMaxWidth()) {
        WidgetSettingsLayerHeader(title = stringResource(R.string.widget_config_layer_preview))
        WidgetConfigPreview(
            config = config,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        SongModeTabRow(
            options = listOf(
                0 to stringResource(R.string.widget_config_layer_appearance),
                1 to stringResource(R.string.widget_config_layer_content)
            ),
            selected = selectedTab,
            onSelected = { selectedTab = it },
            modifier = Modifier.fillMaxWidth()
        )
        if (selectedTab == 0) {
            WidgetOptionGroup(
                title = stringResource(R.string.widget_config_appearance),
                options = listOf(
                    APPEARANCE_SYSTEM to stringResource(R.string.widget_config_appearance_system),
                    APPEARANCE_SOLID to stringResource(R.string.widget_config_appearance_solid),
                    APPEARANCE_TRANSLUCENT to stringResource(R.string.widget_config_appearance_translucent),
                    APPEARANCE_TRANSPARENT to stringResource(R.string.widget_config_appearance_transparent),
                    APPEARANCE_CELADON to stringResource(R.string.widget_config_appearance_celadon),
                    APPEARANCE_SEAL to stringResource(R.string.widget_config_appearance_seal)
                ),
                selected = config.appearancePreset,
                onSelected = { onConfigChange(config.copy(appearancePreset = it).sanitize()) },
                showSwatches = true
            )
            if (widgetAppearanceSupportsOpacity(config.appearancePreset)) {
                WidgetOptionGroup(
                    title = stringResource(R.string.widget_config_opacity),
                    options = listOf(0, 25, 50, 75, 100).map {
                        it to stringResource(R.string.widget_config_percent, it)
                    },
                    selected = config.backgroundOpacityPercent,
                    onSelected = { onConfigChange(config.copy(backgroundOpacityPercent = it).sanitize()) }
                )
            } else {
                Text(
                    text = stringResource(R.string.widget_config_opacity_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            WidgetOptionGroup(
                title = stringResource(R.string.widget_config_border),
                options = listOf(
                    BORDER_AUTO to stringResource(R.string.widget_config_auto),
                    BORDER_ON to stringResource(R.string.toggle_on),
                    BORDER_OFF to stringResource(R.string.toggle_off)
                ),
                selected = config.borderMode,
                onSelected = { onConfigChange(config.copy(borderMode = it).sanitize()) }
            )
            WidgetOptionGroup(
                title = stringResource(R.string.widget_config_corner),
                options = listOf(
                    CORNER_SYSTEM to stringResource(R.string.widget_config_corner_system),
                    CORNER_SMALL to stringResource(R.string.widget_config_corner_small),
                    CORNER_MEDIUM to stringResource(R.string.widget_config_corner_medium),
                    CORNER_LARGE to stringResource(R.string.widget_config_corner_large)
                ),
                selected = config.cornerMode,
                onSelected = { onConfigChange(config.copy(cornerMode = it).sanitize()) }
            )
            WidgetOptionGroup(
                title = stringResource(R.string.widget_config_contrast),
                options = listOf(
                    CONTRAST_AUTO to stringResource(R.string.widget_config_auto),
                    CONTRAST_LIGHT_TEXT to stringResource(R.string.widget_config_contrast_light),
                    CONTRAST_DARK_TEXT to stringResource(R.string.widget_config_contrast_dark)
                ),
                selected = config.contrastMode,
                onSelected = { onConfigChange(config.copy(contrastMode = it).sanitize()) }
            )
            WidgetFontScaleControl(
                value = config.fontScale,
                onValueChange = { onConfigChange(config.copy(fontScale = it).sanitize()) }
            )
        } else {
            WidgetOptionGroup(
                title = stringResource(R.string.widget_config_content_scope),
                options = listOf(
                    CONTENT_ALL to stringResource(R.string.widget_config_content_all),
                    CONTENT_PINNED to stringResource(R.string.widget_config_content_pinned),
                    CONTENT_FUTURE to stringResource(R.string.widget_config_content_future),
                    CONTENT_BIRTHDAY to stringResource(R.string.widget_config_content_birthday)
                ),
                selected = config.contentScope,
                onSelected = { onConfigChange(config.copy(contentScope = it).sanitize()) }
            )
            WidgetOptionGroup(
                title = stringResource(R.string.widget_config_sort),
                options = listOf(
                    SORT_HOME to stringResource(R.string.widget_config_sort_home),
                    SORT_PINNED_FIRST to stringResource(R.string.widget_config_sort_pinned),
                    SORT_NEAREST_FIRST to stringResource(R.string.widget_config_sort_nearest)
                ),
                selected = config.sortMode,
                onSelected = { onConfigChange(config.copy(sortMode = it).sanitize()) }
            )
            WidgetOptionGroup(
                title = stringResource(R.string.widget_config_density),
                options = listOf(
                    DENSITY_COMPACT to stringResource(R.string.widget_config_density_compact),
                    DENSITY_STANDARD to stringResource(R.string.widget_config_density_standard),
                    DENSITY_COMFORTABLE to stringResource(R.string.widget_config_density_comfortable)
                ),
                selected = config.densityMode,
                onSelected = { onConfigChange(config.copy(densityMode = it).sanitize()) }
            )
            WidgetSwitchRow(
                title = stringResource(R.string.widget_config_show_lunar_prefix),
                checked = config.showLunarPrefix,
                onCheckedChange = { onConfigChange(config.copy(showLunarPrefix = it).sanitize()) }
            )
            // One choice for the preview's proportions; the real size is set on the home screen.
            WidgetOptionGroup(
                title = stringResource(R.string.widget_config_preview_size),
                options = WidgetPreviewSizes.mapIndexed { index, (w, h) ->
                    index to stringResource(R.string.widget_config_size_summary, w, h)
                },
                selected = WidgetPreviewSizes.indexOf(config.widthCells to config.heightCells),
                onSelected = { index ->
                    val (w, h) = WidgetPreviewSizes[index]
                    onConfigChange(config.copy(widthCells = w, heightCells = h).sanitize())
                }
            )
            Text(
                text = stringResource(R.string.widget_config_preview_size_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/** 预览常用尺寸（宽 × 高格数）。 */
internal val WidgetPreviewSizes: List<Pair<Int, Int>> = listOf(
    1 to 1, 2 to 1, 2 to 2, 3 to 2, 4 to 2, 4 to 3, 4 to 4
)

/** 只有“跟随系统”与磨砂玻璃两种背景会随不透明度变化。 */
internal fun widgetAppearanceSupportsOpacity(appearancePreset: Int): Boolean =
    appearancePreset == APPEARANCE_SYSTEM || appearancePreset == APPEARANCE_TRANSLUCENT

/** 预览行数随高度格数增加（每格约两行），与桌面 RemoteViews 自上而下的排列一致。 */
internal fun widgetPreviewRowCount(heightCells: Int): Int = (heightCells * 2).coerceIn(1, 6)

@Composable
private fun WidgetFontScaleControl(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    var draft by remember(value) { mutableStateOf(value) }

    LaunchedEffect(value) {
        draft = value
    }

    Text(
        text = stringResource(R.string.settings_widget_font_scale_title),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                R.string.settings_widget_font_scale_value,
                (draft * 100).roundToInt()
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(
            onClick = {
                draft = 1f
                onValueChange(1f)
            }
        ) {
            Text(stringResource(R.string.settings_widget_font_scale_reset_action))
        }
    }
    Slider(
        value = draft,
        onValueChange = {
            draft = it.coerceIn(
                SongDesignTokens.WidgetFontScaleMin,
                SongDesignTokens.WidgetFontScaleMax
            )
        },
        valueRange = SongDesignTokens.WidgetFontScaleMin..SongDesignTokens.WidgetFontScaleMax,
        onValueChangeFinished = { onValueChange(draft) },
        modifier = Modifier.padding(top = 4.dp)
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
}

@Composable
private fun WidgetSettingsLayerHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun WidgetConfigSection(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    SettingsExpandableSection(
        title = title,
        summary = summary,
        initiallyExpanded = initiallyExpanded,
        modifier = modifier,
        content = content
    )
}

@Composable
fun WidgetInstanceManager(
    appWidgetIds: List<Int>,
    instanceConfigs: Map<Int, WidgetConfig>,
    defaultConfig: WidgetConfig,
    onEditWidget: (Int) -> Unit,
    onResetWidget: (Int) -> Unit,
    modifier: Modifier = Modifier,
    singleEventWidgetIds: List<Int> = emptyList(),
    onEditSingleEventWidget: (Int) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        singleEventWidgetIds.forEachIndexed { index, id ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.widget_config_single_widget_title, index + 1),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onEditSingleEventWidget(id) }) {
                    Text(stringResource(R.string.widget_config_edit))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
        }
        if (appWidgetIds.isEmpty() && singleEventWidgetIds.isEmpty()) {
            Text(
                text = stringResource(R.string.widget_config_no_existing_widgets),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
            return
        }
        appWidgetIds.forEachIndexed { index, id ->
            val config = instanceConfigs[id] ?: defaultConfig
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.widget_config_existing_widget_title, index + 1),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = widgetConfigSummary(config),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { onEditWidget(id) }) {
                    Text(stringResource(R.string.widget_config_edit))
                }
                TextButton(onClick = { onResetWidget(id) }) {
                    Text(stringResource(R.string.widget_config_reset_instance))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WidgetOptionGroup(
    title: String,
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelected: (Int) -> Unit,
    showSwatches: Boolean = false
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
    )
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            WidgetOptionPill(
                label = label,
                selected = selected == value,
                swatch = if (showSwatches) appearanceSwatch(value) else null,
                onClick = { onSelected(value) }
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
}

@Composable
private fun WidgetConfigPreview(
    config: WidgetConfig,
    modifier: Modifier = Modifier
) {
    val clean = config.sanitize()
    val previewStyle = resolveWidgetPreviewStyle(clean, isSystemInDarkTheme())
    val backgroundColor = Color(previewStyle.backgroundColorArgb)
    val contentColor = Color(previewStyle.contentColorArgb)
    val secondaryContentColor = Color(previewStyle.secondaryContentColorArgb)
    val accentColor = Color(previewStyle.accentColorArgb)
    val borderColor = Color(previewStyle.borderColorArgb)
    val previewAspectRatio = clean.widthCells.toFloat() / clean.heightCells.toFloat()
    val baseCellSize = 64.dp
    val desiredPreviewWidth = baseCellSize * clean.widthCells.toFloat()
    val maxPreviewHeight = 320.dp
    val cornerRadius = resolveWidgetPreviewCornerRadiusDp(clean.cornerMode).dp
    val contentPadding = if (clean.widthCells == 1) 8.dp else if (clean.heightCells == 1) 10.dp else 14.dp
    val rowVerticalPadding = resolveWidgetPreviewRowVerticalPaddingDp(clean.densityMode).dp

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val constrainedWidth = if (desiredPreviewWidth > maxWidth) maxWidth else desiredPreviewWidth
        val heightFromWidth = constrainedWidth / previewAspectRatio
        val previewHeight = if (heightFromWidth > maxPreviewHeight) maxPreviewHeight else heightFromWidth
        val previewWidth = if (heightFromWidth > maxPreviewHeight) {
            maxPreviewHeight * previewAspectRatio
        } else {
            constrainedWidth
        }
        Column(
            modifier = Modifier
                .width(previewWidth)
                .height(previewHeight)
                .background(backgroundColor, RoundedCornerShape(cornerRadius))
                .border(0.5f.dp, borderColor, RoundedCornerShape(cornerRadius))
                .padding(contentPadding),
            verticalArrangement = if (clean.heightCells <= 1) {
                Arrangement.Center
            } else {
                Arrangement.spacedBy(6.dp, Alignment.Top)
            }
        ) {
            val samples = listOf(
                stringResource(R.string.widget_config_preview_event_primary) to
                    stringResource(R.string.widget_config_preview_value_primary),
                stringResource(resolveWidgetPreviewSecondaryTitleResId(clean.showLunarPrefix)) to
                    stringResource(R.string.widget_config_preview_value_secondary),
                stringResource(R.string.widget_config_preview_event_third) to
                    stringResource(R.string.widget_config_preview_value_third)
            )
            repeat(widgetPreviewRowCount(clean.heightCells)) { index ->
                val (title, value) = samples[index % samples.size]
                WidgetPreviewRow(
                    title = title,
                    value = value,
                    contentColor = if (index == 0) contentColor else secondaryContentColor,
                    accentColor = if (index == 0) accentColor else accentColor.copy(alpha = 0.82f),
                    compact = clean.widthCells <= 1,
                    verticalPadding = rowVerticalPadding
                )
            }
        }
    }
}

internal fun resolveWidgetPreviewCornerRadiusDp(cornerMode: Int): Int = when (cornerMode) {
    CORNER_SMALL -> 10
    CORNER_MEDIUM -> 18
    CORNER_LARGE -> 30
    else -> 22
}

internal fun resolveWidgetPreviewSecondaryTitleResId(showLunarPrefix: Boolean): Int =
    if (showLunarPrefix) {
        R.string.widget_config_preview_event_secondary
    } else {
        R.string.widget_config_preview_event_secondary_plain
    }

internal fun resolveWidgetPreviewRowVerticalPaddingDp(densityMode: Int): Int = when (densityMode) {
    DENSITY_COMPACT -> 1
    DENSITY_COMFORTABLE -> 5
    else -> 3
}

internal data class WidgetPreviewStyle(
    val backgroundColorArgb: Int,
    val borderColorArgb: Int,
    val contentColorArgb: Int,
    val secondaryContentColorArgb: Int,
    val accentColorArgb: Int
)

internal fun resolveWidgetPreviewStyle(
    config: WidgetConfig,
    isDark: Boolean
): WidgetPreviewStyle {
    val clean = config.sanitize()
    val renderStyle = WidgetRenderPolicy.resolve(
        clean,
        WidgetThemeSnapshot(isDark = isDark, usesSystemPalette = true)
    )
    return WidgetPreviewStyle(
        backgroundColorArgb = resolveWidgetPreviewBackgroundArgb(clean, isDark),
        borderColorArgb = resolveWidgetPreviewBorderArgb(clean, isDark),
        contentColorArgb = renderStyle.primaryTextColor,
        secondaryContentColorArgb = renderStyle.secondaryTextColor,
        accentColorArgb = renderStyle.accentTextColor
    )
}

private fun resolveWidgetPreviewBackgroundArgb(config: WidgetConfig, isDark: Boolean): Int {
    return when (config.appearancePreset) {
        APPEARANCE_SEAL -> if (isDark) 0xFF86351C.toInt() else 0xFFAF4E31.toInt()
        APPEARANCE_CELADON -> if (isDark) 0xD9272F2A.toInt() else 0xDDE8EEE6.toInt()
        APPEARANCE_TRANSPARENT -> 0x00000000
        APPEARANCE_TRANSLUCENT -> resolveWidgetPreviewGlassArgb(config.backgroundOpacityPercent, isDark)
        APPEARANCE_SOLID -> if (isDark) 0xF21C1C1E.toInt() else 0xFFF5F3ED.toInt()
        else -> when (config.backgroundOpacityPercent) {
            0 -> 0x00000000
            25, 50, 75 -> resolveWidgetPreviewGlassArgb(config.backgroundOpacityPercent, isDark)
            else -> if (isDark) 0xFF1C1C1E.toInt() else 0xFFF5F3ED.toInt()
        }
    }
}

private fun resolveWidgetPreviewGlassArgb(opacityPercent: Int, isDark: Boolean): Int {
    return if (isDark) {
        when (opacityPercent) {
            25 -> 0xA61C1C1E.toInt()
            50 -> 0xBF1C1C1E.toInt()
            else -> 0xD91C1C1E.toInt()
        }
    } else {
        when (opacityPercent) {
            25 -> 0xB3F1F3F0.toInt()
            50 -> 0xC7F1F3F0.toInt()
            else -> 0xDDF2F3F0.toInt()
        }
    }
}

private fun resolveWidgetPreviewBorderArgb(config: WidgetConfig, isDark: Boolean): Int {
    if (config.borderMode == BORDER_OFF) return 0x00000000
    return when (config.appearancePreset) {
        APPEARANCE_SEAL -> if (isDark) 0x66F6D9A6 else 0x667A2F20
        APPEARANCE_CELADON -> if (isDark) 0x665B8E79 else 0x66457080
        APPEARANCE_TRANSPARENT -> 0x66FFFFFF
        APPEARANCE_TRANSLUCENT -> if (isDark) 0x26FFFFFF else 0x1A202124
        else -> if (config.backgroundOpacityPercent in 25..75) {
            if (isDark) 0x26FFFFFF else 0x1A202124
        } else if (isDark) {
            0x52EDE8DD
        } else {
            0x331F1F1F
        }
    }.toInt()
}

@Composable
private fun WidgetPreviewRow(
    title: String,
    value: String,
    contentColor: Color,
    accentColor: Color,
    compact: Boolean = false,
    verticalPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    Row(
        modifier = Modifier.padding(vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(22.dp)
                .background(accentColor, RoundedCornerShape(2.dp))
        )
        if (compact) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .weight(1f)
            )
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = accentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WidgetOptionPill(
    label: String,
    selected: Boolean,
    swatch: Color?,
    onClick: () -> Unit
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaStrong)
    }
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        Color.Transparent
    }
    Row(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(SongDesignTokens.StandardRadius.dp))
            .border(1.dp, borderColor, RoundedCornerShape(SongDesignTokens.StandardRadius.dp))
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        swatch?.let {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(it, RoundedCornerShape(2.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f), RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun appearanceSwatch(value: Int): Color? {
    return when (value) {
        APPEARANCE_SOLID -> SongPalette.Paper
        APPEARANCE_TRANSLUCENT -> SongPalette.Ink.copy(alpha = 0.42f)
        APPEARANCE_TRANSPARENT -> Color.Transparent
        APPEARANCE_CELADON -> SongPalette.CeladonWash
        APPEARANCE_SEAL -> SongPalette.Seal
        else -> SongPalette.PaperDeep
    }
}

@Composable
internal fun WidgetSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val stateDescriptionText = stringResource(if (checked) R.string.toggle_on else R.string.toggle_off)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .semantics(mergeDescendants = true) {
                stateDescription = stateDescriptionText
            }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        SongToggle(checked = checked, onCheckedChange = null)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
}

@Composable
private fun widgetConfigSummary(config: WidgetConfig): String {
    return listOf(
        stringResource(R.string.widget_config_size_summary, config.widthCells, config.heightCells),
        stringResource(R.string.widget_config_percent, config.backgroundOpacityPercent),
        stringResource(contrastLabelRes(config.contrastMode)),
        stringResource(densityLabelRes(config.densityMode)),
        stringResource(sortLabelRes(config.sortMode))
    ).joinToString(" / ")
}

private fun appearanceLabelRes(value: Int): Int = when (value) {
    APPEARANCE_SOLID -> R.string.widget_config_appearance_solid
    APPEARANCE_TRANSLUCENT -> R.string.widget_config_appearance_translucent
    APPEARANCE_TRANSPARENT -> R.string.widget_config_appearance_transparent
    APPEARANCE_CELADON -> R.string.widget_config_appearance_celadon
    APPEARANCE_SEAL -> R.string.widget_config_appearance_seal
    else -> R.string.widget_config_appearance_system
}

private fun contentScopeLabelRes(value: Int): Int = when (value) {
    CONTENT_PINNED -> R.string.widget_config_content_pinned
    CONTENT_FUTURE -> R.string.widget_config_content_future
    CONTENT_BIRTHDAY -> R.string.widget_config_content_birthday
    else -> R.string.widget_config_content_all
}

private fun densityLabelRes(value: Int): Int = when (value) {
    DENSITY_COMPACT -> R.string.widget_config_density_compact
    DENSITY_COMFORTABLE -> R.string.widget_config_density_comfortable
    else -> R.string.widget_config_density_standard
}

private fun sortLabelRes(value: Int): Int = when (value) {
    SORT_HOME -> R.string.widget_config_sort_home
    SORT_PINNED_FIRST -> R.string.widget_config_sort_pinned
    SORT_NEAREST_FIRST -> R.string.widget_config_sort_nearest
    else -> R.string.widget_config_sort_home
}

private fun contrastLabelRes(value: Int): Int = when (value) {
    CONTRAST_LIGHT_TEXT -> R.string.widget_config_contrast_light
    CONTRAST_DARK_TEXT -> R.string.widget_config_contrast_dark
    else -> R.string.widget_config_contrast_auto_summary
}
