package com.example.timeapk.ui.home

import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.REPEAT_DAILY
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_WEEKLY
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.ui.theme.SongLunarCellKind
import com.example.timeapk.ui.utils.buildLunarSolarDateForYear
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.nextOccurrenceDate
import com.example.timeapk.ui.utils.safeWithYear
import com.nlf.calendar.Solar
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class CalendarEventOccurrence(
    val eventState: EventUiState,
    val date: LocalDate
)

data class CalendarDayCellContent(
    val dayText: String,
    val eventIndicatorText: String?
)

data class MonthHighlightSummary(
    val birthdays: MonthHighlightGroup,
    val anniversaries: MonthHighlightGroup,
    val countdowns: MonthHighlightGroup,
    val milestones: MonthHighlightGroup
)

data class MonthHighlightGroup(
    val totalCount: Int,
    val items: List<CalendarEventOccurrence>
)

fun monthHighlightsForOccurrences(
    occurrences: List<CalendarEventOccurrence>,
    maxItemsPerGroup: Int = 3
): MonthHighlightSummary {
    val ordered = occurrences.sortedWith(
        compareBy<CalendarEventOccurrence> { it.date }
            .thenBy { it.eventState.event.id }
    )
    val birthdays = ordered.filter { it.eventState.event.category == CATEGORY_BIRTHDAY }
    val anniversaries = ordered.filter { it.eventState.event.category == CATEGORY_ANNIVERSARY }
    val countdowns = ordered.filter { it.eventState.event.category !in setOf(CATEGORY_BIRTHDAY, CATEGORY_ANNIVERSARY) }
    val milestones = ordered.filter { (it.eventState.nextMilestoneDays ?: Long.MAX_VALUE) >= 0L && it.eventState.nextMilestoneDays != null }

    return MonthHighlightSummary(
        birthdays = birthdays.toHighlightGroup(maxItemsPerGroup),
        anniversaries = anniversaries.toHighlightGroup(maxItemsPerGroup),
        countdowns = countdowns.toHighlightGroup(maxItemsPerGroup),
        milestones = milestones.toHighlightGroup(maxItemsPerGroup)
    )
}

fun calendarDayCellContent(
    date: LocalDate,
    occurrences: List<CalendarEventOccurrence>
): CalendarDayCellContent {
    return CalendarDayCellContent(
        dayText = date.dayOfMonth.toString(),
        eventIndicatorText = (occurrences.size - CALENDAR_CELL_MAX_MARKERS)
            .takeIf { it > 0 }?.let { "+$it" }
    )
}

private fun List<CalendarEventOccurrence>.toHighlightGroup(maxItems: Int): MonthHighlightGroup {
    return MonthHighlightGroup(
        totalCount = size,
        items = take(maxItems.coerceAtLeast(0))
    )
}

fun calendarOccurrencesForMonth(
    events: List<EventUiState>,
    month: YearMonth
): List<CalendarEventOccurrence> {
    val monthStart = month.atDay(1)
    val monthEnd = month.atEndOfMonth()
    return events.flatMap { state ->
        eventOccurrencesInRange(state, monthStart, monthEnd)
    }
}

private fun eventOccurrencesInRange(
    eventState: EventUiState,
    start: LocalDate,
    end: LocalDate
): List<CalendarEventOccurrence> {
    val origin = eventDateToLocalDate(eventState.event.date)
    if (eventState.event.isArchived || origin.isAfter(end)) return emptyList()

    return when (eventState.event.repeatType) {
        REPEAT_NONE -> if (origin in start..end) {
            listOf(CalendarEventOccurrence(eventState, origin))
        } else {
            emptyList()
        }

        REPEAT_DAILY -> {
            val first = maxOf(origin, start)
            generateSequence(first) { it.plusDays(1) }
                .takeWhile { !it.isAfter(end) }
                .map { CalendarEventOccurrence(eventState, it) }
                .toList()
        }

        REPEAT_WEEKLY -> {
            val daysBetween = ChronoUnit.DAYS.between(origin, start).coerceAtLeast(0)
            val weeksToStart = daysBetween / 7
            var first = origin.plusWeeks(weeksToStart)
            while (first.isBefore(start)) {
                first = first.plusWeeks(1)
            }
            generateSequence(first) { it.plusWeeks(1) }
                .takeWhile { !it.isAfter(end) }
                .map { CalendarEventOccurrence(eventState, it) }
                .toList()
        }

        REPEAT_MONTHLY -> {
            if (!eventState.event.isLunar) {
                fixedMonthIntervalOccurrences(eventState, origin, start, end, intervalMonths = 1)
            } else {
                generateSequence(nextOccurrenceDate(origin, start, REPEAT_MONTHLY, isLunar = true)) { previous ->
                    nextOccurrenceDate(origin, previous.plusDays(1), REPEAT_MONTHLY, isLunar = true)
                }.takeWhile { !it.isAfter(end) }.map { CalendarEventOccurrence(eventState, it) }.toList()
            }
        }

        REPEAT_HALF_YEARLY -> {
            fixedMonthIntervalOccurrences(eventState, origin, start, end, intervalMonths = 6)
        }

        REPEAT_YEARLY -> {
            if (eventState.event.isLunar) {
                lunarYearlyOccurrencesInRange(eventState, origin, start, end)
            } else {
                val candidate = safeWithYear(origin, start.year)
                val occurrence = if (candidate != null && candidate.isBefore(start)) {
                    safeWithYear(origin, start.year + 1)
                } else {
                    candidate
                }
                if (occurrence != null && occurrence in start..end) {
                    listOf(CalendarEventOccurrence(eventState, occurrence))
                } else {
                    emptyList()
                }
            }
        }

        else -> emptyList()
    }
}

