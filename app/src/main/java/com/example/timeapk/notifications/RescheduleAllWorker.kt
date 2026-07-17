package com.example.timeapk.notifications

import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.concurrent.TimeUnit
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.Event
import com.example.timeapk.ui.home.calendarCleanupRequired
import com.example.timeapk.ui.home.eventAfterCleanupAttempt
import com.example.timeapk.widget.WidgetUpdater
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.security.MessageDigest

internal fun cleanupRemovedCalendarEntries(
    removedEventIds: Iterable<Int>,
    cleanup: (Int) -> CalendarCleanupResult
): Map<Int, CalendarCleanupResult> {
    val failures = linkedMapOf<Int, CalendarCleanupResult>()
    removedEventIds.forEach { eventId ->
        val result = cleanup(eventId)
        if (!result.isSuccess) {
            failures[eventId] = result
        }
    }
    return failures
}

class RescheduleAllWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? TimeApplication ?: return Result.success()
        val repository = app.repository
        val prefs = app.userPrefs
        val reason = inputData.getString(KEY_REASON) ?: "unknown"

        return try {
            val preferredCalendarId = prefs.scheduleTargetCalendarIdFlow.first()
            val useRRuleSync = prefs.scheduleUseRRuleSyncFlow.first()
            val milestoneEnabled = prefs.milestoneRemindEnabledFlow.first()
            val milestoneRemindDaysAhead = prefs.milestoneRemindDaysAheadFlow.first()
            val milestoneRemindTime = prefs.milestoneRemindTimeMinutesOfDayFlow.first()
            val customMilestones = prefs.customMilestonesFlow.first()
            val smartMilestonesEnabled = prefs.smartMilestonesEnabledFlow.first()
            val events = repository.getAllEventsSnapshot()

            val state = loadState(applicationContext)
            val preferencesFingerprint = fingerprint(
                listOf(
                    preferredCalendarId?.toString() ?: "auto_calendar",
                    useRRuleSync.toString(),
                    milestoneEnabled.toString(),
                    milestoneRemindDaysAhead.toString(),
                    milestoneRemindTime.toString(),
                    smartMilestonesEnabled.toString(),
                    customMilestones.sorted().joinToString(",")
                ).joinToString("|")
            )
            val eventFingerprints = events.associate { event -> event.id to fingerprintEvent(event) }

            val forceFullReschedule = shouldForceFullReschedule(
                reason = reason,
                previous = state,
                preferencesFingerprint = preferencesFingerprint
            )
            val removedEventIds = state.eventFingerprints.keys - eventFingerprints.keys
            val targetEvents = if (forceFullReschedule) {
                events
            } else {
                events.filter { event ->
                    state.eventFingerprints[event.id] != eventFingerprints[event.id]
                }
            }

            var shouldRetry = false

            val removedCleanupFailures = cleanupRemovedCalendarEntries(removedEventIds) { removedId ->
                try {
                    cancelReminder(applicationContext, removedId)
                    cancelMilestoneReminders(applicationContext, removedId)
                    recordManagedCalendarCleanupForMilestoneOwnership(
                        context = applicationContext,
                        eventId = removedId,
                        result = ScheduleSyncManager.removeManagedCalendarEntries(
                            context = applicationContext,
                            eventId = removedId,
                            calendarEventId = null
                        )
                    )
                } catch (t: Throwable) {
                    Log.w(TAG, "Failed to cleanup removed event $removedId", t)
                    CalendarCleanupResult.ProviderFailure(
                        t.message?.takeIf { it.isNotBlank() } ?: "Calendar cleanup failed"
                    )
                }
            }
            removedCleanupFailures.forEach { (removedId, cleanupResult) ->
                shouldRetry = true
                Log.w(TAG, "Failed to cleanup removed event $removedId: ${cleanupResult.message}")
            }

            targetEvents.forEach { event ->
                val updatedEvent = processEvent(
                    app = app,
                    event = event,
                    preferredCalendarId = preferredCalendarId,
                    useRRuleSync = useRRuleSync,
                    milestoneEnabled = milestoneEnabled
                ) { throwable ->
                    shouldRetry = true
                    Log.w(TAG, "Reschedule failed for eventId=${event.id}", throwable)
                }
                if (updatedEvent != null && updatedEvent != event) {
                    try {
                        repository.updateEvent(updatedEvent)
                    } catch (t: Throwable) {
                        shouldRetry = true
                        Log.w(TAG, "Failed to persist event sync state for eventId=${event.id}", t)
                    }
                }
            }

            if (!milestoneEnabled && (forceFullReschedule || removedEventIds.isNotEmpty() || targetEvents.isNotEmpty())) {
                try {
                    cancelAllMilestoneReminders(applicationContext)
                    val cleanup = clearAllPendingMilestoneCalendarOwnership(applicationContext)
                    if (!cleanup.isSuccess) {
                        shouldRetry = true
                        Log.w(TAG, "Failed to clear all milestone reminders: ${cleanup.message}")
                    }
                } catch (t: Throwable) {
                    shouldRetry = true
                    Log.w(TAG, "Failed to clear all milestone reminders", t)
                }
            }

            try {
                WidgetUpdater.refreshCountdownWidgets(applicationContext)
            } catch (t: Throwable) {
                shouldRetry = true
                Log.w(TAG, "Failed to refresh widgets after reschedule", t)
            }

            val completedState = completedRescheduleState(
                candidate = RescheduleState(
                    preferencesFingerprint = preferencesFingerprint,
                    eventFingerprints = eventFingerprints,
                    lastSuccessAt = System.currentTimeMillis()
                ),
                shouldRetry = shouldRetry
            )
            if (completedState == null) {
                Result.retry()
            } else {
                saveState(
                    context = applicationContext,
                    state = completedState
                )
                Result.success()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "RescheduleAllWorker failed unexpectedly (reason=$reason)", t)
            Result.retry()
        }
    }

    private suspend fun processEvent(
        app: TimeApplication,
        event: Event,
        preferredCalendarId: Long?,
        useRRuleSync: Boolean,
        milestoneEnabled: Boolean,
        onFailure: (Throwable) -> Unit
    ): Event? {
        var updatedEvent = event

        try {
            cancelReminder(applicationContext, event.id)
            if (event.remindEnabled) {
                scheduleReminder(applicationContext, event)
            }
        } catch (t: Throwable) {
            onFailure(t)
        }

        updatedEvent = if (event.syncToScheduleEnabled) {
            val syncResult = ScheduleSyncManager.syncReminderSeries(
                context = applicationContext,
                event = event,
                preferredCalendarId = preferredCalendarId,
                useRRuleSync = useRRuleSync
            )
            if (!syncResult.error.isNullOrBlank()) {
                onFailure(IllegalStateException(syncResult.error))
            }
            eventAfterScheduleSyncAttempt(event, syncResult)
        } else if (calendarCleanupRequired(event)) {
            val cleanup = recordManagedCalendarCleanupForMilestoneOwnership(
                context = applicationContext,
                eventId = event.id,
                result = ScheduleSyncManager.removeManagedCalendarEntries(
                    context = applicationContext,
                    eventId = event.id,
                    calendarEventId = event.scheduleEventId
                )
            )
            if (!cleanup.isSuccess) {
                onFailure(IllegalStateException(cleanup.message ?: "Calendar cleanup failed"))
            }
            eventAfterCleanupAttempt(
                event = event,
                result = cleanup,
                nowMillis = System.currentTimeMillis()
            )
        } else {
            event
        }

        try {
            if (milestoneEnabled) {
                val milestoneResult = syncMilestoneReminderForEvent(app, updatedEvent)
                updatedEvent = eventAfterMilestoneScheduleSyncAttempt(updatedEvent, milestoneResult)
                if (!milestoneResult?.error.isNullOrBlank()) {
                    onFailure(IllegalStateException(milestoneResult?.error))
                }
            } else {
                cancelMilestoneReminders(applicationContext, event.id)
                val cleanup = clearPendingMilestoneCalendarOwnership(
                    applicationContext,
                    event.id
                )
                if (!cleanup.isSuccess) {
                    updatedEvent = eventAfterMilestoneScheduleSyncAttempt(
                        updatedEvent,
                        ScheduleSyncManager.MilestoneScheduleSyncResult(
                            scheduleEventId = null,
                            targetCalendarId = updatedEvent.targetCalendarId,
                            lastSyncAt = System.currentTimeMillis(),
                            error = cleanup.message ?: "Calendar cleanup failed"
                        )
                    )
                    onFailure(IllegalStateException(cleanup.message ?: "Calendar cleanup failed"))
                }
            }
        } catch (t: Throwable) {
            onFailure(t)
        }
        return updatedEvent
    }

    private fun shouldForceFullReschedule(
        reason: String,
        previous: RescheduleState,
        preferencesFingerprint: String
    ): Boolean {
        if (reason in SYSTEM_REBUILD_REASONS) return true
        if (reason.startsWith("manual_")) return true
        if (previous.preferencesFingerprint.isBlank()) return true
        if (previous.preferencesFingerprint != preferencesFingerprint) return true
        if (reason == REASON_COLD_START && previous.lastSuccessAt <= 0L) return true
        return false
    }

    private fun fingerprintEvent(event: Event): String {
        val base = listOf(
            event.id.toString(),
            event.title,
            event.date.toString(),
            event.category,
            event.note,
            event.repeatType,
            event.isLunar.toString(),
            event.remindEnabled.toString(),
            event.remindDaysBefore.toString(),
            event.reminderTimeMinutesOfDay.toString(),
            event.syncToScheduleEnabled.toString(),
            event.colorHex.orEmpty()
        ).joinToString("|")
        return fingerprint(base)
    }

    private fun fingerprint(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun loadState(context: Context): RescheduleState {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val preferencesFingerprint = sp.getString(KEY_PREFS_FINGERPRINT, "") ?: ""
        val lastSuccessAt = sp.getLong(KEY_LAST_SUCCESS_AT, 0L)
        val json = sp.getString(KEY_EVENT_FINGERPRINTS, "{}") ?: "{}"
        val map = mutableMapOf<Int, String>()
        runCatching {
            val obj = JSONObject(json)
            obj.keys().forEach { key ->
                key.toIntOrNull()?.let { id ->
                    map[id] = obj.optString(key, "")
                }
            }
        }
        return RescheduleState(
            preferencesFingerprint = preferencesFingerprint,
            eventFingerprints = map,
            lastSuccessAt = lastSuccessAt
        )
    }

    private fun saveState(context: Context, state: RescheduleState) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val obj = JSONObject()
        state.eventFingerprints.forEach { (id, fp) ->
            obj.put(id.toString(), fp)
        }
        sp.edit {
            putString(KEY_PREFS_FINGERPRINT, state.preferencesFingerprint)
            putString(KEY_EVENT_FINGERPRINTS, obj.toString())
            putLong(KEY_LAST_SUCCESS_AT, state.lastSuccessAt)
        }
    }

    companion object {
        private const val TAG = "RescheduleAllWorker"
        private const val UNIQUE_WORK_NAME = "reschedule_all_work"
        private const val KEY_REASON = "reason"
        private const val PREFS_NAME = "reschedule_all_worker_state"
        private const val KEY_PREFS_FINGERPRINT = "prefs_fingerprint"
        private const val KEY_EVENT_FINGERPRINTS = "event_fingerprints"
        private const val KEY_LAST_SUCCESS_AT = "last_success_at"
        private const val REASON_COLD_START = "cold_start"
        private val SYSTEM_REBUILD_REASONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )

        fun enqueue(context: Context, reason: String) {
            val input: Data = workDataOf(KEY_REASON to reason)
            val request = OneTimeWorkRequestBuilder<RescheduleAllWorker>()
                .setInputData(input)
                .setInitialDelay(500, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

internal data class RescheduleState(
    val preferencesFingerprint: String,
    val eventFingerprints: Map<Int, String>,
    val lastSuccessAt: Long
)

internal fun completedRescheduleState(
    candidate: RescheduleState,
    shouldRetry: Boolean
): RescheduleState? = candidate.takeUnless { shouldRetry }
