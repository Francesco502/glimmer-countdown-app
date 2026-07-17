package com.example.timeapk.notifications

import android.content.Context
import android.content.SharedPreferences

internal enum class MilestoneCleanupScope {
    EVENT,
    GLOBAL
}

internal fun shouldInitializeLegacyMilestoneScan(
    firstInstallTimeMillis: Long,
    lastUpdateTimeMillis: Long
): Boolean = lastUpdateTimeMillis > firstInstallTimeMillis

internal fun cleanupPendingMilestoneOwnership(
    exactOwnershipPending: Boolean,
    legacyScanPending: Boolean,
    scope: MilestoneCleanupScope,
    cleanup: () -> CalendarCleanupResult,
    clearExactOwnership: () -> Unit,
    clearLegacyScan: () -> Unit
): CalendarCleanupResult {
    if (!exactOwnershipPending && !legacyScanPending) {
        return CalendarCleanupResult.RemovedOrNotPresent
    }
    val result = cleanup()
    if (result.isSuccess) {
        if (exactOwnershipPending) {
            clearExactOwnership()
        }
        if (legacyScanPending && scope == MilestoneCleanupScope.GLOBAL) {
            clearLegacyScan()
        }
        return result
    }
    return if (
        result == CalendarCleanupResult.PermissionRequired &&
        legacyScanPending &&
        !exactOwnershipPending
    ) {
        CalendarCleanupResult.RemovedOrNotPresent
    } else {
        result
    }
}

internal fun applyManagedCalendarCleanupOwnershipPolicy(
    result: CalendarCleanupResult,
    clearOwnership: () -> Unit,
    enqueueRepair: () -> Unit
): CalendarCleanupResult = result.also {
    if (it.isSuccess) {
        clearOwnership()
    } else {
        enqueueRepair()
    }
}

internal object MilestoneCalendarOwnershipStore {
    private const val PREFS_NAME = "milestone_calendar_ownership"
    private const val KEY_INITIALIZED = "registry_initialized"
    private const val KEY_LEGACY_SCAN_PENDING = "legacy_scan_pending"
    private const val KEY_PENDING_EVENT_IDS = "pending_event_ids"
    private val lock = Any()

    fun markPendingDurably(context: Context, eventId: Int): Boolean = synchronized(lock) {
        val prefs = initializedPreferences(context)
        val ids = pendingEventIds(prefs).toMutableSet()
        if (!ids.add(eventId)) {
            true
        } else {
            writePendingEventIds(prefs, ids)
        }
    }

    fun clearPendingWithoutProviderDurably(context: Context, eventId: Int): Boolean = synchronized(lock) {
        val prefs = initializedPreferences(context)
        val ids = pendingEventIds(prefs).toMutableSet()
        if (!ids.remove(eventId)) {
            true
        } else {
            writePendingEventIds(prefs, ids)
        }
    }

    fun hasLegacyScanPending(context: Context): Boolean = synchronized(lock) {
        initializedPreferences(context).getBoolean(KEY_LEGACY_SCAN_PENDING, false)
    }

    fun clearEventIfPending(
        context: Context,
        eventId: Int,
        cleanup: () -> CalendarCleanupResult
    ): CalendarCleanupResult = synchronized(lock) {
        val prefs = initializedPreferences(context)
        cleanupPendingMilestoneOwnership(
            exactOwnershipPending = eventId in pendingEventIds(prefs),
            legacyScanPending = prefs.getBoolean(KEY_LEGACY_SCAN_PENDING, false),
            scope = MilestoneCleanupScope.EVENT,
            cleanup = cleanup,
            clearExactOwnership = {
                val ids = pendingEventIds(prefs).toMutableSet()
                ids.remove(eventId)
                writePendingEventIds(prefs, ids)
            },
            clearLegacyScan = {}
        )
    }

    fun clearAllIfPending(
        context: Context,
        cleanup: () -> CalendarCleanupResult
    ): CalendarCleanupResult = synchronized(lock) {
        val prefs = initializedPreferences(context)
        cleanupPendingMilestoneOwnership(
            exactOwnershipPending = pendingEventIds(prefs).isNotEmpty(),
            legacyScanPending = prefs.getBoolean(KEY_LEGACY_SCAN_PENDING, false),
            scope = MilestoneCleanupScope.GLOBAL,
            cleanup = cleanup,
            clearExactOwnership = { writePendingEventIds(prefs, emptySet()) },
            clearLegacyScan = {
                prefs.edit().putBoolean(KEY_LEGACY_SCAN_PENDING, false).commit()
            }
        )
    }

    fun recordManagedCleanup(
        context: Context,
        eventId: Int,
        result: CalendarCleanupResult,
        enqueueRepair: () -> Unit
    ) {
        applyManagedCalendarCleanupOwnershipPolicy(
            result = result,
            clearOwnership = {
                synchronized(lock) {
                    val prefs = initializedPreferences(context)
                    val ids = pendingEventIds(prefs).toMutableSet()
                    if (ids.remove(eventId)) {
                        writePendingEventIds(prefs, ids)
                    }
                }
            },
            enqueueRepair = enqueueRepair
        )
    }

    private fun initializedPreferences(context: Context): SharedPreferences {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            val legacyScanPending = runCatching {
                val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
                shouldInitializeLegacyMilestoneScan(
                    firstInstallTimeMillis = packageInfo.firstInstallTime,
                    lastUpdateTimeMillis = packageInfo.lastUpdateTime
                )
            }.getOrDefault(true)
            prefs.edit()
                .putBoolean(KEY_INITIALIZED, true)
                .putBoolean(KEY_LEGACY_SCAN_PENDING, legacyScanPending)
                .commit()
        }
        return prefs
    }

    private fun pendingEventIds(prefs: SharedPreferences): Set<Int> {
        return prefs.getStringSet(KEY_PENDING_EVENT_IDS, emptySet())
            .orEmpty()
            .mapNotNull(String::toIntOrNull)
            .toSet()
    }

    private fun writePendingEventIds(prefs: SharedPreferences, eventIds: Set<Int>): Boolean {
        val editor = prefs.edit()
        if (eventIds.isEmpty()) {
            editor.remove(KEY_PENDING_EVENT_IDS)
        } else {
            editor.putStringSet(KEY_PENDING_EVENT_IDS, eventIds.map(Int::toString).toSet())
        }
        return editor.commit()
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
    result: CalendarCleanupResult,
    repairReason: String? = null
): CalendarCleanupResult = result.also {
    MilestoneCalendarOwnershipStore.recordManagedCleanup(
        context = context,
        eventId = eventId,
        result = it,
        enqueueRepair = {
            repairReason?.let { reason -> RescheduleAllWorker.enqueue(context, reason) }
        }
    )
}
