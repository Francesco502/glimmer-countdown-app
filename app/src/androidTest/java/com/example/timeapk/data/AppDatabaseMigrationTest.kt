package com.example.timeapk.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migratePublished6To10_runsTheFullChainAndPreservesRows() {
        val dbName = "migration-test-v6-to-v10"

        helper.createDatabase(dbName, 6).apply {
            execSQL(
                """
                INSERT INTO events (
                    id, title, date, category, note, tags, colorHex, repeatType,
                    remindDaysBefore, reminderTimeMinutesOfDay, remindEnabled,
                    syncToScheduleEnabled, scheduleEventId, createdAt, isLunar
                ) VALUES (
                    1, 'Birthday', 1704067200000, 'birthday', 'note', 'family', '#AF4E31', 'yearly',
                    7, 480, 1,
                    1, 91, 1704067200000, 0
                )
                """.trimIndent()
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            dbName,
            10,
            true,
            AppDatabase.MIGRATION_6_7_FOR_TEST,
            AppDatabase.MIGRATION_7_8_FOR_TEST,
            AppDatabase.MIGRATION_8_9_FOR_TEST,
            AppDatabase.MIGRATION_9_10_FOR_TEST
        )

        val columns = columnNamesOf(migratedDb, "events")
        assertFalse(columns.contains("tags"))
        assertFalse(columns.contains("birthHour"))
        assertFalse(columns.contains("birthMinute"))
        assertTrue(columns.contains("targetCalendarId"))
        assertTrue(columns.contains("lastScheduleSyncAt"))
        assertTrue(columns.contains("lastScheduleSyncError"))

        migratedDb.query(
            """
            SELECT title, date, category, note, colorHex, repeatType,
                   remindDaysBefore, reminderTimeMinutesOfDay, remindEnabled,
                   syncToScheduleEnabled, scheduleEventId, createdAt, isLunar,
                   targetCalendarId, lastScheduleSyncAt, lastScheduleSyncError
            FROM events WHERE id = 1
            """.trimIndent()
        ).use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("Birthday", cursor.getString(0))
            assertEquals(1704067200000, cursor.getLong(1))
            assertEquals("birthday", cursor.getString(2))
            assertEquals("note", cursor.getString(3))
            assertEquals("#AF4E31", cursor.getString(4))
            assertEquals("yearly", cursor.getString(5))
            assertEquals(7, cursor.getInt(6))
            assertEquals(480, cursor.getInt(7))
            assertEquals(1, cursor.getInt(8))
            assertEquals(1, cursor.getInt(9))
            assertEquals(91, cursor.getLong(10))
            assertEquals(1704067200000, cursor.getLong(11))
            assertEquals(0, cursor.getInt(12))
            assertEquals(true, cursor.isNull(13))
            assertEquals(true, cursor.isNull(14))
            assertEquals(true, cursor.isNull(15))
        }
        migratedDb.close()
    }

    @Test
    fun migrate9To10_dropsBirthTimeColumnsAndPreservesRows() {
        val dbName = "migration-test-v9-to-v10"

        helper.createDatabase(dbName, 9).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    note TEXT NOT NULL,
                    colorHex TEXT,
                    repeatType TEXT NOT NULL,
                    remindDaysBefore INTEGER NOT NULL,
                    reminderTimeMinutesOfDay INTEGER NOT NULL,
                    remindEnabled INTEGER NOT NULL,
                    syncToScheduleEnabled INTEGER NOT NULL,
                    scheduleEventId INTEGER,
                    targetCalendarId INTEGER,
                    lastScheduleSyncAt INTEGER,
                    lastScheduleSyncError TEXT,
                    createdAt INTEGER NOT NULL,
                    isLunar INTEGER NOT NULL,
                    birthHour INTEGER,
                    birthMinute INTEGER
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO events (
                    id, title, date, category, note, colorHex, repeatType,
                    remindDaysBefore, reminderTimeMinutesOfDay, remindEnabled,
                    syncToScheduleEnabled, scheduleEventId, targetCalendarId,
                    lastScheduleSyncAt, lastScheduleSyncError, createdAt, isLunar,
                    birthHour, birthMinute
                ) VALUES (
                    1, 'Birthday', 1704067200000, 'birthday', 'note', '#AF4E31', 'yearly',
                    7, 480, 1,
                    1, NULL, NULL,
                    NULL, NULL, 1704067200000, 0,
                    8, 30
                )
                """.trimIndent()
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            dbName,
            10,
            true,
            AppDatabase.MIGRATION_9_10_FOR_TEST
        )

        val columns = columnNamesOf(migratedDb, "events")
        assertFalse(columns.contains("birthHour"))
        assertFalse(columns.contains("birthMinute"))

        migratedDb.query("SELECT title, category, remindDaysBefore, isLunar FROM events WHERE id = 1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("Birthday", cursor.getString(0))
            assertEquals("birthday", cursor.getString(1))
            assertEquals(7, cursor.getInt(2))
            assertEquals(0, cursor.getInt(3))
        }
        migratedDb.close()
    }

    private fun columnNamesOf(db: SupportSQLiteDatabase, tableName: String): Set<String> {
        val result = linkedSetOf<String>()
        db.query("PRAGMA table_info($tableName)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                result += cursor.getString(nameIndex)
            }
        }
        return result
    }
}
