package com.example.timeapk.widget

import com.example.timeapk.ui.utils.DisplayModes
import java.util.Locale

object WidgetValueFormatter {

    fun semanticValueOrFallback(
        preferredText: String,
        fallbackText: String,
        maxChars: Int
    ): String {
        if (preferredText.isBlank()) return fallbackText
        val visibleLength = preferredText.count { !it.isWhitespace() }
        return if (visibleLength <= maxChars) preferredText else fallbackText
    }

    fun numericValueForSmall(
        mode: Int,
        isPast: Boolean,
        isRepeating: Boolean,
        isToday: Boolean,
        daysElapsed: Long,
        daysPassed: Long,
        daysRemaining: Long,
        daysLeft: Long
    ): String {
        if (isToday) return "0"

        val days = when (mode) {
            DisplayModes.PAST_DAYS,
            DisplayModes.PAST_YMD -> if (isRepeating) daysPassed else daysElapsed
            DisplayModes.UNTIL_DAYS,
            DisplayModes.UNTIL_YMD -> if (isRepeating) daysLeft else daysRemaining
            else -> if (isPast) daysElapsed else daysRemaining
        }
        val signed = if (isPast) -days else days
        return compactForSmallWidget(signed)
    }

    private fun compactForSmallWidget(value: Long): String {
        val negative = value < 0
        val absValue = kotlin.math.abs(value)
        val compact = when {
            absValue < 100_000L -> absValue.toString()
            absValue < 1_000_000L -> "${absValue / 1_000L}k"
            absValue < 1_000_000_000L -> formatWithUnit(absValue, 1_000_000.0, "m")
            else -> formatWithUnit(absValue, 1_000_000_000.0, "b")
        }
        val signed = if (negative) "-$compact" else compact
        return if (signed.length <= 6) signed else signed.take(6)
    }

    private fun formatWithUnit(value: Long, base: Double, unit: String): String {
        val scaled = value / base
        val text = if (scaled >= 10) scaled.toInt().toString() else String.format(Locale.US, "%.1f", scaled)
        val trimmed = text.removeSuffix(".0")
        return "$trimmed$unit"
    }
}
