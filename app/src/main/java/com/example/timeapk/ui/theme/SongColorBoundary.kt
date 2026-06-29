package com.example.timeapk.ui.theme

data class SongColorClassification(
    val normalizedHex: String?,
    val isRecommended: Boolean
)

object SongColorBoundary {
    private val recommendedHexes = listOf(
        "#AF4E31",
        "#86351C",
        "#457080",
        "#5B8E79",
        "#AC8F62",
        "#F5F3ED",
        "#FFFBF5",
        "#EDE8DD",
        "#1F1F1F",
        "#6A6256"
    )

    fun recommendedPresetHexes(): List<String> = recommendedHexes

    fun classify(hex: String?): SongColorClassification {
        val normalized = normalizeHex(hex)
        return SongColorClassification(
            normalizedHex = normalized,
            isRecommended = normalized != null && normalized in recommendedHexes
        )
    }

    private fun normalizeHex(hex: String?): String? {
        if (hex.isNullOrBlank()) return null
        val trimmed = hex.trim()
        val prefixed = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
        if (!Regex("^#[0-9A-Fa-f]{6}$").matches(prefixed)) return null
        return prefixed.uppercase()
    }
}
