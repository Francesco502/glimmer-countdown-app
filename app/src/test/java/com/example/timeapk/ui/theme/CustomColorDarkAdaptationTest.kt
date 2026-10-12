package com.example.timeapk.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomColorDarkAdaptationTest {
    private fun luminance(rgb: Int): Double {
        fun channel(c: Int): Double {
            val v = c / 255.0
            return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel((rgb shr 16) and 0xFF) +
            0.7152 * channel((rgb shr 8) and 0xFF) +
            0.0722 * channel(rgb and 0xFF)
    }

    private fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @Test
    fun lightBackgroundsBecomeDarkInDarkMode() {
        val adapted = adaptCustomColorForDark(0xF5F3ED, CustomColorRole.Background)
        assertTrue(luminance(adapted) < 0.03)
    }

    @Test
    fun alreadyDarkColorsAreKept() {
        assertEquals(0x101012, adaptCustomColorForDark(0x101012, CustomColorRole.Background))
        assertEquals(0xEDE8DD, adaptCustomColorForDark(0xEDE8DD, CustomColorRole.OnBackground))
    }

    @Test
    fun adaptedTextStaysReadableOnAdaptedBackground() {
        val background = adaptCustomColorForDark(0xFFFBF5, CustomColorRole.Background)
        val text = adaptCustomColorForDark(0x1F1F1F, CustomColorRole.OnBackground)
        assertTrue(contrast(background, text) >= 7.0)
    }

    @Test
    fun hueIsPreservedForPrimary() {
        val adapted = adaptCustomColorForDark(0x2A0F08, CustomColorRole.Primary)
        val r = (adapted shr 16) and 0xFF
        val b = adapted and 0xFF
        assertTrue("warm hue should stay warm", r > b)
        assertTrue(luminance(adapted) > 0.1)
    }
}
