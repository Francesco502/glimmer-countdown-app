package com.example.timeapk.ui.detail

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EventShareCardPolicyTest {
    @Test
    fun shareCardDataKeepsPrivateReminderAndNoteFieldsOutOfExportedImage() {
        val source = readSource("ui/detail/EventShareCard.kt")

        assertTrue(source.contains("data class EventShareCardData("))
        assertTrue(source.contains("fun buildEventShareCardData("))
        assertTrue(source.contains("@Composable\nfun EventShareCard("))

        val dataClassBlock = source.substringBetween(
            "data class EventShareCardData(",
            "fun buildEventShareCardData("
        )
        assertFalse(dataClassBlock.contains("note"))
        assertFalse(dataClassBlock.contains("reminder"))
        assertFalse(dataClassBlock.contains("lastScheduleSyncError"))
        assertFalse(dataClassBlock.contains("permission"))

        val builderBlock = source.substringBetween(
            "fun buildEventShareCardData(",
            "@Composable\nfun EventShareCard("
        )
        assertFalse(builderBlock.contains(".note"))
        assertFalse(builderBlock.contains("lastScheduleSyncError"))
        assertFalse(builderBlock.contains("ReminderStatus"))
        assertFalse(builderBlock.contains("areAppNotificationsEnabledCompat"))
        assertFalse(builderBlock.contains("hasCalendarReadWritePermission"))
    }

    @Test
    fun shareImagePipelineUsesExistingProviderAndNativeSendIntent() {
        val rendererSource = readSource("ui/detail/EventShareImageRenderer.kt")
        val storeSource = readSource("ui/detail/ShareImageStore.kt")

        assertFalse(rendererSource.contains("ComposeView"))
        assertTrue(rendererSource.contains("TextPaint"))
        assertTrue(rendererSource.contains("drawText"))
        assertTrue(rendererSource.contains("SHARE_IMAGE_WIDTH_PX = 1080"))
        assertTrue(rendererSource.contains("SHARE_IMAGE_HEIGHT_PX = 1350"))
        assertTrue(storeSource.contains("FileProvider.getUriForFile("))
        assertTrue(storeSource.contains("Intent.ACTION_SEND"))
        assertTrue(storeSource.contains("MediaStore.Images.Media"))
        assertTrue(storeSource.contains("\"share/\""))
    }

    @Test
    fun shareImageRenderingAndPngWritesStayOffTheUiDispatcher() {
        val detailSource = readSource("ui/detail/DetailScreen.kt")

        assertTrue(detailSource.contains("withContext(Dispatchers.Default)"))
        assertTrue(detailSource.contains("EventShareImageRenderer().render(data)"))
        assertTrue(detailSource.contains("withContext(Dispatchers.IO)"))

        val saveAction = detailSource.substringBetween(
            "text = stringResource(R.string.share_save_image)",
            "text = stringResource(R.string.share_send_image)"
        )
        val sendAction = detailSource.substringBetween(
            "text = stringResource(R.string.share_send_image)",
            "Scaffold("
        )
        assertTrue(saveAction.contains("withRenderedShareImage(shareData)"))
        assertTrue(sendAction.contains("withRenderedShareImage(shareData)"))
    }

    private fun readSource(relative: String): String {
        val direct = File("src/main/java/com/example/timeapk/$relative")
        if (direct.exists()) {
            return direct.readText(Charsets.UTF_8)
        }
        val fromRoot = File("app/src/main/java/com/example/timeapk/$relative")
        require(fromRoot.exists()) { "Missing source file: $relative" }
        return fromRoot.readText(Charsets.UTF_8)
    }

    private fun String.substringBetween(start: String, end: String): String {
        val startIndex = indexOf(start)
        val endIndex = indexOf(end, startIndex + start.length)
        require(startIndex >= 0) { "Missing start marker: $start" }
        require(endIndex > startIndex) { "Missing end marker: $end" }
        return substring(startIndex, endIndex)
    }
}
