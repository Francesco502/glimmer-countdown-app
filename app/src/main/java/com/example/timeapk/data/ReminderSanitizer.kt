package com.example.timeapk.data

private const val MAX_REMIND_DAYS_BEFORE = 3650
private const val MINUTES_PER_DAY = 24 * 60

fun sanitizeRemindDaysBefore(value: Int): Int = value.coerceIn(0, MAX_REMIND_DAYS_BEFORE)

fun sanitizeReminderTimeMinutesOfDay(value: Int): Int = value.coerceIn(0, MINUTES_PER_DAY - 1)

internal const val MAX_REMINDER_LEADS = 3

internal fun normalizeReminderLeadDays(days: Iterable<Int>): List<Int> =
    days.map(::sanitizeRemindDaysBefore).distinct().sortedDescending().take(MAX_REMINDER_LEADS)

internal fun legacyReminderLeadDays(daysBefore: Int): List<Int> =
    normalizeReminderLeadDays(listOf(daysBefore, 0))

internal fun resolveReminderLeadDays(explicit: String?, legacy: Int): List<Int> =
    explicit?.split(',')?.mapNotNull(String::toIntOrNull)
        ?.let(::normalizeReminderLeadDays)?.takeIf { it.isNotEmpty() }
        ?: legacyReminderLeadDays(legacy)

internal fun Event.resolvedRemindLeadDays(): List<Int> = resolveReminderLeadDays(remindLeadDays, remindDaysBefore)

fun Event.sanitizedReminderConfig(): Event = copy(
    remindDaysBefore = sanitizeRemindDaysBefore(remindDaysBefore),
    remindLeadDays = remindLeadDays?.let { resolvedRemindLeadDays().joinToString(",") },
    reminderTimeMinutesOfDay = sanitizeReminderTimeMinutesOfDay(reminderTimeMinutesOfDay)
)
