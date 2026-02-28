package com.example.timeapk.notifications

import android.app.Application
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.ui.home.getMilestoneLabel
import com.example.timeapk.ui.utils.eventDateToLocalDate
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

private const val MILESTONE_REMIND_TAG = "milestone_remind"
private const val DEFAULT_REMIND_MINUTE_OF_DAY = 480 // 8:00

fun cancelMilestoneReminders(context: android.content.Context, eventId: Int) {
    WorkManager.getInstance(context).cancelAllWorkByTag("${MILESTONE_REMIND_TAG}_$eventId")
}

fun cancelAllMilestoneReminders(context: android.content.Context) {
    WorkManager.getInstance(context).cancelAllWorkByTag(MILESTONE_REMIND_TAG)
}

suspend fun rescheduleMilestoneReminders(application: Application) {
    val app = application as? TimeApplication ?: return
    cancelAllMilestoneReminders(application)
    val enabled = app.userPrefs.milestoneRemindEnabledFlow.first()
    if (!enabled) return
    val daysAhead = app.userPrefs.milestoneRemindDaysAheadFlow.first()
    val milestones = app.userPrefs.customMilestonesFlow.first()
    val events = app.repository.getAllEventsSnapshot()
    scheduleMilestoneReminders(application, events, milestones, daysAhead)
}

private fun scheduleMilestoneReminders(
    context: android.content.Context,
    events: List<Event>,
    milestones: List<Long>,
    remindDaysAhead: Int
) {
    val list = milestones.filter { it > 0 }.distinct().sorted()
    if (list.isEmpty()) return
    val today = LocalDate.now()

    for (event in events) {
        if (event.repeatType != REPEAT_YEARLY && event.repeatType != REPEAT_NONE) continue
        val targetDate = eventDateToLocalDate(event.date)
        val hasStarted = !targetDate.isAfter(today)

        if (!hasStarted) continue

        val daysSinceEvent = ChronoUnit.DAYS.between(targetDate, today)
        val nextMilestoneValue = list.firstOrNull { it > daysSinceEvent } ?: continue

        val milestoneDate = targetDate.plusDays(nextMilestoneValue)
        val remindDate = milestoneDate.minusDays(remindDaysAhead.toLong())

        val remindDateTime = remindDate
            .atStartOfDay(ZoneId.systemDefault())
            .plusMinutes(DEFAULT_REMIND_MINUTE_OF_DAY.toLong())
        val remindAt = remindDateTime.toInstant().toEpochMilli()
        val delayMillis = remindAt - System.currentTimeMillis()
        if (delayMillis <= 0) continue

        val milestoneLabel = getMilestoneLabel(context, nextMilestoneValue)
        val data: Data = workDataOf(
            MilestoneReminderWorker.KEY_TITLE to event.title,
            MilestoneReminderWorker.KEY_EVENT_ID to event.id,
            MilestoneReminderWorker.KEY_MILESTONE_LABEL to milestoneLabel,
            MilestoneReminderWorker.KEY_DAYS_LEFT to remindDaysAhead
        )
        val request = OneTimeWorkRequestBuilder<MilestoneReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(MILESTONE_REMIND_TAG)
            .addTag("${MILESTONE_REMIND_TAG}_${event.id}")
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
