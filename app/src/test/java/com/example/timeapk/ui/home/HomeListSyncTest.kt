package com.example.timeapk.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeListSyncTest {

    @Test
    fun replaceWithOrderedItems_reordersAndUpdatesInPlace() {
        val items = mutableListOf(
            TestItem(1, "A"),
            TestItem(2, "B"),
            TestItem(3, "C")
        )

        items.replaceWithOrderedItems(
            target = listOf(
                TestItem(3, "C*"),
                TestItem(1, "A*"),
                TestItem(4, "D")
            ),
            keyOf = TestItem::id
        )

        assertEquals(
            listOf(
                TestItem(3, "C*"),
                TestItem(1, "A*"),
                TestItem(4, "D")
            ),
            items
        )
    }

    @Test
    fun refreshItemsByKey_keepsCurrentOrderWhileRefreshingPayloads() {
        val items = mutableListOf(
            TestItem(3, "old-c"),
            TestItem(1, "old-a"),
            TestItem(2, "old-b")
        )

        items.refreshItemsByKey(
            target = listOf(
                TestItem(1, "new-a"),
                TestItem(2, "new-b"),
                TestItem(3, "new-c")
            ),
            keyOf = TestItem::id
        )

        assertEquals(
            listOf(
                TestItem(3, "new-c"),
                TestItem(1, "new-a"),
                TestItem(2, "new-b")
            ),
            items
        )
    }

    @Test
    fun shouldKeepCurrentCustomOrder_onlyReturnsTrueForCustomSortWithSameIdsDifferentOrder() {
        assertTrue(
            shouldKeepCurrentCustomOrder(
                currentIds = listOf(3, 1, 2),
                targetIds = listOf(1, 2, 3),
                sortType = SortType.Custom
            )
        )
        assertFalse(
            shouldKeepCurrentCustomOrder(
                currentIds = listOf(3, 1),
                targetIds = listOf(1, 2, 3),
                sortType = SortType.Custom
            )
        )
        assertFalse(
            shouldKeepCurrentCustomOrder(
                currentIds = listOf(3, 1, 2),
                targetIds = listOf(1, 2, 3),
                sortType = SortType.ByDays
            )
        )
    }

    private data class TestItem(
        val id: Int,
        val title: String
    )
}
