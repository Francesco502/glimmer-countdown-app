package com.example.timeapk.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.timeapk.BuildConfig
import com.example.timeapk.MainActivity
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.update.CheckUpdateResult
import com.example.timeapk.update.UPDATE_POSTPONE_MS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeUpdateFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun validatedReleaseOpensSharedNotesAndCanBePostponed() {
        val context = compose.activity
        val app = context.application as TimeApplication
        val prefs = app.userPrefs
        val before = runBlocking { prefs.automaticUpdateSettings.first() }
        val available = app.availableUpdate.value ?: CheckUpdateResult(false)
        val release = CheckUpdateResult(
            hasUpdate = true,
            versionName = "42.0.0",
            downloadUrl = "https://example.invalid/qa.apk",
            releaseNotes = "QA release notes"
        )
        try {
            val now = System.currentTimeMillis()
            runBlocking {
                prefs.setAutomaticUpdateEnabled(true)
                prefs.claimAutomaticUpdateCheck(now)
                prefs.postponeUpdate(now - UPDATE_POSTPONE_MS)
            }
            app.rememberAvailableUpdate(release)
            val title = context.getString(R.string.update_banner_title, release.versionName)
            if (!BuildConfig.DIRECT_APK_UPDATES_ENABLED) {
                compose.onNodeWithText(title).assertDoesNotExist()
                return
            }

            val open = context.getString(R.string.update_banner_open)
            compose.waitUntil(5_000) { compose.onAllNodesWithText(open).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(open).performClick()
            compose.onNodeWithText("QA release notes").assertIsDisplayed()
            compose.onNodeWithText(context.getString(R.string.update_download_install)).assertIsDisplayed()
            compose.onNode(hasText(context.getString(R.string.update_later)) and hasAnyAncestor(isDialog())).performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText(open).fetchSemanticsNodes().isEmpty()
            }
            assertTrue(runBlocking { prefs.automaticUpdateSettings.first().postponedUntil } >= now + UPDATE_POSTPONE_MS)
        } finally {
            app.dismissUpdateDetails()
            app.rememberAvailableUpdate(available)
            runBlocking {
                prefs.postponeUpdate(before.postponedUntil - UPDATE_POSTPONE_MS)
                prefs.claimAutomaticUpdateCheck(before.lastCheckedAt)
                prefs.setAutomaticUpdateEnabled(before.enabled)
            }
        }
    }
}
