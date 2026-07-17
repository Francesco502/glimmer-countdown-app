package com.example.timeapk.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.timeapk.MainActivity
import com.example.timeapk.permissions.canPostAppNotifications
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.ui.home.eventAfterCleanupAttempt
import kotlinx.coroutines.flow.first

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val eventId = inputData.getInt(KEY_EVENT_ID, 0)
        val daysLeft = inputData.getInt(KEY_DAYS_LEFT, 0)
        val channelId = "countdown_reminder"

        if (canPostNotifications()) {
            ensureChannel(channelId)
            val content = if (daysLeft == 0) {
                applicationContext.getString(R.string.notification_today)
            } else {
                applicationContext.resources.getQuantityString(
                    R.plurals.notification_days_left,
                    daysLeft,
                    daysLeft
                )
            }
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("open_event_id", eventId)
            }
            val pending = PendingIntent.getActivity(
                applicationContext,
                eventId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setContentTitle(applicationContext.getString(R.string.notification_title_prefix))
                .setContentText("$title, $content")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID_BASE + eventId, notification)
        }

        rescheduleNextReminder(eventId)
        return Result.success()
    }

    private fun canPostNotifications(): Boolean {
        return applicationContext.canPostAppNotifications()
    }

    private fun ensureChannel(channelId: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    applicationContext.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }

    private suspend fun rescheduleNextReminder(eventId: Int) {
        val app = applicationContext as? TimeApplication ?: return
        val repository = app.repository
        val event = repository.getEvent(eventId) ?: return
        val preferredCalendarId = app.userPrefs.scheduleTargetCalendarIdFlow.first()
        val useRRuleSync = app.userPrefs.scheduleUseRRuleSyncFlow.first()

        if (event.remindEnabled) {
            scheduleReminder(applicationContext, event)
        } else {
            cancelReminder(applicationContext, event.id)
        }

        val updatedEvent = if (event.syncToScheduleEnabled) {
            val syncResult = ScheduleSyncManager.syncReminderSeries(
                context = applicationContext,
                event = event,
                preferredCalendarId = preferredCalendarId,
                useRRuleSync = useRRuleSync
            )
            eventAfterScheduleSyncAttempt(event, syncResult)
        } else {
            val cleanup = recordManagedCalendarCleanupForMilestoneOwnership(
                context = applicationContext,
                eventId = event.id,
                result = ScheduleSyncManager.removeManagedCalendarEntries(
                    context = applicationContext,
                    eventId = event.id,
                    calendarEventId = event.scheduleEventId
                )
            )
            eventAfterCleanupAttempt(
                event = event,
                result = cleanup,
                nowMillis = System.currentTimeMillis()
            )
        }

        if (updatedEvent != event) {
            repository.updateEvent(updatedEvent)
        }
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_EVENT_ID = "event_id"
        const val KEY_DAYS_LEFT = "days_left"
        private const val NOTIFICATION_ID_BASE = 1000
    }
}

