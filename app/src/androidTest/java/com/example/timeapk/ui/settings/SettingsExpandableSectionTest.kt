package com.example.timeapk.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsExpandableSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun collapsedSectionExpandsContentWhenHeaderIsClicked() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val collapsedState = context.getString(R.string.settings_section_state_collapsed)
        val expandedState = context.getString(R.string.settings_section_state_expanded)
        composeRule.setContent {
            MaterialTheme {
                SettingsExpandableSection(
                    title = "Font",
                    summary = "Choose app typography"
                ) {
                    Text("Bundled serif choices")
                }
            }
        }

        composeRule.onNodeWithText("Font").assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, collapsedState))
        composeRule.onNodeWithText("Bundled serif choices").assertDoesNotExist()

        composeRule.onNodeWithText("Font").performClick()

        composeRule.onNodeWithText("Font")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expandedState))
        composeRule.onNodeWithText("Bundled serif choices").assertIsDisplayed()
    }

    @Test
    fun toggleExposesSwitchRoleAndUpdatesSpokenState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val onState = context.getString(R.string.toggle_on)
        val offState = context.getString(R.string.toggle_off)
        val checked = mutableStateOf(true)
        composeRule.setContent {
            MaterialTheme {
                SongToggle(
                    checked = checked.value,
                    onCheckedChange = { checked.value = it }
                )
            }
        }

        val switchRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
        composeRule.onNode(switchRole)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, onState))
            .performClick()

        composeRule.onNode(switchRole)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, offState))
    }
}
