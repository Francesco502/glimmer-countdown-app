package com.example.timeapk.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PinnedEventIdsNormalizationTest {
    @Test
    fun parsedRestoredPinnedIdsKeepFirstOccurrenceOnly() {
        assertEquals(
            listOf(4, 2, 3),
            parsePinnedEventIds("[4,4,2,4,2,3,0]")
        )
    }

    @Test
    fun persistedPinnedIdsAreNormalizedBeforeEncoding() {
        assertEquals("[4,2,3]", encodePinnedEventIds(listOf(4, 4, 2, 4, 2, 3)))
    }
}
