package com.example.timeapk.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.timeapk.TimeApplication

/**
 * Milestone reminder worker.
 */
internal enum class MilestoneWorkerCompletion {
    COMPLETE
}

internal fun handleMilestoneWorkerScheduleResult(
    result: ScheduleSyncManager.MilestoneScheduleSyncResult?,
    enqueueReschedule: () -> Unit
): MilestoneWorkerCompletion {
    requestMilestoneScheduleRetryOnFailure(result?.error, enqueueReschedule)
    return MilestoneWorkerCompletion.COMPLETE
}

class MilestoneReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val eventId = inputData.getInt(KEY_EVENT_ID, 0)
        val milestoneLabel = inputData.getString(KEY_MILESTONE_LABEL) ?: return Result.failure()
        val daysLeft = inputData.getInt(KEY_DAYS_LEFT, 0)
        val alreadyDelivered = inputData.getBoolean(KEY_ALREADY_DELIVERED, false)

        if (!alreadyDelivered) {
            ReminderNotificationDelivery.deliverMilestoneReminder(
                context = applicationContext,
                eventId = eventId,
                fallbackTitle = title,
                milestoneLabel = milestoneLabel,
                milestoneDays = inputData.getLong(KEY_MILESTONE_VALUE, 0L).takeIf { it > 0L },
                milestoneYears = inputData.getInt(KEY_MILESTONE_YEARS, 0).takeIf { it > 0 },
                daysLeft = daysLeft
            )
        }

        val scheduleResult = rescheduleMilestoneChain()
        handleMilestoneWorkerScheduleResult(scheduleResult) {
            RescheduleAllWorker.enqueue(
                applicationContext,
                "manual_milestone_worker_schedule_retry"
            )
        }
        return Result.success()
    }

    private suspend fun rescheduleMilestoneChain(): ScheduleSyncManager.MilestoneScheduleSyncResult? {
        val app = applicationContext as? TimeApplication ?: return null
        val eventId = inputData.getInt(KEY_EVENT_ID, 0)
        val event = app.repository.getEvent(eventId) ?: return null
        return syncMilestoneReminderForEvent(
            application = app,
            event = event,
            origin = MilestoneSyncOrigin.WORKER_AFTER_NOTIFICATION
        )
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_EVENT_ID = "event_id"
        const val KEY_MILESTONE_LABEL = "milestone_label"
        const val KEY_DAYS_LEFT = "days_left"
        const val KEY_MILESTONE_VALUE = "milestone_value"
        const val KEY_MILESTONE_YEARS = "milestone_years"
        /** Set by the exact-alarm receiver after it already posted the notification. */
        const val KEY_ALREADY_DELIVERED = "already_delivered"
    }
}

