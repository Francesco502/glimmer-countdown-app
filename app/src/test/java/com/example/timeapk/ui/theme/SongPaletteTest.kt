package com.example.timeapk.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongPaletteTest {
    @Test
    fun coreLightPalette_exposesSongSemanticColors() {
        assertEquals(Color(0xFFF5F3ED), SongPalette.Paper)
        assertEquals(Color(0xFF1F1F1F), SongPalette.Ink)
        assertEquals(Color(0xFFAF4E31), SongPalette.Seal)
        assertEquals(Color(0xFFE8EEE6), SongPalette.CeladonWash)
    }

    @Test
    fun coreLightPalette_meetsTextContrastOnPaper() {
        val contrast = ColorContrastGuardrail.contrastRatio(
            foreground = SongPalette.Ink,
            background = SongPalette.Paper
        )

        assertTrue(contrast >= ColorContrastGuardrail.AaNormalText)
    }
}
