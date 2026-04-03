package com.example.timeapk.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private fun scaleSp(value: Int, scale: Float): TextUnit = (value * scale).sp

fun typographyForFontPreset(preset: Int, baseScale: Float = 1f): Typography {
    val scale = baseScale.coerceIn(SongDesignTokens.BaseFontScaleMin, SongDesignTokens.BaseFontScaleMax)
    val isSlenderGold = preset == 4
    val bodyFamily = if (isSlenderGold) FontFamily.Serif else FontFamily.Serif
    val displayFamily = if (isSlenderGold) FontFamily.Serif else FontFamily.Serif
    val numbersFamily = if (isSlenderGold) FontFamily.Serif else FontFamily.Serif

    // 优化：在此处可以替换为真实的字体资源，例如：
    // val slenderGoldFamily = FontFamily(Font(R.font.slender_gold, FontWeight.Normal))
    // val bodyFamily = if (isSlenderGold) slenderGoldFamily else FontFamily.Serif

    val displayWeight = if (isSlenderGold) FontWeight.Light else FontWeight.SemiBold
    val titleWeight = if (isSlenderGold) FontWeight.Normal else FontWeight.Medium
    val bodyWeight = if (isSlenderGold) FontWeight.Light else FontWeight.Normal
    val labelWeight = if (isSlenderGold) FontWeight.Normal else FontWeight.Medium

    return Typography(
        displayLarge = TextStyle(
            fontFamily = numbersFamily,
            fontWeight = displayWeight,
            fontSize = scaleSp(36, scale),
            lineHeight = scaleSp(44, scale),
            letterSpacing = (-1).sp
        ),
        displayMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = displayWeight,
            fontSize = scaleSp(32, scale),
            lineHeight = scaleSp(40, scale),
            letterSpacing = if (isSlenderGold) 1.sp else 0.5.sp
        ),
        displaySmall = TextStyle(
            fontFamily = numbersFamily,
            fontWeight = displayWeight,
            fontSize = scaleSp(28, scale),
            lineHeight = scaleSp(36, scale),
            letterSpacing = (-0.5).sp
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
            letterSpacing = 0.3.sp
        ),
        titleMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = titleWeight,
            fontSize = scaleSp(18, scale),
            lineHeight = scaleSp(26, scale),
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = titleWeight,
            fontSize = scaleSp(16, scale),
            lineHeight = scaleSp(24, scale),
            letterSpacing = 0.1.sp
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
            letterSpacing = 0.1.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = labelWeight,
            fontSize = scaleSp(14, scale),
            lineHeight = scaleSp(20, scale),
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = labelWeight,
            fontSize = scaleSp(12, scale),
            lineHeight = scaleSp(16, scale),
            letterSpacing = 0.4.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = labelWeight,
            fontSize = scaleSp(11, scale),
            lineHeight = scaleSp(16, scale),
            letterSpacing = 0.4.sp
        )
    )
}
