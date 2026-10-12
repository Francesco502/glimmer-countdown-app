package com.example.timeapk.ui.settings

import android.appwidget.AppWidgetManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.timeapk.BuildConfig
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.CATEGORY_NAME_MAX_LENGTH
import com.example.timeapk.data.Category
import com.example.timeapk.data.LANG_EN
import com.example.timeapk.data.normalizeCategoryName
import com.example.timeapk.ui.common.CategoryColorChoices
import com.example.timeapk.ui.common.LocalCustomCategories
import com.example.timeapk.ui.common.categoryOptions
import com.example.timeapk.ui.components.SongConfirmDialog
import com.example.timeapk.ui.components.SongDialogButton
import com.example.timeapk.ui.components.SongFormDialog
import com.example.timeapk.ui.theme.SongColorSwatch
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind
import com.example.timeapk.ui.utils.parseEventColorOrFallback
import com.example.timeapk.widget.CountdownAppWidgetProvider
import com.example.timeapk.widget.SingleEventWidgetProvider
import com.example.timeapk.widget.WidgetUpdater
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable

/** 设置首页每一行下方显示的当前值。 */
@Composable
internal fun rememberSettingsCategorySummaries(): Map<SettingsCategory, String> {
    val context = LocalContext.current
    val app = context.applicationContext as? TimeApplication ?: return emptyMap()
    val prefs = app.userPrefs
    val themeMode by prefs.themeModeFlow.collectAsState(initial = 0)
    val fontPreset by prefs.fontPresetFlow.collectAsState(initial = 4)
    val dynamicColor by prefs.dynamicColorEnabledFlow.collectAsState(initial = false)
    val languageMode by prefs.languageModeFlow.collectAsState(initial = 0)
    val homeMode by prefs.homeDisplayModeFlow.collectAsState(initial = 0)
    val defaultRemind by prefs.defaultEventRemindEnabledFlow.collectAsState(initial = false)
    val customCategories = LocalCustomCategories.current
    val widgetCount by produceState(0) {
        val manager = AppWidgetManager.getInstance(context)
        value = CountdownAppWidgetProvider.getAppWidgetIds(context, manager).size +
            SingleEventWidgetProvider.getAppWidgetIds(context, manager).size
    }
    val appearance = listOfNotNull(
        stringResource(themeModeLabelRes(themeMode)),
        stringResource(R.string.settings_dynamic_color_short).takeIf { dynamicColor },
        fontPresetTitle(fontPreset)
    ).joinToString(" · ")
    val display = listOf(
        stringResource(if (languageMode == LANG_EN) R.string.language_en else R.string.language_zh),
        stringResource(
            when (homeMode) {
                1 -> R.string.display_mode_list
                2 -> R.string.display_mode_calendar
                else -> R.string.display_mode_card
            }
        )
    ).joinToString(" · ")
    return mapOf(
        SettingsCategory.APPEARANCE to appearance,
        SettingsCategory.WIDGET to if (widgetCount == 0) {
            stringResource(R.string.settings_widget_summary_none)
        } else {
            pluralStringResource(R.plurals.settings_widget_summary_count, widgetCount, widgetCount)
        },
        SettingsCategory.DISPLAY to display,
        SettingsCategory.CATEGORIES to if (customCategories.isEmpty()) {
            stringResource(R.string.settings_categories_summary_none)
        } else {
            pluralStringResource(R.plurals.settings_categories_summary_count, customCategories.size, customCategories.size)
        },
        SettingsCategory.MILESTONE to stringResource(
            if (defaultRemind) R.string.settings_reminder_summary_on else R.string.settings_reminder_summary_off
        ),
        SettingsCategory.DATA to stringResource(R.string.settings_data_summary),
        SettingsCategory.ABOUT to stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME ?: "")
    )
}

