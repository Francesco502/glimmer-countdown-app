package com.example.timeapk.notifications

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderWorkerDeliveryPolicyTest {
    @Test
    fun postDeliveryCasFailureEnqueuesRepairWithoutSelfRetry() {
        val repairReasons = mutableListOf<String>()

        completeReminderDeliveryScheduleRepair(scheduleStatePersisted = false) { reason ->
            repairReasons += reason
        }

        assertEquals(listOf(REMINDER_DELIVERY_REPAIR_REASON), repairReasons)
    }

    @Test
    fun successfulPostDeliveryPersistenceDoesNotEnqueueRepair() {
        var repairEnqueued = false

        completeReminderDeliveryScheduleRepair(scheduleStatePersisted = true) {
            repairEnqueued = true
        }

        assertFalse(repairEnqueued)
    }

    @Test
    fun reminderWorkerAlwaysCompletesDeliveredWorkWithoutRetry() {
        val source = source("notifications/ReminderWorker.kt")

        assertFalse(source.contains("Result.retry()"))
        assertTrue(source.contains("completeReminderDeliveryScheduleRepair("))
        assertTrue(source.contains("RescheduleAllWorker.enqueue("))
        assertTrue(source.contains("REMINDER_DELIVERY_REPAIR_REASON"))
    }

    @Test
    fun renamedEventNotificationUsesLatestTitleNotScheduledTitle() {
        val renamed = com.example.timeapk.data.Event(
            id = 3,
            title = "妈妈生日",
            date = 0L,
            category = com.example.timeapk.data.CATEGORY_BIRTHDAY,
            remindEnabled = true
        )

        val decision = ReminderNotificationTextPolicy.decide(
            latest = renamed,
            lookupFailed = false,
            fallbackTitle = "OSDraft-old",
            reminderStillEnabled = { it.remindEnabled }
        ) as ReminderDeliveryDecision.Deliver

        assertEquals("妈妈生日", decision.title)
        assertTrue(source("notifications/ReminderWorker.kt").contains("fallbackTitle = title"))
    }

    private fun source(relative: String): String = listOf(
        File("src/main/java/com/example/timeapk/$relative"),
        File("app/src/main/java/com/example/timeapk/$relative")
    ).firstOrNull(File::exists)?.readText(Charsets.UTF_8)
        ?: error("Missing source: $relative")
}
