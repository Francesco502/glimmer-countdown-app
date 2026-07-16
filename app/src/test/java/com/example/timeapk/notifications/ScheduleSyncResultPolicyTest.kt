package com.example.timeapk.notifications

import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleSyncResultPolicyTest {

    @Test
    fun failureRetainsKnownCalendarOwnershipAndRecordsAttempt() {
        val event = ownedEvent()

        val actual = eventAfterScheduleSyncAttempt(
            event,
            ScheduleSyncManager.ScheduleSyncResult(
                primaryScheduleEventId = null,
                targetCalendarId = null,
                lastSyncAt = 456L,
                error = "No writable calendar"
            )
        )

        assertEquals(183L, actual.scheduleEventId)
        assertEquals(5L, actual.targetCalendarId)
        assertEquals(456L, actual.lastScheduleSyncAt)
        assertEquals("No writable calendar", actual.lastScheduleSyncError)
    }

    @Test
    fun successAcceptsReplacementOwnership() {
        val actual = eventAfterScheduleSyncAttempt(
            ownedEvent(),
            ScheduleSyncManager.ScheduleSyncResult(200L, 8L, 789L, null)
        )

        assertEquals(200L, actual.scheduleEventId)
        assertEquals(8L, actual.targetCalendarId)
        assertEquals(789L, actual.lastScheduleSyncAt)
        assertNull(actual.lastScheduleSyncError)
    }

    @Test
    fun successfulCleanupMayClearOwnership() {
        val actual = eventAfterScheduleSyncAttempt(
            ownedEvent(),
            ScheduleSyncManager.ScheduleSyncResult(null, null, 999L, null)
        )

        assertNull(actual.scheduleEventId)
        assertNull(actual.targetCalendarId)
        assertEquals(999L, actual.lastScheduleSyncAt)
        assertNull(actual.lastScheduleSyncError)
    }

    private fun ownedEvent() = Event(
        id = 1,
        title = "event",
        date = 1_800_000_000_000L,
        category = CATEGORY_OTHER,
        scheduleEventId = 183L,
        targetCalendarId = 5L,
        lastScheduleSyncAt = 123L
    )
}
