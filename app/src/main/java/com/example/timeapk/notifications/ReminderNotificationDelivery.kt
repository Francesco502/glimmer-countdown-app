package com.example.timeapk.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.timeapk.MainActivity
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.Event
import com.example.timeapk.data.sanitizedReminderConfig
import com.example.timeapk.permissions.canPostAppNotifications
import com.example.timeapk.permissions.canPostEventReminderNotifications
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Notification delivery shared by the WorkManager workers and the exact-alarm receiver.
 * Always re-reads the event by id so renamed or rescheduled events show current data.
 */
internal object ReminderNotificationDelivery {
    private const val TAG = "ReminderDelivery"
    const val EVENT_CHANNEL_ID = "countdown_reminder"
    const val MILESTONE_CHANNEL_ID = "milestone_reminder"
    const val GROUP_KEY = "com.example.timeapk.REMINDERS"
    const val EVENT_NOTIFICATION_ID_BASE = 1000
    const val MILESTONE_NOTIFICATION_ID_BASE = 100000
    const val GROUP_SUMMARY_NOTIFICATION_ID = 999
    const val EXTRA_OPEN_EVENT_ID = "open_event_id"
    private const val SEAL_COLOR = 0xFFAF4E31.toInt()

    fun templates(context: Context): ReminderNotificationTemplates {
        val res = context.resources
        return ReminderNotificationTemplates(
            today = res.getString(R.string.notification_text_today),
            joinFormat = res.getString(R.string.notification_text_join_format),
            datePattern = res.getString(R.string.notification_date_pattern),
            locale = res.configuration.locales[0],
            daysLeft = { days -> res.getQuantityString(R.plurals.notification_days_left, days, days) },
            milestoneElapsed = { days ->
                val quantity = days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                res.getQuantityString(R.plurals.notification_milestone_elapsed, quantity, days)
            },
            milestoneMark = { days ->
                val quantity = days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                res.getQuantityString(R.plurals.notification_milestone_mark, quantity, days)
            }
        )
    }

    /** Returns true when a notification was posted. */
    suspend fun deliverEventReminder(
        context: Context,
        eventId: Int,
        fallbackTitle: String,
        workDaysLeft: Int,
        triggerAtMillis: Long
    ): Boolean {
        if (!context.canPostEventReminderNotifications()) return false
        val decision = decide(context, eventId, fallbackTitle) { it.remindEnabled }
        if (decision !is ReminderDeliveryDecision.Deliver) return false

        val templates = templates(context)
        val referenceDate = referenceDate(triggerAtMillis)
        val (daysLeft, occurrenceDate) = decision.event?.let { event ->
            val occurrence = computeOccurrenceOnOrAfter(event.sanitizedReminderConfig(), referenceDate)
            ChronoUnit.DAYS.between(referenceDate, occurrence).toInt() to occurrence
        } ?: (workDaysLeft to referenceDate.plusDays(workDaysLeft.toLong()))
        val text = ReminderNotificationTextPolicy.countdownText(templates, daysLeft, occurrenceDate)
        post(
            context = context,
            channelId = EVENT_CHANNEL_ID,
            channelName = context.getString(R.string.notification_channel_name),
            notificationId = EVENT_NOTIFICATION_ID_BASE + eventId,
            eventId = eventId,
            content = ReminderNotificationTextPolicy.content(decision.title, text, decision.note)
        )
        return true
    }

    suspend fun deliverMilestoneReminder(
        context: Context,
        eventId: Int,
        fallbackTitle: String,
        milestoneLabel: String,
        milestoneDays: Long?,
        milestoneYears: Int?,
        daysLeft: Int
    ): Boolean {
        if (!context.canPostAppNotifications()) return false
        val milestoneEnabled = (context.applicationContext as? TimeApplication)
            ?.userPrefs?.milestoneRemindEnabledFlow?.first() ?: true
        if (!milestoneEnabled) return false
        val decision = decide(context, eventId, fallbackTitle) { true }
        if (decision !is ReminderDeliveryDecision.Deliver) return false

        val text = ReminderNotificationTextPolicy.milestoneText(
            templates = templates(context),
            daysLeft = daysLeft,
            milestoneDays = milestoneDays,
            milestoneYears = milestoneYears,
            label = milestoneLabel
        )
        post(
            context = context,
            channelId = MILESTONE_CHANNEL_ID,
            channelName = context.getString(R.string.milestone_notification_channel_name),
            notificationId = MILESTONE_NOTIFICATION_ID_BASE + eventId,
            eventId = eventId,
            content = ReminderNotificationTextPolicy.content(decision.title, text, decision.note)
        )
        return true
    }

    private suspend fun decide(
        context: Context,
        eventId: Int,
        fallbackTitle: String,
        stillEnabled: (Event) -> Boolean
    ): ReminderDeliveryDecision {
        val app = context.applicationContext as? TimeApplication
        var lookupFailed = app == null
        val latest = try {
            app?.repository?.getEvent(eventId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to re-read eventId=$eventId; using scheduled title", t)
            lookupFailed = true
            null
        }
        return ReminderNotificationTextPolicy.decide(latest, lookupFailed, fallbackTitle, stillEnabled)
    }

    private fun referenceDate(triggerAtMillis: Long): LocalDate {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        // Prefer the scheduled date so a run delayed past midnight still describes the intended day.
        val millis = if (triggerAtMillis in 1..now) triggerAtMillis else now
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    }

    private fun post(
        context: Context,
        channelId: String,
        channelName: String,
        notificationId: Int,
        eventId: Int,
        content: ReminderNotificationContent
    ) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(nm, channelId, channelName)
        val openDetail = openEventPendingIntent(context, eventId, notificationId)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setColor(SEAL_COLOR)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setGroup(GROUP_KEY)
            .setContentIntent(openDetail)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.notification_action_view), openDetail)
        content.bigText?.let { builder.setStyle(NotificationCompat.BigTextStyle().bigText(it)) }
        nm.notify(notificationId, builder.build())
        postGroupSummaryIfNeeded(context, nm, channelId)
    }

    private fun postGroupSummaryIfNeeded(context: Context, nm: NotificationManager, channelId: String) {
        val activeChildren = try {
            nm.activeNotifications.count { status ->
                status.id != GROUP_SUMMARY_NOTIFICATION_ID && status.notification.group == GROUP_KEY
            }
        } catch (t: Throwable) {
            0
        }
        if (!ReminderNotificationTextPolicy.shouldPostGroupSummary(activeChildren)) return
        val openApp = PendingIntent.getActivity(
            context,
            GROUP_SUMMARY_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val summaryText = context.resources.getQuantityString(
            R.plurals.notification_group_summary,
            activeChildren,
            activeChildren
        )
        val summary = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(summaryText)
            .setContentText(summaryText)
            .setColor(SEAL_COLOR)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        nm.notify(GROUP_SUMMARY_NOTIFICATION_ID, summary)
    }

    private fun openEventPendingIntent(context: Context, eventId: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_OPEN_EVENT_ID, eventId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel(nm: NotificationManager, channelId: String, channelName: String) {
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }
}
