package com.example.timeapk.notifications

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

private val scheduleEventProviderLocks = ConcurrentHashMap<Int, Mutex>()

/** Serializes CalendarProvider reminder mutations for one app event across every caller. */
internal suspend fun <T> withScheduleEventProviderLock(
    eventId: Int,
    transaction: suspend () -> T
): T {
    val mutex = scheduleEventProviderLocks.getOrPut(eventId) { Mutex() }
    mutex.lock()
    try {
        return withContext(Dispatchers.IO) { transaction() }
    } finally {
        mutex.unlock()
    }
}
