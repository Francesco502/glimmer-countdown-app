package com.example.timeapk.notifications

import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_NONE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class MilestoneReminderSchedulerTest {

    private val zoneId: ZoneId = ZoneId.of("UTC")

    private fun epochMillisOf(localDate: LocalDate): Long =
        localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun externallyHandledCalendarCleanup_skipsOnlyMilestoneCalendarCleanup() {
        assertFalse(
            shouldClearCalendarBeforeMilestoneSync(calendarCleanupHandledExternally = true)
        )
    }

    @Test
    fun ordinaryMilestoneSync_keepsCalendarCleanupEnabled() {
        assertTrue(
            shouldClearCalendarBeforeMilestoneSync(calendarCleanupHandledExternally = false)
        )
    }

    @Test
    fun cleanupFailure_doesNotInsertMilestoneReplacement() = runBlocking {
        var insertionCalled = false

        val result = syncMilestoneCalendarReplacement(
            cleanup = { CalendarCleanupResult.ProviderFailure("provider down") },
            onCleanupFailure = { cleanup ->
                ScheduleSyncManager.MilestoneScheduleSyncResult(
                    scheduleEventId = null,
                    targetCalendarId = 7L,
                    lastSyncAt = 123L,
                    error = cleanup.message
                )
            },
            replacement = {
                insertionCalled = true
                ScheduleSyncManager.MilestoneScheduleSyncResult(99L, 7L, 123L, null)
            }
        )

        assertFalse(insertionCalled)
        assertEquals("provider down", result?.error)
    }

    @Test
    fun upgradedLegacyCleanup_runsBeforeMilestoneInsertion() = runBlocking {
        val calls = mutableListOf<String>()
        val legacyScanPending = shouldInitializeLegacyMilestoneScan(
            firstInstallTimeMillis = 100L,
            lastUpdateTimeMillis = 200L
        )

        val result = syncMilestoneCalendarReplacement(
            cleanup = {
                cleanupPendingMilestoneOwnership(
                    exactOwnershipPending = false,
                    legacyScanPending = legacyScanPending,
                    scope = MilestoneCleanupScope.EVENT,
                    cleanup = {
                        calls += "cleanup"
                        CalendarCleanupResult.RemovedOrNotPresent
                    },
                    clearExactOwnership = { error("No exact ownership should be cleared") },
                    clearLegacyScan = { error("Per-event cleanup must retain the global legacy marker") }
                )
            },
            onCleanupFailure = { error("Cleanup should succeed") },
            replacement = {
                calls += "insert"
                ScheduleSyncManager.MilestoneScheduleSyncResult(99L, 7L, 123L, null)
            }
        )

        assertEquals(listOf("cleanup", "insert"), calls)
        assertEquals(99L, result?.scheduleEventId)
    }

    @Test
    fun durableMilestoneInsertion_premarksBeforeProviderAttempt() {
        val calls = mutableListOf<String>()

        val result = insertMilestoneWithDurableOwnership(
            markPendingDurably = {
                calls += "premark"
                true
            },
            clearPendingDurably = {
                calls += "clear"
                true
            },
            transitionInflightToActiveDurably = {
                calls += "activate"
                true
            },
            restoreInflightDurably = { error("Transition should succeed") },
            insertion = {
                calls += "insert"
                MilestoneCalendarInsertionAttempt(
                    result = ScheduleSyncManager.MilestoneScheduleSyncResult(99L, 7L, 123L, null),
                    providerEventMayExist = true
                )
            },
            insertionFailure = { error("Insertion should not fail") },
            registryFailure = { error("Registry should be durable") }
        )

        assertEquals(listOf("premark", "insert", "activate"), calls)
        assertEquals(99L, result.scheduleEventId)
    }

    @Test
    fun durableMilestoneInsertion_nullProviderInsertClearsPremark() {
        var ownershipPending = false

        val result = insertMilestoneWithDurableOwnership(
            markPendingDurably = {
                ownershipPending = true
                true
            },
            clearPendingDurably = {
                ownershipPending = false
                true
            },
            transitionInflightToActiveDurably = { error("Null insert cannot become active") },
            restoreInflightDurably = { error("Null insert has no provider event") },
            insertion = {
                MilestoneCalendarInsertionAttempt(
                    result = ScheduleSyncManager.MilestoneScheduleSyncResult(
                        scheduleEventId = null,
                        targetCalendarId = 7L,
                        lastSyncAt = 123L,
                        error = "Milestone schedule insert failed"
                    ),
                    providerEventMayExist = false
                )
            },
            insertionFailure = { error("Insertion should return a result") },
            registryFailure = { error("Registry should be durable") }
        )

        assertFalse(ownershipPending)
        assertNull(result.scheduleEventId)
    }

    @Test
    fun durableMilestoneInsertion_exceptionRetainsPremarkForRepair() {
        var ownershipPending = false

        val result = insertMilestoneWithDurableOwnership(
            markPendingDurably = {
                ownershipPending = true
                true
            },
            clearPendingDurably = {
                ownershipPending = false
                true
            },
            transitionInflightToActiveDurably = { error("Exception cannot become active") },
            restoreInflightDurably = { error("Existing premark must remain") },
            insertion = { error("crashed after provider insert") },
            insertionFailure = { throwable ->
                ScheduleSyncManager.MilestoneScheduleSyncResult(
                    scheduleEventId = null,
                    targetCalendarId = null,
                    lastSyncAt = 123L,
                    error = throwable.message
                )
            },
            registryFailure = { error("Registry should be durable") }
        )

        assertTrue(ownershipPending)
        assertEquals("crashed after provider insert", result.error)
    }

    @Test
    fun durableMilestoneInsertion_failedPremarkSkipsProvider() {
        var insertionCalled = false

        val result = insertMilestoneWithDurableOwnership(
            markPendingDurably = { false },
            clearPendingDurably = { error("Nothing was marked") },
            transitionInflightToActiveDurably = { error("Nothing was marked") },
            restoreInflightDurably = { error("Nothing was marked") },
            insertion = {
                insertionCalled = true
                error("Provider must not be called")
            },
            insertionFailure = { error("Provider must not be called") },
            registryFailure = {
                ScheduleSyncManager.MilestoneScheduleSyncResult(
                    scheduleEventId = null,
                    targetCalendarId = null,
                    lastSyncAt = 123L,
                    error = "Ownership registry unavailable"
                )
            }
        )

        assertFalse(insertionCalled)
        assertEquals("Ownership registry unavailable", result.error)
    }

    @Test
    fun durableMilestoneInsertion_successTransitionsInflightToActive() {
        val exactIds = mutableSetOf<Int>()
        val inflightIds = mutableSetOf<Int>()

        val result = insertMilestoneWithDurableOwnership(
            markPendingDurably = {
                exactIds += 41
                inflightIds += 41
                true
            },
            clearPendingDurably = { error("Successful insert must stay owned") },
            transitionInflightToActiveDurably = {
                inflightIds -= 41
                true
            },
            restoreInflightDurably = { error("Transition should succeed") },
            insertion = {
                assertEquals(setOf(41), exactIds)
                assertEquals(setOf(41), inflightIds)
                MilestoneCalendarInsertionAttempt(
                    result = ScheduleSyncManager.MilestoneScheduleSyncResult(99L, 7L, 123L, null),
                    providerEventMayExist = true
                )
            },
            insertionFailure = { error("Insertion should not fail") },
            registryFailure = { error("Registry transition should succeed") }
        )

        assertEquals(99L, result.scheduleEventId)
        assertEquals(setOf(41), exactIds)
        assertTrue(inflightIds.isEmpty())
    }

    @Test
    fun durableMilestoneInsertion_transitionCommitFailureRetainsInflightAndReportsFailure() {
        var exactOwnershipPending = false
        var inflightRecoveryPending = false

        val result = insertMilestoneWithDurableOwnership(
            markPendingDurably = {
                exactOwnershipPending = true
                inflightRecoveryPending = true
                true
            },
            clearPendingDurably = { error("Successful provider insert must stay owned") },
            transitionInflightToActiveDurably = {
                inflightRecoveryPending = false
                false
            },
            restoreInflightDurably = {
                inflightRecoveryPending = true
                true
            },
            insertion = {
                MilestoneCalendarInsertionAttempt(
                    result = ScheduleSyncManager.MilestoneScheduleSyncResult(99L, 7L, 123L, null),
                    providerEventMayExist = true
                )
            },
            insertionFailure = { error("Insertion should not fail") },
            registryFailure = {
                ScheduleSyncManager.MilestoneScheduleSyncResult(
                    scheduleEventId = null,
                    targetCalendarId = 7L,
                    lastSyncAt = 123L,
                    error = "Ownership registry transition failed"
                )
            }
        )

        assertTrue(exactOwnershipPending)
        assertTrue(inflightRecoveryPending)
        assertEquals("Ownership registry transition failed", result.error)
    }

    @Test
    fun missingInflightAtTransition_isRestoredAndReportedAsFailure() {
        var exactOwnershipPending = true
        var inflightRecoveryPending = false

        val result = insertMilestoneWithDurableOwnership(
            markPendingDurably = { true },
            clearPendingDurably = { error("Provider event exists") },
            transitionInflightToActiveDurably = { false },
            restoreInflightDurably = {
                exactOwnershipPending = true
                inflightRecoveryPending = true
                true
            },
            insertion = {
                MilestoneCalendarInsertionAttempt(
                    result = ScheduleSyncManager.MilestoneScheduleSyncResult(99L, 7L, 123L, null),
                    providerEventMayExist = true
                )
            },
            insertionFailure = { error("Insertion should not fail") },
            registryFailure = {
                ScheduleSyncManager.MilestoneScheduleSyncResult(
                    scheduleEventId = null,
                    targetCalendarId = 7L,
                    lastSyncAt = 123L,
                    error = "Ownership registry transition failed"
                )
            }
        )

        assertTrue(exactOwnershipPending)
        assertTrue(inflightRecoveryPending)
        assertEquals("Ownership registry transition failed", result.error)
    }

    @Test
    fun concurrentSameEventTransactions_neverLeaveTwoProviderEntries() = runBlocking {
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondEntered = CompletableDeferred<Unit>()
        val providerEntries = linkedSetOf<Int>()
        var nextProviderId = 0
        var maxProviderEntries = 0
        var exactOwnershipPending = false
        var inflightRecoveryPending = false

        suspend fun replacement(isFirst: Boolean) = withMilestoneEventSyncLock(eventId = 41) {
            providerEntries.clear()
            exactOwnershipPending = false
            inflightRecoveryPending = false
            if (isFirst) {
                firstEntered.complete(Unit)
                releaseFirst.await()
            } else {
                secondEntered.complete(Unit)
            }
            exactOwnershipPending = true
            inflightRecoveryPending = true
            providerEntries += ++nextProviderId
            maxProviderEntries = maxOf(maxProviderEntries, providerEntries.size)
            inflightRecoveryPending = false
        }

        val first = async { replacement(isFirst = true) }
        firstEntered.await()
        val second = async { replacement(isFirst = false) }
        delay(50)
        assertFalse(secondEntered.isCompleted)
        releaseFirst.complete(Unit)
        first.await()
        second.await()

        assertEquals(1, maxProviderEntries)
        assertEquals(1, providerEntries.size)
        assertTrue(exactOwnershipPending)
        assertFalse(inflightRecoveryPending)
    }

    @Test
    fun workerOrigin_doesNotCancelItsRunningMilestoneWork() {
        assertFalse(
            shouldCancelMilestoneWorkBeforeSync(MilestoneSyncOrigin.WORKER_AFTER_NOTIFICATION)
        )
        assertTrue(
            shouldCancelMilestoneWorkBeforeSync(MilestoneSyncOrigin.CALLER)
        )
    }

    @Test
    fun successfulMilestoneSync_preservesUnresolvedPrimaryError() {
        val event = ownedEvent(lastError = "primary cleanup failed", lastSyncAt = 100L)

        val updated = eventAfterMilestoneScheduleSyncAttempt(
            event = event,
            result = ScheduleSyncManager.MilestoneScheduleSyncResult(
                scheduleEventId = 900L,
                targetCalendarId = 5L,
                lastSyncAt = 200L,
                error = null
            )
        )

        assertEquals("primary cleanup failed", updated.lastScheduleSyncError)
        assertEquals(100L, updated.lastScheduleSyncAt)
        assertEquals(183L, updated.scheduleEventId)
    }

    @Test
    fun failedMilestoneSync_mergesErrorAndSignalsRetry() {
        val event = ownedEvent(lastError = "primary failed", lastSyncAt = 100L)
        var retryRequests = 0
        val result = ScheduleSyncManager.MilestoneScheduleSyncResult(
            scheduleEventId = null,
            targetCalendarId = 5L,
            lastSyncAt = 200L,
            error = "milestone cleanup failed"
        )

        val updated = eventAfterMilestoneScheduleSyncAttempt(event, result)
        val retryRequested = requestMilestoneScheduleRetryOnFailure(result.error) {
            retryRequests += 1
        }

        assertEquals("primary failed; [Milestone] milestone cleanup failed", updated.lastScheduleSyncError)
        assertEquals(200L, updated.lastScheduleSyncAt)
        assertTrue(retryRequested)
        assertEquals(1, retryRequests)
    }

    @Test
    fun milestoneFailureThenSuccess_clearsOnlyTaggedMilestoneError() {
        val primaryEvent = ownedEvent(lastError = "primary failed", lastSyncAt = 100L)
        val failed = eventAfterMilestoneScheduleSyncAttempt(
            primaryEvent,
            ScheduleSyncManager.MilestoneScheduleSyncResult(
                scheduleEventId = null,
                targetCalendarId = 5L,
                lastSyncAt = 200L,
                error = "provider down"
            )
        )
        val recovered = eventAfterMilestoneScheduleSyncAttempt(
            failed,
            ScheduleSyncManager.MilestoneScheduleSyncResult(
                scheduleEventId = 901L,
                targetCalendarId = 5L,
                lastSyncAt = 300L,
                error = null
            )
        )

        assertEquals("primary failed; [Milestone] provider down", failed.lastScheduleSyncError)
        assertEquals("primary failed", recovered.lastScheduleSyncError)
    }

    @Test
    fun milestoneOnlyFailureThenSuccess_clearsResolvedError() {
        val failed = eventAfterMilestoneScheduleSyncAttempt(
            ownedEvent(lastError = null, lastSyncAt = 100L),
            ScheduleSyncManager.MilestoneScheduleSyncResult(
                scheduleEventId = null,
                targetCalendarId = 5L,
                lastSyncAt = 200L,
                error = "permission denied"
            )
        )
        val recovered = eventAfterMilestoneScheduleSyncAttempt(
            failed,
            ScheduleSyncManager.MilestoneScheduleSyncResult(
                scheduleEventId = 902L,
                targetCalendarId = 5L,
                lastSyncAt = 300L,
                error = null
            )
        )

        assertEquals("[Milestone] permission denied", failed.lastScheduleSyncError)
        assertNull(recovered.lastScheduleSyncError)
    }

    @Test
    fun rescheduleResult_aggregatesEveryMilestoneError() {
        val result = milestoneRescheduleResult(
            listOf("provider down", null, "permission denied", "provider down")
        )

        assertFalse(result.isSuccess)
        assertEquals("provider down; permission denied", result.error)
    }

    @Test
    fun computeNextMilestoneReminderPlan_futureEventStillProducesUpcomingReminder() {
        val today = LocalDate.of(2026, 3, 17)
        val nowMillis = today.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()
        val eventDate = today.plusDays(1)
        val expectedReminderAt = eventDate.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()
        val event = Event(
            id = 1,
            title = "future milestone",
            date = epochMillisOf(eventDate),
            category = CATEGORY_OTHER,
            repeatType = REPEAT_NONE
        )

        val plan = computeNextMilestoneReminderPlan(
            event = event,
            milestones = listOf(1L),
            remindDaysAhead = 1,
            remindMinuteOfDay = 600,
            smartMilestonesEnabled = false,
            today = today,
            nowMillis = nowMillis,
            zoneId = zoneId
        )

        assertNotNull(plan)
        assertEquals(1L, plan!!.milestoneValue)
        assertEquals(expectedReminderAt, plan.remindAtMillis)
    }

    private fun ownedEvent(lastError: String?, lastSyncAt: Long) = Event(
        id = 1,
        title = "owned",
        date = 1_800_000_000_000L,
        category = CATEGORY_OTHER,
        syncToScheduleEnabled = true,
        scheduleEventId = 183L,
        targetCalendarId = 5L,
        lastScheduleSyncAt = lastSyncAt,
        lastScheduleSyncError = lastError
    )
}
