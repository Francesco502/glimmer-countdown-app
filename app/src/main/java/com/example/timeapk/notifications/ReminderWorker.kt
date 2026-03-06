package com.example.timeapk.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.timeapk.MainActivity
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.REPEAT_NONE

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
                applicationContext.getString(R.string.notification_days_left, daysLeft)
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel(channelId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
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

        if (!event.remindEnabled) {
            ScheduleSyncManager.removeScheduleReminder(applicationContext, event.scheduleEventId)
            ScheduleSyncManager.removeScheduleReminderByEventId(applicationContext, event.id)
            repository.updateEvent(event.copy(scheduleEventId = null))
            return
        }

        if (event.repeatType == REPEAT_NONE) {
            ScheduleSyncManager.removeScheduleReminder(applicationContext, event.scheduleEventId)
            ScheduleSyncManager.removeScheduleReminderByEventId(applicationContext, event.id)
            repository.updateEvent(event.copy(scheduleEventId = null))
            return
        }

        scheduleReminder(applicationContext, event)

        if (!event.syncToScheduleEnabled) {
            ScheduleSyncManager.removeScheduleReminder(applicationContext, event.scheduleEventId)
            ScheduleSyncManager.removeScheduleReminderByEventId(applicationContext, event.id)
            repository.updateEvent(event.copy(scheduleEventId = null))
            return
        }

        val newScheduleId = ScheduleSyncManager.upsertScheduleReminder(
            context = applicationContext,
            event = event,
            currentScheduleEventId = event.scheduleEventId
        )
        if (newScheduleId != event.scheduleEventId) {
            repository.updateEvent(event.copy(scheduleEventId = newScheduleId))
        }
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_EVENT_ID = "event_id"
        const val KEY_DAYS_LEFT = "days_left"
        private const val NOTIFICATION_ID_BASE = 1000
    }
}



