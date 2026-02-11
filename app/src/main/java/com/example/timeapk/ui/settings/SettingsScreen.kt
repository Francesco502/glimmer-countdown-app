package com.example.timeapk.ui.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.THEME_DARK
import com.example.timeapk.data.THEME_FOLLOW_SYSTEM
import com.example.timeapk.data.THEME_LIGHT
import com.example.timeapk.data.LANG_ZH
import com.example.timeapk.data.LANG_EN
import com.example.timeapk.data.Event
import com.example.timeapk.data.parseEventsFromJson
import com.example.timeapk.data.toCsvString
import com.example.timeapk.data.toJsonString
import com.example.timeapk.data.toPlainTextListString
import kotlinx.coroutines.launch

import com.example.timeapk.notifications.scheduleReminder
import com.example.timeapk.ui.theme.AnimationSpecs
import com.example.timeapk.ui.components.rememberPressScale
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as TimeApplication
    val prefs = app.userPrefs
    val repository = app.repository
    val themeMode by prefs.themeModeFlow.collectAsState(initial = THEME_FOLLOW_SYSTEM)
    val showHours by prefs.showHoursFlow.collectAsState(initial = true)
    val languageMode by prefs.languageModeFlow.collectAsState(initial = LANG_ZH)
    val scope = rememberCoroutineScope()
    val activity = context as? Activity
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var importResultMessage by remember { mutableStateOf<String?>(null) }
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
            if (list.isEmpty()) {
                importResultMessage = context.getString(R.string.import_error)
            } else {
                list.forEach { event ->
                    val newId = repository.insertEvent(event)
                    // 用真实 ID 调度提醒，确保导入的 remindEnabled 事件能正常通知
                    if (event.remindEnabled) {
                        scheduleReminder(context, event.copy(id = newId.toInt()))
                    }
                }
                importResultMessage = context.getString(R.string.import_success, list.size)
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                importResultMessage = null
            },
            shape = RoundedCornerShape(4.dp), // 港式复古：直角弹窗
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
                        onClick = { importFromFileLauncher.launch("*/*") },
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
                        if (list.isEmpty()) {
                            importResultMessage = context.getString(R.string.import_error)
                        } else {
                            for (event in list) {
                                val newId = repository.insertEvent(event)
                                if (event.remindEnabled) {
                                    scheduleReminder(context, event.copy(id = newId.toInt()))
                                }
                            }
                            importResultMessage = context.getString(R.string.import_success, list.size)
                        }
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
                    Text(stringResource(R.string.delete_confirm_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // 与其他页面背景一致
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background) // 纯色复古背景
                .padding(24.dp)
        ) {
            // Section: Theme
            Text(
                text = stringResource(R.string.theme_title).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            
            listOf(
                THEME_FOLLOW_SYSTEM to stringResource(R.string.theme_follow_system),
                THEME_LIGHT to stringResource(R.string.theme_light),
                THEME_DARK to stringResource(R.string.theme_dark)
            ).forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { scope.launch { prefs.setThemeMode(value) } }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = themeMode == value,
                        onClick = { scope.launch { prefs.setThemeMode(value) } }
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }

            // Section: Language
            Text(
                text = stringResource(R.string.language_title).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 32.dp, bottom = 12.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            
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
                                activity?.recreate()
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
                                activity?.recreate()
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }

            // Section: Display
            Text(
                text = stringResource(R.string.settings_show_hours).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 32.dp, bottom = 12.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            
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
                Switch(
                    checked = showHours,
                    onCheckedChange = { scope.launch { prefs.setShowHours(it) } }
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Section: Data
            Text(
                text = stringResource(R.string.export_import).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 32.dp, bottom = 12.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            
            // 导出 JSON
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            
            // 导出 CSV
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            
            // 导出纯文本
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            
            // 导入
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun SettingsPressableRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 关键修复：将 rememberPressScale 返回的 interactionSource 传给 clickable，
    // 这样按压事件才能驱动缩放动画
    val (pressModifier, interactionSource) = rememberPressScale()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(pressModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // 缩放已提供反馈，不需要涟漪
                onClick = onClick
            )
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}
