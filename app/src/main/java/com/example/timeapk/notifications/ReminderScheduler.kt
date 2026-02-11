package com.example.timeapk.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.timeapk.data.Event
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val REMIND_TAG_PREFIX = "remind_"

fun scheduleReminder(context: Context, event: Event) {
    // 始终先取消旧提醒，防止编辑事件后产生重复通知
    cancelReminder(context, event.id)
    if (!event.remindEnabled) {
        return
    }
    val remindDayMillis = event.date - event.remindDaysBefore * 24L * 60 * 60 * 1000
    val cal = Calendar.getInstance()
    cal.timeInMillis = remindDayMillis
    cal.set(Calendar.HOUR_OF_DAY, event.reminderTimeMinutesOfDay / 60)
    cal.set(Calendar.MINUTE, event.reminderTimeMinutesOfDay % 60)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val remindAtMillis = cal.timeInMillis
    var delayMillis = remindAtMillis - System.currentTimeMillis()
    if (delayMillis < 0) delayMillis = 0
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
