package com.example.timeapk.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.timeapk.widget.WidgetDateBoundaryScheduler
import com.example.timeapk.widget.WidgetRefreshReceiver

/** AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED (API 31). */
internal const val ACTION_EXACT_ALARM_PERMISSION_STATE_CHANGED =
    "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
internal const val REASON_EXACT_ALARM_PERMISSION_CHANGED = "manual_exact_alarm_permission_changed"
internal const val REASON_EXACT_REMINDER_PREFERENCE_CHANGED = "manual_exact_reminder_preference_changed"

class RescheduleBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: "unknown"
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                WidgetDateBoundaryScheduler.scheduleOrCancel(context)
                if (action == Intent.ACTION_TIME_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED) {
                    context.sendBroadcast(
                        Intent(context, WidgetRefreshReceiver::class.java)
                            .setAction(WidgetRefreshReceiver.ACTION_REFRESH_CLOCK_CHANGED)
                    )
                }
                RescheduleAllWorker.enqueue(context, action)
            }
            ACTION_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                // Granting exact alarms moves reminders onto AlarmManager; force a full rebuild.
                RescheduleAllWorker.enqueue(context, REASON_EXACT_ALARM_PERMISSION_CHANGED)
            }
        }
    }
}
