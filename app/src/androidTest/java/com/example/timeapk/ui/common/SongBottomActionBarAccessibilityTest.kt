package com.example.timeapk.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timeapk.ui.theme.SongLineIconKind
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SongBottomActionBarAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun eachActionExposesOneLabeledButtonAndOneClick() {
        var editClicks = 0
        composeRule.setContent {
            MaterialTheme {
                SongBottomActionBar(
                    actions = listOf(
                        SongBottomAction(
                            label = "Edit",
                            icon = SongLineIconKind.Edit,
                            onClick = { editClicks += 1 }
                        ),
                        SongBottomAction(
                            label = "Delete",
                            icon = SongLineIconKind.Delete,
                            onClick = {}
                        )
                    )
                )
            }
        }

        val editMatcher = hasContentDescription("Edit")
        val deleteMatcher = hasContentDescription("Delete")
        val buttonRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        assertEquals(1, composeRule.onAllNodes(editMatcher, useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals(1, composeRule.onAllNodes(deleteMatcher, useUnmergedTree = true).fetchSemanticsNodes().size)
        composeRule.onNode(editMatcher)
            .assert(buttonRole)
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, editClicks) }
    }
}
