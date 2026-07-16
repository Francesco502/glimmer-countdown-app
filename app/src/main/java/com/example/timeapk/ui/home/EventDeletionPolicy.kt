package com.example.timeapk.ui.home

import com.example.timeapk.data.Event
import com.example.timeapk.notifications.CalendarCleanupResult
import kotlinx.coroutines.CancellationException

internal fun eventAfterCleanupAttempt(
    event: Event,
    result: CalendarCleanupResult,
    nowMillis: Long
): Event = when (result) {
    CalendarCleanupResult.RemovedOrNotPresent -> event.copy(
        scheduleEventId = null,
        targetCalendarId = null,
        lastScheduleSyncAt = nowMillis,
        lastScheduleSyncError = null
    )

    else -> event.copy(
        lastScheduleSyncAt = nowMillis,
        lastScheduleSyncError = result.message
    )
}

internal fun calendarCleanupRequired(event: Event): Boolean =
    event.syncToScheduleEnabled || event.scheduleEventId != null || event.targetCalendarId != null

sealed interface DeleteEventResult {
    data object Deleted : DeleteEventResult
    data class Blocked(val message: String) : DeleteEventResult
}

internal suspend fun deleteEventRecoverably(
    event: Event,
    nowMillis: () -> Long,
    cleanup: suspend (Event) -> CalendarCleanupResult,
    update: suspend (Event) -> Unit,
    cancelReminder: suspend (Event) -> Unit,
    cancelMilestones: suspend (Event) -> Unit,
    delete: suspend (Event) -> Unit,
    refreshWidgets: suspend () -> Unit
): DeleteEventResult {
    if (calendarCleanupRequired(event)) {
        val cleanupResult = try {
            cleanup(event)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            CalendarCleanupResult.ProviderFailure(
                error.message?.takeIf { it.isNotBlank() } ?: "Calendar cleanup failed"
            )
        }
        if (!cleanupResult.isSuccess) {
            val message = cleanupResult.message ?: "Calendar cleanup failed"
            val retryableEvent = eventAfterCleanupAttempt(event, cleanupResult, nowMillis())
            return try {
                update(retryableEvent)
                DeleteEventResult.Blocked(message)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                DeleteEventResult.Blocked(message)
            }
        }
    }

    val deletionResult = try {
        cancelReminder(event)
        cancelMilestones(event)
        delete(event)
        DeleteEventResult.Deleted
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        DeleteEventResult.Blocked(error.message ?: "Event deletion failed")
    }
    if (deletionResult is DeleteEventResult.Blocked) return deletionResult

    try {
        refreshWidgets()
    } catch (_: Exception) {
        // Room deletion is already committed; widget refresh is best-effort at this point.
    }
    return DeleteEventResult.Deleted
}
