package com.example.timeapk.ui.reminder

import com.example.timeapk.data.Event

data class ReminderStatusSummary(
    val level: ReminderStatusLevel,
    val messageKey: String,
    val detail: String? = null,
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
            detail = scheduleSyncDisplayDetail(event.lastScheduleSyncError),
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

internal fun scheduleSyncDisplayDetail(rawError: String?): String? {
    return rawError
        ?.takeIf { it.isNotBlank() }
        ?.let { "日历暂未接住此笺，可稍后再试。" }
}
