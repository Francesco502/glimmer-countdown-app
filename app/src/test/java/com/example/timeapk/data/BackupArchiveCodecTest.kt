package com.example.timeapk.data

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupArchiveCodecTest {
    @Test
    fun roundTripPreservesOrderingArchivalAndImageReferences() {
        val image = "0123456789abcdef0123456789abcdef.jpg"
        val category = Category("custom_family", "Family", "#457080", 0, 1)
        val event = Event(
            id = 19, title = "Original date", date = 1704067200000, category = category.id,
            note = "Original note", imagePath = image, createdAt = 1, isArchived = true,
            remindDaysBefore = 7, remindLeadDays = "7,1,0", syncToScheduleEnabled = true,
            scheduleEventId = 51, targetCalendarId = 2
        )
        val imageBytes = byteArrayOf(1, 2, 3, 4)
        val ordering = BackupOrdering(listOf(19), listOf(19), listOf(520, 1000), 2)
        val zip = ByteArrayOutputStream()
        BackupArchiveCodec.write(zip, listOf(event), listOf(category), ordering) { name ->
            assertEquals(image, name)
            imageBytes.inputStream()
        }
        val (parsed, archive) = parseBackupImport(zip.toByteArray().inputStream())
        assertNull(parsed.failure)
        assertEquals(BackupSource.ZIP, parsed.source)
        assertEquals(event.copy(scheduleEventId = null, targetCalendarId = null), parsed.events.single())
        assertEquals(ImportedCategoryDraft("Family", "#457080"), parsed.categoryDrafts[category.id])
        val payload = checkNotNull(archive)
        assertArrayEquals(imageBytes, payload.images[image])
        assertEquals(ordering, payload.ordering)
    }

    @Test
    fun rejectsPathsOutsideTheBackupLayoutBeforeReadingTheirPayloads() {
        for (name in listOf("../manifest.json", "images/../outside.jpg", "/manifest.json", "images\\file.jpg")) {
            val bytes = zipWith(name) { it.write(1) }
            val (parsed, archive) = parseBackupImport(bytes.inputStream())
            assertEquals(name, BackupParseFailure.UNSUPPORTED_FORMAT, parsed.failure)
            assertNull(archive)
        }
    }

    @Test
    fun rejectsInflatedImageBeyondItsLimitEvenWithoutDeclaredSize() {
        val bytes = zipWith("images/0123456789abcdef0123456789abcdef.jpg") { zip ->
            val chunk = ByteArray(8192)
            repeat(MAX_BACKUP_IMAGE_BYTES / chunk.size) { zip.write(chunk) }
            zip.write(1)
        }
        assertThrows(BackupInputTooLargeException::class.java) { parseBackupImport(bytes.inputStream()) }
    }

    private fun zipWith(name: String, write: (ZipOutputStream) -> Unit): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(name))
            write(zip)
            zip.closeEntry()
        }
        return output.toByteArray()
    }
}
