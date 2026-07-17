package com.example.timeapk.notifications

import android.app.Application
import android.content.Context
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

private const val MILESTONE_REMIND_TAG = "milestone_remind"
private const val MILESTONE_WORK_PREFIX = "milestone_event_"
private const val TAG = "MilestoneScheduler"
private const val MILESTONE_ERROR_PREFIX = "[Milestone] "
private val milestoneEventSyncLocks = ConcurrentHashMap<Int, Mutex>()
private val SMART_MILESTONE_REMIND_VALUES = listOf(
    1L, 3L, 7L, 14L, 30L, 60L, 90L, 100L, 180L, 365L, 520L, 730L, 1000L
)

internal data class MilestoneReminderPlan(
    val milestoneValue: Long,
    val remindAtMillis: Long
)

internal enum class MilestoneSyncOrigin {
    CALLER,
    WORKER_AFTER_NOTIFICATION
}

internal data class MilestoneRescheduleResult(val error: String?) {
    val isSuccess: Boolean
        get() = error.isNullOrBlank()
}

internal data class MilestoneCalendarInsertionAttempt(
    val result: ScheduleSyncManager.MilestoneScheduleSyncResult,
    val providerEventMayExist: Boolean
)

internal suspend fun <T> withMilestoneEventSyncLock(
    eventId: Int,
    transaction: suspend () -> T
): T = milestoneEventSyncLocks.getOrPut(eventId) { Mutex() }.withLock {
    transaction()
}

internal fun insertMilestoneWithDurableOwnership(
    markPendingDurably: () -> Boolean,
    clearPendingDurably: () -> Boolean,
    transitionInflightToActiveDurably: () -> Boolean,
    restoreInflightDurably: () -> Boolean,
    insertion: () -> MilestoneCalendarInsertionAttempt,
    insertionFailure: (Throwable) -> ScheduleSyncManager.MilestoneScheduleSyncResult,
    registryFailure: () -> ScheduleSyncManager.MilestoneScheduleSyncResult
): ScheduleSyncManager.MilestoneScheduleSyncResult {
    if (!markPendingDurably()) return registryFailure()
    val attempt = try {
        insertion()
    } catch (t: Throwable) {
        return insertionFailure(t)
    }
    if (attempt.result.scheduleEventId == null && !attempt.providerEventMayExist) {
        if (!clearPendingDurably()) return registryFailure()
    } else if (attempt.result.scheduleEventId != null) {
        if (!transitionInflightToActiveDurably()) {
            restoreInflightDurably()
            return registryFailure()
        }
    }
    return attempt.result
}

internal fun shouldCancelMilestoneWorkBeforeSync(origin: MilestoneSyncOrigin): Boolean =
    origin != MilestoneSyncOrigin.WORKER_AFTER_NOTIFICATION

internal fun mergeScheduleSyncErrors(vararg errors: String?): String? = errors
    .asSequence()
    .filterNotNull()
    .flatMap { it.split("; ").asSequence() }
    .map(String::trim)
    .filter(String::isNotBlank)
    .distinct()
    .joinToString("; ")
    .ifBlank { null }

internal fun mergeMilestoneScheduleError(
    existingError: String?,
    milestoneError: String?
): String? {
    val nonMilestoneComponents = existingError
        ?.split("; ")
        .orEmpty()
        .map(String::trim)
        .filter { it.isNotBlank() && !it.startsWith(MILESTONE_ERROR_PREFIX) }
    val taggedMilestoneError = milestoneError
        ?.removePrefix(MILESTONE_ERROR_PREFIX)
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?.let { "$MILESTONE_ERROR_PREFIX$it" }
    return (nonMilestoneComponents + listOfNotNull(taggedMilestoneError))
        .distinct()
        .joinToString("; ")
        .ifBlank { null }
}

internal fun eventAfterMilestoneScheduleSyncAttempt(
    event: Event,
    result: ScheduleSyncManager.MilestoneScheduleSyncResult?
): Event {
    if (result == null) return event
    val targetCalendarId = result.targetCalendarId ?: event.targetCalendarId
    val mergedError = mergeMilestoneScheduleError(event.lastScheduleSyncError, result.error)
    val shouldStampSyncTime =
        event.lastScheduleSyncAt == null ||
            targetCalendarId != event.targetCalendarId ||
            mergedError != event.lastScheduleSyncError
    return event.copy(
        targetCalendarId = targetCalendarId,
        lastScheduleSyncAt = if (shouldStampSyncTime) result.lastSyncAt else event.lastScheduleSyncAt,
        lastScheduleSyncError = mergedError
    )
}

