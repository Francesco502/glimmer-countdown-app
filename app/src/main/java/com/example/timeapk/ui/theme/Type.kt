package com.example.timeapk.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 参考 Apple HIG：SF Pro 字体层级
// - Large Title: 34pt Bold
// - Title: 22pt
// - Body: 17pt
// - Caption: 12pt
//
// Android 上无法直接使用 SF Pro，这里统一用 SansSerif 近似，并在字号/行高上贴近 iOS。

private val sfLikeFamily = FontFamily.SansSerif
private val serifDisplayFamily = FontFamily.Serif
private val monoNumbersFamily = FontFamily.Monospace

val Typography = Typography(
    // 大标题 (Display) - 衬线体，斜体，模拟电影标题
    displayLarge = TextStyle(
        fontFamily = serifDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = 1.sp
    ),
    // 次级标题 (Title) - 衬线体，庄重
    titleLarge = TextStyle(
        fontFamily = serifDisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.5.sp
    ),
    // 数字大展示 (Display Medium) - 等宽字体，模拟机械钟/打字机
    displayMedium = TextStyle(
        fontFamily = monoNumbersFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1).sp // 紧凑的数字
    ),
    // 普通正文 (Body) - 无衬线，保证清晰
    bodyLarge = TextStyle(
        fontFamily = sfLikeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = sfLikeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    // 小标题 / 标签 (Label)
    labelMedium = TextStyle(
        fontFamily = sfLikeFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    ),
    // 次级辅助文字
    labelSmall = TextStyle(
        fontFamily = sfLikeFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/** 根据字体预设生成 Typography；0=默认 1=衬线 2=手写 3=等宽 4=瘦金体(模拟) */
fun typographyForFontPreset(preset: Int): Typography {
    val (bodyFamily, displayFamily, numbersFamily) = when (preset) {
        1 -> Triple(FontFamily.Serif, FontFamily.Serif, FontFamily.Monospace)
        2 -> Triple(sfLikeFamily, FontFamily.Cursive, FontFamily.Monospace)
        3 -> Triple(FontFamily.Monospace, FontFamily.Monospace, FontFamily.Monospace)
        // 瘦金体 (Slender Gold) 模拟：
        // 由于没有内嵌字体文件，这里使用 Serif 配合 Thin/ExtraLight 字重来模拟瘦金体那种细劲挺拔的感觉。
        // 如果后续导入了 .ttf 文件，可在此处替换为 Font(R.font.slender_gold)
        4 -> Triple(FontFamily.Serif, FontFamily.Serif, FontFamily.Monospace)
        else -> Triple(sfLikeFamily, serifDisplayFamily, monoNumbersFamily)
    }
    
    // 针对瘦金体 (Preset 4) 调整字重
    val isSlenderGold = preset == 4
    val displayWeight = if (isSlenderGold) FontWeight.Light else FontWeight.Bold
    val titleWeight = if (isSlenderGold) FontWeight.Normal else FontWeight.SemiBold
    val bodyWeight = if (isSlenderGold) FontWeight.Light else FontWeight.Normal

    val labelWeight = if (isSlenderGold) FontWeight.Normal else FontWeight.Medium
    return Typography(
        displayLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = displayWeight,
            fontStyle = if (isSlenderGold) FontStyle.Normal else FontStyle.Italic,
            fontSize = 36.sp,
            lineHeight = 42.sp,
            letterSpacing = if (isSlenderGold) 2.sp else 1.sp // 瘦金体字间距稍宽
        ),
        displayMedium = TextStyle(
            fontFamily = numbersFamily,
            fontWeight = if (isSlenderGold) FontWeight.Light else FontWeight.Bold,
            fontSize = 48.sp,
            lineHeight = 56.sp,
            letterSpacing = (-1).sp
        ),
        displaySmall = TextStyle(
            fontFamily = numbersFamily,
            fontWeight = if (isSlenderGold) FontWeight.Light else FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = (-0.5).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = titleWeight,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = titleWeight,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = titleWeight,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = titleWeight,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = 0.5.sp
        ),
        titleMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = titleWeight,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = titleWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = bodyWeight,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = bodyWeight,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = bodyWeight,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = labelWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = labelWeight,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = labelWeight,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}
