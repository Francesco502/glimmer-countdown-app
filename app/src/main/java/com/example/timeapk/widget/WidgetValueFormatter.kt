package com.example.timeapk.widget

import java.util.Locale

internal data class WidgetValueLayout(val text: String, val stacked: Boolean)

object WidgetValueFormatter {

    internal fun fitMeasuredValue(
        candidates: List<String>,
        availableWidth: Float,
        gap: Float,
        measure: (String) -> Float
    ): WidgetValueLayout {
        require(candidates.isNotEmpty())
        // Reserve half of the horizontal row for a readable title.
        candidates.firstOrNull { measure(it) <= (availableWidth - gap) / 2f }?.let {
            return WidgetValueLayout(it, stacked = false)
        }
        // The shortest semantic text may wrap, but must never lose direction or units.
        return WidgetValueLayout(
            candidates.firstOrNull { measure(it) <= availableWidth } ?: candidates.last(),
            stacked = true
        )
    }

    fun semanticValueOrFallback(
        preferredText: String,
        fallbackText: String,
        maxChars: Int
    ): String {
        if (preferredText.isBlank()) return fallbackText
        val visibleLength = preferredText.count { !it.isWhitespace() }
        return if (visibleLength <= maxChars) preferredText else fallbackText
    }

    internal fun compactDayCount(value: Long): String {
        val absValue = value.coerceAtLeast(0L)
        val compact = when {
            absValue < 100_000L -> absValue.toString()
            absValue < 1_000_000L -> "${absValue / 1_000L}k"
            absValue < 1_000_000_000L -> formatWithUnit(absValue, 1_000_000.0, "m")
            else -> formatWithUnit(absValue, 1_000_000_000.0, "b")
        }
        return compact
    }

    private fun formatWithUnit(value: Long, base: Double, unit: String): String {
        val scaled = value / base
        val text = if (scaled >= 10) scaled.toLong().toString() else String.format(Locale.US, "%.1f", scaled)
        val trimmed = text.removeSuffix(".0")
        return "$trimmed$unit"
    }
}
