package com.example.timeapk.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetConfigModelsTest {
    @Test
    fun defaultConfig_uses2x2SystemStandardAllEvents() {
        val config = WidgetConfig.default()

        assertEquals(SIZE_TEMPLATE_2X2, config.sizeTemplate)
        assertEquals(APPEARANCE_SYSTEM, config.appearancePreset)
        assertEquals(75, config.backgroundOpacityPercent)
        assertEquals(DENSITY_STANDARD, config.densityMode)
        assertEquals(CONTENT_ALL, config.contentScope)
        assertEquals(SORT_HOME, config.sortMode)
        assertTrue(config.showLunarPrefix)
    }

    @Test
    fun sanitize_clampsUnknownEnumsAndOpacity() {
        val sanitized = WidgetConfig(
            version = -1,
            sizeTemplate = 99,
            appearancePreset = 99,
            backgroundOpacityPercent = 37,
            borderMode = 99,
            cornerMode = 99,
            densityMode = 99,
            contentScope = 99,
            sortMode = 99,
            showLunarPrefix = false,
            contrastMode = 99,
            fontScale = 99f
        ).sanitize()

        assertEquals(WidgetConfig.VERSION, sanitized.version)
        assertEquals(SIZE_TEMPLATE_2X2, sanitized.sizeTemplate)
        assertEquals(APPEARANCE_SYSTEM, sanitized.appearancePreset)
        assertEquals(50, sanitized.backgroundOpacityPercent)
        assertEquals(BORDER_AUTO, sanitized.borderMode)
        assertEquals(CORNER_SYSTEM, sanitized.cornerMode)
        assertEquals(DENSITY_STANDARD, sanitized.densityMode)
        assertEquals(CONTENT_ALL, sanitized.contentScope)
        assertEquals(SORT_HOME, sanitized.sortMode)
        assertEquals(CONTRAST_AUTO, sanitized.contrastMode)
        assertEquals(1.60f, sanitized.fontScale, 0.01f)
        assertFalse(sanitized.showLunarPrefix)
    }

    @Test
    fun sanitize_acceptsSealAppearancePreset() {
        val sanitized = WidgetConfig.default()
            .copy(appearancePreset = APPEARANCE_SEAL)
            .sanitize()

        assertEquals(APPEARANCE_SEAL, sanitized.appearancePreset)
    }

    @Test
    fun jsonRoundTrip_preservesSupportedFields() {
        val original = WidgetConfig.default().copy(
            sizeTemplate = SIZE_TEMPLATE_4X2,
            appearancePreset = APPEARANCE_SEAL,
            backgroundOpacityPercent = 25,
            borderMode = BORDER_OFF,
            cornerMode = CORNER_LARGE,
            densityMode = DENSITY_COMPACT,
            contentScope = CONTENT_PINNED,
            sortMode = SORT_NEAREST_FIRST,
            showLunarPrefix = false,
            contrastMode = CONTRAST_LIGHT_TEXT,
            fontScale = 1.25f
        )

        val decoded = WidgetConfig.fromJson(original.toJson())

        assertEquals(original, decoded)
    }

    @Test
    fun instanceMapHelpers_roundTripAndRemoveOneWidget() {
        val first = WidgetConfig.default().copy(contentScope = CONTENT_PINNED)
        val second = WidgetConfig.default().copy(sizeTemplate = SIZE_TEMPLATE_3X3)
        val encoded = encodeWidgetInstanceConfigs(mapOf(101 to first, 202 to second))

        val decoded = decodeWidgetInstanceConfigs(encoded)
        val removed = removeWidgetInstanceConfig(encoded, 101)

        assertEquals(first, decoded[101])
        assertEquals(second, decoded[202])
        assertFalse(decodeWidgetInstanceConfigs(removed).containsKey(101))
        assertEquals(second, decodeWidgetInstanceConfigs(removed)[202])
    }
}
