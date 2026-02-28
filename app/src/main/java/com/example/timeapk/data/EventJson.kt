package com.example.timeapk.data

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
            list.add(
                Event(
                    id = 0,
                    title = o.optString("title", ""),
                    date = o.optLong("date", System.currentTimeMillis()),
                    category = o.optString("category", ""),
                    note = o.optString("note", ""),
                    colorHex = if (o.isNull("colorHex")) null else o.optString("colorHex"),
                    repeatType = o.optString("repeatType", REPEAT_NONE),
                    remindDaysBefore = o.optInt("remindDaysBefore", 0),
                    reminderTimeMinutesOfDay = o.optInt("reminderTimeMinutesOfDay", 480),
                    remindEnabled = o.optBoolean("remindEnabled", false),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    isLunar = o.optBoolean("isLunar", false)
                )
            )
        }
    } catch (_: Exception) { }
    return list
}

private fun escapeCsvField(s: String): String {
    return if (s.contains(',') || s.contains('"') || s.contains('\n')) {
        "\"" + s.replace("\"", "\"\"") + "\""
    } else s
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
        val targetDate = com.example.timeapk.ui.utils.eventDateToLocalDate(e.date)
        val hasStarted = !targetDate.isAfter(today)
        var nextTargetDate = targetDate

        when (e.repeatType) {
            REPEAT_YEARLY -> {
                if (hasStarted) {
                    val currentYear = safeWithYearForExport(targetDate, today.year)
                    if (currentYear != null && currentYear.isBefore(today)) {
                        nextTargetDate = safeWithYearForExport(targetDate, today.year + 1) ?: targetDate
                    } else if (currentYear != null) {
                        nextTargetDate = currentYear
                    }
                }
            }
            REPEAT_HALF_YEARLY -> {
                var next = targetDate
                while (next.isBefore(today)) {
                    next = next.plusMonths(6)
                }
                nextTargetDate = next
            }
            REPEAT_MONTHLY -> {
                var next = targetDate
                while (next.isBefore(today)) {
                    next = next.plusMonths(1)
                }
                nextTargetDate = next
            }
            else -> { /* REPEAT_NONE */ }
        }

        val days = java.time.temporal.ChronoUnit.DAYS.between(today, nextTargetDate).toInt()
        val (label, count) = if (days > 0) daysLeftLabel to days else daysPastLabel to -days
        "${e.title}: $label $count $daysUnit"
    }.joinToString("\n")
}

private fun safeWithYearForExport(date: java.time.LocalDate, year: Int): java.time.LocalDate? {
    return try {
        date.withYear(year)
    } catch (_: Exception) {
        try {
            java.time.LocalDate.of(year, date.monthValue, 28)
        } catch (_: Exception) {
            null
        }
    }
}