internal fun requestMilestoneScheduleRetryOnFailure(
    error: String?,
    enqueueRetry: () -> Unit
): Boolean {
    if (error.isNullOrBlank()) return false
    enqueueRetry()
    return true
}

internal fun milestoneRescheduleResult(errors: Iterable<String?>): MilestoneRescheduleResult =
    MilestoneRescheduleResult(mergeScheduleSyncErrors(*errors.toList().toTypedArray()))

internal fun enqueueMilestoneScheduleRetry(context: Context) {
    RescheduleAllWorker.enqueue(context, "manual_milestone_schedule_retry")
}

fun cancelMilestoneReminders(context: android.content.Context, eventId: Int) {
    val wm = WorkManager.getInstance(context)
    wm.cancelUniqueWork("$MILESTONE_WORK_PREFIX$eventId")
    wm.cancelAllWorkByTag("${MILESTONE_REMIND_TAG}_$eventId")
}

fun cancelAllMilestoneReminders(context: android.content.Context) {
    WorkManager.getInstance(context).cancelAllWorkByTag(MILESTONE_REMIND_TAG)
}

internal fun shouldClearCalendarBeforeMilestoneSync(
    calendarCleanupHandledExternally: Boolean
): Boolean = !calendarCleanupHandledExternally

internal suspend fun syncMilestoneCalendarReplacement(
    cleanup: suspend () -> CalendarCleanupResult,
    onCleanupFailure: suspend (CalendarCleanupResult) -> ScheduleSyncManager.MilestoneScheduleSyncResult?,
    replacement: suspend () -> ScheduleSyncManager.MilestoneScheduleSyncResult?
): ScheduleSyncManager.MilestoneScheduleSyncResult? {
    val cleanupResult = cleanup()
    return if (cleanupResult.isSuccess) {
        replacement()
    } else {
        onCleanupFailure(cleanupResult)
    }
}

internal suspend fun syncMilestoneReminderForEvent(
    application: Application,
    event: Event,
    calendarCleanupHandledExternally: Boolean = false,
    origin: MilestoneSyncOrigin = MilestoneSyncOrigin.CALLER
): ScheduleSyncManager.MilestoneScheduleSyncResult? {
    val app = application as? TimeApplication ?: return null
    val context = application
    return withMilestoneEventSyncLock(event.id) {
        if (shouldCancelMilestoneWorkBeforeSync(origin)) {
            cancelMilestoneReminders(context, event.id)
        }

        syncMilestoneCalendarReplacement(
        cleanup = {
            if (shouldClearCalendarBeforeMilestoneSync(calendarCleanupHandledExternally)) {
                clearPendingMilestoneCalendarOwnership(context, event.id)
            } else {
                CalendarCleanupResult.RemovedOrNotPresent
            }
        },
        onCleanupFailure = { cleanupResult ->
            val failure = ScheduleSyncManager.MilestoneScheduleSyncResult(
                scheduleEventId = null,
                targetCalendarId = event.targetCalendarId,
                lastSyncAt = System.currentTimeMillis(),
                error = cleanupResult.message ?: "Calendar cleanup failed"
            )
            persistMilestoneScheduleStatus(app, event, failure)
            failure
        },
        replacement = {
            val enabled = app.userPrefs.milestoneRemindEnabledFlow.first()
            if (!enabled) return@syncMilestoneCalendarReplacement null

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
            if (scheduleResult != null) {
                persistMilestoneScheduleStatus(app, event, scheduleResult)
            }
            scheduleResult
        }
        )
    }
}

