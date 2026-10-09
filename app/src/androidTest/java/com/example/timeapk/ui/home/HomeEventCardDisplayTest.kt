package com.example.timeapk.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.R
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.ui.captureComponentUiEvidence
import com.example.timeapk.ui.utils.DisplayModes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class HomeEventCardDisplayTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun hourPreferenceChangesTheVisibleValueAndKeepsNarrowLargeTextActionsReachable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val event = EventUiState(
            event = Event(
                id = 981734,
                title = "A longer countdown title",
                category = CATEGORY_OTHER,
                date = tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            ),
            daysRemaining = 1,
            isPast = false,
            daysLeft = 1,
            hoursRemaining = 13,
            nextOccurrenceDate = tomorrow
        )
        var showHours by mutableStateOf(true)
        var timeClicks = 0
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                MaterialTheme {
                    Box(Modifier.width(320.dp)) {
                        EventCard(
                            eventState = event,
                            today = today,
                            dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE,
                            sortType = SortType.Custom,
                            dateDeltaDisplayMode = DisplayModes.UNTIL_DAYS,
                            onToggleDateDeltaDisplayMode = { timeClicks += 1 },
                            onClick = {},
                            onLongClick = null,
                            showHours = showHours
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("13", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText(context.resources.getQuantityString(R.plurals.hours_unit, 13), useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText(event.event.title, useUnmergedTree = true).assertIsDisplayed()
        captureComponentUiEvidence(
            composeRule, "home-card-narrow-large-text",
            "Synthetic future event; card width 320dp; Compose font scale 1.6; hours enabled and 13 hours visible"
        )
        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_toggle_date_delta_display))
            .assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, timeClicks); showHours = false }
        composeRule.onNodeWithText("13", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.days_unit), useUnmergedTree = true).assertIsDisplayed()
    }
}
