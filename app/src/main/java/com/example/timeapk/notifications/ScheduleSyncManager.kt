package com.example.timeapk.notifications

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.example.timeapk.data.Event
import com.example.timeapk.data.sanitizedReminderConfig

object ScheduleSyncManager {

    private const val MILESTONE_MARKER_PREFIX = "[TimeAPK][Milestone]"

    private fun milestoneMarker(eventId: Int): String = "$MILESTONE_MARKER_PREFIX:$eventId"

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

    fun insertScheduleReminder(context: Context, event: Event): Long? {
        return try {
            val sanitizedEvent = event.sanitizedReminderConfig()
            val calendarId = getDefaultCalendarId(context) ?: return null
            val startMillis = computeNextReminderTriggerAtMillis(sanitizedEvent) ?: return null
            val title = if (sanitizedEvent.remindDaysBefore == 0) {
                sanitizedEvent.title
            } else {
                "${sanitizedEvent.title} (-${sanitizedEvent.remindDaysBefore}d)"
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, startMillis + 60 * 60 * 1000L)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, sanitizedEvent.note.takeIf { it.isNotBlank() }.orEmpty())
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return null
            val eventId = ContentUris.parseId(uri)
            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                put(CalendarContract.Reminders.MINUTES, 0)
            }
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            eventId
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
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
            val marker = milestoneMarker(eventId)
            val mergedDescription = listOf(marker, description.takeIf { it.isNotBlank() })
                .joinToString("\n")

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, triggerAtMillis)
                put(CalendarContract.Events.DTEND, triggerAtMillis + 60 * 60 * 1000L)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, mergedDescription)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return null
            val newId = ContentUris.parseId(uri)
            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, newId)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                put(CalendarContract.Reminders.MINUTES, 0)
            }
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            newId
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    fun clearAllMilestoneScheduleReminders(context: Context) {
        removeEventsByDescriptionLike(context, "$MILESTONE_MARKER_PREFIX%")
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

    fun removeScheduleReminder(context: Context, calendarEventId: Long?) {
        if (calendarEventId == null) return
        try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventId)
            context.contentResolver.delete(uri, null, null)
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }
}
