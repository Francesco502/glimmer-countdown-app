package com.example.timeapk.notifications

import android.provider.CalendarContract
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleSyncManagerTest {

    @Test
    fun reminderMarker_containsStablePrefixAndEventId() {
        val marker = ScheduleSyncManager.buildReminderMarkerForTest(42)
        assertEquals("[TimeAPK][Reminder]:42", marker)
    }

    @Test
    fun reminderTitle_includesOffsetWhenDaysBeforeNotZero() {
        val event = Event(
            id = 1,
            title = "Event A",
            date = 0L,
            category = CATEGORY_OTHER,
            remindDaysBefore = 3
        )
        val title = ScheduleSyncManager.buildReminderTitleForTest(event)
        assertEquals("Event A (-3d)", title)
    }

    @Test
    fun markedDescription_keepsMarkerAtBeginningAndAppendsNote() {
        val marker = ScheduleSyncManager.buildReminderMarkerForTest(7)
        val desc = ScheduleSyncManager.buildMarkedDescriptionForTest(marker, "sample note")
        assertTrue(desc.startsWith(marker))
        assertTrue(desc.contains("sample note"))
    }

    @Test
    fun isManagedReminderMetadataKind_excludesMilestoneEntries() {
        assertFalse(isManagedReminderMetadataKind("milestone_v2"))
    }

    @Test
    fun isManagedReminderMetadataKind_acceptsReminderAndLegacyEntries() {
        assertTrue(isManagedReminderMetadataKind(null))
        assertTrue(isManagedReminderMetadataKind("reminder_v2"))
        assertTrue(isManagedReminderMetadataKind("reminder_rrule_v2"))
    }

    @Test
    fun isNoWritableCalendarError_matchesDedicatedSyncErrorOnly() {
        assertTrue(
            ScheduleSyncManager.isNoWritableCalendarError(
                ScheduleSyncManager.ERROR_NO_WRITABLE_CALENDAR
            )
        )
        assertFalse(ScheduleSyncManager.isNoWritableCalendarError("Calendar permission denied"))
    }

    @Test
    fun isCalendarAccessLevelMarkedWritable_acceptsContributorAndAbove() {
        assertTrue(
            ScheduleSyncManager.isCalendarAccessLevelMarkedWritable(
                CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
            )
        )
        assertTrue(
            ScheduleSyncManager.isCalendarAccessLevelMarkedWritable(
                CalendarContract.Calendars.CAL_ACCESS_OWNER
            )
        )
        assertFalse(
            ScheduleSyncManager.isCalendarAccessLevelMarkedWritable(
                CalendarContract.Calendars.CAL_ACCESS_READ
            )
        )
    }

    @Test
    fun selectCalendarsForSync_prefersCalendarsMarkedWritableByProvider() {
        val calendars = listOf(
            ScheduleSyncManager.CalendarOption(
                id = 1L,
                displayName = "Read only",
                accountName = "ro@example.com",
                accessLevel = CalendarContract.Calendars.CAL_ACCESS_READ,
                isMarkedWritable = false
            ),
            ScheduleSyncManager.CalendarOption(
                id = 2L,
                displayName = "Writable",
                accountName = "rw@example.com",
                accessLevel = CalendarContract.Calendars.CAL_ACCESS_OWNER,
                isMarkedWritable = true
            )
        )

        val selected = ScheduleSyncManager.selectCalendarsForSync(calendars)

        assertEquals(listOf(calendars[1]), selected)
    }

    @Test
    fun selectCalendarsForSync_excludesExplicitlyReadOnlyCalendars() {
        val calendars = listOf(
            ScheduleSyncManager.CalendarOption(
                id = 9L,
                displayName = "Holidays",
                accountName = null,
                accessLevel = CalendarContract.Calendars.CAL_ACCESS_READ,
                isMarkedWritable = false
            )
        )

        val selected = ScheduleSyncManager.selectCalendarsForSync(calendars)

        assertEquals(emptyList<ScheduleSyncManager.CalendarOption>(), selected)
    }

    @Test
    fun selectCalendarsForSync_fallsBackToUnknownAccessLevelCalendars() {
        val calendars = listOf(
            ScheduleSyncManager.CalendarOption(
                id = 10L,
                displayName = "Phone calendar",
                accountName = null,
                accessLevel = null,
                isMarkedWritable = false
            )
        )

        val selected = ScheduleSyncManager.selectCalendarsForSync(calendars)

        assertEquals(calendars, selected)
    }
}
