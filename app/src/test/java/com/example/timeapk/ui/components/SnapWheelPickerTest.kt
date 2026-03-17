package com.example.timeapk.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapWheelPickerTest {

    @Test
    fun centeredVisibleItemIndex_returnsItemClosestToViewportCenter() {
        val centeredIndex = centeredVisibleItemIndex(
            viewportStartOffset = 0,
            viewportEndOffset = 180,
            visibleItems = listOf(
                WheelVisibleItemInfo(index = 13, offset = 0, size = 36),
                WheelVisibleItemInfo(index = 14, offset = 36, size = 36),
                WheelVisibleItemInfo(index = 15, offset = 72, size = 36),
                WheelVisibleItemInfo(index = 16, offset = 108, size = 36),
                WheelVisibleItemInfo(index = 17, offset = 144, size = 36)
            )
        )

        assertEquals(15, centeredIndex)
    }

    @Test
    fun itemIndexFromVisibleIndex_accountsForPaddingRows() {
        assertEquals(
            15,
            itemIndexFromVisibleIndex(
                visibleIndex = 17,
                paddingCount = 2,
                itemCount = 100
            )
        )
    }

    @Test
    fun shouldSyncToSelectedItem_returnsFalseWhenCurrentCenteredItemAlreadyMatchesTarget() {
        assertFalse(
            shouldSyncToSelectedItem(
                currentCenteredVisibleIndex = 17,
                targetItemIndex = 15,
                paddingCount = 2,
                itemCount = 100
            )
        )
    }

    @Test
    fun shouldSyncToSelectedItem_returnsTrueWhenCurrentCenteredItemDiffersFromTarget() {
        assertTrue(
            shouldSyncToSelectedItem(
                currentCenteredVisibleIndex = 3,
                targetItemIndex = 15,
                paddingCount = 2,
                itemCount = 100
            )
        )
    }
}