private fun fixedMonthIntervalOccurrences(
    eventState: EventUiState,
    origin: LocalDate,
    start: LocalDate,
    end: LocalDate,
    intervalMonths: Long
): List<CalendarEventOccurrence> {
    val monthsBetween = ChronoUnit.MONTHS.between(origin, start).coerceAtLeast(0)
    var intervalsToStart = monthsBetween / intervalMonths
    var first = origin.plusMonths(intervalsToStart * intervalMonths)
    while (first.isBefore(start)) {
        intervalsToStart += 1
        first = origin.plusMonths(intervalsToStart * intervalMonths)
    }
    return generateSequence(intervalsToStart) { it + 1 }
        .map { interval -> origin.plusMonths(interval * intervalMonths) }
        .takeWhile { !it.isAfter(end) }
        .map { CalendarEventOccurrence(eventState, it) }
        .toList()
}

private fun lunarYearlyOccurrencesInRange(
    eventState: EventUiState,
    origin: LocalDate,
    start: LocalDate,
    end: LocalDate
): List<CalendarEventOccurrence> {
    return try {
        val originLunar = Solar.fromYmd(
            origin.year,
            origin.monthValue,
            origin.dayOfMonth
        ).lunar
        val startLunarYear = Solar.fromYmd(
            start.year,
            start.monthValue,
            start.dayOfMonth
        ).lunar.year
        val endLunarYear = Solar.fromYmd(
            end.year,
            end.monthValue,
            end.dayOfMonth
        ).lunar.year

        (startLunarYear - 1..endLunarYear + 1)
            .asSequence()
            .mapNotNull { lunarYear ->
                buildLunarSolarDateForYear(lunarYear, originLunar.month, originLunar.day)
            }
            .filter { occurrence -> !occurrence.isBefore(origin) && occurrence in start..end }
            .distinct()
            .sorted()
            .map { occurrence -> CalendarEventOccurrence(eventState, occurrence) }
            .toList()
    } catch (_: Throwable) {
        val candidate = safeWithYear(origin, start.year)
        val occurrence = if (candidate != null && candidate.isBefore(start)) {
            safeWithYear(origin, start.year + 1)
        } else {
            candidate
        }
        if (occurrence != null && occurrence in start..end) {
            listOf(CalendarEventOccurrence(eventState, occurrence))
        } else {
            emptyList()
        }
    }
}

internal data class CalendarLunarLabel(val text: String, val kind: SongLunarCellKind)

internal fun calendarLunarMonthTitle(month: YearMonth): String? = try {
    val lunar = Solar.fromYmd(month.year, month.monthValue, 1).lunar
    "${lunar.yearInGanZhi}年 · ${lunar.monthInChinese}月"
} catch (_: Exception) {
    null
}

internal fun calendarLunarCellLabel(date: LocalDate): CalendarLunarLabel? = try {
    val solar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth)
    val lunar = solar.lunar
    val lunarFestival = lunar.festivals.firstOrNull()
    val jieQi = lunar.jieQi.takeIf { it.isNotBlank() }
    val solarFestival = solar.festivals.firstOrNull()
    when {
        lunarFestival != null -> CalendarLunarLabel(lunarFestival, SongLunarCellKind.Festival)
        jieQi != null -> CalendarLunarLabel(jieQi, SongLunarCellKind.SolarTerm)
        solarFestival != null -> CalendarLunarLabel(solarFestival, SongLunarCellKind.Festival)
        lunar.day == 1 -> CalendarLunarLabel(lunar.monthInChinese + "月", SongLunarCellKind.MonthStart)
        else -> CalendarLunarLabel(lunar.dayInChinese, SongLunarCellKind.Day)
    }
} catch (_: Exception) {
    null
}

private const val CALENDAR_CELL_MAX_MARKERS = 3

internal fun calendarCellBarCategories(occurrences: List<CalendarEventOccurrence>): List<String> =
    occurrences.take(CALENDAR_CELL_MAX_MARKERS).map { it.eventState.event.category }
