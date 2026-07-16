package com.example.timeapk.ui.event

import com.example.timeapk.R
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.DefaultEventReminderSettings
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_DAILY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.notifications.CalendarCleanupResult
import com.example.timeapk.ui.home.calendarCleanupRequired
import com.example.timeapk.ui.home.eventAfterCleanupAttempt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class EventEntryValidationTest {

    @Test
    fun failedCleanup_keepsCalendarIdsAndRecordsRetryableError() {
        val event = testCalendarEvent(
            syncToScheduleEnabled = false,
            scheduleEventId = 88,
            targetCalendarId = 9
        )

        val actual = eventAfterCleanupAttempt(
            event = event,
            result = CalendarCleanupResult.PermissionRequired,
            nowMillis = 123
        )

        assertEquals(88L, actual.scheduleEventId)
        assertEquals(9L, actual.targetCalendarId)
        assertEquals("Calendar permission required", actual.lastScheduleSyncError)
        assertEquals(123L, actual.lastScheduleSyncAt)
    }

    @Test
    fun successfulCleanup_clearsCalendarIdsAndError() {
        val event = testCalendarEvent(
            syncToScheduleEnabled = true,
            scheduleEventId = 88,
            targetCalendarId = 9
        ).copy(lastScheduleSyncError = "old error")

        val actual = eventAfterCleanupAttempt(
            event = event,
            result = CalendarCleanupResult.RemovedOrNotPresent,
            nowMillis = 456
        )

        assertNull(actual.scheduleEventId)
        assertNull(actual.targetCalendarId)
        assertNull(actual.lastScheduleSyncError)
        assertEquals(456L, actual.lastScheduleSyncAt)
    }

    @Test
    fun cleanupIsRequiredOnlyWhenTheEventCouldOwnCalendarData() {
        assertFalse(calendarCleanupRequired(testCalendarEvent(syncToScheduleEnabled = false)))
        assertTrue(calendarCleanupRequired(testCalendarEvent(syncToScheduleEnabled = true)))
        assertTrue(
            calendarCleanupRequired(
                testCalendarEvent(syncToScheduleEnabled = false, scheduleEventId = 88)
            )
        )
    }

    @Test
    fun targetCalendarIdAlone_requiresCleanup() {
        assertTrue(
            calendarCleanupRequired(
                testCalendarEvent(syncToScheduleEnabled = false, targetCalendarId = 9)
            )
        )
    }

    @Test
    fun isEventDateValid_accepts1900BoundaryDate() {
        val millis = LocalDate.of(1900, 1, 1)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        assertTrue(isEventDateValid(millis))
    }

    @Test
    fun isEventDateValid_rejectsDatesBefore1900() {
        val millis = LocalDate.of(1899, 12, 31)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        assertFalse(isEventDateValid(millis))
    }

    @Test
    fun sanitizeRepeatTypeForLunar_restrictsUnsupportedRepeatTypes() {
        assertEquals(REPEAT_NONE, sanitizeRepeatTypeForLunar(isLunar = true, repeatType = REPEAT_MONTHLY))
        assertEquals(REPEAT_NONE, sanitizeRepeatTypeForLunar(isLunar = true, repeatType = REPEAT_DAILY))
        assertEquals(REPEAT_YEARLY, sanitizeRepeatTypeForLunar(isLunar = true, repeatType = REPEAT_YEARLY))
        assertEquals(REPEAT_NONE, sanitizeRepeatTypeForLunar(isLunar = true, repeatType = REPEAT_NONE))
    }

    @Test
    fun supportedRepeatTypes_forLunarOnlyIncludesNoneAndYearly() {
        assertEquals(listOf(REPEAT_NONE, REPEAT_YEARLY), supportedRepeatTypes(isLunar = true))
    }

    @Test
    fun toEvent_normalizesUnsupportedLunarRepeatType() {
        val millis = LocalDate.of(2026, 3, 17)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        val event = EventDetails(
            title = "lunar event",
            date = millis,
            isLunar = true,
            repeatType = REPEAT_MONTHLY
        ).toEvent()

        assertEquals(REPEAT_NONE, event.repeatType)
    }

    @Test
    fun buildNewEventDetails_usesProvidedReminderDefaults() {
        val millis = LocalDate.of(2026, 4, 16)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        val details = buildNewEventDetails(
            defaultReminderSettings = DefaultEventReminderSettings(
                enabled = true,
                daysBefore = 7,
                timeMinutesOfDay = 10 * 60
            ),
            initialCategory = CATEGORY_ANNIVERSARY,
            nowMillis = millis
        )

        assertEquals(CATEGORY_ANNIVERSARY, details.category)
        assertTrue(details.remindEnabled)
        assertEquals(7, details.remindDaysBefore)
        assertEquals(10 * 60, details.reminderTimeMinutesOfDay)
        assertEquals(millis, details.date)
        assertEquals(millis, details.createdAt)
    }

    @Test
    fun buildNewEventDetails_fallsBackToOtherCategoryAndSanitizesReminderValues() {
        val details = buildNewEventDetails(
            defaultReminderSettings = DefaultEventReminderSettings(
                enabled = false,
                daysBefore = -3,
                timeMinutesOfDay = 24 * 60 + 5
            ),
            initialCategory = "custom"
        )

        assertEquals(CATEGORY_OTHER, details.category)
        assertFalse(details.remindEnabled)
        assertEquals(0, details.remindDaysBefore)
        assertEquals(24 * 60 - 1, details.reminderTimeMinutesOfDay)
    }

    @Test
    fun resolvePartialSaveMessageResId_returnsGenericMessageForScheduleSyncFailures() {
        assertEquals(
            R.string.save_event_partial_warning,
            resolvePartialSaveMessageResId(
                hasGenericFailure = false,
                scheduleSyncError = "Calendar sync failed"
            )
        )
    }

    @Test
    fun resolvePartialSaveMessageResId_returnsNoWritableCalendarMessage() {
        assertEquals(
            R.string.save_event_partial_warning_no_writable_calendar,
            resolvePartialSaveMessageResId(
                hasGenericFailure = false,
                scheduleSyncError = "No writable calendar"
            )
        )
    }

    @Test
    fun resolvePartialSaveMessageResId_prefersGenericMessageWhenOtherFailuresAlsoExist() {
        assertEquals(
            R.string.save_event_partial_warning,
            resolvePartialSaveMessageResId(
                hasGenericFailure = true,
                scheduleSyncError = "No writable calendar"
            )
        )
    }

    @Test
    fun resolvePartialSaveMessageResId_returnsNullWhenNoFailures() {
        assertEquals(
            null,
            resolvePartialSaveMessageResId(
                hasGenericFailure = false,
                scheduleSyncError = null
            )
        )
    }

    private fun testCalendarEvent(
        syncToScheduleEnabled: Boolean,
        scheduleEventId: Long? = null,
        targetCalendarId: Long? = null
    ) = Event(
        title = "Trip",
        date = 1,
        category = CATEGORY_OTHER,
        syncToScheduleEnabled = syncToScheduleEnabled,
        scheduleEventId = scheduleEventId,
        targetCalendarId = targetCalendarId
    )
}
