package com.example.timeapk.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY date ASC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: Int): Event?

    /** Flow 版本：当数据库变更时自动推送最新事件 */
    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventByIdFlow(id: Int): Flow<Event?>

    @Query(
        """
        SELECT * FROM events
        WHERE lastScheduleSyncAt IS NOT NULL
        ORDER BY lastScheduleSyncAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestScheduleSyncEvent(): Event?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    @Update
    suspend fun updateEvent(event: Event)

    @Query(
        """
        UPDATE events SET
            scheduleEventId = :scheduleEventId,
            targetCalendarId = :targetCalendarId,
            lastScheduleSyncAt = :lastScheduleSyncAt,
            lastScheduleSyncError = :lastScheduleSyncError
        WHERE id = :id
          AND title = :expectedTitle
          AND date = :expectedDate
          AND note = :expectedNote
          AND repeatType = :expectedRepeatType
          AND remindDaysBefore = :expectedRemindDaysBefore
          AND reminderTimeMinutesOfDay = :expectedReminderTimeMinutesOfDay
          AND remindEnabled = :expectedRemindEnabled
          AND syncToScheduleEnabled = :expectedSyncToScheduleEnabled
          AND isLunar = :expectedIsLunar
          AND scheduleEventId IS :expectedScheduleEventId
          AND targetCalendarId IS :expectedTargetCalendarId
        """
    )
    suspend fun updateScheduleSyncStateIfInputsUnchanged(
        id: Int,
        scheduleEventId: Long?,
        targetCalendarId: Long?,
        lastScheduleSyncAt: Long?,
        lastScheduleSyncError: String?,
        expectedTitle: String,
        expectedDate: Long,
        expectedNote: String,
        expectedRepeatType: String,
        expectedRemindDaysBefore: Int,
        expectedReminderTimeMinutesOfDay: Int,
        expectedRemindEnabled: Boolean,
        expectedSyncToScheduleEnabled: Boolean,
        expectedIsLunar: Boolean,
        expectedScheduleEventId: Long?,
        expectedTargetCalendarId: Long?
    ): Int

    @Delete
    suspend fun deleteEvent(event: Event)
}
