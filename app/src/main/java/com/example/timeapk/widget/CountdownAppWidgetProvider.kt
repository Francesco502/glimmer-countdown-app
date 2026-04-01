package com.example.timeapk.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
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
            val contentSnapshot = WidgetContentResolver.load(context, sizeBucket)

            val views = RemoteViews(context.packageName, R.layout.widget_countdown_static).apply {
                val isDark = themeSnapshot.isDark
                val bgColorRes = if (isDark) R.color.widget_background_dark else R.color.widget_background_light
                val textColorRes = if (isDark) R.color.widget_text_dark else R.color.widget_text_light
                val backgroundColor = ContextCompat.getColor(context, bgColorRes)
                val textColor = ContextCompat.getColor(context, textColorRes)
                setInt(R.id.widget_root, "setBackgroundColor", backgroundColor)
                setInt(R.id.widget_empty, "setBackgroundColor", backgroundColor)
                setTextColor(R.id.widget_empty, textColor)
                setOnClickPendingIntent(
                    R.id.widget_root,
                    createOpenAppPendingIntent(context, appWidgetId)
                )

                if (contentSnapshot.items.isEmpty()) {
                    setViewVisibility(R.id.widget_empty, View.VISIBLE)
                    ROW_IDS.forEach { rowId ->
                        setViewVisibility(rowId, View.GONE)
                    }
                    setOnClickPendingIntent(
                        R.id.widget_empty,
                        createOpenAppPendingIntent(context, appWidgetId)
                    )
                } else {
                    setViewVisibility(R.id.widget_empty, View.GONE)
                    bindRows(
                        context = context,
                        appWidgetId = appWidgetId,
                        views = this,
                        items = contentSnapshot.items,
                        textStyle = contentSnapshot.textStyle,
                        textColor = textColor
                    )
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun bindRows(
            context: Context,
            appWidgetId: Int,
            views: RemoteViews,
            items: List<WidgetRenderedItem>,
            textStyle: WidgetTextStyle,
            textColor: Int
        ) {
            ROW_IDS.indices.forEach { index ->
                val rowId = ROW_IDS[index]
                val titleId = TITLE_IDS[index]
                val valueId = VALUE_IDS[index]
                val item = items.getOrNull(index)

                if (item == null) {
                    views.setViewVisibility(rowId, View.GONE)
                    return@forEach
                }

                views.setViewVisibility(rowId, View.VISIBLE)
                views.setTextViewText(titleId, item.title)
                views.setTextViewText(valueId, item.value)
                views.setTextColor(titleId, textColor)
                views.setTextColor(valueId, textColor)
                views.setTextViewTextSize(titleId, TypedValue.COMPLEX_UNIT_SP, textStyle.titleSp)
                views.setTextViewTextSize(valueId, TypedValue.COMPLEX_UNIT_SP, textStyle.valueSp)
                views.setViewPadding(
                    rowId,
                    dp(context, textStyle.paddingHorizontalDp),
                    dp(context, textStyle.paddingVerticalDp),
                    dp(context, textStyle.paddingHorizontalDp),
                    dp(context, textStyle.paddingVerticalDp)
                )
                views.setInt(valueId, "setMaxEms", textStyle.valueMaxEms)
                views.setOnClickPendingIntent(
                    rowId,
                    createOpenEventPendingIntent(context, appWidgetId, item.eventId, index)
                )
            }
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
            return PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun createOpenEventPendingIntent(
            context: Context,
            appWidgetId: Int,
            eventId: Int,
            rowIndex: Int
        ): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("open_event_id", eventId)
            }
            return PendingIntent.getActivity(
                context,
                appWidgetId * 100 + rowIndex + 1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun dp(context: Context, value: Int): Int {
            val density = context.resources.displayMetrics.density
            return (value * density).toInt()
        }

        private val ROW_IDS = intArrayOf(
            R.id.widget_row_0,
            R.id.widget_row_1,
            R.id.widget_row_2,
            R.id.widget_row_3
        )

        private val TITLE_IDS = intArrayOf(
            R.id.widget_row_0_title,
            R.id.widget_row_1_title,
            R.id.widget_row_2_title,
            R.id.widget_row_3_title
        )

        private val VALUE_IDS = intArrayOf(
            R.id.widget_row_0_value,
            R.id.widget_row_1_value,
            R.id.widget_row_2_value,
            R.id.widget_row_3_value
        )
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
