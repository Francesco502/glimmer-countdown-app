package com.example.timeapk.widget

import android.content.Context
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.ui.home.EventUiState
import com.example.timeapk.ui.home.toEventUiState
import com.example.timeapk.ui.utils.DisplayModes
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.formatBetweenAsYMD
import com.example.timeapk.ui.utils.formatDays
import com.example.timeapk.ui.utils.getAvailableDisplayModes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

internal data class WidgetRenderedItem(
    val eventId: Int,
    val title: String,
    val value: String
)

internal data class WidgetContentSnapshot(
    val items: List<WidgetRenderedItem>,
    val textStyle: WidgetTextStyle
)

internal object WidgetContentResolver {
    private const val SMALL_VISIBLE_ROWS = 2
    private const val MEDIUM_VISIBLE_ROWS = 3
    private const val LARGE_VISIBLE_ROWS = 4

    fun load(context: Context, sizeBucket: Int): WidgetContentSnapshot {
        val app = context.applicationContext as? TimeApplication
            ?: return WidgetContentSnapshot(
                items = emptyList(),
                textStyle = WidgetStylePolicy.resolve(sizeBucket, 1f)
            )

        return runBlocking {
            val prefs = app.userPrefs
            val milestones = prefs.customMilestonesFlow.first()
            val pinnedEventIds = prefs.pinnedEventIdsFlow.first()
            val preferredMode = prefs.dateDeltaDisplayModeFlow.first()
            val perEventModes = prefs.perEventDateDeltaDisplayModesFlow.first()
            val widgetFontScale = prefs.widgetFontScaleFlow.first()
            val showMilestone = prefs.showMilestoneFlow.first()
            val smartMilestonesEnabled = prefs.smartMilestonesEnabledFlow.first()
            val textStyle = WidgetStylePolicy.resolve(sizeBucket, widgetFontScale)

            val ordered = app.repository.getAllEventsSnapshot()
                .map { it.toEventUiState(milestones, smartMilestonesEnabled) }
                .let(::sortForWidget)
                .let { applyPinnedOrder(it, pinnedEventIds) }

            val visibleRows = visibleRowsForSizeBucket(sizeBucket)
            val items = ordered.take(visibleRows).map { state ->
                buildRenderedItem(
                    context = context,
                    state = state,
                    sizeBucket = sizeBucket,
                    preferredMode = perEventModes[state.event.id] ?: preferredMode,
                    showMilestone = showMilestone,
                    textStyle = textStyle
                )
            }

            WidgetContentSnapshot(
                items = items,
                textStyle = textStyle
            )
        }
    }

    internal fun resolveDisplayMode(
        state: EventUiState,
        preferredMode: Int,
        showMilestone: Boolean
    ): Int {
        val availableModes = getAvailableDisplayModes(state, showMilestone)
        return if (preferredMode in availableModes) preferredMode else availableModes.first()
    }

    private fun visibleRowsForSizeBucket(sizeBucket: Int): Int {
        return when (sizeBucket) {
            WidgetSizeBucket.SMALL -> SMALL_VISIBLE_ROWS
            WidgetSizeBucket.LARGE -> LARGE_VISIBLE_ROWS
            else -> MEDIUM_VISIBLE_ROWS
        }
    }

    private fun sortForWidget(states: List<EventUiState>): List<EventUiState> {
        val upcoming = states.filter { !it.isPast }.sortedBy { it.daysRemaining }
        val past = states.filter { it.isPast }.sortedBy { it.daysRemaining }
        return upcoming + past
    }

    private fun applyPinnedOrder(
        states: List<EventUiState>,
        pinnedEventIds: List<Int>
    ): List<EventUiState> {
        if (pinnedEventIds.isEmpty()) return states
        val pinnedSet = pinnedEventIds.toSet()
        val pinned = pinnedEventIds.mapNotNull { id -> states.find { it.event.id == id } }
        val unpinned = states.filter { it.event.id !in pinnedSet }
        return pinned + unpinned
    }

    private fun buildRenderedItem(
        context: Context,
        state: EventUiState,
        sizeBucket: Int,
        preferredMode: Int,
        showMilestone: Boolean,
        textStyle: WidgetTextStyle
    ): WidgetRenderedItem {
        val locale = context.resources.configuration.locales[0]
        val targetLocalDate = eventDateToLocalDate(state.event.date)
        val today = LocalDate.now()
        val isRepeating = state.event.repeatType != REPEAT_NONE
        val isToday = state.daysRemaining == 0L && !state.isPast
        val resolvedMode = resolveDisplayMode(state, preferredMode, showMilestone)

        val valueText = when (sizeBucket) {
            WidgetSizeBucket.SMALL -> buildAdaptiveSmallValueText(
                state = state,
                mode = resolvedMode,
                targetLocalDate = targetLocalDate,
                today = today,
                isRepeating = isRepeating,
                isToday = isToday,
                maxChars = textStyle.valueMaxEms,
                context = context
            )
            WidgetSizeBucket.LARGE -> buildVerboseValueText(
                context = context,
                state = state,
                mode = resolvedMode,
                targetLocalDate = targetLocalDate,
                today = today,
                isRepeating = isRepeating,
                isToday = isToday
            )
            else -> buildCompactValueText(
                context = context,
                state = state,
                mode = resolvedMode,
                targetLocalDate = targetLocalDate,
                today = today,
                isRepeating = isRepeating,
                isToday = isToday
            )
        }

        val titleText = if (state.event.isLunar) {
            context.getString(R.string.widget_lunar_prefix, state.event.title)
        } else {
            state.event.title
        }

        return WidgetRenderedItem(
            eventId = state.event.id,
            title = titleText,
            value = valueText
        )
    }

