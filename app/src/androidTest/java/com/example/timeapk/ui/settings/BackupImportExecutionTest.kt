package com.example.timeapk.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.toJsonString
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupImportExecutionTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun twoImportCallbacksBeforeRecompositionInsertEachEventOnceWithoutCalendarErrors() {
        val app = ApplicationProvider.getApplicationContext<TimeApplication>()
        val prefix = "E2E-Import-${System.nanoTime()}-"
        val events = (1..3).map {
            Event(title = "$prefix$it", date = System.currentTimeMillis(), category = CATEGORY_OTHER,
                remindEnabled = false, syncToScheduleEnabled = false)
        }
        try {
            composeRule.setContent { MaterialTheme { DataSettingsContent(SnackbarHostState()) } }
            val importLabel = app.getString(R.string.import_events)
            composeRule.onNode(hasText(importLabel) and hasClickAction()).performClick()
            composeRule.onNode(hasSetTextAction()).performTextInput(events.toJsonString())
            composeRule.onNode(hasText(app.getString(R.string.import_preview_action)) and hasClickAction()).performClick()
            composeRule.waitUntil(10_000) {
                composeRule.onAllNodes(hasText(importLabel) and hasClickAction()).fetchSemanticsNodes().size == 2
            }
            val importAction = composeRule.onAllNodes(hasText(importLabel) and hasClickAction())
                .onLast().fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
            composeRule.runOnIdle {
                importAction()
                importAction()
                assertTrue(app.backupImportInProgress.value)
            }
            composeRule.waitUntil(20_000) { !app.backupImportInProgress.value }
            val imported = runBlocking { app.repository.getAllEventsSnapshot().filter { it.title.startsWith(prefix) } }
            assertEquals(events.map { it.title }.toSet(), imported.map { it.title }.toSet())
            assertEquals(events.size, imported.size)
            imported.forEach { assertNull(it.lastScheduleSyncError) }
        } finally {
            composeRule.waitUntil(20_000) { !app.backupImportInProgress.value }
            runBlocking {
                app.repository.getAllEventsSnapshot().filter { it.title.startsWith(prefix) }
                    .forEach { app.repository.deleteEvent(it) }
            }
        }
    }
}
