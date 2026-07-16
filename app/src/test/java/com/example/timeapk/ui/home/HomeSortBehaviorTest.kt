package com.example.timeapk.ui.home

import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class HomeSortBehaviorTest {

    @Test
    fun applyHomeSort_customSort_usesCustomOrder_andPinnedStillWins() {
        val items = listOf(
            eventState(id = 1, daysRemaining = 10, createdAt = 100L),
            eventState(id = 2, daysRemaining = 5, createdAt = 300L),
            eventState(id = 3, daysRemaining = 1, createdAt = 200L)
        )

        val sorted = applyHomeSort(
            list = items,
            sortType = SortType.Custom,
            customEventOrderIds = listOf(3, 1, 2),
            pinnedEventIds = listOf(2)
        )

        assertEquals(listOf(2, 3, 1), sorted.map { it.event.id })
    }

    @Test
    fun applyHomeSort_daysSort_ignoresCustomOrder_andPinnedStillWins() {
        val items = listOf(
            eventState(id = 1, daysRemaining = 10, createdAt = 100L),
            eventState(id = 2, daysRemaining = 5, createdAt = 300L),
            eventState(id = 3, daysRemaining = 1, createdAt = 200L)
        )

        val sorted = applyHomeSort(
            list = items,
            sortType = SortType.ByDays,
            customEventOrderIds = listOf(3, 1, 2),
            pinnedEventIds = listOf(1)
        )

        assertEquals(listOf(1, 3, 2), sorted.map { it.event.id })
    }

    @Test
    fun applyHomeSort_dateSort_ignoresCustomOrder_andPinnedStillWins() {
        val items = listOf(
            eventState(id = 1, daysRemaining = 10, createdAt = 100L, date = LocalDate.of(2026, 3, 30)),
            eventState(id = 2, daysRemaining = 5, createdAt = 300L, date = LocalDate.of(2026, 3, 20)),
            eventState(id = 3, daysRemaining = 1, createdAt = 200L, date = LocalDate.of(2026, 3, 10))
        )

        val sorted = applyHomeSort(
            list = items,
            sortType = SortType.ByDate,
            customEventOrderIds = listOf(3, 1, 2),
            pinnedEventIds = listOf(2)
        )

        assertEquals(listOf(2, 3, 1), sorted.map { it.event.id })
    }

    @Test
    fun buildHomeVisibleList_searchesEventsBeyondFormerCaps() {
        val items = (1..151).map { id ->
            eventState(
                id = id,
                daysRemaining = id.toLong(),
                createdAt = id.toLong()
            ).let { state ->
                if (id == 151) {
                    state.copy(event = state.event.copy(title = "needle"))
                } else {
                    state
                }
            }
        }

        val result = buildHomeVisibleList(
            all = items,
            filterType = FilterType.All,
            sortType = SortType.ByDays,
            query = "needle",
            customEventOrderIds = emptyList(),
            pinnedEventIds = emptyList()
        )

        assertEquals(listOf(151), result.map { it.event.id })
    }

    @Test
    fun buildHomeVisibleList_withoutSearchReturnsEveryEvent() {
        val items = (1..175).map { id ->
            eventState(id, id.toLong(), id.toLong())
        }

        assertEquals(
            175,
            buildHomeVisibleList(
                all = items,
                filterType = FilterType.All,
                sortType = SortType.ByDays,
                query = "",
                customEventOrderIds = emptyList(),
                pinnedEventIds = emptyList()
            ).size
        )
    }

    private fun eventState(
        id: Int,
        daysRemaining: Long,
        createdAt: Long,
        date: LocalDate = LocalDate.of(2026, 3, 1)
    ): EventUiState {
        val event = Event(
            id = id,
            title = "event-$id",
            date = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            category = CATEGORY_OTHER,
            createdAt = createdAt
        )
        return EventUiState(
            event = event,
            daysRemaining = daysRemaining,
            isPast = false,
            nextOccurrenceDate = date
        )
    }
}
