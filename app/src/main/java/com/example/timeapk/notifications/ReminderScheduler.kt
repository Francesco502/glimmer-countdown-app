package com.example.timeapk.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.timeapk.data.Event
import com.example.timeapk.ui.utils.eventDateToLocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

private const val REMIND_TAG_PREFIX = "remind_"

fun scheduleReminder(context: Context, event: Event) {
    // 始终先取消旧提醒，防止编辑事件后产生重复通知
    cancelReminder(context, event.id)
    if (!event.remindEnabled) {
        return
    }
    val eventLocalDate = eventDateToLocalDate(event.date)
    val remindDate = eventLocalDate.minusDays(event.remindDaysBefore.toLong())
    val remindZdt = remindDate.atTime(
        event.reminderTimeMinutesOfDay / 60,
        event.reminderTimeMinutesOfDay % 60
    ).atZone(ZoneId.systemDefault())
    val remindAtMillis = remindZdt.toInstant().toEpochMilli()
    val delayMillis = remindAtMillis - System.currentTimeMillis()
    // 若提醒时间已过，直接放弃本次调度，避免立刻弹出过期通知
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
