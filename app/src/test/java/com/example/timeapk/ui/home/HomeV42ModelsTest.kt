package com.example.timeapk.ui.home

import com.example.timeapk.ui.theme.SongLunarCellKind

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
    }

    @Test
    fun lunarCellLabel_prefersFestivalsSolarTermsAndMonthStarts() {
        assertEquals(
            CalendarLunarLabel("春节", SongLunarCellKind.Festival),
            calendarLunarCellLabel(LocalDate.of(2026, 2, 17))
        )
        assertEquals(
            CalendarLunarLabel("除夕", SongLunarCellKind.Festival),
            calendarLunarCellLabel(LocalDate.of(2026, 2, 16))
        )
        assertEquals(
            CalendarLunarLabel("清明", SongLunarCellKind.SolarTerm),
            calendarLunarCellLabel(LocalDate.of(2026, 4, 5))
        )
        assertEquals(
            CalendarLunarLabel("九月", SongLunarCellKind.MonthStart),
            calendarLunarCellLabel(LocalDate.of(2026, 10, 10))
        )
        assertEquals(
            CalendarLunarLabel("廿九", SongLunarCellKind.Day),
            calendarLunarCellLabel(LocalDate.of(2026, 10, 9))
        )
        assertEquals("丙午年 · 九月", calendarLunarMonthTitle(java.time.YearMonth.of(2026, 11)))
    }

    @Test
    fun calendarMarkersCapAtThreeAndReportOverflow() {
        val occurrences = (1..5).map { CalendarEventOccurrence(state(it, today), today) }
        assertEquals(3, calendarCellBarCategories(occurrences).size)
        assertEquals("+2", calendarDayCellContent(today, occurrences).eventIndicatorText)
    }
}
