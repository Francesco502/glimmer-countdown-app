package com.example.timeapk.data

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class BackupImportSizeTest {
    @Test
    fun readsCompleteBackupWithinLimit() {
        val bytes = "[]".toByteArray()
        assertArrayEquals(bytes, readBackupImportBytes(ByteArrayInputStream(bytes)))
    }

    @Test
    fun acceptsExactLimitWithoutReadingPastEnd() {
        val bytes = ByteArray(MAX_BACKUP_IMPORT_BYTES)
        assertEquals(bytes.size, readBackupImportBytes(ByteArrayInputStream(bytes)).size)
    }

    @Test
    fun stopsStreamAtLimitInsteadOfReadingWholeFile() {
        val input = ByteArrayInputStream(ByteArray(MAX_BACKUP_IMPORT_BYTES + 8192))
        try {
            readBackupImportBytes(input)
            fail("Oversized input must fail")
        } catch (_: BackupInputTooLargeException) {
            assertEquals(8191, input.available())
        }
    }

    @Test
    fun directParserRejectsOversizedBytesBeforeParsing() {
        val bytes = ByteArray(MAX_BACKUP_IMPORT_BYTES + 1)
        assertEquals(BackupParseFailure.TOO_LARGE, parseEventsFromBackupBytesDetailed(bytes).failure)
        assertEquals(-1, parseEventsFromBackupBytes(bytes).errorCount)
    }
}
