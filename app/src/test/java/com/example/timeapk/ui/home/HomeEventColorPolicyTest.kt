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
}
