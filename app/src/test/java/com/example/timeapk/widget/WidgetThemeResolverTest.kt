package com.example.timeapk.widget

import android.content.res.Configuration
import com.example.timeapk.data.THEME_DARK
import com.example.timeapk.data.THEME_FOLLOW_SYSTEM
import com.example.timeapk.data.THEME_LIGHT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetThemeResolverTest {

    @Test
    fun resolveIsDark_followsSystemNightModeWhenPreferenceFollowsSystem() {
        assertTrue(
            WidgetThemeResolver.resolveIsDark(
                themeMode = THEME_FOLLOW_SYSTEM,
                systemNightMode = Configuration.UI_MODE_NIGHT_YES
            )
        )
        assertFalse(
            WidgetThemeResolver.resolveIsDark(
                themeMode = THEME_FOLLOW_SYSTEM,
                systemNightMode = Configuration.UI_MODE_NIGHT_NO
            )
        )
    }

    @Test
    fun resolveIsDark_honorsForcedLightMode() {
        assertFalse(
            WidgetThemeResolver.resolveIsDark(
                themeMode = THEME_LIGHT,
                systemNightMode = Configuration.UI_MODE_NIGHT_YES
            )
        )
    }

    @Test
    fun resolveIsDark_honorsForcedDarkMode() {
        assertTrue(
            WidgetThemeResolver.resolveIsDark(
                themeMode = THEME_DARK,
                systemNightMode = Configuration.UI_MODE_NIGHT_NO
            )
        )
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
