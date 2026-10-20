package com.example.timeapk.ui.settings

import com.example.timeapk.widget.WidgetSizeBucket
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetPreviewPolicyTest {
    @Test
    fun selectedCellDimensionsReachTheSameSizeBucketsAsLauncherDimensions() {
        assertEquals(110, widgetPreviewDimensionDp(1))
        assertEquals(180, widgetPreviewDimensionDp(2))
        assertEquals(320, widgetPreviewDimensionDp(4))
        assertEquals(WidgetSizeBucket.COMPACT_SQUARE, resolveWidgetPreviewSizeBucket(110, 110))
        assertEquals(WidgetSizeBucket.STANDARD_SQUARE, resolveWidgetPreviewSizeBucket(180, 180))
        assertEquals(WidgetSizeBucket.WIDE_SHORT, resolveWidgetPreviewSizeBucket(320, 180))
        assertEquals(WidgetSizeBucket.TALL, resolveWidgetPreviewSizeBucket(180, 320))
    }

    @Test
    fun constrainedPreviewUsesVisibleDimensionsForTextLayout() {
        assertEquals(WidgetSizeBucket.STANDARD_SQUARE, resolveWidgetPreviewSizeBucket(240, 135))
        assertEquals(WidgetSizeBucket.COMPACT_SQUARE, resolveWidgetPreviewSizeBucket(120, 120))
        assertEquals(110, widgetPreviewDimensionDp(0))
        assertEquals(320, widgetPreviewDimensionDp(8))
    }
}