package com.example.timeapk.notifications

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.example.timeapk.data.Event
import com.example.timeapk.data.sanitizedReminderConfig

object ScheduleSyncManager {

    private const val MILESTONE_MARKER_PREFIX = "[TimeAPK][Milestone]"
    private const val REMINDER_MARKER_PREFIX = "[TimeAPK][Reminder]"

    private fun milestoneMarker(eventId: Int): String = "$MILESTONE_MARKER_PREFIX:$eventId"
    private fun reminderMarker(eventId: Int): String = "$REMINDER_MARKER_PREFIX:$eventId"

    fun getDefaultCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val uri = CalendarContract.Calendars.CONTENT_URI
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val accessIndex = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
                do {
                    val id = cursor.getLong(idIndex)
                    val access = if (accessIndex >= 0) {
                        cursor.getInt(accessIndex)
                    } else {
                        CalendarContract.Calendars.CAL_ACCESS_EDITOR
                    }
                    if (
                        access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR ||
                        access == CalendarContract.Calendars.CAL_ACCESS_EDITOR ||
                        access == CalendarContract.Calendars.CAL_ACCESS_OWNER
                    ) {
                        return id
                    }
                } while (cursor.moveToNext())
                cursor.moveToFirst()
                return cursor.getLong(idIndex)
            }
        }
        return null
    }

    fun upsertScheduleReminder(
        context: Context,
        event: Event,
        currentScheduleEventId: Long? = event.scheduleEventId
    ): Long? {
        return try {
            val sanitizedEvent = event.sanitizedReminderConfig()
            if (!sanitizedEvent.syncToScheduleEnabled) {
                removeScheduleReminder(context, currentScheduleEventId)
                removeScheduleReminderByEventId(context, sanitizedEvent.id)
                return null
            }

            val calendarId = getDefaultCalendarId(context) ?: return null
            val startMillis = computeNextReminderTriggerAtMillis(sanitizedEvent) ?: run {
                removeScheduleReminder(context, currentScheduleEventId)
                removeScheduleReminderByEventId(context, sanitizedEvent.id)
                return null
            }
            val marker = reminderMarker(sanitizedEvent.id)

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, startMillis + 60 * 60 * 1000L)
                put(CalendarContract.Events.TITLE, buildReminderTitle(sanitizedEvent))
                put(CalendarContract.Events.DESCRIPTION, buildMarkedDescription(marker, sanitizedEvent.note))
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            }

            val existingByMarker = findFirstEventIdByDescriptionPrefix(context, marker)
            val targetEventId = when {
                currentScheduleEventId != null && updateEventAndReminder(context, currentScheduleEventId, values) -> {
                    currentScheduleEventId
                }
                existingByMarker != null && updateEventAndReminder(context, existingByMarker, values) -> {
                    existingByMarker
                }
                else -> {
                    insertEventAndReminder(context, values)
                }
            }

            if (targetEventId != null && currentScheduleEventId != null && currentScheduleEventId != targetEventId) {
                removeScheduleReminder(context, currentScheduleEventId)
            }

            targetEventId
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    fun insertScheduleReminder(context: Context, event: Event): Long? {
        return upsertScheduleReminder(context, event, event.scheduleEventId)
    }

    fun insertMilestoneScheduleReminder(
        context: Context,
        eventId: Int,
        title: String,
        description: String,
        triggerAtMillis: Long
    ): Long? {
        return try {
            val calendarId = getDefaultCalendarId(context) ?: return null
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, triggerAtMillis)
                put(CalendarContract.Events.DTEND, triggerAtMillis + 60 * 60 * 1000L)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, buildMarkedDescription(milestoneMarker(eventId), description))
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            }
            insertEventAndReminder(context, values)
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    fun clearAllMilestoneScheduleReminders(context: Context) {
        removeEventsByDescriptionLike(context, "$MILESTONE_MARKER_PREFIX%")
    }

    fun removeScheduleReminderByEventId(context: Context, eventId: Int) {
        val marker = reminderMarker(eventId)
        removeEventsByDescriptionLike(context, "$marker%")
    }

    fun removeScheduleReminder(context: Context, calendarEventId: Long?) {
        if (calendarEventId == null) return
        try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventId)
            context.contentResolver.delete(uri, null, null)
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }

    private fun buildReminderTitle(event: Event): String {
        return if (event.remindDaysBefore == 0) {
            event.title
        } else {
            "${event.title} (-${event.remindDaysBefore}d)"
        }
    }

    private fun buildMarkedDescription(marker: String, note: String): String {
        return listOf(marker, note.takeIf { it.isNotBlank() }).joinToString("\n")
    }

    internal fun buildReminderMarkerForTest(eventId: Int): String = reminderMarker(eventId)

    internal fun buildReminderTitleForTest(event: Event): String = buildReminderTitle(event)

    internal fun buildMarkedDescriptionForTest(marker: String, note: String): String =
        buildMarkedDescription(marker, note)

    private fun findFirstEventIdByDescriptionPrefix(context: Context, markerPrefix: String): Long? {
        val projection = arrayOf(CalendarContract.Events._ID)
        val selection = "${CalendarContract.Events.DESCRIPTION} LIKE ?"
        val args = arrayOf("$markerPrefix%")

        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            args,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(CalendarContract.Events._ID)
                return cursor.getLong(idIndex)
            }
        }
        return null
    }

    private fun insertEventAndReminder(context: Context, values: ContentValues): Long? {
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return null
        val eventId = ContentUris.parseId(uri)
        upsertReminderAlert(context, eventId)
        return eventId
    }

    private fun updateEventAndReminder(context: Context, eventId: Long, values: ContentValues): Boolean {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val affected = context.contentResolver.update(uri, values, null, null)
        if (affected <= 0) return false
        upsertReminderAlert(context, eventId)
        return true
    }

    private fun upsertReminderAlert(context: Context, eventId: Long) {
        val selection = "${CalendarContract.Reminders.EVENT_ID} = ?"
        val args = arrayOf(eventId.toString())
        context.contentResolver.delete(CalendarContract.Reminders.CONTENT_URI, selection, args)

        val reminderValues = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            put(CalendarContract.Reminders.MINUTES, 0)
        }
        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
    }

    private fun removeEventsByDescriptionLike(context: Context, pattern: String) {
        try {
            val projection = arrayOf(CalendarContract.Events._ID)
            val selection = "${CalendarContract.Events.DESCRIPTION} LIKE ?"
            val args = arrayOf(pattern)
            val ids = mutableListOf<Long>()

            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                args,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(CalendarContract.Events._ID)
                while (cursor.moveToNext()) {
                    ids += cursor.getLong(idIndex)
                }
            }

            ids.forEach { id ->
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)
                context.contentResolver.delete(uri, null, null)
            }
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }
}
