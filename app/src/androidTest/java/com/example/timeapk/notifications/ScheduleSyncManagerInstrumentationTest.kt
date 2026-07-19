package com.example.timeapk.notifications

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.provider.CalendarContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_NONE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class ScheduleSyncManagerInstrumentationTest {

    @Test
    fun writableCalendar_createUpdateDisableAndCleanup_preservesProviderOwnership() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val accountName = "timeapk_connected_${System.currentTimeMillis()}"
        val calendarId: Long

        instrumentation.uiAutomation.adoptShellPermissionIdentity(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
        try {
            calendarId = insertTemporaryCalendar(accountName)
            assertTrue(
                ScheduleSyncManager.getWritableCalendars(context).any {
                    it.id == calendarId && it.isMarkedWritable
                }
            )

            val source = Event(
                id = 2_000_000_001,
                title = "Calendar provider create",
                date = LocalDate.now()
                    .plusDays(3)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli(),
                category = CATEGORY_OTHER,
                repeatType = REPEAT_NONE,
                remindEnabled = true,
                remindDaysBefore = 0,
                reminderTimeMinutesOfDay = 10 * 60,
                syncToScheduleEnabled = true
            )

            val created = ScheduleSyncManager.syncReminderSeries(
                context = context,
                event = source,
                preferredCalendarId = calendarId,
                useRRuleSync = false
            )
            val createdId = assertNotNull(created.primaryScheduleEventId).let {
                created.primaryScheduleEventId!!
            }
            assertEquals(calendarId, created.targetCalendarId)
            assertNull(created.error)
            val createdProviderEvent = requireNotNull(queryEvent(createdId))
            assertEquals(calendarId, createdProviderEvent.calendarId)
            assertTrue(createdProviderEvent.title.contains("Calendar provider create"))
            assertTrue(hasAlertReminder(createdId))

            val updated = ScheduleSyncManager.syncReminderSeries(
                context = context,
                event = source.copy(
                    title = "Calendar provider updated",
                    scheduleEventId = createdId,
                    targetCalendarId = calendarId
                ),
                preferredCalendarId = calendarId,
                useRRuleSync = false
            )
            assertEquals(createdId, updated.primaryScheduleEventId)
            assertEquals(calendarId, updated.targetCalendarId)
            assertNull(updated.error)
            val updatedProviderEvent = requireNotNull(queryEvent(createdId))
            assertEquals(calendarId, updatedProviderEvent.calendarId)
            assertTrue(updatedProviderEvent.title.contains("Calendar provider updated"))
            assertEquals(1, countManagedEvents(source.id))

            val disabled = ScheduleSyncManager.syncReminderSeries(
                context = context,
                event = source.copy(
                    syncToScheduleEnabled = false,
                    scheduleEventId = createdId,
                    targetCalendarId = calendarId
                ),
                preferredCalendarId = calendarId,
                useRRuleSync = false
            )
            assertNull(disabled.primaryScheduleEventId)
            assertNull(disabled.targetCalendarId)
            assertNull(disabled.error)
            assertNull(queryEvent(createdId))
            assertEquals(0, countManagedEvents(source.id))

            val recreated = ScheduleSyncManager.syncReminderSeries(
                context = context,
                event = source,
                preferredCalendarId = calendarId,
                useRRuleSync = false
            )
            val recreatedId = recreated.primaryScheduleEventId
            assertNotNull(recreatedId)
            assertEquals(
                CalendarCleanupResult.RemovedOrNotPresent,
                ScheduleSyncManager.removeManagedCalendarEntries(
                    context = context,
                    eventId = source.id,
                    calendarEventId = recreatedId
                )
            )
            assertNull(queryEvent(recreatedId!!))
            assertEquals(0, countManagedEvents(source.id))
        } finally {
            try {
                deleteTemporaryCalendar(accountName)
            } finally {
                instrumentation.uiAutomation.dropShellPermissionIdentity()
            }
        }
    }

    private fun insertTemporaryCalendar(accountName: String): Long {
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, accountName)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "TimeAPK connected QA")
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xff3f6654.toInt())
            put(
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                CalendarContract.Calendars.CAL_ACCESS_OWNER
            )
            put(CalendarContract.Calendars.OWNER_ACCOUNT, accountName)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, ZoneId.systemDefault().id)
        }
        val uri = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .contentResolver
            .insert(syncAdapterUri(CalendarContract.Calendars.CONTENT_URI, accountName), values)
        return requireNotNull(uri) { "CalendarProvider rejected the temporary calendar" }
            .lastPathSegment
            ?.toLongOrNull()
            ?: error("CalendarProvider returned an invalid calendar URI: $uri")
    }

    private fun deleteTemporaryCalendar(accountName: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.contentResolver.delete(
            syncAdapterUri(CalendarContract.Calendars.CONTENT_URI, accountName),
            "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND " +
                "${CalendarContract.Calendars.ACCOUNT_TYPE} = ?",
            arrayOf(accountName, CalendarContract.ACCOUNT_TYPE_LOCAL)
        )
    }

    private fun syncAdapterUri(uri: Uri, accountName: String): Uri = uri.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
        .appendQueryParameter(
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.ACCOUNT_TYPE_LOCAL
        )
        .build()

    private fun queryEvent(eventId: Long): ProviderEvent? {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return context.contentResolver.query(
            uri,
            arrayOf(
                CalendarContract.Events.CALENDAR_ID,
                CalendarContract.Events.TITLE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            ProviderEvent(
                calendarId = cursor.getLong(0),
                title = cursor.getString(1)
            )
        }
    }

    private fun hasAlertReminder(eventId: Long): Boolean {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return context.contentResolver.query(
            CalendarContract.Reminders.CONTENT_URI,
            arrayOf(CalendarContract.Reminders._ID),
            "${CalendarContract.Reminders.EVENT_ID} = ? AND " +
                "${CalendarContract.Reminders.METHOD} = ?",
            arrayOf(
                eventId.toString(),
                CalendarContract.Reminders.METHOD_ALERT.toString()
            ),
            null
        )?.use { it.moveToFirst() } == true
    }

    private fun countManagedEvents(eventId: Int): Int {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID),
            "${CalendarContract.Events.DESCRIPTION} LIKE ?",
            arrayOf("[TimeAPK][Reminder]:$eventId%"),
            null
        )?.use { it.count } ?: 0
    }

    private data class ProviderEvent(
        val calendarId: Long,
        val title: String
    )
}
