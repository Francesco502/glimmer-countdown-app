package com.example.timeapk.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.timeapk.data.Event
import java.util.concurrent.TimeUnit

private const val REMIND_TAG_PREFIX = "remind_"
private const val REMIND_WORK_PREFIX = "remind_event_"

fun scheduleReminder(context: Context, event: Event): Boolean {
    cancelReminder(context, event.id)
    if (!event.remindEnabled) {
        return false
    }

    val trigger = computeNextReminderTrigger(event) ?: return false
    val delayMillis = trigger.triggerAtMillis - System.currentTimeMillis()
    if (delayMillis <= 0) {
        return false
    }

    val data: Data = workDataOf(
        ReminderWorker.KEY_TITLE to event.title,
        ReminderWorker.KEY_EVENT_ID to event.id,
        ReminderWorker.KEY_DAYS_LEFT to trigger.daysLeft
    )
    val request = OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .setInputData(data)
        .addTag("$REMIND_TAG_PREFIX${event.id}")
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        "$REMIND_WORK_PREFIX${event.id}",
        ExistingWorkPolicy.REPLACE,
        request
    )
    return true
}

fun cancelReminder(context: Context, eventId: Int) {
    val wm = WorkManager.getInstance(context)
    wm.cancelUniqueWork("$REMIND_WORK_PREFIX$eventId")
    wm.cancelAllWorkByTag("$REMIND_TAG_PREFIX$eventId")
}
