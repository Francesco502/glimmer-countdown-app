package com.example.timeapk.ui.event

import com.example.timeapk.R
import com.example.timeapk.data.REPEAT_DAILY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.notifications.ScheduleSyncManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class EventEntryValidationTest {

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
    fun resolvePartialSaveMessageResId_returnsSpecificMessageForNoWritableCalendarOnly() {
        assertEquals(
            R.string.save_event_partial_warning_no_writable_calendar,
            resolvePartialSaveMessageResId(
                hasGenericFailure = false,
                scheduleSyncError = ScheduleSyncManager.ERROR_NO_WRITABLE_CALENDAR
            )
        )
    }

    @Test
    fun resolvePartialSaveMessageResId_prefersGenericMessageWhenOtherFailuresAlsoExist() {
        assertEquals(
            R.string.save_event_partial_warning,
            resolvePartialSaveMessageResId(
                hasGenericFailure = true,
                scheduleSyncError = ScheduleSyncManager.ERROR_NO_WRITABLE_CALENDAR
            )
        )
    }
}
