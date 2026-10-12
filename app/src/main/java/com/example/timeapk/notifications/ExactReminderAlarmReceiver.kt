package com.example.timeapk.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal const val MILESTONE_EXACT_FOLLOW_UP_WORK_PREFIX = "milestone_exact_follow_up_"

/**
 * Fires for reminders armed with AlarmManager.setExactAndAllowWhileIdle. Posts the notification right away
 * through the same delivery code as the workers, then hands rescheduling and calendar sync to a worker
 * (flagged as already delivered) so the slow part does not run inside the broadcast window.
 */
class ExactReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val kind = ExactAlarmKind.entries.firstOrNull { it.action == action } ?: return
        val extras = intent.extras ?: return
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        receiverScope.launch {
            var delivered = false
            try {
                delivered = withTimeoutOrNull(DELIVERY_TIMEOUT_MILLIS) {
                    deliver(appContext, kind, intent)
                    true
                } ?: false
            } catch (t: Throwable) {
                Log.w(TAG, "Exact reminder delivery failed; follow-up worker will retry delivery", t)
            } finally {
                try {
                    enqueueFollowUp(appContext, kind, extras.toWorkData(), delivered)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun deliver(context: Context, kind: ExactAlarmKind, intent: Intent) {
        when (kind) {
            ExactAlarmKind.EVENT_REMINDER -> {
                val title = intent.getStringExtra(ReminderWorker.KEY_TITLE) ?: return
                ReminderNotificationDelivery.deliverEventReminder(
                    context = context,
                    eventId = intent.getIntExtra(ReminderWorker.KEY_EVENT_ID, 0),
                    fallbackTitle = title,
                    workDaysLeft = intent.getIntExtra(ReminderWorker.KEY_DAYS_LEFT, 0),
                    triggerAtMillis = intent.getLongExtra(ReminderWorker.KEY_TRIGGER_AT_MILLIS, 0L)
                )
            }
            ExactAlarmKind.MILESTONE_REMINDER -> {
                val title = intent.getStringExtra(MilestoneReminderWorker.KEY_TITLE) ?: return
                val label = intent.getStringExtra(MilestoneReminderWorker.KEY_MILESTONE_LABEL) ?: return
                ReminderNotificationDelivery.deliverMilestoneReminder(
                    context = context,
                    eventId = intent.getIntExtra(MilestoneReminderWorker.KEY_EVENT_ID, 0),
                    fallbackTitle = title,
                    milestoneLabel = label,
                    milestoneDays = intent.getLongExtra(MilestoneReminderWorker.KEY_MILESTONE_VALUE, 0L)
                        .takeIf { it > 0L },
                    milestoneYears = intent.getIntExtra(MilestoneReminderWorker.KEY_MILESTONE_YEARS, 0)
                        .takeIf { it > 0 },
                    daysLeft = intent.getIntExtra(MilestoneReminderWorker.KEY_DAYS_LEFT, 0)
                )
            }
        }
    }

    private fun enqueueFollowUp(context: Context, kind: ExactAlarmKind, payload: Data, delivered: Boolean) {
        val wm = WorkManager.getInstance(context)
        when (kind) {
            ExactAlarmKind.EVENT_REMINDER -> {
                val eventId = payload.getInt(ReminderWorker.KEY_EVENT_ID, 0)
                val input = Data.Builder()
                    .putAll(payload)
                    .putBoolean(ReminderWorker.KEY_ALREADY_DELIVERED, delivered)
                    .build()
                wm.enqueueUniqueWork(
                    "$REMIND_EXACT_FOLLOW_UP_WORK_PREFIX$eventId",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<ReminderWorker>().setInputData(input).build()
                )
            }
            ExactAlarmKind.MILESTONE_REMINDER -> {
                val eventId = payload.getInt(MilestoneReminderWorker.KEY_EVENT_ID, 0)
                ExactReminderPreferenceMirror.updateMilestoneAlarmEventId(context, eventId, armed = false)
                val input = Data.Builder()
                    .putAll(payload)
                    .putBoolean(MilestoneReminderWorker.KEY_ALREADY_DELIVERED, delivered)
                    .build()
                wm.enqueueUniqueWork(
                    "$MILESTONE_EXACT_FOLLOW_UP_WORK_PREFIX$eventId",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<MilestoneReminderWorker>().setInputData(input).build()
                )
            }
        }
    }

    private fun android.os.Bundle.toWorkData(): Data {
        val builder = Data.Builder()
        keySet().forEach { key ->
            @Suppress("DEPRECATION")
            when (val value = get(key)) {
                is String -> builder.putString(key, value)
                is Int -> builder.putInt(key, value)
                is Long -> builder.putLong(key, value)
                is Boolean -> builder.putBoolean(key, value)
            }
        }
        return builder.build()
    }

    private companion object {
        const val TAG = "ExactReminderReceiver"
        const val DELIVERY_TIMEOUT_MILLIS = 8_000L
        val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