    private fun buildAdaptiveSmallValueText(
        state: EventUiState,
        mode: Int,
        targetLocalDate: LocalDate,
        today: LocalDate,
        isRepeating: Boolean,
        isToday: Boolean,
        maxChars: Int,
        context: Context
    ): String {
        val preferred = buildCompactValueText(
            context = context,
            state = state,
            mode = mode,
            targetLocalDate = targetLocalDate,
            today = today,
            isRepeating = isRepeating,
            isToday = isToday
        )
        val fallback = buildNumericValueText(
            state = state,
            mode = mode,
            isRepeating = isRepeating,
            isToday = isToday
        )
        return WidgetValueFormatter.semanticValueOrFallback(preferred, fallback, maxChars)
    }

    private fun buildNumericValueText(
        state: EventUiState,
        mode: Int,
        isRepeating: Boolean,
        isToday: Boolean
    ): String {
        return WidgetValueFormatter.numericValueForSmall(
            mode = mode,
            isPast = state.isPast,
            isRepeating = isRepeating,
            isToday = isToday,
            daysElapsed = state.daysElapsed,
            daysPassed = state.daysPassed,
            daysRemaining = state.daysRemaining,
            daysLeft = state.daysLeft
        )
    }

    private fun buildCompactValueText(
        context: Context,
        state: EventUiState,
        mode: Int,
        targetLocalDate: LocalDate,
        today: LocalDate,
        isRepeating: Boolean,
        isToday: Boolean
    ): String {
        if (isToday) return context.getString(R.string.widget_today_compact)
        val locale = context.resources.configuration.locales[0]

        return when (mode) {
            DisplayModes.PAST_DAYS -> {
                val days = if (isRepeating) state.daysPassed else state.daysElapsed
                context.getString(R.string.widget_past_days_semantic, formatDays(days, locale))
            }
            DisplayModes.PAST_YMD -> {
                context.getString(
                    R.string.widget_past_ymd_semantic,
                    formatBetweenAsYMD(targetLocalDate, today, locale)
                )
            }
            DisplayModes.UNTIL_DAYS -> {
                val days = if (isRepeating) state.daysLeft else state.daysRemaining
                context.getString(R.string.widget_until_days_semantic, formatDays(days, locale))
            }
            DisplayModes.UNTIL_YMD -> {
                val days = if (isRepeating) state.daysLeft else state.daysRemaining
                context.getString(
                    R.string.widget_until_ymd_semantic,
                    formatBetweenAsYMD(today, today.plusDays(days), locale)
                )
            }
            DisplayModes.MILESTONE -> {
                val days = state.nextMilestoneDays ?: 0L
                context.getString(R.string.widget_until_days_semantic, formatDays(days, locale))
            }
            else -> ""
        }
    }

    private fun buildVerboseValueText(
        context: Context,
        state: EventUiState,
        mode: Int,
        targetLocalDate: LocalDate,
        today: LocalDate,
        isRepeating: Boolean,
        isToday: Boolean
    ): String {
        if (isToday) return context.getString(R.string.days_today_label)
        val locale = context.resources.configuration.locales[0]

        return when (mode) {
            DisplayModes.PAST_DAYS -> {
                val days = if (isRepeating) state.daysPassed else state.daysElapsed
                context.resources.getQuantityString(
                    R.plurals.days_elapsed_format,
                    days.toDisplayInt(),
                    days.toDisplayInt()
                )
            }
            DisplayModes.PAST_YMD -> {
                context.getString(
                    R.string.widget_past_ymd_semantic,
                    formatBetweenAsYMD(targetLocalDate, today, locale)
                )
            }
            DisplayModes.UNTIL_DAYS,
            DisplayModes.MILESTONE -> {
                val days = when {
                    mode == DisplayModes.MILESTONE -> state.nextMilestoneDays ?: 0L
                    isRepeating -> state.daysLeft
                    else -> state.daysRemaining
                }
                context.resources.getQuantityString(
                    R.plurals.notification_days_left,
                    days.toDisplayInt(),
                    days.toDisplayInt()
                )
            }
            DisplayModes.UNTIL_YMD -> {
                val days = if (isRepeating) state.daysLeft else state.daysRemaining
                context.getString(
                    R.string.widget_until_ymd_semantic,
                    formatBetweenAsYMD(today, today.plusDays(days), locale)
                )
            }
            else -> ""
        }
    }

    private fun Long.toDisplayInt(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
