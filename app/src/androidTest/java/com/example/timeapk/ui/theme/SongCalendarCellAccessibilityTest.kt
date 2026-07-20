package com.example.timeapk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SongCalendarCellAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectedDateExposesOneLabeledButtonActionAndSelectionState() {
        val spokenLabel = "July 20, 2026, today, selected, 2 events"
        var clicks = 0
        composeRule.setContent {
            MaterialTheme {
                SongCalendarCell(
                    dayText = "20",
                    eventIndicatorText = "2",
                    selected = true,
                    today = true,
                    hasEvents = true,
                    modifier = Modifier.semantics {
                        contentDescription = spokenLabel
                    },
                    onClick = { clicks += 1 }
                )
            }
        }

        val buttonRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        val cell = composeRule.onNodeWithContentDescription(spokenLabel, useUnmergedTree = true)
        cell.assert(buttonRole)
            .assertIsSelected()
            .assertHasClickAction()
            .performClick()
        check(
            cell.onChildren().fetchSemanticsNodes().none {
                it.config.contains(SemanticsActions.OnClick)
            }
        )

        composeRule.runOnIdle { assertEquals(1, clicks) }
    }
}
