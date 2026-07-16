package com.example.timeapk.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.timeapk.widget.CountdownAppWidgetProvider
import com.example.timeapk.widget.WidgetDateBoundaryScheduler

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
                        Intent(context, CountdownAppWidgetProvider::class.java)
                            .setAction(CountdownAppWidgetProvider.ACTION_REFRESH_CLOCK_CHANGED)
                    )
                }
                RescheduleAllWorker.enqueue(context, action)
            }
        }
    }
}
