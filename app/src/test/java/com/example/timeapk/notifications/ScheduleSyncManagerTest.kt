package com.example.timeapk.notifications

import android.provider.CalendarContract
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_MONTHLY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException

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
    fun managedCalendarDescription_rejectsMalformedOrUnknownMarkers() {
        listOf(
            "[TimeAPK][Reminder]:1:v3:occ=42:days=3:rr=0",
            "[TimeAPK][Reminder]:1:v2:occ=42:days=3",
            "[TimeAPK][Reminder]:1:v2:occ=42:rr=0:days=3",
            "[TimeAPK][Reminder]:1:v2:occ=42:days=3:rr=2",
            "[TimeAPK][Reminder]:1:v2:occ=9223372036854775808:days=3:rr=0",
            "[TimeAPK][Reminder]:1:v2:occ=42:days=2147483648:rr=0",
            "[TimeAPK][Reminder]:1:v2:occ=42:days=3:rr=0:extra=x",
            "[TimeAPK][Reminder]:1:anything",
            "[TimeAPK][Reminder]:1 trailing text",
            "prefix [TimeAPK][Reminder]:1",
            "[TimeAPK][Milestone]:1:v3:trigger=42",
            "[TimeAPK][Milestone]:1:v2",
            "[TimeAPK][Milestone]:1:v2:trigger=9223372036854775808",
            "[TimeAPK][Milestone]:1:v2:trigger=42:extra=x",
            "[TimeAPK][Milestone]:1:anything"
        ).forEach { description ->
            assertFalse(
                description,
                ScheduleSyncManager.isManagedCalendarDescriptionForEvent(description, 1)
            )
        }

        assertFalse(
            ScheduleSyncManager.isManagedCalendarDescriptionForEvent(
                "note first\n[TimeAPK][Reminder]:1",
                1
            )
        )
        assertFalse(
            ScheduleSyncManager.isManagedCalendarDescriptionForEvent(
                "[TimeAPK][Reminder]:9223372036854775808:v2:occ=42:days=3:rr=0",
                Int.MAX_VALUE
            )
        )
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
    fun managedCalendarCleanup_combinesExactSourcesAndDeduplicatesDeletes() {
        val gateway = populatedCleanupGateway()

        val result = ScheduleSyncManager.removeManagedCalendarEntries(gateway, 1, 100L)

        assertEquals(CalendarCleanupResult.RemovedOrNotPresent, result)
        assertEquals(setOf(100L, 101L, 102L, 103L, 104L, 105L, 106L, 107L), gateway.deleted.toSet())
        assertEquals(1, gateway.deleteAttempts.count { it == 102L })
        assertFalse(gateway.deleted.contains(108L))
        assertFalse(gateway.deleted.contains(110L))
        assertFalse(gateway.deleted.contains(111L))
    }

    @Test
    fun reminderOnlyCleanup_doesNotDeleteMilestones() {
        val gateway = populatedCleanupGateway()

        val result = ScheduleSyncManager.removeScheduleReminderByEventId(gateway, 1)

        assertEquals(CalendarCleanupResult.RemovedOrNotPresent, result)
        assertEquals(setOf(101L, 102L, 105L, 106L), gateway.deleted.toSet())
    }

    @Test
    fun managedCalendarCleanup_requiresPermissionsWithoutQueryingOrDeleting() {
        listOf(false to true, true to false, false to false).forEach { (read, write) ->
            val gateway = populatedCleanupGateway().apply {
                readGranted = read
                writeGranted = write
            }

            assertEquals(
                CalendarCleanupResult.PermissionRequired,
                ScheduleSyncManager.removeManagedCalendarEntries(gateway, 1, 100L)
            )
            assertEquals(0, gateway.descriptionQueryCount)
            assertEquals(0, gateway.metadataQueryCount)
            assertTrue(gateway.deleted.isEmpty())
        }
    }

    @Test
    fun managedCalendarCleanup_mapsNullQueriesToProviderFailure() {
        val nullDescriptions = populatedCleanupGateway().apply { descriptions = null }
        val nullMetadata = populatedCleanupGateway().apply { metadata = null }

        assertEquals(
            CalendarCleanupResult.ProviderFailure("Calendar events query returned no cursor"),
            ScheduleSyncManager.removeManagedCalendarEntries(nullDescriptions, 1, null)
        )
        assertEquals(
            CalendarCleanupResult.ProviderFailure("Calendar extended-properties query returned no cursor"),
            ScheduleSyncManager.removeManagedCalendarEntries(nullMetadata, 1, null)
        )
    }

    @Test
    fun managedCalendarCleanup_mapsQueryFailuresToExplicitResults() {
        val denied = populatedCleanupGateway().apply {
            descriptionFailure = SecurityException("denied")
        }
        val brokenCursor = populatedCleanupGateway().apply {
            metadataFailure = IllegalStateException("cursor broken")
        }

        assertEquals(
            CalendarCleanupResult.PermissionRequired,
            ScheduleSyncManager.removeManagedCalendarEntries(denied, 1, null)
        )
        assertEquals(
            CalendarCleanupResult.ProviderFailure("cursor broken"),
            ScheduleSyncManager.removeManagedCalendarEntries(brokenCursor, 1, null)
        )
    }

    @Test
    fun managedCalendarCleanupCancellationEscapesUnchanged() {
        val cancellation = CancellationException("cancel managed cleanup")
        val gateway = populatedCleanupGateway().apply {
            descriptionFailure = cancellation
        }

        val actual = assertThrows(CancellationException::class.java) {
            ScheduleSyncManager.removeManagedCalendarEntries(gateway, 1, null)
        }

        assertSame(cancellation, actual)
        assertTrue(gateway.deleted.isEmpty())
    }

    @Test
    fun calendarMetadataWriteCancellationEscapesWithoutWarning() {
        val cancellation = CancellationException("cancel metadata write")
        var warningCount = 0

        val actual = assertThrows(CancellationException::class.java) {
            executeCalendarMetadataWrite(
                write = { throw cancellation },
                onPermissionFailure = { warningCount += 1 },
                onProviderFailure = { warningCount += 1 }
            )
        }

        assertSame(cancellation, actual)
        assertEquals(0, warningCount)
    }

    @Test
    fun managedCalendarCleanup_mapsDeleteSecurityException() {
        val gateway = populatedCleanupGateway().apply {
            deleteFailures.getOrPut(101L, ::ArrayDeque).add(SecurityException("denied"))
        }

        assertEquals(
            CalendarCleanupResult.PermissionRequired,
            ScheduleSyncManager.removeManagedCalendarEntries(gateway, 1, null)
        )
    }

    @Test
    fun managedCalendarCleanup_returnsFailureAfterPartialDeleteAndRetryRemovesRemaining() {
        val gateway = populatedCleanupGateway().apply {
            existing += setOf(101L, 102L, 103L, 104L, 105L, 106L, 107L)
            deleteFailures.getOrPut(102L, ::ArrayDeque).add(IllegalStateException("provider down"))
        }

        assertEquals(
            CalendarCleanupResult.ProviderFailure("provider down"),
            ScheduleSyncManager.removeManagedCalendarEntries(gateway, 1, null)
        )
        assertFalse(gateway.existing.contains(101L))
        assertTrue(gateway.existing.contains(102L))

        assertEquals(
            CalendarCleanupResult.RemovedOrNotPresent,
            ScheduleSyncManager.removeManagedCalendarEntries(gateway, 1, null)
        )
        assertTrue(gateway.existing.isEmpty())
    }

    @Test
    fun clearAllMilestones_requiresReadAndWriteWithoutCallingProvider() {
        var queryCalls = 0

        val result = ScheduleSyncManager.cleanupAllMilestoneEntries(
            readGranted = false,
            writeGranted = true,
            queryMetadataIds = { queryCalls += 1; emptyList() },
            queryDescriptionIds = { queryCalls += 1; emptyList() },
            deleteEvent = {}
        )

        assertEquals(CalendarCleanupResult.PermissionRequired, result)
        assertEquals(0, queryCalls)
    }

    @Test
    fun clearAllMilestones_mapsProviderAndDeleteFailures() {
        val queryFailure = ScheduleSyncManager.cleanupAllMilestoneEntries(
            readGranted = true,
            writeGranted = true,
            queryMetadataIds = { throw IllegalStateException("query failed") },
            queryDescriptionIds = { emptyList() },
            deleteEvent = {}
        )
        val deleteFailure = ScheduleSyncManager.cleanupAllMilestoneEntries(
            readGranted = true,
            writeGranted = true,
            queryMetadataIds = { listOf(10L) },
            queryDescriptionIds = { listOf(10L, 11L) },
            deleteEvent = { id -> if (id == 11L) throw SecurityException("denied") }
        )

        assertEquals(CalendarCleanupResult.ProviderFailure("query failed"), queryFailure)
        assertEquals(CalendarCleanupResult.PermissionRequired, deleteFailure)
    }

    @Test
    fun disabledSync_cleanupFailureBecomesErrorAndRetainsKnownOwnership() {
        val event = ownedCalendarEvent()
        var cleanupAttempts = 0

        val cleanup = ScheduleSyncManager.cleanupReminderSeriesEntries(
            expectedEntriesPresent = false,
            staleIds = emptySet(),
            cleanupWholeSeries = {
                cleanupAttempts += 1
                CalendarCleanupResult.PermissionRequired
            },
            cleanupSingleEntry = { error("No single-entry cleanup expected") }
        )
        val result = ScheduleSyncManager.scheduleSyncResultAfterCleanup(
            event = event,
            primaryScheduleEventId = null,
            targetCalendarId = null,
            lastSyncAt = 456L,
            cleanupResult = cleanup
        )

        assertEquals(1, cleanupAttempts)
        assertEquals(183L, result.primaryScheduleEventId)
        assertEquals(5L, result.targetCalendarId)
        assertEquals("Calendar permission required", result.error)
    }

    @Test
    fun emptySeries_cleanupFailureBecomesErrorWithoutClearingOwnership() {
        val event = ownedCalendarEvent()

        val cleanup = ScheduleSyncManager.cleanupReminderSeriesEntries(
            expectedEntriesPresent = false,
            staleIds = setOf(183L),
            cleanupWholeSeries = { CalendarCleanupResult.ProviderFailure("provider down") },
            cleanupSingleEntry = { error("Empty series must use whole-series cleanup") }
        )
        val result = ScheduleSyncManager.scheduleSyncResultAfterCleanup(
            event = event,
            primaryScheduleEventId = null,
            targetCalendarId = null,
            lastSyncAt = 789L,
            cleanupResult = cleanup
        )

        assertEquals(183L, result.primaryScheduleEventId)
        assertEquals(5L, result.targetCalendarId)
        assertEquals("provider down", result.error)
    }

    @Test
    fun staleSeries_checksEveryDeleteAndReportsFailure() {
        val attempts = mutableListOf<Long>()

        val cleanup = ScheduleSyncManager.cleanupReminderSeriesEntries(
            expectedEntriesPresent = true,
            staleIds = setOf(201L, 202L),
            cleanupWholeSeries = { error("Non-empty series must clean stale entries individually") },
            cleanupSingleEntry = { id ->
                attempts += id
                if (id == 201L) {
                    CalendarCleanupResult.ProviderFailure("stale delete failed")
                } else {
                    CalendarCleanupResult.RemovedOrNotPresent
                }
            }
        )
        val result = ScheduleSyncManager.scheduleSyncResultAfterCleanup(
            event = ownedCalendarEvent(),
            primaryScheduleEventId = 300L,
            targetCalendarId = 8L,
            lastSyncAt = 999L,
            cleanupResult = cleanup
        )

        assertEquals(listOf(201L, 202L), attempts)
        assertEquals(300L, result.primaryScheduleEventId)
        assertEquals(8L, result.targetCalendarId)
        assertEquals("stale delete failed", result.error)
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

    private fun populatedCleanupGateway(): FakeCalendarCleanupGateway {
        return FakeCalendarCleanupGateway(
            descriptions = listOf(
                CalendarCleanupDescriptionCandidate(101L, "[TimeAPK][Reminder]:1"),
                CalendarCleanupDescriptionCandidate(102L, "[TimeAPK][Reminder]:1:v2:occ=-42:days=-3:rr=0"),
                CalendarCleanupDescriptionCandidate(103L, "[TimeAPK][Milestone]:1"),
                CalendarCleanupDescriptionCandidate(104L, "[TimeAPK][Milestone]:1:v2:trigger=-42"),
                CalendarCleanupDescriptionCandidate(110L, "[TimeAPK][Reminder]:10"),
                CalendarCleanupDescriptionCandidate(111L, "[TimeAPK][Reminder]:1:v3:occ=42:days=3:rr=0")
            ),
            metadata = listOf(
                CalendarCleanupMetadataCandidate(102L, "reminder_v2"),
                CalendarCleanupMetadataCandidate(105L, "reminder_v2"),
                CalendarCleanupMetadataCandidate(106L, "reminder_rrule_v2"),
                CalendarCleanupMetadataCandidate(107L, "milestone_v2"),
                CalendarCleanupMetadataCandidate(108L, "unknown_v3")
            )
        )
    }

    private fun ownedCalendarEvent() = Event(
        id = 1,
        title = "owned",
        date = 1_800_000_000_000L,
        category = CATEGORY_OTHER,
        syncToScheduleEnabled = true,
        scheduleEventId = 183L,
        targetCalendarId = 5L
    )

    private class FakeCalendarCleanupGateway(
        var descriptions: List<CalendarCleanupDescriptionCandidate>?,
        var metadata: List<CalendarCleanupMetadataCandidate>?
    ) : CalendarCleanupGateway {
        var readGranted = true
        var writeGranted = true
        var descriptionQueryCount = 0
        var metadataQueryCount = 0
        var descriptionFailure: Exception? = null
        var metadataFailure: Exception? = null
        val deleteFailures = mutableMapOf<Long, ArrayDeque<Exception>>()
        val deleteAttempts = mutableListOf<Long>()
        val deleted = mutableListOf<Long>()
        val existing = mutableSetOf<Long>()

        override fun hasReadPermission(): Boolean = readGranted

        override fun hasWritePermission(): Boolean = writeGranted

        override fun queryDescriptionCandidates(
            eventId: Int,
            includeReminders: Boolean,
            includeMilestones: Boolean
        ): List<CalendarCleanupDescriptionCandidate>? {
            descriptionQueryCount += 1
            descriptionFailure?.let { throw it }
            return descriptions
        }

        override fun queryMetadataCandidates(eventId: Int): List<CalendarCleanupMetadataCandidate>? {
            metadataQueryCount += 1
            metadataFailure?.let { throw it }
            return metadata
        }

        override fun deleteEvent(calendarEventId: Long) {
            deleteAttempts += calendarEventId
            deleteFailures[calendarEventId]?.removeFirstOrNull()?.let { throw it }
            existing.remove(calendarEventId)
            deleted += calendarEventId
        }
    }
}
