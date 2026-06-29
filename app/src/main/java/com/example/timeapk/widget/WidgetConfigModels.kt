package com.example.timeapk.widget

import org.json.JSONObject

const val SIZE_TEMPLATE_2X2 = 0
const val SIZE_TEMPLATE_3X3 = 1
const val SIZE_TEMPLATE_4X2 = 2

const val APPEARANCE_SYSTEM = 0
const val APPEARANCE_SOLID = 1
const val APPEARANCE_TRANSLUCENT = 2
const val APPEARANCE_TRANSPARENT = 3
const val APPEARANCE_CELADON = 4
const val APPEARANCE_SEAL = 5

const val BORDER_AUTO = 0
const val BORDER_ON = 1
const val BORDER_OFF = 2

const val CORNER_SYSTEM = 0
const val CORNER_SMALL = 1
const val CORNER_MEDIUM = 2
const val CORNER_LARGE = 3

const val DENSITY_COMPACT = 0
const val DENSITY_STANDARD = 1
const val DENSITY_COMFORTABLE = 2

const val CONTENT_ALL = 0
const val CONTENT_PINNED = 1
const val CONTENT_FUTURE = 2
const val CONTENT_BIRTHDAY = 3

const val SORT_HOME = 0
const val SORT_PINNED_FIRST = 1
const val SORT_NEAREST_FIRST = 2

const val CONTRAST_AUTO = 0
const val CONTRAST_LIGHT_TEXT = 1
const val CONTRAST_DARK_TEXT = 2

private const val FIELD_VERSION = "version"
private const val FIELD_SIZE_TEMPLATE = "sizeTemplate"
private const val FIELD_APPEARANCE_PRESET = "appearancePreset"
private const val FIELD_BACKGROUND_OPACITY = "backgroundOpacityPercent"
private const val FIELD_BORDER_MODE = "borderMode"
private const val FIELD_CORNER_MODE = "cornerMode"
private const val FIELD_DENSITY_MODE = "densityMode"
private const val FIELD_CONTENT_SCOPE = "contentScope"
private const val FIELD_SORT_MODE = "sortMode"
private const val FIELD_SHOW_LUNAR_PREFIX = "showLunarPrefix"
private const val FIELD_CONTRAST_MODE = "contrastMode"
private const val FIELD_FONT_SCALE = "fontScale"

