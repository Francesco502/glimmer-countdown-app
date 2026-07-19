package com.example.timeapk.notifications

import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlinx.coroutines.CancellationException

class ReminderDiscoveryPolicyTest {
    @Test
    fun eventOneDiscoveryCannotUpdateOrDeleteEventTenOrOneHundredEntries() {
        val discovery = discoverExistingReminderEntries(
            readGranted = true,
            queryDescriptionCandidates = {
                listOf(
                    ReminderDiscoveryDescriptionCandidate(
                        101L,
                        "[TimeAPK][Reminder]:1:v2:occ=42:days=3:rr=0"
                    ),
                    ReminderDiscoveryDescriptionCandidate(
                        110L,
                        "[TimeAPK][Reminder]:10:v2:occ=42:days=3:rr=0"
                    ),
                    ReminderDiscoveryDescriptionCandidate(
                        100L,
                        "[TimeAPK][Reminder]:100:v2:occ=42:days=3:rr=0"
                    )
                )
            },
            queryMetadataCandidates = { emptyList() },
            eventId = 1
        )

        val discovered = (discovery as ReminderDiscoveryResult.Success).entries
        val expectedKey = "42:3:0"
        val updatedIds = discovered.filter { it.key == expectedKey }.map { it.calendarEventId }
        val deletedIds = discovered.filter { it.key != expectedKey }.map { it.calendarEventId }

        assertEquals(listOf(101L), updatedIds)
        assertTrue(deletedIds.isEmpty())
        assertFalse(110L in updatedIds || 110L in deletedIds)
        assertFalse(100L in updatedIds || 100L in deletedIds)
    }

    @Test
    fun discoveryFailuresAbortBeforeMutationAndPreserveKnownOwnership() {
        val failures = listOf(
            "Calendar events query returned no cursor" to {
                discoverExistingReminderEntries(
                    readGranted = true,
                    queryDescriptionCandidates = { null },
                    queryMetadataCandidates = { emptyList() },
                    eventId = 7
                )
            },
            "Calendar extended-properties query returned no cursor" to {
                discoverExistingReminderEntries(
                    readGranted = true,
                    queryDescriptionCandidates = { emptyList() },
                    queryMetadataCandidates = { null },
                    eventId = 7
                )
            },
            "Calendar permission denied" to {
                discoverExistingReminderEntries(
                    readGranted = true,
                    queryDescriptionCandidates = { throw SecurityException("denied") },
                    queryMetadataCandidates = { error("Metadata query must not run") },
                    eventId = 7
                )
            },
            "provider down" to {
                discoverExistingReminderEntries(
                    readGranted = true,
                    queryDescriptionCandidates = { throw IllegalStateException("provider down") },
                    queryMetadataCandidates = { error("Metadata query must not run") },
                    eventId = 7
                )
            }
        )

        failures.forEach { (expectedError, discovery) ->
            var mutationCalls = 0
            val result = syncAfterReminderDiscovery(
                event = ownedEvent(),
                lastSyncAt = 456L,
                discovery = discovery()
            ) {
                mutationCalls += 1
                error("Provider mutations must not run after discovery failure")
            }

            assertEquals(expectedError, result.error)
            assertEquals(183L, result.primaryScheduleEventId)
            assertEquals(5L, result.targetCalendarId)
            assertEquals(456L, result.lastSyncAt)
            assertEquals(0, mutationCalls)
        }
    }

    @Test
    fun missingReadPermissionAbortsBeforeProviderQueries() {
        var queryCalls = 0
        val discovery = discoverExistingReminderEntries(
            readGranted = false,
            queryDescriptionCandidates = { queryCalls += 1; emptyList() },
            queryMetadataCandidates = { queryCalls += 1; emptyList() },
            eventId = 7
        )

        val result = syncAfterReminderDiscovery(ownedEvent(), 789L, discovery) {
            error("Provider mutations must not run without read permission")
        }

        assertEquals("Calendar read permission required", result.error)
        assertEquals(183L, result.primaryScheduleEventId)
        assertEquals(5L, result.targetCalendarId)
        assertEquals(0, queryCalls)
    }

    @Test
    fun discoveryCancellationEscapesUnchanged() {
        val cancellation = CancellationException("cancel reminder discovery")

        val actual = assertThrows(CancellationException::class.java) {
            discoverExistingReminderEntries(
                readGranted = true,
                queryDescriptionCandidates = { throw cancellation },
                queryMetadataCandidates = { error("Metadata query must not run") },
                eventId = 7
            )
        }

        assertSame(cancellation, actual)
    }

    @Test
    fun blankProviderMessagesUseStableNonblankDiscoveryFallback() {
        listOf(null, "", "   ").forEach { providerMessage ->
            val discovery = discoverExistingReminderEntries(
                readGranted = true,
                queryDescriptionCandidates = { throw IllegalStateException(providerMessage) },
                queryMetadataCandidates = { error("Metadata query must not run") },
                eventId = 7
            )
            val result = syncAfterReminderDiscovery(ownedEvent(), 999L, discovery) {
                error("Provider mutation must not run after discovery failure")
            }

            assertEquals("Calendar provider discovery failed", result.error)
            assertTrue(result.error?.isNotBlank() == true)
        }
    }

    private fun ownedEvent() = Event(
        id = 7,
        title = "owned",
        date = 1_800_000_000_000L,
        category = CATEGORY_OTHER,
        scheduleEventId = 183L,
        targetCalendarId = 5L
    )
}
