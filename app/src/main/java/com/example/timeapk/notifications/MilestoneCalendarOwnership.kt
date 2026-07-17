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

@Suppress("UNUSED_PARAMETER")
internal fun shouldForceMilestoneRecovery(
    activeOwnershipPending: Boolean,
    inflightRecoveryPending: Boolean,
    legacyScanPending: Boolean
): Boolean = inflightRecoveryPending || legacyScanPending

internal fun cleanupPendingMilestoneOwnership(
    exactOwnershipPending: Boolean,
    legacyScanPending: Boolean,
    scope: MilestoneCleanupScope,
    cleanup: () -> CalendarCleanupResult,
    clearExactOwnership: () -> Boolean,
    clearLegacyScan: () -> Boolean
): CalendarCleanupResult {
    if (!exactOwnershipPending && !legacyScanPending) {
        return CalendarCleanupResult.RemovedOrNotPresent
    }
    val result = cleanup()
    if (result.isSuccess) {
        val exactCleared = !exactOwnershipPending || clearExactOwnership()
        val legacyCleared =
            legacyScanPending.not() ||
                scope != MilestoneCleanupScope.GLOBAL ||
                clearLegacyScan()
        if (!exactCleared || !legacyCleared) {
            return CalendarCleanupResult.ProviderFailure("Calendar ownership registry update failed")
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
    clearOwnership: () -> Boolean,
    enqueueRepair: () -> Unit
): CalendarCleanupResult {
    if (!result.isSuccess) {
        enqueueRepair()
        return result
    }
    if (clearOwnership()) return result
    enqueueRepair()
    return CalendarCleanupResult.ProviderFailure("Calendar ownership registry update failed")
}

internal object MilestoneCalendarOwnershipStore {
    private const val PREFS_NAME = "milestone_calendar_ownership"
    private const val KEY_INITIALIZED = "registry_initialized"
    private const val KEY_LEGACY_SCAN_PENDING = "legacy_scan_pending"
    private const val KEY_PENDING_EVENT_IDS = "pending_event_ids"
    private const val KEY_INFLIGHT_EVENT_IDS = "inflight_event_ids"
    private val lock = Any()

    fun markPendingDurably(context: Context, eventId: Int): Boolean = synchronized(lock) {
        val prefs = initializedPreferences(context)
        val exactIds = pendingEventIds(prefs).toMutableSet()
        val inflightIds = inflightEventIds(prefs).toMutableSet()
        val exactAdded = exactIds.add(eventId)
        val inflightAdded = inflightIds.add(eventId)
        if (!exactAdded && !inflightAdded) {
            true
        } else {
            writeOwnershipEventIds(prefs, exactIds, inflightIds)
        }
    }

    fun clearPendingWithoutProviderDurably(context: Context, eventId: Int): Boolean = synchronized(lock) {
        val prefs = initializedPreferences(context)
        val exactIds = pendingEventIds(prefs).toMutableSet()
        val inflightIds = inflightEventIds(prefs).toMutableSet()
        val exactRemoved = exactIds.remove(eventId)
        val inflightRemoved = inflightIds.remove(eventId)
        if (!exactRemoved && !inflightRemoved) {
            true
        } else {
            writeOwnershipEventIds(prefs, exactIds, inflightIds)
        }
    }

    fun transitionInflightToActiveDurably(context: Context, eventId: Int): Boolean = synchronized(lock) {
        val prefs = initializedPreferences(context)
        val inflightIds = inflightEventIds(prefs).toMutableSet()
        if (!inflightIds.remove(eventId)) {
            true
        } else {
            writeOwnershipEventIds(prefs, pendingEventIds(prefs), inflightIds)
        }
    }

    fun hasRecoveryPending(context: Context): Boolean = synchronized(lock) {
        val prefs = initializedPreferences(context)
        shouldForceMilestoneRecovery(
            activeOwnershipPending = pendingEventIds(prefs).isNotEmpty(),
            inflightRecoveryPending = inflightEventIds(prefs).isNotEmpty(),
            legacyScanPending = prefs.getBoolean(KEY_LEGACY_SCAN_PENDING, false)
        )
    }

    fun clearEventIfPending(
        context: Context,
        eventId: Int,
        cleanup: () -> CalendarCleanupResult
    ): CalendarCleanupResult = synchronized(lock) {
        val prefs = initializedPreferences(context)
        val exactIds = pendingEventIds(prefs)
        val inflightIds = inflightEventIds(prefs)
        cleanupPendingMilestoneOwnership(
            exactOwnershipPending = eventId in exactIds || eventId in inflightIds,
            legacyScanPending = prefs.getBoolean(KEY_LEGACY_SCAN_PENDING, false),
            scope = MilestoneCleanupScope.EVENT,
            cleanup = cleanup,
            clearExactOwnership = {
                writeOwnershipEventIds(
                    prefs = prefs,
                    exactEventIds = exactIds - eventId,
                    inflightEventIds = inflightIds - eventId
                )
            },
            clearLegacyScan = { true }
        )
    }

    fun clearAllIfPending(
        context: Context,
        cleanup: () -> CalendarCleanupResult
    ): CalendarCleanupResult = synchronized(lock) {
        val prefs = initializedPreferences(context)
        val exactIds = pendingEventIds(prefs)
        val inflightIds = inflightEventIds(prefs)
        cleanupPendingMilestoneOwnership(
            exactOwnershipPending = exactIds.isNotEmpty() || inflightIds.isNotEmpty(),
            legacyScanPending = prefs.getBoolean(KEY_LEGACY_SCAN_PENDING, false),
            scope = MilestoneCleanupScope.GLOBAL,
            cleanup = cleanup,
            clearExactOwnership = {
                writeOwnershipEventIds(prefs, emptySet(), emptySet())
            },
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
    ): CalendarCleanupResult = applyManagedCalendarCleanupOwnershipPolicy(
            result = result,
            clearOwnership = {
                synchronized(lock) {
                    val prefs = initializedPreferences(context)
                    val exactIds = pendingEventIds(prefs)
                    val inflightIds = inflightEventIds(prefs)
                    if (eventId in exactIds || eventId in inflightIds) {
                        writeOwnershipEventIds(
                            prefs = prefs,
                            exactEventIds = exactIds - eventId,
                            inflightEventIds = inflightIds - eventId
                        )
                    } else {
                        true
                    }
                }
            },
            enqueueRepair = enqueueRepair
        )

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

    private fun inflightEventIds(prefs: SharedPreferences): Set<Int> {
        return prefs.getStringSet(KEY_INFLIGHT_EVENT_IDS, emptySet())
            .orEmpty()
            .mapNotNull(String::toIntOrNull)
            .toSet()
    }

    private fun writeOwnershipEventIds(
        prefs: SharedPreferences,
        exactEventIds: Set<Int>,
        inflightEventIds: Set<Int>
    ): Boolean {
        val editor = prefs.edit()
        if (exactEventIds.isEmpty()) {
            editor.remove(KEY_PENDING_EVENT_IDS)
        } else {
            editor.putStringSet(KEY_PENDING_EVENT_IDS, exactEventIds.map(Int::toString).toSet())
        }
        if (inflightEventIds.isEmpty()) {
            editor.remove(KEY_INFLIGHT_EVENT_IDS)
        } else {
            editor.putStringSet(KEY_INFLIGHT_EVENT_IDS, inflightEventIds.map(Int::toString).toSet())
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
): CalendarCleanupResult = MilestoneCalendarOwnershipStore.recordManagedCleanup(
        context = context,
        eventId = eventId,
        result = result,
        enqueueRepair = {
            repairReason?.let { reason -> RescheduleAllWorker.enqueue(context, reason) }
        }
    )
