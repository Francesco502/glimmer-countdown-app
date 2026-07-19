package com.example.timeapk.notifications

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class ScheduleSyncLegacyHelperContractTest {
    @Test
    fun prefixAndFailureEmptyDiscoveryHelpersCannotReturn() {
        val source = source("notifications/ScheduleSyncManager.kt")

        listOf(
            "findManagedReminderEventIds",
            "findManagedMilestoneEventIdsByEventId",
            "findEventIdsByExtendedProperty",
            "readExtendedProperties"
        ).forEach { legacyHelper ->
            assertFalse(legacyHelper, source.contains(legacyHelper))
        }
    }

    private fun source(relative: String): String = listOf(
        File("src/main/java/com/example/timeapk/$relative"),
        File("app/src/main/java/com/example/timeapk/$relative")
    ).firstOrNull(File::exists)?.readText(Charsets.UTF_8)
        ?: error("Missing source: $relative")
}
