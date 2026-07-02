package com.example.timeapk.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeInteractionPolicyTest {

    @Test
    fun homeCardLongPressEditEnabled_isDisabledWhenCustomSorting() {
        assertFalse(homeCardLongPressEditEnabled(SortType.Custom))
    }

    @Test
    fun homeCardLongPressEditEnabled_isDisabledAcrossHomeSortModes() {
        assertFalse(homeCardLongPressEditEnabled(SortType.ByDays))
        assertFalse(homeCardLongPressEditEnabled(SortType.ByDate))
    }

    @Test
    fun homeCardUsesTapOnlyInteraction_isDisabledAcrossHomeSortModes() {
        assertFalse(homeCardUsesTapOnlyInteraction(SortType.Custom))
        assertFalse(homeCardUsesTapOnlyInteraction(SortType.ByDays))
        assertFalse(homeCardUsesTapOnlyInteraction(SortType.ByDate))
    }

    @Test
    fun homeCardTapNavigationEnabled_isEnabledAcrossHomeSortModes() {
        assertTrue(homeCardTapNavigationEnabled(SortType.Custom))
        assertTrue(homeCardTapNavigationEnabled(SortType.ByDays))
        assertTrue(homeCardTapNavigationEnabled(SortType.ByDate))
    }

    @Test
    fun homeCardDragSortEnabled_isEnabledOnlyForCustomSort() {
        assertTrue(homeCardDragSortEnabled(SortType.Custom))
        assertFalse(homeCardDragSortEnabled(SortType.ByDays))
        assertFalse(homeCardDragSortEnabled(SortType.ByDate))
    }

    @Test
    fun homeUsesListLevelReorderDetection_isDisabledAcrossHomeSortModes() {
        assertFalse(homeUsesListLevelReorderDetection(SortType.Custom))
        assertFalse(homeUsesListLevelReorderDetection(SortType.ByDays))
        assertFalse(homeUsesListLevelReorderDetection(SortType.ByDate))
    }

    @Test
    fun homeScreenRendersTimelineDigestAndMonthHighlights() {
        val source = readSource("ui/home/HomeScreen.kt")

        assertTrue(source.contains("HomeTimelineDigestRow("))
        assertTrue(source.contains("MonthHighlightsSection("))
        assertTrue(source.contains("buildHomeTimelineDigest("))
        assertTrue(source.contains("filterEventsForTimelineBucket("))
        assertTrue(source.contains("monthHighlightsForOccurrences("))
    }

    private fun readSource(relative: String): String {
        val direct = File("src/main/java/com/example/timeapk/$relative")
        if (direct.exists()) {
            return direct.readText(Charsets.UTF_8)
        }
        val fromRoot = File("app/src/main/java/com/example/timeapk/$relative")
        require(fromRoot.exists()) { "Missing source file: $relative" }
        return fromRoot.readText(Charsets.UTF_8)
    }
}
