package com.example.timeapk.ui.detail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventShareRendererTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun darkThemePreviewUsesTheExportedPaperColors() {
        val data = EventShareCardData("Preview consistency", "Anniversary", "2026-10-09", "24", "days left", Color(0xFF396A60), "Glimmer")
        composeRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                EventShareCard(data, Modifier.width(280.dp).aspectRatio(4f / 5f))
            }
        }
        val imageMatcher = hasContentDescription(data.title, substring = true)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(imageMatcher).fetchSemanticsNodes().isNotEmpty()
        }
        val preview = composeRule.onNode(imageMatcher).captureToImage().toPixelMap()
        val exported = EventShareImageRenderer().render(data)
        try {
            val samples = listOf(5 to 5, 540 to 1120)
            samples.forEach { (x, y) ->
                val actual = preview[(x * preview.width / SHARE_IMAGE_WIDTH_PX).coerceAtMost(preview.width - 1),
                    (y * preview.height / SHARE_IMAGE_HEIGHT_PX).coerceAtMost(preview.height - 1)].toArgb()
                val expected = exported.getPixel(x, y)
                listOf(16, 8, 0).forEach { shift ->
                    assertTrue(kotlin.math.abs(((actual shr shift) and 255) - ((expected shr shift) and 255)) <= 2)
                }
            }
        } finally {
            exported.recycle()
        }
    }

    @Test
    fun previewAndExportRendererAreDeterministicForLongMultilineText() {
        val data = EventShareCardData(
            title = "A very long shared countdown title\n一笺长题，岁月有期",
            categoryLabel = "Anniversary",
            dateText = "October 9, 2026",
            timeText = "12 years 10 months 28 days",
            timeLabel = "Until the next anniversary",
            accentColor = Color(0xFF396A60),
            brandText = "Glimmer Countdown"
        )
        val preview = EventShareImageRenderer().render(data)
        val exported = EventShareImageRenderer().render(data)
        try {
            assertEquals(SHARE_IMAGE_WIDTH_PX, preview.width)
            assertEquals(SHARE_IMAGE_HEIGHT_PX, preview.height)
            assertTrue(preview.sameAs(exported))
        } finally {
            preview.recycle()
            exported.recycle()
        }
    }
}
