package com.example.timeapk.data

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

enum class BackupSource {
    JSON,
    LEGACY_REMEMBER_DAYS_MDB
}

enum class BackupParseFailure {
    EMPTY_FILE,
    UNSUPPORTED_FORMAT,
    NO_EVENTS_FOUND
}

data class BackupParseResult(
    val events: List<Event>,
    val source: BackupSource?,
    val recognizedCount: Int,
    val parseErrorCount: Int,
    val skippedDuplicateCount: Int,
    val failure: BackupParseFailure?
)

data class ExistingDuplicateFilterResult(
    val importableEvents: List<Event>,
    val existingDuplicateCount: Int
)

fun parseEventsFromBackupBytes(bytes: ByteArray): ParseResult {
    val result = parseEventsFromBackupBytesDetailed(bytes)
    return if (result.failure == BackupParseFailure.EMPTY_FILE ||
        result.failure == BackupParseFailure.UNSUPPORTED_FORMAT
    ) {
        ParseResult(emptyList(), -1)
    } else {
        ParseResult(result.events, result.parseErrorCount)
    }
}

fun parseEventsFromBackupBytesDetailed(bytes: ByteArray): BackupParseResult {
    if (bytes.isEmpty()) {
        return BackupParseResult(
            events = emptyList(),
            source = null,
            recognizedCount = 0,
            parseErrorCount = 0,
            skippedDuplicateCount = 0,
            failure = BackupParseFailure.EMPTY_FILE
        )
    }

    val jsonText = bytes.toString(Charsets.UTF_8).trimStart('\uFEFF')
    val jsonResult = parseEventsFromJson(jsonText)
    if (jsonResult.errorCount >= 0) {
        val recognizedCount = jsonResult.events.size + jsonResult.errorCount
        return BackupParseResult(
            events = jsonResult.events,
            source = BackupSource.JSON,
            recognizedCount = recognizedCount,
            parseErrorCount = jsonResult.errorCount,
            skippedDuplicateCount = 0,
            failure = if (jsonResult.events.isEmpty()) BackupParseFailure.NO_EVENTS_FOUND else null
        )
    }

    val legacyResult = parseEventsFromLegacyRememberDaysRealmDetailed(bytes)
    return if (legacyResult == null) {
        BackupParseResult(
            events = emptyList(),
            source = null,
            recognizedCount = 0,
            parseErrorCount = 0,
            skippedDuplicateCount = 0,
            failure = BackupParseFailure.UNSUPPORTED_FORMAT
        )
    } else {
        BackupParseResult(
            events = legacyResult.events,
            source = BackupSource.LEGACY_REMEMBER_DAYS_MDB,
            recognizedCount = legacyResult.recognizedCount,
            parseErrorCount = 0,
            skippedDuplicateCount = (legacyResult.recognizedCount - legacyResult.events.size).coerceAtLeast(0),
            failure = if (legacyResult.events.isEmpty()) BackupParseFailure.NO_EVENTS_FOUND else null
        )
    }
}

fun filterExistingDuplicateEvents(
    events: List<Event>,
    existingEvents: List<Event>
): ExistingDuplicateFilterResult {
    val seen = existingEvents.mapTo(mutableSetOf()) { it.importDuplicateKey() }
    val importableEvents = mutableListOf<Event>()
    var duplicateCount = 0

    events.forEach { event ->
        if (seen.add(event.importDuplicateKey())) {
            importableEvents += event
        } else {
            duplicateCount += 1
        }
    }

    return ExistingDuplicateFilterResult(importableEvents, duplicateCount)
}

private data class LegacyEncodedString(
    val offset: Int,
    val value: String,
    val nextOffset: Int
)

private data class LegacyDayRecord(
    val sourceOffset: Int,
    val oid: String,
    val title: String,
    val targetIso: String,
    val createIso: String,
    val modifyIso: String,
    val reminderTime: String?,
    val remark: String
)

private data class LegacyParseResult(
    val events: List<Event>,
    val recognizedCount: Int
)

private data class ImportDuplicateKey(
    val title: String,
    val date: Long,
    val category: String,
    val note: String,
    val colorHex: String?,
    val repeatType: String,
    val remindDaysBefore: Int,
    val reminderTimeMinutesOfDay: Int,
    val remindEnabled: Boolean,
    val syncToScheduleEnabled: Boolean,
    val isLunar: Boolean
)

