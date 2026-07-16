package com.example.timeapk.ui.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.timeapk.ui.theme.ColorContrastGuardrail

object HomeEventColorPolicy {
    private const val SearchIterations = 24

    fun ensureTextContrast(
        eventColor: Color,
        onSurface: Color,
        background: Color,
        minRatio: Double = ColorContrastGuardrail.AaNormalText
    ): Color {
        val opaqueEventColor = eventColor.copy(alpha = 1f)
        if (ColorContrastGuardrail.contrastRatio(opaqueEventColor, background) >= minRatio) {
            return opaqueEventColor
        }

        val opaqueOnSurface = onSurface.copy(alpha = 1f)
        if (ColorContrastGuardrail.contrastRatio(opaqueOnSurface, background) < minRatio) {
            return opaqueOnSurface
        }

        var failingFraction = 0f
        var passingFraction = 1f
        repeat(SearchIterations) {
            val candidateFraction = (failingFraction + passingFraction) / 2f
            val candidate = lerp(opaqueEventColor, opaqueOnSurface, candidateFraction)
            if (ColorContrastGuardrail.contrastRatio(candidate, background) >= minRatio) {
                passingFraction = candidateFraction
            } else {
                failingFraction = candidateFraction
            }
        }
        return lerp(opaqueEventColor, opaqueOnSurface, passingFraction).copy(alpha = 1f)
    }
}
