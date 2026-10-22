package com.example.timeapk.ui.settings

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.RemoteViews
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.example.timeapk.widget.DISPLAY_POLICY_AUTO
import com.example.timeapk.widget.DISPLAY_POLICY_FOLLOW_APP
import com.example.timeapk.widget.DISPLAY_POLICY_COUNTDOWN
import com.example.timeapk.widget.CountdownAppWidgetProvider
import com.example.timeapk.widget.SORT_HOME
import com.example.timeapk.widget.SORT_NEAREST_FIRST
import com.example.timeapk.widget.SORT_PINNED_FIRST
import com.example.timeapk.widget.WidgetConfigActivity
import com.example.timeapk.widget.WidgetConfig
import com.example.timeapk.widget.WidgetConfigRepository
import com.example.timeapk.widget.WidgetContentResolver
import com.example.timeapk.widget.WidgetContentSnapshot
import com.example.timeapk.widget.WidgetSizeBucket
import com.example.timeapk.widget.WidgetUpdater
import com.example.timeapk.widget.widgetLanguageContext
import com.example.timeapk.widget.buildWidgetItemRemoteViews
import com.example.timeapk.widget.setWidgetTextColors
import com.example.timeapk.widget.setWidgetBackground
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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
                WidgetOpacityControl(
                    value = config.backgroundOpacityPercent,
                    onValueChange = { onConfigChange(config.copy(backgroundOpacityPercent = it).sanitize()) }
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
                    CONTRAST_AUTO to stringResource(R.string.widget_config_contrast_auto),
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
                title = stringResource(R.string.widget_config_display_policy),
                options = listOf(
                    DISPLAY_POLICY_AUTO to stringResource(R.string.widget_config_display_auto),
                    DISPLAY_POLICY_FOLLOW_APP to stringResource(R.string.widget_config_display_follow_app),
                    DISPLAY_POLICY_COUNTDOWN to stringResource(R.string.widget_config_display_countdown)
                ),
                selected = config.displayPolicy,
                onSelected = { onConfigChange(config.copy(displayPolicy = it).sanitize()) }
            )
            Text(
                text = stringResource(R.string.widget_config_display_policy_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
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

@Composable
internal fun WidgetOpacityControl(value: Int, onValueChange: (Int) -> Unit) {
    var draft by remember(value) { mutableStateOf(value.toFloat()) }
    val label = stringResource(R.string.widget_config_opacity)
    val percent = stringResource(R.string.widget_config_percent, draft.roundToInt())
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(percent, style = MaterialTheme.typography.bodyLarge)
    }
    Slider(
        value = draft,
        onValueChange = { draft = it.coerceIn(0f, 100f) },
        valueRange = 0f..100f,
        onValueChangeFinished = { onValueChange(draft.roundToInt()) },
        modifier = Modifier.semantics {
            contentDescription = label
            stateDescription = percent
        }
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.widget_config_opacity_transparent), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.widget_config_opacity_opaque), style = MaterialTheme.typography.bodySmall)
    }
}

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
    val context = LocalContext.current
    val app = context.applicationContext as TimeApplication
    val configuration = LocalConfiguration.current
    val clean = config.sanitize()
    val previewAspectRatio = widgetPreviewDimensionDp(clean.widthCells).toFloat() /
        widgetPreviewDimensionDp(clean.heightCells)
    val desiredPreviewWidth = widgetPreviewDimensionDp(clean.widthCells).dp
    val maxPreviewHeight = 320.dp
    val changes = remember(app) {
        val prefs = app.userPrefs
        combine(
            listOf(
                app.repository.getAllEvents().map { Unit },
                prefs.customMilestonesFlow.map { Unit },
                prefs.pinnedEventIdsFlow.map { Unit },
                prefs.customEventOrderFlow.map { Unit },
                prefs.sortTypeFlow.map { Unit },
                prefs.dateDeltaDisplayModeFlow.map { Unit },
                prefs.perEventDateDeltaDisplayModesFlow.map { Unit },
                prefs.showMilestoneFlow.map { Unit },
                prefs.smartMilestonesEnabledFlow.map { Unit },
                flow {
                    while (true) {
                        emit(Unit)
                        delay(60_000)
                    }
                }
            )
        ) { Unit }
    }
    var loadAttempt by remember { mutableStateOf(0) }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val constrainedWidth = minOf(desiredPreviewWidth, maxWidth)
        val previewHeight = minOf(constrainedWidth / previewAspectRatio, maxPreviewHeight)
        val previewWidth = minOf(constrainedWidth, previewHeight * previewAspectRatio)
        val sizeBucket = resolveWidgetPreviewSizeBucket(previewWidth.value.roundToInt(), previewHeight.value.roundToInt())
        var snapshot by remember { mutableStateOf<WidgetContentSnapshot?>(null) }
        var loadFailed by remember { mutableStateOf(false) }
        LaunchedEffect(clean, sizeBucket, previewWidth, configuration, loadAttempt) {
            snapshot = null
            loadFailed = false
            try {
                changes.collectLatest {
                    snapshot = withContext(Dispatchers.IO) {
                        WidgetContentResolver.load(
                            context = context,
                            appWidgetId = null,
                            sizeBucket = sizeBucket,
                            configOverride = clean,
                            previewWidthDp = previewWidth.value.roundToInt()
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                snapshot = null
                loadFailed = true
            }
        }
        val content = snapshot
        if (content == null) {
            Box(
                modifier = Modifier.width(previewWidth).height(previewHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(
                        if (loadFailed) R.string.widget_preview_load_failed else R.string.widget_preview_loading
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (loadFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = if (loadFailed) Modifier.clickable(role = Role.Button) { loadAttempt += 1 } else Modifier
                )
            }
        } else {
            AndroidView(
                modifier = Modifier.width(previewWidth).height(previewHeight),
                factory = { FrameLayout(it) },
                update = { host ->
                    if (host.tag != content) {
                        host.tag = content
                        host.removeAllViews()
                        val root = buildWidgetPreviewView(host, content)
                        host.addView(root, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    }
                }
            )
        }
    }
}

// ponytail: Launcher cell sizes vary; use nominal dimensions until a specific host size is available.
internal fun widgetPreviewDimensionDp(cells: Int): Int = 40 + 70 * cells.coerceIn(1, 4)

internal fun resolveWidgetPreviewSizeBucket(widthDp: Int, heightDp: Int): Int =
    WidgetSizeBucket.resolve(widthDp, widthDp, heightDp, heightDp)

internal fun buildWidgetPreviewView(host: ViewGroup, snapshot: WidgetContentSnapshot): View {
    val context = widgetLanguageContext(host.context)
    val style = snapshot.renderStyle
    val textStyle = snapshot.textStyle
    val root = RemoteViews(context.packageName, style.rootLayoutResId).apply {
        setWidgetBackground(style)
        setWidgetTextColors(context, style, secondaryIds = intArrayOf(R.id.widget_empty))
        setTextViewTextSize(R.id.widget_empty, TypedValue.COMPLEX_UNIT_SP, textStyle.emptyTextSp)
        setTextViewText(R.id.widget_empty, context.getString(R.string.home_empty_title))
    }.apply(context, host)
    val list = root.findViewById<ListView>(R.id.widget_list)
    list.emptyView = root.findViewById(R.id.widget_empty)
    list.adapter = object : BaseAdapter() {
        override fun getCount(): Int = snapshot.items.size
        override fun getItem(position: Int) = snapshot.items[position]
        override fun getItemId(position: Int): Long = getItem(position).eventId.toLong()
        override fun hasStableIds(): Boolean = true
        override fun isEnabled(position: Int): Boolean = false
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            buildWidgetItemRemoteViews(context, getItem(position), textStyle, style, snapshot.widthDp).apply(context, parent).apply {
                // A preview has no launcher binding or event-opening action.
                setOnClickListener(null)
                isClickable = false
            }
    }
    return root
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
        stringResource(sortLabelRes(config.sortMode)),
        stringResource(displayPolicyLabelRes(config.displayPolicy))
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

private fun displayPolicyLabelRes(value: Int): Int = when (value) {
    DISPLAY_POLICY_FOLLOW_APP -> R.string.widget_config_display_follow_app
    DISPLAY_POLICY_COUNTDOWN -> R.string.widget_config_display_countdown
    else -> R.string.widget_config_display_auto
}

private fun contrastLabelRes(value: Int): Int = when (value) {
    CONTRAST_LIGHT_TEXT -> R.string.widget_config_contrast_light
    CONTRAST_DARK_TEXT -> R.string.widget_config_contrast_dark
    else -> R.string.widget_config_contrast_auto_summary
}
