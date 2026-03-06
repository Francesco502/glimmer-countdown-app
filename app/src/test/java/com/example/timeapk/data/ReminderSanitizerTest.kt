package com.example.timeapk.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSanitizerTest {

    @Test
    fun sanitizeRemindDaysBefore_clampsToValidRange() {
        assertEquals(0, sanitizeRemindDaysBefore(-10))
        assertEquals(3650, sanitizeRemindDaysBefore(99999))
    }

    @Test
    fun sanitizeReminderTimeMinutesOfDay_clampsToValidRange() {
        assertEquals(0, sanitizeReminderTimeMinutesOfDay(-1))
        assertEquals(1439, sanitizeReminderTimeMinutesOfDay(1440))
        assertEquals(1439, sanitizeReminderTimeMinutesOfDay(9999))
    }
}
