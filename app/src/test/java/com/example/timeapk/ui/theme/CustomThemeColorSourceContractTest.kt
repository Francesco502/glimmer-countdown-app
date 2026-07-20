package com.example.timeapk.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CustomThemeColorSourceContractTest {

    @Test
    fun customThemeEditorAcceptsOnlyOpaqueSixDigitRgb() {
        val settings = readSource("ui/settings/SettingsSubScreens.kt")
        val picker = settings.substringAfter("colorPickerKey?.let { key ->")
            .substringBefore("if (showFontPresetDialog)")

        assertTrue(picker.contains("normalizeOpaqueThemeHex("))
        assertTrue(picker.contains(".take(6)"))
        assertFalse(picker.contains(".take(8)"))
        assertFalse(picker.contains("normalizedCustomHex.length == 9"))
    }

    @Test
    fun savedLegacyArgbDoesNotProduceAContradictoryColorSwatch() {
        val components = readSource("ui/settings/SettingsComponents.kt")
        val colorRow = components.substringAfter("fun CustomColorRow(")
            .substringBefore("@Composable\nfun SettingsExpandableSection(")

        assertTrue(colorRow.contains("normalizeOpaqueThemeHex(currentHex)"))
        assertFalse(colorRow.contains("currentHex?.let { try { Color(it.toColorInt())"))
    }

    @Test
    fun themeLoaderRejectsLegacyArgbOverridesInsteadOfCompositingThemIncorrectly() {
        val theme = readSource("ui/theme/Theme.kt")
        val parser = theme.substringAfter("private fun parseHexOrNull(")
            .substringBefore("@Suppress(\"DEPRECATION\")")

        assertTrue(parser.contains("normalizeOpaqueThemeHex(hex)"))
        assertFalse(parser.contains("Color(hex.toColorInt())"))
    }

    private fun readSource(relative: String): String {
        val direct = File("src/main/java/com/example/timeapk/$relative")
        if (direct.exists()) return direct.readText(Charsets.UTF_8)
        val fromRoot = File("app/src/main/java/com/example/timeapk/$relative")
        require(fromRoot.exists()) { "Missing source file: $relative" }
        return fromRoot.readText(Charsets.UTF_8)
    }
}
