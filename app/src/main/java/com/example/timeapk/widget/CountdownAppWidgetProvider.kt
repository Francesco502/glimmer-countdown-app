package com.example.timeapk.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
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
        for (id in appWidgetIds) {
            val intent = Intent(context, CountdownWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            val views = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
                setRemoteAdapter(R.id.widget_list, intent)
                setEmptyView(R.id.widget_list, R.id.widget_empty)

                // 背景颜色跟随系统深浅
                val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                val bgColorRes = if (isDark) R.color.widget_background_dark else R.color.widget_background_light
                val bgColor = ContextCompat.getColor(context, bgColorRes)
                setInt(R.id.widget_root, "setBackgroundColor", bgColor)

                val textColorRes = if (isDark) R.color.widget_text_dark else R.color.widget_text_light
                val textColor = ContextCompat.getColor(context, textColorRes)
                setTextColor(R.id.widget_empty, textColor)
            }

            // 列表项点击模板：打开对应事件详情
            val clickIntent = Intent(context, MainActivity::class.java)
            val clickPending = PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list, clickPending)

            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
