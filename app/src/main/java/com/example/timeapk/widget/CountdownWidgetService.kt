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
import com.example.timeapk.ui.home.getMilestoneLabel
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
                dateDeltaDisplayMode = app.userPrefs.dateDeltaDisplayModeFlow.first()
                perEventDateDeltaModes = app.userPrefs.perEventDateDeltaDisplayModesFlow.first()
                showMilestone = app.userPrefs.showMilestoneFlow.first()
                
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

        val targetLocalDate = eventDateToLocalDate(state.event.date)
        val today = LocalDate.now()
        val isRepeating = state.event.repeatType != REPEAT_NONE
        val isToday = state.daysRemaining == 0L && !state.isPast
        
        val persistedMode = perEventDateDeltaModes[state.event.id] ?: dateDeltaDisplayMode
        val availableModes = getAvailableDisplayModes(state, showMilestone)
        val modeIndex = availableModes.indexOf(persistedMode)
        val mode = if (modeIndex != -1) persistedMode else availableModes.first()

        val valueText = when (mode) {
            DisplayModes.PAST_DAYS -> {
                val days = if (isRepeating) state.daysPassed else state.daysElapsed
                "${formatDays(days)} ${context.getString(R.string.days_unit)}"
            }
            DisplayModes.PAST_YMD -> {
                formatBetweenAsYMD(targetLocalDate, today)
            }
            DisplayModes.UNTIL_DAYS -> {
                if (isToday) {
                    context.getString(R.string.days_today_label)
                } else {
                    val days = if (isRepeating) state.daysLeft else state.daysRemaining
                    val label = com.example.timeapk.ui.utils.getUntilLabel(context, state)
                    if (label != context.getString(R.string.days_until_label)) {
                        "$label ${formatDays(days)} ${context.getString(R.string.days_unit)}"
                    } else {
                        "${formatDays(days)} ${context.getString(R.string.days_unit)}"
                    }
                }
            }
            DisplayModes.UNTIL_YMD -> {
                if (isToday) {
                    context.getString(R.string.days_today_label)
                } else {
                    val days = if (isRepeating) state.daysLeft else state.daysRemaining
                    val ymd = formatBetweenAsYMD(today, today.plusDays(days))
                    val label = com.example.timeapk.ui.utils.getUntilLabel(context, state)
                    if (label != context.getString(R.string.days_until_label)) {
                        "$label $ymd"
                    } else {
                        ymd
                    }
                }
            }
            DisplayModes.MILESTONE -> {
                val milestoneStr = getMilestoneLabel(context, state.nextMilestoneValue!!)
                context.getString(R.string.milestone_in_days, milestoneStr, state.nextMilestoneDays!!.toInt())
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

