package com.example.timeapk.widget

import android.content.Context
import com.example.timeapk.R
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.ui.home.EventUiState
import com.example.timeapk.ui.home.pickNearestUpcomingEvent
import com.example.timeapk.ui.utils.formatDays
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 单事件小组件选中“跟随最近事件”时保存的值。 */
const val SINGLE_EVENT_FOLLOW_NEAREST = -1

/** 单事件小组件渲染所需的文字；与 RemoteViews 解耦，便于测试。 */
internal data class SingleEventWidgetText(
    val eventId: Int,
    val title: String,
    val label: String,
    val number: String,
    val unit: String,
    val dateText: String,
    val isToday: Boolean
)

internal object SingleEventWidgetPolicy {
    /** 选中的事件已被删除时回落到最近事件；没有任何事件时返回 null。 */
    fun resolveTarget(
        states: List<EventUiState>,
        selectedEventId: Int,
        today: LocalDate,
        pinnedEventIds: List<Int>
    ): EventUiState? {
        if (selectedEventId != SINGLE_EVENT_FOLLOW_NEAREST) {
            states.firstOrNull { it.event.id == selectedEventId }?.let { return it }
        }
        return pickNearestUpcomingEvent(states, today, pinnedEventIds)
            ?: states.maxByOrNull { it.nextOccurrenceDate }
    }

    fun buildText(context: Context, state: EventUiState): SingleEventWidgetText {
        val locale = context.resources.configuration.locales[0]
        val isRepeating = state.event.repeatType != REPEAT_NONE
        val isToday = state.daysRemaining == 0L && !state.isPast
        val datePattern = context.getString(R.string.calendar_selected_date_pattern)
        val dateText = state.nextOccurrenceDate.format(DateTimeFormatter.ofPattern(datePattern, locale))
        if (isToday) {
            return SingleEventWidgetText(
                eventId = state.event.id,
                title = state.event.title,
                label = "",
                number = context.getString(R.string.widget_today_compact),
                unit = "",
                dateText = dateText,
                isToday = true
            )
        }
        val days = when {
            state.isPast -> if (isRepeating) state.daysPassed else state.daysElapsed
            isRepeating -> state.daysLeft
            else -> state.daysRemaining
        }
        val dayCount = days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return SingleEventWidgetText(
            eventId = state.event.id,
            title = state.event.title,
            label = context.getString(if (state.isPast) R.string.days_past_label else R.string.days_until_label),
            number = formatDays(days, locale),
            unit = context.resources.getQuantityString(R.plurals.days_unit, dayCount),
            dateText = dateText,
            isToday = false
        )
    }

    /** 数字字号随宽度和高度格数变化；1×1 只保留数字与标题。 */
    fun numberTextSp(widthDp: Int, heightDp: Int): Float = when {
        widthDp < 120 || heightDp < 100 -> 30f
        widthDp < 200 -> 40f
        else -> 48f
    }

    fun showsDate(widthDp: Int, heightDp: Int): Boolean = widthDp >= 120 && heightDp >= 100
}
