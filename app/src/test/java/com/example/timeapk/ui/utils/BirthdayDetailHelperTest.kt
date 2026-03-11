package com.example.timeapk.ui.utils

import com.nlf.calendar.Solar
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BirthdayDetailHelperTest {

    @Test
    fun zodiacAnimal_matchesLunarLibraryValue() {
        val date = LocalDate.of(2024, 2, 10)
        val expected = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar.yearShengXiao

        assertEquals(expected, zodiacAnimalFromDate(date))
    }
}
