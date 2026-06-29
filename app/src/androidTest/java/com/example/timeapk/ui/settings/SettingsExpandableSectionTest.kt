package com.example.timeapk.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsExpandableSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun collapsedSectionExpandsContentWhenHeaderIsClicked() {
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
        composeRule.onNodeWithText("Bundled serif choices").assertDoesNotExist()

        composeRule.onNodeWithText("Font").performClick()

        composeRule.onNodeWithText("Bundled serif choices").assertIsDisplayed()
    }
}
