package com.example.timeapk.ui.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.R
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.ui.home.EventUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class DetailHeroTimeAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cyclingTimeDisplayAnnouncesValueModeAndAction() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val actionLabel = context.getString(R.string.cd_toggle_date_delta_display)
        val targetDate = LocalDate.now().plusDays(10)
        val eventState = EventUiState(
            event = Event(
                id = 918273,
                title = "Accessible countdown",
                date = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                category = CATEGORY_OTHER,
                repeatType = REPEAT_NONE,
                syncToScheduleEnabled = false
            ),
            daysRemaining = 10,
            daysLeft = 10,
            isPast = false,
            nextOccurrenceDate = targetDate
        )
        composeRule.setContent {
            MaterialTheme {
                DetailScreen(
                    eventState = eventState,
                    onNavigateBack = {},
                    onEditClick = {},
                    onDeleteClick = { true }
                )
            }
        }

        val labeledCycleAction = SemanticsMatcher("time display cycle action label") { node ->
            node.config.contains(SemanticsActions.OnClick) &&
                node.config[SemanticsActions.OnClick].label == actionLabel
        }
        val nonEmptyDescription = SemanticsMatcher("non-empty time display description") { node ->
            node.config.contains(SemanticsProperties.ContentDescription) &&
                node.config[SemanticsProperties.ContentDescription].any(String::isNotBlank)
        }
        val nonEmptyMode = SemanticsMatcher("non-empty current time display mode") { node ->
            node.config.contains(SemanticsProperties.StateDescription) &&
                node.config[SemanticsProperties.StateDescription].isNotBlank()
        }

        composeRule.onNode(labeledCycleAction)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(nonEmptyDescription)
            .assert(nonEmptyMode)
            .assertHasClickAction()
    }
}
