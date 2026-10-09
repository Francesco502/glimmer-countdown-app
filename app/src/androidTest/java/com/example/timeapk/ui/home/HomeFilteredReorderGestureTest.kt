package com.example.timeapk.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timeapk.MainActivity
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class HomeFilteredReorderGestureTest {
    @get:Rule
    // The pinned library stores its edge-scroll Job after launch returns. Queued
    // effects prevent it from reading the previous Job during that assignment.
    val composeRule = createAndroidComposeRule<MainActivity>(effectContext = StandardTestDispatcher())

    private lateinit var app: TimeApplication
    private var dragAId = 0
    private var hiddenId = 0
    private var dragBId = 0
    private var dragCId = 0
    private var originalSort = 0
    private var originalFilter = 0
    private var originalDisplayMode = 0
    private var originalOrder = emptyList<Int>()
    private val additionalDragIds = mutableListOf<Int>()

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
        (listOf(dragAId, hiddenId, dragBId, dragCId) + additionalDragIds).filter { it != 0 }.forEach { id ->
            app.repository.getEvent(id)?.let { app.repository.deleteEvent(it) }
        }
        app.userPrefs.setCustomEventOrder(originalOrder)
        app.userPrefs.setSortType(originalSort)
        app.userPrefs.setFilterType(originalFilter)
        app.userPrefs.setHomeDisplayMode(originalDisplayMode)
    }

    @Test
    fun draggingWithinSearchSubsetPreservesHiddenGlobalSlot() {
        selectCustomSortAndSearchDragEvents()

        composeRule.onNodeWithText("E2EHidden").assertDoesNotExist()
        val secondCenterY = composeRule.onNodeWithText("E2EDrag-B")
            .fetchSemanticsNode().boundsInRoot.center.y
        val thirdCenterY = composeRule.onNodeWithText("E2EDrag-C")
            .fetchSemanticsNode().boundsInRoot.center.y
        val downwardDistance = thirdCenterY - secondCenterY

        beginLongPressDrag("E2EDrag-B")
        try {
            composeRule.onRoot().performTouchInput {
                moveBy(Offset(0f, downwardDistance / 2f), 250)
            }
            assertDragIsActive("E2EDrag-B")
            composeRule.onRoot().performTouchInput {
                moveBy(Offset(0f, downwardDistance / 2f + 120f), 250)
            }
            assertDragIsActive("E2EDrag-B")
        } finally {
            composeRule.onRoot().performTouchInput { up() }
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

    @Test
    fun accessibleMovePreservesHiddenGlobalSlot() {
        selectCustomSortAndSearchDragEvents()
        val moveDownLabel = composeRule.activity.getString(R.string.home_move_down)
        val actions = composeRule.onNodeWithContentDescription("E2EDrag-B", substring = true)
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        val moveDown = actions.single { it.label == moveDownLabel }
        composeRule.runOnIdle { assertTrue(moveDown.action()) }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { app.userPrefs.customEventOrderFlow.first().take(4) } ==
                listOf(dragAId, hiddenId, dragCId, dragBId)
        }
    }

    @Test
    fun holdingADragNearTheListEdgeScrollsBeyondTheInitialViewport() {
        runBlocking {
            val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            (1..12).forEach { index ->
                additionalDragIds += app.repository.insertEvent(testEvent("E2EDrag-M$index", today, -index.toLong())).toInt()
            }
            app.userPrefs.setCustomEventOrder(listOf(dragAId, hiddenId, dragBId, dragCId) + additionalDragIds + originalOrder)
        }
        selectCustomSortAndSearchDragEvents()
        val itemBounds = composeRule.onNodeWithText("E2EDrag-A").fetchSemanticsNode().boundsInRoot
        val listNode = composeRule.onNode(hasScrollAction()).fetchSemanticsNode()
        val listBounds = listNode.boundsInRoot
        val scrollRange = listNode.config[SemanticsProperties.VerticalScrollAxisRange]
        val initialScroll = scrollRange.value()
        val targetY = listBounds.bottom - 8f * composeRule.activity.resources.displayMetrics.density
        assertTrue(composeRule.onAllNodesWithText("E2EDrag-M6").fetchSemanticsNodes().none {
            val bounds = it.boundsInRoot
            bounds.top < listBounds.bottom && bounds.bottom > listBounds.top
        })
        beginLongPressDrag("E2EDrag-A")
        composeRule.mainClock.autoAdvance = false
        val scrollClockStart = composeRule.mainClock.currentTime
        try {
            composeRule.onRoot().performTouchInput {
                moveTo(Offset(itemBounds.center.x, targetY), 300)
            }
            composeRule.mainClock.advanceTimeByFrame()
            assertDragIsActive("E2EDrag-A")
            // Advance a short continuous run of frames between Android layout observations.
            // One frame per wall-clock poll starves the library's frame-clock acceleration
            // while synchronized semantics queries wait for Android drawing.
            composeRule.waitUntil(timeoutMillis = 5_000) {
                val remainingClockMillis = 5_000 - (composeRule.mainClock.currentTime - scrollClockStart)
                check(remainingClockMillis > 0) { "M6 still outside the viewport after five seconds of drag frames" }
                composeRule.mainClock.advanceTimeBy(minOf(100L, remainingClockMillis))
                assertDragIsActive("E2EDrag-A")
                composeRule.onAllNodesWithText("E2EDrag-M6").fetchSemanticsNodes().any {
                    val bounds = it.boundsInRoot
                    bounds.top < listBounds.bottom && bounds.bottom > listBounds.top
                }
            }
            println("Edge drag reached M6: composeElapsedMs=${composeRule.mainClock.currentTime - scrollClockStart}, " +
                "scrollRange=$initialScroll -> ${scrollRange.value()}")
        } catch (failure: Throwable) {
            // Retain the clock budget, scroll range and visible bounds if native CI fails.
            println("Edge drag: composeElapsedMs=${composeRule.mainClock.currentTime - scrollClockStart}, " +
                "scrollRange=$initialScroll -> ${scrollRange.value()}, " +
                "listBounds=$listBounds, pointerTargetY=$targetY")
            println(runCatching { composeRule.onRoot(useUnmergedTree = true).printToString() }
                .getOrElse { "Semantics unavailable: $it" })
            throw failure
        } finally {
            composeRule.onRoot().performTouchInput { up() }
            composeRule.mainClock.autoAdvance = true
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { app.userPrefs.customEventOrderFlow.first().indexOf(dragAId) } > 4
        }
        val persisted = runBlocking { app.userPrefs.customEventOrderFlow.first() }
        assertEquals(1, persisted.indexOf(hiddenId))
    }

    @Test
    fun backClosesTheToolsPopupWithoutLeavingHome() {
        val homeToolsDescription = composeRule.activity.getString(R.string.home_tools_action)
        composeRule.onNodeWithContentDescription(homeToolsDescription).performClick()
        composeRule.onNode(hasSetTextAction()).assertExists()
        Espresso.pressBack()
        composeRule.onNode(hasSetTextAction()).assertDoesNotExist()
        composeRule.onNodeWithText("E2EDrag-A").assertExists()
    }

    private fun selectCustomSortAndSearchDragEvents() {
        val homeToolsDescription = composeRule.activity.getString(R.string.home_tools_action)
        val customSortLabel = composeRule.activity.getString(R.string.sort_by_created)
        // Room/DataStore completion is not a rendered frame. Pump queued collectors
        // while awaiting the fixture, before the first single-node assertion.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.onAllNodesWithText("E2EDrag-A").fetchSemanticsNodes().size == 1
        }
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
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) != true
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.mainClock.advanceTimeByFrame()
            listOf("E2EDrag-A", "E2EDrag-B", "E2EDrag-C").all { title ->
                composeRule.onAllNodesWithText(title).fetchSemanticsNodes().size == 1
            } && composeRule.onAllNodesWithText("E2EHidden").fetchSemanticsNodes().isEmpty()
        }
        listOf("E2EDrag-A", "E2EDrag-B", "E2EDrag-C").forEach { title ->
            composeRule.onNodeWithText(title).assertIsDisplayed()
        }
    }

    private fun beginLongPressDrag(title: String) {
        composeRule.onNodeWithText(title).performTouchInput { down(center) }
        // The pinned reorder library uses a coroutine timeout for long press. A batch
        // of synthetic event timestamps must not release the pointer before it fires.
        val reorderingLabel = composeRule.activity.getString(R.string.home_reordering)
        try {
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onNodeWithContentDescription(title, substring = true)
                    .fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription) ==
                    reorderingLabel
            }
        } catch (failure: Throwable) {
            composeRule.onRoot().performTouchInput { up() }
            throw failure
        }
    }

    private fun assertDragIsActive(title: String) {
        assertEquals(
            "Long-press drag must remain active after movement: $title",
            composeRule.activity.getString(R.string.home_reordering),
            composeRule.onNodeWithContentDescription(title, substring = true)
                .fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)
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
