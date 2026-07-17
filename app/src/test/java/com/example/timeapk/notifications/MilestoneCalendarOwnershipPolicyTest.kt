package com.example.timeapk.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MilestoneCalendarOwnershipPolicyTest {

    @Test
    fun freshDisabledState_withoutPendingOwnership_skipsProviderAndSavesState() {
        var providerCalls = 0
        val legacyScanPending = shouldInitializeLegacyMilestoneScan(
            firstInstallTimeMillis = 100L,
            lastUpdateTimeMillis = 100L
        )
        val cleanup = cleanupPendingMilestoneOwnership(
            exactOwnershipPending = false,
            legacyScanPending = legacyScanPending,
            scope = MilestoneCleanupScope.GLOBAL,
            cleanup = {
                providerCalls += 1
                CalendarCleanupResult.PermissionRequired
            },
            clearExactOwnership = { error("Nothing exact should be cleared") },
            clearLegacyScan = { error("No legacy scan should be cleared") }
        )
        val state = completedRescheduleState(
            candidate = RescheduleState("prefs", emptyMap(), 123L),
            shouldRetry = !cleanup.isSuccess
        )

        assertEquals(CalendarCleanupResult.RemovedOrNotPresent, cleanup)
        assertEquals(0, providerCalls)
        assertNotNull(state)
    }

    @Test
    fun pendingOwnership_withoutPermission_retriesWithoutAdvancingState() {
        var pendingOwnershipCleared = false
        val cleanup = cleanupPendingMilestoneOwnership(
            exactOwnershipPending = true,
            legacyScanPending = false,
            scope = MilestoneCleanupScope.EVENT,
            cleanup = { CalendarCleanupResult.PermissionRequired },
            clearExactOwnership = {
                pendingOwnershipCleared = true
                true
            },
            clearLegacyScan = { error("No legacy scan should be cleared") }
        )
        val state = completedRescheduleState(
            candidate = RescheduleState("prefs", emptyMap(), 123L),
            shouldRetry = !cleanup.isSuccess
        )

        assertEquals(CalendarCleanupResult.PermissionRequired, cleanup)
        assertFalse(pendingOwnershipCleared)
        assertNull(state)
    }

    @Test
    fun successfulPendingCleanup_clearsTrackedOwnership() {
        var pendingOwnershipCleared = false

        val cleanup = cleanupPendingMilestoneOwnership(
            exactOwnershipPending = true,
            legacyScanPending = false,
            scope = MilestoneCleanupScope.EVENT,
            cleanup = { CalendarCleanupResult.RemovedOrNotPresent },
            clearExactOwnership = {
                pendingOwnershipCleared = true
                true
            },
            clearLegacyScan = { error("No legacy scan should be cleared") }
        )

        assertTrue(cleanup.isSuccess)
        assertTrue(pendingOwnershipCleared)
    }

    @Test
    fun installClassification_marksOnlyAnUpgradedInstallForLegacyScan() {
        assertFalse(
            shouldInitializeLegacyMilestoneScan(
                firstInstallTimeMillis = 100L,
                lastUpdateTimeMillis = 100L
            )
        )
        assertTrue(
            shouldInitializeLegacyMilestoneScan(
                firstInstallTimeMillis = 100L,
                lastUpdateTimeMillis = 200L
            )
        )
    }

    @Test
    fun upgradedLegacyWithoutPermission_defersAndKeepsMarkerWithoutRetryLoop() {
        var legacyScanCleared = false
        val legacyScanPending = shouldInitializeLegacyMilestoneScan(
            firstInstallTimeMillis = 100L,
            lastUpdateTimeMillis = 200L
        )
        val cleanup = cleanupPendingMilestoneOwnership(
            exactOwnershipPending = false,
            legacyScanPending = legacyScanPending,
            scope = MilestoneCleanupScope.GLOBAL,
            cleanup = { CalendarCleanupResult.PermissionRequired },
            clearExactOwnership = { error("No exact ownership should be cleared") },
            clearLegacyScan = {
                legacyScanCleared = true
                true
            }
        )
        val state = completedRescheduleState(
            candidate = RescheduleState("prefs", emptyMap(), 123L),
            shouldRetry = !cleanup.isSuccess
        )

        assertEquals(CalendarCleanupResult.RemovedOrNotPresent, cleanup)
        assertFalse(legacyScanCleared)
        assertNotNull(state)
    }

    @Test
    fun successfulGlobalLegacyScan_clearsPersistedMarker() {
        var legacyScanCleared = false

        val cleanup = cleanupPendingMilestoneOwnership(
            exactOwnershipPending = false,
            legacyScanPending = true,
            scope = MilestoneCleanupScope.GLOBAL,
            cleanup = { CalendarCleanupResult.RemovedOrNotPresent },
            clearExactOwnership = { error("No exact ownership should be cleared") },
            clearLegacyScan = {
                legacyScanCleared = true
                true
            }
        )

        assertTrue(cleanup.isSuccess)
        assertTrue(legacyScanCleared)
    }

    @Test
    fun combinedCleanupFailure_retainsOwnershipAndRequestsRepair() {
        var ownershipPending = true
        var repairRequests = 0

        applyManagedCalendarCleanupOwnershipPolicy(
            result = CalendarCleanupResult.ProviderFailure("provider down"),
            clearOwnership = {
                ownershipPending = false
                true
            },
            enqueueRepair = { repairRequests += 1 }
        )

        assertTrue(ownershipPending)
        assertEquals(1, repairRequests)
    }

    @Test
    fun combinedCleanupSuccess_clearsOwnershipWithoutRepair() {
        var ownershipPending = true
        var repairRequests = 0

        val result = applyManagedCalendarCleanupOwnershipPolicy(
            result = CalendarCleanupResult.RemovedOrNotPresent,
            clearOwnership = {
                ownershipPending = false
                true
            },
            enqueueRepair = { repairRequests += 1 }
        )

        assertTrue(result.isSuccess)
        assertFalse(ownershipPending)
        assertEquals(0, repairRequests)
    }

    @Test
    fun eventCleanupCommitFailure_returnsFailureForRetry() {
        val original = MilestoneOwnershipRegistryState(
            exactEventIds = setOf(41),
            inflightEventIds = emptySet(),
            legacyScanPending = false,
            initialized = true
        )
        var inMemoryState = original
        val result = cleanupPendingMilestoneOwnership(
            exactOwnershipPending = true,
            legacyScanPending = false,
            scope = MilestoneCleanupScope.EVENT,
            cleanup = { CalendarCleanupResult.RemovedOrNotPresent },
            clearExactOwnership = {
                commitMilestoneOwnershipStateWithRollback(
                    oldState = original,
                    newState = original.copy(exactEventIds = emptySet()),
                    write = { state ->
                        inMemoryState = state
                        false
                    }
                )
            },
            clearLegacyScan = { error("No legacy marker should be cleared") }
        )

        assertTrue(result is CalendarCleanupResult.ProviderFailure)
        var secondCleanupCalls = 0
        cleanupPendingMilestoneOwnership(
            exactOwnershipPending = 41 in inMemoryState.exactEventIds,
            legacyScanPending = inMemoryState.legacyScanPending,
            scope = MilestoneCleanupScope.EVENT,
            cleanup = {
                secondCleanupCalls += 1
                CalendarCleanupResult.RemovedOrNotPresent
            },
            clearExactOwnership = { true },
            clearLegacyScan = { true }
        )
        assertEquals(original, inMemoryState)
        assertEquals(1, secondCleanupCalls)
    }

    @Test
    fun globalExactCleanupCommitFailure_returnsFailureForRetry() {
        val original = MilestoneOwnershipRegistryState(
            exactEventIds = setOf(41),
            inflightEventIds = setOf(41),
            legacyScanPending = true,
            initialized = true
        )
        var inMemoryState = original
        var legacyClearCalls = 0
        val result = cleanupPendingMilestoneOwnership(
            exactOwnershipPending = true,
            legacyScanPending = true,
            scope = MilestoneCleanupScope.GLOBAL,
            cleanup = { CalendarCleanupResult.RemovedOrNotPresent },
            clearExactOwnership = {
                commitMilestoneOwnershipStateWithRollback(
                    oldState = original,
                    newState = original.copy(
                        exactEventIds = emptySet(),
                        inflightEventIds = emptySet()
                    ),
                    write = { state ->
                        inMemoryState = state
                        false
                    }
                )
            },
            clearLegacyScan = {
                legacyClearCalls += 1
                true
            }
        )

        assertTrue(result is CalendarCleanupResult.ProviderFailure)
        assertEquals(original, inMemoryState)
        assertEquals(0, legacyClearCalls)
        assertTrue(
            shouldForceMilestoneRecovery(
                activeOwnershipPending = inMemoryState.exactEventIds.isNotEmpty(),
                inflightRecoveryPending = inMemoryState.inflightEventIds.isNotEmpty(),
                legacyScanPending = inMemoryState.legacyScanPending
            )
        )
    }

    @Test
    fun globalLegacyCleanupCommitFailure_returnsFailureForRetry() {
        val original = MilestoneOwnershipRegistryState(
            exactEventIds = emptySet(),
            inflightEventIds = emptySet(),
            legacyScanPending = true,
            initialized = true
        )
        var inMemoryState = original
        val result = cleanupPendingMilestoneOwnership(
            exactOwnershipPending = false,
            legacyScanPending = true,
            scope = MilestoneCleanupScope.GLOBAL,
            cleanup = { CalendarCleanupResult.RemovedOrNotPresent },
            clearExactOwnership = { error("No exact ownership should be cleared") },
            clearLegacyScan = {
                commitMilestoneOwnershipStateWithRollback(
                    oldState = original,
                    newState = original.copy(legacyScanPending = false),
                    write = { state ->
                        inMemoryState = state
                        false
                    }
                )
            }
        )

        assertTrue(result is CalendarCleanupResult.ProviderFailure)
        assertEquals(original, inMemoryState)
        assertTrue(inMemoryState.legacyScanPending)
        assertTrue(
            shouldForceMilestoneRecovery(
                activeOwnershipPending = false,
                inflightRecoveryPending = false,
                legacyScanPending = inMemoryState.legacyScanPending
            )
        )
    }

    @Test
    fun managedCleanupCommitFailure_retainsOwnershipAndRequestsRepair() {
        val original = MilestoneOwnershipRegistryState(
            exactEventIds = setOf(41),
            inflightEventIds = emptySet(),
            legacyScanPending = false,
            initialized = true
        )
        var inMemoryState = original
        var repairRequests = 0

        val result = applyManagedCalendarCleanupOwnershipPolicy(
            result = CalendarCleanupResult.RemovedOrNotPresent,
            clearOwnership = {
                commitMilestoneOwnershipStateWithRollback(
                    oldState = original,
                    newState = original.copy(exactEventIds = emptySet()),
                    write = { state ->
                        inMemoryState = state
                        false
                    }
                )
            },
            enqueueRepair = { repairRequests += 1 }
        )

        assertTrue(result is CalendarCleanupResult.ProviderFailure)
        assertEquals(original, inMemoryState)
        assertEquals(1, repairRequests)
    }

    @Test
    fun transitionCommitFailure_restoresInflightRecoveryForNextWorker() {
        val original = MilestoneOwnershipRegistryState(
            exactEventIds = setOf(41),
            inflightEventIds = setOf(41),
            legacyScanPending = false,
            initialized = true
        )
        var inMemoryState = original

        val committed = commitMilestoneOwnershipStateWithRollback(
            oldState = original,
            newState = original.copy(inflightEventIds = emptySet()),
            write = { state ->
                inMemoryState = state
                false
            }
        )

        assertFalse(committed)
        assertEquals(original, inMemoryState)
        assertTrue(
            shouldForceMilestoneRecovery(
                activeOwnershipPending = true,
                inflightRecoveryPending = inMemoryState.inflightEventIds.isNotEmpty(),
                legacyScanPending = false
            )
        )
    }
}
