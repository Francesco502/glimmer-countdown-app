package com.example.timeapk.data

private const val MAX_REMIND_DAYS_BEFORE = 3650
private const val MINUTES_PER_DAY = 24 * 60

fun sanitizeRemindDaysBefore(value: Int): Int = value.coerceIn(0, MAX_REMIND_DAYS_BEFORE)

fun sanitizeReminderTimeMinutesOfDay(value: Int): Int = value.coerceIn(0, MINUTES_PER_DAY - 1)

fun Event.sanitizedReminderConfig(): Event = copy(
    remindDaysBefore = sanitizeRemindDaysBefore(remindDaysBefore),
    reminderTimeMinutesOfDay = sanitizeReminderTimeMinutesOfDay(reminderTimeMinutesOfDay)
)
