package com.example.timeapk.notifications

import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_DAILY
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_WEEKLY
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.data.sanitizedReminderConfig
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.getNextLunarOccurrence
import com.example.timeapk.ui.utils.nextOccurrenceDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val BASE_OCCURRENCE_SCAN = 16
private const val MAX_OCCURRENCE_SCAN = 10_000

internal fun computeNextReminderTriggerAtMillis(
    event: Event,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): Long? {
    val sanitizedEvent = event.sanitizedReminderConfig()
    val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    var occurrenceDate = computeOccurrenceOnOrAfter(sanitizedEvent, nowDate)
    val hour = sanitizedEvent.reminderTimeMinutesOfDay / 60
    val minute = sanitizedEvent.reminderTimeMinutesOfDay % 60

    repeat(computeMaxOccurrenceScan(sanitizedEvent)) {
        val remindDate = occurrenceDate.minusDays(sanitizedEvent.remindDaysBefore.toLong())
        val remindAt = remindDate.atTime(hour, minute).atZone(zoneId).toInstant().toEpochMilli()
        if (remindAt > nowMillis) {
            return remindAt
        }
        if (sanitizedEvent.repeatType == REPEAT_NONE) {
            return null
        }
        occurrenceDate = computeOccurrenceOnOrAfter(sanitizedEvent, occurrenceDate.plusDays(1))
    }
    return null
}

private fun computeMaxOccurrenceScan(event: Event): Int {
    val daysBefore = event.remindDaysBefore.coerceAtLeast(0)
    val extraScan = when (event.repeatType) {
        REPEAT_DAILY -> daysBefore
        REPEAT_WEEKLY -> daysBefore / 7
        REPEAT_MONTHLY -> daysBefore / 28
        REPEAT_HALF_YEARLY -> daysBefore / 182
        REPEAT_YEARLY -> daysBefore / 365
        REPEAT_NONE -> 0
        else -> daysBefore
    }
    return (BASE_OCCURRENCE_SCAN + extraScan + 2).coerceIn(1, MAX_OCCURRENCE_SCAN)
}

private fun computeOccurrenceOnOrAfter(event: Event, onOrAfterDate: LocalDate): LocalDate {
    val originDate = eventDateToLocalDate(event.date)
    return if (event.isLunar && event.repeatType == REPEAT_YEARLY) {
        getNextLunarOccurrence(originDate, onOrAfterDate)
    } else {
        nextOccurrenceDate(originDate, onOrAfterDate, event.repeatType)
    }
}
