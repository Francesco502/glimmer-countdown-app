package com.example.timeapk.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.ui.home.EventUiState
import com.example.timeapk.ui.home.toEventUiState
import com.example.timeapk.ui.utils.formatDays
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.core.content.ContextCompat

class CountdownWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CountdownRemoteViewsFactory(applicationContext, intent)
    }
}

class CountdownRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var appWidgetId: Int =
        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

    private val items = mutableListOf<EventUiState>()

    override fun onCreate() {
        // no-op
    }

    override fun onDestroy() {
        items.clear()
    }

    override fun onDataSetChanged() {
        // 允许在 DataStore / Room 上下文中访问
        val identityToken = Binder.clearCallingIdentity()
        try {
            val app = context.applicationContext as TimeApplication
            val list = runBlocking {
                val milestones = app.userPrefs.customMilestonesFlow.first()
                app.repository.getAllEventsSnapshot()
                    .map { it.toEventUiState(milestones) }
                    .sortedWith(
                        compareBy<EventUiState> { it.isPast }.thenBy { it.daysRemaining }
                    )
            }
            items.clear()
            items.addAll(list)
        } finally {
            Binder.restoreCallingIdentity(identityToken)
        }
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= items.size) return null
        val state = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_countdown_item)

        val isDark = (context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val textColorRes = if (isDark) R.color.widget_text_dark else R.color.widget_text_light
        val textColor = ContextCompat.getColor(context, textColorRes)
        val tagColorRes = if (isDark) R.color.widget_tag_dark else R.color.widget_tag_light
        val tagColor = ContextCompat.getColor(context, tagColorRes)

        val tagText = when {
            state.isPast -> context.getString(R.string.widget_tag_past)
            !state.isPast && state.daysRemaining <= 7L -> context.getString(R.string.widget_tag_soon)
            else -> null
        }
        if (tagText != null) {
            views.setViewVisibility(R.id.widget_item_tag, View.VISIBLE)
            views.setTextViewText(R.id.widget_item_tag, tagText)
            views.setTextColor(R.id.widget_item_tag, tagColor)
        } else {
            views.setViewVisibility(R.id.widget_item_tag, View.GONE)
        }

        val daysStr = formatDays(state.daysRemaining)
        val valueText = when {
            state.isPast ->
                "${context.getString(R.string.days_past_label)} $daysStr ${context.getString(R.string.days_left)}"
            state.daysRemaining == 0L ->
                context.getString(R.string.days_today_label)
            else ->
                "${context.getString(R.string.days_left_label)} $daysStr ${context.getString(R.string.days_left)}"
        }

        views.setTextViewText(R.id.widget_item_title, state.event.title)
        views.setTextViewText(R.id.widget_item_value, valueText)
        views.setTextColor(R.id.widget_item_title, textColor)
        views.setTextColor(R.id.widget_item_value, textColor)

        val fillIntent = Intent().apply {
            putExtra("open_event_id", state.event.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = items.getOrNull(position)?.event?.id?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}

