package com.example.timeapk.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.example.timeapk.BuildConfig
import com.example.timeapk.permissions.areAppNotificationsEnabledCompat
import com.example.timeapk.permissions.hasCalendarReadWritePermission
import com.example.timeapk.permissions.markCalendarPermissionRequested
import com.example.timeapk.permissions.openAppDetailsSettings
import com.example.timeapk.permissions.shouldShowCalendarPermissionRationaleCompat
import com.example.timeapk.permissions.wasCalendarPermissionRequestedBefore
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.*
import com.example.timeapk.notifications.RescheduleAllWorker
import com.example.timeapk.notifications.rescheduleMilestoneReminders
import com.example.timeapk.notifications.scheduleReminder
import com.example.timeapk.notifications.ScheduleSyncManager
import com.example.timeapk.notifications.eventAfterScheduleSyncAttempt
import com.example.timeapk.ui.components.PermissionActionDialog
import com.example.timeapk.ui.components.PermissionDialogSpec
import com.example.timeapk.ui.components.SongDialogButton
import com.example.timeapk.ui.components.SongFormDialog
import com.example.timeapk.ui.components.SongWheelPickerDialog
import com.example.timeapk.ui.components.SnapWheelPicker
import com.example.timeapk.ui.common.SongMiniPreviewSurface
import com.example.timeapk.ui.common.SongReminderStatusStrip
import com.example.timeapk.ui.reminder.ReminderStatusAction
import com.example.timeapk.ui.reminder.ReminderStatusSummary
import com.example.timeapk.ui.reminder.buildReminderStatus
import com.example.timeapk.ui.theme.ColorContrastGuardrail
import com.example.timeapk.ui.theme.FONT_PRESET_DEFAULT
import com.example.timeapk.ui.theme.FONT_PRESET_NOTO_SERIF_SC
import com.example.timeapk.ui.theme.FONT_PRESET_SYSTEM_SANS
import com.example.timeapk.ui.theme.FONT_PRESET_SYSTEM_SERIF
import com.example.timeapk.ui.theme.FONT_PRESET_ZCOOL_XIAOWEI
import com.example.timeapk.ui.theme.FontPresetValues
import com.example.timeapk.ui.theme.SongColorBoundary
import com.example.timeapk.widget.WidgetUpdater
import com.example.timeapk.update.CheckUpdateResult
import com.example.timeapk.update.UpdateInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.timeapk.ui.theme.SongColorSwatch
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongFilterChip
import com.example.timeapk.ui.theme.SongHexColorField
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind
import com.example.timeapk.ui.theme.typographyForFontPreset
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.findActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private data class PendingImportPreview(
    val parseResult: BackupParseResult,
    val importableEvents: List<Event>,
    val existingDuplicateCount: Int
) {
    val totalSkippedDuplicates: Int
        get() = parseResult.skippedDuplicateCount + existingDuplicateCount
}

private data class ImportExecutionResult(
    val successCount: Int,
    val failedCount: Int,
    val warningCount: Int
)

@Composable
fun ClassicalToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val stateDescription = stringResource(if (checked) R.string.toggle_on else R.string.toggle_off)
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .semantics {
                role = Role.Switch
                this.stateDescription = stateDescription
            }
            .border(
                width = 0.5.dp,
                color = if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaSoft),
                shape = RoundedCornerShape(2.dp)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stateDescription,
            style = MaterialTheme.typography.bodyLarge,
            color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

private val PRESET_COLOR_HEX = SongColorBoundary.recommendedPresetHexes()

