package com.example.timeapk.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.notifications.ExactReminderAlarms
import com.example.timeapk.notifications.REASON_EXACT_REMINDER_PREFERENCE_CHANGED
import com.example.timeapk.notifications.RescheduleAllWorker
import com.example.timeapk.permissions.openExactAlarmSettings
import kotlinx.coroutines.launch

internal enum class ExactReminderStatus {
    ACTIVE,
    OFF,
    PERMISSION_NEEDED
}

internal fun exactReminderStatus(userEnabled: Boolean, canScheduleExact: Boolean): ExactReminderStatus = when {
    !userEnabled -> ExactReminderStatus.OFF
    canScheduleExact -> ExactReminderStatus.ACTIVE
    else -> ExactReminderStatus.PERMISSION_NEEDED
}

/**
 * "精确提醒 / Exact reminders" toggle with a status line and a shortcut to the system
 * "Alarms & reminders" page. Not shown on channels that do not ship SCHEDULE_EXACT_ALARM (Play).
 */
@Composable
fun ExactReminderSettingRow(modifier: Modifier = Modifier) {
    if (!ExactReminderAlarms.channelSupportsExact()) return
    val context = LocalContext.current
    val app = context.applicationContext as? TimeApplication ?: return
    val scope = rememberCoroutineScope()
    val enabled by app.userPrefs.exactRemindersEnabledFlow.collectAsState(initial = false)
    var canScheduleExact by remember { mutableStateOf(ExactReminderAlarms.canScheduleExactAlarms(context)) }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canScheduleExact = ExactReminderAlarms.canScheduleExactAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val status = exactReminderStatus(enabled, canScheduleExact)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_exact_reminder_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_exact_reminder_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = stringResource(
                        when (status) {
                            ExactReminderStatus.ACTIVE -> R.string.settings_exact_reminder_status_active
                            ExactReminderStatus.OFF -> R.string.settings_exact_reminder_status_off
                            ExactReminderStatus.PERMISSION_NEEDED ->
                                R.string.settings_exact_reminder_status_permission_needed
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status == ExactReminderStatus.PERMISSION_NEEDED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            ClassicalToggle(
                checked = enabled,
                onCheckedChange = { checked ->
                    scope.launch {
                        app.userPrefs.setExactRemindersEnabled(checked)
                        RescheduleAllWorker.enqueue(context, REASON_EXACT_REMINDER_PREFERENCE_CHANGED)
                    }
                }
            )
        }
        if (status == ExactReminderStatus.PERMISSION_NEEDED) {
            SettingsActionRow(
                label = stringResource(R.string.settings_exact_reminder_open_settings),
                onClick = { context.openExactAlarmSettings() }
            )
        }
    }
}
