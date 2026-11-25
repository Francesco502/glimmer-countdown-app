package com.example.timeapk.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.example.timeapk.R
import com.example.timeapk.update.DownloadProgress
import org.junit.Rule
import org.junit.Test

class UpdateDownloadProgressRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun downloadShowsPercentageOrBytesAndAccessibleProgress() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val progress = mutableStateOf(DownloadProgress(bytesRead = 256, totalBytes = 1024))
        compose.setContent { MaterialTheme { UpdateDownloadProgressSection(progress.value) } }

        compose.onNodeWithText(context.getString(R.string.update_download_progress, 25)).assertIsDisplayed()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo,
            ProgressBarRangeInfo(0.25f, 0f..1f))).assertExists()

        compose.runOnIdle { progress.value = DownloadProgress(bytesRead = 256, totalBytes = -1) }

        compose.onNodeWithText(context.getString(R.string.update_download_progress_unknown, "256 B")).assertIsDisplayed()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo,
            ProgressBarRangeInfo.Indeterminate)).assertExists()
    }
}
