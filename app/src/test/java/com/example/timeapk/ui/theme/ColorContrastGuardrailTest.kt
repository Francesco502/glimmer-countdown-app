package com.example.timeapk.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastGuardrailTest {

    @Test
    fun readableText_fallsBackToAccessibleEndpointWhenPreferredColorFails() {
        val background = Color(0xFF8A877F)
        val preferred = Color(0xFFAC8F62)

        val resolved = ColorContrastGuardrail.ensureReadableText(
            preferred = preferred,
            background = background
        )

        assertTrue(
            ColorContrastGuardrail.contrastRatio(resolved, background) >=
                ColorContrastGuardrail.AaNormalText
        )
    }

    @Test
    fun readableText_preservesPreferredColorWhenItAlreadyPasses() {
        val preferred = Color(0xFF1F1F1F)
        val background = Color(0xFFAC8F62)

        assertEquals(
            preferred,
            ColorContrastGuardrail.ensureReadableText(preferred, background)
        )
    }

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
