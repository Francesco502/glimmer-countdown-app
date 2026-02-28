package com.example.timeapk.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.timeapk.ui.utils.findActivity

// 宋代工笔画 (Song Dynasty Gongbi) 主题配置 V5
// 默认主题切换为：宋代工笔画风格 (宣纸/沉墨)
private val DarkColorScheme = darkColorScheme(
    primary = SongDarkPrimary,                   // 泥金
    secondary = SongDarkSecondary,               // 黛绿
    tertiary = SongDarkTertiary,                 // 赭石
    background = SongDarkBackground,             // 漆黑
    surface = SongDarkSurface,                   // 墨锭 (卡片背景)
    surfaceVariant = SongDarkSurface,            // 保持一致
    onPrimary = Color(0xFF101012),               // 深色背景上的文字
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = SongDarkTextOnBG,             // 银灰
    onSurface = SongDarkTextOnSurface,           // 霜白
    onSurfaceVariant = SongDarkTextOnBG,         // 银灰
    outline = SongDarkTextOnBG.copy(alpha = 0.2f) // 极淡的边框
)

private val LightColorScheme = lightColorScheme(
    primary = SongLightPrimary,                  // 胭脂
    secondary = SongLightSecondary,              // 石绿
    tertiary = SongLightTertiary,                // 淡墨
    background = SongLightBackground,            // 宣纸
    surface = SongLightSurface,                  // 留白 (卡片背景)
    surfaceVariant = SongLightBackground,        // 辅助背景
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = SongLightTextOnBG,            // 焦墨
    onSurface = SongLightTextOnSurface,          // 焦墨
    onSurfaceVariant = SongLightTextOnBG,        // 焦墨
    outline = SongLightTextOnBG.copy(alpha = 0.12f) // 极淡的边框，模拟纸张边缘
)

private fun parseHexOrNull(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        null
    }
}

/** @param themeMode 0=跟随系统 1=浅色 2=深色；fontPreset 0=默认 1=衬线 2=手写 3=等宽；custom*Hex 为用户自定义色 */
@Composable
fun TimeAPKTheme(
    themeMode: Int = 0,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    fontPreset: Int = 0,
    customBackgroundHex: String? = null,
    customSurfaceHex: String? = null,
    customPrimaryHex: String? = null,
    customOnBackgroundHex: String? = null,
    content: @Composable () -> Unit
) {
    val resolvedDark = when (themeMode) {
        1 -> false
        2 -> true
        else -> darkTheme
    }
    var colorScheme = when {
        // 动态配色仅用于浅色主题，深色始终使用自定义 Cinematic Dark Glass
        dynamicColor && !resolvedDark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (resolvedDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        resolvedDark -> DarkColorScheme
        else -> LightColorScheme
    }
    parseHexOrNull(customBackgroundHex)?.let { colorScheme = colorScheme.copy(background = it, surface = it) }
    parseHexOrNull(customSurfaceHex)?.let { colorScheme = colorScheme.copy(surfaceVariant = it) }
    parseHexOrNull(customPrimaryHex)?.let { colorScheme = colorScheme.copy(primary = it) }
    parseHexOrNull(customOnBackgroundHex)?.let {
        colorScheme = colorScheme.copy(
            onBackground = it,
            onSurface = it,
            onSurfaceVariant = it
        )
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            // 状态栏使用与背景一致的色彩，营造沉浸感
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !resolvedDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typographyForFontPreset(fontPreset),
        content = content
    )
}
