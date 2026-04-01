package com.example.timeapk.widget

import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.ui.home.toEventUiState
import com.example.timeapk.ui.utils.DisplayModes
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class WidgetContentResolverTest {

    @Test
    fun resolveDisplayMode_futureEventFallsBackToUntilMode() {
        val futureEvent = Event(
            title = "future",
            date = epochMillisOf(LocalDate.now().plusDays(10)),
            category = CATEGORY_OTHER,
            repeatType = REPEAT_NONE
        )

        val resolved = WidgetContentResolver.resolveDisplayMode(
            state = futureEvent.toEventUiState(),
            preferredMode = DisplayModes.PAST_DAYS,
            showMilestone = true
        )

        assertEquals(DisplayModes.UNTIL_DAYS, resolved)
    }

    @Test
    fun resolveDisplayMode_dropsMilestoneWhenMilestoneIsHidden() {
        val futureEvent = Event(
            title = "future",
            date = epochMillisOf(LocalDate.now().plusDays(10)),
            category = CATEGORY_OTHER,
            repeatType = REPEAT_NONE
        )

        val resolved = WidgetContentResolver.resolveDisplayMode(
            state = futureEvent.toEventUiState(),
            preferredMode = DisplayModes.MILESTONE,
            showMilestone = false
        )

        assertEquals(DisplayModes.UNTIL_DAYS, resolved)
    }

    private fun epochMillisOf(localDate: LocalDate): Long {
        return localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
}
