package com.example.timeapk.data

import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.getNextLunarOccurrence
import com.example.timeapk.ui.utils.nextOccurrenceDate
import org.json.JSONArray
import org.json.JSONObject

fun List<Event>.toJsonString(): String {
    val arr = JSONArray()
    for (e in this) {
        arr.put(
            JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("date", e.date)
                put("category", e.category)
                put("note", e.note)
                put("colorHex", e.colorHex ?: JSONObject.NULL)
                put("repeatType", e.repeatType)
                put("remindDaysBefore", e.remindDaysBefore)
                put("reminderTimeMinutesOfDay", e.reminderTimeMinutesOfDay)
                put("remindEnabled", e.remindEnabled)
                put("syncToScheduleEnabled", e.syncToScheduleEnabled)
                put("createdAt", e.createdAt)
                put("isLunar", e.isLunar)
            }
        )
    }
    return arr.toString()
}

data class ParseResult(val events: List<Event>, val errorCount: Int)

fun parseEventsFromJson(json: String): ParseResult {
    val list = mutableListOf<Event>()
    var errorCount = 0
    try {
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                val repeatType = normalizeRepeatType(o.optionalString("repeatType", REPEAT_NONE))
                val category = o.optionalString("category", "")
                list.add(
                    Event(
                        id = 0,
                        title = o.optionalString("title", ""),
                        date = o.optionalLong("date", System.currentTimeMillis()),
                        category = category,
                        note = o.optionalString("note", ""),
                        colorHex = o.optionalNullableString("colorHex"),
                        repeatType = repeatType,
                        remindDaysBefore = sanitizeRemindDaysBefore(o.optionalInt("remindDaysBefore", 0)),
                        reminderTimeMinutesOfDay = sanitizeReminderTimeMinutesOfDay(o.optionalInt("reminderTimeMinutesOfDay", 480)),
                        remindEnabled = o.optionalBoolean("remindEnabled", false),
                        syncToScheduleEnabled = o.optionalBoolean("syncToScheduleEnabled", true),
                        scheduleEventId = null,
                        targetCalendarId = null,
                        lastScheduleSyncAt = null,
                        lastScheduleSyncError = null,
                        createdAt = o.optionalLong("createdAt", System.currentTimeMillis()),
                        isLunar = o.optionalBoolean("isLunar", false)
                    )
                )
            } catch (_: Exception) {
                errorCount++
            }
        }
    } catch (_: Exception) {
        return ParseResult(emptyList(), -1)
    }
    return ParseResult(list, errorCount)
}

private fun JSONObject.optionalString(name: String, defaultValue: String): String {
    if (!has(name) || isNull(name)) return defaultValue
    return get(name) as? String ?: throw IllegalArgumentException("Expected string for $name")
}

private fun JSONObject.optionalNullableString(name: String): String? {
    if (!has(name) || isNull(name)) return null
    return get(name) as? String ?: throw IllegalArgumentException("Expected string for $name")
}

private fun JSONObject.optionalLong(name: String, defaultValue: Long): Long {
    if (!has(name) || isNull(name)) return defaultValue
    return (get(name) as? Number)?.toLong() ?: throw IllegalArgumentException("Expected number for $name")
}

private fun JSONObject.optionalInt(name: String, defaultValue: Int): Int {
    if (!has(name) || isNull(name)) return defaultValue
    return (get(name) as? Number)?.toInt() ?: throw IllegalArgumentException("Expected number for $name")
}

private fun JSONObject.optionalBoolean(name: String, defaultValue: Boolean): Boolean {
    if (!has(name) || isNull(name)) return defaultValue
    return get(name) as? Boolean ?: throw IllegalArgumentException("Expected boolean for $name")
}

private fun normalizeRepeatType(value: String): String {
    return when (value) {
        REPEAT_NONE,
        REPEAT_DAILY,
        REPEAT_WEEKLY,
        REPEAT_MONTHLY,
        REPEAT_HALF_YEARLY,
        REPEAT_YEARLY -> value

        else -> REPEAT_NONE
    }
}

private fun escapeCsvField(s: String): String {
    return if (s.contains(',') || s.contains('"') || s.contains('\n')) {
        "\"" + s.replace("\"", "\"\"") + "\""
    } else {
        s
    }
}

fun List<Event>.toCsvString(): String {
    val header = "title,date,category,note,colorHex,repeatType,remindDaysBefore,reminderTimeMinutesOfDay,remindEnabled,syncToScheduleEnabled,createdAt,isLunar"
    val lines = mutableListOf(header)
    for (e in this) {
        lines.add(
            listOf(
                escapeCsvField(e.title),
                e.date.toString(),
                escapeCsvField(e.category),
                escapeCsvField(e.note),
                e.colorHex ?: "",
                e.repeatType,
                e.remindDaysBefore.toString(),
                e.reminderTimeMinutesOfDay.toString(),
                e.remindEnabled.toString(),
                e.syncToScheduleEnabled.toString(),
                e.createdAt.toString(),
                e.isLunar.toString()
            ).joinToString(",")
        )
    }
    return lines.joinToString("\n")
}

fun List<Event>.toPlainTextListString(daysLeftLabel: String, daysPastLabel: String, daysUnit: String): String {
    val today = java.time.LocalDate.now()
    return this.sortedBy { it.date }.map { e ->
        val targetDate = eventDateToLocalDate(e.date)
        val hasStarted = !targetDate.isAfter(today)
        val nextTargetDate = when {
            e.repeatType == REPEAT_YEARLY && e.isLunar && hasStarted -> getNextLunarOccurrence(targetDate, today)
            e.repeatType != REPEAT_NONE && hasStarted -> nextOccurrenceDate(targetDate, today, e.repeatType)
            else -> targetDate
        }

        val days = java.time.temporal.ChronoUnit.DAYS.between(today, nextTargetDate).toInt()
        val (label, count) = when {
            days > 0 -> daysLeftLabel to days
            days < 0 -> daysPastLabel to -days
            else -> "" to 0
        }
        if (count == 0) "${e.title}: $daysLeftLabel 0 $daysUnit" else "${e.title}: $label $count $daysUnit"
    }.joinToString("\n")
}
