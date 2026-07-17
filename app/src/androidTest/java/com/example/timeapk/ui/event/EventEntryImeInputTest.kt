package com.example.timeapk.ui.event

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.MainActivity
import com.example.timeapk.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventEntryImeInputTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun composingCommitAndHardwareKeySurviveRecreationAndBack() {
        val addDescription = composeRule.activity.getString(R.string.cd_add_event)
        val titleLabel = composeRule.activity.getString(R.string.field_title)
        val discardTitle = composeRule.activity.getString(R.string.discard_changes_dialog_title)

        composeRule.onNodeWithContentDescription(addDescription).performClick()
        composeRule.onNodeWithContentDescription(titleLabel).performClick()
        composeRule.waitForIdle()

        val composingConnection = focusedInputConnection()
        composeRule.runOnUiThread {
            assertTrue(composingConnection.setComposingText(PINYIN_COMPOSING_TEXT, 1))
            assertTrue(composingConnection.commitText(COMMITTED_CHINESE_TEXT, 1))
        }
        composeRule.waitForIdle()
        assertEquals(
            AnnotatedString(COMMITTED_CHINESE_TEXT),
            composeRule.onNodeWithContentDescription(titleLabel)
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
        )

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_1)
        composeRule.waitForIdle()
        val actualTitle = composeRule.onNodeWithContentDescription(titleLabel)
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
        assertEquals(AnnotatedString(EXPECTED_TITLE), actualTitle)

        composeRule.activityRule.scenario.recreate()
        composeRule.onNode(hasSetTextAction() and hasText(EXPECTED_TITLE)).assertExists()

        pressBack()
        composeRule.waitForIdle()
        if (composeRule.onAllNodes(hasText(discardTitle)).fetchSemanticsNodes().isEmpty()) {
            composeRule.onNode(hasSetTextAction() and hasText(EXPECTED_TITLE)).assertExists()
            pressBack()
        }
        composeRule.onNodeWithText(discardTitle).assertExists()
        composeRule.onNode(hasSetTextAction() and hasText(EXPECTED_TITLE)).assertExists()
    }

    @Test
    fun composingTextAlreadyDeliveredByImeSurvivesActivityRecreationAsDraft() {
        val addDescription = composeRule.activity.getString(R.string.cd_add_event)
        val titleLabel = composeRule.activity.getString(R.string.field_title)

        composeRule.onNodeWithContentDescription(addDescription).performClick()
        composeRule.onNodeWithContentDescription(titleLabel).performClick()
        composeRule.waitForIdle()

        val inputConnection = focusedInputConnection()
        composeRule.runOnUiThread {
            assertTrue(inputConnection.setComposingText(PINYIN_COMPOSING_TEXT, 1))
        }
        composeRule.onNode(hasSetTextAction() and hasText(PINYIN_COMPOSING_TEXT)).assertExists()

        composeRule.activityRule.scenario.recreate()
        composeRule.onNode(hasSetTextAction() and hasText(PINYIN_COMPOSING_TEXT)).assertExists()
    }

    private fun focusedInputConnection(): InputConnection {
        var connection: InputConnection? = null
        composeRule.runOnUiThread {
            val focusedView = composeRule.activity.currentFocus
            assertNotNull("The title field should own Android input focus", focusedView)
            connection = focusedView?.onCreateInputConnection(EditorInfo())
        }
        return requireNotNull(connection) { "The focused Compose field did not expose an InputConnection" }
    }

    private companion object {
        const val PINYIN_COMPOSING_TEXT = "pin"
        const val COMMITTED_CHINESE_TEXT = "拼"
        const val EXPECTED_TITLE = "拼1"
    }
}
