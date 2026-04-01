package com.example.timeapk.widget

import android.appwidget.AppWidgetManager
import android.content.Context

/**
 * 在事件数据变更后主动刷新倒计时小组件，使列表即时同步。
 */
object WidgetUpdater {
    fun refreshCountdownWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = CountdownAppWidgetProvider.getAppWidgetIds(context, appWidgetManager)
        if (ids.isEmpty()) return
        CountdownAppWidgetProvider.refreshAllWidgets(context)
    }
}
