package com.example.timeapk.widget

import android.content.Context
import android.appwidget.AppWidgetManager
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.ui.home.EventUiState
import com.example.timeapk.ui.home.SortType
import com.example.timeapk.ui.home.applyHomeSort
import com.example.timeapk.ui.home.toEventUiState
import com.example.timeapk.ui.utils.DisplayModes
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.formatBetweenAsYMD
import com.example.timeapk.ui.utils.formatDays
import com.example.timeapk.ui.utils.getAvailableDisplayModes
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId

internal data class WidgetRenderedItem(
    val eventId: Int,
    val title: String,
    val value: String,
    val fullValue: String = value,
    val contentDescription: String = "$title, $fullValue",
    val compactValue: String = value,
    val shortValue: String = value
)

internal data class WidgetContentSnapshot(
    val items: List<WidgetRenderedItem>,
    val textStyle: WidgetTextStyle,
    val renderStyle: WidgetRenderStyle = WidgetRenderPolicy.resolve(
        WidgetConfig.default(),
        WidgetThemeSnapshot(isDark = false, usesSystemPalette = false)
    ),
    val widthDp: Int = 110
)

internal object WidgetContentResolver {
    suspend fun load(context: Context, sizeBucket: Int): WidgetContentSnapshot {
        return load(context = context, appWidgetId = null, sizeBucket = sizeBucket)
    }

    suspend fun load(
        context: Context,
        appWidgetId: Int?,
        sizeBucket: Int,
        configOverride: WidgetConfig? = null,
        previewWidthDp: Int? = null
    ): WidgetContentSnapshot {
        val app = context.applicationContext as? TimeApplication
            ?: return WidgetContentSnapshot(
                items = emptyList(),
                textStyle = WidgetStylePolicy.resolve(sizeBucket, 1f)
            )

        val prefs = app.userPrefs
        val widgetContext = widgetLanguageContext(context)
        val options = appWidgetId?.let { AppWidgetManager.getInstance(context).getAppWidgetOptions(it) }
        // Use the smaller host width so the same collection also fits after rotation.
        val widthDp = previewWidthDp ?: listOfNotNull(
            options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
            options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        ).filter { it > 0 }.minOrNull() ?: 110
        val milestones = prefs.customMilestonesFlow.first()
        val pinnedEventIds = prefs.pinnedEventIdsFlow.first()
        val customEventOrder = prefs.customEventOrderFlow.first()
        val homeSortType = SortType.entries.getOrNull(prefs.sortTypeFlow.first()) ?: SortType.Custom
        val preferredMode = prefs.dateDeltaDisplayModeFlow.first()
        val perEventModes = prefs.perEventDateDeltaDisplayModesFlow.first()
        val showMilestone = prefs.showMilestoneFlow.first()
        val smartMilestonesEnabled = prefs.smartMilestonesEnabledFlow.first()
        val widgetConfigRepository = WidgetConfigRepository(context)
        val config = (configOverride ?: if (appWidgetId != null) {
            widgetConfigRepository.getConfigForWidget(appWidgetId)
        } else {
            widgetConfigRepository.getDefaultConfig()
        }).sanitize()
        val textStyle = WidgetStylePolicy.resolve(sizeBucket, config.fontScale, config.densityMode)
        val renderStyle = WidgetRenderPolicy.resolve(config, WidgetThemeResolver.resolve(context))

        val now = Instant.now()
        val today = now.atZone(ZoneId.systemDefault()).toLocalDate()
        val ordered = app.repository.getAllEventsSnapshot()
            .map { it.toEventUiState(milestones, smartMilestonesEnabled, now) }
            .let { filterAndSortStates(it, config, pinnedEventIds, customEventOrder, homeSortType) }

        val items = ordered.map { state ->
            buildRenderedItem(
                context = widgetContext,
                state = state,
                preferredMode = perEventModes[state.event.id] ?: preferredMode,
                displayPolicy = config.displayPolicy,
                showMilestone = showMilestone,
                showLunarPrefix = config.showLunarPrefix,
                textStyle = textStyle,
                today = today
            )
        }

        return WidgetContentSnapshot(
            items = items,
            textStyle = textStyle,
            renderStyle = renderStyle,
            widthDp = widthDp
        )
    }

    internal fun resolveDisplayMode(
        state: EventUiState,
        preferredMode: Int,
        showMilestone: Boolean,
        displayPolicy: Int = DISPLAY_POLICY_AUTO
    ): Int {
        if (displayPolicy == DISPLAY_POLICY_COUNTDOWN) {
            return if (state.isPast) DisplayModes.PAST_DAYS else DisplayModes.UNTIL_DAYS
        }
        if (displayPolicy != DISPLAY_POLICY_FOLLOW_APP && state.isYearlyBirthday()) {
            return DisplayModes.UNTIL_DAYS
        }
        val availableModes = getAvailableDisplayModes(state, showMilestone)
        return if (preferredMode in availableModes) preferredMode else availableModes.first()
    }

    private fun sortForWidget(states: List<EventUiState>): List<EventUiState> {
        val upcoming = states.filter { !it.isPast }.sortedBy { it.daysRemaining }
        val past = states.filter { it.isPast }.sortedBy { it.daysRemaining }
        return upcoming + past
    }

