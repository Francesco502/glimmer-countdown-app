package com.example.timeapk.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.json.JSONObject
import org.junit.Test

class WidgetConfigModelsTest {
    @Test
    fun defaultConfig_uses2x2SystemStandardAllEvents() {
        val config = WidgetConfig.default()
        val json = JSONObject(config.toJson())

        assertEquals(SIZE_TEMPLATE_2X2, config.sizeTemplate)
        assertEquals(2, json.optInt("widthCells", -1))
        assertEquals(2, json.optInt("heightCells", -1))
        assertEquals(APPEARANCE_SYSTEM, config.appearancePreset)
        assertEquals(75, config.backgroundOpacityPercent)
        assertEquals(DENSITY_STANDARD, config.densityMode)
        assertEquals(CONTENT_ALL, config.contentScope)
        assertEquals(SORT_HOME, config.sortMode)
        assertTrue(config.showLunarPrefix)
    }

    @Test
    fun sanitize_clampsUnknownEnumsAndOpacity() {
        val sanitized = WidgetConfig.fromJson(
            """
            {
              "version": -1,
              "widthCells": 0,
              "heightCells": 99,
              "appearancePreset": 99,
              "backgroundOpacityPercent": 37,
              "borderMode": 99,
              "cornerMode": 99,
              "densityMode": 99,
              "contentScope": 99,
              "sortMode": 99,
              "showLunarPrefix": false,
              "contrastMode": 99,
              "fontScale": 99
            }
            """.trimIndent()
        )
        val json = JSONObject(sanitized.toJson())

        assertEquals(WidgetConfig.VERSION, sanitized.version)
        assertEquals(SIZE_TEMPLATE_2X2, sanitized.sizeTemplate)
        assertEquals(1, json.optInt("widthCells", -1))
        assertEquals(5, json.optInt("heightCells", -1))
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
        val original = WidgetConfig.fromJson(
            """
            {
              "widthCells": 5,
              "heightCells": 1,
              "appearancePreset": $APPEARANCE_SEAL,
              "backgroundOpacityPercent": 25,
              "borderMode": $BORDER_OFF,
              "cornerMode": $CORNER_LARGE,
              "densityMode": $DENSITY_COMPACT,
              "contentScope": $CONTENT_PINNED,
              "sortMode": $SORT_NEAREST_FIRST,
              "showLunarPrefix": false,
              "contrastMode": $CONTRAST_LIGHT_TEXT,
              "fontScale": 1.25
            }
            """.trimIndent()
        )

        val decoded = WidgetConfig.fromJson(original.toJson())
        val decodedJson = JSONObject(decoded.toJson())

        assertEquals(original, decoded)
        assertEquals(5, decodedJson.optInt("widthCells", -1))
        assertEquals(1, decodedJson.optInt("heightCells", -1))
    }

    @Test
    fun fromJson_migratesLegacySizeTemplatesToCustomCells() {
        val compact = JSONObject(WidgetConfig.fromJson("""{"sizeTemplate":$SIZE_TEMPLATE_2X2}""").toJson())
        val standard = JSONObject(WidgetConfig.fromJson("""{"sizeTemplate":$SIZE_TEMPLATE_3X3}""").toJson())
        val wide = JSONObject(WidgetConfig.fromJson("""{"sizeTemplate":$SIZE_TEMPLATE_4X2}""").toJson())

        assertEquals(2, compact.optInt("widthCells", -1))
        assertEquals(2, compact.optInt("heightCells", -1))
        assertEquals(3, standard.optInt("widthCells", -1))
        assertEquals(3, standard.optInt("heightCells", -1))
        assertEquals(4, wide.optInt("widthCells", -1))
        assertEquals(2, wide.optInt("heightCells", -1))
    }

    @Test
    fun cacheKey_changesWhenCustomCellsChange() {
        val square = WidgetConfig.fromJson("""{"widthCells":2,"heightCells":2}""")
        val wide = WidgetConfig.fromJson("""{"widthCells":5,"heightCells":2}""")

        assertNotEquals(square.cacheKey, wide.cacheKey)
    }

    @Test
    fun instanceMapHelpers_roundTripAndRemoveOneWidget() {
        val first = WidgetConfig.default().copy(contentScope = CONTENT_PINNED)
        val second = WidgetConfig.default().copy(widthCells = 3, heightCells = 3)
        val encoded = encodeWidgetInstanceConfigs(mapOf(101 to first, 202 to second))

        val decoded = decodeWidgetInstanceConfigs(encoded)
        val removed = removeWidgetInstanceConfig(encoded, 101)

        assertEquals(first, decoded[101])
        assertEquals(second, decoded[202])
        assertFalse(decodeWidgetInstanceConfigs(removed).containsKey(101))
        assertEquals(second, decodeWidgetInstanceConfigs(removed)[202])
    }
}
