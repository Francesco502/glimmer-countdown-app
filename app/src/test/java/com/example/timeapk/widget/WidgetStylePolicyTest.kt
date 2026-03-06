package com.example.timeapk.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStylePolicyTest {

    @Test
    fun smallBucket_usesNumericFriendlyBoundsAtMaxScale() {
        val style = WidgetStylePolicy.resolve(WidgetSizeBucket.SMALL, 1.60f)
        assertEquals(16.0f, style.titleSp, 0.01f)
        assertEquals(16.0f, style.valueSp, 0.01f)
        assertEquals(6, style.valueMaxEms)
    }

    @Test
    fun mediumAndLargeBuckets_expandValueWidthBudget() {
        val medium = WidgetStylePolicy.resolve(WidgetSizeBucket.MEDIUM, 1.0f)
        val large = WidgetStylePolicy.resolve(WidgetSizeBucket.LARGE, 1.0f)
        assertEquals(10, medium.valueMaxEms)
        assertEquals(14, large.valueMaxEms)
    }

    @Test
    fun fontScale_isClampedToSupportedRange() {
        val tooSmall = WidgetStylePolicy.resolve(WidgetSizeBucket.MEDIUM, 0.1f)
        val tooLarge = WidgetStylePolicy.resolve(WidgetSizeBucket.MEDIUM, 10f)
        assertEquals(9.35f, tooSmall.titleSp, 0.01f)
        assertEquals(17.60f, tooLarge.titleSp, 0.01f)
        assertTrue(tooSmall.titleSp < tooLarge.titleSp)
    }
}
