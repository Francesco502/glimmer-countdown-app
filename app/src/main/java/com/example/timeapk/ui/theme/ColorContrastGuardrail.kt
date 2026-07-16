package com.example.timeapk.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

private const val RelativeLuminanceOffset = 0.05

data class ContrastCheck(
    val key: String,
    val ratio: Double,
    val passes: Boolean
)

data class ContrastAudit(val checks: List<ContrastCheck>) {
    val isPass: Boolean get() = checks.all { it.passes }
    val minRatio: Double get() = checks.minOfOrNull { it.ratio } ?: 0.0
    val failedKeys: List<String> get() = checks.filterNot { it.passes }.map { it.key }
}

object ColorContrastGuardrail {
    const val AaNormalText = 4.5

    fun contrastRatio(foreground: Color, background: Color): Double {
        val fg = foreground.luminance().toDouble()
        val bg = background.luminance().toDouble()
        val lighter = max(fg, bg)
        val darker = min(fg, bg)
        return (lighter + RelativeLuminanceOffset) / (darker + RelativeLuminanceOffset)
    }

    fun ensureReadableText(
        preferred: Color,
        background: Color,
        minRatio: Double = AaNormalText
    ): Color {
        if (contrastRatio(preferred, background) >= minRatio) return preferred
        val blackRatio = contrastRatio(Color.Black, background)
        val whiteRatio = contrastRatio(Color.White, background)
        return if (blackRatio >= whiteRatio) Color.Black else Color.White
    }

    fun audit(
        background: Color,
        onBackground: Color,
        surface: Color,
        onSurface: Color,
        primary: Color,
        onPrimary: Color,
        minRatio: Double = AaNormalText
    ): ContrastAudit {
        val backgroundRatio = contrastRatio(onBackground, background)
        val surfaceRatio = contrastRatio(onSurface, surface)
        val primaryRatio = contrastRatio(onPrimary, primary)
        val checks = listOf(
            ContrastCheck(
                key = "background_text",
                ratio = backgroundRatio,
                passes = backgroundRatio >= minRatio
            ),
            ContrastCheck(
                key = "surface_text",
                ratio = surfaceRatio,
                passes = surfaceRatio >= minRatio
            ),
            ContrastCheck(
                key = "primary_text",
                ratio = primaryRatio,
                passes = primaryRatio >= minRatio
            )
        )
        return ContrastAudit(checks)
    }
}
