package com.example.timeapk.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
