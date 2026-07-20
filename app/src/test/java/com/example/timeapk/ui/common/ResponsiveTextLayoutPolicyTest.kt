package com.example.timeapk.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsiveTextLayoutPolicyTest {
    @Test
    fun largeTextLayout_stacksAtTheSupportedAccessibilityBreakpoint() {
        assertFalse(useLargeTextLayout(1.0f))
        assertFalse(useLargeTextLayout(1.29f))
        assertTrue(useLargeTextLayout(1.3f))
        assertTrue(useLargeTextLayout(1.5f))
    }

    @Test
    fun homeCardTitle_usesOneLineOnlyForLargeText() {
        assertEquals(2, homeCardTitleMaxLines(1.0f))
        assertEquals(2, homeCardTitleMaxLines(1.29f))
        assertEquals(1, homeCardTitleMaxLines(1.3f))
        assertEquals(1, homeCardTitleMaxLines(1.5f))
    }
}
