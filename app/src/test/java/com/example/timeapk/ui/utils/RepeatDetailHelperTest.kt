package com.example.timeapk.ui.utils

import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.nlf.calendar.Solar
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RepeatDetailHelperTest {

    @Test
    fun monthlyRepeat_onOccurrenceDay_returnsToday() {
        val origin = LocalDate.of(2025, 1, 31)
        val today = LocalDate.of(2025, 2, 28)

        val next = nextOccurrenceDate(origin, today, REPEAT_MONTHLY)

        assertEquals(today, next)
    }

    @Test
    fun monthlyRepeat_crossShortMonth_neverReturnsPastDate() {
        val origin = LocalDate.of(2025, 1, 31)
        val today = LocalDate.of(2025, 3, 30)

        val next = nextOccurrenceDate(origin, today, REPEAT_MONTHLY)

        assertEquals(LocalDate.of(2025, 3, 31), next)
    }

    @Test
    fun halfYearlyRepeat_onOccurrenceDay_returnsToday() {
        val origin = LocalDate.of(2025, 3, 6)
        val today = LocalDate.of(2025, 9, 6)

        val next = nextOccurrenceDate(origin, today, REPEAT_HALF_YEARLY)

        assertEquals(today, next)
    }

    @Test
    fun formatLunarMonthDay_matchesLunarLibraryValue() {
        val date = LocalDate.of(2024, 2, 10)
        val lunar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar

        assertEquals("${lunar.monthInChinese}${lunar.dayInChinese}", formatLunarMonthDay(date))
    }
}
