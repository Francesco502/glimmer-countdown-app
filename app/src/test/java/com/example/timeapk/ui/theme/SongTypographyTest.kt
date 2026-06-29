package com.example.timeapk.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class SongTypographyTest {
    @Test
    fun songThinSerifPreset_usesNonNegativeLetterSpacingForDisplayText() {
        val typography = typographyForFontPreset(preset = 4, baseScale = 1f)

        assertTrue(typography.displayLarge.letterSpacing.value >= 0f)
        assertTrue(typography.displayMedium.letterSpacing.value >= 0f)
        assertTrue(typography.displaySmall.letterSpacing.value >= 0f)
    }
}
