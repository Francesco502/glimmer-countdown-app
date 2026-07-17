package com.example.timeapk.ui.utils

import com.example.timeapk.data.REPEAT_DAILY
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_WEEKLY
import com.example.timeapk.data.REPEAT_YEARLY
import com.nlf.calendar.Solar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    // --- previousOccurrenceDate tests ---

    @Test
    fun previousOccurrence_weekly_nonOccurrenceDay_returnsPreviousWeek() {
        // Origin: Sunday 2025-06-01, today: Tuesday 2025-06-03
        val origin = LocalDate.of(2025, 6, 1)
        val today = LocalDate.of(2025, 6, 3)
        val result = previousOccurrenceDate(origin, today, REPEAT_WEEKLY)
        assertEquals(LocalDate.of(2025, 6, 1), result)
    }

    @Test
    fun previousOccurrence_weekly_onOccurrenceDay_returnsToday() {
        val origin = LocalDate.of(2025, 6, 1) // Sunday
        val today = LocalDate.of(2025, 6, 8)  // Next Sunday
        val result = previousOccurrenceDate(origin, today, REPEAT_WEEKLY)
        assertEquals(today, result)
    }

    @Test
    fun previousOccurrence_monthly_nonOccurrenceDay_returnsPreviousMonth() {
        val origin = LocalDate.of(2025, 1, 15)
        val today = LocalDate.of(2025, 3, 20)
        val result = previousOccurrenceDate(origin, today, REPEAT_MONTHLY)
        assertEquals(LocalDate.of(2025, 3, 15), result)
    }

    @Test
    fun previousOccurrence_halfYearly_nonOccurrenceDay_returnsPreviousHalfYear() {
        val origin = LocalDate.of(2025, 1, 10)
        val today = LocalDate.of(2025, 8, 15)
        val result = previousOccurrenceDate(origin, today, REPEAT_HALF_YEARLY)
        assertEquals(LocalDate.of(2025, 7, 10), result)
    }

    @Test
    fun previousOccurrence_yearly_nonOccurrenceDay_returnsPreviousYear() {
        val origin = LocalDate.of(2020, 3, 15)
        val today = LocalDate.of(2025, 7, 1)
        val result = previousOccurrenceDate(origin, today, REPEAT_YEARLY)
        assertEquals(LocalDate.of(2025, 3, 15), result)
    }

    @Test
    fun previousOccurrence_calendarRepeats_preserveOriginAnchorAfterClamping() {
        assertEquals(
            LocalDate.of(2024, 2, 29),
            previousOccurrenceDate(
                origin = LocalDate.of(2020, 2, 29),
                today = LocalDate.of(2025, 2, 27),
                repeatType = REPEAT_YEARLY
            )
        )
        assertEquals(
            LocalDate.of(2025, 1, 31),
            previousOccurrenceDate(
                origin = LocalDate.of(2025, 1, 31),
                today = LocalDate.of(2025, 2, 27),
                repeatType = REPEAT_MONTHLY
            )
        )
        assertEquals(
            LocalDate.of(2024, 8, 31),
            previousOccurrenceDate(
                origin = LocalDate.of(2024, 8, 31),
                today = LocalDate.of(2025, 2, 27),
                repeatType = REPEAT_HALF_YEARLY
            )
        )
    }

    @Test
    fun previousOccurrence_originAfterToday_returnsNull() {
        val origin = LocalDate.of(2026, 1, 1)
        val today = LocalDate.of(2025, 6, 1)
        val result = previousOccurrenceDate(origin, today, REPEAT_YEARLY)
        assertNull(result)
    }

    @Test
    fun previousOccurrence_noneRepeat_returnsOrigin() {
        val origin = LocalDate.of(2025, 1, 1)
        val today = LocalDate.of(2025, 6, 1)
        val result = previousOccurrenceDate(origin, today, REPEAT_NONE)
        assertEquals(origin, result)
    }
}