data class WidgetConfig(
    val version: Int = VERSION,
    val sizeTemplate: Int = SIZE_TEMPLATE_2X2,
    val appearancePreset: Int = APPEARANCE_SYSTEM,
    val backgroundOpacityPercent: Int = 75,
    val borderMode: Int = BORDER_AUTO,
    val cornerMode: Int = CORNER_SYSTEM,
    val densityMode: Int = DENSITY_STANDARD,
    val contentScope: Int = CONTENT_ALL,
    val sortMode: Int = SORT_HOME,
    val showLunarPrefix: Boolean = true,
    val contrastMode: Int = CONTRAST_AUTO,
    val fontScale: Float = 1.0f
) {
    companion object {
        const val VERSION = 1
        private const val FONT_SCALE_MIN = 0.85f
        private const val FONT_SCALE_MAX = 1.60f

        fun default(): WidgetConfig = WidgetConfig()

        fun fromJson(raw: String?): WidgetConfig {
            if (raw.isNullOrBlank()) return default()
            return try {
                val obj = JSONObject(raw)
                WidgetConfig(
                    version = obj.optInt(FIELD_VERSION, VERSION),
                    sizeTemplate = obj.optInt(FIELD_SIZE_TEMPLATE, SIZE_TEMPLATE_2X2),
                    appearancePreset = obj.optInt(FIELD_APPEARANCE_PRESET, APPEARANCE_SYSTEM),
                    backgroundOpacityPercent = obj.optInt(FIELD_BACKGROUND_OPACITY, 75),
                    borderMode = obj.optInt(FIELD_BORDER_MODE, BORDER_AUTO),
                    cornerMode = obj.optInt(FIELD_CORNER_MODE, CORNER_SYSTEM),
                    densityMode = obj.optInt(FIELD_DENSITY_MODE, DENSITY_STANDARD),
                    contentScope = obj.optInt(FIELD_CONTENT_SCOPE, CONTENT_ALL),
                    sortMode = obj.optInt(FIELD_SORT_MODE, SORT_HOME),
                    showLunarPrefix = obj.optBoolean(FIELD_SHOW_LUNAR_PREFIX, true),
                    contrastMode = obj.optInt(FIELD_CONTRAST_MODE, CONTRAST_AUTO),
                    fontScale = obj.optDouble(FIELD_FONT_SCALE, 1.0).toFloat()
                ).sanitize()
            } catch (_: Exception) {
                default()
            }
        }

        internal fun sanitizeFontScale(scale: Float): Float =
            scale.coerceIn(FONT_SCALE_MIN, FONT_SCALE_MAX)
    }

    fun sanitize(): WidgetConfig {
        return copy(
            version = VERSION,
            sizeTemplate = sizeTemplate.sanitizeEnum(
                SIZE_TEMPLATE_2X2,
                SIZE_TEMPLATE_3X3,
                SIZE_TEMPLATE_4X2,
                fallback = SIZE_TEMPLATE_2X2
            ),
            appearancePreset = appearancePreset.sanitizeEnum(
                APPEARANCE_SYSTEM,
                APPEARANCE_SOLID,
                APPEARANCE_TRANSLUCENT,
                APPEARANCE_TRANSPARENT,
                APPEARANCE_CELADON,
                APPEARANCE_SEAL,
                fallback = APPEARANCE_SYSTEM
            ),
            backgroundOpacityPercent = sanitizeOpacity(backgroundOpacityPercent),
            borderMode = borderMode.sanitizeEnum(BORDER_AUTO, BORDER_ON, BORDER_OFF, fallback = BORDER_AUTO),
            cornerMode = cornerMode.sanitizeEnum(
                CORNER_SYSTEM,
                CORNER_SMALL,
                CORNER_MEDIUM,
                CORNER_LARGE,
                fallback = CORNER_SYSTEM
            ),
            densityMode = densityMode.sanitizeEnum(
                DENSITY_COMPACT,
                DENSITY_STANDARD,
                DENSITY_COMFORTABLE,
                fallback = DENSITY_STANDARD
            ),
            contentScope = contentScope.sanitizeEnum(
                CONTENT_ALL,
                CONTENT_PINNED,
                CONTENT_FUTURE,
                CONTENT_BIRTHDAY,
                fallback = CONTENT_ALL
            ),
            sortMode = sortMode.sanitizeEnum(
                SORT_HOME,
                SORT_PINNED_FIRST,
                SORT_NEAREST_FIRST,
                fallback = SORT_HOME
            ),
            contrastMode = contrastMode.sanitizeEnum(
                CONTRAST_AUTO,
                CONTRAST_LIGHT_TEXT,
                CONTRAST_DARK_TEXT,
                fallback = CONTRAST_AUTO
            ),
            fontScale = sanitizeFontScale(fontScale)
        )
    }

    fun toJson(): String {
        val clean = sanitize()
        return JSONObject()
            .put(FIELD_VERSION, clean.version)
            .put(FIELD_SIZE_TEMPLATE, clean.sizeTemplate)
            .put(FIELD_APPEARANCE_PRESET, clean.appearancePreset)
            .put(FIELD_BACKGROUND_OPACITY, clean.backgroundOpacityPercent)
            .put(FIELD_BORDER_MODE, clean.borderMode)
            .put(FIELD_CORNER_MODE, clean.cornerMode)
            .put(FIELD_DENSITY_MODE, clean.densityMode)
            .put(FIELD_CONTENT_SCOPE, clean.contentScope)
            .put(FIELD_SORT_MODE, clean.sortMode)
            .put(FIELD_SHOW_LUNAR_PREFIX, clean.showLunarPrefix)
            .put(FIELD_CONTRAST_MODE, clean.contrastMode)
            .put(FIELD_FONT_SCALE, clean.fontScale.toDouble())
            .toString()
    }

    val cacheKey: String
        get() = listOf(
            version,
            sizeTemplate,
            appearancePreset,
            backgroundOpacityPercent,
            borderMode,
            cornerMode,
            densityMode,
            contentScope,
            sortMode,
            showLunarPrefix,
            contrastMode,
            (fontScale * 100).toInt()
        ).joinToString("-")
}

fun encodeWidgetInstanceConfigs(configs: Map<Int, WidgetConfig>): String {
    val obj = JSONObject()
    configs.toSortedMap().forEach { (appWidgetId, config) ->
        obj.put(appWidgetId.toString(), JSONObject(config.toJson()))
    }
    return obj.toString()
}

fun decodeWidgetInstanceConfigs(raw: String?): Map<Int, WidgetConfig> {
    if (raw.isNullOrBlank()) return emptyMap()
    return try {
        val obj = JSONObject(raw)
        obj.keys().asSequence().mapNotNull { key ->
            val id = key.toIntOrNull() ?: return@mapNotNull null
            id to WidgetConfig.fromJson(obj.optJSONObject(key)?.toString())
        }.toMap()
    } catch (_: Exception) {
        emptyMap()
    }
}

fun removeWidgetInstanceConfig(raw: String?, appWidgetId: Int): String {
    val configs = decodeWidgetInstanceConfigs(raw).toMutableMap()
    configs.remove(appWidgetId)
    return encodeWidgetInstanceConfigs(configs)
}

private fun Int.sanitizeEnum(vararg allowed: Int, fallback: Int): Int =
    if (this in allowed) this else fallback

private fun sanitizeOpacity(value: Int): Int {
    val allowed = listOf(0, 25, 50, 75, 100)
    return allowed.firstOrNull { value <= it } ?: 100
}
