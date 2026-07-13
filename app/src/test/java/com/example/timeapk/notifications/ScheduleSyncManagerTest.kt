package com.example.timeapk.notifications

import android.provider.CalendarContract
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_MONTHLY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

class ScheduleSyncManagerTest {

    @Test
    fun managedCalendarDescription_matchesLegacyAndV2MarkersForExactEventId() {
        assertTrue(
            ScheduleSyncManager.isManagedCalendarDescriptionForEvent(
                "[TimeAPK][Reminder]:1\nlegacy note",
                1
            )
        )
        assertTrue(
            ScheduleSyncManager.isManagedCalendarDescriptionForEvent(
                "[TimeAPK][Reminder]:1:v2:occ=42:days=3:rr=0\nnote",
                1
            )
        )
        assertTrue(
            ScheduleSyncManager.isManagedCalendarDescriptionForEvent(
                "[TimeAPK][Milestone]:1",
                1
            )
        )
        assertTrue(
            ScheduleSyncManager.isManagedCalendarDescriptionForEvent(
                "[TimeAPK][Milestone]:1:v2:trigger=42\nnote",
                1
            )
        )
    }

    @Test
    fun managedCalendarDescription_doesNotTreatEventIdAsNumericPrefix() {
        listOf("10", "11").forEach { otherId ->
            assertFalse(
                ScheduleSyncManager.isManagedCalendarDescriptionForEvent(
                    "[TimeAPK][Reminder]:$otherId:v2:occ=42:days=3:rr=0",
                    1
                )
            )
            assertFalse(
                ScheduleSyncManager.isManagedCalendarDescriptionForEvent(
                    "[TimeAPK][Milestone]:$otherId:v2:trigger=42",
                    1
                )
            )
        }
    }

    @Test
    fun managedCalendarMetadataKind_acceptsOnlyRequestedTimeApkKinds() {
        assertTrue(ScheduleSyncManager.isManagedCalendarMetadataKind("reminder_v2", true, false))
        assertTrue(ScheduleSyncManager.isManagedCalendarMetadataKind("reminder_rrule_v2", true, false))
        assertFalse(ScheduleSyncManager.isManagedCalendarMetadataKind("milestone_v2", true, false))

        assertFalse(ScheduleSyncManager.isManagedCalendarMetadataKind("reminder_v2", false, true))
        assertTrue(ScheduleSyncManager.isManagedCalendarMetadataKind("milestone_v2", false, true))

        assertTrue(ScheduleSyncManager.isManagedCalendarMetadataKind("reminder_v2", true, true))
        assertTrue(ScheduleSyncManager.isManagedCalendarMetadataKind("reminder_rrule_v2", true, true))
        assertTrue(ScheduleSyncManager.isManagedCalendarMetadataKind("milestone_v2", true, true))
        assertFalse(ScheduleSyncManager.isManagedCalendarMetadataKind(null, true, true))
        assertFalse(ScheduleSyncManager.isManagedCalendarMetadataKind("unknown_v3", true, true))
    }

    @Test
    fun managedCalendarCleanup_requiresBothReadAndWritePermissions() {
        assertTrue(ScheduleSyncManager.hasRequiredCalendarCleanupPermissions(true, true))
        assertFalse(ScheduleSyncManager.hasRequiredCalendarCleanupPermissions(false, true))
        assertFalse(ScheduleSyncManager.hasRequiredCalendarCleanupPermissions(true, false))
        assertFalse(ScheduleSyncManager.hasRequiredCalendarCleanupPermissions(false, false))
    }

