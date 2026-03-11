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
}
