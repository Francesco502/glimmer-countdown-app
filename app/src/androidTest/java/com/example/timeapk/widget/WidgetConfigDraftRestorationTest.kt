package com.example.timeapk.widget

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetConfigDraftRestorationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun unsavedConfigurationSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(composeRule)
        var changeDraft: (WidgetConfig) -> Unit = {}
        restoration.setContent {
            var draft by rememberSaveable(stateSaver = WidgetConfigDraftSaver) { mutableStateOf(WidgetConfig.default()) }
            changeDraft = { draft = it }
            Text(draft.toJson())
        }
        val edited = WidgetConfig.default().copy(widthCells = 5, heightCells = 1, fontScale = 1.45f, appearancePreset = APPEARANCE_SEAL)
        composeRule.runOnIdle { changeDraft(edited) }
        composeRule.onNodeWithText(edited.toJson()).assertExists()
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithText(edited.toJson()).assertExists()
    }
}
