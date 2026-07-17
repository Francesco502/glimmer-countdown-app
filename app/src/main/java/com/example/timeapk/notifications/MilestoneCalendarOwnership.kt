package com.example.timeapk.notifications

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

internal enum class MilestoneCleanupScope {
    EVENT,
    GLOBAL
}

internal data class MilestoneOwnershipRegistryState(
    val exactEventIds: Set<Int>,
    val inflightEventIds: Set<Int>,
    val legacyScanPending: Boolean,
    val initialized: Boolean
)

internal fun commitMilestoneOwnershipStateWithRollback(
    oldState: MilestoneOwnershipRegistryState,
    newState: MilestoneOwnershipRegistryState,
    write: (MilestoneOwnershipRegistryState) -> Boolean
): Boolean {
    if (write(newState)) return true
    write(oldState)
    return false
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
        if (exactOwnershipPending && !clearExactOwnership()) {
            return CalendarCleanupResult.ProviderFailure("Calendar ownership registry update failed")
        }
        if (
            legacyScanPending &&
            scope == MilestoneCleanupScope.GLOBAL &&
            !clearLegacyScan()
        ) {
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

    private data class InitializedPreferences(
        val prefs: SharedPreferences,
        val isDurable: Boolean
    )

    fun markPendingDurably(context: Context, eventId: Int): Boolean = synchronized(lock) {
        val initialized = initializedPreferences(context)
        if (!initialized.isDurable) return@synchronized false
        updateRegistryState(initialized.prefs) { state ->
            state.copy(
                exactEventIds = state.exactEventIds + eventId,
                inflightEventIds = state.inflightEventIds + eventId
            )
        }
    }

    fun clearPendingWithoutProviderDurably(context: Context, eventId: Int): Boolean = synchronized(lock) {
        val initialized = initializedPreferences(context)
        if (!initialized.isDurable) return@synchronized false
        updateRegistryState(initialized.prefs) { state ->
            state.copy(
                exactEventIds = state.exactEventIds - eventId,
                inflightEventIds = state.inflightEventIds - eventId
            )
        }
    }

    fun transitionInflightToActiveDurably(context: Context, eventId: Int): Boolean = synchronized(lock) {
        val initialized = initializedPreferences(context)
        if (!initialized.isDurable) return@synchronized false
        val state = readRegistryState(initialized.prefs)
        if (eventId !in state.inflightEventIds) return@synchronized false
        updateRegistryState(initialized.prefs) {
            it.copy(inflightEventIds = it.inflightEventIds - eventId)
        }
    }

    fun restoreInflightDurably(context: Context, eventId: Int): Boolean = synchronized(lock) {
        val initialized = initializedPreferences(context)
        if (!initialized.isDurable) return@synchronized false
        updateRegistryState(initialized.prefs) { state ->
            state.copy(
                exactEventIds = state.exactEventIds + eventId,
                inflightEventIds = state.inflightEventIds + eventId
            )
        }
    }

    fun hasRecoveryPending(context: Context): Boolean = synchronized(lock) {
        val initialized = initializedPreferences(context)
        if (!initialized.isDurable) return@synchronized true
        val state = readRegistryState(initialized.prefs)
        shouldForceMilestoneRecovery(
            activeOwnershipPending = state.exactEventIds.isNotEmpty(),
            inflightRecoveryPending = state.inflightEventIds.isNotEmpty(),
            legacyScanPending = state.legacyScanPending
        )
    }

    fun clearEventIfPending(
        context: Context,
        eventId: Int,
        cleanup: () -> CalendarCleanupResult
    ): CalendarCleanupResult = synchronized(lock) {
        val initialized = initializedPreferences(context)
        if (!initialized.isDurable) {
            return@synchronized CalendarCleanupResult.ProviderFailure(
                "Calendar ownership registry update failed"
            )
        }
        val state = readRegistryState(initialized.prefs)
        cleanupPendingMilestoneOwnership(
            exactOwnershipPending =
                eventId in state.exactEventIds || eventId in state.inflightEventIds,
            legacyScanPending = state.legacyScanPending,
            scope = MilestoneCleanupScope.EVENT,
            cleanup = cleanup,
            clearExactOwnership = {
                updateRegistryState(initialized.prefs) {
                    it.copy(
                        exactEventIds = it.exactEventIds - eventId,
                        inflightEventIds = it.inflightEventIds - eventId
                    )
                }
            },
            clearLegacyScan = { true }
        )
    }

    fun clearAllIfPending(
        context: Context,
        cleanup: () -> CalendarCleanupResult
    ): CalendarCleanupResult = synchronized(lock) {
        val initialized = initializedPreferences(context)
        if (!initialized.isDurable) {
            return@synchronized CalendarCleanupResult.ProviderFailure(
                "Calendar ownership registry update failed"
            )
        }
        val state = readRegistryState(initialized.prefs)
        cleanupPendingMilestoneOwnership(
            exactOwnershipPending =
                state.exactEventIds.isNotEmpty() || state.inflightEventIds.isNotEmpty(),
            legacyScanPending = state.legacyScanPending,
            scope = MilestoneCleanupScope.GLOBAL,
            cleanup = cleanup,
            clearExactOwnership = {
                updateRegistryState(initialized.prefs) {
                    it.copy(exactEventIds = emptySet(), inflightEventIds = emptySet())
                }
            },
            clearLegacyScan = {
                updateRegistryState(initialized.prefs) {
                    it.copy(legacyScanPending = false)
                }
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
                    val initialized = initializedPreferences(context)
                    if (!initialized.isDurable) return@synchronized false
                    updateRegistryState(initialized.prefs) {
                        it.copy(
                            exactEventIds = it.exactEventIds - eventId,
                            inflightEventIds = it.inflightEventIds - eventId
                        )
                    }
                }
            },
            enqueueRepair = enqueueRepair
        )

    private fun initializedPreferences(context: Context): InitializedPreferences {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val state = readRegistryState(prefs)
        if (!state.initialized) {
            val legacyScanPending = runCatching {
                val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
                shouldInitializeLegacyMilestoneScan(
                    firstInstallTimeMillis = packageInfo.firstInstallTime,
                    lastUpdateTimeMillis = packageInfo.lastUpdateTime
                )
            }.getOrDefault(true)
            val initialized = commitMilestoneOwnershipStateWithRollback(
                oldState = state,
                newState = state.copy(
                    initialized = true,
                    legacyScanPending = legacyScanPending
                ),
                write = { writeRegistryState(prefs, it) }
            )
            return InitializedPreferences(prefs, initialized)
        }
        return InitializedPreferences(prefs, true)
    }

    private fun updateRegistryState(
        prefs: SharedPreferences,
        transform: (MilestoneOwnershipRegistryState) -> MilestoneOwnershipRegistryState
    ): Boolean {
        val oldState = readRegistryState(prefs)
        val newState = transform(oldState)
        if (newState == oldState) return true
        return commitMilestoneOwnershipStateWithRollback(
            oldState = oldState,
            newState = newState,
            write = { writeRegistryState(prefs, it) }
        )
    }

    private fun readRegistryState(prefs: SharedPreferences): MilestoneOwnershipRegistryState {
        fun eventIds(key: String): Set<Int> = prefs.getStringSet(key, emptySet())
            .orEmpty()
            .mapNotNull(String::toIntOrNull)
            .toSet()
        return MilestoneOwnershipRegistryState(
            exactEventIds = eventIds(KEY_PENDING_EVENT_IDS),
            inflightEventIds = eventIds(KEY_INFLIGHT_EVENT_IDS),
            legacyScanPending = prefs.getBoolean(KEY_LEGACY_SCAN_PENDING, false),
            initialized = prefs.getBoolean(KEY_INITIALIZED, false)
        )
    }

    @SuppressLint("UseKtx") // The KTX helper discards commit(), but registry durability requires its Boolean result.
    private fun writeRegistryState(
        prefs: SharedPreferences,
        state: MilestoneOwnershipRegistryState
    ): Boolean {
        val editor = prefs.edit()
            .putBoolean(KEY_INITIALIZED, state.initialized)
            .putBoolean(KEY_LEGACY_SCAN_PENDING, state.legacyScanPending)
        if (state.exactEventIds.isEmpty()) {
            editor.remove(KEY_PENDING_EVENT_IDS)
        } else {
            editor.putStringSet(
                KEY_PENDING_EVENT_IDS,
                state.exactEventIds.map(Int::toString).toSet()
            )
        }
        if (state.inflightEventIds.isEmpty()) {
            editor.remove(KEY_INFLIGHT_EVENT_IDS)
        } else {
            editor.putStringSet(
                KEY_INFLIGHT_EVENT_IDS,
                state.inflightEventIds.map(Int::toString).toSet()
            )
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
