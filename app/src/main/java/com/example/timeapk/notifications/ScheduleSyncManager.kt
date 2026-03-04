package com.example.timeapk.notifications

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.getNextLunarOccurrence
import java.time.ZoneId

/**
 * 将 APP 内设置的「提前 N 天 + 几点提醒」同步到系统日程（日历）。
 * 系统会在该时刻通过通知栏提醒，用户也可在日历/日程应用中看到。
 */
object ScheduleSyncManager {

    /**
     * 获取可写的默认日历 ID；无权限或失败时返回 null。
     */
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
                    val access = if (accessIndex >= 0) cursor.getInt(accessIndex) else CalendarContract.Calendars.CAL_ACCESS_EDITOR
                    if (access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR ||
                        access == CalendarContract.Calendars.CAL_ACCESS_EDITOR ||
                        access == CalendarContract.Calendars.CAL_ACCESS_OWNER) {
                        return id
                    }
                } while (cursor.moveToNext())
                cursor.moveToFirst()
                return cursor.getLong(idIndex)
            }
        }
        return null
    }

    /**
     * 在系统日程中插入一条提醒：在「目标日 − N 天」的「设定时刻」触发，标题为「距离「XXX」还有 N 天」。
     * 与 [ReminderScheduler] 使用相同的目标日与下次发生日逻辑（含农历）。
     * @return 系统日历事件 ID，失败或无权限时返回 null
     */
    fun insertScheduleReminder(context: Context, event: Event): Long? {
        return try {
            val calendarId = getDefaultCalendarId(context) ?: return null
            val eventLocalDate = eventDateToLocalDate(event.date)
            val today = java.time.LocalDate.now()
            val baseDate = if (event.isLunar && event.repeatType == REPEAT_YEARLY) {
                getNextLunarOccurrence(eventLocalDate, today)
            } else {
                eventLocalDate
            }
            val remindDate = baseDate.minusDays(event.remindDaysBefore.toLong())
            val hour = event.reminderTimeMinutesOfDay / 60
            val minute = event.reminderTimeMinutesOfDay % 60
            val startZdt = remindDate.atTime(hour, minute).atZone(ZoneId.systemDefault())
            val startMillis = startZdt.toInstant().toEpochMilli()
            if (startMillis <= System.currentTimeMillis()) {
                return null
            }
            val title = if (event.remindDaysBefore == 0) {
                context.getString(com.example.timeapk.R.string.schedule_reminder_title_today, event.title)
            } else {
                context.getString(
                    com.example.timeapk.R.string.schedule_reminder_title_format,
                    event.title,
                    event.remindDaysBefore
                )
            }
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, startMillis + 60 * 60 * 1000L)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, event.note.takeIf { it.isNotBlank() }.orEmpty())
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
                if (event.repeatType == REPEAT_YEARLY && !event.isLunar) {
                    put(CalendarContract.Events.RRULE, "FREQ=YEARLY")
                }
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

    /**
     * 从系统日程中删除此前写入的提醒。
     */
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
