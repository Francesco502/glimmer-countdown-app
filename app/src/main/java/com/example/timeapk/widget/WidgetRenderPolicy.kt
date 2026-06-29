package com.example.timeapk.widget

import com.example.timeapk.R

internal data class WidgetRenderStyle(
    val backgroundResId: Int,
    val primaryTextColor: Int,
    val secondaryTextColor: Int,
    val accentTextColor: Int
) {
    val cacheKey: String
        get() = listOf(backgroundResId, primaryTextColor, secondaryTextColor, accentTextColor)
            .joinToString("-") { it.toString(16) }
}

internal object WidgetRenderPolicy {
    private const val InkText = 0xFF1F1F1F.toInt()
    private const val InkSecondary = 0xFF6A6256.toInt()
    private const val SealAccent = 0xFFAF4E31.toInt()
    private const val PaperText = 0xFFFFFBF5.toInt()
    private const val PaperSecondary = 0xFFEEDFD2.toInt()
    private const val GoldAccent = 0xFFF6D9A6.toInt()
    private const val DarkText = 0xFFEDE8DD.toInt()
    private const val DarkSecondary = 0xFFC8BBAA.toInt()

    fun resolve(config: WidgetConfig, theme: WidgetThemeSnapshot): WidgetRenderStyle {
        val clean = config.sanitize()
        val useLightText = resolveUseLightText(clean, theme)
        val colors = resolveTextColors(clean, useLightText)
        return WidgetRenderStyle(
            backgroundResId = resolveBackgroundResId(clean),
            primaryTextColor = colors.primary,
            secondaryTextColor = colors.secondary,
            accentTextColor = colors.accent
        )
    }

    private fun resolveBackgroundResId(config: WidgetConfig): Int {
        val borderless = config.borderMode == BORDER_OFF
        return when (config.appearancePreset) {
            APPEARANCE_SOLID -> if (borderless) {
                R.drawable.widget_background_solid_borderless
            } else {
                R.drawable.widget_background_solid
            }
            APPEARANCE_TRANSLUCENT -> if (borderless) {
                R.drawable.widget_background_translucent_borderless
            } else {
                R.drawable.widget_background_translucent
            }
            APPEARANCE_TRANSPARENT -> if (config.borderMode == BORDER_ON) {
                R.drawable.widget_background_transparent_border
            } else {
                R.drawable.widget_background_transparent
            }
            APPEARANCE_CELADON -> if (borderless) {
                R.drawable.widget_background_celadon_borderless
            } else {
                R.drawable.widget_background_celadon
            }
            APPEARANCE_SEAL -> if (borderless) {
                R.drawable.widget_background_seal_borderless
            } else {
                R.drawable.widget_background_seal
            }
            else -> when (config.backgroundOpacityPercent) {
                0 -> R.drawable.widget_background_transparent
                25, 50, 75 -> R.drawable.widget_background_translucent
                else -> R.drawable.widget_background
            }
        }
    }

    private data class TextColors(
        val primary: Int,
        val secondary: Int,
        val accent: Int
    )

    private fun resolveTextColors(config: WidgetConfig, useLightText: Boolean): TextColors {
        return when {
            config.appearancePreset == APPEARANCE_SEAL -> TextColors(
                primary = PaperText,
                secondary = PaperSecondary,
                accent = GoldAccent
            )
            useLightText -> TextColors(
                primary = DarkText,
                secondary = DarkSecondary,
                accent = GoldAccent
            )
            else -> TextColors(
                primary = InkText,
                secondary = InkSecondary,
                accent = SealAccent
            )
        }
    }

    private fun resolveUseLightText(config: WidgetConfig, theme: WidgetThemeSnapshot): Boolean {
        return when (config.contrastMode) {
            CONTRAST_LIGHT_TEXT -> true
            CONTRAST_DARK_TEXT -> false
            else -> config.appearancePreset == APPEARANCE_SEAL ||
                (theme.isDark && config.appearancePreset != APPEARANCE_TRANSPARENT)
        }
    }
}