    @Test
    fun legacyReminderCleanup_keepsMilestoneOutsideItsScope() {
        val source = mainSource("notifications/ScheduleSyncManager.kt").readText(Charsets.UTF_8)
        val legacyMilestoneCleanup = source.substringAfter("fun clearMilestoneScheduleRemindersByEventId(")
            .substringBefore("fun removeScheduleReminderByEventId(")
        val legacyReminderCleanup = source.substringAfter("fun removeScheduleReminderByEventId(")
            .substringBefore("fun removeScheduleReminder(")
        val combinedCleanup = source.substringAfter("fun removeManagedCalendarEntries(")
            .substringBefore("internal fun isManagedCalendarDescriptionForEvent")

        assertTrue(legacyMilestoneCleanup.contains("includeReminders = false"))
        assertTrue(legacyMilestoneCleanup.contains("includeMilestones = true"))
        assertTrue(legacyReminderCleanup.contains("includeReminders = true"))
        assertTrue(legacyReminderCleanup.contains("includeMilestones = false"))
        assertTrue(combinedCleanup.contains("includeReminders = true"))
        assertTrue(combinedCleanup.contains("includeMilestones = true"))
    }

    @Test
    fun scheduleCleanup_returnsExplicitOutcomeAndDoesNotSwallowExceptions() {
        val source = mainSource("notifications/ScheduleSyncManager.kt").readText(Charsets.UTF_8)
        val directCleanup = source.substringAfter("fun removeScheduleReminder(")
            .substringBefore("fun removeManagedCalendarEntries(")
        val cleanup = source.substringAfter("fun removeManagedCalendarEntries(")
            .substringBefore("private fun buildExpectedReminderEntries")

        assertTrue(directCleanup.indexOf("return try {") < directCleanup.indexOf("hasCalendarWriteAccess"))
        assertTrue(cleanup.contains("CalendarCleanupResult.PermissionRequired"))
        assertTrue(cleanup.contains("CalendarCleanupResult.ProviderFailure"))
        assertTrue(cleanup.contains("requireCalendarCleanupQuery"))
        assertTrue(cleanup.indexOf("return try {") < cleanup.indexOf("hasRequiredCalendarCleanupPermissions"))
        assertFalse(cleanup.contains("catch (_: SecurityException) {\n        }"))
        assertFalse(cleanup.contains("catch (_: Exception) {\n        }"))
    }

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

    @Test
    fun buildReminderSyncPlan_createsDailyEntriesFromLeadDaysToOccurrence() {
        val zoneId = ZoneId.of("UTC")
        val eventDate = LocalDate.of(2026, 4, 12)
        val nowMillis = LocalDate.of(2026, 4, 9)
            .atTime(8, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val event = Event(
            id = 11,
            title = "Trip",
            date = eventDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            category = CATEGORY_OTHER,
            remindDaysBefore = 3,
            reminderTimeMinutesOfDay = 9 * 60
        )

        val plan = ScheduleSyncManager.buildReminderSyncPlan(
            event = event,
            nowMillis = nowMillis,
            zoneId = zoneId,
            useRRuleSync = true
        )

        assertEquals(listOf(3, 2, 1, 0), plan.map { it.daysLeft })
        assertTrue(plan.none { it.useRRule })
        assertEquals(
            LocalDate.of(2026, 4, 9).atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli(),
            plan.first().triggerAtMillis
        )
    }

    @Test
    fun buildReminderSyncPlan_keepsRRuleForRepeatEventsWithSameDayReminder() {
        val zoneId = ZoneId.of("UTC")
        val nowMillis = LocalDate.of(2026, 4, 9)
            .atTime(8, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val event = Event(
            id = 12,
            title = "Bill",
            date = LocalDate.of(2026, 4, 12).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            category = CATEGORY_OTHER,
            repeatType = REPEAT_MONTHLY,
            remindDaysBefore = 0,
            reminderTimeMinutesOfDay = 9 * 60
        )

        val plan = ScheduleSyncManager.buildReminderSyncPlan(
            event = event,
            nowMillis = nowMillis,
            zoneId = zoneId,
            useRRuleSync = true
        )

        assertEquals(1, plan.size)
        assertTrue(plan.single().useRRule)
        assertEquals(0, plan.single().daysLeft)
    }

    private fun mainSource(relative: String): File {
        return listOf(
            File("src/main/java/com/example/timeapk/$relative"),
            File("app/src/main/java/com/example/timeapk/$relative")
        ).firstOrNull(File::exists) ?: error("Missing main source: $relative")
    }
}