private val legacyIsoRegex =
    Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$""")
private val legacyHex32Regex = Regex("""^[0-9a-f]{32}$""")
private val legacyReminderTimeRegex = Regex("""^\d{1,2}:\d{2}$""")
private val legacyNumberListRegex = Regex("""^\d+(,\d+)+$""")

private fun parseEventsFromLegacyRememberDaysRealmDetailed(bytes: ByteArray): LegacyParseResult? {
    if (!bytes.containsAscii("DayDTO") || !bytes.containsAscii("target_time")) {
        return null
    }

    val encodedStrings = extractLegacyEncodedStrings(bytes)
    if (encodedStrings.isEmpty()) {
        return LegacyParseResult(emptyList(), 0)
    }

    val records = encodedStrings
        .filter { legacyIsoRegex.matches(it.value) }
        .mapNotNull { targetString ->
            parseLegacyDayRecord(bytes, encodedStrings, targetString)
        }

    if (records.isEmpty()) {
        return LegacyParseResult(emptyList(), 0)
    }

    val events = records
        .groupBy { it.oid }
        .values
        .mapNotNull { candidates ->
            candidates.maxWithOrNull(
                compareBy<LegacyDayRecord> { it.modifyIso }
                    .thenBy { it.sourceOffset }
            )
        }
        .sortedBy { it.sourceOffset }
        .mapNotNull { it.toEvent() }

    return LegacyParseResult(events, records.size)
}

private fun parseLegacyDayRecord(
    bytes: ByteArray,
    encodedStrings: List<LegacyEncodedString>,
    targetString: LegacyEncodedString
): LegacyDayRecord? {
    val createString = nextLegacyString(bytes, targetString.nextOffset) ?: return null
    if (!legacyIsoRegex.matches(createString.value)) return null

    val modifyString = nextLegacyString(bytes, createString.nextOffset) ?: return null
    if (!legacyIsoRegex.matches(modifyString.value)) return null

    val firstIdentityOrTitle = nextLegacyString(bytes, modifyString.nextOffset, maxSkipBytes = 96) ?: return null
    val titleString: LegacyEncodedString
    val oidString: LegacyEncodedString

    if (legacyHex32Regex.matches(firstIdentityOrTitle.value)) {
        titleString = nextLegacyString(bytes, firstIdentityOrTitle.nextOffset, maxSkipBytes = 32) ?: return null
        oidString = nextLegacyString(bytes, titleString.nextOffset, maxSkipBytes = 32) ?: return null
    } else {
        titleString = firstIdentityOrTitle
        oidString = nextLegacyString(bytes, titleString.nextOffset, maxSkipBytes = 32) ?: return null
    }

    val title = titleString.value.trim()
    if (title.isBlank() || !legacyHex32Regex.matches(oidString.value)) return null

    val previousStrings = encodedStrings
        .asSequence()
        .filter { it.offset < targetString.offset && targetString.offset - it.offset <= 360 }
        .sortedBy { it.offset }
        .toList()
    val reminderTimeString = previousStrings.lastOrNull {
        legacyReminderTimeRegex.matches(it.value)
    }
    val remark = previousStrings
        .asSequence()
        .filter { reminderTimeString == null || it.offset > reminderTimeString.offset }
        .map { it.value.trim() }
        .filter { it.isPossibleLegacyRemark() }
        .lastOrNull()
        .orEmpty()

    return LegacyDayRecord(
        sourceOffset = targetString.offset,
        oid = oidString.value,
        title = title,
        targetIso = targetString.value,
        createIso = createString.value,
        modifyIso = modifyString.value,
        reminderTime = reminderTimeString?.value,
        remark = remark
    )
}

private fun LegacyDayRecord.toEvent(): Event? {
    val targetDateMillis = runCatching {
        val localDate = Instant.parse(targetIso)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }.getOrNull() ?: return null

    val createdAtMillis = runCatching {
        Instant.parse(createIso).toEpochMilli()
    }.getOrDefault(System.currentTimeMillis())

    val birthday = title.isLegacyBirthdayTitle()
    val category = when {
        birthday -> CATEGORY_BIRTHDAY
        title.isLegacyAnniversaryTitle() -> CATEGORY_ANNIVERSARY
        else -> CATEGORY_OTHER
    }

    return Event(
        id = 0,
        title = title,
        date = targetDateMillis,
        category = category,
        note = remark,
        colorHex = null,
        repeatType = if (birthday) REPEAT_YEARLY else REPEAT_NONE,
        remindDaysBefore = 0,
        reminderTimeMinutesOfDay = parseLegacyReminderMinutes(reminderTime) ?: 480,
        remindEnabled = false,
        syncToScheduleEnabled = false,
        scheduleEventId = null,
        targetCalendarId = null,
        lastScheduleSyncAt = null,
        lastScheduleSyncError = null,
        createdAt = createdAtMillis,
        isLunar = false
    )
}

private fun extractLegacyEncodedStrings(bytes: ByteArray): List<LegacyEncodedString> {
    val result = mutableListOf<LegacyEncodedString>()
    var offset = 0
    while (offset <= bytes.size - 5) {
        val encoded = parseLegacyStringAt(bytes, offset)
        if (encoded != null) {
            result += encoded
        }
        offset += 1
    }
    return result.distinctBy { it.offset }
}

private fun nextLegacyString(
    bytes: ByteArray,
    startOffset: Int,
    maxSkipBytes: Int = 64
): LegacyEncodedString? {
    val lastOffset = minOf(bytes.size - 5, startOffset + maxSkipBytes)
    var offset = startOffset.coerceAtLeast(0)
    while (offset <= lastOffset) {
        parseLegacyStringAt(bytes, offset)?.let { return it }
        offset += 1
    }
    return null
}

private fun parseLegacyStringAt(bytes: ByteArray, offset: Int): LegacyEncodedString? {
    if (offset < 0 || offset + 5 > bytes.size) return null
    val length = bytes.readIntLe(offset)
    if (length !in 1..512) return null

    val contentStart = offset + 4
    val contentEnd = contentStart + length
    val nextOffset = offset + 4 + alignToFour(length + 1)
    if (contentEnd >= bytes.size || nextOffset > bytes.size) return null

    for (i in contentEnd until nextOffset) {
        if (bytes[i].toInt() != 0) return null
    }

    val value = decodeUtf8Strict(bytes, contentStart, length) ?: return null
    if (!value.isLikelyLegacyString()) return null
    return LegacyEncodedString(offset, value, nextOffset)
}

private fun decodeUtf8Strict(bytes: ByteArray, offset: Int, length: Int): String? {
    return try {
        Charsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes, offset, length))
            .toString()
    } catch (_: Exception) {
        null
    }
}

private fun String.isLikelyLegacyString(): Boolean {
    if (isBlank()) return false
    if (indexOf('\u0000') >= 0 || indexOf('\uFFFD') >= 0) return false
    return none { it.isISOControl() }
}

private fun String.isPossibleLegacyRemark(): Boolean {
    if (isBlank()) return false
    if (legacyIsoRegex.matches(this)) return false
    if (legacyHex32Regex.matches(this)) return false
    if (legacyReminderTimeRegex.matches(this)) return false
    if (legacyNumberListRegex.matches(this)) return false
    if (this in legacySchemaNames) return false

    val hasNonAscii = any { it.code > 0x7f }
    val hasWhitespace = any { it.isWhitespace() }
    return hasNonAscii || hasWhitespace || length >= 8
}

private fun String.isLegacyBirthdayTitle(): Boolean {
    return contains("生日") || contains("出生") || contains("破壳")
}

private fun String.isLegacyAnniversaryTitle(): Boolean {
    return contains("纪念") ||
        contains("周年") ||
        contains("结婚") ||
        contains("领证") ||
        contains("同居") ||
        contains("恋爱") ||
        contains("相识")
}

private fun parseLegacyReminderMinutes(value: String?): Int? {
    if (value.isNullOrBlank()) return null
    val parts = value.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return sanitizeReminderTimeMinutesOfDay(hour * 60 + minute)
}

private fun Event.importDuplicateKey(): ImportDuplicateKey {
    return ImportDuplicateKey(
        title = title.trim(),
        date = date,
        category = category,
        note = note.trim(),
        colorHex = colorHex?.trim()?.uppercase(),
        repeatType = repeatType,
        remindDaysBefore = remindDaysBefore,
        reminderTimeMinutesOfDay = reminderTimeMinutesOfDay,
        remindEnabled = remindEnabled,
        syncToScheduleEnabled = syncToScheduleEnabled,
        isLunar = isLunar
    )
}

private fun ByteArray.containsAscii(value: String): Boolean {
    val needle = value.toByteArray(Charsets.US_ASCII)
    if (needle.isEmpty() || needle.size > size) return false
    outer@ for (offset in 0..size - needle.size) {
        for (i in needle.indices) {
            if (this[offset + i] != needle[i]) continue@outer
        }
        return true
    }
    return false
}

private fun ByteArray.readIntLe(offset: Int): Int {
    return (this[offset].toInt() and 0xff) or
        ((this[offset + 1].toInt() and 0xff) shl 8) or
        ((this[offset + 2].toInt() and 0xff) shl 16) or
        ((this[offset + 3].toInt() and 0xff) shl 24)
}

private fun alignToFour(value: Int): Int = ((value + 3) / 4) * 4

private val legacySchemaNames = setOf(
    "DayDTO",
    "preview_style",
    "background_setting",
    "cover_setting",
    "left_day_format",
    "background_url",
    "weight",
    "hide_desktop",
    "recycle_end_date",
    "recycle_end_num",
    "cover_url",
    "reminder_special",
    "reminder_end_time",
    "reminder_time",
    "reminder_mode",
    "advanced_days",
    "isremind",
    "istop",
    "show_notification",
    "sync",
    "recycle_num",
    "recycle",
    "isarchived",
    "remark",
    "color_custom",
    "color_type",
    "isdelete",
    "modify_num",
    "end_time",
    "islunar",
    "target_time",
    "create_time",
    "modify_time",
    "uid",
    "title",
    "id",
    "oid"
)
