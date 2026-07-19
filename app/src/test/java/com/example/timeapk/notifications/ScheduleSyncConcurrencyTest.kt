package com.example.timeapk.notifications

import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.eventWithScheduleSyncStateIfInputsUnchanged
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ScheduleSyncConcurrencyTest {
    @Test
    fun twoSameEventProviderTransactionsNeverOverlap() {
        val executor = Executors.newFixedThreadPool(2)
        val firstEntered = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondEntered = CountDownLatch(1)
        val active = AtomicInteger(0)
        val maximumActive = AtomicInteger(0)

        try {
            val first = executor.submit {
                withScheduleEventProviderLock(41) {
                    val nowActive = active.incrementAndGet()
                    maximumActive.accumulateAndGet(nowActive, ::maxOf)
                    firstEntered.countDown()
                    assertTrue(releaseFirst.await(5, TimeUnit.SECONDS))
                    active.decrementAndGet()
                }
            }
            assertTrue(firstEntered.await(5, TimeUnit.SECONDS))
            val second = executor.submit {
                withScheduleEventProviderLock(41) {
                    val nowActive = active.incrementAndGet()
                    maximumActive.accumulateAndGet(nowActive, ::maxOf)
                    secondEntered.countDown()
                    active.decrementAndGet()
                }
            }

            assertFalse(secondEntered.await(150, TimeUnit.MILLISECONDS))
            releaseFirst.countDown()
            first.get(5, TimeUnit.SECONDS)
            second.get(5, TimeUnit.SECONDS)

            assertEquals(1, maximumActive.get())
        } finally {
            releaseFirst.countDown()
            executor.shutdownNow()
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
    fun backgroundSyncUsesAtomicScheduleOnlyPersistenceAndRetriesCasMisses() {
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
        assertTrue(reminderWorker.contains("Result.retry()"))
        assertTrue(rescheduleWorker.contains("shouldRetry = true"))
        assertTrue(milestoneScheduler.contains("EVENT_CHANGED_DURING_SCHEDULE_SYNC"))
    }

    @Test
    fun regularProviderEntrypointsUseTheSharedPerEventCoordinator() {
        val manager = source("notifications/ScheduleSyncManager.kt")
        val coordinator = source("notifications/ScheduleEventCoordinator.kt")

        assertTrue(coordinator.contains("ConcurrentHashMap<Int, ReentrantLock>()"))
        assertTrue(coordinator.contains("withScheduleEventProviderLock("))
        val regularSync = manager.substringAfter("fun syncReminderSeries(")
            .substringBefore("private fun syncReminderSeriesLocked(")
        assertTrue(regularSync.contains("withScheduleEventProviderLock(event.id)"))
        listOf(
            "fun insertMilestoneScheduleReminderAttempt(",
            "fun clearMilestoneScheduleRemindersByEventId(",
            "fun removeScheduleReminderByEventId(context:",
            "fun removeManagedCalendarEntries("
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
