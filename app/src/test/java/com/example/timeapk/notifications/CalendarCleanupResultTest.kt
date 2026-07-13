package com.example.timeapk.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarCleanupResultTest {

    @Test
    fun cleanupFailureMessage_keepsPermissionAndProviderFailuresDistinct() {
        assertEquals("Calendar permission required", CalendarCleanupResult.PermissionRequired.message)
        assertEquals(
            "provider down",
            CalendarCleanupResult.ProviderFailure("provider down").message
        )
    }

    @Test
    fun cleanupSuccess_isOnlyRemovedOrNotPresent() {
        assertTrue(CalendarCleanupResult.RemovedOrNotPresent.isSuccess)
        assertFalse(CalendarCleanupResult.PermissionRequired.isSuccess)
        assertFalse(CalendarCleanupResult.ProviderFailure("x").isSuccess)
    }

    @Test
    fun cleanupExceptionMapping_mapsSecurityAndBoundsProviderMessages() {
        assertEquals(
            CalendarCleanupResult.PermissionRequired,
            ScheduleSyncManager.calendarCleanupFailureFor(SecurityException("denied"))
        )

        val result = ScheduleSyncManager.calendarCleanupFailureFor(IllegalStateException("x".repeat(240)))
        assertTrue(result is CalendarCleanupResult.ProviderFailure)
        assertEquals(180, result.message?.length)
    }

    @Test
    fun nullProviderQuery_mapsToProviderFailure() {
        val failure = assertThrows(IllegalStateException::class.java) {
            requireCalendarCleanupQuery<String>(null, "Calendar events")
        }

        assertEquals(
            CalendarCleanupResult.ProviderFailure("Calendar events query returned no cursor"),
            ScheduleSyncManager.calendarCleanupFailureFor(failure)
        )
    }
}
