package com.example.timeapk.ui.home

import com.example.timeapk.data.Event
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_YEARLY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class EventUiStateTest {
    private fun epochMillisOf(localDate: LocalDate): Long {
        return localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    @Test
    fun yearlyRepeat_started_shouldUseDaysSinceStartAsDaysPassed() {
        val today = LocalDate.now()
        val birthday = LocalDate.of(1998, 5, 2)
        val event = Event(
            title = "birthday",
            date = epochMillisOf(birthday),
            category = "birthday",
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
            title = "future anniversary",
            date = epochMillisOf(future),
            category = "anniversary",
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
            title = "future monthly",
            date = epochMillisOf(future),
            category = "other",
            repeatType = REPEAT_MONTHLY
        )

        val state = event.toEventUiState()

        assertFalse(state.isPast)
        assertEquals(0L, state.daysPassed)
        assertEquals(ChronoUnit.DAYS.between(today, future), state.daysRemaining)
    }

    @Test
    fun smartMilestones_shouldProvideDynamicNextValue() {
        val today = LocalDate.now()
        val start = today.minusDays(95)
        val event = Event(
            title = "progress event",
            date = epochMillisOf(start),
            category = CATEGORY_OTHER,
            repeatType = REPEAT_NONE
        )

        val stateWithSmart = event.toEventUiState(
            milestones = emptyList(),
            smartMilestonesEnabled = true
        )
        val stateWithoutSmart = event.toEventUiState(
            milestones = emptyList(),
            smartMilestonesEnabled = false
        )

        assertEquals(100L, stateWithSmart.nextMilestoneValue)
        assertEquals(5L, stateWithSmart.nextMilestoneDays)
        assertNull(stateWithoutSmart.nextMilestoneValue)
        assertNull(stateWithoutSmart.nextMilestoneDays)
    }

    @Test
    fun birthdaySmartMilestones_prefersHalfYearOverSmallDynamicStep() {
        val today = LocalDate.of(2026, 10, 9)
        val start = LocalDate.of(2026, 4, 11)
        val event = Event(
            title = "birthday",
            date = epochMillisOf(start),
            category = CATEGORY_BIRTHDAY,
            repeatType = REPEAT_NONE
        )

        val state = event.toEventUiState(
            milestones = emptyList(),
            smartMilestonesEnabled = true,
            now = today.atTime(12, 0).toInstant(ZoneOffset.UTC),
            zoneId = ZoneOffset.UTC
        )

        assertEquals(183L, state.nextMilestoneValue)
        assertEquals(2L, state.nextMilestoneDays)
        assertEquals(MilestoneReason.BIRTHDAY_HALF_YEAR, state.nextMilestoneReason)
    }

    @Test
    fun anniversarySmartMilestones_exposesYearlyReason() {
        val today = LocalDate.of(2024, 10, 8)
        val start = LocalDate.of(2023, 10, 9)
        val event = Event(
            title = "anniversary",
            date = epochMillisOf(start),
            category = CATEGORY_ANNIVERSARY,
            repeatType = REPEAT_NONE
        )

        val state = event.toEventUiState(
            milestones = emptyList(),
            smartMilestonesEnabled = true,
            now = today.atTime(12, 0).toInstant(ZoneOffset.UTC),
            zoneId = ZoneOffset.UTC
        )

        assertEquals(366L, state.nextMilestoneValue)
        assertEquals(1L, state.nextMilestoneDays)
        assertEquals(MilestoneReason.ANNIVERSARY_YEAR, state.nextMilestoneReason)
        assertEquals(1, state.nextMilestoneYears)
    }

    @Test
    fun futureCountdownSmartMilestones_skipsSameDayDynamicThreshold() {
        val today = LocalDate.now()
        val target = today.plusDays(95)
        val event = Event(
            title = "countdown",
            date = epochMillisOf(target),
            category = CATEGORY_OTHER,
            repeatType = REPEAT_NONE
        )

        val state = event.toEventUiState(
            milestones = emptyList(),
            smartMilestonesEnabled = true
        )

        assertEquals(90L, state.nextMilestoneValue)
        assertEquals(5L, state.nextMilestoneDays)
        assertEquals(MilestoneReason.COUNTDOWN_THRESHOLD, state.nextMilestoneReason)
    }

    @Test
    fun hoursRemaining_onlyShowsFutureDatesWithin24HoursAndRoundsUp() {
        val target = LocalDate.of(2026, 10, 10)
        val event = Event(title = "tomorrow", date = epochMillisOf(target), category = CATEGORY_OTHER)
        fun hoursAt(date: LocalDate, hour: Int, minute: Int) = event.toEventUiState(
            now = date.atTime(hour, minute).toInstant(ZoneOffset.UTC), zoneId = ZoneOffset.UTC
        ).hoursRemaining

        assertEquals(1L, hoursAt(target.minusDays(1), 23, 30))
        assertEquals(24L, hoursAt(target.minusDays(1), 0, 1))
        assertNull(hoursAt(target.minusDays(1), 0, 0))
        assertNull(hoursAt(target.minusDays(2), 12, 0))
        assertNull(hoursAt(target, 12, 0))
        assertNull(hoursAt(target.plusDays(1), 12, 0))
    }

    @Test
    fun hoursRemaining_usesActualDurationAcrossDaylightSavingChange() {
        val zone = ZoneId.of("America/New_York")
        val target = LocalDate.of(2026, 3, 9)
        val event = Event(title = "after DST", date = epochMillisOf(target), category = CATEGORY_OTHER)
        val state = event.toEventUiState(now = target.minusDays(1).atStartOfDay(zone).toInstant(), zoneId = zone)
        assertEquals(23L, state.hoursRemaining)
    }

    @Test
    fun anniversaryDates_preserveLeapDayAnchorAndLunarYearBoundary() {
        val leapDay = Event(title = "leap", date = epochMillisOf(LocalDate.of(2024, 2, 29)), category = CATEGORY_ANNIVERSARY)
        val leapAnniversaries = leapDay.upcomingAnniversaryMilestones(LocalDate.of(2025, 2, 27))
        assertEquals(AnniversaryMilestone(LocalDate.of(2025, 2, 28), 1), leapAnniversaries.first())
        assertEquals(LocalDate.of(2028, 2, 29), leapAnniversaries.first { it.years == 4 }.date)

        val lunar = Event(title = "lunar", date = epochMillisOf(LocalDate.of(1996, 1, 25)), category = CATEGORY_BIRTHDAY, isLunar = true)
        val next = lunar.upcomingAnniversaryMilestones(LocalDate.of(2026, 1, 20)).first()
        assertEquals(AnniversaryMilestone(LocalDate.of(2026, 1, 24), 30), next)
    }
}
