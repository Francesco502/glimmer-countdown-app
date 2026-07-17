package com.example.timeapk.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class MilestoneReminderWorkerPolicyTest {

    @Test
    fun failureAfterNotification_enqueuesDistinctRescheduleAndCompletesCurrentWork() {
        var enqueueCount = 0
        val result = ScheduleSyncManager.MilestoneScheduleSyncResult(
            scheduleEventId = null,
            targetCalendarId = 5L,
            lastSyncAt = 123L,
            error = "provider down"
        )

        val completion = handleMilestoneWorkerScheduleResult(result) {
            enqueueCount += 1
        }

        assertEquals(MilestoneWorkerCompletion.COMPLETE, completion)
        assertEquals(1, enqueueCount)
    }

    @Test
    fun successAfterNotification_completesWithoutAnotherJob() {
        var enqueueCount = 0

        val completion = handleMilestoneWorkerScheduleResult(null) {
            enqueueCount += 1
        }

        assertEquals(MilestoneWorkerCompletion.COMPLETE, completion)
        assertEquals(0, enqueueCount)
    }
}
