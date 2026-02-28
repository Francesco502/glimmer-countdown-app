package com.example.timeapk.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.example.timeapk.R

/**
 * 在事件数据变更后主动刷新倒计时小组件，使列表即时同步。
 */
object WidgetUpdater {
    fun refreshCountdownWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, CountdownAppWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(provider)
        if (ids.isNotEmpty()) {
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
        }
    }
}
