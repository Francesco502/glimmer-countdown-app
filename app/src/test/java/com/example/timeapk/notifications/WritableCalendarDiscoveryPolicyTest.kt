package com.example.timeapk.notifications

import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WritableCalendarDiscoveryPolicyTest {
    @Test
    fun nullSecurityAndProviderFailuresAbortWithoutAnyMutation() {
        val failures = listOf(
            "Calendar query returned no cursor" to {
                discoverWritableCalendars(
                    queryDetailed = { null },
                    queryLegacy = { error("Legacy query must not run after a null cursor") }
                )
            },
            "Calendar permission denied" to {
                discoverWritableCalendars(
                    queryDetailed = { throw SecurityException("denied") },
                    queryLegacy = { error("Legacy query must not run after a permission failure") }
                )
            },
            "provider down" to {
                discoverWritableCalendars(
                    queryDetailed = { throw IllegalStateException("provider down") },
                    queryLegacy = { error("Legacy query must not run after a provider failure") }
                )
            }
        )

        failures.forEach { (expectedError, discover) ->
            var mutationCalls = 0
            val result = syncAfterWritableCalendarDiscovery(
                event = ownedEvent(),
                preferredCalendarId = 8L,
                lastSyncAt = 456L,
                discovery = discover(),
                onNoWritableCalendar = {
                    mutationCalls += 1
                    error("Cleanup must not run after writable-calendar discovery failure")
                },
                onWritableCalendar = {
                    mutationCalls += 1
                    error("Insert/update must not run after writable-calendar discovery failure")
                }
            )

            assertEquals(expectedError, result.error)
            assertEquals(183L, result.primaryScheduleEventId)
            assertEquals(5L, result.targetCalendarId)
            assertEquals(456L, result.lastSyncAt)
            assertEquals(0, mutationCalls)
        }
    }

    @Test
    fun successfulEmptyDiscoveryAloneEntersNoWritableCalendarPolicy() {
        val discovery = discoverWritableCalendars(
            queryDetailed = { emptyList() },
            queryLegacy = { emptyList() }
        )
        var cleanupCalls = 0

        val result = syncAfterWritableCalendarDiscovery(
            event = ownedEvent(),
            preferredCalendarId = null,
            lastSyncAt = 789L,
            discovery = discovery,
            onNoWritableCalendar = {
                cleanupCalls += 1
                ScheduleSyncManager.ScheduleSyncResult(null, null, 789L, ScheduleSyncManager.ERROR_NO_WRITABLE_CALENDAR)
            },
            onWritableCalendar = { error("No writable calendar should be selected") }
        )

        assertTrue(discovery is WritableCalendarDiscoveryResult.Success)
        assertEquals(ScheduleSyncManager.ERROR_NO_WRITABLE_CALENDAR, result.error)
        assertEquals(1, cleanupCalls)
    }

    private fun ownedEvent() = Event(
        id = 7,
        title = "owned",
        date = 1_800_000_000_000L,
        category = CATEGORY_OTHER,
        scheduleEventId = 183L,
        targetCalendarId = 5L,
        syncToScheduleEnabled = true
    )
}
