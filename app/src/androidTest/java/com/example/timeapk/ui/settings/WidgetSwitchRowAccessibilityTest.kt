package com.example.timeapk.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetSwitchRowAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lunarPrefixSettingExposesExactlyOneSwitchAction() {
        val title = "Show lunar prefix"
        var checked by mutableStateOf(true)
        composeRule.setContent {
            MaterialTheme {
                WidgetSwitchRow(
                    title = title,
                    checked = checked,
                    onCheckedChange = { checked = it }
                )
            }
        }

        val switchRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
        assertEquals(
            1,
            composeRule.onAllNodes(switchRole, useUnmergedTree = true).fetchSemanticsNodes().size
        )
        val row = composeRule.onNodeWithText(title)
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
        check(
            row.onChildren().fetchSemanticsNodes().none {
                it.config.contains(SemanticsActions.OnClick)
            }
        )

        row.performClick()
        composeRule.runOnIdle {
            check(!checked)
        }
    }
}
