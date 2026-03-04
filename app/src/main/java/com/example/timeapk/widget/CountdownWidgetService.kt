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
import com.example.timeapk.ui.utils.DisplayModes
import com.example.timeapk.ui.utils.getAvailableDisplayModes
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.formatBetweenAsYMD
import com.example.timeapk.data.REPEAT_NONE
import java.time.LocalDate
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
    
    private var dateDeltaDisplayMode: Int = 0
    private var perEventDateDeltaModes: Map<Int, Int> = emptyMap()
    private var showMilestone: Boolean = true

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
                val pinnedEventIds = app.userPrefs.pinnedEventIdsFlow.first()
                dateDeltaDisplayMode = app.userPrefs.dateDeltaDisplayModeFlow.first()
                perEventDateDeltaModes = app.userPrefs.perEventDateDeltaDisplayModesFlow.first()
                showMilestone = app.userPrefs.showMilestoneFlow.first()

                val all = app.repository.getAllEventsSnapshot()
                    .map { it.toEventUiState(milestones) }

                // 先按是否已过期 + 剩余天数排序（与首页逻辑一致）
                val upcoming = all
                    .filter { !it.isPast }
                    .sortedBy { it.daysRemaining }
                val past = all
                    .filter { it.isPast }
                    .sortedBy { it.daysRemaining }
                var base = upcoming + past

                // 应用“置顶”排序：置顶事件始终在最前，其余按剩余时间排序
                if (pinnedEventIds.isNotEmpty()) {
                    val pinnedSet = pinnedEventIds.toSet()
                    val pinned = pinnedEventIds.mapNotNull { id ->
                        base.find { it.event.id == id }
                    }
                    val unpinned = base.filter { it.event.id !in pinnedSet }
                    base = pinned + unpinned
                }
                base
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

        val targetLocalDate = eventDateToLocalDate(state.event.date)
        val today = LocalDate.now()
        val isRepeating = state.event.repeatType != REPEAT_NONE
        val isToday = state.daysRemaining == 0L && !state.isPast

        val persistedMode = perEventDateDeltaModes[state.event.id] ?: dateDeltaDisplayMode
        val mode = when (persistedMode) {
            // 小组件不展示“重大里程碑”，若用户在应用中选择了里程碑模式，则退化为天数字段显示
            DisplayModes.MILESTONE -> if (state.isPast) DisplayModes.PAST_DAYS else DisplayModes.UNTIL_DAYS
            else -> persistedMode
        }

        val valueText = when (mode) {
            DisplayModes.PAST_DAYS -> {
                val days = if (isRepeating) state.daysPassed else state.daysElapsed
                "${context.getString(R.string.days_past_label)} ${formatDays(days)} ${context.getString(R.string.days_unit)}"
            }
            DisplayModes.PAST_YMD -> {
                "${context.getString(R.string.days_past_label)} ${formatBetweenAsYMD(targetLocalDate, today)}"
            }
            DisplayModes.UNTIL_DAYS -> {
                if (isToday) {
                    context.getString(R.string.days_today_label)
                } else {
                    val days = if (isRepeating) state.daysLeft else state.daysRemaining
                    val label = com.example.timeapk.ui.utils.getUntilLabel(context, state)
                    "$label ${formatDays(days)} ${context.getString(R.string.days_unit)}"
                }
            }
            DisplayModes.UNTIL_YMD -> {
                if (isToday) {
                    context.getString(R.string.days_today_label)
                } else {
                    val days = if (isRepeating) state.daysLeft else state.daysRemaining
                    val ymd = formatBetweenAsYMD(today, today.plusDays(days))
                    val label = com.example.timeapk.ui.utils.getUntilLabel(context, state)
                    "$label $ymd"
                }
            }
            else -> ""
        }

        val titleText = if (state.event.isLunar) {
            "[农] ${state.event.title}"
        } else {
            state.event.title
        }
        views.setTextViewText(R.id.widget_item_title, titleText)
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

