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

fun parseEventsFromJson(json: String): List<Event> {
    val list = mutableListOf<Event>()
    try {
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val repeatType = normalizeRepeatType(o.optString("repeatType", REPEAT_NONE))
            val category = o.optString("category", "")
            list.add(
                Event(
                    id = 0,
                    title = o.optString("title", ""),
                    date = o.optLong("date", System.currentTimeMillis()),
                    category = category,
                    note = o.optString("note", ""),
                    colorHex = if (o.isNull("colorHex")) null else o.optString("colorHex"),
                    repeatType = repeatType,
                    remindDaysBefore = sanitizeRemindDaysBefore(o.optInt("remindDaysBefore", 0)),
                    reminderTimeMinutesOfDay = sanitizeReminderTimeMinutesOfDay(o.optInt("reminderTimeMinutesOfDay", 480)),
                    remindEnabled = o.optBoolean("remindEnabled", false),
                    syncToScheduleEnabled = o.optBoolean("syncToScheduleEnabled", true),
                    scheduleEventId = null,
                    targetCalendarId = null,
                    lastScheduleSyncAt = null,
                    lastScheduleSyncError = null,
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    isLunar = o.optBoolean("isLunar", false)
                )
            )
        }
    } catch (_: Exception) {
    }
    return list
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
    val header = "title,date,category,note,repeatType,remindEnabled"
    val lines = mutableListOf(header)
    for (e in this) {
        lines.add(
            listOf(
                escapeCsvField(e.title),
                e.date.toString(),
                escapeCsvField(e.category),
                escapeCsvField(e.note),
                e.repeatType,
                e.remindEnabled.toString()
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
        val (label, count) = if (days > 0) {
            daysLeftLabel to days
        } else {
            daysPastLabel to -days
        }
        "${e.title}: $label $count $daysUnit"
    }.joinToString("\n")
}
