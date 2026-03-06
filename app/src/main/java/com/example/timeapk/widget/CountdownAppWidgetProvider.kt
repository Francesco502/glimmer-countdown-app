package com.example.timeapk.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.example.timeapk.MainActivity
import com.example.timeapk.R

class CountdownAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateSingleWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateSingleWidget(context, appWidgetManager, appWidgetId)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
    }

    private fun updateSingleWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val sizeBucket = resolveSizeBucket(appWidgetManager.getAppWidgetOptions(appWidgetId))
        val serviceIntent = Intent(context, CountdownWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(WidgetSizeBucket.EXTRA_SIZE_BUCKET, sizeBucket)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }

        val views = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
            setRemoteAdapter(R.id.widget_list, serviceIntent)
            setEmptyView(R.id.widget_list, R.id.widget_empty)

            val isDark = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val bgColorRes = if (isDark) R.color.widget_background_dark else R.color.widget_background_light
            val textColorRes = if (isDark) R.color.widget_text_dark else R.color.widget_text_light
            setInt(R.id.widget_root, "setBackgroundColor", ContextCompat.getColor(context, bgColorRes))
            setTextColor(R.id.widget_empty, ContextCompat.getColor(context, textColorRes))
        }

        val clickPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_list, clickPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun resolveSizeBucket(options: Bundle): Int {
        val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
        return when {
            minWidthDp < 150 -> WidgetSizeBucket.SMALL
            minWidthDp < 250 -> WidgetSizeBucket.MEDIUM
            else -> WidgetSizeBucket.LARGE
        }
    }
}
