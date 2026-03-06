package com.example.timeapk.data

private val TAG_SPLITTER = Regex("[,，;；\\n]+")

fun parseTags(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    return raw
        .split(TAG_SPLITTER)
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

fun normalizeTags(raw: String): String {
    val seen = linkedSetOf<String>()
    parseTags(raw).forEach { tag ->
        val dedupeKey = tag.lowercase()
        if (seen.none { it.lowercase() == dedupeKey }) {
            seen += tag
        }
    }
    return seen.joinToString(", ")
}

fun Event.tagsList(): List<String> = parseTags(tags)
