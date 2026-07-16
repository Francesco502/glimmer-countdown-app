package com.example.timeapk.notifications

import com.example.timeapk.data.Event

/**
 * Applies a CalendarProvider sync attempt without losing ownership of entries that
 * still need to be updated or removed. A failed provider attempt cannot prove that
 * an existing entry disappeared, so its known IDs remain authoritative.
 */
internal fun eventAfterScheduleSyncAttempt(
    event: Event,
    result: ScheduleSyncManager.ScheduleSyncResult
): Event = if (result.error.isNullOrBlank()) {
    event.copy(
        scheduleEventId = result.primaryScheduleEventId,
        targetCalendarId = result.targetCalendarId,
        lastScheduleSyncAt = result.lastSyncAt,
        lastScheduleSyncError = null
    )
} else {
    event.copy(
        scheduleEventId = event.scheduleEventId ?: result.primaryScheduleEventId,
        targetCalendarId = event.targetCalendarId ?: result.targetCalendarId,
        lastScheduleSyncAt = result.lastSyncAt,
        lastScheduleSyncError = result.error
    )
}
