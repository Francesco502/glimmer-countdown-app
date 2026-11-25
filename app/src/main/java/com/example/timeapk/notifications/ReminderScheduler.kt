package com.example.timeapk.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.permissions.hasCalendarReadWritePermission
import java.util.concurrent.TimeUnit

private const val REMIND_TAG_PREFIX = "remind_"
private const val REMIND_WORK_PREFIX = "remind_event_"
internal const val REMIND_EXACT_FOLLOW_UP_WORK_PREFIX = "remind_exact_follow_up_"

fun scheduleReminder(context: Context, event: Event): Boolean = scheduleReminder(context, event, fromWorker = false)

internal fun scheduleReminder(context: Context, event: Event, fromWorker: Boolean): Boolean {
    if (!fromWorker) cancelReminder(context, event.id)
    val calendarOnly = !event.remindEnabled && event.syncToScheduleEnabled &&
        event.repeatType != REPEAT_NONE && context.hasCalendarReadWritePermission()
    if (event.isArchived || !event.remindEnabled && !calendarOnly) return false

    val trigger = computeNextReminderTrigger(event) ?: return false
    val delayMillis = trigger.triggerAtMillis - System.currentTimeMillis()
    if (delayMillis <= 0) {
        return false
    }

    val data: Data = workDataOf(
        ReminderWorker.KEY_TITLE to event.title,
        ReminderWorker.KEY_EVENT_ID to event.id,
        ReminderWorker.KEY_DAYS_LEFT to trigger.daysLeft,
        ReminderWorker.KEY_TRIGGER_AT_MILLIS to trigger.triggerAtMillis,
        ReminderWorker.KEY_ALREADY_DELIVERED to calendarOnly
    )
    if (!calendarOnly && ExactReminderAlarms.currentPath(context) == ReminderDeliveryPath.EXACT_ALARM &&
        ExactReminderAlarms.schedule(
            context = context,
            kind = ExactAlarmKind.EVENT_REMINDER,
            eventId = event.id,
            triggerAtMillis = trigger.triggerAtMillis,
            payload = data
        )
    ) {
        return true
    }

    val request = OneTimeWorkRequestBuilder<ReminderWorker>()
        // Give CalendarProvider time to deliver its own alert before rolling the series forward.
        .setInitialDelay(delayMillis + if (calendarOnly) 60_000 else 0, TimeUnit.MILLISECONDS)
        .setInputData(data)
        .addTag("$REMIND_TAG_PREFIX${event.id}")
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        "$REMIND_WORK_PREFIX${event.id}",
        if (fromWorker) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE,
        request
    )
    return true
}

/** Clears both delivery paths so switching paths can never leave a duplicate armed. */
fun cancelReminder(context: Context, eventId: Int) {
    val wm = WorkManager.getInstance(context)
    wm.cancelUniqueWork("$REMIND_WORK_PREFIX$eventId")
    wm.cancelAllWorkByTag("$REMIND_TAG_PREFIX$eventId")
    ExactReminderAlarms.cancel(context, ExactAlarmKind.EVENT_REMINDER, eventId)
}
