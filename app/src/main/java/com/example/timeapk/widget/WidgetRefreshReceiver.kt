package com.example.timeapk.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.timeapk.TimeApplication

/** App-internal boundary for custom widget refresh broadcasts. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "WidgetRefreshReceiver"
        internal const val ACTION_REFRESH_DATE_BOUNDARY =
            "com.example.timeapk.action.REFRESH_WIDGET_DATE_BOUNDARY"
        internal const val ACTION_REFRESH_CLOCK_CHANGED =
            "com.example.timeapk.action.REFRESH_WIDGET_CLOCK_CHANGED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action !in setOf(ACTION_REFRESH_DATE_BOUNDARY, ACTION_REFRESH_CLOCK_CHANGED)) return

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val countdownIds = CountdownAppWidgetProvider.getAppWidgetIds(context, appWidgetManager)
        if (action != ACTION_REFRESH_CLOCK_CHANGED) {
            WidgetDateBoundaryScheduler.scheduleOrCancel(context)
        }
        // Both widget families must be refreshed inside one goAsync lifetime; the single-event
        // widget used to be fire-and-forget and could be dropped when the process was reaped.
        val app = context.applicationContext as? TimeApplication
        val pending = goAsync()
        if (app == null) {
            pending.finish()
            return
        }
        app.launchAppTask {
            try {
                SingleEventWidgetProvider.refreshAllAndAwait(context)
                if (countdownIds.isNotEmpty()) {
                    CountdownAppWidgetProvider.refreshNow(
                        context = context,
                        appWidgetManager = appWidgetManager,
                        appWidgetIds = countdownIds
                    )
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Boundary widget refresh failed", t)
            } finally {
                pending.finish()
            }
        }
    }
}
