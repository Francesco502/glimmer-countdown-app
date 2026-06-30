package com.example.timeapk.widget

import com.example.timeapk.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetRenderPolicyTest {
    @Test
    fun resolve_transparentWithLightTextOverrideUsesTransparentBackgroundAndLightText() {
        val transparent = WidgetRenderPolicy.resolve(
            config = WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_TRANSPARENT,
                backgroundOpacityPercent = 0,
                contrastMode = CONTRAST_LIGHT_TEXT
            ),
            theme = WidgetThemeSnapshot(isDark = true, usesSystemPalette = false)
        )

        assertEquals(R.drawable.widget_background_transparent, transparent.backgroundResId)
        assertEquals(0xFFEDE8DD.toInt(), transparent.primaryTextColor)
        assertEquals(0xFFC8BBAA.toInt(), transparent.secondaryTextColor)
    }

    @Test
    fun resolve_solidLightThemeUsesSolidBackgroundAndDarkText() {
        val solid = WidgetRenderPolicy.resolve(
            config = WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_SOLID,
                backgroundOpacityPercent = 100
            ),
            theme = WidgetThemeSnapshot(isDark = false, usesSystemPalette = false)
        )

        assertEquals(R.drawable.widget_background_solid, solid.backgroundResId)
        assertEquals(0xFF1F1F1F.toInt(), solid.primaryTextColor)
        assertEquals(0xFFAF4E31.toInt(), solid.accentTextColor)
    }

    @Test
    fun resolve_transparentPresetAutoContrastFollowsDarkSystemTheme() {
        val transparent = WidgetRenderPolicy.resolve(
            config = WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_TRANSPARENT,
                backgroundOpacityPercent = 0,
                contrastMode = CONTRAST_AUTO
            ),
            theme = WidgetThemeSnapshot(isDark = true, usesSystemPalette = false)
        )

        assertEquals(R.drawable.widget_background_transparent, transparent.backgroundResId)
        assertEquals(0xFFEDE8DD.toInt(), transparent.primaryTextColor)
        assertEquals(0xFFC8BBAA.toInt(), transparent.secondaryTextColor)
        assertEquals(0xFFF6D9A6.toInt(), transparent.accentTextColor)
        assertEquals(R.layout.widget_countdown_item_shadow_dark, transparent.itemLayoutResId)
    }

    @Test
    fun resolve_transparentPresetAutoContrastKeepsDarkTextInLightSystemTheme() {
        val transparent = WidgetRenderPolicy.resolve(
            config = WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_TRANSPARENT,
                backgroundOpacityPercent = 0,
                contrastMode = CONTRAST_AUTO
            ),
            theme = WidgetThemeSnapshot(isDark = false, usesSystemPalette = false)
        )

        assertEquals(R.drawable.widget_background_transparent, transparent.backgroundResId)
        assertEquals(0xFF1F1F1F.toInt(), transparent.primaryTextColor)
        assertEquals(0xFF6A6256.toInt(), transparent.secondaryTextColor)
        assertEquals(0xFFAF4E31.toInt(), transparent.accentTextColor)
        assertEquals(R.layout.widget_countdown_item_shadow_light, transparent.itemLayoutResId)
    }

    @Test
    fun resolve_systemOpacityPresetsUseDistinctBackgrounds() {
        val config = WidgetConfig.default().copy(appearancePreset = APPEARANCE_SYSTEM)

        val transparent = WidgetRenderPolicy.resolve(
            config = config.copy(backgroundOpacityPercent = 0),
            theme = WidgetThemeSnapshot(isDark = false, usesSystemPalette = true)
        )
        val lightGlass = WidgetRenderPolicy.resolve(
            config = config.copy(backgroundOpacityPercent = 25),
            theme = WidgetThemeSnapshot(isDark = false, usesSystemPalette = true)
        )
        val mediumGlass = WidgetRenderPolicy.resolve(
            config = config.copy(backgroundOpacityPercent = 50),
            theme = WidgetThemeSnapshot(isDark = false, usesSystemPalette = true)
        )
        val denseGlass = WidgetRenderPolicy.resolve(
            config = config.copy(backgroundOpacityPercent = 75),
            theme = WidgetThemeSnapshot(isDark = false, usesSystemPalette = true)
        )
        val solid = WidgetRenderPolicy.resolve(
            config = config.copy(backgroundOpacityPercent = 100),
            theme = WidgetThemeSnapshot(isDark = false, usesSystemPalette = true)
        )

        assertEquals(R.drawable.widget_background_transparent, transparent.backgroundResId)
        assertNotEquals(lightGlass.backgroundResId, mediumGlass.backgroundResId)
        assertNotEquals(mediumGlass.backgroundResId, denseGlass.backgroundResId)
        assertNotEquals(denseGlass.backgroundResId, solid.backgroundResId)
        assertEquals(R.layout.widget_countdown_item_shadow_light, lightGlass.itemLayoutResId)
        assertEquals(R.layout.widget_countdown_item_shadow_light, mediumGlass.itemLayoutResId)
        assertEquals(R.layout.widget_countdown_item_shadow_light, denseGlass.itemLayoutResId)
        assertEquals(R.layout.widget_countdown_item, solid.itemLayoutResId)
    }

    @Test
    fun resolve_sealPresetUsesCinnabarBackgroundAndLightText() {
        val seal = WidgetRenderPolicy.resolve(
            config = WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_SEAL,
                backgroundOpacityPercent = 100
            ),
            theme = WidgetThemeSnapshot(isDark = false, usesSystemPalette = false)
        )

        assertEquals(R.drawable.widget_background_seal, seal.backgroundResId)
        assertEquals(0xFFFFFBF5.toInt(), seal.primaryTextColor)
        assertEquals(0xFFEEDFD2.toInt(), seal.secondaryTextColor)
        assertEquals(0xFFF6D9A6.toInt(), seal.accentTextColor)
    }

    @Test
    fun resolve_celadonPresetUsesCeladonBackgroundResource() {
        val celadon = WidgetRenderPolicy.resolve(
            config = WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_CELADON,
                backgroundOpacityPercent = 75
            ),
            theme = WidgetThemeSnapshot(isDark = false, usesSystemPalette = false)
        )

        assertEquals(R.drawable.widget_background_celadon, celadon.backgroundResId)
        assertEquals(0xFF1F1F1F.toInt(), celadon.primaryTextColor)
    }

    @Test
    fun resolve_borderModesSelectBorderedOrBorderlessBackgrounds() {
        val transparentBorder = WidgetRenderPolicy.resolve(
            config = WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_TRANSPARENT,
                backgroundOpacityPercent = 0,
                borderMode = BORDER_ON
            ),
            theme = WidgetThemeSnapshot(isDark = true, usesSystemPalette = false)
        )
        val solidBorderless = WidgetRenderPolicy.resolve(
            config = WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_SOLID,
                backgroundOpacityPercent = 100,
                borderMode = BORDER_OFF
            ),
            theme = WidgetThemeSnapshot(isDark = false, usesSystemPalette = false)
        )

        assertEquals(R.drawable.widget_background_transparent_border, transparentBorder.backgroundResId)
        assertEquals(R.drawable.widget_background_solid_borderless, solidBorderless.backgroundResId)
    }

    @Test
    fun buildWidgetRemoteAdapterDataUri_includesConfigurationFields() {
        val key = buildWidgetRemoteAdapterDataUriString(
            appWidgetId = 7,
            sizeBucket = WidgetSizeBucket.WIDE_SHORT,
            config = WidgetConfig.default().copy(contentScope = CONTENT_PINNED),
            themeKey = "dark-fallback"
        )

        assertTrue(key.contains("widget/7"))
        assertTrue(key.contains("size=${WidgetSizeBucket.WIDE_SHORT}"))
        assertTrue(key.contains("scope=$CONTENT_PINNED"))
        assertTrue(key.contains("theme=dark-fallback"))
    }
}
