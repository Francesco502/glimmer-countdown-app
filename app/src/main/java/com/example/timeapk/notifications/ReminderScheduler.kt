package com.example.timeapk.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.timeapk.data.Event
import java.util.concurrent.TimeUnit

private const val REMIND_TAG_PREFIX = "remind_"

fun scheduleReminder(context: Context, event: Event) {
    cancelReminder(context, event.id)
    if (!event.remindEnabled) {
        return
    }

    val remindAtMillis = computeNextReminderTriggerAtMillis(event) ?: return
    val delayMillis = remindAtMillis - System.currentTimeMillis()
    if (delayMillis <= 0) {
        return
    }

    val data: Data = workDataOf(
        ReminderWorker.KEY_TITLE to event.title,
        ReminderWorker.KEY_EVENT_ID to event.id,
        ReminderWorker.KEY_DAYS_LEFT to event.remindDaysBefore
    )
    val request = OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .setInputData(data)
        .addTag("$REMIND_TAG_PREFIX${event.id}")
        .build()
    WorkManager.getInstance(context).enqueue(request)
}

fun cancelReminder(context: Context, eventId: Int) {
    WorkManager.getInstance(context).cancelAllWorkByTag("$REMIND_TAG_PREFIX$eventId")
}
