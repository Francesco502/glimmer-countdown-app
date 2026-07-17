package com.example.timeapk.notifications

import android.content.Context

internal fun cleanupPendingMilestoneOwnership(
    pendingOwnership: Boolean,
    cleanup: () -> CalendarCleanupResult,
    clearPendingOwnership: () -> Unit
): CalendarCleanupResult {
    if (!pendingOwnership) return CalendarCleanupResult.RemovedOrNotPresent
    val result = cleanup()
    if (result.isSuccess) {
        clearPendingOwnership()
    }
    return result
}

internal object MilestoneCalendarOwnershipStore {
    private const val PREFS_NAME = "milestone_calendar_ownership"
    private const val KEY_PENDING_EVENT_IDS = "pending_event_ids"
    private val lock = Any()

    fun markPending(context: Context, eventId: Int) = synchronized(lock) {
        val ids = pendingEventIds(context).toMutableSet()
        if (ids.add(eventId)) {
            writePendingEventIds(context, ids)
        }
    }

    fun clearEventIfPending(
        context: Context,
        eventId: Int,
        cleanup: () -> CalendarCleanupResult
    ): CalendarCleanupResult = synchronized(lock) {
        cleanupPendingMilestoneOwnership(
            pendingOwnership = eventId in pendingEventIds(context),
            cleanup = cleanup,
            clearPendingOwnership = {
                val ids = pendingEventIds(context).toMutableSet()
                ids.remove(eventId)
                writePendingEventIds(context, ids)
            }
        )
    }

    fun clearAllIfPending(
        context: Context,
        cleanup: () -> CalendarCleanupResult
    ): CalendarCleanupResult = synchronized(lock) {
        cleanupPendingMilestoneOwnership(
            pendingOwnership = pendingEventIds(context).isNotEmpty(),
            cleanup = cleanup,
            clearPendingOwnership = { writePendingEventIds(context, emptySet()) }
        )
    }

    fun recordManagedCleanup(context: Context, eventId: Int, result: CalendarCleanupResult) {
        if (!result.isSuccess) return
        synchronized(lock) {
            val ids = pendingEventIds(context).toMutableSet()
            if (ids.remove(eventId)) {
                writePendingEventIds(context, ids)
            }
        }
    }

    private fun pendingEventIds(context: Context): Set<Int> {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_PENDING_EVENT_IDS, emptySet())
            .orEmpty()
            .mapNotNull(String::toIntOrNull)
            .toSet()
    }

    private fun writePendingEventIds(context: Context, eventIds: Set<Int>) {
        val editor = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
        if (eventIds.isEmpty()) {
            editor.remove(KEY_PENDING_EVENT_IDS)
        } else {
            editor.putStringSet(KEY_PENDING_EVENT_IDS, eventIds.map(Int::toString).toSet())
        }
        editor.apply()
    }
}

internal fun clearPendingMilestoneCalendarOwnership(
    context: Context,
    eventId: Int
): CalendarCleanupResult = MilestoneCalendarOwnershipStore.clearEventIfPending(context, eventId) {
    ScheduleSyncManager.clearMilestoneScheduleRemindersByEventId(context, eventId)
}

internal fun clearAllPendingMilestoneCalendarOwnership(context: Context): CalendarCleanupResult =
    MilestoneCalendarOwnershipStore.clearAllIfPending(context) {
        ScheduleSyncManager.clearAllMilestoneScheduleReminders(context)
    }

internal fun recordManagedCalendarCleanupForMilestoneOwnership(
    context: Context,
    eventId: Int,
    result: CalendarCleanupResult
): CalendarCleanupResult = result.also {
    MilestoneCalendarOwnershipStore.recordManagedCleanup(context, eventId, it)
}
