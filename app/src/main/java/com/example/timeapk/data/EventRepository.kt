package com.example.timeapk.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class EventRepository(private val eventDao: EventDao) {
    fun getAllEvents(): Flow<List<Event>> = eventDao.getAllEvents()

    suspend fun getAllEventsSnapshot(): List<Event> = eventDao.getAllEvents().first()

    suspend fun getEvent(id: Int): Event? = eventDao.getEventById(id)

    fun getEventFlow(id: Int): Flow<Event?> = eventDao.getEventByIdFlow(id)

    suspend fun getLatestScheduleSyncEvent(): Event? = eventDao.getLatestScheduleSyncEvent()

    suspend fun insertEvent(event: Event): Long = eventDao.insertEvent(event)

    suspend fun deleteEvent(event: Event) = eventDao.deleteEvent(event)
    
    suspend fun updateEvent(event: Event) = eventDao.updateEvent(event)
}
