package com.example.timeapk.ui.theme

import androidx.compose.ui.graphics.Color

object SongDesignTokens {
    const val RadiusXs = 2
    const val RadiusSm = 4
    const val RadiusMd = 8
    const val RadiusLg = 12

    const val StandardRadius = RadiusSm // Global default corner radius: 4dp.
    const val BorderWidth = 0.5f // Global default border width: 0.5dp.
    const val BorderAlphaSoft = 0.18f
    const val BorderAlphaStrong = 0.32f
    const val PressScaleSubtle = 0.985f

    const val PaddingBase = 8 // Base spacing unit.
    const val PaddingList = 16 // Default list item spacing.
    const val MotionFastMs = 140
    const val MotionNormalMs = 220
    const val MotionSlowMs = 300

    const val BaseFontScaleMin = 0.85f
    const val BaseFontScaleMax = 1.30f
    const val WidgetFontScaleMin = 0.85f
    const val WidgetFontScaleMax = 1.60f
}

object SongPalette {
    val Paper = Color(0xFFF5F3ED)
    val PaperWarm = Color(0xFFFFFBF5)
    val PaperMuted = Color(0xFFF2EDE3)
    val PaperDeep = Color(0xFFEDE8DD)
    val Ink = Color(0xFF1F1F1F)
    val InkMuted = Color(0xFF6A6256)
    val InkSoft = Color(0x331F1F1F)
    val Seal = Color(0xFFAF4E31)
    val SealDark = Color(0xFF86351C)
    val Celadon = Color(0xFF457080)
    val CeladonWash = Color(0xFFE8EEE6)
    val Jade = Color(0xFF5B8E79)
    val Gold = Color(0xFFAC8F62)
    val DarkPaper = Color(0xFF1C1C1E)
    val DarkPaperVariant = Color(0xFF27272B)
    val DarkInk = Color(0xFFEDE8DD)
}
