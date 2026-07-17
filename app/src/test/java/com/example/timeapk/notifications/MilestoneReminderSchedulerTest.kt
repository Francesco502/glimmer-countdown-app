package com.example.timeapk.notifications

import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_NONE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class MilestoneReminderSchedulerTest {

    private val zoneId: ZoneId = ZoneId.of("UTC")

    private fun epochMillisOf(localDate: LocalDate): Long =
        localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun externallyHandledCalendarCleanup_skipsOnlyMilestoneCalendarCleanup() {
        assertFalse(
            shouldClearCalendarBeforeMilestoneSync(calendarCleanupHandledExternally = true)
        )
    }

    @Test
    fun ordinaryMilestoneSync_keepsCalendarCleanupEnabled() {
        assertTrue(
            shouldClearCalendarBeforeMilestoneSync(calendarCleanupHandledExternally = false)
        )
    }

    @Test
    fun cleanupFailure_doesNotInsertMilestoneReplacement() = runBlocking {
        var insertionCalled = false

        val result = syncMilestoneCalendarReplacement(
            cleanup = { CalendarCleanupResult.ProviderFailure("provider down") },
            onCleanupFailure = { cleanup ->
                ScheduleSyncManager.MilestoneScheduleSyncResult(
                    scheduleEventId = null,
                    targetCalendarId = 7L,
                    lastSyncAt = 123L,
                    error = cleanup.message
                )
            },
            replacement = {
                insertionCalled = true
                ScheduleSyncManager.MilestoneScheduleSyncResult(99L, 7L, 123L, null)
            }
        )

        assertFalse(insertionCalled)
        assertEquals("provider down", result?.error)
    }

    @Test
    fun computeNextMilestoneReminderPlan_futureEventStillProducesUpcomingReminder() {
        val today = LocalDate.of(2026, 3, 17)
        val nowMillis = today.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()
        val eventDate = today.plusDays(1)
        val expectedReminderAt = eventDate.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()
        val event = Event(
            id = 1,
            title = "future milestone",
            date = epochMillisOf(eventDate),
            category = CATEGORY_OTHER,
            repeatType = REPEAT_NONE
        )

        val plan = computeNextMilestoneReminderPlan(
            event = event,
            milestones = listOf(1L),
            remindDaysAhead = 1,
            remindMinuteOfDay = 600,
            smartMilestonesEnabled = false,
            today = today,
            nowMillis = nowMillis,
            zoneId = zoneId
        )

        assertNotNull(plan)
        assertEquals(1L, plan!!.milestoneValue)
        assertEquals(expectedReminderAt, plan.remindAtMillis)
    }
}
