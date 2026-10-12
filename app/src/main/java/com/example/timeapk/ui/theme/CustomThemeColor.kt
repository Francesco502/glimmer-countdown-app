package com.example.timeapk.ui.theme

internal fun normalizeOpaqueThemeHex(input: String?): String? {
    val digits = input
        ?.trim()
        ?.removePrefix("#")
        ?.takeIf { it.length == 6 }
        ?: return null
    if (digits.any { !it.isDigit() && it.uppercaseChar() !in 'A'..'F' }) return null
    return "#${digits.uppercase()}"
}

/** 用户自定义颜色在深色模式下的用途。 */
internal enum class CustomColorRole { Background, Surface, Primary, OnBackground }

/**
 * 自定义颜色只保存一组；深色模式下保持色相，把明度映射到深色可读区间，
 * 避免浅色背景在深色模式里刺眼或文字不可读。返回 0xRRGGBB。
 */
internal fun adaptCustomColorForDark(rgb: Int, role: CustomColorRole): Int {
    val hsl = rgbToHsl(rgb)
    val (h, s, l) = hsl
    val adapted = when (role) {
        CustomColorRole.Background -> if (l <= 0.22f) return rgb else Triple(h, s * 0.45f, 0.08f)
        CustomColorRole.Surface -> if (l <= 0.28f) return rgb else Triple(h, s * 0.40f, 0.13f)
        CustomColorRole.Primary -> if (l in 0.45f..0.80f) return rgb else Triple(h, s.coerceAtMost(0.65f), 0.62f)
        CustomColorRole.OnBackground -> if (l >= 0.72f) return rgb else Triple(h, s * 0.25f, 0.88f)
    }
    return hslToRgb(adapted.first, adapted.second, adapted.third)
}

private fun rgbToHsl(rgb: Int): Triple<Float, Float, Float> {
    val r = ((rgb shr 16) and 0xFF) / 255f
    val g = ((rgb shr 8) and 0xFF) / 255f
    val b = (rgb and 0xFF) / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    if (max == min) return Triple(0f, 0f, l)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        r -> ((g - b) / d + (if (g < b) 6f else 0f))
        g -> ((b - r) / d + 2f)
        else -> ((r - g) / d + 4f)
    } / 6f
    return Triple(h, s, l)
}

private fun hslToRgb(h: Float, s: Float, l: Float): Int {
    fun hueToRgb(p: Float, q: Float, tIn: Float): Float {
        var t = tIn
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 1f / 2f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
    }
    val (r, g, b) = if (s == 0f) {
        Triple(l, l, l)
    } else {
        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q
        Triple(hueToRgb(p, q, h + 1f / 3f), hueToRgb(p, q, h), hueToRgb(p, q, h - 1f / 3f))
    }
    fun channel(v: Float) = (v.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
    return (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
}
