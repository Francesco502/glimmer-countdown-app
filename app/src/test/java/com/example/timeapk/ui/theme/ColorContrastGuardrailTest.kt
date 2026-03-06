package com.example.timeapk.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastGuardrailTest {

    @Test
    fun songLightPalette_meetsAaForCoreTextPairs() {
        val audit = ColorContrastGuardrail.audit(
            background = SongLightBackground,
            onBackground = SongLightTextOnBG,
            surface = SongLightSurface,
            onSurface = SongLightTextOnSurface,
            primary = SongLightPrimary,
            onPrimary = Color.White
        )
        assertTrue(audit.isPass)
        assertTrue(audit.minRatio >= ColorContrastGuardrail.AaNormalText)
    }

    @Test
    fun songDarkPalette_meetsAaForCoreTextPairs() {
        val audit = ColorContrastGuardrail.audit(
            background = SongDarkBackground,
            onBackground = SongDarkTextOnBG,
            surface = SongDarkSurface,
            onSurface = SongDarkTextOnSurface,
            primary = SongDarkPrimary,
            onPrimary = Color(0xFF101012)
        )
        assertTrue(audit.isPass)
        assertTrue(audit.minRatio >= ColorContrastGuardrail.AaNormalText)
    }

    @Test
    fun lowContrastPair_isBlockedByGuardrail() {
        val audit = ColorContrastGuardrail.audit(
            background = Color.White,
            onBackground = Color(0xFFF2F2F2),
            surface = Color.White,
            onSurface = Color(0xFFF2F2F2),
            primary = Color(0xFFFDFDFD),
            onPrimary = Color.White
        )
        assertFalse(audit.isPass)
        assertTrue(audit.failedKeys.isNotEmpty())
    }
}
