package com.example.timeapk.ui.utils

import com.example.timeapk.data.REPEAT_MONTHLY
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LunarMonthlyOccurrenceTest {
    @Test
    fun dayThirtyClampsInTheShortMonthThenReturnsToThirty() {
        val origin = LocalDate.of(2020, 1, 24)
        val february = LocalDate.of(2020, 2, 22)
        val march = LocalDate.of(2020, 3, 23)
        assertEquals(february, nextOccurrenceDate(origin, origin.plusDays(1), REPEAT_MONTHLY, isLunar = true))
        assertEquals(march, nextOccurrenceDate(origin, february.plusDays(1), REPEAT_MONTHLY, isLunar = true))
        assertEquals(february, previousOccurrenceDate(origin, march.minusDays(1), REPEAT_MONTHLY, isLunar = true))
        assertEquals(2L, lunarMonthsBetween(origin, march))
    }

    @Test
    fun leapMonthIsItsOwnOccurrence() {
        val origin = LocalDate.of(2023, 2, 20)
        val leap = LocalDate.of(2023, 3, 22)
        assertEquals(leap, nextOccurrenceDate(origin, origin.plusDays(1), REPEAT_MONTHLY, isLunar = true))
        assertEquals(origin, previousOccurrenceDate(origin, leap.minusDays(1), REPEAT_MONTHLY, isLunar = true))
        assertEquals(1L, lunarMonthsBetween(origin, leap))
    }
}
