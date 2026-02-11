package com.example.timeapk.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.timeapk.MainActivity
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.ui.home.EventUiState
import com.example.timeapk.ui.home.toEventUiState
import kotlinx.coroutines.runBlocking

class CountdownAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val app = context.applicationContext as TimeApplication
        val list = runBlocking {
            app.repository.getAllEventsSnapshot()
                .map { it.toEventUiState() }
                // 优先显示未来事件（isPast=false 排前），同组内按天数升序
                .sortedWith(compareBy<EventUiState> { it.isPast }.thenBy { it.daysRemaining })
                .take(3)
        }
        val lines = list.map { state ->
            val base = if (state.isPast) {
                "${state.event.title}: ${context.getString(R.string.days_past_label)} ${state.daysRemaining} ${context.getString(R.string.days_left)}"
            } else {
                "${state.event.title}: ${context.getString(R.string.days_left_label)} ${state.daysRemaining} ${context.getString(R.string.days_left)}"
            }
            val tag = when {
                state.isPast -> context.getString(R.string.widget_tag_past)
                !state.isPast && state.daysRemaining in 0..3 -> context.getString(R.string.widget_tag_soon)
                else -> null
            }
            if (tag != null) "[$tag] $base" else base
        }
        val pendingHome = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        fun pendingForEvent(eventId: Int): PendingIntent = PendingIntent.getActivity(
            context, eventId,
            Intent(context, MainActivity::class.java).putExtra("open_event_id", eventId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
                setOnClickPendingIntent(R.id.widget_title, pendingHome)
                setTextViewText(R.id.widget_line1, lines.getOrNull(0) ?: context.getString(R.string.home_empty_title))
                setTextViewText(R.id.widget_line2, lines.getOrNull(1) ?: "")
                setTextViewText(R.id.widget_line3, lines.getOrNull(2) ?: "")
                setOnClickPendingIntent(R.id.widget_line1, list.getOrNull(0)?.event?.id?.let { pendingForEvent(it) } ?: pendingHome)
                setOnClickPendingIntent(R.id.widget_line2, list.getOrNull(1)?.event?.id?.let { pendingForEvent(it) } ?: pendingHome)
                setOnClickPendingIntent(R.id.widget_line3, list.getOrNull(2)?.event?.id?.let { pendingForEvent(it) } ?: pendingHome)
            }
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
