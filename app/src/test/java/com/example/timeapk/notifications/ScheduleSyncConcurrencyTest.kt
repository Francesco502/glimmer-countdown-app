package com.example.timeapk.notifications

import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.eventWithScheduleSyncStateIfInputsUnchanged
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class ScheduleSyncConcurrencyTest {
    @Test
    fun twoSameEventProviderTransactionsNeverOverlap() = runBlocking {
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondEntered = CompletableDeferred<Unit>()
        val active = AtomicInteger(0)
        val maximumActive = AtomicInteger(0)

        val first = async {
            withScheduleEventProviderLock(41) {
                val nowActive = active.incrementAndGet()
                maximumActive.accumulateAndGet(nowActive, ::maxOf)
                firstEntered.complete(Unit)
                releaseFirst.await()
                active.decrementAndGet()
            }
        }
        firstEntered.await()
        val second = async {
            withScheduleEventProviderLock(41) {
                val nowActive = active.incrementAndGet()
                maximumActive.accumulateAndGet(nowActive, ::maxOf)
                secondEntered.complete(Unit)
                active.decrementAndGet()
            }
        }

        delay(100)
        assertFalse(secondEntered.isCompleted)
        releaseFirst.complete(Unit)
        withTimeout(5_000) {
            first.await()
            second.await()
        }

        assertEquals(1, maximumActive.get())
    }

    @Test
    fun differentEventProviderTransactionsCanRunConcurrently() = runBlocking {
        val bothEntered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val active = AtomicInteger(0)
        val maximumActive = AtomicInteger(0)

        suspend fun enter(eventId: Int) = withScheduleEventProviderLock(eventId) {
            val nowActive = active.incrementAndGet()
            maximumActive.accumulateAndGet(nowActive, ::maxOf)
            if (nowActive == 2) bothEntered.complete(Unit)
            release.await()
            active.decrementAndGet()
        }

        val first = async { enter(41) }
        val second = async { enter(42) }
        withTimeout(5_000) { bothEntered.await() }
        release.complete(Unit)
        first.await()
        second.await()

        assertEquals(2, maximumActive.get())
    }

    @Test
    fun cancellationWhileWaitingNeverEntersProviderTransaction() = runBlocking {
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val waiterStarted = CompletableDeferred<Unit>()
        var cancelledTransactionEntered = false

        val first = launch {
            withScheduleEventProviderLock(43) {
                firstEntered.complete(Unit)
                releaseFirst.await()
            }
        }
        firstEntered.await()
        val cancelledWaiter = launch {
            waiterStarted.complete(Unit)
            withScheduleEventProviderLock(43) {
                cancelledTransactionEntered = true
            }
        }
        waiterStarted.await()
        delay(100)
        cancelledWaiter.cancelAndJoin()
        releaseFirst.complete(Unit)
        first.join()
        delay(100)

        assertFalse(cancelledTransactionEntered)
    }

    @Test
    fun callerThreadNeverRunsBlockingProviderTransaction() = runBlocking {
        val callerDispatcher = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "schedule-sync-test-main")
        }.asCoroutineDispatcher()
        try {
            var callerThread = ""
            var transactionThread = ""
            withContext(callerDispatcher) {
                callerThread = Thread.currentThread().name
                withScheduleEventProviderLock(44) {
                    transactionThread = Thread.currentThread().name
                }
            }

            assertTrue(callerThread.startsWith("schedule-sync-test-main"))
            assertNotEquals(callerThread, transactionThread)
        } finally {
            callerDispatcher.close()
        }
    }

    @Test
    fun editedScheduleInputsRejectStaleScheduleStateWithoutOverwritingEdit() {
        val expected = event(title = "Before", note = "old note")
        val concurrentlyEdited = expected.copy(
            title = "After",
            date = expected.date + 86_400_000L,
            note = "new note",
            reminderTimeMinutesOfDay = 11 * 60
        )
        val staleSyncOutput = expected.copy(
            scheduleEventId = 900L,
            targetCalendarId = 8L,
            lastScheduleSyncAt = 1234L,
            lastScheduleSyncError = null
        )

        val result = eventWithScheduleSyncStateIfInputsUnchanged(
            current = concurrentlyEdited,
            expected = expected,
            updated = staleSyncOutput
        )

        assertNull(result)
        assertEquals("After", concurrentlyEdited.title)
        assertEquals("new note", concurrentlyEdited.note)
        assertEquals(11 * 60, concurrentlyEdited.reminderTimeMinutesOfDay)
    }

    @Test
    fun unchangedInputsApplyOnlyScheduleState() {
        val current = event(title = "Current", note = "note")
        val updated = current.copy(
            scheduleEventId = 901L,
            targetCalendarId = 9L,
            lastScheduleSyncAt = 5678L,
            lastScheduleSyncError = "provider warning"
        )

        val result = eventWithScheduleSyncStateIfInputsUnchanged(current, current, updated)

        assertEquals(updated, result)
    }

    @Test
    fun backgroundSyncUsesAtomicScheduleOnlyPersistenceAndRepairsCasMisses() {
        val dao = source("data/EventDao.kt")
        val repository = source("data/EventRepository.kt")
        val reminderWorker = source("notifications/ReminderWorker.kt")
        val rescheduleWorker = source("notifications/RescheduleAllWorker.kt")
        val milestoneScheduler = source("notifications/MilestoneReminderScheduler.kt")

        val atomicQuery = dao.substringAfter("suspend fun updateScheduleSyncStateIfInputsUnchanged(")
        assertTrue(dao.contains("UPDATE events"))
        assertTrue(dao.contains("scheduleEventId = :scheduleEventId"))
        assertTrue(dao.contains("targetCalendarId = :targetCalendarId"))
        assertTrue(dao.contains("lastScheduleSyncAt = :lastScheduleSyncAt"))
        assertTrue(dao.contains("lastScheduleSyncError = :lastScheduleSyncError"))
        listOf(
            "title = :expectedTitle",
            "date = :expectedDate",
            "note = :expectedNote",
            "repeatType = :expectedRepeatType",
            "remindDaysBefore = :expectedRemindDaysBefore",
            "reminderTimeMinutesOfDay = :expectedReminderTimeMinutesOfDay",
            "remindEnabled = :expectedRemindEnabled",
            "syncToScheduleEnabled = :expectedSyncToScheduleEnabled",
            "isLunar = :expectedIsLunar"
        ).forEach { expectedPredicate ->
            assertTrue(expectedPredicate, dao.contains(expectedPredicate))
        }
        assertFalse(atomicQuery.contains("title = :title"))
        assertTrue(repository.contains("suspend fun updateScheduleSyncState("))

        listOf(reminderWorker, rescheduleWorker, milestoneScheduler).forEach { backgroundSource ->
            assertTrue(backgroundSource.contains("updateScheduleSyncState("))
        }
        assertFalse(reminderWorker.contains("repository.updateEvent(updatedEvent)"))
        assertFalse(rescheduleWorker.contains("repository.updateEvent(updatedEvent)"))
        assertFalse(milestoneScheduler.contains("app.repository.updateEvent(updatedEvent)"))
        assertFalse(reminderWorker.contains("Result.retry()"))
        assertTrue(reminderWorker.contains("REMINDER_DELIVERY_REPAIR_REASON"))
        assertTrue(reminderWorker.contains("RescheduleAllWorker.enqueue("))
        assertTrue(rescheduleWorker.contains("shouldRetry = true"))
        assertTrue(milestoneScheduler.contains("EVENT_CHANGED_DURING_SCHEDULE_SYNC"))
    }

    @Test
    fun regularProviderEntrypointsUseTheSharedPerEventCoordinator() {
        val manager = source("notifications/ScheduleSyncManager.kt")
        val coordinator = source("notifications/ScheduleEventCoordinator.kt")

        assertTrue(coordinator.contains("ConcurrentHashMap<Int, Mutex>()"))
        assertTrue(coordinator.contains("suspend fun <T> withScheduleEventProviderLock("))
        assertTrue(coordinator.contains("withScheduleEventProviderLock("))
        assertTrue(coordinator.contains("withContext(Dispatchers.IO)"))
        assertFalse(coordinator.contains("ReentrantLock"))
        assertFalse(coordinator.contains("runBlocking"))
        val regularSync = manager.substringAfter("suspend fun syncReminderSeries(")
            .substringBefore("private fun syncReminderSeriesLocked(")
        assertTrue(regularSync.contains("withScheduleEventProviderLock(event.id)"))
        listOf(
            "suspend fun insertMilestoneScheduleReminderAttempt(",
            "suspend fun clearMilestoneScheduleRemindersByEventId(",
            "suspend fun removeScheduleReminderByEventId(",
            "suspend fun removeManagedCalendarEntries("
        ).forEach { entrypoint ->
            val body = manager.substringAfter(entrypoint).substringBefore("\n    }")
            assertTrue(entrypoint, body.contains("withScheduleEventProviderLock(eventId)"))
        }
    }

    private fun event(title: String, note: String) = Event(
        id = 41,
        title = title,
        date = 1_800_000_000_000L,
        category = CATEGORY_OTHER,
        note = note,
        remindDaysBefore = 3,
        reminderTimeMinutesOfDay = 10 * 60,
        remindEnabled = true,
        syncToScheduleEnabled = true
    )

    private fun source(relative: String): String = listOf(
        File("src/main/java/com/example/timeapk/$relative"),
        File("app/src/main/java/com/example/timeapk/$relative")
    ).firstOrNull(File::exists)?.readText(Charsets.UTF_8)
        ?: error("Missing source: $relative")
}
