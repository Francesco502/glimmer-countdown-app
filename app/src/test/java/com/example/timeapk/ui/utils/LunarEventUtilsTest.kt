package com.example.timeapk.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LunarEventUtilsTest {

    @Test
    fun nextLunarOccurrence_beforeLunarNewYear_searchesCurrentLunarYear() {
        val origin = LocalDate.of(1996, 1, 25) // lunar 1995-12-06
        val pivot = LocalDate.of(2026, 1, 20) // still lunar year 2025

        val actual = getNextLunarOccurrence(origin, pivot)

        assertEquals(LocalDate.of(2026, 1, 24), actual)
    }

    @Test
    fun previousLunarOccurrence_beforeLunarNewYear_usesCurrentLunarYear() {
        val origin = LocalDate.of(1996, 1, 25)
        val pivot = LocalDate.of(2026, 1, 20)

        assertEquals(LocalDate.of(2025, 1, 5), getPreviousLunarOccurrence(origin, pivot))
    }

    @Test
    fun nextLunarOccurrence_onOccurrenceDay_returnsPivotDay() {
        val origin = LocalDate.of(1996, 1, 25)
        val pivot = LocalDate.of(2026, 1, 24)

        assertEquals(pivot, getNextLunarOccurrence(origin, pivot))
    }

    @Test
    fun nextLunarOccurrence_afterCurrentOccurrence_crossesGregorianYear() {
        val origin = LocalDate.of(1996, 1, 25) // lunar 1995-12-06
        val pivot = LocalDate.of(2026, 1, 25)

        assertEquals(LocalDate.of(2027, 1, 13), getNextLunarOccurrence(origin, pivot))
    }
}
