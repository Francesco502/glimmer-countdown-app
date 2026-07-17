package com.example.timeapk.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MilestoneCalendarOwnershipPolicyTest {

    @Test
    fun freshDisabledState_withoutPendingOwnership_skipsProviderAndSavesState() {
        var providerCalls = 0
        val cleanup = cleanupPendingMilestoneOwnership(
            pendingOwnership = false,
            cleanup = {
                providerCalls += 1
                CalendarCleanupResult.PermissionRequired
            },
            clearPendingOwnership = { error("Nothing should be cleared") }
        )
        val state = completedRescheduleState(
            candidate = RescheduleState("prefs", emptyMap(), 123L),
            shouldRetry = !cleanup.isSuccess
        )

        assertEquals(CalendarCleanupResult.RemovedOrNotPresent, cleanup)
        assertEquals(0, providerCalls)
        assertNotNull(state)
    }

    @Test
    fun pendingOwnership_withoutPermission_retriesWithoutAdvancingState() {
        var pendingOwnershipCleared = false
        val cleanup = cleanupPendingMilestoneOwnership(
            pendingOwnership = true,
            cleanup = { CalendarCleanupResult.PermissionRequired },
            clearPendingOwnership = { pendingOwnershipCleared = true }
        )
        val state = completedRescheduleState(
            candidate = RescheduleState("prefs", emptyMap(), 123L),
            shouldRetry = !cleanup.isSuccess
        )

        assertEquals(CalendarCleanupResult.PermissionRequired, cleanup)
        assertFalse(pendingOwnershipCleared)
        assertNull(state)
    }

    @Test
    fun successfulPendingCleanup_clearsTrackedOwnership() {
        var pendingOwnershipCleared = false

        val cleanup = cleanupPendingMilestoneOwnership(
            pendingOwnership = true,
            cleanup = { CalendarCleanupResult.RemovedOrNotPresent },
            clearPendingOwnership = { pendingOwnershipCleared = true }
        )

        assertTrue(cleanup.isSuccess)
        assertTrue(pendingOwnershipCleared)
    }
}
