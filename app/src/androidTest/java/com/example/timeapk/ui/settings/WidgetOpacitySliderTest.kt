package com.example.timeapk.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.example.timeapk.ui.theme.TimeAPKTheme
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetOpacitySliderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun continuousSliderSavesIntermediateValuesAndBothEndpoints() {
        val value = mutableStateOf(75)
        compose.setContent {
            TimeAPKTheme {
                Column {
                    WidgetOpacityControl(value.value) { value.value = it }
                }
            }
        }
        val slider = compose.onNode(SemanticsMatcher("Supports progress changes") {
            it.config.contains(SemanticsActions.SetProgress)
        })
        slider.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(75f, 0f..100f, 0)
            )
        )
        listOf(37, 68, 0, 100).forEach { percent ->
            slider.performSemanticsAction(SemanticsActions.SetProgress) { it(percent.toFloat()) }
            compose.runOnIdle { assertEquals(percent, value.value) }
            compose.onNodeWithText("$percent%").assertExists()
        }
    }
}
