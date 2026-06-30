package com.example.timeapk.ui.home

import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_WEEKLY
import com.example.timeapk.data.REPEAT_YEARLY
import com.nlf.calendar.Lunar
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class HomeCalendarModelTest {

    @Test
    fun calendarOccurrencesForMonth_includesOnlyOneTimeEventsInsideMonth() {
        val month = YearMonth.of(2026, 6)
        val events = listOf(
            eventState(1, "inside", LocalDate.of(2026, 6, 24), REPEAT_NONE),
            eventState(2, "outside", LocalDate.of(2026, 7, 1), REPEAT_NONE)
        )

        val occurrences = calendarOccurrencesForMonth(events, month)

        assertEquals(listOf(1), occurrences.map { it.eventState.event.id })
        assertEquals(listOf(LocalDate.of(2026, 6, 24)), occurrences.map { it.date })
    }

    @Test
    fun calendarOccurrencesForMonth_expandsWeeklyEventsAcrossVisibleMonth() {
        val month = YearMonth.of(2026, 6)
        val events = listOf(
            eventState(1, "weekly", LocalDate.of(2026, 6, 1), REPEAT_WEEKLY)
        )

        val occurrences = calendarOccurrencesForMonth(events, month)

        assertEquals(
            listOf(
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 8),
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 6, 22),
                LocalDate.of(2026, 6, 29)
            ),
            occurrences.map { it.date }
        )
    }

    @Test
    fun calendarOccurrencesForMonth_expandsMonthlyEndOfMonthEvents() {
        val month = YearMonth.of(2026, 2)
        val events = listOf(
            eventState(1, "monthly", LocalDate.of(2026, 1, 31), REPEAT_MONTHLY)
        )

        val occurrences = calendarOccurrencesForMonth(events, month)

        assertEquals(listOf(LocalDate.of(2026, 2, 28)), occurrences.map { it.date })
    }

    @Test
    fun calendarOccurrencesForMonth_keepsOriginalDayForMonthlyEndOfMonthAfterClampedMonth() {
        val month = YearMonth.of(2026, 3)
        val events = listOf(
            eventState(1, "monthly", LocalDate.of(2026, 1, 31), REPEAT_MONTHLY)
        )

        val occurrences = calendarOccurrencesForMonth(events, month)

        assertEquals(listOf(LocalDate.of(2026, 3, 31)), occurrences.map { it.date })
    }

    @Test
    fun calendarOccurrencesForMonth_expandsYearlyEventsInFutureYears() {
        val month = YearMonth.of(2026, 6)
        val events = listOf(
            eventState(
                id = 1,
                title = "anniversary",
                date = LocalDate.of(2020, 6, 24),
                repeatType = REPEAT_YEARLY,
                category = CATEGORY_ANNIVERSARY
            )
        )

        val occurrences = calendarOccurrencesForMonth(events, month)

        assertEquals(listOf(LocalDate.of(2026, 6, 24)), occurrences.map { it.date })
    }

    @Test
    fun calendarOccurrencesForMonth_expandsLunarYearlyEventsByLunarDate() {
        val origin = solarDateForLunarDate(2024, 1, 1)
        val expected = solarDateForLunarDate(2025, 1, 1)
        val events = listOf(
            eventState(
                id = 1,
                title = "lunar birthday",
                date = origin,
                repeatType = REPEAT_YEARLY,
                category = CATEGORY_ANNIVERSARY,
                isLunar = true
            )
        )

        val occurrences = calendarOccurrencesForMonth(events, YearMonth.from(expected))

        assertEquals(listOf(expected), occurrences.map { it.date })
    }

    @Test
    fun calendarOccurrencesForMonth_doesNotApplyHomeListWindowLimit() {
        val month = YearMonth.of(2026, 6)
        val events = (1..180).map { id ->
            eventState(
                id = id,
                title = "event-$id",
                date = LocalDate.of(2026, 6, (id % 28) + 1),
                repeatType = REPEAT_NONE
            )
        }

        val occurrences = calendarOccurrencesForMonth(events, month)

        assertEquals(180, occurrences.size)
        assertEquals((1..180).toList(), occurrences.map { it.eventState.event.id })
    }

    @Test
    fun calendarDayCellContent_keepsCompactCellsFreeOfLunarText() {
        val content = calendarDayCellContent(
            date = LocalDate.of(2026, 5, 20),
            occurrences = emptyList()
        )

        assertEquals("20", content.dayText)
        assertEquals(null, content.eventIndicatorText)
    }

    @Test
    fun calendarDayCellContent_usesShortCountInsteadOfEventTitleForCrowdedCells() {
        val date = LocalDate.of(2026, 6, 24)
        val occurrences = listOf(
            CalendarEventOccurrence(
                eventState(1, "A very long birthday title", date, REPEAT_NONE),
                date
            ),
            CalendarEventOccurrence(
                eventState(2, "Another long event title", date, REPEAT_NONE),
                date
            )
        )

        val content = calendarDayCellContent(date, occurrences)

        assertEquals("2", content.eventIndicatorText)
    }

    private fun eventState(
        id: Int,
        title: String,
        date: LocalDate,
        repeatType: String,
        category: String = CATEGORY_OTHER,
        isLunar: Boolean = false
    ): EventUiState {
        val event = Event(
            id = id,
            title = title,
            date = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            category = category,
            repeatType = repeatType,
            isLunar = isLunar,
            createdAt = id.toLong()
        )
        return EventUiState(
            event = event,
            daysRemaining = 0,
            isPast = false,
            nextOccurrenceDate = date
        )
    }

    private fun solarDateForLunarDate(year: Int, month: Int, day: Int): LocalDate {
        val solar = Lunar.fromYmd(year, month, day).solar
        return LocalDate.of(solar.year, solar.month, solar.day)
    }
}