    internal fun filterAndSortStates(
        states: List<EventUiState>,
        config: WidgetConfig,
        pinnedEventIds: List<Int>,
        customEventOrder: List<Int>,
        homeSortType: SortType
    ): List<EventUiState> {
        val pinnedSet = pinnedEventIds.toSet()
        val filtered = when (config.contentScope) {
            CONTENT_PINNED -> states.filter { it.event.id in pinnedSet }
            CONTENT_FUTURE -> states.filter { !it.isPast }
            CONTENT_BIRTHDAY -> states.filter { it.event.category == CATEGORY_BIRTHDAY }
            else -> states
        }
        return when (config.sortMode) {
            SORT_HOME -> applyHomeSort(
                list = filtered,
                sortType = homeSortType,
                customEventOrderIds = customEventOrder,
                pinnedEventIds = pinnedEventIds
            )
            SORT_PINNED_FIRST -> applyPinnedOrder(sortForWidget(filtered), pinnedEventIds)
            SORT_NEAREST_FIRST -> sortForWidget(filtered)
            else -> applyHomeSort(
                list = filtered,
                sortType = homeSortType,
                customEventOrderIds = customEventOrder,
                pinnedEventIds = pinnedEventIds
            )
        }
    }

    private fun applyPinnedOrder(
        states: List<EventUiState>,
        pinnedEventIds: List<Int>
    ): List<EventUiState> {
        if (pinnedEventIds.isEmpty()) return states
        val normalizedPinnedEventIds = pinnedEventIds.distinct()
        val pinnedSet = normalizedPinnedEventIds.toSet()
        val pinned = normalizedPinnedEventIds.mapNotNull { id -> states.find { it.event.id == id } }
        val unpinned = states.filter { it.event.id !in pinnedSet }
        return pinned + unpinned
    }

    internal fun buildRenderedItem(
        context: Context,
        state: EventUiState,
        preferredMode: Int,
        displayPolicy: Int,
        showMilestone: Boolean,
        showLunarPrefix: Boolean,
        textStyle: WidgetTextStyle,
        today: LocalDate
    ): WidgetRenderedItem {
        val targetLocalDate = eventDateToLocalDate(state.event.date)
        val isRepeating = state.event.repeatType != REPEAT_NONE
        val isToday = state.daysRemaining == 0L && !state.isPast
        val resolvedMode = resolveDisplayMode(state, preferredMode, showMilestone, displayPolicy)
        val fullValueText = buildVerboseValueText(
            context, state, resolvedMode, targetLocalDate, today, isRepeating, isToday
        )
        val compactValueText = buildCompactValueText(
            context, state, resolvedMode, targetLocalDate, today, isRepeating, isToday
        )
        val shortestValueText = buildShortValueText(context, state, resolvedMode, isRepeating, isToday)
        val valueText = WidgetValueFormatter.semanticValueOrFallback(
            fullValueText,
            WidgetValueFormatter.semanticValueOrFallback(compactValueText, shortestValueText, textStyle.valueMaxEms),
            textStyle.valueMaxEms
        )

        val titleText = if (state.event.isLunar && showLunarPrefix) {
            context.getString(R.string.widget_lunar_prefix, state.event.title)
        } else {
            state.event.title
        }

        return WidgetRenderedItem(
            eventId = state.event.id,
            title = titleText,
            value = valueText,
            fullValue = fullValueText,
            compactValue = compactValueText,
            shortValue = shortestValueText
        )
    }

    private fun buildShortValueText(
        context: Context,
        state: EventUiState,
        mode: Int,
        isRepeating: Boolean,
        isToday: Boolean
    ): String {
        if (isToday) return context.getString(
            if (state.event.category == CATEGORY_BIRTHDAY) R.string.widget_birthday_today else R.string.widget_today_compact
        )
        val isPastMode = mode == DisplayModes.PAST_DAYS || mode == DisplayModes.PAST_YMD
        val days = when {
            mode == DisplayModes.MILESTONE -> state.nextMilestoneDays ?: 0L
            isPastMode -> if (isRepeating) state.daysPassed else state.daysElapsed
            isRepeating -> state.daysLeft
            else -> state.daysRemaining
        }
        val textRes = when {
            isPastMode -> R.string.widget_past_days_short
            else -> R.string.widget_until_days_short
        }
        return context.getString(textRes, WidgetValueFormatter.compactDayCount(days))
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
        if (isToday) return context.getString(
            if (state.event.category == CATEGORY_BIRTHDAY) R.string.widget_birthday_today else R.string.widget_today_compact
        )
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
                context.getString(
                    if (state.isYearlyBirthday()) R.string.widget_birthday_until_compact else R.string.widget_until_days_semantic,
                    formatDays(days, locale)
                )
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
        if (isToday) return context.getString(
            if (state.event.category == CATEGORY_BIRTHDAY) R.string.widget_birthday_today else R.string.days_today_label
        )
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
                if (state.isYearlyBirthday() && mode == DisplayModes.UNTIL_DAYS) {
                    context.resources.getQuantityString(
                        R.plurals.widget_birthday_until_days, days.toDisplayInt(), formatDays(days, locale)
                    )
                } else {
                    context.resources.getQuantityString(
                        R.plurals.notification_days_left, days.toDisplayInt(), days.toDisplayInt()
                    )
                }
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

    private fun EventUiState.isYearlyBirthday(): Boolean =
        event.category == CATEGORY_BIRTHDAY && event.repeatType == REPEAT_YEARLY
}
