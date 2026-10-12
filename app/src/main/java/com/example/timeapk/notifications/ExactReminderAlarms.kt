package com.example.timeapk.notifications

import androidx.core.net.toUri
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import androidx.work.Data
import com.example.timeapk.BuildConfig

/** How a single reminder is armed. Exactly one path is armed per reminder; cancellation clears both. */
internal enum class ReminderDeliveryPath {
    EXACT_ALARM,
    WORK_MANAGER
}

/** Which notification a scheduled alarm belongs to. Each kind owns a disjoint request-code range. */
internal enum class ExactAlarmKind(val requestCodeBase: Int, val action: String) {
    EVENT_REMINDER(0x1000_0000, "com.example.timeapk.action.EXACT_EVENT_REMINDER"),
    MILESTONE_REMINDER(0x2000_0000, "com.example.timeapk.action.EXACT_MILESTONE_REMINDER")
}

internal object ExactReminderPolicy {
    /** Event ids are masked into the low 28 bits so kind ranges never overlap. */
    const val EVENT_ID_MASK = 0x0FFF_FFFF
    /** Build.VERSION_CODES.S: exact alarms became permission-gated. */
    const val API_EXACT_ALARM_GATED = 31

    /**
     * Exact alarms are used only when the channel ships the permission (not Play), the user opted in,
     * and the system currently allows exact alarms. Everything else stays on WorkManager.
     */
    fun selectPath(
        channelSupportsExact: Boolean,
        userEnabled: Boolean,
        canScheduleExactAlarms: Boolean
    ): ReminderDeliveryPath =
        if (channelSupportsExact && userEnabled && canScheduleExactAlarms) {
            ReminderDeliveryPath.EXACT_ALARM
        } else {
            ReminderDeliveryPath.WORK_MANAGER
        }

    /** API 31-32 grant SCHEDULE_EXACT_ALARM at install; 33+ needs a user grant; below 31 it is not gated. */
    fun canScheduleExact(sdkInt: Int, systemAllows: () -> Boolean): Boolean =
        if (sdkInt < API_EXACT_ALARM_GATED) true else systemAllows()

    fun requestCode(kind: ExactAlarmKind, eventId: Int): Int =
        kind.requestCodeBase + (eventId and EVENT_ID_MASK)

    fun dataUri(kind: ExactAlarmKind, eventId: Int): String =
        "glimmer-reminder://${kind.name.lowercase()}/$eventId"
}

/** Synchronous mirror of the DataStore "exact reminders" preference so schedulers can read it off any thread. */
internal object ExactReminderPreferenceMirror {
    private const val PREFS = "exact_reminder_mirror"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_MILESTONE_ALARM_IDS = "milestone_alarm_event_ids"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit(commit = true) {
            putBoolean(KEY_ENABLED, enabled)
        }
    }

    fun milestoneAlarmEventIds(context: Context): Set<Int> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_MILESTONE_ALARM_IDS, emptySet())
            .orEmpty()
            .mapNotNull(String::toIntOrNull)
            .toSet()

    @Synchronized
    fun updateMilestoneAlarmEventId(context: Context, eventId: Int, armed: Boolean) {
        val current = milestoneAlarmEventIds(context)
        val next = if (armed) current + eventId else current - eventId
        if (next == current) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putStringSet(KEY_MILESTONE_ALARM_IDS, next.map(Int::toString).toSet())
        }
    }
}

internal object ExactReminderAlarms {
    private const val TAG = "ExactReminderAlarms"

    fun channelSupportsExact(): Boolean = BuildConfig.EXACT_REMINDERS_SUPPORTED

    fun canScheduleExactAlarms(context: Context): Boolean =
        ExactReminderPolicy.canScheduleExact(Build.VERSION.SDK_INT) {
            val am = context.getSystemService(AlarmManager::class.java)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am?.canScheduleExactAlarms() == true
        }

    fun currentPath(context: Context): ReminderDeliveryPath = ExactReminderPolicy.selectPath(
        channelSupportsExact = channelSupportsExact(),
        userEnabled = ExactReminderPreferenceMirror.isEnabled(context),
        canScheduleExactAlarms = canScheduleExactAlarms(context)
    )

    /** Returns false when the alarm could not be armed, so the caller falls back to WorkManager. */
    fun schedule(
        context: Context,
        kind: ExactAlarmKind,
        eventId: Int,
        triggerAtMillis: Long,
        payload: Data
    ): Boolean {
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        val intent = baseIntent(context, kind, eventId).apply { putExtras(payload) }
        val pending = PendingIntent.getBroadcast(
            context,
            ExactReminderPolicy.requestCode(kind, eventId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            if (kind == ExactAlarmKind.MILESTONE_REMINDER) {
                ExactReminderPreferenceMirror.updateMilestoneAlarmEventId(context, eventId, armed = true)
            }
            true
        } catch (security: SecurityException) {
            Log.w(TAG, "Exact alarm denied for eventId=$eventId; falling back to WorkManager", security)
            pending.cancel()
            false
        }
    }

    fun cancel(context: Context, kind: ExactAlarmKind, eventId: Int) {
        val pending = PendingIntent.getBroadcast(
            context,
            ExactReminderPolicy.requestCode(kind, eventId),
            baseIntent(context, kind, eventId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            context.getSystemService(AlarmManager::class.java)?.cancel(pending)
            pending.cancel()
        }
        if (kind == ExactAlarmKind.MILESTONE_REMINDER) {
            ExactReminderPreferenceMirror.updateMilestoneAlarmEventId(context, eventId, armed = false)
        }
    }

    fun cancelAllMilestones(context: Context) {
        ExactReminderPreferenceMirror.milestoneAlarmEventIds(context).forEach { eventId ->
            cancel(context, ExactAlarmKind.MILESTONE_REMINDER, eventId)
        }
    }

    private fun baseIntent(context: Context, kind: ExactAlarmKind, eventId: Int): Intent =
        Intent(context, ExactReminderAlarmReceiver::class.java)
            .setAction(kind.action)
            .setData(ExactReminderPolicy.dataUri(kind, eventId).toUri())

    private fun Intent.putExtras(data: Data) {
        data.keyValueMap.forEach { (key, value) ->
            when (value) {
                is String -> putExtra(key, value)
                is Int -> putExtra(key, value)
                is Long -> putExtra(key, value)
                is Boolean -> putExtra(key, value)
            }
        }
    }
}
