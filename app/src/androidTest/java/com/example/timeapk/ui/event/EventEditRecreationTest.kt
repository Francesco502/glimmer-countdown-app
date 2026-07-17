package com.example.timeapk.ui.event

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timeapk.MainActivity
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class EventEditRecreationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var app: TimeApplication
    private var eventId = 0

    @Before
    fun seedEditableEvent() = runBlocking {
        app = composeRule.activity.application as TimeApplication
        val date = LocalDate.now().plusDays(7)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        eventId = app.repository.insertEvent(
            Event(
                title = SEED_TITLE,
                date = date,
                category = CATEGORY_OTHER,
                remindEnabled = false,
                syncToScheduleEnabled = false
            )
        ).toInt()
    }

    @After
    fun removeEditableEvent() = runBlocking {
        if (eventId != 0) {
            app.repository.getEvent(eventId)?.let { app.repository.deleteEvent(it) }
        }
    }

    @Test
    fun editedTitleSurvivesActivityRecreationAndBackConfirmationBeforeSaving() {
        val editDescription = composeRule.activity.getString(R.string.cd_edit)
        val saveDescription = composeRule.activity.getString(R.string.button_save_event)
        val discardTitle = composeRule.activity.getString(R.string.discard_changes_dialog_title)
        val stayLabel = composeRule.activity.getString(R.string.discard_changes_dialog_dismiss)

        composeRule.onNodeWithText(SEED_TITLE).performClick()
        composeRule.onNodeWithContentDescription(editDescription).performClick()
        composeRule.onNode(hasSetTextAction() and hasText(SEED_TITLE))
            .performTextReplacement(UPDATED_TITLE)

        composeRule.activityRule.scenario.recreate()

        composeRule.onNode(hasSetTextAction() and hasText(UPDATED_TITLE)).assertExists()
        pressBack()
        composeRule.onNodeWithText(discardTitle).assertExists()
        composeRule.onNodeWithText(stayLabel).performClick()
        composeRule.onNode(hasSetTextAction() and hasText(UPDATED_TITLE)).assertExists()
        composeRule.onNodeWithContentDescription(saveDescription).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { app.repository.getEvent(eventId)?.title == UPDATED_TITLE }
        }
        composeRule.onNodeWithContentDescription(saveDescription).assertDoesNotExist()
        composeRule.onNodeWithText(UPDATED_TITLE).assertExists()
        assertEquals(UPDATED_TITLE, runBlocking { app.repository.getEvent(eventId)?.title })
    }

    @Test
    fun editableFieldsExposeTheirVisibleLabelsToAccessibilityServices() {
        val editDescription = composeRule.activity.getString(R.string.cd_edit)
        val titleLabel = composeRule.activity.getString(R.string.field_title)
        val noteLabel = composeRule.activity.getString(R.string.field_note)

        composeRule.onNodeWithText(SEED_TITLE).performClick()
        composeRule.onNodeWithContentDescription(editDescription).performClick()

        composeRule.onNodeWithContentDescription(titleLabel).assertExists()
        composeRule.onNodeWithContentDescription(noteLabel).assertExists()
    }

    private companion object {
        const val SEED_TITLE = "E2EEdit-Recreate-Seed"
        const val UPDATED_TITLE = "E2EEdit-Recreate-Saved"
    }
}
