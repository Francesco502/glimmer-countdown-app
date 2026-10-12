package com.example.timeapk.ui.home

import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HomeV42ModelsTest {
    private val today = LocalDate.of(2026, 10, 12)

    private fun state(id: Int, next: LocalDate, past: Boolean = false, category: String = CATEGORY_OTHER) =
        EventUiState(
            event = Event(id = id, title = "e$id", date = 0L, category = category),
            daysRemaining = 0,
            isPast = past,
            nextOccurrenceDate = next
        )

    @Test
    fun nearestEvent_prefersEarliestFutureDateThenPinned() {
        val events = listOf(
            state(1, today.plusDays(5)),
            state(2, today.plusDays(2)),
            state(3, today.plusDays(2)),
            state(4, today.minusDays(1), past = true)
        )
        assertEquals(2, pickNearestUpcomingEvent(events, today)?.event?.id)
        assertEquals(3, pickNearestUpcomingEvent(events, today, pinnedEventIds = listOf(3))?.event?.id)
    }

    @Test
    fun nearestEvent_includesTodayAndIgnoresPast() {
        assertEquals(7, pickNearestUpcomingEvent(listOf(state(7, today)), today)?.event?.id)
        assertNull(pickNearestUpcomingEvent(listOf(state(8, today.minusDays(3), past = true)), today))
    }

    @Test
    fun categoryFilter_matchesExactIdAndNullMeansAll() {
        val birthday = state(1, today, category = CATEGORY_BIRTHDAY)
        val custom = state(2, today, category = "custom_trip")
        assertTrue(birthday.matchesCategoryFilter(null))
        assertTrue(custom.matchesCategoryFilter("custom_trip"))
        assertFalse(birthday.matchesCategoryFilter("custom_trip"))
        assertEquals(CATEGORY_BIRTHDAY, FilterType.Birthday.toCategoryId())
        assertNull(FilterType.All.toCategoryId())
    }

    @Test
    fun visibleList_searchMatchesCustomCategoryLabel() {
        val custom = state(2, today, category = "custom_trip")
        val other = state(3, today)
        val result = buildHomeVisibleList(
            all = listOf(custom, other),
            categoryFilter = null,
            sortType = SortType.ByDate,
            query = "旅行",
            customEventOrderIds = emptyList(),
            pinnedEventIds = emptyList(),
            categoryLabels = mapOf("custom_trip" to "旅行")
        )
        assertEquals(listOf(2), result.map { it.event.id })
    }

    @Test
    fun longPressMenu_isOfferedOnlyOutsideCustomSort() {
        assertTrue(homeCardLongPressMenuEnabled(SortType.ByDays))
        assertTrue(homeCardLongPressMenuEnabled(SortType.ByDate))
        assertFalse(homeCardLongPressMenuEnabled(SortType.Custom))
    }

    @Test
    fun lunarCellLabel_prefersFestivalsSolarTermsAndMonthStarts() {
        // 2026-02-17 is the first day of the lunar new year (春节).
        assertEquals("春节", calendarLunarCellLabel(LocalDate.of(2026, 2, 17)))
        // 2026-09-25 is the mid-autumn festival.
        assertEquals("中秋节", calendarLunarCellLabel(LocalDate.of(2026, 9, 25)))
        assertEquals("廿九", calendarLunarCellLabel(LocalDate.of(2026, 10, 9)))
        // The first day of any lunar month shows the month name instead of 初一.
        val monthStart = generateSequence(LocalDate.of(2026, 10, 1)) { it.plusDays(1) }
            .first { com.nlf.calendar.Solar.fromYmd(it.year, it.monthValue, it.dayOfMonth).lunar.day == 1 }
        val label = calendarLunarCellLabel(monthStart).orEmpty()
        assertTrue(label, label.endsWith("月") || label.length in 2..3)
    }

    @Test
    fun calendarDots_capAtThreeAndReportOverflow() {
        val occurrences = (1..5).map { CalendarEventOccurrence(state(it, today), today) }
        val (shown, overflow) = calendarCellDotCategories(occurrences)
        assertEquals(CALENDAR_CELL_MAX_DOTS, shown.size)
        assertEquals(2, overflow)
    }
}
