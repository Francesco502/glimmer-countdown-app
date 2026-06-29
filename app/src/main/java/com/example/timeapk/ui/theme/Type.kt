package com.example.timeapk.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.timeapk.R

private fun scaleSp(value: Int, scale: Float): TextUnit = (value * scale).sp

private val SongSerifFontFamily = FontFamily(
    Font(R.font.noto_serif_sc, weight = FontWeight.Light),
    Font(R.font.noto_serif_sc, weight = FontWeight.Normal),
    Font(R.font.noto_serif_sc, weight = FontWeight.Medium),
    Font(R.font.noto_serif_sc, weight = FontWeight.SemiBold)
)

private val ZcoolXiaoWeiFontFamily = FontFamily(
    Font(R.font.zcool_xiaowei_regular, weight = FontWeight.Normal)
)

fun typographyForFontPreset(preset: Int, baseScale: Float = 1f): Typography {
    val scale = baseScale.coerceIn(SongDesignTokens.BaseFontScaleMin, SongDesignTokens.BaseFontScaleMax)
    val sanitizedPreset = sanitizeFontPreset(preset)
    val isSongThinSerif = sanitizedPreset == FONT_PRESET_NOTO_SERIF_SC
    val isDisplaySerif = sanitizedPreset == FONT_PRESET_ZCOOL_XIAOWEI
    val bodyFamily = when (sanitizedPreset) {
        FONT_PRESET_NOTO_SERIF_SC -> SongSerifFontFamily
        FONT_PRESET_SYSTEM_SANS -> FontFamily.SansSerif
        FONT_PRESET_ZCOOL_XIAOWEI -> SongSerifFontFamily
        FONT_PRESET_SYSTEM_SERIF -> FontFamily.Serif
        else -> FontFamily.Default
    }
    val displayFamily = when (sanitizedPreset) {
        FONT_PRESET_NOTO_SERIF_SC -> SongSerifFontFamily
        FONT_PRESET_SYSTEM_SANS -> FontFamily.SansSerif
        FONT_PRESET_ZCOOL_XIAOWEI -> ZcoolXiaoWeiFontFamily
        FONT_PRESET_SYSTEM_SERIF -> FontFamily.Serif
        else -> FontFamily.Default
    }
    val numbersFamily = when (sanitizedPreset) {
        FONT_PRESET_NOTO_SERIF_SC -> SongSerifFontFamily
        FONT_PRESET_SYSTEM_SANS -> FontFamily.SansSerif
        FONT_PRESET_ZCOOL_XIAOWEI -> SongSerifFontFamily
        FONT_PRESET_SYSTEM_SERIF -> FontFamily.Serif
        else -> FontFamily.Default
    }

    val displayWeight = when {
        isSongThinSerif -> FontWeight.Light
        isDisplaySerif -> FontWeight.Normal
        else -> FontWeight.SemiBold
    }
    val titleWeight = if (isSongThinSerif || isDisplaySerif) FontWeight.Normal else FontWeight.Medium
    val bodyWeight = if (isSongThinSerif) FontWeight.Light else FontWeight.Normal
    val labelWeight = if (isSongThinSerif || isDisplaySerif) FontWeight.Normal else FontWeight.Medium

    return Typography(
        displayLarge = TextStyle(
            fontFamily = numbersFamily,
            fontWeight = displayWeight,
            fontSize = scaleSp(36, scale),
            lineHeight = scaleSp(44, scale),
            letterSpacing = 0.sp
        ),
        displayMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = displayWeight,
            fontSize = scaleSp(32, scale),
            lineHeight = scaleSp(40, scale),
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = numbersFamily,
            fontWeight = displayWeight,
            fontSize = scaleSp(28, scale),
            lineHeight = scaleSp(36, scale),
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = titleWeight,
            fontSize = scaleSp(28, scale),
            lineHeight = scaleSp(36, scale),
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = titleWeight,
            fontSize = scaleSp(24, scale),
            lineHeight = scaleSp(32, scale),
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = titleWeight,
            fontSize = scaleSp(20, scale),
            lineHeight = scaleSp(28, scale),
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = titleWeight,
            fontSize = scaleSp(22, scale),
            lineHeight = scaleSp(30, scale),
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = titleWeight,
            fontSize = scaleSp(18, scale),
            lineHeight = scaleSp(26, scale),
            letterSpacing = 0.sp
        ),
        titleSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = titleWeight,
            fontSize = scaleSp(16, scale),
            lineHeight = scaleSp(24, scale),
            letterSpacing = 0.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = bodyWeight,
            fontSize = scaleSp(16, scale),
            lineHeight = scaleSp(26, scale),
            letterSpacing = 0.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = bodyWeight,
            fontSize = scaleSp(14, scale),
            lineHeight = scaleSp(22, scale),
            letterSpacing = 0.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = bodyWeight,
            fontSize = scaleSp(13, scale),
            lineHeight = scaleSp(18, scale),
            letterSpacing = 0.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = labelWeight,
            fontSize = scaleSp(14, scale),
            lineHeight = scaleSp(20, scale),
            letterSpacing = 0.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = labelWeight,
            fontSize = scaleSp(12, scale),
            lineHeight = scaleSp(16, scale),
            letterSpacing = 0.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = labelWeight,
            fontSize = scaleSp(11, scale),
            lineHeight = scaleSp(16, scale),
            letterSpacing = 0.sp
        )
    )
}
