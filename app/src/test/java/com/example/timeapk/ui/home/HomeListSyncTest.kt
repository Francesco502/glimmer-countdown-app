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
    fun pendingLocalReorder_isRetainedOnlyWhileOldUpstreamOrderAwaitsPersistence() {
        val pending = PendingLocalReorderSnapshot(
            upstreamIds = listOf(1, 2, 3),
            reorderedIds = listOf(3, 1, 2)
        )

        assertTrue(
            shouldRetainPendingLocalReorder(
                currentIds = listOf(3, 1, 2),
                targetIds = listOf(1, 2, 3),
                sortType = SortType.Custom,
                pending = pending
            )
        )
    }

    @Test
    fun customModeTransition_withoutPendingDrag_appliesPersistedTarget() {
        assertFalse(
            shouldRetainPendingLocalReorder(
                currentIds = listOf(3, 1, 2),
                targetIds = listOf(1, 2, 3),
                sortType = SortType.Custom,
                pending = null
            )
        )
    }

    @Test
    fun pendingLocalReorder_isClearedForModeTargetOrActiveIdChanges() {
        val pending = PendingLocalReorderSnapshot(
            upstreamIds = listOf(1, 2, 3),
            reorderedIds = listOf(3, 1, 2)
        )

        assertFalse(
            shouldRetainPendingLocalReorder(
                currentIds = listOf(3, 1, 2),
                targetIds = listOf(1, 2, 3),
                sortType = SortType.ByDays,
                pending = pending
            )
        )
        assertFalse(
            shouldRetainPendingLocalReorder(
                currentIds = listOf(3, 1),
                targetIds = listOf(1, 2, 3),
                sortType = SortType.Custom,
                pending = pending
            )
        )
        assertFalse(
            shouldRetainPendingLocalReorder(
                currentIds = listOf(3, 1, 2),
                targetIds = listOf(2, 1, 3),
                sortType = SortType.Custom,
                pending = pending
            )
        )
        assertFalse(
            shouldRetainPendingLocalReorder(
                currentIds = listOf(3, 1, 2),
                targetIds = listOf(3, 1, 2),
                sortType = SortType.Custom,
                pending = pending
            )
        )
    }

    private data class TestItem(
        val id: Int,
        val title: String
    )
}
