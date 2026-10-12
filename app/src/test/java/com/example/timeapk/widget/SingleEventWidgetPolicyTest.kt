package com.example.timeapk.widget

import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.ui.home.EventUiState
import com.example.timeapk.ui.settings.WidgetPreviewSizes
import com.example.timeapk.ui.settings.widgetAppearanceSupportsOpacity
import com.example.timeapk.ui.settings.widgetPreviewRowCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SingleEventWidgetPolicyTest {
    private val today = LocalDate.of(2026, 10, 12)

    private fun state(id: Int, next: LocalDate, past: Boolean = false) = EventUiState(
        event = Event(id = id, title = "e$id", date = 0L, category = CATEGORY_OTHER),
        daysRemaining = 0,
        isPast = past,
        nextOccurrenceDate = next
    )

    @Test
    fun selectedEventWins_deletedSelectionFallsBackToNearest() {
        val events = listOf(state(1, today.plusDays(9)), state(2, today.plusDays(3)))
        assertEquals(1, SingleEventWidgetPolicy.resolveTarget(events, 1, today, emptyList())?.event?.id)
        assertEquals(2, SingleEventWidgetPolicy.resolveTarget(events, 99, today, emptyList())?.event?.id)
        assertEquals(
            2,
            SingleEventWidgetPolicy.resolveTarget(events, SINGLE_EVENT_FOLLOW_NEAREST, today, emptyList())?.event?.id
        )
    }

    @Test
    fun onlyPastEvents_showTheMostRecentOne() {
        val events = listOf(state(1, today.minusDays(40), past = true), state(2, today.minusDays(2), past = true))
        assertEquals(2, SingleEventWidgetPolicy.resolveTarget(events, SINGLE_EVENT_FOLLOW_NEAREST, today, emptyList())?.event?.id)
        assertNull(SingleEventWidgetPolicy.resolveTarget(emptyList(), SINGLE_EVENT_FOLLOW_NEAREST, today, emptyList()))
    }

    @Test
    fun numberSizeGrowsWithWidthAndDateHidesOnTinyWidgets() {
        assertTrue(SingleEventWidgetPolicy.numberTextSp(60, 60) < SingleEventWidgetPolicy.numberTextSp(180, 180))
        assertTrue(SingleEventWidgetPolicy.numberTextSp(180, 180) < SingleEventWidgetPolicy.numberTextSp(300, 180))
        assertFalse(SingleEventWidgetPolicy.showsDate(60, 60))
        assertTrue(SingleEventWidgetPolicy.showsDate(180, 180))
    }

    @Test
    fun selections_roundTripThroughJson() {
        val encoded = encodeSingleEventSelections(mapOf(12 to 5, 13 to SINGLE_EVENT_FOLLOW_NEAREST))
        assertEquals(mapOf(12 to 5, 13 to SINGLE_EVENT_FOLLOW_NEAREST), decodeSingleEventSelections(encoded))
        assertTrue(decodeSingleEventSelections("not json").isEmpty())
    }

    @Test
    fun singleEventLayoutsFollowCornerAndTransparency() {
        assertEquals(
            com.example.timeapk.R.layout.widget_single_event_transparent,
            WidgetRenderPolicy.resolveSingleEventLayoutResId(WidgetConfig(appearancePreset = APPEARANCE_TRANSPARENT))
        )
        assertEquals(
            com.example.timeapk.R.layout.widget_single_event_corner_large,
            WidgetRenderPolicy.resolveSingleEventLayoutResId(WidgetConfig(cornerMode = CORNER_LARGE))
        )
    }

    @Test
    fun previewHelpers_matchRemoteViewsBehaviour() {
        assertTrue(widgetAppearanceSupportsOpacity(APPEARANCE_SYSTEM))
        assertTrue(widgetAppearanceSupportsOpacity(APPEARANCE_TRANSLUCENT))
        assertFalse(widgetAppearanceSupportsOpacity(APPEARANCE_SEAL))
        assertFalse(widgetAppearanceSupportsOpacity(APPEARANCE_SOLID))
        assertEquals(4, widgetPreviewRowCount(2))
        assertEquals(1, widgetPreviewRowCount(0))
        assertTrue((2 to 2) in WidgetPreviewSizes)
    }
}
