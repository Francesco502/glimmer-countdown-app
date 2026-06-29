package com.example.timeapk.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongColorBoundaryTest {
    @Test
    fun recommendedPresetHexes_areLimitedToSongSemanticPalette() {
        assertEquals(
            listOf(
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
            ),
            SongColorBoundary.recommendedPresetHexes()
        )
    }

    @Test
    fun classify_rejectsHighChromaColorsOutsideSongBoundary() {
        assertTrue(SongColorBoundary.classify("#AF4E31").isRecommended)
        assertFalse(SongColorBoundary.classify("#FF00FF").isRecommended)
    }
}
