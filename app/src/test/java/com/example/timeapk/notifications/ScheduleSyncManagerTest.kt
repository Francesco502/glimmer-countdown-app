package com.example.timeapk.notifications

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
}
