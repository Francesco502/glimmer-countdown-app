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
                    category = o.optString("category", "其他"),
                    note = o.optString("note", ""),
                    colorHex = if (o.isNull("colorHex")) null else o.optString("colorHex"),
                    repeatType = o.optString("repeatType", REPEAT_NONE),
                    remindDaysBefore = o.optInt("remindDaysBefore", 0),
                    reminderTimeMinutesOfDay = o.optInt("reminderTimeMinutesOfDay", 480),
                    remindEnabled = o.optBoolean("remindEnabled", false),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
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
    val now = System.currentTimeMillis()
    val dayMs = 24L * 60 * 60 * 1000
    return this.sortedBy { it.date }.map { e ->
        val days = ((e.date - now) / dayMs).toInt()
        val (label, count) = if (days >= 0) daysLeftLabel to days else daysPastLabel to -days
        "${e.title}: $label $count $daysUnit"
    }.joinToString("\n")
}
