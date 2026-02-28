package com.example.timeapk.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.timeapk.MainActivity
import com.example.timeapk.R

/**
 * 节点临近提醒 Worker：在距离重大时间节点 N 天时发送通知。
 */
class MilestoneReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val eventId = inputData.getInt(KEY_EVENT_ID, 0)
        val milestoneLabel = inputData.getString(KEY_MILESTONE_LABEL) ?: return Result.failure()
        val daysLeft = inputData.getInt(KEY_DAYS_LEFT, 0)
        val channelId = "countdown_reminder"
        ensureChannel(channelId)
        val content = applicationContext.getString(R.string.milestone_notification_content, milestoneLabel, daysLeft)
        val fullTitle = applicationContext.getString(R.string.milestone_notification_title_prefix)
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            applicationContext,
            MILESTONE_NOTIFICATION_ID_BASE + eventId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(fullTitle)
            .setContentText("$title：$content")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(MILESTONE_NOTIFICATION_ID_BASE + eventId, notification)
        return Result.success()
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

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_EVENT_ID = "event_id"
        const val KEY_MILESTONE_LABEL = "milestone_label"
        const val KEY_DAYS_LEFT = "days_left"
        private const val MILESTONE_NOTIFICATION_ID_BASE = 2000
    }
}
