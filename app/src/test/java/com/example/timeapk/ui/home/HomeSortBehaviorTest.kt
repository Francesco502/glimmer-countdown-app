package com.example.timeapk.ui.home

import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.ui.utils.DisplayModes
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
    fun applyHomeSort_duplicatePinnedIdsNeverDuplicateHomeCards() {
        val items = listOf(
            eventState(id = 1, daysRemaining = 10, createdAt = 100L),
            eventState(id = 2, daysRemaining = 5, createdAt = 300L),
            eventState(id = 3, daysRemaining = 1, createdAt = 200L)
        )

        val sorted = applyHomeSort(
            list = items,
            sortType = SortType.Custom,
            customEventOrderIds = listOf(3, 1, 2),
            pinnedEventIds = listOf(2, 2, 1, 2, 1)
        )

        assertEquals(listOf(2, 1, 3), sorted.map { it.event.id })
    }

    @Test
    fun settlePersistedHomeReorder_pinnedDragUsesAuthoritativeVisibleOrderEvenWithoutFlowEmission() {
        val items = listOf(
            eventState(id = 1, daysRemaining = 10, createdAt = 100L),
            eventState(id = 2, daysRemaining = 5, createdAt = 300L),
            eventState(id = 3, daysRemaining = 1, createdAt = 200L)
        )
        val pinnedIds = listOf(2, 1)
        val visibleBeforeDrag = applyHomeSort(
            list = items,
            sortType = SortType.Custom,
            customEventOrderIds = listOf(3, 2, 1),
            pinnedEventIds = pinnedIds
        )
        val draggedVisibleIds = listOf(1, 2, 3)
        val persistedMergedIds = mergeVisibleOrderIntoGlobalOrder(
            globalIds = listOf(3, 2, 1),
            visibleIds = visibleBeforeDrag.map { it.event.id },
            reorderedVisibleIds = draggedVisibleIds
        )

        val settled = settlePersistedHomeReorder(
            displayedItems = visibleBeforeDrag,
            persistedMergedIds = persistedMergedIds,
            pinnedEventIds = pinnedIds,
            sortType = SortType.Custom
        )

        assertEquals(listOf(1, 2, 3), persistedMergedIds)
        assertEquals(listOf(2, 1, 3), visibleBeforeDrag.map { it.event.id })
        assertEquals(visibleBeforeDrag.map { it.event.id }, settled.map { it.event.id })
        assertEquals(false, draggedVisibleIds == settled.map { it.event.id })
    }

    @Test
    fun settlePersistedHomeReorder_usesLatestFilteredPinnedAndModeTarget() {
        val latestFilteredItems = listOf(
            eventState(id = 1, daysRemaining = 10, createdAt = 100L),
            eventState(id = 3, daysRemaining = 1, createdAt = 200L)
        )

        val customSettled = settlePersistedHomeReorder(
            displayedItems = latestFilteredItems,
            persistedMergedIds = listOf(3, 2, 1),
            pinnedEventIds = listOf(1),
            sortType = SortType.Custom
        )
        val modeChangedSettled = settlePersistedHomeReorder(
            displayedItems = latestFilteredItems,
            persistedMergedIds = listOf(3, 2, 1),
            pinnedEventIds = listOf(1),
            sortType = SortType.ByDays
        )
        val failureSettled = settlePersistedHomeReorder(
            displayedItems = latestFilteredItems,
            persistedMergedIds = null,
            pinnedEventIds = listOf(1),
            sortType = SortType.Custom
        )

        assertEquals(listOf(1, 3), customSettled.map { it.event.id })
        assertEquals(latestFilteredItems, modeChangedSettled)
        assertEquals(latestFilteredItems, failureSettled)
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

    @Test
    fun mergeVisibleOrderIntoGlobalOrder_preservesHiddenSlots() {
        assertEquals(
            listOf(3, 2, 1, 4, 5),
            mergeVisibleOrderIntoGlobalOrder(
                globalIds = listOf(1, 2, 3, 4, 5),
                visibleIds = listOf(1, 3, 5),
                reorderedVisibleIds = listOf(3, 1, 5)
            )
        )
    }

    @Test
    fun mergeVisibleOrderIntoGlobalOrder_appendsUnstoredIdsExactlyOnce() {
        assertEquals(
            listOf(1, 2, 3, 4),
            mergeVisibleOrderIntoGlobalOrder(
                globalIds = listOf(1, 2),
                visibleIds = listOf(1, 3, 4),
                reorderedVisibleIds = listOf(1, 3, 4)
            )
        )
    }

    @Test
    fun mergeVisibleOrderIntoGlobalOrder_invalidReorderKeepsCanonicalGlobalOrder() {
        assertEquals(
            listOf(1, 2, 3),
            mergeVisibleOrderIntoGlobalOrder(
                globalIds = listOf(1, 1, 2, 3),
                visibleIds = listOf(1, 3),
                reorderedVisibleIds = listOf(3, 3)
            )
        )
    }

    @Test
    fun defaultCustomEventOrderIds_breaksCreatedAtTiesById() {
        val events = listOf(
            eventState(id = 4, daysRemaining = 1, createdAt = 200L).event,
            eventState(id = 2, daysRemaining = 1, createdAt = 300L).event,
            eventState(id = 3, daysRemaining = 1, createdAt = 300L).event,
            eventState(id = 1, daysRemaining = 1, createdAt = 100L).event
        )

        assertEquals(listOf(2, 3, 4, 1), defaultCustomEventOrderIds(events))
    }

    @Test
    fun resolveHomeDateDeltaDisplayMode_byDaysUsesPastDaysForPastOneTimeEvent() {
        val eventState = eventState(
            id = 1,
            daysRemaining = -3,
            createdAt = 1,
            isPast = true
        )

        assertEquals(
            DisplayModes.PAST_DAYS,
            resolveHomeDateDeltaDisplayMode(
                sortType = SortType.ByDays,
                requestedMode = DisplayModes.UNTIL_YMD,
                eventState = eventState
            )
        )
    }

    @Test
    fun resolveHomeDateDeltaDisplayMode_byDaysUsesUntilDaysForUpcomingOneTimeEvent() {
        val eventState = eventState(
            id = 1,
            daysRemaining = 3,
            createdAt = 1
        )

        assertEquals(
            DisplayModes.UNTIL_DAYS,
            resolveHomeDateDeltaDisplayMode(
                sortType = SortType.ByDays,
                requestedMode = DisplayModes.PAST_YMD,
                eventState = eventState
            )
        )
    }

    @Test
    fun resolveHomeDateDeltaDisplayMode_byDaysUsesUntilDaysForRecurringEvent() {
        val eventState = eventState(
            id = 1,
            daysRemaining = 3,
            createdAt = 1,
            isPast = true,
            repeatType = REPEAT_YEARLY
        )

        assertEquals(
            DisplayModes.UNTIL_DAYS,
            resolveHomeDateDeltaDisplayMode(
                sortType = SortType.ByDays,
                requestedMode = DisplayModes.PAST_DAYS,
                eventState = eventState
            )
        )
    }

    @Test
    fun resolveHomeDateDeltaDisplayMode_nonByDaysPreservesRequestedMode() {
        val eventState = eventState(
            id = 1,
            daysRemaining = 3,
            createdAt = 1
        )

        assertEquals(
            DisplayModes.MILESTONE,
            resolveHomeDateDeltaDisplayMode(
                sortType = SortType.Custom,
                requestedMode = DisplayModes.MILESTONE,
                eventState = eventState
            )
        )
    }

    private fun eventState(
        id: Int,
        daysRemaining: Long,
        createdAt: Long,
        date: LocalDate = LocalDate.of(2026, 3, 1),
        isPast: Boolean = false,
        repeatType: String = "none"
    ): EventUiState {
        val event = Event(
            id = id,
            title = "event-$id",
            date = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            category = CATEGORY_OTHER,
            repeatType = repeatType,
            createdAt = createdAt
        )
        return EventUiState(
            event = event,
            daysRemaining = daysRemaining,
            isPast = isPast,
            nextOccurrenceDate = date
        )
    }
}
