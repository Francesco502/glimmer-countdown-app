package com.example.timeapk.widget

import com.example.timeapk.ui.utils.DisplayModes
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetValueFormatterTest {

    @Test
    fun smallMode_todayAlwaysShowsZero() {
        val text = WidgetValueFormatter.numericValueForSmall(
            mode = DisplayModes.UNTIL_DAYS,
            isPast = false,
            isRepeating = false,
            isToday = true,
            daysElapsed = 0,
            daysPassed = 0,
            daysRemaining = 0,
            daysLeft = 0
        )
        assertEquals("0", text)
    }

    @Test
    fun smallMode_pastUsesNegativeNumber() {
        val text = WidgetValueFormatter.numericValueForSmall(
            mode = DisplayModes.PAST_DAYS,
            isPast = true,
            isRepeating = false,
            isToday = false,
            daysElapsed = 12,
            daysPassed = 0,
            daysRemaining = 0,
            daysLeft = 0
        )
        assertEquals("-12", text)
    }

    @Test
    fun smallMode_repeatingUsesDaysLeftForFuture() {
        val text = WidgetValueFormatter.numericValueForSmall(
            mode = DisplayModes.UNTIL_YMD,
            isPast = false,
            isRepeating = true,
            isToday = false,
            daysElapsed = 0,
            daysPassed = 0,
            daysRemaining = 99,
            daysLeft = 7
        )
        assertEquals("7", text)
    }

    @Test
    fun smallMode_largeNumbersAreCompactedWithinSixChars() {
        val text = WidgetValueFormatter.numericValueForSmall(
            mode = DisplayModes.UNTIL_DAYS,
            isPast = false,
            isRepeating = false,
            isToday = false,
            daysElapsed = 0,
            daysPassed = 0,
            daysRemaining = 123_456_789L,
            daysLeft = 0
        )
        assertTrue(text.length <= 6)
        assertTrue(text.endsWith("m") || text.endsWith("b") || text.endsWith("k"))
    }
}
