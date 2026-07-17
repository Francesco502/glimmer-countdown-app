package com.example.timeapk.widget

import com.example.timeapk.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetRenderPolicyTest {
    @Test
    fun resolve_transparentSurfacesUseDedicatedRootLayoutsForEveryCornerMode() {
        val theme = WidgetThemeSnapshot(isDark = false, usesSystemPalette = true)
        val cornerModes = listOf(CORNER_SYSTEM, CORNER_SMALL, CORNER_MEDIUM, CORNER_LARGE)

        cornerModes.forEach { cornerMode ->
            val solid = WidgetRenderPolicy.resolve(
                WidgetConfig.default().copy(
                    cornerMode = cornerMode,
                    appearancePreset = APPEARANCE_SOLID
                ),
                theme
            )
            val transparentPreset = WidgetRenderPolicy.resolve(
                WidgetConfig.default().copy(
                    cornerMode = cornerMode,
                    appearancePreset = APPEARANCE_TRANSPARENT
                ),
                theme
            )
            val transparentSystemSurface = WidgetRenderPolicy.resolve(
                WidgetConfig.default().copy(
                    cornerMode = cornerMode,
                    appearancePreset = APPEARANCE_SYSTEM,
                    backgroundOpacityPercent = 0
                ),
                theme
            )

            assertNotEquals(
                "Transparent and opaque surfaces must not share a RemoteViews layout for corner $cornerMode",
                solid.rootLayoutResId,
                transparentPreset.rootLayoutResId
            )
            assertEquals(transparentPreset.rootLayoutResId, transparentSystemSurface.rootLayoutResId)
        }
    }

    @Test
    fun resolve_themeAdaptiveAutoSurfacesDelegateTextColorsToLayoutTheme() {
        val adaptiveConfigs = listOf(
            WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_SYSTEM,
                backgroundOpacityPercent = 100
            ),
            WidgetConfig.default().copy(appearancePreset = APPEARANCE_SOLID),
            WidgetConfig.default().copy(appearancePreset = APPEARANCE_CELADON),
            WidgetConfig.default().copy(appearancePreset = APPEARANCE_TRANSPARENT)
        )
        adaptiveConfigs.forEach { config ->
            val light = WidgetRenderPolicy.resolve(
                config,
                WidgetThemeSnapshot(isDark = false, usesSystemPalette = true)
            )
            val dark = WidgetRenderPolicy.resolve(
                config,
                WidgetThemeSnapshot(isDark = true, usesSystemPalette = true)
            )
            assertTrue("Light theme should come from the layout for $config", light.useThemeTextColors)
            assertTrue("Dark theme should come from the layout for $config", dark.useThemeTextColors)
        }
    }

    @Test
    fun resolve_fixedSurfacesAndManualContrastKeepExplicitTextColors() {
        val fixedConfigs = listOf(
            WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_SYSTEM,
                backgroundOpacityPercent = 25,
                contrastMode = CONTRAST_AUTO
            ),
            WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_TRANSLUCENT,
                contrastMode = CONTRAST_AUTO
            ),
            WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_SEAL,
                contrastMode = CONTRAST_AUTO
            ),
            WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_SOLID,
                contrastMode = CONTRAST_LIGHT_TEXT
            ),
            WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_TRANSPARENT,
                contrastMode = CONTRAST_DARK_TEXT
            )
        )

        fixedConfigs.forEach { config ->
            val style = WidgetRenderPolicy.resolve(
                config,
                WidgetThemeSnapshot(isDark = true, usesSystemPalette = true)
            )
            assertFalse("Explicit colors are required for $config", style.useThemeTextColors)
        }
    }

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
        assertEquals(R.layout.widget_countdown_item, lightGlass.itemLayoutResId)
        assertEquals(R.layout.widget_countdown_item, mediumGlass.itemLayoutResId)
        assertEquals(R.layout.widget_countdown_item, denseGlass.itemLayoutResId)
        assertEquals(R.layout.widget_countdown_item, solid.itemLayoutResId)
    }

    @Test
    fun resolve_systemGlassPresetsUseNativeMilkySurfaceTextEvenInDarkTheme() {
        val glass = WidgetRenderPolicy.resolve(
            config = WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_SYSTEM,
                backgroundOpacityPercent = 25,
                contrastMode = CONTRAST_AUTO
            ),
            theme = WidgetThemeSnapshot(isDark = true, usesSystemPalette = true)
        )

        assertEquals(R.drawable.widget_background_translucent_25, glass.backgroundResId)
        assertEquals(0xFF202124.toInt(), glass.primaryTextColor)
        assertEquals(0xCC202124.toInt(), glass.secondaryTextColor)
        assertEquals(0xFFB45A4E.toInt(), glass.accentTextColor)
        assertEquals(R.layout.widget_countdown_item, glass.itemLayoutResId)
    }

    @Test
    fun resolve_translucentPresetUsesNativeMilkySurfaceTextEvenInDarkTheme() {
        val glass = WidgetRenderPolicy.resolve(
            config = WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_TRANSLUCENT,
                backgroundOpacityPercent = 25,
                contrastMode = CONTRAST_AUTO
            ),
            theme = WidgetThemeSnapshot(isDark = true, usesSystemPalette = true)
        )

        assertEquals(R.drawable.widget_background_translucent_25, glass.backgroundResId)
        assertEquals(0xFF202124.toInt(), glass.primaryTextColor)
        assertEquals(0xFFB45A4E.toInt(), glass.accentTextColor)
        assertEquals(R.layout.widget_countdown_item, glass.itemLayoutResId)
    }

    @Test
    fun resolve_glassWithLightTextOverrideKeepsTextProtection() {
        val glass = WidgetRenderPolicy.resolve(
            config = WidgetConfig.default().copy(
                appearancePreset = APPEARANCE_TRANSLUCENT,
                backgroundOpacityPercent = 25,
                contrastMode = CONTRAST_LIGHT_TEXT
            ),
            theme = WidgetThemeSnapshot(isDark = true, usesSystemPalette = true)
        )

        assertEquals(0xFFEDE8DD.toInt(), glass.primaryTextColor)
        assertEquals(R.layout.widget_countdown_item_shadow_dark, glass.itemLayoutResId)
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
    fun resolve_systemSolidBorderOffSelectsDifferentBackgroundFromBorderOn() {
        val base = WidgetConfig.default().copy(
            appearancePreset = APPEARANCE_SYSTEM,
            backgroundOpacityPercent = 100
        )
        val bordered = WidgetRenderPolicy.resolve(
            config = base.copy(borderMode = BORDER_ON),
            theme = WidgetThemeSnapshot(isDark = false, usesSystemPalette = true)
        )
        val borderless = WidgetRenderPolicy.resolve(
            config = base.copy(borderMode = BORDER_OFF),
            theme = WidgetThemeSnapshot(isDark = false, usesSystemPalette = true)
        )

        assertNotEquals(bordered.backgroundResId, borderless.backgroundResId)
    }

    @Test
    fun buildWidgetRemoteAdapterDataUri_includesConfigurationFields() {
        val key = buildWidgetRemoteAdapterDataUriString(
            appWidgetId = 7,
            sizeBucket = WidgetSizeBucket.WIDE_SHORT,
            config = WidgetConfig.fromJson(
                """
                {
                  "widthCells": 5,
                  "heightCells": 1,
                  "contentScope": $CONTENT_PINNED
                }
                """.trimIndent()
            ),
            themeKey = "dark-fallback"
        )

        assertTrue(key.contains("widget/7"))
        assertTrue(key.contains("size=${WidgetSizeBucket.WIDE_SHORT}"))
        assertTrue(key.contains("width=5"))
        assertTrue(key.contains("height=1"))
        assertTrue(key.contains("scope=$CONTENT_PINNED"))
        assertTrue(key.contains("theme=dark-fallback"))
    }
}
