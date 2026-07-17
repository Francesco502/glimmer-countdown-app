package com.example.timeapk.data

import com.example.timeapk.ui.event.buildNewEventDetails
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultEventReminderPreferenceTest {

    @Test
    fun resolveDefaultEventReminderEnabled_defaultsMissingPreferenceToDisabled() {
        assertFalse(resolveDefaultEventReminderEnabled(null))
    }

    @Test
    fun resolveDefaultEventReminderEnabled_preservesExplicitStoredValues() {
        assertTrue(resolveDefaultEventReminderEnabled(true))
        assertFalse(resolveDefaultEventReminderEnabled(false))
    }

    @Test
    fun defaultReminderSettings_createNewEventsWithRemindersDisabled() {
        val details = buildNewEventDetails(DefaultEventReminderSettings())

        assertFalse(DefaultEventReminderSettings().enabled)
        assertFalse(details.remindEnabled)
    }
}
