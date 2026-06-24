package com.example.timeapk.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Base64
import java.util.TimeZone

class EventJsonTest {
    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUpTimeZone() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Hong_Kong"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

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

    @Test
    fun parseEventsFromBackupBytes_jsonBytes_usesJsonParser() {
        val json = """[{"title":"From JSON","date":1700000000000}]"""

        val result = parseEventsFromBackupBytes(json.toByteArray(Charsets.UTF_8))

        assertEquals(1, result.events.size)
        assertEquals(0, result.errorCount)
        assertEquals("From JSON", result.events[0].title)
    }

    @Test
    fun parseEventsFromBackupBytesDetailed_jsonBytes_reportsSourceAndRowCounts() {
        val json = """
            [
                {"title":"Good","date":1700000000000},
                {"title":123,"date":"bad"},
                {"title":"Also Good","date":1700100000000}
            ]
        """.trimIndent()

        val result = parseEventsFromBackupBytesDetailed(json.toByteArray(Charsets.UTF_8))

        assertEquals(BackupSource.JSON, result.source)
        assertNull(result.failure)
        assertEquals(3, result.recognizedCount)
        assertEquals(2, result.events.size)
        assertEquals(1, result.parseErrorCount)
        assertEquals(0, result.skippedDuplicateCount)
    }

    @Test
    fun parseEventsFromBackupBytes_legacyRememberDaysRealm_deduplicatesByOidAndMapsFields() {
        val birthdayOid = "8ddb67942819408b8c8d3a1b835e977c"
        val emojiOid = "0e1953ce90154b9496b49365af4d5f33"
        val backup = legacyRealmBackup(
            legacyDayRecord(
                title = "佟宝生日旧",
                targetIso = "1996-01-24T16:00:00.000Z",
                createIso = "2024-04-10T13:33:46.867Z",
                modifyIso = "2024-04-12T07:15:47.519Z",
                oid = birthdayOid,
                reminderTime = "18:00",
                remark = "旧备注"
            ),
            legacyDayRecord(
                title = "佟宝生日",
                targetIso = "1996-01-24T16:00:00.000Z",
                createIso = "2024-04-10T13:33:46.867Z",
                modifyIso = "2024-04-17T07:15:47.519Z",
                oid = birthdayOid,
                reminderTime = "18:00",
                remark = "标题包括生日，破壳日，出生等关键字会自动计算显示生日标签"
            ),
            legacyDayRecord(
                title = "💍🏠",
                targetIso = "2024-12-11T16:00:00.000Z",
                createIso = "2024-12-13T00:47:13.387Z",
                modifyIso = "2024-12-13T00:47:51.056Z",
                oid = emojiOid,
                uid = null,
                reminderTime = "08:00"
            )
        )

        val result = parseEventsFromBackupBytes(backup)

        assertEquals(2, result.events.size)
        assertEquals(0, result.errorCount)

        val birthday = result.events.first { it.title == "佟宝生日" }
        assertEquals(utcDateMillis(LocalDate.of(1996, 1, 25)), birthday.date)
        assertEquals(Instant.parse("2024-04-10T13:33:46.867Z").toEpochMilli(), birthday.createdAt)
        assertEquals(CATEGORY_BIRTHDAY, birthday.category)
        assertEquals(REPEAT_YEARLY, birthday.repeatType)
        assertEquals("标题包括生日，破壳日，出生等关键字会自动计算显示生日标签", birthday.note)
        assertEquals(18 * 60, birthday.reminderTimeMinutesOfDay)
        assertFalse(birthday.remindEnabled)
        assertFalse(birthday.syncToScheduleEnabled)

        val emoji = result.events.first { it.title == "💍🏠" }
        assertEquals(utcDateMillis(LocalDate.of(2024, 12, 12)), emoji.date)
        assertEquals(CATEGORY_OTHER, emoji.category)
        assertEquals(REPEAT_NONE, emoji.repeatType)
    }

    @Test
    fun parseEventsFromBackupBytesDetailed_legacyRememberDaysRealm_reportsInternalDuplicateCount() {
        val birthdayOid = "8ddb67942819408b8c8d3a1b835e977c"
        val backup = legacyRealmBackup(
            legacyDayRecord(
                title = "佟宝生日旧",
                targetIso = "1996-01-24T16:00:00.000Z",
                createIso = "2024-04-10T13:33:46.867Z",
                modifyIso = "2024-04-12T07:15:47.519Z",
                oid = birthdayOid
            ),
            legacyDayRecord(
                title = "佟宝生日",
                targetIso = "1996-01-24T16:00:00.000Z",
                createIso = "2024-04-10T13:33:46.867Z",
                modifyIso = "2024-04-17T07:15:47.519Z",
                oid = birthdayOid
            )
        )

        val result = parseEventsFromBackupBytesDetailed(backup)

        assertEquals(BackupSource.LEGACY_REMEMBER_DAYS_MDB, result.source)
        assertNull(result.failure)
        assertEquals(2, result.recognizedCount)
        assertEquals(1, result.events.size)
        assertEquals(1, result.skippedDuplicateCount)
        assertEquals("佟宝生日", result.events.single().title)
    }

