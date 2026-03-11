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
import java.time.temporal.ChronoUnit

private const val BASE_OCCURRENCE_SCAN = 16
private const val MAX_OCCURRENCE_SCAN = 10_000

internal data class ReminderTrigger(
    val triggerAtMillis: Long,
    val occurrenceDate: LocalDate,
    val reminderDate: LocalDate,
    val daysLeft: Int
)

internal data class ReminderSeries(
    val occurrenceDate: LocalDate,
    val entries: List<ReminderSeriesEntry>
)

internal data class ReminderSeriesEntry(
    val reminderDate: LocalDate,
    val daysLeft: Int
)

internal fun computeNextReminderTrigger(
    event: Event,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): ReminderTrigger? {
    val sanitizedEvent = event.sanitizedReminderConfig()
    val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val hour = sanitizedEvent.reminderTimeMinutesOfDay / 60
    val minute = sanitizedEvent.reminderTimeMinutesOfDay % 60

    var pivotDate = nowDate
    repeat(computeMaxOccurrenceScan(sanitizedEvent)) {
        val occurrenceDate = computeOccurrenceOnOrAfter(sanitizedEvent, pivotDate)
        var candidateDate = maxOf(nowDate, occurrenceDate.minusDays(sanitizedEvent.remindDaysBefore.toLong()))

        while (!candidateDate.isAfter(occurrenceDate)) {
            val remindAt = candidateDate.atTime(hour, minute).atZone(zoneId).toInstant().toEpochMilli()
            if (remindAt > nowMillis) {
                val daysLeft = ChronoUnit.DAYS.between(candidateDate, occurrenceDate).toInt()
                return ReminderTrigger(
                    triggerAtMillis = remindAt,
                    occurrenceDate = occurrenceDate,
                    reminderDate = candidateDate,
                    daysLeft = daysLeft
                )
            }
            candidateDate = candidateDate.plusDays(1)
        }

        if (sanitizedEvent.repeatType == REPEAT_NONE) {
            return null
        }
        pivotDate = occurrenceDate.plusDays(1)
    }

    return null
}

internal fun computeUpcomingReminderSeries(
    event: Event,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): ReminderSeries? {
    val sanitizedEvent = event.sanitizedReminderConfig()
    val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    var pivotDate = nowDate

    repeat(computeMaxOccurrenceScan(sanitizedEvent)) {
        val occurrenceDate = computeOccurrenceOnOrAfter(sanitizedEvent, pivotDate)
        var currentDate = maxOf(nowDate, occurrenceDate.minusDays(sanitizedEvent.remindDaysBefore.toLong()))
        val entries = mutableListOf<ReminderSeriesEntry>()

        while (!currentDate.isAfter(occurrenceDate)) {
            val daysLeft = ChronoUnit.DAYS.between(currentDate, occurrenceDate).toInt()
            entries += ReminderSeriesEntry(
                reminderDate = currentDate,
                daysLeft = daysLeft
            )
            currentDate = currentDate.plusDays(1)
        }

        if (entries.isNotEmpty()) {
            return ReminderSeries(
                occurrenceDate = occurrenceDate,
                entries = entries
            )
        }

        if (sanitizedEvent.repeatType == REPEAT_NONE) {
            return null
        }
        pivotDate = occurrenceDate.plusDays(1)
    }

    return null
}

internal fun computeNextReminderTriggerAtMillis(
    event: Event,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): Long? = computeNextReminderTrigger(event, nowMillis, zoneId)?.triggerAtMillis

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

internal fun computeOccurrenceOnOrAfter(event: Event, onOrAfterDate: LocalDate): LocalDate {
    val originDate = eventDateToLocalDate(event.date)
    return if (event.isLunar && event.repeatType == REPEAT_YEARLY) {
        getNextLunarOccurrence(originDate, onOrAfterDate)
    } else {
        nextOccurrenceDate(originDate, onOrAfterDate, event.repeatType)
    }
}
