package com.example.timeapk.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.ui.home.EventUiState
import com.example.timeapk.ui.home.toEventUiState
import com.example.timeapk.ui.utils.DisplayModes
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.formatBetweenAsYMD
import com.example.timeapk.ui.utils.formatDays
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import kotlin.math.roundToInt

class CountdownWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CountdownRemoteViewsFactory(applicationContext, intent)
    }
}

class CountdownRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId: Int =
        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

    private val widgetSizeBucket: Int = intent.getIntExtra(
        WidgetSizeBucket.EXTRA_SIZE_BUCKET,
        WidgetSizeBucket.MEDIUM
    )

    private val items = mutableListOf<EventUiState>()

    private var dateDeltaDisplayMode: Int = 0
    private var perEventDateDeltaModes: Map<Int, Int> = emptyMap()
    private var widgetFontScale: Float = 1f

    override fun onCreate() {
        // no-op
    }

    override fun onDestroy() {
        items.clear()
    }

    override fun onDataSetChanged() {
        val identityToken = Binder.clearCallingIdentity()
        try {
            val app = context.applicationContext as TimeApplication
            val list = runBlocking {
                val milestones = app.userPrefs.customMilestonesFlow.first()
                val pinnedEventIds = app.userPrefs.pinnedEventIdsFlow.first()
                dateDeltaDisplayMode = app.userPrefs.dateDeltaDisplayModeFlow.first()
                perEventDateDeltaModes = app.userPrefs.perEventDateDeltaDisplayModesFlow.first()
                widgetFontScale = app.userPrefs.widgetFontScaleFlow.first()

                val all = app.repository.getAllEventsSnapshot().map { it.toEventUiState(milestones) }
                val upcoming = all.filter { !it.isPast }.sortedBy { it.daysRemaining }
                val past = all.filter { it.isPast }.sortedBy { it.daysRemaining }
                var base = upcoming + past

                if (pinnedEventIds.isNotEmpty()) {
                    val pinnedSet = pinnedEventIds.toSet()
                    val pinned = pinnedEventIds.mapNotNull { id -> base.find { it.event.id == id } }
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
        val textStyle = applySizeStyle(views)

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
            DisplayModes.MILESTONE -> if (state.isPast) DisplayModes.PAST_DAYS else DisplayModes.UNTIL_DAYS
            else -> persistedMode
        }

        val valueText = when (widgetSizeBucket) {
            WidgetSizeBucket.SMALL -> buildAdaptiveSmallValueText(
                state = state,
                mode = mode,
                targetLocalDate = targetLocalDate,
                today = today,
                isRepeating = isRepeating,
                isToday = isToday,
                maxChars = textStyle.valueMaxEms
            )
            WidgetSizeBucket.LARGE -> buildVerboseValueText(state, mode, targetLocalDate, today, isRepeating, isToday)
            else -> buildCompactValueText(state, mode, targetLocalDate, today, isRepeating, isToday)
        }

        val titleText = if (state.event.isLunar) {
            context.getString(R.string.widget_lunar_prefix, state.event.title)
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

    private fun applySizeStyle(views: RemoteViews): WidgetTextStyle {
        val style = WidgetStylePolicy.resolve(widgetSizeBucket, widgetFontScale)
        views.setTextViewTextSize(R.id.widget_item_title, TypedValue.COMPLEX_UNIT_SP, style.titleSp)
        views.setTextViewTextSize(R.id.widget_item_value, TypedValue.COMPLEX_UNIT_SP, style.valueSp)
        views.setViewPadding(
            R.id.widget_item_root,
            dp(style.paddingHorizontalDp),
            dp(style.paddingVerticalDp),
            dp(style.paddingHorizontalDp),
            dp(style.paddingVerticalDp)
        )
        views.setInt(R.id.widget_item_value, "setMaxEms", style.valueMaxEms)
        return style
    }

    private fun buildAdaptiveSmallValueText(
        state: EventUiState,
        mode: Int,
        targetLocalDate: LocalDate,
        today: LocalDate,
        isRepeating: Boolean,
        isToday: Boolean,
        maxChars: Int
    ): String {
        val preferred = buildCompactValueText(
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
                context.getString(R.string.widget_past_days_semantic, formatDays(days))
            }
            DisplayModes.PAST_YMD -> {
                context.getString(R.string.widget_past_ymd_semantic, formatBetweenAsYMD(targetLocalDate, today, locale))
            }
            DisplayModes.UNTIL_DAYS -> {
                val days = if (isRepeating) state.daysLeft else state.daysRemaining
                context.getString(R.string.widget_until_days_semantic, formatDays(days))
            }
            DisplayModes.UNTIL_YMD -> {
                val days = if (isRepeating) state.daysLeft else state.daysRemaining
                context.getString(R.string.widget_until_ymd_semantic, formatBetweenAsYMD(today, today.plusDays(days), locale))
            }
            else -> ""
        }
    }

    private fun buildVerboseValueText(
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
                context.getString(R.string.widget_past_ymd_semantic, formatBetweenAsYMD(targetLocalDate, today, locale))
            }
            DisplayModes.UNTIL_DAYS -> {
                val days = if (isRepeating) state.daysLeft else state.daysRemaining
                context.resources.getQuantityString(
                    R.plurals.notification_days_left,
                    days.toDisplayInt(),
                    days.toDisplayInt()
                )
            }
            DisplayModes.UNTIL_YMD -> {
                val days = if (isRepeating) state.daysLeft else state.daysRemaining
                context.getString(R.string.widget_until_ymd_semantic, formatBetweenAsYMD(today, today.plusDays(days), locale))
            }
            else -> ""
        }
    }

    private fun Long.toDisplayInt(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    private fun dp(value: Int): Int {
        val density = context.resources.displayMetrics.density
        return (value * density).roundToInt()
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long =
        items.getOrNull(position)?.event?.id?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}



