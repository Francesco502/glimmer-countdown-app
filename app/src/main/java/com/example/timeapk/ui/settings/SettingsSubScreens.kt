package com.example.timeapk.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.example.timeapk.BuildConfig
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
import com.example.timeapk.ui.components.PermissionActionDialog
import com.example.timeapk.ui.components.PermissionDialogSpec
import com.example.timeapk.ui.components.SnapWheelPicker
import com.example.timeapk.ui.theme.ColorContrastGuardrail
import com.example.timeapk.widget.WidgetUpdater
import com.example.timeapk.update.CheckUpdateResult
import com.example.timeapk.update.UpdateInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.timeapk.ui.theme.SongDesignTokens
import androidx.compose.ui.window.Dialog
import com.example.timeapk.ui.utils.findActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ClassicalToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Text(
        text = if (checked) stringResource(R.string.toggle_on) else stringResource(R.string.toggle_off),
        style = MaterialTheme.typography.bodyLarge,
        color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        modifier = Modifier
            .clickable { onCheckedChange(!checked) }
            .border(
                width = 0.5.dp,
                color = if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaSoft),
                shape = RoundedCornerShape(2.dp)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

private val PRESET_COLOR_HEX = listOf(
    "#AF4E31", "#AC8F62", "#457080", "#5B8E79", "#86351C",
    "#4A4933", "#785B64", "#3A4550", "#F5F3ED", "#EDE8DD",
    "#1F1F1F", "#FFFFFF"
)

@Composable
fun AppearanceSettingsContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimeApplication
    if (app == null) return
    val prefs = app.userPrefs
    val scope = rememberCoroutineScope()
    fun launchWidgetSettingsUpdate(update: suspend () -> Unit) {
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
    val widgetFontScale by prefs.widgetFontScaleFlow.collectAsState(initial = 1f)
    var appBaseFontScaleDraft by remember(appBaseFontScale) { mutableStateOf(appBaseFontScale) }
    var widgetFontScaleDraft by remember(widgetFontScale) { mutableStateOf(widgetFontScale) }
    var colorPickerKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(appBaseFontScale) {
        appBaseFontScaleDraft = appBaseFontScale
    }
    LaunchedEffect(widgetFontScale) {
        widgetFontScaleDraft = widgetFontScale
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SettingsGroupHeader(title = stringResource(R.string.theme_title))

        listOf(
            THEME_FOLLOW_SYSTEM to stringResource(R.string.theme_follow_system),
            THEME_LIGHT to stringResource(R.string.theme_light),
            THEME_DARK to stringResource(R.string.theme_dark)
        ).forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        launchWidgetSettingsUpdate {
                            prefs.setThemeMode(value)
                        }
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = themeMode == value,
                    onClick = {
                        launchWidgetSettingsUpdate {
                            prefs.setThemeMode(value)
                        }
                    }
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))
        }

        SettingsGroupHeader(
            title = stringResource(R.string.custom_colors_title),
            modifier = Modifier.padding(top = 20.dp)
        )

        CustomColorRow(
            label = stringResource(R.string.custom_color_background),
            currentHex = customBackgroundHex,
            defaultColor = MaterialTheme.colorScheme.background,
            onPick = { colorPickerKey = "background" },
            onReset = { scope.launch { prefs.setCustomBackgroundHex(null) } }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        CustomColorRow(
            label = stringResource(R.string.custom_color_surface),
            currentHex = customSurfaceHex,
            defaultColor = MaterialTheme.colorScheme.surfaceVariant,
            onPick = { colorPickerKey = "surface" },
            onReset = { scope.launch { prefs.setCustomSurfaceHex(null) } }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        CustomColorRow(
            label = stringResource(R.string.custom_color_primary),
            currentHex = customPrimaryHex,
            defaultColor = MaterialTheme.colorScheme.primary,
            onPick = { colorPickerKey = "primary" },
            onReset = { scope.launch { prefs.setCustomPrimaryHex(null) } }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        CustomColorRow(
            label = stringResource(R.string.custom_color_on_background),
            currentHex = customOnBackgroundHex,
            defaultColor = MaterialTheme.colorScheme.onBackground,
            showBorder = true,
            onPick = { colorPickerKey = "on_background" },
            onReset = { scope.launch { prefs.setCustomOnBackgroundHex(null) } }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        Text(
            text = stringResource(R.string.custom_color_accessibility_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

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
            val isValidHex = remember(customHexInput) {
                try {
                    if (customHexInput.startsWith("#") && (customHexInput.length == 7 || customHexInput.length == 9)) {
                        customHexInput.toColorInt()
                        true
                    } else {
                        false
                    }
                } catch (_: Exception) {
                    false
                }
            }
            val candidateAudit = remember(key, customHexInput, currentColorScheme) {
                parseHexColor(customHexInput)?.let {
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

            AlertDialog(
                onDismissRequest = { colorPickerKey = null },
                shape = RoundedCornerShape(SongDesignTokens.StandardRadius.dp),
                title = { Text(label) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PRESET_COLOR_HEX.chunked(6).forEach { rowHexes ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowHexes.forEach { hex ->
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(hex.toColorInt()), RoundedCornerShape(SongDesignTokens.StandardRadius.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaSoft), RoundedCornerShape(SongDesignTokens.StandardRadius.dp))
                                                .clickable {
                                                    tryApplyColor(hex)
                                                }
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))
                        OutlinedTextField(
                            value = customHexInput,
                            onValueChange = {
                                customHexInput = it
                                contrastErrorRatio = null
                            },
                            label = { Text(stringResource(R.string.custom_color_hex_hint)) },
                            placeholder = { Text("#RRGGBB") },
                            isError = customHexInput.isNotEmpty() && (!isValidHex || candidateAudit?.isPass == false),
                            singleLine = true,
                            trailingIcon = {
                                if (isValidHex) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(customHexInput.toColorInt()), RoundedCornerShape(4.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                                    )
                                }
                            },
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
                confirmButton = {
                    TextButton(
                        onClick = {
                            tryApplyColor(customHexInput)
                        },
                        enabled = isValidHex && candidateAudit?.isPass == true
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { colorPickerKey = null }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }

        SettingsGroupHeader(
            title = stringResource(R.string.font_title),
            modifier = Modifier.padding(top = 20.dp)
        )

        listOf(
            4 to stringResource(R.string.font_slender_gold),
            1 to stringResource(R.string.font_serif),
            0 to stringResource(R.string.font_default)
        ).forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { scope.launch { prefs.setFontPreset(value) } }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = fontPreset == value,
                    onClick = { scope.launch { prefs.setFontPreset(value) } }
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))
        }

        Text(
            text = stringResource(R.string.settings_app_font_scale_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 12.dp)
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

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        Text(
            text = stringResource(R.string.settings_widget_font_scale_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = stringResource(R.string.settings_font_scale_summary, (widgetFontScaleDraft * 100).roundToInt()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        Slider(
            value = widgetFontScaleDraft,
            onValueChange = {
                widgetFontScaleDraft = it.coerceIn(
                    SongDesignTokens.WidgetFontScaleMin,
                    SongDesignTokens.WidgetFontScaleMax
                )
            },
            valueRange = SongDesignTokens.WidgetFontScaleMin..SongDesignTokens.WidgetFontScaleMax,
            onValueChangeFinished = {
                launchWidgetSettingsUpdate {
                    prefs.setWidgetFontScale(widgetFontScaleDraft)
                }
            },
            modifier = Modifier.padding(top = 4.dp)
        )
        TextButton(
            onClick = {
                widgetFontScaleDraft = 1f
                launchWidgetSettingsUpdate {
                    prefs.setWidgetFontScale(1f)
                }
            }
        ) {
            Text(stringResource(R.string.settings_font_scale_reset))
        }
    }
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
        
        listOf(
            LANG_ZH to stringResource(R.string.language_zh),
            LANG_EN to stringResource(R.string.language_en)
        ).forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            prefs.setLanguageMode(value)
                            withContext(Dispatchers.Main) { activity?.recreate() }
                        }
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = languageMode == value,
                    onClick = {
                        scope.launch {
                            prefs.setLanguageMode(value)
                            withContext(Dispatchers.Main) { activity?.recreate() }
                        }
                    }
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))
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
                text = stringResource(R.string.settings_show_hours_summary),
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
                Text(
                    text = stringResource(R.string.settings_reduce_motion_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ClassicalToggle(
                checked = reduceMotionEnabled,
                onCheckedChange = { scope.launch { prefs.setReduceMotionEnabled(it) } }
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        Text(
            text = stringResource(R.string.home_density_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { scope.launch { prefs.setHomeDensityMode(0) } }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = homeDensityMode == 0,
                onClick = { scope.launch { prefs.setHomeDensityMode(0) } }
            )
            Text(
                text = stringResource(R.string.home_density_compact),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.home_density_compact_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { scope.launch { prefs.setHomeDensityMode(1) } }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = homeDensityMode == 1,
                onClick = { scope.launch { prefs.setHomeDensityMode(1) } }
            )
            Text(
                text = stringResource(R.string.home_density_detailed),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.home_density_detailed_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Text(
                    text = stringResource(R.string.settings_smart_milestones_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                text = stringResource(R.string.settings_show_milestone_summary),
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
                Text(
                    text = stringResource(R.string.settings_milestone_remind_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1 to R.string.settings_milestone_remind_days_1,
                    3 to R.string.settings_milestone_remind_days_3,
                    7 to R.string.settings_milestone_remind_days_7,
                    14 to R.string.settings_milestone_remind_days_14).forEach { (days, resId) ->
                    FilterChip(
                        selected = milestoneRemindDaysAhead == days,
                        onClick = {
                            scope.launch {
                                prefs.setMilestoneRemindDaysAhead(days)
                                rescheduleMilestoneReminders(app)
                            }
                        },
                        label = { Text(stringResource(resId)) },
                        shape = RoundedCornerShape(4.dp)
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
                    label = { Text(stringResource(R.string.settings_milestone_remind_days_custom_hint)) },
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
                    FilterChip(
                        selected = milestoneRemindTimeMinutesOfDay == minutes,
                        onClick = {
                            scope.launch {
                                prefs.setMilestoneRemindTimeMinutesOfDay(minutes)
                                rescheduleMilestoneReminders(app)
                            }
                        },
                        label = { Text(label) },
                        shape = RoundedCornerShape(4.dp)
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
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { scope.launch { prefs.setDateFormatMode(0) } }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = dateFormatMode == 0,
                onClick = { scope.launch { prefs.setDateFormatMode(0) } }
            )
            Text(
                text = stringResource(R.string.date_format_dot),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { scope.launch { prefs.setDateFormatMode(1) } }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = dateFormatMode == 1,
                onClick = { scope.launch { prefs.setDateFormatMode(1) } }
            )
            Text(
                text = stringResource(R.string.date_format_dash),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
                color = MaterialTheme.colorScheme.onSurface
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
    val dateFormatMode by prefs.dateFormatModeFlow.collectAsState(initial = 0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        SettingsGroupHeader(title = stringResource(R.string.settings_category_display_title))

        Text(
            text = stringResource(R.string.language_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )

        listOf(
            LANG_ZH to stringResource(R.string.language_zh),
            LANG_EN to stringResource(R.string.language_en)
        ).forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            prefs.setLanguageMode(value)
                            withContext(Dispatchers.Main) { activity?.recreate() }
                        }
                    }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = languageMode == value,
                    onClick = {
                        scope.launch {
                            prefs.setLanguageMode(value)
                            withContext(Dispatchers.Main) { activity?.recreate() }
                        }
                    }
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))
        }

        SettingsGroupHeader(
            title = stringResource(R.string.home_density_title),
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
                text = stringResource(R.string.settings_show_hours_summary),
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
                Text(
                    text = stringResource(R.string.settings_reduce_motion_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ClassicalToggle(
                checked = reduceMotionEnabled,
                onCheckedChange = { scope.launch { prefs.setReduceMotionEnabled(it) } }
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        listOf(
            Triple(0, R.string.home_density_compact, R.string.home_density_compact_summary),
            Triple(1, R.string.home_density_detailed, R.string.home_density_detailed_summary)
        ).forEach { (value, titleRes, summaryRes) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { scope.launch { prefs.setHomeDensityMode(value) } }
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = homeDensityMode == value,
                    onClick = { scope.launch { prefs.setHomeDensityMode(value) } }
                )
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(summaryRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        SettingsGroupHeader(
            title = stringResource(R.string.date_format_title),
            modifier = Modifier.padding(top = 20.dp)
        )

        listOf(
            0 to R.string.date_format_dot,
            1 to R.string.date_format_dash
        ).forEach { (value, labelRes) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { scope.launch { prefs.setDateFormatMode(value) } }
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = dateFormatMode == value,
                    onClick = { scope.launch { prefs.setDateFormatMode(value) } }
                )
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
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
    val remindDayOptions = remember { (0..3650).toList() }
    val remindHourOptions = remember { (0..23).toList() }
    val remindMinuteOptions = remember { (0..59).toList() }

    var writableCalendars by remember { mutableStateOf<List<ScheduleSyncManager.CalendarOption>>(emptyList()) }
    var latestScheduleSyncEvent by remember { mutableStateOf<Event?>(null) }
    var syncStatusLoading by remember { mutableStateOf(false) }
    var permissionDialog by remember { mutableStateOf<PermissionDialogSpec?>(null) }
    val calendarPermissions = remember {
        arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    }

    fun hasCalendarPermission(): Boolean {
        return context.hasCalendarReadWritePermission()
    }

    val refreshScheduleSyncStatus: suspend () -> Unit = {
        syncStatusLoading = true
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
        syncStatusLoading = false
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

    LaunchedEffect(Unit) {
        refreshScheduleSyncStatus()
    }

    LaunchedEffect(writableCalendars, scheduleTargetCalendarId) {
        if (!hasCalendarPermission()) return@LaunchedEffect
        val selected = scheduleTargetCalendarId
        if (selected != null && writableCalendars.none { it.id == selected }) {
            prefs.setScheduleTargetCalendarId(null)
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
        SettingsGroupHeader(title = stringResource(R.string.settings_milestone_entry_title))

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
                Text(
                    text = stringResource(R.string.settings_default_event_remind_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            val selectedDays = defaultEventRemindDaysBefore.coerceIn(
                remindDayOptions.first(),
                remindDayOptions.last()
            )
            val selectedHour = (defaultEventRemindTimeMinutesOfDay / 60).coerceIn(0, 23)
            val selectedMinute = (defaultEventRemindTimeMinutesOfDay % 60).coerceIn(0, 59)

            Text(
                text = stringResource(R.string.settings_default_event_remind_days_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
            Text(
                text = if (selectedDays == 0) {
                    stringResource(R.string.remind_same_day)
                } else {
                    context.resources.getQuantityString(
                        R.plurals.remind_days_before_format,
                        selectedDays,
                        selectedDays
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SnapWheelPicker(
                items = remindDayOptions,
                selectedItem = selectedDays,
                onItemSelected = { days ->
                    if (days != defaultEventRemindDaysBefore) {
                        scope.launch {
                            prefs.setDefaultEventRemindDaysBefore(days)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                itemLabel = { days ->
                    if (days == 0) {
                        context.getString(R.string.remind_same_day)
                    } else {
                        context.resources.getQuantityString(
                            R.plurals.remind_days_before_format,
                            days,
                            days
                        )
                    }
                }
            )

            Text(
                text = stringResource(R.string.settings_default_event_remind_time_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            Text(
                text = formatMinutesOfDay(defaultEventRemindTimeMinutesOfDay),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                        items = remindHourOptions,
                        selectedItem = selectedHour,
                        onItemSelected = { hour ->
                            val updatedMinutes = hour * 60 + selectedMinute
                            if (updatedMinutes != defaultEventRemindTimeMinutesOfDay) {
                                scope.launch {
                                    prefs.setDefaultEventRemindTimeMinutesOfDay(updatedMinutes)
                                }
                            }
                        },
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
                        items = remindMinuteOptions,
                        selectedItem = selectedMinute,
                        onItemSelected = { minute ->
                            val updatedMinutes = selectedHour * 60 + minute
                            if (updatedMinutes != defaultEventRemindTimeMinutesOfDay) {
                                scope.launch {
                                    prefs.setDefaultEventRemindTimeMinutesOfDay(updatedMinutes)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        itemLabel = { value -> String.format(Locale.US, "%02d", value) }
                    )
                }
            }
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
                    text = stringResource(R.string.settings_show_milestone),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_show_milestone_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                Text(
                    text = stringResource(R.string.settings_smart_milestones_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_milestone_remind_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_milestone_remind_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            val selectedDays = milestoneRemindDaysAhead.coerceIn(
                remindDayOptions.first(),
                remindDayOptions.last()
            )
            val selectedHour = (milestoneRemindTimeMinutesOfDay / 60).coerceIn(0, 23)
            val selectedMinute = (milestoneRemindTimeMinutesOfDay % 60).coerceIn(0, 59)

            Text(
                text = stringResource(R.string.settings_milestone_remind_days_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
            Text(
                text = if (selectedDays == 0) {
                    stringResource(R.string.remind_same_day)
                } else {
                    context.resources.getQuantityString(
                        R.plurals.remind_days_before_format,
                        selectedDays,
                        selectedDays
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SnapWheelPicker(
                items = remindDayOptions,
                selectedItem = selectedDays,
                onItemSelected = { days ->
                    if (days != milestoneRemindDaysAhead) {
                        scope.launch {
                            prefs.setMilestoneRemindDaysAhead(days)
                            RescheduleAllWorker.enqueue(context, "milestone_remind_days_changed")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                itemLabel = { days ->
                    if (days == 0) {
                        context.getString(R.string.remind_same_day)
                    } else {
                        context.resources.getQuantityString(
                            R.plurals.remind_days_before_format,
                            days,
                            days
                        )
                    }
                }
            )

            Text(
                text = stringResource(R.string.settings_milestone_remind_time_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            Text(
                text = formatMinutesOfDay(milestoneRemindTimeMinutesOfDay),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                        items = remindHourOptions,
                        selectedItem = selectedHour,
                        onItemSelected = { hour ->
                            val updatedMinutes = hour * 60 + selectedMinute
                            if (updatedMinutes != milestoneRemindTimeMinutesOfDay) {
                                scope.launch {
                                    prefs.setMilestoneRemindTimeMinutesOfDay(updatedMinutes)
                                    RescheduleAllWorker.enqueue(context, "milestone_remind_time_changed")
                                }
                            }
                        },
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
                        items = remindMinuteOptions,
                        selectedItem = selectedMinute,
                        onItemSelected = { minute ->
                            val updatedMinutes = selectedHour * 60 + minute
                            if (updatedMinutes != milestoneRemindTimeMinutesOfDay) {
                                scope.launch {
                                    prefs.setMilestoneRemindTimeMinutesOfDay(updatedMinutes)
                                    RescheduleAllWorker.enqueue(context, "milestone_remind_time_changed")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        itemLabel = { value -> String.format(Locale.US, "%02d", value) }
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        SettingsGroupHeader(
            title = stringResource(R.string.settings_schedule_sync_title),
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = stringResource(R.string.settings_schedule_sync_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = stringResource(R.string.settings_schedule_target_calendar_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
        val calendarPermissionGranted = hasCalendarPermission()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (!calendarPermissionGranted) {
                        requestCalendarPermissionAccess()
                    } else {
                        scope.launch {
                            prefs.setScheduleTargetCalendarId(null)
                            RescheduleAllWorker.enqueue(context, "schedule_target_calendar_changed")
                        }
                    }
                }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
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
            Text(
                text = stringResource(R.string.settings_schedule_calendar_auto),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!calendarPermissionGranted) {
                                requestCalendarPermissionAccess()
                            } else {
                                scope.launch {
                                    prefs.setScheduleTargetCalendarId(calendar.id)
                                    RescheduleAllWorker.enqueue(context, "schedule_target_calendar_changed")
                                }
                            }
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
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
                    Text(
                        text = calendar.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp)
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
                Text(
                    text = stringResource(R.string.settings_schedule_rrule_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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

    suspend fun importEvents(events: List<Event>): Triple<Int, Int, Int> {
        if (events.isEmpty()) return Triple(0, 0, 0)
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
                    ScheduleSyncManager.removeScheduleReminderByEventId(context, savedEvent.id)
                    ScheduleSyncManager.ScheduleSyncResult(
                        primaryScheduleEventId = null,
                        targetCalendarId = null,
                        lastSyncAt = System.currentTimeMillis(),
                        error = null
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
                        savedEvent.copy(
                            scheduleEventId = syncResult.primaryScheduleEventId,
                            targetCalendarId = syncResult.targetCalendarId,
                            lastScheduleSyncAt = syncResult.lastSyncAt,
                            lastScheduleSyncError = syncResult.error
                        )
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

        return Triple(successCount, failedCount, warningCount)
    }

    fun importResultText(successCount: Int, failedCount: Int, warningCount: Int): String {
        return when {
            successCount <= 0 -> context.getString(R.string.import_error)
            failedCount > 0 || warningCount > 0 -> context.getString(
                R.string.import_partial_summary,
                successCount,
                failedCount,
                warningCount
            )
            else -> context.resources.getQuantityString(
                R.plurals.import_success,
                successCount,
                successCount
            )
        }
    }

    val importFromFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val text = try {
            context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: ""
        } catch (_: Exception) {
            ""
        }
        if (text.isBlank()) {
            importResultMessage = context.getString(R.string.import_error)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val list = parseEventsFromJson(text)
            val (successCount, failedCount, warningCount) = importEvents(list)
            importResultMessage = importResultText(successCount, failedCount, warningCount)
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                importResultMessage = null
            },
            shape = RoundedCornerShape(4.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(stringResource(R.string.import_events)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = { Text(stringResource(R.string.import_hint)) },
                        maxLines = 8,
                        shape = RoundedCornerShape(4.dp)
                    )
                    OutlinedButton(
                        onClick = { importFromFileLauncher.launch("text/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(stringResource(R.string.import_from_file))
                    }
                    importResultMessage?.let { msg ->
                        Text(msg, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val list = parseEventsFromJson(importJsonText)
                        val (successCount, failedCount, warningCount) = importEvents(list)
            importResultMessage = importResultText(successCount, failedCount, warningCount)
                    }
                }) {
                    Text(stringResource(R.string.import_events), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    importResultMessage = null
                }) {
                    Text(
                        stringResource(R.string.delete_confirm_cancel),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
        SettingsGroupHeader(title = stringResource(R.string.export_import))

        SettingsPressableRow(
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
        ) {
            Text(
                text = stringResource(R.string.export_events),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        SettingsPressableRow(
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
        ) {
            Text(
                text = stringResource(R.string.export_csv),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        SettingsPressableRow(
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
        ) {
            Text(
                text = stringResource(R.string.export_plain_text),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = com.example.timeapk.ui.theme.SongDesignTokens.BorderAlphaStrong))

        SettingsPressableRow(
            onClick = {
                showImportDialog = true
                importJsonText = ""
                importResultMessage = null
            }
        ) {
            Text(
                text = stringResource(R.string.import_events),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
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
    
    var updateResult by remember { mutableStateOf<CheckUpdateResult?>(null) }
    var updateCheckInProgress by remember { mutableStateOf(false) }
    var updateDownloading by remember { mutableStateOf(false) }

    // Update Dialog
    updateResult?.takeIf { it.hasUpdate }?.let { result ->
        AlertDialog(
            onDismissRequest = { updateResult = null },
            shape = RoundedCornerShape(4.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(context.getString(R.string.update_new_title, result.versionName ?: "")) },
            text = {
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
            confirmButton = {
                TextButton(
                    onClick = {
                        val url = result.downloadUrl ?: return@TextButton
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
                ) {
                    Text(
                        if (updateDownloading) context.getString(R.string.update_downloading)
                        else context.getString(R.string.update_download_install),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    result.downloadUrl?.let { url ->
                        TextButton(onClick = {
                            UpdateInstaller.openDownloadPageInBrowser(context, url)
                            updateResult = null
                        }) {
                            Text(context.getString(R.string.update_open_browser), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    TextButton(onClick = { updateResult = null }) {
                        Text(context.getString(R.string.update_later), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
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
        
        SettingsPressableRow(
            onClick = {
                if (updateCheckInProgress) return@SettingsPressableRow
                updateResult = null
                updateCheckInProgress = true
                scope.launch {
                    val result = try {
                        app.updateChecker.checkUpdate()
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) {
                            updateCheckInProgress = false
                            snackbarHostState.showSnackbar(context.getString(R.string.update_error), withDismissAction = true)
                        }
                        return@launch
                    }
                    withContext(Dispatchers.Main) {
                        updateCheckInProgress = false
                        updateResult = result
                        if (result.checkFailed) {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.update_error),
                                withDismissAction = true
                            )
                        } else if (!result.hasUpdate) {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.update_latest),
                                withDismissAction = true
                            )
                        }
                    }
                }
            }
        ) {
            Text(
                text = stringResource(R.string.settings_check_update),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (updateCheckInProgress) {
                Text(
                    text = stringResource(R.string.settings_check_update_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
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

