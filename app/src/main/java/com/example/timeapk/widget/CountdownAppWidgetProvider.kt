package com.example.timeapk.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.net.toUri
import com.example.timeapk.MainActivity
import com.example.timeapk.R

class CountdownAppWidgetProvider : AppWidgetProvider() {
    companion object {
        fun refreshAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            refreshWidgets(context, appWidgetManager, getAppWidgetIds(context, appWidgetManager))
        }

        fun getAppWidgetIds(
            context: Context,
            appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
        ): IntArray {
            val provider = ComponentName(context, CountdownAppWidgetProvider::class.java)
            return appWidgetManager.getAppWidgetIds(provider)
        }

        private fun refreshWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (appWidgetIds.isEmpty()) return
            val themeSnapshot = WidgetThemeResolver.resolve(context)
            appWidgetIds.forEach { appWidgetId ->
                updateSingleWidget(context, appWidgetManager, appWidgetId, themeSnapshot)
            }
        }

        private fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            themeSnapshot: WidgetThemeSnapshot
        ) {
            val sizeBucket = resolveSizeBucket(appWidgetManager.getAppWidgetOptions(appWidgetId))
            val openAppPendingIntent = createOpenAppPendingIntent(context, appWidgetId)

            val views = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
                setWidgetListRemoteAdapter(
                    createRemoteAdapterIntent(context, appWidgetId, sizeBucket, themeSnapshot)
                )
                setEmptyView(R.id.widget_list, R.id.widget_empty)
                setPendingIntentTemplate(R.id.widget_list, openAppPendingIntent)
                setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
                setOnClickPendingIntent(R.id.widget_empty, openAppPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
            notifyWidgetListChanged(appWidgetManager, appWidgetId)
        }

        private fun resolveSizeBucket(options: Bundle): Int {
            val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
            val maxWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidthDp)
            return WidgetSizeBucket.resolve(minWidthDp, maxWidthDp)
        }

        private fun createOpenAppPendingIntent(
            context: Context,
            appWidgetId: Int
        ): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
            return PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                flags
            )
        }

        private fun createRemoteAdapterIntent(
            context: Context,
            appWidgetId: Int,
            sizeBucket: Int,
            themeSnapshot: WidgetThemeSnapshot
        ): Intent {
            return Intent(context, CountdownWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(WidgetSizeBucket.EXTRA_SIZE_BUCKET, sizeBucket)
                data =
                    "glimmer://widget/$appWidgetId?size=$sizeBucket&theme=${themeSnapshot.remoteCollectionKey}"
                        .toUri()
            }
        }

        @Suppress("DEPRECATION")
        private fun RemoteViews.setWidgetListRemoteAdapter(intent: Intent) {
            setRemoteAdapter(R.id.widget_list, intent)
        }

        @Suppress("DEPRECATION")
        private fun notifyWidgetListChanged(
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_CONFIGURATION_CHANGED) {
            refreshAllWidgets(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        refreshWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        refreshWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
    }
}
