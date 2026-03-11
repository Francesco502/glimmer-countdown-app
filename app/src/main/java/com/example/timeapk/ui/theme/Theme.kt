package com.example.timeapk.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import com.example.timeapk.ui.utils.findActivity

private val DarkColorScheme = darkColorScheme(
    primary = SongDarkPrimary,
    secondary = SongDarkSecondary,
    tertiary = SongDarkTertiary,
    background = SongDarkBackground,
    surface = SongDarkSurface,
    surfaceVariant = SongDarkSurfaceVariant,
    onPrimary = Color(0xFF101012),
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = SongDarkTextOnBG,
    onSurface = SongDarkTextOnSurface,
    onSurfaceVariant = SongDarkTextOnBG,
    outline = SongDarkTextOnBG.copy(alpha = SongDesignTokens.BorderAlphaSoft)
)

private val LightColorScheme = lightColorScheme(
    primary = SongLightPrimary,
    secondary = SongLightSecondary,
    tertiary = SongLightTertiary,
    background = SongLightBackground,
    surface = SongLightSurface,
    surfaceVariant = SongLightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = SongLightTextOnBG,
    onSurface = SongLightTextOnSurface,
    onSurfaceVariant = SongLightTextOnBG,
    outline = SongLightTextOnBG.copy(alpha = SongDesignTokens.BorderAlphaSoft)
)

private fun parseHexOrNull(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        Color(hex.toColorInt())
    } catch (_: Exception) {
        null
    }
}

@Composable
fun TimeAPKTheme(
    themeMode: Int = 0,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    fontPreset: Int = 0,
    baseFontScale: Float = 1f,
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
        dynamicColor && !resolvedDark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicLightColorScheme(LocalContext.current)
        }
        resolvedDark -> DarkColorScheme
        else -> LightColorScheme
    }

    parseHexOrNull(customBackgroundHex)?.let {
        colorScheme = colorScheme.copy(background = it)
    }
    parseHexOrNull(customSurfaceHex)?.let {
        colorScheme = colorScheme.copy(surface = it, surfaceVariant = it.copy(alpha = 0.95f))
    }
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
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !resolvedDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typographyForFontPreset(fontPreset, baseFontScale),
        content = content
    )
}
