package com.example.timeapk.ui.reminder

import com.example.timeapk.R
import com.example.timeapk.data.Event
import com.example.timeapk.notifications.ScheduleSyncManager

data class ReminderStatusSummary(
    val level: ReminderStatusLevel,
    val messageKey: String,
    val detailResId: Int? = null,
    val primaryAction: ReminderStatusAction = ReminderStatusAction.None,
    val appReminderAvailable: Boolean = false,
    val scheduleSyncAvailable: Boolean = false
)

enum class ReminderStatusLevel {
    Ready,
    Warning,
    Error,
    Off
}

enum class ReminderStatusAction {
    None,
    EnableReminder,
    OpenNotificationSettings,
    OpenCalendarSettings,
    DisableScheduleSync,
    RebuildScheduleSync
}

fun buildReminderStatus(
    event: Event,
    notificationsEnabled: Boolean,
    calendarPermissionGranted: Boolean,
    hasWritableCalendar: Boolean
): ReminderStatusSummary {
    if (
        !event.syncToScheduleEnabled &&
        (event.scheduleEventId != null ||
            event.targetCalendarId != null ||
            !event.lastScheduleSyncError.isNullOrBlank())
    ) {
        return ReminderStatusSummary(
            level = ReminderStatusLevel.Error,
            messageKey = "reminder_status_schedule_sync_failed",
            detailResId = scheduleSyncDisplayDetail(event.lastScheduleSyncError),
            primaryAction = if (calendarPermissionGranted) {
                ReminderStatusAction.RebuildScheduleSync
            } else {
                ReminderStatusAction.OpenCalendarSettings
            },
            appReminderAvailable = event.remindEnabled && notificationsEnabled,
            scheduleSyncAvailable = false
        )
    }

    if (!event.remindEnabled) {
        return ReminderStatusSummary(
            level = ReminderStatusLevel.Off,
            messageKey = "reminder_status_off",
            primaryAction = ReminderStatusAction.EnableReminder
        )
    }

    if (!notificationsEnabled) {
        return ReminderStatusSummary(
            level = ReminderStatusLevel.Warning,
            messageKey = "reminder_status_notification_permission_needed",
            primaryAction = ReminderStatusAction.OpenNotificationSettings,
            appReminderAvailable = false,
            scheduleSyncAvailable = false
        )
    }

    if (!event.syncToScheduleEnabled) {
        return ReminderStatusSummary(
            level = ReminderStatusLevel.Ready,
            messageKey = "reminder_status_app_ready",
            appReminderAvailable = true,
            scheduleSyncAvailable = false
        )
    }

    if (!calendarPermissionGranted) {
        return ReminderStatusSummary(
            level = ReminderStatusLevel.Warning,
            messageKey = "reminder_status_calendar_permission_needed",
            primaryAction = ReminderStatusAction.OpenCalendarSettings,
            appReminderAvailable = true,
            scheduleSyncAvailable = false
        )
    }

    if (!hasWritableCalendar) {
        return ReminderStatusSummary(
            level = ReminderStatusLevel.Warning,
            messageKey = "reminder_status_no_writable_calendar",
            primaryAction = ReminderStatusAction.DisableScheduleSync,
            appReminderAvailable = true,
            scheduleSyncAvailable = false
        )
    }

    if (!event.lastScheduleSyncError.isNullOrBlank()) {
        return ReminderStatusSummary(
            level = ReminderStatusLevel.Error,
            messageKey = "reminder_status_schedule_sync_failed",
            detailResId = scheduleSyncDisplayDetail(event.lastScheduleSyncError),
            primaryAction = ReminderStatusAction.RebuildScheduleSync,
            appReminderAvailable = true,
            scheduleSyncAvailable = false
        )
    }

    return ReminderStatusSummary(
        level = ReminderStatusLevel.Ready,
        messageKey = if (event.lastScheduleSyncAt != null) {
            "reminder_status_app_and_schedule_ready"
        } else {
            "reminder_status_schedule_pending"
        },
        appReminderAvailable = true,
        scheduleSyncAvailable = event.lastScheduleSyncAt != null
    )
}

internal fun scheduleSyncDisplayDetail(rawError: String?): Int? {
    if (rawError.isNullOrBlank()) return null
    return when {
        rawError.contains("permission", ignoreCase = true) ->
            R.string.reminder_status_detail_calendar_permission
        ScheduleSyncManager.isNoWritableCalendarError(rawError) ->
            R.string.reminder_status_detail_no_writable_calendar
        else -> R.string.reminder_status_detail_calendar_failed
    }
}
