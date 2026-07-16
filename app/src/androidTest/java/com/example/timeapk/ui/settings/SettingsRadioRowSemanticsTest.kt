package com.example.timeapk.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRadioRowSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rowExposesOneMergedLabeledRadioAndHandlesClick() {
        var clicks = 0
        composeRule.setContent {
            MaterialTheme {
                SettingsRadioGroup {
                    SettingsRadioRow(
                        label = "Dark mode",
                        selected = true,
                        onClick = { clicks += 1 }
                    )
                }
            }
        }

        val radioRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        assertEquals(1, composeRule.onAllNodes(radioRole, useUnmergedTree = true).fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Dark mode")
            .assert(radioRole)
            .assertIsSelected()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(1, clicks) }
    }
}
