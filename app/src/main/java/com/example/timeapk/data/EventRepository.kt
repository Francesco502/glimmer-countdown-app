package com.example.timeapk.data

import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

internal fun Event.hasSameScheduleSyncInputs(expected: Event): Boolean =
    id == expected.id &&
        title == expected.title &&
        date == expected.date &&
        note == expected.note &&
        repeatType == expected.repeatType &&
        remindDaysBefore == expected.remindDaysBefore &&
        remindLeadDays == expected.remindLeadDays &&
        isArchived == expected.isArchived &&
        reminderTimeMinutesOfDay == expected.reminderTimeMinutesOfDay &&
        remindEnabled == expected.remindEnabled &&
        syncToScheduleEnabled == expected.syncToScheduleEnabled &&
        isLunar == expected.isLunar &&
        scheduleEventId == expected.scheduleEventId &&
        targetCalendarId == expected.targetCalendarId

internal fun eventWithScheduleSyncStateIfInputsUnchanged(
    current: Event,
    expected: Event,
    updated: Event
): Event? {
    if (!current.hasSameScheduleSyncInputs(expected) || updated.id != expected.id) return null
    return current.copy(
        scheduleEventId = updated.scheduleEventId,
        targetCalendarId = updated.targetCalendarId,
        lastScheduleSyncAt = updated.lastScheduleSyncAt,
        lastScheduleSyncError = updated.lastScheduleSyncError
    )
}

class EventRepository(private val eventDao: EventDao) {
    fun getAllEvents(): Flow<List<Event>> = eventDao.getAllEvents()

    suspend fun getAllEventsSnapshot(): List<Event> = eventDao.getAllEvents().first()

    suspend fun getEvent(id: Int): Event? = eventDao.getEventById(id)

    fun getEventFlow(id: Int): Flow<Event?> = eventDao.getEventByIdFlow(id)

    suspend fun getLatestScheduleSyncEvent(): Event? = eventDao.getLatestScheduleSyncEvent()

    suspend fun insertEvent(event: Event): Long = eventDao.insertEvent(event)

    suspend fun deleteEvent(event: Event) = eventDao.deleteEvent(event)

    suspend fun updateEvent(event: Event) = eventDao.updateEvent(event)
    suspend fun setEventArchived(id: Int, archived: Boolean): Boolean =
        eventDao.setEventArchived(id, archived, todayUtcMillis()) == 1

    /** @return true when the row still existed and the user-editable columns were written. */
    suspend fun updateEventUserFields(event: Event): Boolean = eventDao.updateEventUserFields(
        id = event.id,
        title = event.title,
        date = event.date,
        category = event.category,
        note = event.note,
        colorHex = event.colorHex,
        repeatType = event.repeatType,
        remindDaysBefore = event.remindDaysBefore,
        remindLeadDays = event.remindLeadDays,
        reminderTimeMinutesOfDay = event.reminderTimeMinutesOfDay,
        remindEnabled = event.remindEnabled,
        syncToScheduleEnabled = event.syncToScheduleEnabled,
        isLunar = event.isLunar,
        imagePath = event.imagePath,
        todayUtcMillis = todayUtcMillis()
    ) == 1

    suspend fun updateEventScheduleFields(event: Event) = eventDao.updateEventScheduleFields(
        id = event.id,
        scheduleEventId = event.scheduleEventId,
        targetCalendarId = event.targetCalendarId,
        lastScheduleSyncAt = event.lastScheduleSyncAt,
        lastScheduleSyncError = event.lastScheduleSyncError
    )

    suspend fun updateScheduleSyncState(expected: Event, updated: Event): Boolean {
        val current = eventDao.getEventById(expected.id) ?: return false
        val scheduleState = eventWithScheduleSyncStateIfInputsUnchanged(
            current = current,
            expected = expected,
            updated = updated
        ) ?: return false
        return eventDao.updateScheduleSyncStateIfInputsUnchanged(
            id = expected.id,
            scheduleEventId = scheduleState.scheduleEventId,
            targetCalendarId = scheduleState.targetCalendarId,
            lastScheduleSyncAt = scheduleState.lastScheduleSyncAt,
            lastScheduleSyncError = scheduleState.lastScheduleSyncError,
            expectedTitle = expected.title,
            expectedDate = expected.date,
            expectedNote = expected.note,
            expectedRepeatType = expected.repeatType,
            expectedRemindDaysBefore = expected.remindDaysBefore,
            expectedRemindLeadDays = expected.remindLeadDays,
            expectedIsArchived = expected.isArchived,
            expectedReminderTimeMinutesOfDay = expected.reminderTimeMinutesOfDay,
            expectedRemindEnabled = expected.remindEnabled,
            expectedSyncToScheduleEnabled = expected.syncToScheduleEnabled,
            expectedIsLunar = expected.isLunar,
            expectedScheduleEventId = expected.scheduleEventId,
            expectedTargetCalendarId = expected.targetCalendarId
        ) == 1
    }
}

private fun todayUtcMillis(): Long =
    LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
