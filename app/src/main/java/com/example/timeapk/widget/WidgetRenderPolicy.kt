package com.example.timeapk.widget

import com.example.timeapk.R

internal data class WidgetRenderStyle(
    val backgroundResId: Int,
    val itemLayoutResId: Int,
    val primaryTextColor: Int,
    val secondaryTextColor: Int,
    val accentTextColor: Int
) {
    val cacheKey: String
        get() = listOf(backgroundResId, itemLayoutResId, primaryTextColor, secondaryTextColor, accentTextColor)
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
            itemLayoutResId = resolveItemLayoutResId(clean, useLightText),
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
            APPEARANCE_TRANSLUCENT -> resolveTranslucentBackgroundResId(config, borderless)
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
                0 -> if (config.borderMode == BORDER_ON) {
                    R.drawable.widget_background_transparent_border
                } else {
                    R.drawable.widget_background_transparent
                }
                25, 50, 75 -> resolveTranslucentBackgroundResId(config, borderless)
                else -> R.drawable.widget_background
            }
        }
    }

    private fun resolveTranslucentBackgroundResId(config: WidgetConfig, borderless: Boolean): Int {
        return when (config.backgroundOpacityPercent) {
            25 -> if (borderless) {
                R.drawable.widget_background_translucent_25_borderless
            } else {
                R.drawable.widget_background_translucent_25
            }
            50 -> if (borderless) {
                R.drawable.widget_background_translucent_50_borderless
            } else {
                R.drawable.widget_background_translucent_50
            }
            else -> if (borderless) {
                R.drawable.widget_background_translucent_borderless
            } else {
                R.drawable.widget_background_translucent
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

    private fun resolveItemLayoutResId(config: WidgetConfig, useLightText: Boolean): Int {
        if (!needsTextProtection(config)) return R.layout.widget_countdown_item
        return if (useLightText) {
            R.layout.widget_countdown_item_shadow_dark
        } else {
            R.layout.widget_countdown_item_shadow_light
        }
    }

    private fun needsTextProtection(config: WidgetConfig): Boolean {
        return when (config.appearancePreset) {
            APPEARANCE_TRANSPARENT,
            APPEARANCE_TRANSLUCENT -> true
            APPEARANCE_SYSTEM -> config.backgroundOpacityPercent < 100
            else -> false
        }
    }

    private fun resolveUseLightText(config: WidgetConfig, theme: WidgetThemeSnapshot): Boolean {
        return when (config.contrastMode) {
            CONTRAST_LIGHT_TEXT -> true
            CONTRAST_DARK_TEXT -> false
            else -> config.appearancePreset == APPEARANCE_SEAL ||
                theme.isDark
        }
    }
}
