package com.example.timeapk.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timeapk.data.Event
import com.example.timeapk.data.CATEGORY_OTHER
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class HomeMonthCompactHeightTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun selectedEventIsReachableBelowASixWeekGridInAShortWindow() {
        val selectedDate = LocalDate.of(2026, 3, 15)
        val event = EventUiState(
            event = Event(
                id = 981736,
                title = "Six-week calendar event",
                category = CATEGORY_OTHER,
                date = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            ),
            daysRemaining = 0,
            isPast = false,
            nextOccurrenceDate = selectedDate
        )
        var clickedId = 0
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.width(360.dp).height(240.dp)) {
                    MonthCalendarView(
                        events = listOf(event),
                        selectedDate = selectedDate,
                        onEventClick = { clickedId = it },
                        onEventLongClick = null
                    )
                }
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(event.event.title))
        composeRule.onNodeWithText(event.event.title).assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(event.event.id, clickedId) }
    }
}
