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
