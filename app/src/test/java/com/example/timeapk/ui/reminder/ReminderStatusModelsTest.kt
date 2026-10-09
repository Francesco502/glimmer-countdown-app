package com.example.timeapk.ui.reminder

import com.example.timeapk.R
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderStatusModelsTest {

    @Test
    fun reminderStatus_reportsOffWhenEventReminderDisabled() {
        val status = buildReminderStatus(
            event = event(remindEnabled = false),
            notificationsEnabled = true,
            calendarPermissionGranted = true,
            hasWritableCalendar = true
        )

        assertEquals(ReminderStatusLevel.Off, status.level)
        assertEquals(ReminderStatusAction.EnableReminder, status.primaryAction)
    }

    @Test
    fun reminderStatus_reportsNotificationPermissionNeeded() {
        val status = buildReminderStatus(
            event = event(remindEnabled = true),
            notificationsEnabled = false,
            calendarPermissionGranted = true,
            hasWritableCalendar = true
        )

        assertEquals(ReminderStatusLevel.Warning, status.level)
        assertEquals(ReminderStatusAction.OpenNotificationSettings, status.primaryAction)
        assertTrue(status.messageKey.contains("notification"))
    }

    @Test
    fun reminderStatus_reportsCalendarPermissionNeededForScheduleSync() {
        val status = buildReminderStatus(
            event = event(remindEnabled = true, syncToScheduleEnabled = true),
            notificationsEnabled = true,
            calendarPermissionGranted = false,
            hasWritableCalendar = true
        )

        assertEquals(ReminderStatusLevel.Warning, status.level)
        assertEquals(ReminderStatusAction.OpenCalendarSettings, status.primaryAction)
    }

    @Test
    fun reminderStatus_reportsNoWritableCalendarButAppReminderAvailable() {
        val status = buildReminderStatus(
            event = event(remindEnabled = true, syncToScheduleEnabled = true),
            notificationsEnabled = true,
            calendarPermissionGranted = true,
            hasWritableCalendar = false
        )

        assertEquals(ReminderStatusLevel.Warning, status.level)
        assertEquals(ReminderStatusAction.DisableScheduleSync, status.primaryAction)
        assertTrue(status.appReminderAvailable)
    }

    @Test
    fun reminderStatus_reportsScheduleSyncError() {
        val status = buildReminderStatus(
            event = event(
                remindEnabled = true,
                syncToScheduleEnabled = true,
                lastScheduleSyncError = "provider failed"
            ),
            notificationsEnabled = true,
            calendarPermissionGranted = true,
            hasWritableCalendar = true
        )

        assertEquals(ReminderStatusLevel.Error, status.level)
        assertEquals(ReminderStatusAction.RebuildScheduleSync, status.primaryAction)
        assertEquals(R.string.reminder_status_detail_calendar_failed, status.detailResId)
    }

    @Test
    fun reminderStatus_reportsHealthyWhenReminderAndScheduleAreReady() {
        val status = buildReminderStatus(
            event = event(
                remindEnabled = true,
                syncToScheduleEnabled = true,
                lastScheduleSyncAt = 1_778_000_000_000L
            ),
            notificationsEnabled = true,
            calendarPermissionGranted = true,
            hasWritableCalendar = true
        )

        assertEquals(ReminderStatusLevel.Ready, status.level)
        assertEquals(ReminderStatusAction.None, status.primaryAction)
        assertTrue(status.appReminderAvailable)
        assertTrue(status.scheduleSyncAvailable)
    }

    @Test
    fun reminderStatus_reportsRetryableCleanupFailureWhenScheduleSyncWasDisabled() {
        val status = buildReminderStatus(
            event = event(
                remindEnabled = true,
                syncToScheduleEnabled = false,
                lastScheduleSyncAt = 1_778_000_000_000L,
                lastScheduleSyncError = "Calendar permission required"
            ).copy(scheduleEventId = 183L, targetCalendarId = 5L),
            notificationsEnabled = true,
            calendarPermissionGranted = false,
            hasWritableCalendar = false
        )

        assertEquals(ReminderStatusLevel.Error, status.level)
        assertEquals(ReminderStatusAction.OpenCalendarSettings, status.primaryAction)
        assertEquals(R.string.reminder_status_detail_calendar_permission, status.detailResId)
    }

    @Test
    fun reminderStatusDoesNotHideRetainedCleanupFailureWhenAppReminderIsOff() {
        val status = buildReminderStatus(
            event = event(
                remindEnabled = false,
                syncToScheduleEnabled = false,
                lastScheduleSyncError = "Calendar permission required"
            ).copy(scheduleEventId = 183L, targetCalendarId = 5L),
            notificationsEnabled = true,
            calendarPermissionGranted = false,
            hasWritableCalendar = false
        )

        assertEquals(ReminderStatusLevel.Error, status.level)
        assertEquals(ReminderStatusAction.OpenCalendarSettings, status.primaryAction)
        assertFalse(status.appReminderAvailable)
    }

    @Test
    fun scheduleSyncDetailsUseLocalizedResourceIdsForEachFailureKind() {
        assertNull(scheduleSyncDisplayDetail(null))
        assertNull(scheduleSyncDisplayDetail(" "))
        assertEquals(
            R.string.reminder_status_detail_calendar_permission,
            scheduleSyncDisplayDetail("Calendar READ permission required")
        )
        assertEquals(
            R.string.reminder_status_detail_no_writable_calendar,
            scheduleSyncDisplayDetail("No writable calendar")
        )
        assertEquals(
            R.string.reminder_status_detail_calendar_failed,
            scheduleSyncDisplayDetail("provider failed")
        )
    }

    @Test
    fun retainedCalendarFailureDoesNotClaimAppReminderIsAvailableWhenNotificationsAreBlocked() {
        val status = buildReminderStatus(
            event = event(
                remindEnabled = true,
                syncToScheduleEnabled = false,
                lastScheduleSyncError = "provider failed"
            ).copy(scheduleEventId = 183L),
            notificationsEnabled = false,
            calendarPermissionGranted = true,
            hasWritableCalendar = true
        )

        assertEquals(ReminderStatusLevel.Error, status.level)
        assertEquals(ReminderStatusAction.RebuildScheduleSync, status.primaryAction)
        assertFalse(status.appReminderAvailable)
    }

    private fun event(
        remindEnabled: Boolean,
        syncToScheduleEnabled: Boolean = false,
        lastScheduleSyncAt: Long? = null,
        lastScheduleSyncError: String? = null
    ): Event {
        return Event(
            id = 1,
            title = "event",
            date = 1_778_000_000_000L,
            category = CATEGORY_OTHER,
            remindEnabled = remindEnabled,
            syncToScheduleEnabled = syncToScheduleEnabled,
            lastScheduleSyncAt = lastScheduleSyncAt,
            lastScheduleSyncError = lastScheduleSyncError
        )
    }
}
