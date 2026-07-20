package com.example.timeapk.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timeapk.MainActivity
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class HomeFilteredReorderGestureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var app: TimeApplication
    private var dragAId = 0
    private var hiddenId = 0
    private var dragBId = 0
    private var dragCId = 0
    private var originalSort = 0
    private var originalFilter = 0
    private var originalDisplayMode = 0
    private var originalOrder = emptyList<Int>()

    @Before
    fun seedFilteredReorderScenario() = runBlocking {
        app = composeRule.activity.application as TimeApplication
        originalSort = app.userPrefs.sortTypeFlow.first()
        originalFilter = app.userPrefs.filterTypeFlow.first()
        originalDisplayMode = app.userPrefs.homeDisplayModeFlow.first()
        originalOrder = app.userPrefs.customEventOrderFlow.first()

        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        dragAId = app.repository.insertEvent(testEvent("E2EDrag-A", today, 30L)).toInt()
        hiddenId = app.repository.insertEvent(testEvent("E2EHidden", today, 20L)).toInt()
        dragBId = app.repository.insertEvent(testEvent("E2EDrag-B", today, 10L)).toInt()
        dragCId = app.repository.insertEvent(testEvent("E2EDrag-C", today, 5L)).toInt()
        app.userPrefs.setFilterType(FilterType.All.ordinal)
        app.userPrefs.setSortType(SortType.ByDays.ordinal)
        app.userPrefs.setHomeDisplayMode(0)
        app.userPrefs.setCustomEventOrder(listOf(dragAId, hiddenId, dragBId, dragCId) + originalOrder)
    }

    @After
    fun restoreAppState() = runBlocking {
        listOf(dragAId, hiddenId, dragBId, dragCId).filter { it != 0 }.forEach { id ->
            app.repository.getEvent(id)?.let { app.repository.deleteEvent(it) }
        }
        app.userPrefs.setCustomEventOrder(originalOrder)
        app.userPrefs.setSortType(originalSort)
        app.userPrefs.setFilterType(originalFilter)
        app.userPrefs.setHomeDisplayMode(originalDisplayMode)
    }

    @Test
    fun draggingWithinSearchSubsetPreservesHiddenGlobalSlot() {
        val homeToolsDescription = composeRule.activity.getString(R.string.home_tools_action)
        val customSortLabel = composeRule.activity.getString(R.string.sort_by_created)
        composeRule.onNodeWithText("E2EDrag-A").assertExists()
        composeRule.onNodeWithContentDescription(homeToolsDescription).performClick()
        composeRule.onNodeWithText(customSortLabel).performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { app.userPrefs.sortTypeFlow.first() } == SortType.Custom.ordinal
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithContentDescription(homeToolsDescription).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size == 1
        }
        composeRule.onNode(hasSetTextAction()).performTextReplacement("E2EDrag")
        composeRule.onNodeWithContentDescription(homeToolsDescription).performClick()

        composeRule.onNodeWithText("E2EHidden").assertDoesNotExist()
        val secondCenterY = composeRule.onNodeWithText("E2EDrag-B")
            .fetchSemanticsNode().boundsInRoot.center.y
        val thirdCenterY = composeRule.onNodeWithText("E2EDrag-C")
            .fetchSemanticsNode().boundsInRoot.center.y
        val downwardDistance = thirdCenterY - secondCenterY

        composeRule.onNodeWithText("E2EDrag-B").performTouchInput {
            down(center)
            advanceEventTime(750)
            moveBy(Offset(0f, downwardDistance / 2f), 250)
            moveBy(Offset(0f, downwardDistance / 2f + 120f), 250)
            up()
        }
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking {
                app.userPrefs.customEventOrderFlow.first().take(4) ==
                    listOf(dragAId, hiddenId, dragCId, dragBId)
            }
        }

        assertEquals(
            listOf(dragAId, hiddenId, dragCId, dragBId),
            runBlocking { app.userPrefs.customEventOrderFlow.first().take(4) }
        )
    }

    private fun testEvent(title: String, date: Long, createdAt: Long) = Event(
        title = title,
        date = date,
        category = CATEGORY_OTHER,
        remindEnabled = false,
        syncToScheduleEnabled = false,
        createdAt = createdAt
    )
}
