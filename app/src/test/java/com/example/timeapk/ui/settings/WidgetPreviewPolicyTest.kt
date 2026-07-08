package com.example.timeapk.ui.settings

import com.example.timeapk.widget.APPEARANCE_SYSTEM
import com.example.timeapk.widget.APPEARANCE_TRANSPARENT
import com.example.timeapk.widget.CONTRAST_AUTO
import com.example.timeapk.widget.WidgetConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetPreviewPolicyTest {
    @Test
    fun resolveWidgetPreviewStyle_transparentAutoContrastFollowsDarkSystemTheme() {
        val style = resolveWidgetPreviewStyle(
            config = WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_TRANSPARENT,
                backgroundOpacityPercent = 0,
                contrastMode = CONTRAST_AUTO
            ),
            isDark = true
        )

        assertEquals(0xFFEDE8DD.toInt(), style.contentColorArgb)
        assertEquals(0xFFF6D9A6.toInt(), style.accentColorArgb)
    }

    @Test
    fun resolveWidgetPreviewStyle_systemGlassUsesNativeMilkySurfaceInDarkTheme() {
        val style = resolveWidgetPreviewStyle(
            config = WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_SYSTEM,
                backgroundOpacityPercent = 25,
                contrastMode = CONTRAST_AUTO
            ),
            isDark = true
        )

        assertEquals(0xB3F1F3F0.toInt(), style.backgroundColorArgb)
        assertEquals(0x1A202124, style.borderColorArgb)
        assertEquals(0xFF202124.toInt(), style.contentColorArgb)
        assertEquals(0xFFB45A4E.toInt(), style.accentColorArgb)
    }
}
