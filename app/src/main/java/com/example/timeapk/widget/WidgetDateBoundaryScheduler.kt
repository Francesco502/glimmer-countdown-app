package com.example.timeapk.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
        if (CountdownAppWidgetProvider.getAppWidgetIds(appContext).isEmpty()) {
            alarmManager.cancel(operation)
            return
        }

        // This intentionally avoids exact-alarm APIs and their restricted permission.
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextLocalDateStartMillis(),
            operation
        )
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
