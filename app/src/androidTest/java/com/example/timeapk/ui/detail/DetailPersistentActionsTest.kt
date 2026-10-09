package com.example.timeapk.ui.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.R
import com.example.timeapk.data.Event
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.ui.home.EventUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class DetailPersistentActionsTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun editAndShareRemainVisibleBeforeScrollingPastALongNote() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tomorrow = LocalDate.now().plusDays(1)
        val event = EventUiState(
            event = Event(
                id = 981735,
                title = "Long note detail",
                category = CATEGORY_OTHER,
                date = tomorrow.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                note = (1..150).joinToString("\n") { "Long note line $it" },
                remindEnabled = false
            ),
            daysRemaining = 1,
            isPast = false,
            daysLeft = 1,
            nextOccurrenceDate = tomorrow
        )
        var editClicks = 0
        composeRule.setContent {
            MaterialTheme {
                DetailScreen(eventState = event, onNavigateBack = {}, onEditClick = { editClicks += 1 }, onDeleteClick = { true })
            }
        }
        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_edit)).assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, editClicks) }
        composeRule.onNodeWithContentDescription(context.getString(R.string.button_share)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText(context.getString(R.string.share_card_title)).assertIsDisplayed()
    }
}
