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

    @Test
    fun workerOriginAvoidsSelfCancellationAndHandsFailureToRescheduleWorker() {
        val worker = source("notifications/MilestoneReminderWorker.kt")
        val scheduler = source("notifications/MilestoneReminderScheduler.kt")

        assertTrue(worker.contains("MilestoneSyncOrigin.WORKER_AFTER_NOTIFICATION"))
        assertTrue(worker.contains("RescheduleAllWorker.enqueue("))
        assertFalse(worker.contains("Result.retry()"))
        assertTrue(scheduler.contains("shouldCancelMilestoneWorkBeforeSync(origin)"))
    }

    @Test
    fun milestoneStatusIsMergedAndCallersInspectErrors() {
        val scheduler = source("notifications/MilestoneReminderScheduler.kt")
        val reschedule = source("notifications/RescheduleAllWorker.kt")
        val eventEntry = source("ui/event/EventEntryViewModel.kt")
        val home = source("ui/home/HomeViewModel.kt")
        val settings = source("ui/settings/SettingsSubScreens.kt")

        assertTrue(scheduler.contains("eventAfterMilestoneScheduleSyncAttempt("))
        assertTrue(reschedule.contains("updatedEvent = eventAfterMilestoneScheduleSyncAttempt("))
        assertTrue(eventEntry.contains("val milestoneResult = syncMilestoneReminderForEvent("))
        assertTrue(home.contains("val milestoneResult = try {"))
        assertTrue(home.contains("syncMilestoneReminderForEvent(application, updatedEvent)"))
        assertTrue(settings.contains("val milestoneResult = rescheduleMilestoneReminders(app)"))
    }

    @Test
    fun clearAllMilestonesReturnsAndPropagatesCleanupFailure() {
        val manager = source("notifications/ScheduleSyncManager.kt")
        val scheduler = source("notifications/MilestoneReminderScheduler.kt")
        val reschedule = source("notifications/RescheduleAllWorker.kt")

        assertTrue(manager.contains("fun clearAllMilestoneScheduleReminders(context: Context): CalendarCleanupResult"))
        assertTrue(scheduler.contains("val cleanup = clearAllPendingMilestoneCalendarOwnership(application)"))
        assertTrue(reschedule.contains("val cleanup = clearAllPendingMilestoneCalendarOwnership(applicationContext)"))
    }

    @Test
    fun milestoneCleanupIsGatedByTrackedPendingOwnership() {
        val scheduler = source("notifications/MilestoneReminderScheduler.kt")
        val ownership = source("notifications/MilestoneCalendarOwnership.kt")
        val reschedule = source("notifications/RescheduleAllWorker.kt")
        val reminderWorker = source("notifications/ReminderWorker.kt")
        val eventEntry = source("ui/event/EventEntryViewModel.kt")

        assertTrue(scheduler.contains("MilestoneCalendarOwnershipStore.markPendingDurably(context, event.id)"))
        assertTrue(ownership.contains("cleanupPendingMilestoneOwnership("))
        assertTrue(reschedule.contains("clearAllPendingMilestoneCalendarOwnership("))
        assertTrue(reschedule.contains("recordManagedCalendarCleanupForMilestoneOwnership("))
        assertTrue(reminderWorker.contains("recordManagedCalendarCleanupForMilestoneOwnership("))
        assertTrue(eventEntry.contains("recordManagedCalendarCleanupForMilestoneOwnership("))
        assertTrue(eventEntry.contains("clearPendingMilestoneCalendarOwnership("))
        assertTrue(reminderWorker.contains("repairReason = \"reminder_worker_cleanup\""))
        assertTrue(eventEntry.contains("repairReason = \"manual_event_save_cleanup\""))
    }

    private fun source(relativePath: String): String {
        val direct = File("src/main/java/com/example/timeapk/$relativePath")
        return (if (direct.exists()) direct else File("app/${direct.path}")).readText()
    }
}
