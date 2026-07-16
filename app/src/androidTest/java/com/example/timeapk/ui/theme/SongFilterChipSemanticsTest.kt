package com.example.timeapk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SongFilterChipSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectedFilterChipExposesRadioButtonSelection() {
        composeRule.setContent {
            MaterialTheme {
                SongFilterChip(selected = true, onClick = {}, label = "Seven days")
            }
        }

        composeRule.onNodeWithText("Seven days")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()
    }

    @Test
    fun actionFilterChipExposesButtonWithoutSelectionState() {
        composeRule.setContent {
            MaterialTheme {
                SongFilterChip(
                    selected = false,
                    onClick = {},
                    label = "Custom",
                    selectionRole = null
                )
            }
        }

        composeRule.onNodeWithText("Custom")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
    }
}
