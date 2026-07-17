package com.example.timeapk.ui.settings

import com.example.timeapk.R
import com.example.timeapk.widget.APPEARANCE_SYSTEM
import com.example.timeapk.widget.APPEARANCE_TRANSPARENT
import com.example.timeapk.widget.CONTRAST_AUTO
import com.example.timeapk.widget.CORNER_LARGE
import com.example.timeapk.widget.CORNER_MEDIUM
import com.example.timeapk.widget.CORNER_SMALL
import com.example.timeapk.widget.CORNER_SYSTEM
import com.example.timeapk.widget.DENSITY_COMFORTABLE
import com.example.timeapk.widget.DENSITY_COMPACT
import com.example.timeapk.widget.DENSITY_STANDARD
import com.example.timeapk.widget.WidgetConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetPreviewPolicyTest {
    @Test
    fun previewCornerPolicy_distinguishesConfiguredModes() {
        assertEquals(22, resolveWidgetPreviewCornerRadiusDp(CORNER_SYSTEM))
        assertEquals(10, resolveWidgetPreviewCornerRadiusDp(CORNER_SMALL))
        assertEquals(18, resolveWidgetPreviewCornerRadiusDp(CORNER_MEDIUM))
        assertEquals(30, resolveWidgetPreviewCornerRadiusDp(CORNER_LARGE))
    }

    @Test
    fun previewLunarTitlePolicy_reflectsPrefixToggle() {
        assertEquals(
            R.string.widget_config_preview_event_secondary,
            resolveWidgetPreviewSecondaryTitleResId(showLunarPrefix = true)
        )
        assertEquals(
            R.string.widget_config_preview_event_secondary_plain,
            resolveWidgetPreviewSecondaryTitleResId(showLunarPrefix = false)
        )
    }

    @Test
    fun previewDensityPolicy_changesRowSpacing() {
        assertEquals(1, resolveWidgetPreviewRowVerticalPaddingDp(DENSITY_COMPACT))
        assertEquals(3, resolveWidgetPreviewRowVerticalPaddingDp(DENSITY_STANDARD))
        assertEquals(5, resolveWidgetPreviewRowVerticalPaddingDp(DENSITY_COMFORTABLE))
    }

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
