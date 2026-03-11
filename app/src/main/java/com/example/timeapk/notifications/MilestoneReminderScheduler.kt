package com.example.timeapk.notifications

import android.app.Application
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.timeapk.R
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
private const val MILESTONE_WORK_PREFIX = "milestone_event_"
private const val TAG = "MilestoneScheduler"
private val SMART_MILESTONE_REMIND_VALUES = listOf(
    1L, 3L, 7L, 14L, 30L, 60L, 90L, 100L, 180L, 365L, 520L, 730L, 1000L
)

fun cancelMilestoneReminders(context: android.content.Context, eventId: Int) {
    val wm = WorkManager.getInstance(context)
    wm.cancelUniqueWork("$MILESTONE_WORK_PREFIX$eventId")
    wm.cancelAllWorkByTag("${MILESTONE_REMIND_TAG}_$eventId")
}

fun cancelAllMilestoneReminders(context: android.content.Context) {
    WorkManager.getInstance(context).cancelAllWorkByTag(MILESTONE_REMIND_TAG)
}

suspend fun syncMilestoneReminderForEvent(application: Application, event: Event) {
    val app = application as? TimeApplication ?: return
    val context = application
    cancelMilestoneReminders(context, event.id)
    ScheduleSyncManager.clearMilestoneScheduleRemindersByEventId(context, event.id)

    val enabled = app.userPrefs.milestoneRemindEnabledFlow.first()
    if (!enabled) return

    val daysAhead = app.userPrefs.milestoneRemindDaysAheadFlow.first()
    val remindMinuteOfDay = app.userPrefs.milestoneRemindTimeMinutesOfDayFlow.first()
    val milestones = app.userPrefs.customMilestonesFlow.first()
    val smartMilestonesEnabled = app.userPrefs.smartMilestonesEnabledFlow.first()
    val preferredCalendarId = app.userPrefs.scheduleTargetCalendarIdFlow.first()

    val scheduleResult = scheduleMilestoneReminderForEvent(
        context = context,
        event = event,
        milestones = milestones,
        remindDaysAhead = daysAhead,
        remindMinuteOfDay = remindMinuteOfDay,
        smartMilestonesEnabled = smartMilestonesEnabled,
        targetCalendarId = preferredCalendarId
    )

    if (scheduleResult != null && event.syncToScheduleEnabled) {
        val shouldStampSyncTime =
            event.lastScheduleSyncAt == null ||
                scheduleResult.targetCalendarId != event.targetCalendarId ||
                scheduleResult.error != event.lastScheduleSyncError
        val updatedEvent = event.copy(
            targetCalendarId = scheduleResult.targetCalendarId ?: event.targetCalendarId,
            lastScheduleSyncAt = if (shouldStampSyncTime) scheduleResult.lastSyncAt else event.lastScheduleSyncAt,
            lastScheduleSyncError = scheduleResult.error
        )
        if (updatedEvent != event) {
            try {
                app.repository.updateEvent(updatedEvent)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to persist milestone schedule status for eventId=${event.id}", t)
            }
        }
        if (!scheduleResult.error.isNullOrBlank()) {
            Log.w(TAG, "Milestone schedule sync warning for eventId=${event.id}: ${scheduleResult.error}")
        }
    }
}

suspend fun rescheduleMilestoneReminders(application: Application) {
    val app = application as? TimeApplication ?: return
    val enabled = app.userPrefs.milestoneRemindEnabledFlow.first()
    if (!enabled) {
        cancelAllMilestoneReminders(application)
        ScheduleSyncManager.clearAllMilestoneScheduleReminders(application)
        return
    }

    val events = app.repository.getAllEventsSnapshot()
    events.forEach { event ->
        syncMilestoneReminderForEvent(application, event)
    }
}

private fun scheduleMilestoneReminderForEvent(
    context: android.content.Context,
    event: Event,
    milestones: List<Long>,
    remindDaysAhead: Int,
    remindMinuteOfDay: Int,
    smartMilestonesEnabled: Boolean,
    targetCalendarId: Long?
): ScheduleSyncManager.MilestoneScheduleSyncResult? {
    if (event.repeatType != REPEAT_YEARLY && event.repeatType != REPEAT_NONE) return null

    val list = buildMilestonePool(milestones, smartMilestonesEnabled)
    if (list.isEmpty()) return null

    val today = LocalDate.now()
    val targetDate = eventDateToLocalDate(event.date)
    if (targetDate.isAfter(today)) return null

    val daysSinceEvent = ChronoUnit.DAYS.between(targetDate, today)
    for (milestoneValue in list) {
        if (milestoneValue <= daysSinceEvent) continue

        val milestoneDate = targetDate.plusDays(milestoneValue)
        val remindDate = milestoneDate.minusDays(remindDaysAhead.toLong())
        val remindAt = remindDate
            .atStartOfDay(ZoneId.systemDefault())
            .plusMinutes(remindMinuteOfDay.toLong())
            .toInstant()
            .toEpochMilli()
        val delayMillis = remindAt - System.currentTimeMillis()
        if (delayMillis <= 0) continue

        val milestoneLabel = getMilestoneLabel(context, milestoneValue)
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
        WorkManager.getInstance(context).enqueueUniqueWork(
            "$MILESTONE_WORK_PREFIX${event.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )

        var milestoneScheduleResult: ScheduleSyncManager.MilestoneScheduleSyncResult? = null
        if (event.syncToScheduleEnabled) {
            val scheduleTitle = if (remindDaysAhead == 0) {
                context.getString(
                    R.string.schedule_milestone_reminder_title_today_format,
                    event.title,
                    milestoneLabel
                )
            } else {
                context.resources.getQuantityString(
                    R.plurals.schedule_milestone_reminder_title_format,
                    remindDaysAhead,
                    event.title,
                    milestoneLabel,
                    remindDaysAhead
                )
            }
            milestoneScheduleResult = ScheduleSyncManager.insertMilestoneScheduleReminderWithStatus(
                context = context,
                eventId = event.id,
                title = scheduleTitle,
                description = event.note,
                triggerAtMillis = remindAt,
                targetCalendarId = targetCalendarId
            )
        }
        return milestoneScheduleResult
    }
    return null
}

private fun buildMilestonePool(milestones: List<Long>, smartMilestonesEnabled: Boolean): List<Long> {
    val base = milestones.filter { it > 0 }
    return if (smartMilestonesEnabled) {
        (base + SMART_MILESTONE_REMIND_VALUES).distinct().sorted()
    } else {
        base.distinct().sorted()
    }
}
