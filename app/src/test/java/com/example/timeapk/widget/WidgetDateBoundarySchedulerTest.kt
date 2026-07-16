package com.example.timeapk.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class WidgetDateBoundarySchedulerTest {
    @Test
    fun exactMidnightSchedulesTheFollowingLocalMidnight() {
        val zone = ZoneId.of("Asia/Hong_Kong")
        val now = ZonedDateTime.of(2026, 7, 16, 0, 0, 0, 0, zone).toInstant()

        val result = Instant.ofEpochMilli(WidgetDateBoundaryScheduler.nextLocalDateStartMillis(now, zone))

        assertEquals(ZonedDateTime.of(2026, 7, 17, 0, 0, 0, 0, zone).toInstant(), result)
        assertTrue(result.isAfter(now))
    }

    @Test
    fun springForwardUsesNextCivilDateStartAcrossTwentyThreeHourDay() {
        val zone = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 3, 8, 0, 0, 0, 0, zone).toInstant()

        val result = Instant.ofEpochMilli(WidgetDateBoundaryScheduler.nextLocalDateStartMillis(now, zone))

        assertEquals(23, Duration.between(now, result).toHours())
        assertEquals("2026-03-09T00:00-04:00[America/New_York]", result.atZone(zone).toString())
    }

    @Test
    fun fallBackUsesNextCivilDateStartAcrossTwentyFiveHourDay() {
        val zone = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 11, 1, 0, 0, 0, 0, zone).toInstant()

        val result = Instant.ofEpochMilli(WidgetDateBoundaryScheduler.nextLocalDateStartMillis(now, zone))

        assertEquals(25, Duration.between(now, result).toHours())
        assertEquals("2026-11-02T00:00-05:00[America/New_York]", result.atZone(zone).toString())
    }
}
