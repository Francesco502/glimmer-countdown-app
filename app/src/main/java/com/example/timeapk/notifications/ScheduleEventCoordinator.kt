package com.example.timeapk.notifications

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val scheduleEventProviderLocks = ConcurrentHashMap<Int, ReentrantLock>()

/** Serializes CalendarProvider reminder mutations for one app event across every caller. */
internal fun <T> withScheduleEventProviderLock(
    eventId: Int,
    transaction: () -> T
): T = scheduleEventProviderLocks.getOrPut(eventId) { ReentrantLock() }.withLock(transaction)
