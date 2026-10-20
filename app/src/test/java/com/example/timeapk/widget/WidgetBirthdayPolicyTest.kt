package com.example.timeapk.widget

import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.ui.home.EventUiState
import com.example.timeapk.ui.home.toEventUiState
import com.example.timeapk.ui.utils.DisplayModes
import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class WidgetBirthdayPolicyTest {
    @Test
    fun existingBirthdayDefaultsToNextBirthdayDespiteSavedPastMode() {
        val state = birthday(LocalDate.of(1995, 12, 31), LocalDate.of(2026, 10, 20))

        assertEquals(72L, state.daysLeft)
        assertTrue(state.daysPassed > 10_000)
        assertEquals(DisplayModes.UNTIL_DAYS, resolve(state, DISPLAY_POLICY_AUTO))
        assertEquals(DisplayModes.PAST_DAYS, resolve(state, DISPLAY_POLICY_FOLLOW_APP))
        assertEquals(DisplayModes.UNTIL_DAYS, resolve(state, DISPLAY_POLICY_COUNTDOWN))
        assertEquals(DisplayModes.UNTIL_DAYS, resolve(state, 99))
    }

    @Test
    fun autoPreservesOtherEventsAndDoesNotRepeatSingleBirthdays() {
        val birthday = birthday(LocalDate.of(2000, 3, 10), LocalDate.of(2026, 3, 11))
        val anniversary = birthday.copy(event = birthday.event.copy(category = CATEGORY_ANNIVERSARY))
        val single = birthday.event.copy(repeatType = REPEAT_NONE)
            .toEventUiState(now = LocalDate.of(2026, 3, 11).atStartOfDay(ZoneOffset.UTC).toInstant(), zoneId = ZoneOffset.UTC)

        assertEquals(DisplayModes.PAST_DAYS, resolve(anniversary, DISPLAY_POLICY_AUTO))
        assertEquals(DisplayModes.PAST_DAYS, resolve(single, DISPLAY_POLICY_AUTO))
        assertEquals(DisplayModes.PAST_DAYS, resolve(single, DISPLAY_POLICY_COUNTDOWN))
        assertTrue(single.isPast)
    }

    @Test
    fun birthdayTodayAndNextDayRollForwardWithoutChangingOrigin() {
        val origin = LocalDate.of(2000, 10, 20)
        val birthdayDay = birthday(origin, LocalDate.of(2026, 10, 20))
        val nextDay = birthday(origin, LocalDate.of(2026, 10, 21))

        assertEquals(0L, birthdayDay.daysLeft)
        assertFalse(birthdayDay.isPast)
        assertEquals(LocalDate.of(2027, 10, 20), nextDay.nextOccurrenceDate)
        assertEquals(364L, nextDay.daysLeft)
        assertEquals(DisplayModes.UNTIL_DAYS, resolve(nextDay, DISPLAY_POLICY_AUTO))
    }

    @Test
    fun leapBirthdayPreservesFebruaryAnchorAcrossNonLeapAndLeapYears() {
        val origin = LocalDate.of(2000, 2, 29)
        val nonLeap = birthday(origin, LocalDate.of(2027, 2, 27))
        val nextLeap = birthday(origin, LocalDate.of(2027, 3, 1))

        assertEquals(LocalDate.of(2027, 2, 28), nonLeap.nextOccurrenceDate)
        assertEquals(1L, nonLeap.daysLeft)
        assertEquals(LocalDate.of(2028, 2, 29), nextLeap.nextOccurrenceDate)
        assertEquals(DisplayModes.UNTIL_DAYS, resolve(nextLeap, DISPLAY_POLICY_AUTO))
    }

    @Test
    fun lunarBirthdayUsesLunarNewYearBoundaryAndRollsAfterBirthday() {
        val origin = LocalDate.of(1996, 1, 25)
        val before = birthday(origin, LocalDate.of(2026, 1, 20), lunar = true)
        val onDay = birthday(origin, LocalDate.of(2026, 1, 24), lunar = true)
        val after = birthday(origin, LocalDate.of(2026, 1, 25), lunar = true)

        assertEquals(LocalDate.of(2026, 1, 24), before.nextOccurrenceDate)
        assertEquals(4L, before.daysLeft)
        assertEquals(0L, onDay.daysLeft)
        assertEquals(LocalDate.of(2027, 1, 13), after.nextOccurrenceDate)
        assertEquals(353L, after.daysLeft)
        assertEquals(DisplayModes.UNTIL_DAYS, resolve(after, DISPLAY_POLICY_AUTO))
    }

    @Test
    fun lunarLeapMonthKeepsExistingSkipYearConvention() {
        val originSolar = Lunar.fromYmd(2023, -2, 10).solar
        val origin = LocalDate.of(originSolar.year, originSolar.month, originSolar.day)
        val today = LocalDate.of(2024, 1, 1)
        val state = birthday(origin, today, lunar = true)
        val nextLunar = Solar.fromYmd(state.nextOccurrenceDate.year, state.nextOccurrenceDate.monthValue, state.nextOccurrenceDate.dayOfMonth).lunar

        assertEquals(-2, nextLunar.month)
        assertEquals(10, nextLunar.day)
        assertTrue(state.daysLeft > 366L)
        assertEquals(ChronoUnit.DAYS.between(today, state.nextOccurrenceDate), state.daysLeft)
        assertEquals(DisplayModes.UNTIL_DAYS, resolve(state, DISPLAY_POLICY_AUTO))
    }

    @Test
    fun missingLunarThirtiethKeepsExistingSkipYearConvention() {
        val today = LocalDate.of(2025, 1, 29)
        val state = birthday(LocalDate.of(2024, 4, 8), today, lunar = true)
        val lunar = Solar.fromYmd(state.nextOccurrenceDate.year, state.nextOccurrenceDate.monthValue, state.nextOccurrenceDate.dayOfMonth).lunar

        assertTrue(runCatching { Lunar.fromYmd(2025, 2, 30) }.isFailure)
        assertTrue(runCatching { Lunar.fromYmd(2026, 2, 30) }.isFailure)
        assertEquals(LocalDate.of(2027, 4, 6), state.nextOccurrenceDate)
        assertEquals(2, lunar.month)
        assertEquals(30, lunar.day)
        assertEquals(2027, lunar.year)
        assertEquals(ChronoUnit.DAYS.between(today, LocalDate.of(2027, 4, 6)), state.daysLeft)
        assertEquals(DisplayModes.UNTIL_DAYS, resolve(state, DISPLAY_POLICY_AUTO))
    }

    @Test
    fun midnightAndTimeZoneRefreshRecomputesNextOccurrence() {
        val event = Event(title = "birthday", date = epoch(LocalDate.of(2000, 10, 20)), category = CATEGORY_BIRTHDAY, repeatType = REPEAT_YEARLY)
        val instant = java.time.Instant.parse("2026-10-20T16:00:00Z")
        val utc = event.toEventUiState(now = instant, zoneId = ZoneOffset.UTC)
        val hongKong = event.toEventUiState(now = instant, zoneId = ZoneOffset.ofHours(8))

        assertEquals(0L, utc.daysLeft)
        assertEquals(364L, hongKong.daysLeft)
        assertEquals(DisplayModes.UNTIL_DAYS, resolve(hongKong, DISPLAY_POLICY_AUTO))
    }

    private fun resolve(state: EventUiState, policy: Int): Int =
        WidgetContentResolver.resolveDisplayMode(state, DisplayModes.PAST_DAYS, showMilestone = false, displayPolicy = policy)

    private fun birthday(origin: LocalDate, today: LocalDate, lunar: Boolean = false): EventUiState =
        Event(title = "birthday", date = epoch(origin), category = CATEGORY_BIRTHDAY, repeatType = REPEAT_YEARLY, isLunar = lunar)
            .toEventUiState(now = today.atStartOfDay(ZoneOffset.UTC).toInstant(), zoneId = ZoneOffset.UTC)

    private fun epoch(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
