package com.example.timeapk.notifications

import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_DAILY
import com.example.timeapk.data.REPEAT_WEEKLY
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReminderDateCalculatorEdgeTest {

    private val zoneId: ZoneId = ZoneId.of("UTC")

    @Test
    fun dailyRepeat_withLargeDaysBefore_stillComputesNextTrigger() {
        val nowMillis = LocalDate.of(2026, 3, 6)
            .atTime(10, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val event = Event(
            id = 1,
            title = "daily",
            date = LocalDate.of(2024, 1, 1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            category = CATEGORY_OTHER,
            repeatType = REPEAT_DAILY,
            remindEnabled = true,
            remindDaysBefore = 2500,
            reminderTimeMinutesOfDay = 540
        )

        val trigger = computeNextReminderTriggerAtMillis(event, nowMillis, zoneId)
        assertNotNull(trigger)
        assertTrue(trigger!! > nowMillis)
    }

    @Test
    fun weeklyRepeat_withLargeDaysBefore_stillComputesNextTrigger() {
        val nowMillis = LocalDate.of(2026, 3, 6)
            .atTime(10, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val event = Event(
            id = 2,
            title = "weekly",
            date = LocalDate.of(2024, 1, 1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            category = CATEGORY_OTHER,
            repeatType = REPEAT_WEEKLY,
            remindEnabled = true,
            remindDaysBefore = 1500,
            reminderTimeMinutesOfDay = 600
        )

        val trigger = computeNextReminderTriggerAtMillis(event, nowMillis, zoneId)
        assertNotNull(trigger)
        assertTrue(trigger!! > nowMillis)
    }
}
