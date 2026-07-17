package com.example.timeapk.notifications

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RescheduleAllWorkerOwnershipContractTest {

    @Test
    fun unchangedFingerprints_withInflightOwnership_forceRecovery() {
        assertTrue(
            shouldForceMilestoneRecovery(
                activeOwnershipPending = true,
                inflightRecoveryPending = true,
                legacyScanPending = false
            )
        )
    }

    @Test
    fun unchangedFingerprints_withActiveOwnershipOnly_doNotForceColdStart() {
        assertFalse(
            shouldForceMilestoneRecovery(
                activeOwnershipPending = true,
                inflightRecoveryPending = false,
                legacyScanPending = false
            )
        )
    }

    @Test
    fun workerMergesEnabledSyncThroughOwnershipPolicy() {
        val source = source("RescheduleAllWorker.kt")

        assertTrue(source.contains("eventAfterScheduleSyncAttempt(event, syncResult)"))
        assertTrue(
            source.contains(
                "if (!syncResult.error.isNullOrBlank()) {\n" +
                    "                onFailure(IllegalStateException(syncResult.error))\n" +
                    "            }"
            )
        )
        assertFalse(source.contains("scheduleEventId = syncResult.primaryScheduleEventId"))
    }

    @Test
    fun workerDisabledPathUsesRecoverableCombinedCleanup() {
        val source = source("RescheduleAllWorker.kt")

        assertTrue(source.contains("} else if (calendarCleanupRequired(event)) {"))
        assertTrue(source.contains("ScheduleSyncManager.removeManagedCalendarEntries("))
        assertTrue(source.contains("eventAfterCleanupAttempt("))
        assertFalse(source.contains("ScheduleSyncManager.removeScheduleReminder(applicationContext, event.scheduleEventId)"))
    }

    @Test
    fun everyEnabledEventSyncCallPathUsesOwnershipPolicy() {
        listOf(
            "notifications/ReminderWorker.kt",
            "notifications/RescheduleAllWorker.kt",
            "ui/event/EventEntryViewModel.kt",
            "ui/home/HomeViewModel.kt",
            "ui/settings/SettingsSubScreens.kt"
        ).forEach { relativePath ->
            val source = sourceAt(relativePath)
            assertTrue(
                "$relativePath must merge through the ownership policy",
                source.contains("eventAfterScheduleSyncAttempt(")
            )
            assertFalse(
                "$relativePath must not copy possibly-null ownership from a failed result",
                source.contains("scheduleEventId = syncResult.primaryScheduleEventId")
            )
        }
    }

    @Test
    fun removedEventCleanupFailure_keepsFingerprintStatePendingForRetry() {
        val previous = RescheduleState(
            preferencesFingerprint = "prefs-old",
            eventFingerprints = mapOf(41 to "orphan", 42 to "active-old"),
            lastSuccessAt = 100L
        )
        val cleanupFailures = cleanupRemovedCalendarEntries(setOf(41)) {
            CalendarCleanupResult.PermissionRequired
        }
        val candidate = RescheduleState(
            preferencesFingerprint = "prefs-new",
            eventFingerprints = mapOf(42 to "active-new"),
            lastSuccessAt = 200L
        )

        val stateToPersist = completedRescheduleState(
            candidate = candidate,
            shouldRetry = cleanupFailures.isNotEmpty()
        )

        assertEquals(setOf(41), cleanupFailures.keys)
        assertNull(stateToPersist)
        assertEquals("orphan", previous.eventFingerprints[41])
    }

    @Test
    fun milestoneFailure_isMergedIntoReturnedEventBeforeRepositoryPersistence() {
        val primary = com.example.timeapk.data.Event(
            id = 9,
            title = "event",
            date = 1_800_000_000_000L,
            category = com.example.timeapk.data.CATEGORY_OTHER,
            scheduleEventId = 71L,
            targetCalendarId = 5L,
            lastScheduleSyncAt = 100L,
            lastScheduleSyncError = "primary cleanup failed"
        )

        val updated = eventAfterMilestoneScheduleSyncAttempt(
            primary,
            ScheduleSyncManager.MilestoneScheduleSyncResult(
                scheduleEventId = null,
                targetCalendarId = 5L,
                lastSyncAt = 200L,
                error = "milestone cleanup failed"
            )
        )

        assertEquals("primary cleanup failed; [Milestone] milestone cleanup failed", updated.lastScheduleSyncError)
        assertEquals(71L, updated.scheduleEventId)
    }

    private fun source(name: String): String {
        return sourceAt("notifications/$name")
    }

    private fun sourceAt(relativePath: String): String {
        val direct = File("src/main/java/com/example/timeapk/$relativePath")
        return (if (direct.exists()) direct else File("app/${direct.path}")).readText()
    }
}
