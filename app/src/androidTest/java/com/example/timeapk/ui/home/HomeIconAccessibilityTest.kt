package com.example.timeapk.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timeapk.ui.theme.SongLineIconKind
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeIconAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun toolbarIconExposesOneLabeled48DpButton() {
        var clicks = 0
        val label = "Filters and sorting"
        composeRule.setContent {
            MaterialTheme {
                InlineActionIconButton(
                    icon = SongLineIconKind.Filter,
                    contentDescription = label,
                    onClick = { clicks += 1 }
                )
            }
        }

        val labelMatcher = hasContentDescription(label)
        val buttonRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        assertEquals(
            1,
            composeRule.onAllNodes(labelMatcher, useUnmergedTree = true).fetchSemanticsNodes().size
        )
        composeRule.onNode(labelMatcher)
            .assert(buttonRole)
            .assertHasClickAction()
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(1, clicks) }
    }
}
