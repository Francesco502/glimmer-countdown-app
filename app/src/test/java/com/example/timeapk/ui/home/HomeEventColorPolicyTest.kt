package com.example.timeapk.ui.home

import androidx.compose.ui.graphics.Color
import com.example.timeapk.ui.theme.ColorContrastGuardrail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeEventColorPolicyTest {
    private val darkSurface = Color(0xFF1A1816)
    private val darkOnSurface = Color(0xFFE9E1D6)

    @Test
    fun darkEventColorsAreLiftedToNormalTextContrast() {
        val eventColors = listOf(
            Color(0xFF6F3028),
            Color(0xFF28506B),
            Color(0xFF395E3B),
            Color(0xFF725626)
        )

        val ratios = eventColors.map { eventColor ->
            val readable = HomeEventColorPolicy.ensureTextContrast(
                eventColor = eventColor,
                onSurface = darkOnSurface,
                background = darkSurface
            )
            assertEquals(1f, readable.alpha)
            ColorContrastGuardrail.contrastRatio(readable, darkSurface)
        }
        val minimumRatio = ratios.min()

        println("dark event text minimum contrast ratio=$minimumRatio")
        assertTrue(minimumRatio >= ColorContrastGuardrail.AaNormalText)
    }

    @Test
    fun contrastAdjustmentRetainsEventHueDifferences() {
        val red = HomeEventColorPolicy.ensureTextContrast(
            eventColor = Color(0xFF6F3028),
            onSurface = darkOnSurface,
            background = darkSurface
        )
        val blue = HomeEventColorPolicy.ensureTextContrast(
            eventColor = Color(0xFF28506B),
            onSurface = darkOnSurface,
            background = darkSurface
        )

        assertNotEquals(red, blue)
        assertNotEquals(darkOnSurface, red)
        assertNotEquals(darkOnSurface, blue)
    }

    @Test
    fun alreadyReadableEventColorIsNotChanged() {
        val readableEventColor = Color(0xFFD18C62)

        assertEquals(
            readableEventColor,
            HomeEventColorPolicy.ensureTextContrast(
                eventColor = readableEventColor,
                onSurface = darkOnSurface,
                background = darkSurface
            )
        )
    }

    @Test
    fun failingThemeEndpointFallsBackToReadableBlackOrWhite() {
        val customBackground = Color(0xFF777777)
        val failingOnSurface = Color(0xFF787878)

        val readable = HomeEventColorPolicy.ensureTextContrast(
            eventColor = Color(0xFF747474),
            onSurface = failingOnSurface,
            background = customBackground
        )

        assertNotEquals(failingOnSurface, readable)
        assertTrue(readable.red < customBackground.red)
        val customEndpointRatio = ColorContrastGuardrail.contrastRatio(readable, customBackground)
        println("custom endpoint final contrast ratio=$customEndpointRatio")
        assertTrue(customEndpointRatio >= ColorContrastGuardrail.AaNormalText)
    }

    @Test
    fun unreachableRequestedRatioReturnsBestBlackOrWhiteEndpoint() {
        val customBackground = Color(0xFF777777)

        val bestAvailable = HomeEventColorPolicy.ensureTextContrast(
            eventColor = Color(0xFF747474),
            onSurface = Color(0xFF787878),
            background = customBackground,
            minRatio = 30.0
        )

        assertEquals(Color.Black, bestAvailable)
        assertTrue(
            ColorContrastGuardrail.contrastRatio(bestAvailable, customBackground) >=
                ColorContrastGuardrail.contrastRatio(Color.White, customBackground)
        )
    }

    @Test
    fun finalCompositedHomeStatesMeetNormalTextContrast() {
        val appBackground = Color(0xFF12100F)
        val surface = Color(0xFF211E1B)
        val primary = Color(0xFFD78865)
        val eventColor = Color(0xFF54352E)
        val effectiveBackgrounds = listOf(
            appBackground,
            HomeEventColorPolicy.compositeOver(surface.copy(alpha = 0.86f), appBackground),
            HomeEventColorPolicy.compositeOver(primary.copy(alpha = 0.05f), appBackground),
            surface
        )

        val finalRatios = effectiveBackgrounds.map { effectiveBackground ->
            val textColor = HomeEventColorPolicy.ensureTextContrast(
                eventColor = eventColor,
                onSurface = darkOnSurface,
                background = effectiveBackground
            )
            ColorContrastGuardrail.contrastRatio(textColor, effectiveBackground)
        }
        val minimumFinalRatio = finalRatios.min()

        println("normal/past/pressed/dragging final contrast ratios=$finalRatios")
        assertTrue(minimumFinalRatio >= ColorContrastGuardrail.AaNormalText)
    }
}
