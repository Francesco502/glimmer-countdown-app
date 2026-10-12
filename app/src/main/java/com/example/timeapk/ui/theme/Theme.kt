package com.example.timeapk.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
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
    onPrimary = Color(0xFF1F1F1F), // 更柔和的墨色
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = SongDarkTextOnBG,
    onSurface = SongDarkTextOnSurface,
    onSurfaceVariant = SongDarkTextSecondary,
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
    onSurfaceVariant = SongLightTextSecondary,
    outline = SongLightTextOnBG.copy(alpha = SongDesignTokens.BorderAlphaSoft)
)

private fun parseHexOrNull(hex: String?): Color? {
    val normalized = normalizeOpaqueThemeHex(hex) ?: return null
    return Color(normalized.toColorInt())
}

private fun parseCustomColor(hex: String?, dark: Boolean, role: CustomColorRole): Color? {
    val color = parseHexOrNull(hex) ?: return null
    if (!dark) return color
    val rgb = color.toArgb() and 0xFFFFFF
    return Color(0xFF000000.toInt() or adaptCustomColorForDark(rgb, role))
}

/** 动态取色：取壁纸配色的色相，保留宋韵的纸纹、字体与圆角。 */
@androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
private fun songDynamicColorScheme(
    context: android.content.Context,
    dark: Boolean
): androidx.compose.material3.ColorScheme {
    val dynamic = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    return dynamic.copy(
        surfaceVariant = dynamic.surfaceContainerHigh,
        outline = dynamic.onBackground.copy(alpha = SongDesignTokens.BorderAlphaSoft)
    )
}

@Suppress("DEPRECATION")
private fun applyStatusBarStyle(
    view: android.view.View,
    backgroundColorArgb: Int,
    darkIcons: Boolean
) {
    val window = view.context.findActivity()?.window ?: return
    window.statusBarColor = backgroundColorArgb
    window.navigationBarColor = backgroundColorArgb
    WindowCompat.getInsetsController(window, view).apply {
        isAppearanceLightStatusBars = darkIcons
        isAppearanceLightNavigationBars = darkIcons
    }
}

private object SongRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor(): Color = MaterialTheme.colorScheme.onSurface

    @Composable
    override fun rippleAlpha(): RippleAlpha = RippleAlpha(
        pressedAlpha = SongDesignTokens.RippleAlpha,
        focusedAlpha = SongDesignTokens.RippleAlpha,
        draggedAlpha = SongDesignTokens.RippleAlpha * 1.2f,
        hoveredAlpha = SongDesignTokens.RippleAlpha * 0.6f
    )
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

    val useDynamicColor = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    var colorScheme = when {
        useDynamicColor -> songDynamicColorScheme(context, resolvedDark)
        resolvedDark -> DarkColorScheme
        else -> LightColorScheme
    }

    // 动态取色开启时自定义颜色不生效（设置页有说明）。
    if (!useDynamicColor) {
        parseCustomColor(customBackgroundHex, resolvedDark, CustomColorRole.Background)?.let {
            colorScheme = colorScheme.copy(background = it)
        }
        parseCustomColor(customSurfaceHex, resolvedDark, CustomColorRole.Surface)?.let {
            colorScheme = colorScheme.copy(surface = it, surfaceVariant = it.copy(alpha = 0.95f))
        }
        parseCustomColor(customPrimaryHex, resolvedDark, CustomColorRole.Primary)?.let {
            colorScheme = colorScheme.copy(primary = it)
        }
        parseCustomColor(customOnBackgroundHex, resolvedDark, CustomColorRole.OnBackground)?.let {
            colorScheme = colorScheme.copy(
                onBackground = it,
                onSurface = it,
                onSurfaceVariant = it.copy(alpha = 0.74f),
                outline = it.copy(alpha = SongDesignTokens.BorderAlphaSoft)
            )
        }
    }

    if (!useDynamicColor) {
        // Menus, sheets and dialogs read surfaceContainer*; keep them on paper instead of M3 baseline lilac.
        val paper = colorScheme.surface
        colorScheme = colorScheme.copy(
            surfaceContainerLowest = paper,
            surfaceContainerLow = paper,
            surfaceContainer = paper,
            surfaceContainerHigh = paper,
            surfaceContainerHighest = colorScheme.surfaceVariant,
            surfaceBright = paper,
            surfaceDim = colorScheme.background,
            surfaceTint = Color.Transparent
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // onCreate can compose before the window attaches; apply after attachment.
            view.post {
                applyStatusBarStyle(
                    view = view,
                    backgroundColorArgb = colorScheme.background.toArgb(),
                    darkIcons = ColorContrastGuardrail.ensureReadableText(
                        Color.Black, colorScheme.background
                    ) == Color.Black
                )
            }
        }
    }

    val songShapes = Shapes(
        extraSmall = RoundedCornerShape(SongDesignTokens.StandardRadius.dp),
        small = RoundedCornerShape(SongDesignTokens.StandardRadius.dp),
        medium = RoundedCornerShape(SongDesignTokens.StandardRadius.dp),
        large = RoundedCornerShape(SongDesignTokens.StandardRadius.dp),
        extraLarge = RoundedCornerShape(SongDesignTokens.StandardRadius.dp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typographyForFontPreset(fontPreset, baseFontScale),
        shapes = songShapes
    ) {
        CompositionLocalProvider(
            LocalRippleTheme provides SongRippleTheme,
            content = content
        )
    }
}
