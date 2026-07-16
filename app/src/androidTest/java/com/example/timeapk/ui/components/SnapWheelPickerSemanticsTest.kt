package com.example.timeapk.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapWheelPickerSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun wheelExposesCurrentValueAndCanBeAdjusted() {
        var selected = 1
        composeRule.setContent {
            MaterialTheme {
                SnapWheelPicker(
                    items = listOf(0, 1, 2),
                    selectedItem = selected,
                    onItemSelected = { selected = it },
                    accessibilityLabel = "Reminder hour",
                    itemLabel = { listOf("Zero", "One", "Two")[it] }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Reminder hour")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "One"))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(current = 1f, range = 0f..2f, steps = 1)
                )
            )
            .assertHeightIsEqualTo(240.dp)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                setProgress(2f)
            }

        composeRule.runOnIdle { assertEquals(2, selected) }
    }
}
