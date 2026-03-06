package com.example.timeapk.data

private val TAG_SPLITTER = Regex("[,，;；\\n]+")
private val TAG_COLOR_PALETTE = listOf(
    "#4F6D7A",
    "#C06C84",
    "#6C9A8B",
    "#C58C5D",
    "#7A6C9D",
    "#8E6A5B",
    "#5D8AA8",
    "#9C6B8F"
)

data class EventTag(
    val label: String,
    val normalized: String,
    val colorHex: String
)

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

fun Event.structuredTags(): List<EventTag> {
    return tagsList().map { tag ->
        val normalized = tag.lowercase()
        EventTag(
            label = tag,
            normalized = normalized,
            colorHex = tagColorHex(normalized)
        )
    }
}

fun Event.hasTag(normalizedTag: String): Boolean {
    val key = normalizedTag.trim().lowercase()
    if (key.isBlank()) return false
    return structuredTags().any { it.normalized == key }
}

private fun tagColorHex(normalizedTag: String): String {
    if (normalizedTag.isBlank()) return TAG_COLOR_PALETTE.first()
    val hash = normalizedTag.hashCode()
    val safeHash = if (hash == Int.MIN_VALUE) 0 else kotlin.math.abs(hash)
    return TAG_COLOR_PALETTE[safeHash % TAG_COLOR_PALETTE.size]
}
