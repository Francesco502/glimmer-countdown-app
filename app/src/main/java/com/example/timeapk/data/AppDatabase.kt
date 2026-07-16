package com.example.timeapk.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Event::class], version = 10, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN repeatType TEXT NOT NULL DEFAULT 'none'")
                db.execSQL("ALTER TABLE events ADD COLUMN remindDaysBefore INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE events ADD COLUMN remindEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE events ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN reminderTimeMinutesOfDay INTEGER NOT NULL DEFAULT 480")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN isLunar INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN syncToScheduleEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE events ADD COLUMN scheduleEventId INTEGER")
            }
        }
        // Note: MIGRATION_5_6 added tags; MIGRATION_7_8 intentionally drops it
        // during the table rebuild as the feature was removed.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
            }
        }
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN targetCalendarId INTEGER")
                db.execSQL("ALTER TABLE events ADD COLUMN lastScheduleSyncAt INTEGER")
                db.execSQL("ALTER TABLE events ADD COLUMN lastScheduleSyncError TEXT")
            }
        }
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS events_new (
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
                        isLunar INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO events_new (
                        id,
                        title,
                        date,
                        category,
                        note,
                        colorHex,
                        repeatType,
                        remindDaysBefore,
                        reminderTimeMinutesOfDay,
                        remindEnabled,
                        syncToScheduleEnabled,
                        scheduleEventId,
                        targetCalendarId,
                        lastScheduleSyncAt,
                        lastScheduleSyncError,
                        createdAt,
                        isLunar
                    )
                    SELECT
                        id,
                        title,
                        date,
                        category,
                        note,
                        colorHex,
                        repeatType,
                        remindDaysBefore,
                        reminderTimeMinutesOfDay,
                        remindEnabled,
                        syncToScheduleEnabled,
                        scheduleEventId,
                        targetCalendarId,
                        lastScheduleSyncAt,
                        lastScheduleSyncError,
                        createdAt,
                        isLunar
                    FROM events
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE events")
                db.execSQL("ALTER TABLE events_new RENAME TO events")
            }
        }
        // Note: MIGRATION_8_9 added birthHour/birthMinute; MIGRATION_9_10 intentionally
        // drops them during the table rebuild as the feature was removed.
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN birthHour INTEGER")
                db.execSQL("ALTER TABLE events ADD COLUMN birthMinute INTEGER")
            }
        }
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS events_new (
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
                        isLunar INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO events_new (
                        id,
                        title,
                        date,
                        category,
                        note,
                        colorHex,
                        repeatType,
                        remindDaysBefore,
                        reminderTimeMinutesOfDay,
                        remindEnabled,
                        syncToScheduleEnabled,
                        scheduleEventId,
                        targetCalendarId,
                        lastScheduleSyncAt,
                        lastScheduleSyncError,
                        createdAt,
                        isLunar
                    )
                    SELECT
                        id,
                        title,
                        date,
                        category,
                        note,
                        colorHex,
                        repeatType,
                        remindDaysBefore,
                        reminderTimeMinutesOfDay,
                        remindEnabled,
                        syncToScheduleEnabled,
                        scheduleEventId,
                        targetCalendarId,
                        lastScheduleSyncAt,
                        lastScheduleSyncError,
                        createdAt,
                        isLunar
                    FROM events
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE events")
                db.execSQL("ALTER TABLE events_new RENAME TO events")
            }
        }
        internal val MIGRATION_6_7_FOR_TEST: Migration = MIGRATION_6_7
        internal val MIGRATION_7_8_FOR_TEST: Migration = MIGRATION_7_8
        internal val MIGRATION_8_9_FOR_TEST: Migration = MIGRATION_8_9
        internal val MIGRATION_9_10_FOR_TEST: Migration = MIGRATION_9_10

        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "event_database")
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10
                    )
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
