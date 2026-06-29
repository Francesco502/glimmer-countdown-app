package com.example.timeapk.ui.theme

const val FONT_PRESET_DEFAULT = 0
const val FONT_PRESET_SYSTEM_SERIF = 1
const val FONT_PRESET_SYSTEM_SANS = 2
const val FONT_PRESET_ZCOOL_XIAOWEI = 3
const val FONT_PRESET_NOTO_SERIF_SC = 4

val FontPresetValues = listOf(
    FONT_PRESET_NOTO_SERIF_SC,
    FONT_PRESET_SYSTEM_SANS,
    FONT_PRESET_ZCOOL_XIAOWEI,
    FONT_PRESET_SYSTEM_SERIF,
    FONT_PRESET_DEFAULT
)

fun sanitizeFontPreset(preset: Int): Int {
    return if (preset in FontPresetValues) preset else FONT_PRESET_NOTO_SERIF_SC
}
