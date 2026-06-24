package com.example.timeapk.ui.home

import com.example.timeapk.data.REPEAT_DAILY
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_WEEKLY
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.ui.utils.buildLunarSolarDateForYear
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.safeWithYear
import com.nlf.calendar.Solar
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class CalendarEventOccurrence(
    val eventState: EventUiState,
    val date: LocalDate
)

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
    if (origin.isAfter(end)) return emptyList()

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
            fixedMonthIntervalOccurrences(eventState, origin, start, end, intervalMonths = 1)
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
