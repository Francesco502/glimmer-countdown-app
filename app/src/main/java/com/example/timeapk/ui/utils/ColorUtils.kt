package com.example.timeapk.ui.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.toColorInt

/**
 * 解析事件颜色，如果无效则返回回退颜色
 */
fun parseEventColorOrFallback(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(hex.toColorInt())
    } catch (_: Exception) {
        fallback
    }
}

/**
 * 根据背景色计算合适的内容色（黑或白）
 */
fun contentColorFor(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() > 0.5f) Color(0xFF1A232C) else Color.White
}

/**
 * 获取事件颜色，使用 MaterialTheme 作为回退
 */
@Composable
fun getEventColor(colorHex: String?): Color {
    return parseEventColorOrFallback(
        hex = colorHex,
        fallback = MaterialTheme.colorScheme.primary
    )
}
