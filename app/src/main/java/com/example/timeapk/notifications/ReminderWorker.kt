package com.example.timeapk.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.timeapk.TimeApplication
import com.example.timeapk.ui.home.eventAfterCleanupAttempt
import kotlinx.coroutines.flow.first

internal const val REMINDER_DELIVERY_REPAIR_REASON = "reminder_delivery_schedule_state_conflict"

internal fun completeReminderDeliveryScheduleRepair(
    scheduleStatePersisted: Boolean,
    enqueueRepair: (String) -> Unit
) {
    if (!scheduleStatePersisted) {
        enqueueRepair(REMINDER_DELIVERY_REPAIR_REASON)
    }
}

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val eventId = inputData.getInt(KEY_EVENT_ID, 0)
        val daysLeft = inputData.getInt(KEY_DAYS_LEFT, 0)
        val alreadyDelivered = inputData.getBoolean(KEY_ALREADY_DELIVERED, false)

        if (!alreadyDelivered) {
            ReminderNotificationDelivery.deliverEventReminder(
                context = applicationContext,
                eventId = eventId,
                fallbackTitle = title,
                workDaysLeft = daysLeft,
                triggerAtMillis = inputData.getLong(KEY_TRIGGER_AT_MILLIS, 0L)
            )
        }

        completeReminderDeliveryScheduleRepair(
            scheduleStatePersisted = rescheduleNextReminder(eventId),
            enqueueRepair = { reason -> RescheduleAllWorker.enqueue(applicationContext, reason) }
        )
        return Result.success()
    }

    private suspend fun rescheduleNextReminder(eventId: Int): Boolean {
        val app = applicationContext as? TimeApplication ?: return true
        val repository = app.repository
        val event = repository.getEvent(eventId) ?: return true
        val preferredCalendarId = app.userPrefs.scheduleTargetCalendarIdFlow.first()
        val useRRuleSync = app.userPrefs.scheduleUseRRuleSyncFlow.first()

        if (event.remindEnabled) {
            scheduleReminder(applicationContext, event)
        } else {
            cancelReminder(applicationContext, event.id)
        }

        val updatedEvent = if (event.syncToScheduleEnabled) {
            val syncResult = ScheduleSyncManager.syncReminderSeries(
                context = applicationContext,
                event = event,
                preferredCalendarId = preferredCalendarId,
                useRRuleSync = useRRuleSync
            )
            eventAfterScheduleSyncAttempt(event, syncResult)
        } else {
            val cleanup = ScheduleSyncManager.removeManagedCalendarEntries(
                context = applicationContext,
                event = event,
                repairReason = "manual_reminder_worker_cleanup"
            )
            eventAfterCleanupAttempt(
                event = event,
                result = cleanup,
                nowMillis = System.currentTimeMillis()
            )
        }

        return updatedEvent == event || repository.updateScheduleSyncState(event, updatedEvent)
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_EVENT_ID = "event_id"
        const val KEY_DAYS_LEFT = "days_left"
        const val KEY_TRIGGER_AT_MILLIS = "trigger_at_millis"
        /** Set by the exact-alarm receiver after it already posted the notification. */
        const val KEY_ALREADY_DELIVERED = "already_delivered"
    }
}
