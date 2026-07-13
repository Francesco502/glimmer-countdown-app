package com.example.timeapk.notifications

sealed interface CalendarCleanupResult {
    val isSuccess: Boolean
    val message: String?

    data object RemovedOrNotPresent : CalendarCleanupResult {
        override val isSuccess = true
        override val message: String? = null
    }

    data object PermissionRequired : CalendarCleanupResult {
        override val isSuccess = false
        override val message = "Calendar permission required"
    }

    data class ProviderFailure(override val message: String) : CalendarCleanupResult {
        override val isSuccess = false
    }
}

internal fun <T : Any> requireCalendarCleanupQuery(result: T?, source: String): T {
    return result ?: throw IllegalStateException("$source query returned no cursor")
}
