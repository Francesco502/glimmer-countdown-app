package com.example.timeapk.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.net.toUri
import com.example.timeapk.MainActivity
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CountdownAppWidgetProvider : AppWidgetProvider() {
    companion object {
        fun refreshAllWidgets(context: Context) {
            val app = context.applicationContext as? TimeApplication ?: return
            app.launchAppTask {
                refreshAllWidgetsAndAwait(context)
            }
        }

        suspend fun refreshAllWidgetsAndAwait(context: Context) {
            val app = context.applicationContext as? TimeApplication ?: return
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = getAppWidgetIds(context, appWidgetManager)
            if (ids.isEmpty()) return
            withContext(Dispatchers.IO) {
                runCoordinatedRefresh(app, appWidgetManager, ids)
            }
        }

        fun getAppWidgetIds(
            context: Context,
            appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
        ): IntArray {
            val provider = ComponentName(context, CountdownAppWidgetProvider::class.java)
            return appWidgetManager.getAppWidgetIds(provider)
        }

        private suspend fun runCoordinatedRefresh(
            app: TimeApplication,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            WidgetRefreshCoordinator.runLatestSnapshot {
                refreshWidgets(app, appWidgetManager, appWidgetIds)
            }
        }

        private suspend fun refreshWidgets(
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

        private suspend fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            themeSnapshot: WidgetThemeSnapshot
        ) {
            val sizeBucket = resolveSizeBucket(appWidgetManager.getAppWidgetOptions(appWidgetId))
            val config = WidgetConfigRepository(context).getConfigForWidget(appWidgetId)
            val views = buildWidgetRemoteViews(context, appWidgetId, sizeBucket, config, themeSnapshot)
            appWidgetManager.updateAppWidget(appWidgetId, views)
            notifyWidgetListChanged(appWidgetManager, appWidgetId)
        }

        internal fun launchRefresh(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            pendingResult: PendingResult
        ) {
            val app = context.applicationContext as? TimeApplication
            if (app == null) {
                pendingResult.finish()
                return
            }
            app.launchAppTask {
                try {
                    withContext(Dispatchers.IO) {
                        runCoordinatedRefresh(app, appWidgetManager, appWidgetIds)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }

        internal fun buildWidgetRemoteViews(
            context: Context,
            appWidgetId: Int,
            sizeBucket: Int,
            config: WidgetConfig,
            themeSnapshot: WidgetThemeSnapshot,
            attachRemoteAdapter: Boolean = true
        ): RemoteViews {
            val openAppPendingIntent = createOpenAppPendingIntent(context, appWidgetId)
            val renderStyle = WidgetRenderPolicy.resolve(config, themeSnapshot)
            val textStyle = WidgetStylePolicy.resolve(sizeBucket, config.fontScale, config.densityMode)

            return RemoteViews(context.packageName, renderStyle.rootLayoutResId).apply {
                setInt(android.R.id.background, "setBackgroundResource", renderStyle.backgroundResId)
                if (!renderStyle.useThemeTextColors) {
                    setTextColor(R.id.widget_empty, renderStyle.secondaryTextColor)
                }
                setTextViewTextSize(R.id.widget_empty, TypedValue.COMPLEX_UNIT_SP, textStyle.emptyTextSp)
                if (attachRemoteAdapter) {
                    setWidgetListRemoteAdapter(
                        createRemoteAdapterIntent(context, appWidgetId, sizeBucket, config, themeSnapshot)
                    )
                    setPendingIntentTemplate(R.id.widget_list, openAppPendingIntent)
                }
                setEmptyView(R.id.widget_list, R.id.widget_empty)
                setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
                setOnClickPendingIntent(R.id.widget_empty, openAppPendingIntent)
            }
        }

        private fun resolveSizeBucket(options: Bundle): Int {
            val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
            val maxWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidthDp)
            val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            val maxHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeightDp)
            return WidgetSizeBucket.resolve(minWidthDp, maxWidthDp, minHeightDp, maxHeightDp)
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
            config: WidgetConfig,
            themeSnapshot: WidgetThemeSnapshot
        ): Intent {
            return Intent(context, CountdownWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(WidgetSizeBucket.EXTRA_SIZE_BUCKET, sizeBucket)
                data = buildWidgetRemoteAdapterDataUri(
                    appWidgetId = appWidgetId,
                    sizeBucket = sizeBucket,
                    config = config,
                    themeKey = themeSnapshot.remoteCollectionKey
                )
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
        if (intent.action == Intent.ACTION_DATE_CHANGED) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = getAppWidgetIds(context, appWidgetManager)
            WidgetDateBoundaryScheduler.scheduleOrCancel(context)
            if (appWidgetIds.isEmpty()) return
            launchRefresh(
                context,
                appWidgetManager,
                appWidgetIds,
                goAsync()
            )
            return
        }
        super.onReceive(context, intent)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetDateBoundaryScheduler.scheduleOrCancel(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetDateBoundaryScheduler.scheduleOrCancel(context)
        launchRefresh(context, appWidgetManager, appWidgetIds, goAsync())
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        WidgetDateBoundaryScheduler.scheduleOrCancel(context)
        launchRefresh(context, appWidgetManager, intArrayOf(appWidgetId), goAsync())
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WidgetDateBoundaryScheduler.scheduleOrCancel(context)
        val app = context.applicationContext as? TimeApplication ?: return
        app.launchAppTask {
            val repository = WidgetConfigRepository(app)
            appWidgetIds.forEach { id ->
                repository.removeConfigForWidget(id)
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetDateBoundaryScheduler.cancel(context)
    }
}

internal fun buildWidgetRemoteAdapterDataUri(
    appWidgetId: Int,
    sizeBucket: Int,
    config: WidgetConfig,
    themeKey: String
): android.net.Uri {
    return buildWidgetRemoteAdapterDataUriString(
        appWidgetId = appWidgetId,
        sizeBucket = sizeBucket,
        config = config,
        themeKey = themeKey
    ).toUri()
}

internal fun buildWidgetRemoteAdapterDataUriString(
    appWidgetId: Int,
    sizeBucket: Int,
    config: WidgetConfig,
    themeKey: String
): String {
    val clean = config.sanitize()
    return ("glimmer://widget/$appWidgetId" +
        "?size=$sizeBucket" +
        "&width=${clean.widthCells}" +
        "&height=${clean.heightCells}" +
        "&appearance=${clean.appearancePreset}" +
        "&opacity=${clean.backgroundOpacityPercent}" +
        "&density=${clean.densityMode}" +
        "&scope=${clean.contentScope}" +
        "&sort=${clean.sortMode}" +
        "&font=${(clean.fontScale * 100).toInt()}" +
        "&theme=$themeKey" +
        "&key=${clean.cacheKey}")
}
