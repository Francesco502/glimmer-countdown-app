package com.example.timeapk.ui.home

import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_YEARLY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class EventUiStateTest {
    private fun epochMillisOf(localDate: LocalDate): Long {
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    @Test
    fun yearlyRepeat_started_shouldUseDaysSinceStartAsDaysPassed() {
        val today = LocalDate.now()
        val birthday = LocalDate.of(1998, 5, 2)
        val event = Event(
            title = "生日",
            date = epochMillisOf(birthday),
            category = "生日",
            repeatType = REPEAT_YEARLY
        )

        val state = event.toEventUiState()

        assertFalse(state.isPast)
        val expectedDaysPassed = if (!birthday.isAfter(today)) ChronoUnit.DAYS.between(birthday, today) else 0L
        assertEquals(expectedDaysPassed, state.daysPassed)

        val thisYear = birthday.withYear(today.year)
        val expectedNext = if (thisYear.isBefore(today)) thisYear.plusYears(1) else thisYear
        val expectedRemaining = ChronoUnit.DAYS.between(today, expectedNext)
        assertEquals(expectedRemaining, state.daysRemaining)
    }

    @Test
    fun yearlyRepeat_futureDate_shouldNotShowDaysPassed() {
        val today = LocalDate.now()
        val future = today.plusYears(2).plusDays(10)
        val event = Event(
            title = "未来纪念日",
            date = epochMillisOf(future),
            category = "纪念日",
            repeatType = REPEAT_YEARLY
        )

        val state = event.toEventUiState()

        assertFalse(state.isPast)
        assertEquals(0L, state.daysPassed)
        assertEquals(ChronoUnit.DAYS.between(today, future), state.daysRemaining)
    }

    @Test
    fun monthlyRepeat_futureDate_shouldNotShowDaysPassed() {
        val today = LocalDate.now()
        val future = today.plusDays(10)
        val event = Event(
            title = "未来每月事件",
            date = epochMillisOf(future),
            category = "其他",
            repeatType = REPEAT_MONTHLY
        )

        val state = event.toEventUiState()

        assertFalse(state.isPast)
        assertEquals(0L, state.daysPassed)
        assertEquals(ChronoUnit.DAYS.between(today, future), state.daysRemaining)
    }
}

