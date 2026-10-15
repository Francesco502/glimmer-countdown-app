package com.example.timeapk.widget

import com.example.timeapk.R

internal data class WidgetTextColors(val primary: Int, val secondary: Int, val accent: Int)

internal data class WidgetRenderStyle(
    val rootLayoutResId: Int,
    val backgroundResId: Int,
    val itemLayoutResId: Int,
    val useThemeTextColors: Boolean,
    val lightTextColors: WidgetTextColors,
    val darkTextColors: WidgetTextColors,
    val isDark: Boolean
) {
    private val textColors get() = if (isDark) darkTextColors else lightTextColors
    val primaryTextColor get() = textColors.primary
    val secondaryTextColor get() = textColors.secondary
    val accentTextColor get() = textColors.accent

    val cacheKey: String
        get() = listOf(
            rootLayoutResId,
            backgroundResId,
            itemLayoutResId,
            if (useThemeTextColors) 1 else 0,
            primaryTextColor,
            secondaryTextColor,
            accentTextColor
        )
            .joinToString("-") { it.toString(16) }
}

internal object WidgetRenderPolicy {
    private const val InkText = 0xFF1F1F1F.toInt()
    private const val InkSecondary = 0xFF6A6256.toInt()
    private const val SealAccent = 0xFFAF4E31.toInt()
    private const val NativeGlassText = 0xFF202124.toInt()
    private const val NativeGlassSecondary = 0xCC202124.toInt()
    private const val NativeGlassAccent = 0xFFB45A4E.toInt()
    private const val PaperText = 0xFFFFFBF5.toInt()
    private const val PaperSecondary = 0xFFEEDFD2.toInt()
    private const val GoldAccent = 0xFFF6D9A6.toInt()
    private const val DarkText = 0xFFEDE8DD.toInt()
    private const val DarkSecondary = 0xFFC8BBAA.toInt()

    fun resolve(config: WidgetConfig, theme: WidgetThemeSnapshot): WidgetRenderStyle {
        val clean = config.sanitize()
        val useLightText = resolveUseLightText(clean, theme)
        return WidgetRenderStyle(
            rootLayoutResId = resolveRootLayoutResId(clean),
            backgroundResId = resolveBackgroundResId(clean),
            itemLayoutResId = resolveItemLayoutResId(clean, useLightText, theme.isDark),
            useThemeTextColors = resolveUseThemeTextColors(clean),
            lightTextColors = resolveTextColors(clean, resolveUseLightText(clean, theme.copy(isDark = false))),
            darkTextColors = resolveTextColors(clean, resolveUseLightText(clean, theme.copy(isDark = true))),
            isDark = theme.isDark
        )
    }

    private fun resolveUseThemeTextColors(config: WidgetConfig): Boolean {
        if (config.contrastMode != CONTRAST_AUTO) return false
        return when (config.appearancePreset) {
            APPEARANCE_SYSTEM -> config.backgroundOpacityPercent == 0 ||
                config.backgroundOpacityPercent == 100
            APPEARANCE_SOLID,
            APPEARANCE_TRANSPARENT,
            APPEARANCE_CELADON -> true
            else -> false
        }
    }

    private fun resolveRootLayoutResId(config: WidgetConfig): Int {
        if (isTransparentSurface(config)) {
            return when (config.cornerMode) {
                CORNER_SMALL -> R.layout.widget_countdown_transparent_corner_small
                CORNER_MEDIUM -> R.layout.widget_countdown_transparent_corner_medium
                CORNER_LARGE -> R.layout.widget_countdown_transparent_corner_large
                else -> R.layout.widget_countdown_transparent
            }
        }
        return when (config.cornerMode) {
            CORNER_SMALL -> R.layout.widget_countdown_corner_small
            CORNER_MEDIUM -> R.layout.widget_countdown_corner_medium
            CORNER_LARGE -> R.layout.widget_countdown_corner_large
            else -> R.layout.widget_countdown
        }
    }

    /** 单事件小组件的根布局，圆角与透明表面规则与列表小组件一致。 */
    fun resolveSingleEventLayoutResId(config: WidgetConfig): Int {
        val clean = config.sanitize()
        if (isTransparentSurface(clean)) return R.layout.widget_single_event_transparent
        return when (clean.cornerMode) {
            CORNER_SMALL -> R.layout.widget_single_event_corner_small
            CORNER_MEDIUM -> R.layout.widget_single_event_corner_medium
            CORNER_LARGE -> R.layout.widget_single_event_corner_large
            else -> R.layout.widget_single_event
        }
    }

    private fun isTransparentSurface(config: WidgetConfig): Boolean {
        return config.appearancePreset == APPEARANCE_TRANSPARENT ||
            (config.appearancePreset == APPEARANCE_SYSTEM && config.backgroundOpacityPercent == 0)
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
                else -> if (borderless) {
                    R.drawable.widget_background_borderless
                } else {
                    R.drawable.widget_background
                }
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

    private fun resolveTextColors(config: WidgetConfig, useLightText: Boolean): WidgetTextColors {
        return when {
            config.appearancePreset == APPEARANCE_SEAL -> WidgetTextColors(
                primary = PaperText,
                secondary = PaperSecondary,
                accent = GoldAccent
            )
            isMilkyGlassSurface(config) && !useLightText -> WidgetTextColors(
                primary = NativeGlassText,
                secondary = NativeGlassSecondary,
                accent = NativeGlassAccent
            )
            useLightText -> WidgetTextColors(
                primary = DarkText,
                secondary = DarkSecondary,
                accent = GoldAccent
            )
            else -> WidgetTextColors(
                primary = InkText,
                secondary = InkSecondary,
                accent = SealAccent
            )
        }
    }

    private fun resolveItemLayoutResId(config: WidgetConfig, useLightText: Boolean, isDark: Boolean): Int {
        if (!needsTextProtection(config, useLightText, isDark)) return R.layout.widget_countdown_item
        return if (useLightText) {
            R.layout.widget_countdown_item_shadow_dark
        } else {
            R.layout.widget_countdown_item_shadow_light
        }
    }

    private fun needsTextProtection(config: WidgetConfig, useLightText: Boolean, isDark: Boolean): Boolean {
        // Glass is milky by day and deep by night: text matching that tone needs no shadow,
        // a forced opposite text colour (light on milky, dark on deep) does.
        if (isMilkyGlassSurface(config)) return useLightText != isDark
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
            // Glass follows the theme: milky glass with ink text by day, deep glass with light text by night.
            else -> config.appearancePreset == APPEARANCE_SEAL || theme.isDark
        }
    }

    private fun isMilkyGlassSurface(config: WidgetConfig): Boolean {
        return config.appearancePreset == APPEARANCE_TRANSLUCENT ||
            (config.appearancePreset == APPEARANCE_SYSTEM && config.backgroundOpacityPercent in 25..75)
    }
}
