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
        val onSurfaceRatio = ColorContrastGuardrail.contrastRatio(opaqueOnSurface, background)
        val blackRatio = ColorContrastGuardrail.contrastRatio(Color.Black, background)
        val whiteRatio = ColorContrastGuardrail.contrastRatio(Color.White, background)
        val endpoint = if (onSurfaceRatio >= minRatio) {
            opaqueOnSurface
        } else if (blackRatio >= whiteRatio) {
            Color.Black
        } else {
            Color.White
        }
        if (ColorContrastGuardrail.contrastRatio(endpoint, background) < minRatio) {
            return endpoint
        }

        var failingFraction = 0f
        var passingFraction = 1f
        repeat(SearchIterations) {
            val candidateFraction = (failingFraction + passingFraction) / 2f
            val candidate = lerp(opaqueEventColor, endpoint, candidateFraction)
            if (ColorContrastGuardrail.contrastRatio(candidate, background) >= minRatio) {
                passingFraction = candidateFraction
            } else {
                failingFraction = candidateFraction
            }
        }
        return lerp(opaqueEventColor, endpoint, passingFraction).copy(alpha = 1f)
    }

    fun compositeOver(foreground: Color, background: Color): Color {
        val outputAlpha = foreground.alpha + background.alpha * (1f - foreground.alpha)
        if (outputAlpha <= 0f) return Color.Transparent

        fun compositeChannel(foregroundChannel: Float, backgroundChannel: Float): Float =
            (
                foregroundChannel * foreground.alpha +
                    backgroundChannel * background.alpha * (1f - foreground.alpha)
                ) / outputAlpha

        return Color(
            red = compositeChannel(foreground.red, background.red),
            green = compositeChannel(foreground.green, background.green),
            blue = compositeChannel(foreground.blue, background.blue),
            alpha = outputAlpha
        )
    }
}
