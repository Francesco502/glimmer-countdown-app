package com.example.timeapk.ui.home

import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.notifications.CalendarCleanupResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventDeletionPolicyTest {

    @Test
    fun eventWithoutCalendarOwnership_skipsCleanupAndDeletesDirectly() = runBlocking {
        val fake = DeletionFake()

        val result = deleteEventRecoverably(
            event = event(),
            nowMillis = { 123 },
            cleanup = fake::cleanup,
            update = fake::update,
            cancelReminder = fake::cancelReminder,
            cancelMilestones = fake::cancelMilestones,
            delete = fake::delete,
            refreshWidgets = fake::refreshWidgets
        )

        assertEquals(DeleteEventResult.Deleted, result)
        assertEquals(listOf("cancelReminder", "cancelMilestones", "delete", "refreshWidgets"), fake.calls)
        assertNull(fake.updatedEvent)
    }

    @Test
    fun permissionFailure_persistsRetryableStateWithoutDestructiveSideEffects() = runBlocking {
        val fake = DeletionFake(cleanupResult = CalendarCleanupResult.PermissionRequired)

        val result = runDeletion(fake)

        assertEquals(DeleteEventResult.Blocked("Calendar permission required"), result)
        assertEquals(listOf("cleanup", "update"), fake.calls)
        assertEquals(88L, fake.updatedEvent?.scheduleEventId)
        assertEquals(9L, fake.updatedEvent?.targetCalendarId)
        assertEquals("Calendar permission required", fake.updatedEvent?.lastScheduleSyncError)
        assertEquals(123L, fake.updatedEvent?.lastScheduleSyncAt)
    }

    @Test
    fun providerFailure_persistsRetryableStateWithoutDestructiveSideEffects() = runBlocking {
        val fake = DeletionFake(
            cleanupResult = CalendarCleanupResult.ProviderFailure("provider down")
        )

        val result = runDeletion(fake)

        assertEquals(DeleteEventResult.Blocked("provider down"), result)
        assertEquals(listOf("cleanup", "update"), fake.calls)
        assertEquals("provider down", fake.updatedEvent?.lastScheduleSyncError)
        assertEquals(123L, fake.updatedEvent?.lastScheduleSyncAt)
    }

    @Test
    fun failedCleanupWhoseErrorCannotBePersisted_remainsBlocked() = runBlocking {
        val fake = DeletionFake(
            cleanupResult = CalendarCleanupResult.PermissionRequired,
            updateFailure = IllegalStateException("room down")
        )

        val result = runDeletion(fake)

        assertTrue(result is DeleteEventResult.Blocked)
        assertEquals(listOf("cleanup", "update"), fake.calls)
        assertFalse(fake.calls.contains("delete"))
    }

    @Test
    fun successfulCleanup_runsDestructiveSideEffectsInOrder() = runBlocking {
        val fake = DeletionFake()

        val result = runDeletion(fake)

        assertEquals(DeleteEventResult.Deleted, result)
        assertEquals(
            listOf("cleanup", "cancelReminder", "cancelMilestones", "delete", "refreshWidgets"),
            fake.calls
        )
    }

    @Test
    fun sideEffectException_isNeverReportedAsDeleted() = runBlocking {
        val fake = DeletionFake(deleteFailure = IllegalStateException("room down"))

        val result = runDeletion(fake)

        assertTrue(result is DeleteEventResult.Blocked)
        assertFalse(fake.calls.contains("refreshWidgets"))
    }

    @Test
    fun widgetRefreshFailureAfterRoomDelete_isStillReportedAsDeleted() = runBlocking {
        val fake = DeletionFake(refreshFailure = IllegalStateException("widget down"))

        val result = runDeletion(fake)

        assertEquals(DeleteEventResult.Deleted, result)
        assertEquals("refreshWidgets", fake.calls.last())
    }

    @Test(expected = CancellationException::class)
    fun cleanupCancellation_isPropagated() = runBlocking {
        runDeletion(DeletionFake(cleanupFailure = CancellationException("cancelled")))
        Unit
    }

    @Test(expected = CancellationException::class)
    fun retryableStateUpdateCancellation_isPropagated() = runBlocking {
        runDeletion(
            DeletionFake(
                cleanupResult = CalendarCleanupResult.PermissionRequired,
                updateFailure = CancellationException("cancelled")
            )
        )
        Unit
    }

    @Test(expected = CancellationException::class)
    fun reminderCancellation_isPropagated() = runBlocking {
        runDeletion(DeletionFake(cancelReminderFailure = CancellationException("cancelled")))
        Unit
    }

    @Test(expected = CancellationException::class)
    fun roomDeleteCancellation_isPropagated() = runBlocking {
        runDeletion(DeletionFake(deleteFailure = CancellationException("cancelled")))
        Unit
    }

    @Test(expected = CancellationException::class)
    fun widgetRefreshCancellation_isPropagated() = runBlocking {
        runDeletion(DeletionFake(refreshFailure = CancellationException("cancelled")))
        Unit
    }

    private suspend fun runDeletion(fake: DeletionFake): DeleteEventResult = deleteEventRecoverably(
        event = event(syncToScheduleEnabled = true, scheduleEventId = 88, targetCalendarId = 9),
        nowMillis = { 123 },
        cleanup = fake::cleanup,
        update = fake::update,
        cancelReminder = fake::cancelReminder,
        cancelMilestones = fake::cancelMilestones,
        delete = fake::delete,
        refreshWidgets = fake::refreshWidgets
    )

    private fun event(
        syncToScheduleEnabled: Boolean = false,
        scheduleEventId: Long? = null,
        targetCalendarId: Long? = null
    ) = Event(
        id = 7,
        title = "Trip",
        date = 1,
        category = CATEGORY_OTHER,
        syncToScheduleEnabled = syncToScheduleEnabled,
        scheduleEventId = scheduleEventId,
        targetCalendarId = targetCalendarId
    )

    private class DeletionFake(
        private val cleanupResult: CalendarCleanupResult = CalendarCleanupResult.RemovedOrNotPresent,
        private val cleanupFailure: Throwable? = null,
        private val updateFailure: Throwable? = null,
        private val cancelReminderFailure: Throwable? = null,
        private val deleteFailure: Throwable? = null,
        private val refreshFailure: Throwable? = null
    ) {
        val calls = mutableListOf<String>()
        var updatedEvent: Event? = null

        suspend fun cleanup(event: Event): CalendarCleanupResult {
            calls += "cleanup"
            cleanupFailure?.let { throw it }
            return cleanupResult
        }

        suspend fun update(event: Event) {
            calls += "update"
            updatedEvent = event
            updateFailure?.let { throw it }
        }

        suspend fun cancelReminder(event: Event) {
            calls += "cancelReminder"
            cancelReminderFailure?.let { throw it }
        }

        suspend fun cancelMilestones(event: Event) {
            calls += "cancelMilestones"
        }

        suspend fun delete(event: Event) {
            calls += "delete"
            deleteFailure?.let { throw it }
        }

        suspend fun refreshWidgets() {
            calls += "refreshWidgets"
            refreshFailure?.let { throw it }
        }
    }
}