    @Test
    fun parseEventsFromBackupBytesDetailed_legacyRememberDaysFixture_mapsSanitizedBackup() {
        val fixtureBase64 = requireNotNull(
            javaClass.classLoader?.getResource("legacy_remember_days_minimal.mdb.b64")
        ).readText()
        val backup = Base64.getMimeDecoder().decode(fixtureBase64)

        val result = parseEventsFromBackupBytesDetailed(backup)

        assertEquals(BackupSource.LEGACY_REMEMBER_DAYS_MDB, result.source)
        assertNull(result.failure)
        assertEquals(2, result.recognizedCount)
        assertEquals(2, result.events.size)
        assertTrue(result.events.any { it.title == "测试生日" })
        assertTrue(result.events.any { it.title == "测试纪念日" })

        val birthday = result.events.first { it.title == "测试生日" }
        assertEquals(CATEGORY_BIRTHDAY, birthday.category)
        assertEquals(REPEAT_YEARLY, birthday.repeatType)
        assertEquals("脱敏 fixture 生日备注", birthday.note)

        val anniversary = result.events.first { it.title == "测试纪念日" }
        assertEquals(CATEGORY_ANNIVERSARY, anniversary.category)
        assertEquals(REPEAT_NONE, anniversary.repeatType)
    }

    @Test
    fun parseEventsFromBackupBytes_unsupportedBinary_returnsErrorCountMinusOne() {
        val result = parseEventsFromBackupBytes(byteArrayOf(1, 2, 3, 4, 5))

        assertTrue(result.events.isEmpty())
        assertEquals(-1, result.errorCount)
    }

    @Test
    fun parseEventsFromBackupBytesDetailed_emptyAndUnsupportedBytes_reportFailureReason() {
        val empty = parseEventsFromBackupBytesDetailed(ByteArray(0))
        val unsupported = parseEventsFromBackupBytesDetailed(byteArrayOf(1, 2, 3, 4, 5))

        assertEquals(BackupParseFailure.EMPTY_FILE, empty.failure)
        assertEquals(BackupParseFailure.UNSUPPORTED_FORMAT, unsupported.failure)
        assertTrue(empty.events.isEmpty())
        assertTrue(unsupported.events.isEmpty())
    }

    @Test
    fun filterExistingDuplicateEvents_skipsEventsWithSameTitleDateAndCategory() {
        val duplicateDate = utcDateMillis(LocalDate.of(2026, 6, 24))
        val existing = listOf(
            Event(
                title = " 佟宝生日 ",
                date = duplicateDate,
                category = CATEGORY_BIRTHDAY,
                repeatType = REPEAT_YEARLY
            )
        )
        val incomingDuplicate = Event(
            title = "佟宝生日",
            date = duplicateDate,
            category = CATEGORY_BIRTHDAY,
            repeatType = REPEAT_YEARLY
        )
        val incomingNew = Event(
            title = "香港领证",
            date = utcDateMillis(LocalDate.of(2025, 3, 29)),
            category = CATEGORY_ANNIVERSARY
        )

        val result = filterExistingDuplicateEvents(
            events = listOf(incomingDuplicate, incomingNew),
            existingEvents = existing
        )

        assertEquals(1, result.importableEvents.size)
        assertEquals("香港领证", result.importableEvents.single().title)
        assertEquals(1, result.existingDuplicateCount)
    }

    private fun legacyRealmBackup(vararg records: ByteArray): ByteArray {
        return ByteArrayOutputStream().apply {
            write(byteArrayOf(0xde.toByte(), 0xc0.toByte(), 0xef.toByte(), 0xbe.toByte()))
            write(legacyString("DayDTO"))
            write(legacyString("target_time"))
            write(legacyString("create_time"))
            write(legacyString("modify_time"))
            write(legacyString("title"))
            write(legacyString("oid"))
            records.forEach { write(it) }
        }.toByteArray()
    }

    private fun legacyDayRecord(
        title: String,
        targetIso: String,
        createIso: String,
        modifyIso: String,
        oid: String,
        uid: String? = "419b3b4bb6384ce69ec8ac128cf606f1",
        reminderTime: String = "08:00",
        remark: String? = null
    ): ByteArray {
        return ByteArrayOutputStream().apply {
            write(ByteArray(64))
            write(legacyString("20,0,100"))
            write(legacyString("20,0,100"))
            write(ByteArray(24))
            write(legacyString(reminderTime))
            if (remark != null) {
                write(legacyString(remark))
            } else {
                write(ByteArray(24))
            }
            write(legacyString(targetIso))
            write(legacyString(createIso))
            write(legacyString(modifyIso))
            if (uid != null) {
                write(legacyString(uid))
            } else {
                write(ByteArray(12))
            }
            write(legacyString(title))
            write(legacyString(oid))
        }.toByteArray()
    }

    private fun legacyString(value: String): ByteArray {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val payloadSize = alignToFour(bytes.size + 1)
        return ByteArrayOutputStream().apply {
            write(bytes.size)
            write(0)
            write(0)
            write(0)
            write(bytes)
            repeat(payloadSize - bytes.size) { write(0) }
        }.toByteArray()
    }

    private fun alignToFour(value: Int): Int = ((value + 3) / 4) * 4

    private fun utcDateMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
