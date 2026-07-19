package com.example.timeapk.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** App-internal boundary for custom widget refresh broadcasts. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    companion object {
        internal const val ACTION_REFRESH_DATE_BOUNDARY =
            "com.example.timeapk.action.REFRESH_WIDGET_DATE_BOUNDARY"
        internal const val ACTION_REFRESH_CLOCK_CHANGED =
            "com.example.timeapk.action.REFRESH_WIDGET_CLOCK_CHANGED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action !in setOf(ACTION_REFRESH_DATE_BOUNDARY, ACTION_REFRESH_CLOCK_CHANGED)) return

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = CountdownAppWidgetProvider.getAppWidgetIds(context, appWidgetManager)
        if (action != ACTION_REFRESH_CLOCK_CHANGED) {
            WidgetDateBoundaryScheduler.scheduleOrCancel(context)
        }
        if (appWidgetIds.isEmpty()) return
        CountdownAppWidgetProvider.launchRefresh(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetIds = appWidgetIds,
            pendingResult = goAsync()
        )
    }
}
