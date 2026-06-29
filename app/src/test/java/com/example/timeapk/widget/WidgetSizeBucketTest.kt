package com.example.timeapk.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSizeBucketTest {

    @Test
    fun resolve_usesMaxWidthWhenWidgetIsResized() {
        val bucket = WidgetSizeBucket.resolve(minWidthDp = 110, maxWidthDp = 190)

        assertEquals(WidgetSizeBucket.MEDIUM, bucket)
    }

    @Test
    fun resolve_keepsVeryNarrowWidgetsSmall() {
        val bucket = WidgetSizeBucket.resolve(minWidthDp = 110, maxWidthDp = 110)

        assertEquals(WidgetSizeBucket.SMALL, bucket)
    }

    @Test
    fun resolve_usesWidthAndHeightFor2x2CompactSquare() {
        val bucket = WidgetSizeBucket.resolve(
            minWidthDp = 110,
            maxWidthDp = 130,
            minHeightDp = 110,
            maxHeightDp = 130
        )

        assertEquals(WidgetSizeBucket.COMPACT_SQUARE, bucket)
    }

    @Test
    fun resolve_usesWidthAndHeightFor3x3StandardSquare() {
        val bucket = WidgetSizeBucket.resolve(
            minWidthDp = 180,
            maxWidthDp = 230,
            minHeightDp = 180,
            maxHeightDp = 230
        )

        assertEquals(WidgetSizeBucket.STANDARD_SQUARE, bucket)
    }

    @Test
    fun resolve_usesWidthAndHeightFor4x2WideShort() {
        val bucket = WidgetSizeBucket.resolve(
            minWidthDp = 260,
            maxWidthDp = 330,
            minHeightDp = 110,
            maxHeightDp = 150
        )

        assertEquals(WidgetSizeBucket.WIDE_SHORT, bucket)
    }

    @Test
    fun resolve_usesWidthAndHeightForTallWidgets() {
        val bucket = WidgetSizeBucket.resolve(
            minWidthDp = 120,
            maxWidthDp = 160,
            minHeightDp = 250,
            maxHeightDp = 340
        )

        assertEquals(WidgetSizeBucket.TALL, bucket)
    }
}
