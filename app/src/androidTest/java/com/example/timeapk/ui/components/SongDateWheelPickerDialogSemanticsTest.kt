package com.example.timeapk.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class SongDateWheelPickerDialogSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dateDialogExposesAdjustableWheelsAndConfirmsTheirValues() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val initialDate = LocalDate.of(2026, 7, 17)
        var confirmedDate: LocalDate? = null
        var confirmedAsLunar = true

        composeRule.setContent {
            MaterialTheme {
                SongDateWheelPickerDialog(
                    initialDateMillis = initialDate.toEpochMillis(),
                    onDismissRequest = {},
                    onConfirm = { millis, isLunar ->
                        confirmedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        confirmedAsLunar = isLunar
                    },
                    title = context.getString(R.string.field_date)
                )
            }
        }

        assertWheel(
            label = context.getString(R.string.date_part_year),
            stateDescription = "2026",
            currentIndex = 126f,
            lastIndex = 200f,
            steps = 199
        )
        assertWheel(
            label = context.getString(R.string.date_part_month),
            stateDescription = "07",
            currentIndex = 6f,
            lastIndex = 11f,
            steps = 10
        )
        assertWheel(
            label = context.getString(R.string.date_part_day),
            stateDescription = "17",
            currentIndex = 16f,
            lastIndex = 30f,
            steps = 29
        )

        setWheelProgress(context.getString(R.string.date_part_year), 127f)
        setWheelProgress(context.getString(R.string.date_part_month), 7f)
        setWheelProgress(context.getString(R.string.date_part_day), 17f)

        composeRule.onNodeWithContentDescription(context.getString(R.string.date_part_year))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "2027"))
        composeRule.onNodeWithContentDescription(context.getString(R.string.date_part_month))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "08"))
        composeRule.onNodeWithContentDescription(context.getString(R.string.date_part_day))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "18"))

        composeRule.onNodeWithText(context.getString(R.string.date_picker_ok)).performClick()

        composeRule.runOnIdle {
            assertNotNull(confirmedDate)
            assertEquals(LocalDate.of(2027, 8, 18), confirmedDate)
            assertFalse(confirmedAsLunar)
        }
    }

    private fun assertWheel(
        label: String,
        stateDescription: String,
        currentIndex: Float,
        lastIndex: Float,
        steps: Int
    ) {
        composeRule.onNodeWithContentDescription(label)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, stateDescription))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(currentIndex, 0f..lastIndex, steps)
                )
            )
    }

    private fun setWheelProgress(label: String, progress: Float) {
        composeRule.onNodeWithContentDescription(label)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                setProgress(progress)
            }
        composeRule.waitForIdle()
    }

    private fun LocalDate.toEpochMillis(): Long =
        atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
