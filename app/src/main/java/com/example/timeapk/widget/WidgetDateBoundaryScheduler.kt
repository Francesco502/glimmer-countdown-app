package com.example.timeapk.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.timeapk.notifications.ExactReminderAlarms
import java.time.Instant
import java.time.ZoneId

/** Maintains one best-effort wake-up for the next civil-date boundary. */
internal object WidgetDateBoundaryScheduler {
    private const val REQUEST_CODE = 4_000

    fun nextLocalDateStartMillis(
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long = now.atZone(zoneId)
        .toLocalDate()
        .plusDays(1)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()

    fun scheduleOrCancel(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        val operation = pendingIntent(appContext)
        if (CountdownAppWidgetProvider.getAppWidgetIds(appContext).isEmpty() &&
            SingleEventWidgetProvider.getAppWidgetIds(appContext).isEmpty()
        ) {
            alarmManager.cancel(operation)
            return
        }

        // Prefer an exact wake-up when the user already granted exact reminders; otherwise the
        // inexact alarm is a best-effort fallback (in-app resume and DATE_CHANGED also refresh).
        val triggerAtMillis = nextLocalDateStartMillis()
        try {
            if (ExactReminderAlarms.canScheduleExactAlarms(appContext)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    operation
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    operation
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                operation
            )
        }
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        appContext.getSystemService(AlarmManager::class.java).cancel(pendingIntent(appContext))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WidgetRefreshReceiver::class.java)
            .setAction(WidgetRefreshReceiver.ACTION_REFRESH_DATE_BOUNDARY)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
