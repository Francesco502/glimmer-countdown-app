package com.example.timeapk.notifications

import com.example.timeapk.data.Event
import com.example.timeapk.data.resolvedRemindLeadDays
import com.example.timeapk.data.sanitizeReminderTimeMinutesOfDay
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.nextOccurrenceDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal data class ReminderTrigger(
    val triggerAtMillis: Long,
    val occurrenceDate: LocalDate,
    val reminderDate: LocalDate,
    val daysLeft: Int
)

internal fun computeNextReminderTrigger(
    event: Event,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): ReminderTrigger? = computeUpcomingReminderTriggers(event, nowMillis, zoneId).firstOrNull()

internal fun computeUpcomingReminderTriggers(
    event: Event,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<ReminderTrigger> {
    if (event.isArchived) return emptyList()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val minutes = sanitizeReminderTimeMinutesOfDay(event.reminderTimeMinutesOfDay)
    fun triggerAt(date: LocalDate): Long =
        date.atTime(minutes / 60, minutes % 60).atZone(zoneId).toInstant().toEpochMilli()

    val firstReminderDay = if (triggerAt(today) > nowMillis) today else today.plusDays(1)
    return event.resolvedRemindLeadDays().mapNotNull { leadDays ->
        val pivot = firstReminderDay.plusDays(leadDays.toLong())
        val occurrence = computeOccurrenceOnOrAfter(event, pivot)
        if (occurrence.isBefore(pivot)) return@mapNotNull null
        val reminderDate = occurrence.minusDays(leadDays.toLong())
        ReminderTrigger(triggerAt(reminderDate), occurrence, reminderDate, leadDays)
    }.sortedWith(compareBy<ReminderTrigger> { it.triggerAtMillis }.thenBy { it.daysLeft })
        .distinctBy { it.triggerAtMillis }
}

internal fun computeNextReminderTriggerAtMillis(
    event: Event,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): Long? = computeNextReminderTrigger(event, nowMillis, zoneId)?.triggerAtMillis

internal fun computeOccurrenceOnOrAfter(event: Event, onOrAfterDate: LocalDate): LocalDate {
    val originDate = eventDateToLocalDate(event.date)
    return nextOccurrenceDate(originDate, onOrAfterDate, event.repeatType, event.isLunar)
}
