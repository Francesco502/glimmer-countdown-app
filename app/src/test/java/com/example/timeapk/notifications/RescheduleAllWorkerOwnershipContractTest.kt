package com.example.timeapk.notifications

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RescheduleAllWorkerOwnershipContractTest {

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

    private fun source(name: String): String {
        return sourceAt("notifications/$name")
    }

    private fun sourceAt(relativePath: String): String {
        val direct = File("src/main/java/com/example/timeapk/$relativePath")
        return (if (direct.exists()) direct else File("app/${direct.path}")).readText()
    }
}
