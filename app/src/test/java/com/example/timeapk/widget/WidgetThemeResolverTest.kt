package com.example.timeapk.widget

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetThemeResolverTest {

    @Test
    fun resolveIsDark_followsSystemNightMode() {
        assertTrue(WidgetThemeResolver.resolveIsDark(Configuration.UI_MODE_NIGHT_YES))
        assertFalse(WidgetThemeResolver.resolveIsDark(Configuration.UI_MODE_NIGHT_NO))
    }

    @Test
    fun normalizeNightMode_masksUnrelatedUiModeBits() {
        val uiMode = Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_YES

        assertEquals(
            Configuration.UI_MODE_NIGHT_YES,
            WidgetThemeResolver.normalizeNightMode(uiMode)
        )
    }
}