@Composable
fun AppearanceSettingsContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimeApplication
    if (app == null) return
    val prefs = app.userPrefs
    val scope = rememberCoroutineScope()
    fun launchAppearanceUpdate(update: suspend () -> Unit) {
        app.launchAppTask {
            update()
            WidgetUpdater.refreshCountdownWidgets(app)
        }
    }

    val themeMode by prefs.themeModeFlow.collectAsState(initial = THEME_FOLLOW_SYSTEM)
    val customBackgroundHex by prefs.customBackgroundHexFlow.collectAsState(initial = null)
    val customSurfaceHex by prefs.customSurfaceHexFlow.collectAsState(initial = null)
    val customPrimaryHex by prefs.customPrimaryHexFlow.collectAsState(initial = null)
    val customOnBackgroundHex by prefs.customOnBackgroundHexFlow.collectAsState(initial = null)
    val fontPreset by prefs.fontPresetFlow.collectAsState(initial = 4)
    val appBaseFontScale by prefs.appBaseFontScaleFlow.collectAsState(initial = 1f)
    var appBaseFontScaleDraft by remember(appBaseFontScale) { mutableStateOf(appBaseFontScale) }
    var colorPickerKey by remember { mutableStateOf<String?>(null) }
    var showFontPresetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(appBaseFontScale) {
        appBaseFontScaleDraft = appBaseFontScale
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SongMiniPreviewSurface(
            contentDescription = stringResource(R.string.settings_appearance_preview_cd),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.settings_appearance_preview_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.widget_config_preview_event_primary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.widget_config_preview_value_primary),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        SettingsExpandableSection(
            title = stringResource(R.string.theme_title),
            summary = stringResource(R.string.settings_section_theme_summary),
            initiallyExpanded = true
        ) {
            SettingsRadioGroup {
                listOf(
                    THEME_FOLLOW_SYSTEM to stringResource(R.string.theme_follow_system),
                    THEME_LIGHT to stringResource(R.string.theme_light),
                    THEME_DARK to stringResource(R.string.theme_dark)
                ).forEach { (value, label) ->
                    SettingsRadioRow(
                        label = label,
                        selected = themeMode == value,
                        onClick = {
                            launchAppearanceUpdate {
                                prefs.setThemeMode(value)
                            }
                        }
                    )
                }
            }
        }

        SettingsExpandableSection(
            title = stringResource(R.string.custom_colors_title),
            summary = stringResource(R.string.settings_section_color_summary)
        ) {
            CustomColorRow(
                label = stringResource(R.string.custom_color_background),
                currentHex = customBackgroundHex,
                defaultColor = MaterialTheme.colorScheme.background,
                onPick = { colorPickerKey = "background" },
                onReset = { scope.launch { prefs.setCustomBackgroundHex(null) } }
            )
            CustomColorRow(
                label = stringResource(R.string.custom_color_surface),
                currentHex = customSurfaceHex,
                defaultColor = MaterialTheme.colorScheme.surfaceVariant,
                onPick = { colorPickerKey = "surface" },
                onReset = { scope.launch { prefs.setCustomSurfaceHex(null) } }
            )
            CustomColorRow(
                label = stringResource(R.string.custom_color_primary),
                currentHex = customPrimaryHex,
                defaultColor = MaterialTheme.colorScheme.primary,
                onPick = { colorPickerKey = "primary" },
                onReset = { scope.launch { prefs.setCustomPrimaryHex(null) } }
            )
            CustomColorRow(
                label = stringResource(R.string.custom_color_on_background),
                currentHex = customOnBackgroundHex,
                defaultColor = MaterialTheme.colorScheme.onBackground,
                showBorder = true,
                onPick = { colorPickerKey = "on_background" },
                onReset = { scope.launch { prefs.setCustomOnBackgroundHex(null) } }
            )
        }

        colorPickerKey?.let { key ->
            val (label, setter) = when (key) {
                "background" -> stringResource(R.string.custom_color_background) to { hex: String ->
                    scope.launch { prefs.setCustomBackgroundHex(hex) }
                }
                "surface" -> stringResource(R.string.custom_color_surface) to { hex: String ->
                    scope.launch { prefs.setCustomSurfaceHex(hex) }
                }
                "primary" -> stringResource(R.string.custom_color_primary) to { hex: String ->
                    scope.launch { prefs.setCustomPrimaryHex(hex) }
                }
                "on_background" -> stringResource(R.string.custom_color_on_background) to { hex: String ->
                    scope.launch { prefs.setCustomOnBackgroundHex(hex) }
                }
                else -> return@let
            }
            var customHexInput by remember(key) { mutableStateOf("") }
            var contrastErrorRatio by remember(key) { mutableStateOf<Double?>(null) }
            val currentColorScheme = MaterialTheme.colorScheme
            val normalizedCustomHex = remember(customHexInput) {
                val trimmed = customHexInput.trim().removePrefix("#")
                if (trimmed.isBlank()) "" else "#${trimmed.uppercase()}"
            }
            val isValidHex = remember(customHexInput) {
                try {
                    if (normalizedCustomHex.length == 7 || normalizedCustomHex.length == 9) {
                        normalizedCustomHex.toColorInt()
                        true
                    } else {
                        false
                    }
                } catch (_: Exception) {
                    false
                }
            }
            val candidateAudit = remember(key, customHexInput, currentColorScheme) {
                parseHexColor(normalizedCustomHex)?.let {
                    evaluateContrastAuditForKey(currentColorScheme, key, it)
                }
            }
            fun tryApplyColor(hex: String) {
                val parsed = parseHexColor(hex) ?: return
                val audit = evaluateContrastAuditForKey(currentColorScheme, key, parsed)
                if (audit.isPass) {
                    setter(hex)
                    colorPickerKey = null
                    contrastErrorRatio = null
                } else {
                    contrastErrorRatio = audit.minRatio
                }
            }

            SongFormDialog(
                title = label,
                onDismissRequest = { colorPickerKey = null },
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PRESET_COLOR_HEX.chunked(6).forEach { rowHexes ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowHexes.forEach { hex ->
                                        SongColorSwatch(
                                            color = Color(hex.toColorInt()),
                                            onClick = { tryApplyColor(hex) },
                                            selected = false,
                                            showBorder = true
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))
                        SongHexColorField(
                            value = customHexInput,
                            onValueChange = {
                                customHexInput = it.removePrefix("#").take(8).uppercase()
                                contrastErrorRatio = null
                            },
                            label = stringResource(R.string.custom_color_hex_hint),
                            placeholder = "RRGGBB",
                            isError = customHexInput.isNotEmpty() && (!isValidHex || candidateAudit?.isPass == false),
                            previewColor = if (isValidHex) Color(normalizedCustomHex.toColorInt()) else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        val customAuditFailed = isValidHex && candidateAudit?.isPass == false
                        if (customAuditFailed) {
                            Text(
                                text = stringResource(
                                    R.string.custom_color_contrast_error,
                                    candidateAudit?.minRatio ?: 0.0
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        contrastErrorRatio?.let { minRatio ->
                            Text(
                                text = stringResource(R.string.custom_color_contrast_error, minRatio),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                buttons = {
                    SongDialogButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = { colorPickerKey = null }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    SongDialogButton(
                        text = stringResource(android.R.string.ok),
                        onClick = { tryApplyColor(normalizedCustomHex) },
                        enabled = isValidHex && candidateAudit?.isPass == true
                    )
                }
            )
        }

        if (showFontPresetDialog) {
            FontPresetPickerDialog(
                selectedPreset = fontPreset,
                onPresetSelected = { preset ->
                    scope.launch { prefs.setFontPreset(preset) }
                    showFontPresetDialog = false
                },
                onDismiss = { showFontPresetDialog = false }
            )
        }

        SettingsExpandableSection(
            title = stringResource(R.string.font_title),
            summary = stringResource(R.string.settings_section_font_summary, fontPresetTitle(fontPreset))
        ) {
            SettingsPressableRow(onClick = { showFontPresetDialog = true }) {
                val description = fontPresetDescription(fontPreset)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_font_picker_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SettingsSupportText(text = description)
                }
                Text(
                    text = fontPresetTitle(fontPreset),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        SettingsExpandableSection(
            title = stringResource(R.string.settings_typography_scale_title),
            summary = stringResource(R.string.settings_font_scale_summary, (appBaseFontScaleDraft * 100).roundToInt())
        ) {
            Text(
                text = stringResource(R.string.settings_app_font_scale_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.settings_font_scale_summary, (appBaseFontScaleDraft * 100).roundToInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            Slider(
                value = appBaseFontScaleDraft,
                onValueChange = {
                    appBaseFontScaleDraft = it.coerceIn(
                        SongDesignTokens.BaseFontScaleMin,
                        SongDesignTokens.BaseFontScaleMax
                    )
                },
                valueRange = SongDesignTokens.BaseFontScaleMin..SongDesignTokens.BaseFontScaleMax,
                onValueChangeFinished = {
                    scope.launch { prefs.setAppBaseFontScale(appBaseFontScaleDraft) }
                },
                modifier = Modifier.padding(top = 4.dp)
            )
            TextButton(
                onClick = {
                    appBaseFontScaleDraft = 1f
                    scope.launch { prefs.setAppBaseFontScale(1f) }
                }
            ) {
                Text(stringResource(R.string.settings_font_scale_reset))
            }
        }
    }
}

@Composable
private fun FontPresetPickerDialog(
    selectedPreset: Int,
    onPresetSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    SongFormDialog(
        title = stringResource(R.string.settings_font_picker_title),
        onDismissRequest = onDismiss,
        content = {
            SettingsRadioGroup(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FontPresetValues.forEach { preset ->
                    val previewTypography = typographyForFontPreset(preset)
                    val description = fontPresetDescription(preset)
                    SettingsRadioRow(
                        label = fontPresetTitle(preset),
                        selected = selectedPreset == preset,
                        onClick = { onPresetSelected(preset) },
                        labelStyle = previewTypography.titleMedium,
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.settings_font_preview_text),
                                style = previewTypography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            SettingsSupportText(
                                text = description,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    )
                }
            }
        },
        buttons = {
            SongDialogButton(
                text = stringResource(android.R.string.cancel),
                onClick = onDismiss
            )
        }
    )
}

@Composable
private fun fontPresetTitle(preset: Int): String {
    return stringResource(
        when (preset) {
            FONT_PRESET_NOTO_SERIF_SC -> R.string.font_noto_serif_sc
            FONT_PRESET_SYSTEM_SANS -> R.string.font_system_sans
            FONT_PRESET_ZCOOL_XIAOWEI -> R.string.font_zcool_xiaowei
            FONT_PRESET_SYSTEM_SERIF -> R.string.font_serif
            FONT_PRESET_DEFAULT -> R.string.font_default
            else -> R.string.font_noto_serif_sc
        }
    )
}

@Composable
private fun fontPresetDescription(preset: Int): String {
    return stringResource(
        when (preset) {
            FONT_PRESET_NOTO_SERIF_SC -> R.string.font_noto_serif_sc_desc
            FONT_PRESET_SYSTEM_SANS -> R.string.font_system_sans_desc
            FONT_PRESET_ZCOOL_XIAOWEI -> R.string.font_zcool_xiaowei_desc
            FONT_PRESET_SYSTEM_SERIF -> R.string.font_serif_desc
            FONT_PRESET_DEFAULT -> R.string.font_default_desc
            else -> R.string.font_noto_serif_sc_desc
        }
    )
}

@Composable
private fun SettingsSupportText(
    text: String,
    modifier: Modifier = Modifier
) {
    if (text.isBlank()) return
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun SettingsTrailingSupportText(text: String) {
    if (text.isBlank()) return
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun LegacyDisplaySettingsContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimeApplication
    if (app == null) return
    val prefs = app.userPrefs
    val scope = rememberCoroutineScope()
    val activity = context.findActivity()

    val languageMode by prefs.languageModeFlow.collectAsState(initial = LANG_ZH)
    val showHours by prefs.showHoursFlow.collectAsState(initial = true)
    val homeDensityMode by prefs.homeDensityModeFlow.collectAsState(initial = 1)
    val showMilestone by prefs.showMilestoneFlow.collectAsState(initial = true)
    val reduceMotionEnabled by prefs.reduceMotionEnabledFlow.collectAsState(initial = false)
    val songSoundEnabled by prefs.songSoundEnabledFlow.collectAsState(initial = false)
    val milestoneRemindEnabled by prefs.milestoneRemindEnabledFlow.collectAsState(initial = false)
    val milestoneRemindDaysAhead by prefs.milestoneRemindDaysAheadFlow.collectAsState(initial = 7)
    val milestoneRemindTimeMinutesOfDay by prefs.milestoneRemindTimeMinutesOfDayFlow.collectAsState(initial = 480)
    val smartMilestonesEnabled by prefs.smartMilestonesEnabledFlow.collectAsState(initial = true)
    val customMilestones by prefs.customMilestonesFlow.collectAsState(initial = DEFAULT_MILESTONE_DAYS)
    val dateFormatMode by prefs.dateFormatModeFlow.collectAsState(initial = 0)
    var newMilestoneInput by remember { mutableStateOf("") }
    var customMilestoneRemindDaysInput by remember { mutableStateOf(milestoneRemindDaysAhead.toString()) }
    var customMilestoneRemindTimeInput by remember { mutableStateOf(formatMinutesOfDay(milestoneRemindTimeMinutesOfDay)) }
    var customMilestoneRemindTimeError by remember { mutableStateOf(false) }

    LaunchedEffect(milestoneRemindDaysAhead) {
        customMilestoneRemindDaysInput = milestoneRemindDaysAhead.toString()
    }
    LaunchedEffect(milestoneRemindTimeMinutesOfDay) {
        customMilestoneRemindTimeInput = formatMinutesOfDay(milestoneRemindTimeMinutesOfDay)
        customMilestoneRemindTimeError = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Language Section
        SettingsGroupHeader(title = stringResource(R.string.language_title))
        
        SettingsRadioGroup {
            listOf(
                LANG_ZH to stringResource(R.string.language_zh),
                LANG_EN to stringResource(R.string.language_en)
            ).forEach { (value, label) ->
                SettingsRadioRow(
                    label = label,
                    selected = languageMode == value,
                    onClick = {
                        scope.launch {
                            prefs.setLanguageMode(value)
                            withContext(Dispatchers.Main) { activity?.recreate() }
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))
            }
        }

        // Display Section
        SettingsGroupHeader(
            title = stringResource(R.string.settings_show_hours),
            modifier = Modifier.padding(top = 20.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_show_hours),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            ClassicalToggle(
                checked = showHours,
                onCheckedChange = { scope.launch { prefs.setShowHours(it) } }
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_reduce_motion_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingsSupportText(text = stringResource(R.string.settings_reduce_motion_summary))
            }
            ClassicalToggle(
                checked = reduceMotionEnabled,
                onCheckedChange = { scope.launch { prefs.setReduceMotionEnabled(it) } }
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_song_sound_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingsSupportText(text = stringResource(R.string.settings_song_sound_summary))
            }
            ClassicalToggle(
                checked = songSoundEnabled,
                onCheckedChange = { scope.launch { prefs.setSongSoundEnabled(it) } }
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        Text(
            text = stringResource(R.string.home_density_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )
        SettingsRadioGroup {
            SettingsRadioRow(
                label = stringResource(R.string.home_density_compact),
                selected = homeDensityMode == 0,
                onClick = { scope.launch { prefs.setHomeDensityMode(0) } },
                supportingText = stringResource(R.string.home_density_compact_summary)
            )
            SettingsRadioRow(
                label = stringResource(R.string.home_density_detailed),
                selected = homeDensityMode == 1,
                onClick = { scope.launch { prefs.setHomeDensityMode(1) } },
                supportingText = stringResource(R.string.home_density_detailed_summary)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_smart_milestones_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingsSupportText(text = stringResource(R.string.settings_smart_milestones_summary))
            }
            ClassicalToggle(
                checked = smartMilestonesEnabled,
                onCheckedChange = {
                    scope.launch {
                        prefs.setSmartMilestonesEnabled(it)
                        rescheduleMilestoneReminders(app)
                    }
                }
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_show_milestone),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            ClassicalToggle(
                checked = showMilestone,
                onCheckedChange = { scope.launch { prefs.setShowMilestone(it) } }
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_milestone_remind_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingsSupportText(text = stringResource(R.string.settings_milestone_remind_summary))
            }
            ClassicalToggle(
                checked = milestoneRemindEnabled,
                onCheckedChange = {
                    scope.launch {
                        prefs.setMilestoneRemindEnabled(it)
                        rescheduleMilestoneReminders(app)
                    }
                }
            )
        }
        if (milestoneRemindEnabled) {
            Text(
                text = stringResource(R.string.settings_milestone_remind_days_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1 to R.string.settings_milestone_remind_days_1,
                    3 to R.string.settings_milestone_remind_days_3,
                    7 to R.string.settings_milestone_remind_days_7,
                    14 to R.string.settings_milestone_remind_days_14).forEach { (days, resId) ->
                    SongFilterChip(
                        selected = milestoneRemindDaysAhead == days,
                        onClick = {
                            scope.launch {
                                prefs.setMilestoneRemindDaysAhead(days)
                                rescheduleMilestoneReminders(app)
                            }
                        },
                        label = stringResource(resId)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customMilestoneRemindDaysInput,
                    onValueChange = {
                        customMilestoneRemindDaysInput = it.filter { c -> c.isDigit() }
                    },
                    label = { Text(stringResource(R.string.settings_reminder_custom_days_label)) },
                    placeholder = { Text(stringResource(R.string.settings_reminder_custom_days_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(4.dp)
                )
                OutlinedButton(
                    onClick = {
                        val parsed = customMilestoneRemindDaysInput.toIntOrNull() ?: return@OutlinedButton
                        scope.launch {
                            prefs.setMilestoneRemindDaysAhead(parsed)
                            rescheduleMilestoneReminders(app)
                        }
                    },
                    enabled = customMilestoneRemindDaysInput.isNotBlank(),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(stringResource(R.string.action_apply))
                }
            }
            Text(
                text = stringResource(R.string.settings_milestone_remind_time_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    stringResource(R.string.reminder_time_6) to 360,
                    stringResource(R.string.reminder_time_8) to 480,
                    stringResource(R.string.reminder_time_9) to 540,
                    stringResource(R.string.reminder_time_12) to 720,
                    stringResource(R.string.reminder_time_18) to 1080
                ).forEach { (label, minutes) ->
                    SongFilterChip(
                        selected = milestoneRemindTimeMinutesOfDay == minutes,
                        onClick = {
                            scope.launch {
                                prefs.setMilestoneRemindTimeMinutesOfDay(minutes)
                                rescheduleMilestoneReminders(app)
                            }
                        },
                        label = label
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customMilestoneRemindTimeInput,
                    onValueChange = {
                        customMilestoneRemindTimeInput = it.filter { c -> c.isDigit() || c == ':' }.take(5)
                        customMilestoneRemindTimeError = false
                    },
                    label = { Text(stringResource(R.string.settings_milestone_remind_time_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = customMilestoneRemindTimeError,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(4.dp)
                )
                OutlinedButton(
                    onClick = {
                        val parsed = parseReminderTimeInput(customMilestoneRemindTimeInput)
                        if (parsed == null) {
                            customMilestoneRemindTimeError = true
                        } else {
                            scope.launch {
                                customMilestoneRemindTimeError = false
                                prefs.setMilestoneRemindTimeMinutesOfDay(parsed)
                                rescheduleMilestoneReminders(app)
                            }
                        }
                    },
                    enabled = customMilestoneRemindTimeInput.isNotBlank(),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(stringResource(R.string.action_apply))
                }
            }
            if (customMilestoneRemindTimeError) {
                Text(
                    text = stringResource(R.string.custom_reminder_time_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        Text(
            text = stringResource(R.string.settings_custom_milestones_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newMilestoneInput,
                onValueChange = { newMilestoneInput = it.filter { c -> c.isDigit() } },
                placeholder = { Text(stringResource(R.string.settings_custom_milestones_add_hint)) },
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = {
                    val v = newMilestoneInput.trim().toLongOrNull()
                    if (v != null && v > 0) {
                        scope.launch {
                            prefs.setCustomMilestones(customMilestones + v)
                            newMilestoneInput = ""
                            rescheduleMilestoneReminders(app)
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.settings_custom_milestones_add))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            customMilestones.forEach { days ->
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(SongDesignTokens.StandardRadius.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$days",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = {
                            scope.launch {
                                prefs.setCustomMilestones(customMilestones.filter { it != days })
                                rescheduleMilestoneReminders(app)
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        SongLineIcon(
                            kind = SongLineIconKind.Delete,
                            contentDescription = stringResource(R.string.cd_delete_custom_milestone, days),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 16.dp
                        )
                    }
                }
            }
        }
        OutlinedButton(
            onClick = {
                scope.launch {
                    prefs.setCustomMilestones(DEFAULT_MILESTONE_DAYS)
                    rescheduleMilestoneReminders(app)
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.settings_custom_milestones_restore))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong), modifier = Modifier.padding(top = 12.dp))

        Text(
            text = stringResource(R.string.date_format_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )
        SettingsRadioGroup {
            SettingsRadioRow(
                label = stringResource(R.string.date_format_dot),
                selected = dateFormatMode == 0,
                onClick = { scope.launch { prefs.setDateFormatMode(0) } }
            )
            SettingsRadioRow(
                label = stringResource(R.string.date_format_dash),
                selected = dateFormatMode == 1,
                onClick = { scope.launch { prefs.setDateFormatMode(1) } }
            )
        }
    }
}

@Composable
fun DisplaySettingsContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimeApplication
    if (app == null) return
    val prefs = app.userPrefs
    val scope = rememberCoroutineScope()
    val activity = context.findActivity()

    val languageMode by prefs.languageModeFlow.collectAsState(initial = LANG_ZH)
    val showHours by prefs.showHoursFlow.collectAsState(initial = true)
    val homeDensityMode by prefs.homeDensityModeFlow.collectAsState(initial = 1)
    val reduceMotionEnabled by prefs.reduceMotionEnabledFlow.collectAsState(initial = false)
    val songSoundEnabled by prefs.songSoundEnabledFlow.collectAsState(initial = false)
    val dateFormatMode by prefs.dateFormatModeFlow.collectAsState(initial = 0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SettingsExpandableSection(
            title = stringResource(R.string.settings_display_section_language_title),
            summary = stringResource(R.string.settings_display_section_language_summary),
            initiallyExpanded = true
        ) {
            SettingsRadioGroup {
                listOf(
                    LANG_ZH to stringResource(R.string.language_zh),
                    LANG_EN to stringResource(R.string.language_en)
                ).forEach { (value, label) ->
                    SettingsRadioRow(
                        label = label,
                        selected = languageMode == value,
                        onClick = {
                            scope.launch {
                                prefs.setLanguageMode(value)
                                withContext(Dispatchers.Main) { activity?.recreate() }
                            }
                        }
                    )
                }
            }
        }

        SettingsExpandableSection(
            title = stringResource(R.string.settings_display_section_home_title),
            summary = stringResource(R.string.settings_display_section_home_summary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_show_hours),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                ClassicalToggle(
                    checked = showHours,
                    onCheckedChange = { scope.launch { prefs.setShowHours(it) } }
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_reduce_motion_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SettingsSupportText(text = stringResource(R.string.settings_reduce_motion_summary))
                }
                ClassicalToggle(
                    checked = reduceMotionEnabled,
                    onCheckedChange = { scope.launch { prefs.setReduceMotionEnabled(it) } }
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_song_sound_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SettingsSupportText(text = stringResource(R.string.settings_song_sound_summary))
                }
                ClassicalToggle(
                    checked = songSoundEnabled,
                    onCheckedChange = { scope.launch { prefs.setSongSoundEnabled(it) } }
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

            SettingsRadioGroup {
                listOf(
                    Triple(0, R.string.home_density_compact, R.string.home_density_compact_summary),
                    Triple(1, R.string.home_density_detailed, R.string.home_density_detailed_summary)
                ).forEach { (value, titleRes, summaryRes) ->
                    SettingsRadioRow(
                        label = stringResource(titleRes),
                        selected = homeDensityMode == value,
                        onClick = { scope.launch { prefs.setHomeDensityMode(value) } },
                        supportingText = stringResource(summaryRes)
                    )
                }
            }
        }

        SettingsExpandableSection(
            title = stringResource(R.string.settings_display_section_date_title),
            summary = stringResource(R.string.settings_display_section_date_summary)
        ) {
            SettingsRadioGroup {
                listOf(
                    0 to R.string.date_format_dot,
                    1 to R.string.date_format_dash
                ).forEach { (value, labelRes) ->
                    SettingsRadioRow(
                        label = stringResource(labelRes),
                        selected = dateFormatMode == value,
                        onClick = { scope.launch { prefs.setDateFormatMode(value) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsReminderTimePickerWheels(
    hourOptions: List<Int>,
    minuteOptions: List<Int>,
    selectedHour: Int,
    selectedMinute: Int,
    onHourSelected: (Int) -> Unit,
    onMinuteSelected: (Int) -> Unit,
    onHourScrollStateChanged: (Boolean) -> Unit,
    onMinuteScrollStateChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.reminder_time_hour),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SnapWheelPicker(
                items = hourOptions,
                selectedItem = selectedHour,
                onItemSelected = onHourSelected,
                accessibilityLabel = stringResource(R.string.reminder_time_hour),
                onScrollStateChanged = onHourScrollStateChanged,
                modifier = Modifier.fillMaxWidth(),
                itemLabel = { value -> String.format(Locale.US, "%02d", value) }
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.reminder_time_minute),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SnapWheelPicker(
                items = minuteOptions,
                selectedItem = selectedMinute,
                onItemSelected = onMinuteSelected,
                accessibilityLabel = stringResource(R.string.reminder_time_minute),
                onScrollStateChanged = onMinuteScrollStateChanged,
                modifier = Modifier.fillMaxWidth(),
                itemLabel = { value -> String.format(Locale.US, "%02d", value) }
            )
        }
    }
}

@Composable
fun MilestoneSettingsContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimeApplication
    if (app == null) return
    val prefs = app.userPrefs
    val scope = rememberCoroutineScope()

    val showMilestone by prefs.showMilestoneFlow.collectAsState(initial = true)
    val smartMilestonesEnabled by prefs.smartMilestonesEnabledFlow.collectAsState(initial = true)
    val milestoneRemindEnabled by prefs.milestoneRemindEnabledFlow.collectAsState(initial = false)
    val milestoneRemindDaysAhead by prefs.milestoneRemindDaysAheadFlow.collectAsState(initial = 7)
    val milestoneRemindTimeMinutesOfDay by prefs.milestoneRemindTimeMinutesOfDayFlow.collectAsState(initial = 480)
    val defaultEventRemindEnabled by prefs.defaultEventRemindEnabledFlow.collectAsState(
        initial = DEFAULT_NEW_EVENT_REMIND_ENABLED
    )
    val defaultEventRemindDaysBefore by prefs.defaultEventRemindDaysBeforeFlow.collectAsState(
        initial = DEFAULT_NEW_EVENT_REMIND_DAYS_BEFORE
    )
    val defaultEventRemindTimeMinutesOfDay by prefs.defaultEventRemindTimeMinutesOfDayFlow.collectAsState(
        initial = DEFAULT_NEW_EVENT_REMIND_TIME_MINUTES_OF_DAY
    )
    val customMilestones by prefs.customMilestonesFlow.collectAsState(initial = DEFAULT_MILESTONE_DAYS)
    val scheduleTargetCalendarId by prefs.scheduleTargetCalendarIdFlow.collectAsState(initial = null)
    val scheduleUseRRuleSync by prefs.scheduleUseRRuleSyncFlow.collectAsState(initial = true)

    var newMilestoneInput by remember { mutableStateOf("") }
    val leadTimePresets = remember { listOf(0, 1, 3, 7, 30) }
    var defaultReminderCustomDaysInput by rememberSaveable { mutableStateOf("") }
    var milestoneReminderCustomDaysInput by rememberSaveable { mutableStateOf("") }
    val remindHourOptions = remember { (0..23).toList() }
    val remindMinuteOptions = remember { (0..59).toList() }
    var showDefaultEventReminderTimePicker by rememberSaveable { mutableStateOf(false) }
    var showMilestoneReminderTimePicker by rememberSaveable { mutableStateOf(false) }

    var writableCalendars by remember { mutableStateOf<List<ScheduleSyncManager.CalendarOption>>(emptyList()) }
    var latestScheduleSyncEvent by remember { mutableStateOf<Event?>(null) }
    var syncStatusLoading by remember { mutableStateOf(false) }
    var scheduleSyncStatusLoaded by rememberSaveable { mutableStateOf(false) }
    var permissionDialog by remember { mutableStateOf<PermissionDialogSpec?>(null) }
    val calendarPermissions = remember {
        arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    }

    fun hasCalendarPermission(): Boolean {
        return context.hasCalendarReadWritePermission()
    }

    val refreshScheduleSyncStatus: suspend () -> Unit = {
        syncStatusLoading = true
        try {
            latestScheduleSyncEvent = withContext(Dispatchers.IO) {
                app.repository.getLatestScheduleSyncEvent()
            }
            writableCalendars = if (hasCalendarPermission()) {
                withContext(Dispatchers.IO) {
                    ScheduleSyncManager.getWritableCalendars(context)
                }
            } else {
                emptyList()
            }
            scheduleSyncStatusLoaded = true
        } finally {
            syncStatusLoading = false
        }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        scope.launch {
            refreshScheduleSyncStatus()
        }
    }

    fun launchCalendarPermissionRequest() {
        context.markCalendarPermissionRequested()
        calendarPermissionLauncher.launch(calendarPermissions)
    }

    fun requestCalendarPermissionAccess() {
        when {
            hasCalendarPermission() -> scope.launch { refreshScheduleSyncStatus() }
            context.shouldShowCalendarPermissionRationaleCompat() -> {
                permissionDialog = PermissionDialogSpec(
                    title = context.getString(R.string.permission_dialog_title_calendar),
                    message = context.getString(R.string.calendar_permission_rationale_message),
                    confirmText = context.getString(R.string.permission_dialog_button_continue),
                    dismissText = context.getString(R.string.permission_dialog_button_not_now),
                    onConfirm = {
                        permissionDialog = null
                        launchCalendarPermissionRequest()
                    },
                    onDismiss = { permissionDialog = null }
                )
            }
            context.wasCalendarPermissionRequestedBefore() -> {
                permissionDialog = PermissionDialogSpec(
                    title = context.getString(R.string.permission_dialog_title_calendar),
                    message = context.getString(R.string.calendar_permission_settings_message),
                    confirmText = context.getString(R.string.permission_dialog_button_open_settings),
                    dismissText = context.getString(R.string.permission_dialog_button_not_now),
                    onConfirm = {
                        permissionDialog = null
                        context.openAppDetailsSettings()
                    },
                    onDismiss = { permissionDialog = null }
                )
            }
            else -> launchCalendarPermissionRequest()
        }
    }

    LaunchedEffect(writableCalendars, scheduleTargetCalendarId, scheduleSyncStatusLoaded) {
        if (!scheduleSyncStatusLoaded) return@LaunchedEffect
        if (!hasCalendarPermission()) return@LaunchedEffect
        val selected = scheduleTargetCalendarId
        if (selected != null && writableCalendars.none { it.id == selected }) {
            prefs.setScheduleTargetCalendarId(null)
        }
    }

    if (showDefaultEventReminderTimePicker) {
        var draftHour by remember(defaultEventRemindTimeMinutesOfDay) {
            mutableStateOf((defaultEventRemindTimeMinutesOfDay / 60).coerceIn(0, 23))
        }
        var draftMinute by remember(defaultEventRemindTimeMinutesOfDay) {
            mutableStateOf((defaultEventRemindTimeMinutesOfDay % 60).coerceIn(0, 59))
        }
        var isHourPickerScrolling by remember { mutableStateOf(false) }
        var isMinutePickerScrolling by remember { mutableStateOf(false) }

        SongWheelPickerDialog(
            title = stringResource(R.string.settings_default_event_remind_time_title),
            onDismissRequest = { showDefaultEventReminderTimePicker = false },
            confirmEnabled = !isHourPickerScrolling && !isMinutePickerScrolling,
            onConfirm = {
                val updatedMinutes = draftHour * 60 + draftMinute
                if (updatedMinutes != defaultEventRemindTimeMinutesOfDay) {
                    scope.launch {
                        prefs.setDefaultEventRemindTimeMinutesOfDay(updatedMinutes)
                    }
                }
                showDefaultEventReminderTimePicker = false
            }
        ) {
            SettingsReminderTimePickerWheels(
                hourOptions = remindHourOptions,
                minuteOptions = remindMinuteOptions,
                selectedHour = draftHour,
                selectedMinute = draftMinute,
                onHourSelected = { draftHour = it },
                onMinuteSelected = { draftMinute = it },
                onHourScrollStateChanged = { isHourPickerScrolling = it },
                onMinuteScrollStateChanged = { isMinutePickerScrolling = it }
            )
        }
    }

    if (showMilestoneReminderTimePicker) {
        var draftHour by remember(milestoneRemindTimeMinutesOfDay) {
            mutableStateOf((milestoneRemindTimeMinutesOfDay / 60).coerceIn(0, 23))
        }
        var draftMinute by remember(milestoneRemindTimeMinutesOfDay) {
            mutableStateOf((milestoneRemindTimeMinutesOfDay % 60).coerceIn(0, 59))
        }
        var isHourPickerScrolling by remember { mutableStateOf(false) }
        var isMinutePickerScrolling by remember { mutableStateOf(false) }

        SongWheelPickerDialog(
            title = stringResource(R.string.settings_milestone_remind_time_title),
            onDismissRequest = { showMilestoneReminderTimePicker = false },
            confirmEnabled = !isHourPickerScrolling && !isMinutePickerScrolling,
            onConfirm = {
                val updatedMinutes = draftHour * 60 + draftMinute
                if (updatedMinutes != milestoneRemindTimeMinutesOfDay) {
                    scope.launch {
                        prefs.setMilestoneRemindTimeMinutesOfDay(updatedMinutes)
                        RescheduleAllWorker.enqueue(context, "milestone_remind_time_changed")
                    }
                }
                showMilestoneReminderTimePicker = false
            }
        ) {
            SettingsReminderTimePickerWheels(
                hourOptions = remindHourOptions,
                minuteOptions = remindMinuteOptions,
                selectedHour = draftHour,
                selectedMinute = draftMinute,
                onHourSelected = { draftHour = it },
                onMinuteSelected = { draftMinute = it },
                onHourScrollStateChanged = { isHourPickerScrolling = it },
                onMinuteScrollStateChanged = { isMinutePickerScrolling = it }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        permissionDialog?.let { dialog ->
            PermissionActionDialog(spec = dialog)
        }
        SettingsExpandableSection(
            title = stringResource(R.string.settings_section_default_reminder_title),
            summary = stringResource(R.string.settings_section_default_reminder_summary),
            initiallyExpanded = true
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_default_event_remind_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingsSupportText(text = stringResource(R.string.settings_default_event_remind_summary))
            }
            ClassicalToggle(
                checked = defaultEventRemindEnabled,
                onCheckedChange = {
                    scope.launch {
                        prefs.setDefaultEventRemindEnabled(it)
                    }
                }
            )
        }
        if (defaultEventRemindEnabled) {
            val selectedDays = defaultEventRemindDaysBefore.coerceIn(0, 3650)

            Text(
                text = stringResource(R.string.settings_default_event_remind_days_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
            ReminderLeadTimePresetRow(
                selectedDays = selectedDays,
                leadTimePresets = leadTimePresets,
                onSelected = { days ->
                    if (days != defaultEventRemindDaysBefore) {
                        scope.launch {
                            prefs.setDefaultEventRemindDaysBefore(days)
                        }
                    }
                    defaultReminderCustomDaysInput = ""
                }
            )
            OutlinedTextField(
                value = defaultReminderCustomDaysInput,
                onValueChange = { raw ->
                    val filtered = raw.filter { it.isDigit() }.take(4)
                    defaultReminderCustomDaysInput = filtered
                    filtered.toIntOrNull()?.coerceIn(0, 3650)?.let { days ->
                        if (days != defaultEventRemindDaysBefore) {
                            scope.launch {
                                prefs.setDefaultEventRemindDaysBefore(days)
                            }
                        }
                    }
                },
                label = { Text(stringResource(R.string.settings_reminder_custom_days_label)) },
                placeholder = { Text(stringResource(R.string.settings_reminder_custom_days_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )

            SettingsValueRow(
                label = stringResource(R.string.settings_default_event_remind_time_title),
                value = formatMinutesOfDay(defaultEventRemindTimeMinutesOfDay),
                onClick = { showDefaultEventReminderTimePicker = true },
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        }

        SettingsExpandableSection(
            title = stringResource(R.string.settings_section_milestone_display_title),
            summary = stringResource(R.string.settings_section_milestone_display_summary)
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_show_milestone),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingsSupportText(text = stringResource(R.string.settings_show_milestone_summary))
            }
            ClassicalToggle(
                checked = showMilestone,
                onCheckedChange = { scope.launch { prefs.setShowMilestone(it) } }
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_smart_milestones_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingsSupportText(text = stringResource(R.string.settings_smart_milestones_summary))
            }
            ClassicalToggle(
                checked = smartMilestonesEnabled,
                onCheckedChange = {
                    scope.launch {
                        prefs.setSmartMilestonesEnabled(it)
                        rescheduleMilestoneReminders(app)
                    }
                }
            )
        }
        }

        SettingsExpandableSection(
            title = stringResource(R.string.settings_section_milestone_reminder_title),
            summary = stringResource(R.string.settings_section_milestone_reminder_summary)
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_milestone_remind_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingsSupportText(text = stringResource(R.string.settings_milestone_remind_summary))
            }
            ClassicalToggle(
                checked = milestoneRemindEnabled,
                onCheckedChange = {
                    scope.launch {
                        prefs.setMilestoneRemindEnabled(it)
                        RescheduleAllWorker.enqueue(context, "milestone_remind_toggle")
                    }
                }
            )
        }
        if (milestoneRemindEnabled) {
            val selectedDays = milestoneRemindDaysAhead.coerceIn(0, 3650)

            Text(
                text = stringResource(R.string.settings_milestone_remind_days_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
            ReminderLeadTimePresetRow(
                selectedDays = selectedDays,
                leadTimePresets = leadTimePresets,
                onSelected = { days ->
                    if (days != milestoneRemindDaysAhead) {
                        scope.launch {
                            prefs.setMilestoneRemindDaysAhead(days)
                            RescheduleAllWorker.enqueue(context, "milestone_remind_days_changed")
                        }
                    }
                    milestoneReminderCustomDaysInput = ""
                }
            )
            OutlinedTextField(
                value = milestoneReminderCustomDaysInput,
                onValueChange = { raw ->
                    val filtered = raw.filter { it.isDigit() }.take(4)
                    milestoneReminderCustomDaysInput = filtered
                    filtered.toIntOrNull()?.coerceIn(0, 3650)?.let { days ->
                        if (days != milestoneRemindDaysAhead) {
                            scope.launch {
                                prefs.setMilestoneRemindDaysAhead(days)
                                RescheduleAllWorker.enqueue(context, "milestone_remind_days_changed")
                            }
                        }
                    }
                },
                label = { Text(stringResource(R.string.settings_reminder_custom_days_label)) },
                placeholder = { Text(stringResource(R.string.settings_reminder_custom_days_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )

            SettingsValueRow(
                label = stringResource(R.string.settings_milestone_remind_time_title),
                value = formatMinutesOfDay(milestoneRemindTimeMinutesOfDay),
                onClick = { showMilestoneReminderTimePicker = true },
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        }

        SettingsExpandableSection(
            title = stringResource(R.string.settings_section_schedule_sync_title),
            summary = stringResource(R.string.settings_schedule_sync_summary),
            onExpandedChange = { expanded ->
                if (expanded && !scheduleSyncStatusLoaded) {
                    scope.launch { refreshScheduleSyncStatus() }
                }
            }
        ) {

        Text(
            text = stringResource(R.string.settings_schedule_target_calendar_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
        val calendarPermissionGranted = hasCalendarPermission()
        val scheduleHealthEvent = latestScheduleSyncEvent ?: Event(
            id = 0,
            title = stringResource(R.string.settings_section_schedule_sync_title),
            date = System.currentTimeMillis(),
            category = CATEGORY_OTHER,
            remindEnabled = true,
            syncToScheduleEnabled = true
        )
        val scheduleHealthStatus = buildReminderStatus(
            event = scheduleHealthEvent.copy(remindEnabled = true, syncToScheduleEnabled = true),
            notificationsEnabled = context.areAppNotificationsEnabledCompat(),
            calendarPermissionGranted = calendarPermissionGranted,
            hasWritableCalendar = writableCalendars.isNotEmpty()
        )
        SongReminderStatusStrip(
            status = scheduleHealthStatus,
            title = settingsReminderStatusTitle(context, scheduleHealthStatus),
            actionLabel = if (scheduleHealthStatus.primaryAction != ReminderStatusAction.None) {
                stringResource(R.string.reminder_status_action_open_settings)
            } else {
                null
            },
            onActionClick = if (scheduleHealthStatus.primaryAction != ReminderStatusAction.None) {
                { requestCalendarPermissionAccess() }
            } else {
                null
            },
            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
        )

        SettingsRadioGroup {
            SettingsRadioRow(
                label = stringResource(R.string.settings_schedule_calendar_auto),
                selected = scheduleTargetCalendarId == null,
                onClick = {
                        if (!calendarPermissionGranted) {
                            requestCalendarPermissionAccess()
                        } else {
                            scope.launch {
                                prefs.setScheduleTargetCalendarId(null)
                                RescheduleAllWorker.enqueue(context, "schedule_target_calendar_changed")
                            }
                        }
                    }
            )

            if (!calendarPermissionGranted) {
                Text(
                    text = stringResource(R.string.calendar_permission_required_for_sync),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                OutlinedButton(
                    onClick = { requestCalendarPermissionAccess() },
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(stringResource(R.string.settings_schedule_request_calendar_permission))
                }
            } else if (writableCalendars.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_schedule_calendar_no_writable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                writableCalendars.forEach { calendar ->
                    SettingsRadioRow(
                        label = calendar.label,
                        selected = scheduleTargetCalendarId == calendar.id,
                        onClick = {
                                if (!calendarPermissionGranted) {
                                    requestCalendarPermissionAccess()
                                } else {
                                    scope.launch {
                                        prefs.setScheduleTargetCalendarId(calendar.id)
                                        RescheduleAllWorker.enqueue(context, "schedule_target_calendar_changed")
                                    }
                                }
                            }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_schedule_rrule_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SettingsSupportText(text = stringResource(R.string.settings_schedule_rrule_summary))
            }
            ClassicalToggle(
                checked = scheduleUseRRuleSync,
                onCheckedChange = { enabled ->
                    scope.launch {
                        prefs.setScheduleUseRRuleSync(enabled)
                        RescheduleAllWorker.enqueue(context, "schedule_rrule_changed")
                    }
                }
            )
        }

        Text(
            text = stringResource(R.string.settings_schedule_status_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )

        val lastSyncText = latestScheduleSyncEvent?.lastScheduleSyncAt?.let {
            formatScheduleSyncTime(it)
        } ?: stringResource(R.string.settings_schedule_status_never)
        Text(
            text = stringResource(R.string.settings_schedule_status_last_sync, lastSyncText),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        val targetCalendarText = latestScheduleSyncEvent?.targetCalendarId?.let { calendarId ->
            writableCalendars.firstOrNull { it.id == calendarId }?.label ?: calendarId.toString()
        } ?: stringResource(R.string.settings_schedule_calendar_auto)
        Text(
            text = stringResource(R.string.settings_schedule_status_calendar_format, targetCalendarText),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        val lastSyncError = latestScheduleSyncEvent?.lastScheduleSyncError
        Text(
            text = if (lastSyncError.isNullOrBlank()) {
                stringResource(R.string.settings_schedule_status_ok)
            } else {
                stringResource(R.string.settings_schedule_status_error_format, lastSyncError)
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (lastSyncError.isNullOrBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            },
            modifier = Modifier.padding(top = 2.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { RescheduleAllWorker.enqueue(context, "manual_settings_reschedule_all") },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.settings_schedule_rebuild_now))
            }
            OutlinedButton(
                onClick = { scope.launch { refreshScheduleSyncStatus() } },
                enabled = !syncStatusLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.settings_schedule_refresh_status))
            }
        }
        }

        SettingsExpandableSection(
            title = stringResource(R.string.settings_section_custom_milestones_title),
            summary = stringResource(R.string.settings_section_custom_milestones_summary)
        ) {
        Text(
            text = stringResource(R.string.settings_custom_milestones_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newMilestoneInput,
                onValueChange = { newMilestoneInput = it.filter { c -> c.isDigit() } },
                placeholder = { Text(stringResource(R.string.settings_custom_milestones_add_hint)) },
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = {
                    val v = newMilestoneInput.trim().toLongOrNull()
                    if (v != null && v > 0) {
                        scope.launch {
                            prefs.setCustomMilestones(customMilestones + v)
                            newMilestoneInput = ""
                            RescheduleAllWorker.enqueue(context, "custom_milestones_changed")
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.settings_custom_milestones_add))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            customMilestones.forEach { days ->
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(SongDesignTokens.StandardRadius.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$days",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = {
                            scope.launch {
                                prefs.setCustomMilestones(customMilestones.filter { it != days })
                                RescheduleAllWorker.enqueue(context, "custom_milestones_changed")
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        SongLineIcon(
                            kind = SongLineIconKind.Delete,
                            contentDescription = stringResource(R.string.cd_delete_custom_milestone, days),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 16.dp
                        )
                    }
                }
            }
        }
        OutlinedButton(
            onClick = {
                scope.launch {
                    prefs.setCustomMilestones(DEFAULT_MILESTONE_DAYS)
                    RescheduleAllWorker.enqueue(context, "custom_milestones_reset")
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.settings_custom_milestones_restore))
        }
        }
    }
}

@Composable
private fun ReminderLeadTimePresetRow(
    selectedDays: Int,
    leadTimePresets: List<Int>,
    onSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadTimePresets.forEach { days ->
            val label = if (days == 0) {
                stringResource(R.string.remind_same_day)
            } else {
                context.resources.getQuantityString(
                    R.plurals.remind_days_before_format,
                    days,
                    days
                )
            }
            SongFilterChip(
                selected = selectedDays == days,
                onClick = { onSelected(days) },
                label = label
            )
        }
    }
}

@Composable
fun DataSettingsContent(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimeApplication
    if (app == null) return
    val repository = app.repository
    val scope = rememberCoroutineScope()

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var importResultMessage by remember { mutableStateOf<String?>(null) }
    var pendingImportPreview by remember { mutableStateOf<PendingImportPreview?>(null) }
    var pendingExportText by remember { mutableStateOf<String?>(null) }

    suspend fun importEvents(events: List<Event>): ImportExecutionResult {
        if (events.isEmpty()) return ImportExecutionResult(0, 0, 0)
        var successCount = 0
        var failedCount = 0
        var warningCount = 0
        val preferredCalendarId = app.userPrefs.scheduleTargetCalendarIdFlow.first()
        val useRRuleSync = app.userPrefs.scheduleUseRRuleSyncFlow.first()

        events.forEach { sourceEvent ->
            val event = sourceEvent.sanitizedReminderConfig()
            val newId = try {
                repository.insertEvent(event)
            } catch (_: Exception) {
                failedCount += 1
                return@forEach
            }
            val savedEvent = event.copy(id = newId.toInt(), scheduleEventId = null)

            var hasWarning = false
            if (savedEvent.remindEnabled) {
                try {
                    scheduleReminder(context, savedEvent)
                } catch (_: Exception) {
                    hasWarning = true
                }
            }

            val syncResult = try {
                if (savedEvent.syncToScheduleEnabled) {
                    ScheduleSyncManager.syncReminderSeries(
                        context = context,
                        event = savedEvent,
                        preferredCalendarId = preferredCalendarId,
                        useRRuleSync = useRRuleSync
                    )
                } else {
                    val cleanup = ScheduleSyncManager.removeScheduleReminderByEventId(
                        context,
                        savedEvent.id
                    )
                    ScheduleSyncManager.scheduleSyncResultAfterCleanup(
                        event = savedEvent,
                        primaryScheduleEventId = null,
                        targetCalendarId = null,
                        lastSyncAt = System.currentTimeMillis(),
                        cleanupResult = cleanup
                    )
                }
            } catch (_: Exception) {
                hasWarning = true
                null
            }

            if (syncResult != null) {
                if (syncResult.error != null) hasWarning = true
                try {
                    repository.updateEvent(
                        eventAfterScheduleSyncAttempt(savedEvent, syncResult)
                    )
                } catch (_: Exception) {
                    hasWarning = true
                }
            }

            successCount += 1
            if (hasWarning) {
                warningCount += 1
            }
        }

        if (successCount > 0) {
            try {
                rescheduleMilestoneReminders(app)
            } catch (_: Exception) {
                warningCount += 1
            }
            try {
                WidgetUpdater.refreshCountdownWidgets(context)
            } catch (_: Exception) {
                warningCount += 1
            }
        }

        return ImportExecutionResult(successCount, failedCount, warningCount)
    }

    fun importFailureText(parseResult: BackupParseResult): String {
        return when (parseResult.failure) {
            BackupParseFailure.EMPTY_FILE -> context.getString(R.string.import_error_empty_file)
            BackupParseFailure.NO_EVENTS_FOUND -> context.getString(R.string.import_error_no_events)
            BackupParseFailure.UNSUPPORTED_FORMAT,
            null -> context.getString(R.string.import_error_unsupported_format)
        }
    }

    fun importResultText(
        preview: PendingImportPreview,
        executionResult: ImportExecutionResult
    ): String {
        return context.getString(
            R.string.import_completed_detailed,
            preview.parseResult.recognizedCount,
            executionResult.successCount,
            preview.totalSkippedDuplicates,
            preview.parseResult.parseErrorCount + executionResult.failedCount,
            executionResult.warningCount
        )
    }

    suspend fun buildImportPreview(parseResult: BackupParseResult): PendingImportPreview? {
        if (parseResult.failure != null) {
            importResultMessage = importFailureText(parseResult)
            return null
        }
        val existingEvents = repository.getAllEventsSnapshot()
        val duplicateFilter = filterExistingDuplicateEvents(
            events = parseResult.events,
            existingEvents = existingEvents
        )
        return PendingImportPreview(
            parseResult = parseResult,
            importableEvents = duplicateFilter.importableEvents,
            existingDuplicateCount = duplicateFilter.existingDuplicateCount
        )
    }

    suspend fun parseBytesForPreview(bytes: ByteArray) {
        pendingImportPreview = null
        val parseResult = parseEventsFromBackupBytesDetailed(bytes)
        pendingImportPreview = buildImportPreview(parseResult)
    }

    suspend fun writeExportText(uri: Uri, text: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(text.toByteArray(Charsets.UTF_8))
                } != null
            } catch (_: Exception) {
                false
            }
        }
    }

    suspend fun showExportSaveResult(success: Boolean) {
        snackbarHostState.showSnackbar(
            context.getString(
                if (success) R.string.export_file_saved else R.string.export_file_failed
            )
        )
    }

    val importFromFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                } catch (_: Exception) {
                    ByteArray(0)
                }
            }
            if (bytes.isEmpty()) {
                importResultMessage = context.getString(R.string.import_error_empty_file)
                pendingImportPreview = null
                return@launch
            }
            importResultMessage = null
            parseBytesForPreview(bytes)
        }
    }

    val saveJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val text = pendingExportText
        pendingExportText = null
        if (uri == null || text == null) return@rememberLauncherForActivityResult
        scope.launch {
            showExportSaveResult(writeExportText(uri, text))
        }
    }

    val saveCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val text = pendingExportText
        pendingExportText = null
        if (uri == null || text == null) return@rememberLauncherForActivityResult
        scope.launch {
            showExportSaveResult(writeExportText(uri, text))
        }
    }

    if (showImportDialog) {
        SongFormDialog(
            title = stringResource(R.string.import_events),
            onDismissRequest = {
                showImportDialog = false
                importResultMessage = null
                pendingImportPreview = null
            },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = {
                            importJsonText = it
                            pendingImportPreview = null
                            importResultMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = { Text(stringResource(R.string.import_hint)) },
                        maxLines = 8,
                        shape = RoundedCornerShape(4.dp)
                    )
                    OutlinedButton(
                        onClick = { importFromFileLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(stringResource(R.string.import_from_file))
                    }
                    importResultMessage?.let { msg ->
                        Text(msg, color = MaterialTheme.colorScheme.primary)
                    }
                    pendingImportPreview?.let { preview ->
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaStrong)
                        )
                        Text(
                            text = stringResource(
                                R.string.import_preview_summary,
                                preview.parseResult.recognizedCount,
                                preview.importableEvents.size,
                                preview.totalSkippedDuplicates,
                                preview.parseResult.parseErrorCount
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (preview.importableEvents.isEmpty()) {
                            Text(
                                text = stringResource(R.string.import_preview_empty_after_duplicates),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val sampleEvents = preview.importableEvents.take(5)
                            sampleEvents.forEach { event ->
                                Text(
                                    text = "${event.title} · ${formatImportPreviewDate(event)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val remaining = preview.importableEvents.size - sampleEvents.size
                            if (remaining > 0) {
                                Text(
                                    text = pluralStringResource(R.plurals.import_preview_more, remaining, remaining),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            buttons = {
                SongDialogButton(
                    text = stringResource(R.string.delete_confirm_cancel),
                    onClick = {
                        showImportDialog = false
                        importResultMessage = null
                        pendingImportPreview = null
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                SongDialogButton(
                    text = stringResource(
                        if (pendingImportPreview == null) {
                            R.string.import_preview_action
                        } else {
                            R.string.import_events
                        }
                    ),
                    enabled = pendingImportPreview?.importableEvents?.isNotEmpty()
                        ?: importJsonText.isNotBlank(),
                    onClick = {
                        scope.launch {
                            val preview = pendingImportPreview
                            if (preview == null) {
                                importResultMessage = null
                                parseBytesForPreview(importJsonText.toByteArray(Charsets.UTF_8))
                                return@launch
                            }

                            val executionResult = importEvents(preview.importableEvents)
                            importResultMessage = importResultText(preview, executionResult)
                            pendingImportPreview = null
                            if (executionResult.successCount > 0) {
                                importJsonText = ""
                            }
                        }
                    }
                )
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SettingsGroupHeader(title = stringResource(R.string.export_import))

        SettingsActionRow(
            label = stringResource(R.string.export_events),
            onClick = {
                scope.launch {
                    val events = repository.getAllEventsSnapshot()
                    val json = events.toJsonString()
                    context.startActivity(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, json as CharSequence)
                        }
                    )
                }
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        SettingsActionRow(
            label = stringResource(R.string.export_events_file),
            onClick = {
                scope.launch {
                    pendingExportText = repository.getAllEventsSnapshot().toJsonString()
                    saveJsonLauncher.launch("timeapk-events.json")
                }
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        SettingsActionRow(
            label = stringResource(R.string.export_csv),
            onClick = {
                scope.launch {
                    val events = repository.getAllEventsSnapshot()
                    val csv = events.toCsvString()
                    context.startActivity(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, csv as CharSequence)
                        }
                    )
                }
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        SettingsActionRow(
            label = stringResource(R.string.export_csv_file),
            onClick = {
                scope.launch {
                    pendingExportText = repository.getAllEventsSnapshot().toCsvString()
                    saveCsvLauncher.launch("timeapk-events.csv")
                }
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        SettingsActionRow(
            label = stringResource(R.string.export_plain_text),
            onClick = {
                scope.launch {
                    val events = repository.getAllEventsSnapshot()
                    val text = events.toPlainTextListString(
                        context.getString(R.string.days_left_label),
                        context.getString(R.string.days_past_label),
                        context.getString(R.string.days_left)
                    )
                    context.startActivity(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text as CharSequence)
                        }
                    )
                }
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        SettingsActionRow(
            label = stringResource(R.string.import_events),
            onClick = {
                showImportDialog = true
                importJsonText = ""
                importResultMessage = null
                pendingImportPreview = null
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))
    }
}

@Composable
fun AboutSettingsContent(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimeApplication
    if (app == null) return
    val scope = rememberCoroutineScope()
    val directApkUpdatesEnabled = BuildConfig.DIRECT_APK_UPDATES_ENABLED
    
    var updateResult by remember { mutableStateOf<CheckUpdateResult?>(null) }
    var updateCheckInProgress by remember { mutableStateOf(false) }
    var updateDownloading by remember { mutableStateOf(false) }

    fun startUpdateCheck() {
        if (updateCheckInProgress) return
        updateResult = null
        updateCheckInProgress = true
        scope.launch {
            val result = try {
                app.updateChecker.checkUpdate()
            } catch (cancellation: CancellationException) {
                updateCheckInProgress = false
                throw cancellation
            } catch (_: Exception) {
                updateCheckInProgress = false
                snackbarHostState.showSnackbar(
                    context.getString(R.string.update_error),
                    withDismissAction = true
                )
                return@launch
            }
            updateCheckInProgress = false
            updateResult = result
            when {
                result.checkFailed -> snackbarHostState.showSnackbar(
                    context.getString(R.string.update_error),
                    withDismissAction = true
                )
                !result.hasUpdate -> snackbarHostState.showSnackbar(
                    context.getString(R.string.update_latest),
                    withDismissAction = true
                )
            }
        }
    }

    if (directApkUpdatesEnabled) updateResult?.takeIf { it.hasUpdate }?.let { result ->
        SongFormDialog(
            title = context.getString(R.string.update_new_title, result.versionName ?: ""),
            onDismissRequest = { updateResult = null },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    result.releaseNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                        Text(
                            context.getString(R.string.update_release_notes),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            buttons = {
                result.downloadUrl?.takeIf { directApkUpdatesEnabled }?.let { url ->
                    SongDialogButton(
                        text = context.getString(R.string.update_open_browser),
                        onClick = {
                            UpdateInstaller.openDownloadPageInBrowser(context, url)
                            updateResult = null
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                SongDialogButton(
                    text = context.getString(R.string.update_later),
                    onClick = { updateResult = null }
                )
                if (directApkUpdatesEnabled) {
                    Spacer(modifier = Modifier.width(4.dp))
                    SongDialogButton(
                        text = if (updateDownloading) context.getString(R.string.update_downloading)
                        else context.getString(R.string.update_download_install),
                        onClick = {
                            val url = result.downloadUrl
                            if (url != null) {
                                updateDownloading = true
                                scope.launch {
                                    val ok = UpdateInstaller.downloadAndInstall(context, url)
                                    updateDownloading = false
                                    updateResult = null
                                    if (!ok) {
                                        snackbarHostState.showSnackbar(context.getString(R.string.update_download_failed))
                                    }
                                }
                            }
                        }
                    )
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SettingsGroupHeader(title = stringResource(R.string.settings_about_entry_title))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME ?: ""),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))
        
        if (directApkUpdatesEnabled) {
            SettingsActionRow(
                label = stringResource(R.string.settings_check_update),
                supportingText = if (updateCheckInProgress) {
                    stringResource(R.string.settings_check_update_loading)
                } else {
                    null
                },
                onClick = { startUpdateCheck() }
            )
        } else {
            Text(
                text = stringResource(R.string.settings_updates_managed_by_store),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))
    }
}
















private fun formatMinutesOfDay(minutesOfDay: Int): String {
    val safe = sanitizeReminderTimeMinutesOfDay(minutesOfDay)
    val hour = safe / 60
    val minute = safe % 60
    return String.format(Locale.US, "%02d:%02d", hour, minute)
}

private fun formatScheduleSyncTime(millis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

private fun settingsReminderStatusTitle(
    context: Context,
    status: ReminderStatusSummary
): String {
    val resId = when (status.messageKey) {
        "reminder_status_off" -> R.string.reminder_status_off
        "reminder_status_notification_permission_needed" -> R.string.reminder_status_notification_permission_needed
        "reminder_status_calendar_permission_needed" -> R.string.reminder_status_calendar_permission_needed
        "reminder_status_no_writable_calendar" -> R.string.reminder_status_no_writable_calendar
        "reminder_status_schedule_sync_failed" -> R.string.reminder_status_schedule_sync_failed
        "reminder_status_app_and_schedule_ready" -> R.string.reminder_status_app_and_schedule_ready
        "reminder_status_app_ready" -> R.string.reminder_status_app_ready
        else -> R.string.reminder_status_schedule_pending
    }
    return context.getString(resId)
}

private fun formatImportPreviewDate(event: Event): String {
    return eventDateToLocalDate(event.date).format(DateTimeFormatter.ISO_LOCAL_DATE)
}

private fun parseReminderTimeInput(input: String): Int? {
    val raw = input.trim()
    if (raw.isBlank()) return null

    val parsed = if (raw.contains(":")) {
        val parts = raw.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        hour to minute
    } else {
        val digits = raw.filter { it.isDigit() }
        when (digits.length) {
            1, 2 -> (digits.toIntOrNull() ?: return null) to 0
            3 -> {
                val hour = digits.substring(0, 1).toIntOrNull() ?: return null
                val minute = digits.substring(1, 3).toIntOrNull() ?: return null
                hour to minute
            }
            4 -> {
                val hour = digits.substring(0, 2).toIntOrNull() ?: return null
                val minute = digits.substring(2, 4).toIntOrNull() ?: return null
                hour to minute
            }
            else -> return null
        }
    }

    val (hour, minute) = parsed
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun parseHexColor(hex: String): Color? {
    return try {
        Color(hex.toColorInt())
    } catch (_: Exception) {
        null
    }
}

private fun evaluateContrastAuditForKey(
    colorScheme: ColorScheme,
    key: String,
    candidate: Color
): com.example.timeapk.ui.theme.ContrastAudit {
    val background = if (key == "background") candidate else colorScheme.background
    val surface = if (key == "surface") candidate else colorScheme.surface
    val primary = if (key == "primary") candidate else colorScheme.primary
    val onBackground = if (key == "on_background") candidate else colorScheme.onBackground
    val onSurface = if (key == "on_background") candidate else colorScheme.onSurface
    val onPrimary = colorScheme.onPrimary
    return ColorContrastGuardrail.audit(
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        primary = primary,
        onPrimary = onPrimary
    )
}
