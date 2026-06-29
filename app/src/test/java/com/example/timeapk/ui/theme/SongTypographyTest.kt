package com.example.timeapk.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SongTypographyTest {
    @Test
    fun songThinSerifPreset_usesNonNegativeLetterSpacingForDisplayText() {
        val typography = typographyForFontPreset(preset = 4, baseScale = 1f)

        assertTrue(typography.displayLarge.letterSpacing.value >= 0f)
        assertTrue(typography.displayMedium.letterSpacing.value >= 0f)
        assertTrue(typography.displaySmall.letterSpacing.value >= 0f)
    }

    @Test
    fun bundledFontPresets_referenceMultiplePackagedChineseFonts() {
        val source = readSource("ui/theme/Type.kt")
        val presetSource = readSource("ui/theme/FontPresets.kt")

        assertTrue(source.contains("R.font.noto_serif_sc"))
        assertTrue(source.contains("R.font.zcool_xiaowei_regular"))
        assertTrue(source.contains("FontFamily.SansSerif"))
        assertTrue(source.contains("FONT_PRESET_NOTO_SERIF_SC"))
        assertTrue(source.contains("FONT_PRESET_SYSTEM_SANS"))
        assertTrue(source.contains("FONT_PRESET_ZCOOL_XIAOWEI"))
        assertTrue(presetSource.contains("FONT_PRESET_SYSTEM_SANS"))
    }

    private fun readSource(relative: String): String {
        val direct = File("src/main/java/com/example/timeapk/$relative")
        if (direct.exists()) {
            return direct.readText(Charsets.UTF_8)
        }
        val fromRoot = File("app/src/main/java/com/example/timeapk/$relative")
        require(fromRoot.exists()) { "Missing source file: $relative" }
        return fromRoot.readText(Charsets.UTF_8)
    }
}
