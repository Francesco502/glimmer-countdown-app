package com.example.timeapk.ui.settings

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
}
