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

    @Test
    fun overlappingReorder_isBlockedUntilFirstTargetEchoClearsPending() {
        val first = pendingLocalReorderSnapshotOrNull(
            upstreamIds = listOf(1, 2, 3),
            reorderedIds = listOf(3, 1, 2)
        )
        requireNotNull(first)

        assertFalse(canStartHomeReorder(SortType.Custom, first))
        assertTrue(
            shouldRetainPendingLocalReorder(
                currentIds = first.reorderedIds,
                targetIds = first.upstreamIds,
                sortType = SortType.Custom,
                pending = first
            )
        )

        val targetEchoClearsFirst = !shouldRetainPendingLocalReorder(
            currentIds = first.reorderedIds,
            targetIds = first.reorderedIds,
            sortType = SortType.Custom,
            pending = first
        )
        assertTrue(targetEchoClearsFirst)
        assertTrue(canStartHomeReorder(SortType.Custom, pending = null))

        val second = pendingLocalReorderSnapshotOrNull(
            upstreamIds = first.reorderedIds,
            reorderedIds = listOf(3, 2, 1)
        )
        requireNotNull(second)
        assertEquals(first.reorderedIds, second.upstreamIds)
    }

    @Test
    fun overlappingReorder_isAllowedAfterPersistenceFailureClearsPending() {
        val first = pendingLocalReorderSnapshotOrNull(
            upstreamIds = listOf(1, 2, 3),
            reorderedIds = listOf(3, 1, 2)
        )
        requireNotNull(first)
        assertFalse(canStartHomeReorder(SortType.Custom, first))

        val pendingAfterFailure: PendingLocalReorderSnapshot? = null
        assertTrue(canStartHomeReorder(SortType.Custom, pendingAfterFailure))
    }

    @Test
    fun invalidOrNoOpReorder_doesNotCreateAPersistenceSnapshot() {
        assertEquals(
            null,
            pendingLocalReorderSnapshotOrNull(
                upstreamIds = listOf(1, 2, 3),
                reorderedIds = listOf(1, 2, 3)
            )
        )
        assertEquals(
            null,
            pendingLocalReorderSnapshotOrNull(
                upstreamIds = listOf(1, 2, 3),
                reorderedIds = listOf(3, 1)
            )
        )
        assertEquals(
            null,
            pendingLocalReorderSnapshotOrNull(
                upstreamIds = listOf(1, 2, 3),
                reorderedIds = listOf(3, 1, 4)
            )
        )
    }

    @Test
    fun targetChangesDisplayImmediatelyButKeepReorderLockedUntilMatchingCallback() {
        val pending = requireNotNull(
            pendingLocalReorderSnapshotOrNull(
                upstreamIds = listOf(1, 2, 3),
                reorderedIds = listOf(3, 1, 2)
            )
        )
        val changedTargets = listOf(
            Triple(listOf(3, 1), SortType.Custom, "filtered"),
            Triple(listOf(2, 3, 1), SortType.Custom, "pinned"),
            Triple(listOf(1, 2, 3), SortType.ByDays, "mode"),
            Triple(emptyList(), SortType.Custom, "empty")
        )

        changedTargets.forEach { (targetIds, sortType, label) ->
            val decision = decideHomeListTargetSync(
                currentIds = pending.reorderedIds,
                targetIds = targetIds,
                sortType = sortType,
                pending = pending
            )

            assertFalse("$label target must replace the displayed order", decision.retainCurrentOrder)
            assertEquals("$label must not end A", pending, decision.pendingAfterSync)
            assertFalse("$label must not enable B", canStartHomeReorder(SortType.Custom, decision.pendingAfterSync))
        }

        val pendingAfterMatchingCallback: PendingLocalReorderSnapshot? = null
        assertTrue(canStartHomeReorder(SortType.Custom, pendingAfterMatchingCallback))
    }

    @Test
    fun dragEndAfterSortModeChange_createsNeitherPendingTokenNorPersistenceRequest() {
        val request = homeReorderPersistenceRequestOrNull(
            dragEnabledAtEnd = false,
            visibleIds = listOf(1, 2, 3),
            reorderedVisibleIds = listOf(3, 1, 2)
        )

        assertEquals(null, request)
        assertTrue(canStartHomeReorder(SortType.Custom, pending = request?.snapshot))
    }

    @Test
    fun validCustomDragEnd_bindsPendingTokenToItsPersistenceRequest() {
        val request = requireNotNull(
            homeReorderPersistenceRequestOrNull(
                dragEnabledAtEnd = true,
                visibleIds = listOf(1, 2, 3),
                reorderedVisibleIds = listOf(3, 1, 2)
            )
        )

        assertEquals(listOf(1, 2, 3), request.visibleIds)
        assertEquals(listOf(3, 1, 2), request.reorderedVisibleIds)
        assertEquals(request.reorderedVisibleIds, request.snapshot.reorderedIds)
        assertFalse(canStartHomeReorder(SortType.Custom, request.snapshot))
    }

    private data class TestItem(
        val id: Int,
        val title: String
    )
}