/** 分类管理：新增、改名、改色、调整顺序、删除自定义分类。 */
@Composable
fun CategoryManagementContent(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimeApplication ?: return
    val repository = app.categoryRepository
    val scope = rememberCoroutineScope()
    val customCategories = LocalCustomCategories.current
    val builtIns = categoryOptions().filter { it.builtIn }
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Pair<Category, Int>?>(null) }

    if (creating || editing != null) {
        CategoryEditorDialog(
            initial = editing,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { name, colorHex ->
                scope.launch {
                    val target = editing
                    if (target == null) {
                        repository.add(name, colorHex)
                    } else {
                        repository.update(target.copy(name = name, colorHex = colorHex))
                    }
                    WidgetUpdater.refreshCountdownWidgets(app)
                    creating = false
                    editing = null
                }
            }
        )
    }
    pendingDelete?.let { (category, count) ->
        SongConfirmDialog(
            title = stringResource(R.string.category_delete_title),
            message = if (count == 0) {
                stringResource(R.string.category_delete_message_empty, category.name)
            } else {
                pluralStringResource(R.plurals.category_delete_message, count, category.name, count)
            },
            confirmText = stringResource(R.string.delete_confirm_ok),
            dismissText = stringResource(R.string.delete_confirm_cancel),
            destructiveConfirm = true,
            onConfirm = {
                pendingDelete = null
                scope.launch {
                    repository.delete(category.id)
                    if (app.userPrefs.filterCategoryIdFlow.first() == category.id) {
                        app.userPrefs.setFilterCategoryId(null)
                    }
                    WidgetUpdater.refreshCountdownWidgets(app)
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.category_deleted, category.name),
                        duration = SnackbarDuration.Short
                    )
                }
            },
            onDismiss = { pendingDelete = null }
        )
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .widthIn(max = SongDesignTokens.ContentMaxWidth.dp)
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_categories_builtin),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        builtIns.forEach { option ->
            CategoryRow(name = option.label, color = option.color, trailing = {
                Text(
                    text = stringResource(R.string.settings_categories_builtin_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            })
        }
        Spacer(Modifier.padding(top = 16.dp))
        Text(
            text = stringResource(R.string.settings_categories_custom),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        if (customCategories.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_categories_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
        customCategories.forEachIndexed { index, category ->
            CategoryRow(
                name = category.name,
                color = parseEventColorOrFallback(category.colorHex, MaterialTheme.colorScheme.primary),
                onClick = { editing = category },
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { scope.launch { repository.reorder(customCategories.moveItem(index, index - 1).map { it.id }) } },
                            enabled = index > 0
                        ) {
                            SongLineIcon(
                                kind = SongLineIconKind.ChevronUp,
                                contentDescription = stringResource(R.string.home_move_up),
                                size = SongDesignTokens.IconSmall.dp,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (index > 0) 1f else 0.3f)
                            )
                        }
                        IconButton(
                            onClick = { scope.launch { repository.reorder(customCategories.moveItem(index, index + 1).map { it.id }) } },
                            enabled = index < customCategories.lastIndex
                        ) {
                            SongLineIcon(
                                kind = SongLineIconKind.ChevronDown,
                                contentDescription = stringResource(R.string.home_move_down),
                                size = SongDesignTokens.IconSmall.dp,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = if (index < customCategories.lastIndex) 1f else 0.3f
                                )
                            )
                        }
                        IconButton(onClick = {
                            scope.launch { pendingDelete = category to repository.countEvents(category.id) }
                        }) {
                            SongLineIcon(
                                kind = SongLineIconKind.Delete,
                                contentDescription = stringResource(R.string.button_delete),
                                size = SongDesignTokens.IconSmall.dp,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
        TextButton(onClick = { creating = true }, modifier = Modifier.padding(top = 8.dp)) {
            SongLineIcon(SongLineIconKind.Add, size = SongDesignTokens.IconSmall.dp, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.category_add_new))
        }
        Text(
            text = stringResource(R.string.settings_categories_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

private fun <T> List<T>.moveItem(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices) return this
    val result = toMutableList()
    val item = result.removeAt(from)
    result.add(to, item)
    return result
}

@Composable
private fun CategoryRow(
    name: String,
    color: Color,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(color, RoundedCornerShape(SongDesignTokens.RadiusXs.dp))
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            trailing()
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaSoft))
    }
}

@Composable
private fun CategoryEditorDialog(
    initial: Category?,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String?) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: CategoryColorChoices.first()) }
    val normalized = normalizeCategoryName(name)
    SongFormDialog(
        title = stringResource(if (initial == null) R.string.category_add_new else R.string.category_edit_title),
        onDismissRequest = onDismiss,
        content = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(CATEGORY_NAME_MAX_LENGTH) },
                label = { Text(stringResource(R.string.category_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryColorChoices.forEach { hex ->
                    SongColorSwatch(
                        color = Color(hex.toColorInt()),
                        selected = colorHex.equals(hex, ignoreCase = true),
                        size = 28.dp,
                        onClick = { colorHex = hex },
                        contentDescription = hex
                    )
                }
            }
        },
        buttons = {
            SongDialogButton(text = stringResource(R.string.date_picker_cancel), onClick = onDismiss)
            Spacer(modifier = Modifier.width(4.dp))
            SongDialogButton(
                text = stringResource(if (initial == null) R.string.category_create else R.string.category_save),
                enabled = normalized.isNotEmpty(),
                onClick = { onSave(normalized, colorHex) }
            )
        }
    )
}
