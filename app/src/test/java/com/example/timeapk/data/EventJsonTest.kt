package com.example.timeapk.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventJsonTest {

    @Test
    fun parseEventsFromJson_validArray_returnsAllEvents() {
        val json = """
            [
                {"title":"Birthday","date":1700000000000,"category":"birthday","note":"","repeatType":"yearly","remindEnabled":false,"isLunar":false},
                {"title":"Anniversary","date":1700100000000,"category":"anniversary","note":"test","repeatType":"none","remindEnabled":true,"isLunar":false}
            ]
        """.trimIndent()

        val result = parseEventsFromJson(json)

        assertEquals(2, result.events.size)
        assertEquals(0, result.errorCount)
        assertEquals("Birthday", result.events[0].title)
        assertEquals("Anniversary", result.events[1].title)
    }

    @Test
    fun parseEventsFromJson_emptyArray_returnsEmptyList() {
        val result = parseEventsFromJson("[]")

        assertTrue(result.events.isEmpty())
        assertEquals(0, result.errorCount)
    }

    @Test
    fun parseEventsFromJson_malformedTopLevel_returnsErrorCountMinusOne() {
        val result = parseEventsFromJson("not json at all")

        assertTrue(result.events.isEmpty())
        assertEquals(-1, result.errorCount)
    }

    @Test
    fun parseEventsFromJson_partialFailure_skipsBadEntries() {
        val json = """
            [
                {"title":"Good","date":1700000000000,"category":"other","note":"","repeatType":"none","remindEnabled":false,"isLunar":false},
                {"title":123,"date":"not_a_number"},
                {"title":"Also Good","date":1700100000000,"category":"birthday","note":"","repeatType":"none","remindEnabled":false,"isLunar":false}
            ]
        """.trimIndent()

        val result = parseEventsFromJson(json)

        assertEquals(2, result.events.size)
        assertEquals(1, result.errorCount)
        assertEquals("Good", result.events[0].title)
        assertEquals("Also Good", result.events[1].title)
    }

    @Test
    fun parseEventsFromJson_defaultsApplied() {
        val json = """[{"title":"Minimal"}]"""

        val result = parseEventsFromJson(json)

        assertEquals(1, result.events.size)
        val event = result.events[0]
        assertEquals("Minimal", event.title)
        assertEquals(REPEAT_NONE, event.repeatType)
        assertEquals("", event.note)
        assertEquals(false, event.remindEnabled)
        assertEquals(true, event.syncToScheduleEnabled)
        assertEquals(false, event.isLunar)
    }

    @Test
    fun parseEventsFromJson_unknownRepeatType_defaultsToNone() {
        val json = """[{"title":"Test","repeatType":"unknown_type"}]"""

        val result = parseEventsFromJson(json)

        assertEquals(1, result.events.size)
        assertEquals(REPEAT_NONE, result.events[0].repeatType)
    }
}
