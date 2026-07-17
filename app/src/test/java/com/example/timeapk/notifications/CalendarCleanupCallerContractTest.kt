package com.example.timeapk.notifications

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarCleanupCallerContractTest {

    @Test
    fun scheduleSyncChecksCleanupFailuresBeforeReportingSuccess() {
        val source = source("notifications/ScheduleSyncManager.kt")

        assertTrue(source.contains("cleanupReminderSeriesEntries("))
        assertTrue(source.contains("cleanupResult.message"))
        assertFalse(source.contains("(allExistingIds - usedIds).forEach { staleId ->\n                        removeScheduleReminder(context, staleId)"))
    }

    @Test
    fun milestoneReplacementIsGuardedByCleanupResult() {
        val source = source("notifications/MilestoneReminderScheduler.kt")

        assertTrue(source.contains("syncMilestoneCalendarReplacement("))
    }

    @Test
    fun removedEventCleanupUsesOneCheckedManagedCleanupResult() {
        val source = source("notifications/RescheduleAllWorker.kt")

        assertTrue(source.contains("cleanupRemovedCalendarEntries("))
        assertFalse(source.contains("ScheduleSyncManager.removeScheduleReminderByEventId(applicationContext, removedId)"))
        assertFalse(source.contains("ScheduleSyncManager.clearMilestoneScheduleRemindersByEventId(applicationContext, removedId)"))
    }

    @Test
    fun adjacentRestoreImportAndSaveCallersDoNotDiscardCleanupResults() {
        val home = source("ui/home/HomeViewModel.kt")
        val settings = source("ui/settings/SettingsSubScreens.kt")
        val eventEntry = source("ui/event/EventEntryViewModel.kt")

        assertFalse(home.contains("                    ScheduleSyncManager.removeScheduleReminderByEventId(application, savedEvent.id)\n"))
        assertFalse(settings.contains("                    ScheduleSyncManager.removeScheduleReminderByEventId(context, savedEvent.id)\n"))
        assertFalse(eventEntry.contains("                ScheduleSyncManager.clearMilestoneScheduleRemindersByEventId(application, updatedEvent.id)\n"))
    }

    private fun source(relativePath: String): String {
        val direct = File("src/main/java/com/example/timeapk/$relativePath")
        return (if (direct.exists()) direct else File("app/${direct.path}")).readText()
    }
}
