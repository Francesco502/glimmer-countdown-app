package com.example.timeapk.ui.home

import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_HALF_YEARLY
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

    @Test
    fun monthHighlights_groupsBirthdayAnniversaryCountdownAndMilestone() {
        val date = LocalDate.of(2026, 6, 24)
        val occurrences = listOf(
            CalendarEventOccurrence(
                eventState(1, "birthday", date, REPEAT_YEARLY, category = CATEGORY_BIRTHDAY),
                date
            ),
            CalendarEventOccurrence(
                eventState(2, "anniversary", date, REPEAT_YEARLY, category = CATEGORY_ANNIVERSARY),
                date
            ),
            CalendarEventOccurrence(
                eventState(3, "countdown", date, REPEAT_NONE, category = CATEGORY_OTHER),
                date
            ),
            CalendarEventOccurrence(
                eventState(4, "milestone", date, REPEAT_NONE, category = CATEGORY_OTHER, nextMilestoneDays = 3),
                date
            )
        )

        val highlights = monthHighlightsForOccurrences(occurrences)

        assertEquals(1, highlights.birthdays.totalCount)
        assertEquals(1, highlights.anniversaries.totalCount)
        assertEquals(2, highlights.countdowns.totalCount)
        assertEquals(1, highlights.milestones.totalCount)
        assertEquals(listOf(1), highlights.birthdays.items.map { it.eventState.event.id })
        assertEquals(listOf(2), highlights.anniversaries.items.map { it.eventState.event.id })
        assertEquals(listOf(3, 4), highlights.countdowns.items.map { it.eventState.event.id })
        assertEquals(listOf(4), highlights.milestones.items.map { it.eventState.event.id })
    }

    @Test
    fun monthHighlights_usesFullOccurrenceSetWithoutHomeWindowLimit() {
        val month = YearMonth.of(2026, 6)
        val occurrences = calendarOccurrencesForMonth(
            events = (1..180).map { id ->
                eventState(
                    id = id,
                    title = "event-$id",
                    date = LocalDate.of(2026, 6, (id % 28) + 1),
                    repeatType = REPEAT_NONE
                )
            },
            month = month
        )

        val highlights = monthHighlightsForOccurrences(occurrences, maxItemsPerGroup = 3)

        assertEquals(180, highlights.countdowns.totalCount)
        assertEquals(3, highlights.countdowns.items.size)
    }

    @Test
    fun monthHighlights_respectsFilteredOccurrences() {
        val date = LocalDate.of(2026, 6, 24)
        val allOccurrences = listOf(
            CalendarEventOccurrence(
                eventState(1, "birthday", date, REPEAT_YEARLY, category = CATEGORY_BIRTHDAY),
                date
            ),
            CalendarEventOccurrence(
                eventState(2, "anniversary", date, REPEAT_YEARLY, category = CATEGORY_ANNIVERSARY),
                date
            )
        )
        val birthdayOnly = allOccurrences.filter { it.eventState.event.category == CATEGORY_BIRTHDAY }

        val highlights = monthHighlightsForOccurrences(birthdayOnly)

        assertEquals(1, highlights.birthdays.totalCount)
        assertEquals(0, highlights.anniversaries.totalCount)
    }

    @Test
    fun monthHighlights_expandsWeeklyMonthlyHalfYearlyYearlyAndLunarEvents() {
        val month = YearMonth.of(2026, 6)
        val lunarOrigin = solarDateForLunarDate(2025, 5, 1)
        val lunarExpected = solarDateForLunarDate(2026, 5, 1)
        val occurrences = calendarOccurrencesForMonth(
            events = listOf(
                eventState(1, "weekly", LocalDate.of(2026, 6, 1), REPEAT_WEEKLY),
                eventState(2, "monthly", LocalDate.of(2026, 1, 30), REPEAT_MONTHLY),
                eventState(3, "half yearly", LocalDate.of(2025, 12, 15), REPEAT_HALF_YEARLY),
                eventState(4, "yearly", LocalDate.of(2020, 6, 24), REPEAT_YEARLY, category = CATEGORY_ANNIVERSARY),
                eventState(5, "lunar", lunarOrigin, REPEAT_YEARLY, category = CATEGORY_BIRTHDAY, isLunar = true)
            ),
            month = month
        )

        val highlights = monthHighlightsForOccurrences(occurrences, maxItemsPerGroup = 20)

        assertEquals(9, occurrences.size)
        assertEquals(1, highlights.birthdays.totalCount)
        assertEquals(1, highlights.anniversaries.totalCount)
        assertEquals(7, highlights.countdowns.totalCount)
        assertEquals(lunarExpected, highlights.birthdays.items.single().date)
    }

    private fun eventState(
        id: Int,
        title: String,
        date: LocalDate,
        repeatType: String,
        category: String = CATEGORY_OTHER,
        isLunar: Boolean = false,
        nextMilestoneDays: Long? = null
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
            nextMilestoneDays = nextMilestoneDays,
            nextMilestoneValue = nextMilestoneDays?.let { 100L },
            nextOccurrenceDate = date
        )
    }

    private fun solarDateForLunarDate(year: Int, month: Int, day: Int): LocalDate {
        val solar = Lunar.fromYmd(year, month, day).solar
        return LocalDate.of(solar.year, solar.month, solar.day)
    }
}
