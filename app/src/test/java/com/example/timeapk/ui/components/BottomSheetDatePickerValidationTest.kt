package com.example.timeapk.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BottomSheetDatePickerValidationTest {

    @Test
    fun parseSolarDateInput_returnsDateForValidInput() {
        assertEquals(
            LocalDate.of(2026, 2, 28),
            parseSolarDateInput("2026", "02", "28", 1900..2100)
        )
    }

    @Test
    fun parseSolarDateInput_returnsNullForImpossibleDate() {
        assertNull(parseSolarDateInput("2026", "02", "30", 1900..2100))
    }

    @Test
    fun parseSolarDateInput_returnsNullForOutOfRangeYear() {
        assertNull(parseSolarDateInput("2200", "02", "28", 1900..2100))
    }
}