private suspend fun persistMilestoneScheduleStatus(
    app: TimeApplication,
    event: Event,
    scheduleResult: ScheduleSyncManager.MilestoneScheduleSyncResult
) {
    if (!event.syncToScheduleEnabled && scheduleResult.error.isNullOrBlank()) return

    val updatedEvent = eventAfterMilestoneScheduleSyncAttempt(event, scheduleResult)
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

internal suspend fun rescheduleMilestoneReminders(application: Application): MilestoneRescheduleResult {
    val app = application as? TimeApplication ?: return MilestoneRescheduleResult(null)
    val enabled = app.userPrefs.milestoneRemindEnabledFlow.first()
    if (!enabled) {
        cancelAllMilestoneReminders(application)
        val cleanup = clearAllPendingMilestoneCalendarOwnership(application)
        val result = milestoneRescheduleResult(listOf(cleanup.message))
        requestMilestoneScheduleRetryOnFailure(result.error) {
            enqueueMilestoneScheduleRetry(application)
        }
        return result
    }

    val legacyCleanup = if (MilestoneCalendarOwnershipStore.hasRecoveryPending(application)) {
        clearAllPendingMilestoneCalendarOwnership(application)
    } else {
        CalendarCleanupResult.RemovedOrNotPresent
    }
    if (!legacyCleanup.isSuccess) {
        val result = milestoneRescheduleResult(listOf(legacyCleanup.message))
        requestMilestoneScheduleRetryOnFailure(result.error) {
            enqueueMilestoneScheduleRetry(application)
        }
        return result
    }

    val events = app.repository.getAllEventsSnapshot()
    val errors = events.map { event ->
        syncMilestoneReminderForEvent(application, event)?.error
    }
    val result = milestoneRescheduleResult(errors)
    requestMilestoneScheduleRetryOnFailure(result.error) {
        enqueueMilestoneScheduleRetry(application)
    }
    return result
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
    val plan = computeNextMilestoneReminderPlan(
        event = event,
        milestones = milestones,
        remindDaysAhead = remindDaysAhead,
        remindMinuteOfDay = remindMinuteOfDay,
        smartMilestonesEnabled = smartMilestonesEnabled
    ) ?: return null

    val delayMillis = plan.remindAtMillis - System.currentTimeMillis()
    if (delayMillis <= 0) return null

    val milestoneLabel = getMilestoneLabel(context, plan.milestoneValue)
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
        milestoneScheduleResult = insertMilestoneWithDurableOwnership(
            markPendingDurably = {
                MilestoneCalendarOwnershipStore.markPendingDurably(context, event.id)
            },
            clearPendingDurably = {
                MilestoneCalendarOwnershipStore.clearPendingWithoutProviderDurably(context, event.id)
            },
            transitionInflightToActiveDurably = {
                MilestoneCalendarOwnershipStore.transitionInflightToActiveDurably(context, event.id)
            },
            restoreInflightDurably = {
                MilestoneCalendarOwnershipStore.restoreInflightDurably(context, event.id)
            },
            insertion = {
                ScheduleSyncManager.insertMilestoneScheduleReminderAttempt(
                    context = context,
                    eventId = event.id,
                    title = scheduleTitle,
                    description = event.note,
                    triggerAtMillis = plan.remindAtMillis,
                    targetCalendarId = targetCalendarId
                )
            },
            insertionFailure = { throwable ->
                ScheduleSyncManager.MilestoneScheduleSyncResult(
                    scheduleEventId = null,
                    targetCalendarId = targetCalendarId,
                    lastSyncAt = System.currentTimeMillis(),
                    error = (throwable.message ?: "Unknown milestone schedule sync error").take(180)
                )
            },
            registryFailure = {
                ScheduleSyncManager.MilestoneScheduleSyncResult(
                    scheduleEventId = null,
                    targetCalendarId = targetCalendarId,
                    lastSyncAt = System.currentTimeMillis(),
                    error = "Unable to record milestone calendar ownership"
                )
            }
        )
    }
    return milestoneScheduleResult
}

internal fun computeNextMilestoneReminderPlan(
    event: Event,
    milestones: List<Long>,
    remindDaysAhead: Int,
    remindMinuteOfDay: Int,
    smartMilestonesEnabled: Boolean,
    today: LocalDate = LocalDate.now(),
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): MilestoneReminderPlan? {
    if (event.repeatType != REPEAT_YEARLY && event.repeatType != REPEAT_NONE) return null

    val list = buildMilestonePool(milestones, smartMilestonesEnabled)
    if (list.isEmpty()) return null

    val targetDate = eventDateToLocalDate(event.date)
    val daysSinceEvent = ChronoUnit.DAYS.between(targetDate, today)
    for (milestoneValue in list) {
        if (milestoneValue <= daysSinceEvent) continue

        val milestoneDate = targetDate.plusDays(milestoneValue)
        val remindDate = milestoneDate.minusDays(remindDaysAhead.toLong())
        val remindAt = remindDate
            .atStartOfDay(zoneId)
            .plusMinutes(remindMinuteOfDay.toLong())
            .toInstant()
            .toEpochMilli()
        if (remindAt <= nowMillis) continue

        return MilestoneReminderPlan(
            milestoneValue = milestoneValue,
            remindAtMillis = remindAt
        )
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
