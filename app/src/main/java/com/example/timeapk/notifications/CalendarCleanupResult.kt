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

internal data class CalendarCleanupDescriptionCandidate(
    val calendarEventId: Long,
    val description: String
)

internal data class CalendarCleanupMetadataCandidate(
    val calendarEventId: Long,
    val kind: String?
)

internal interface CalendarCleanupGateway {
    fun hasReadPermission(): Boolean
    fun hasWritePermission(): Boolean

    fun queryDescriptionCandidates(
        eventId: Int,
        includeReminders: Boolean,
        includeMilestones: Boolean
    ): List<CalendarCleanupDescriptionCandidate>?

    fun queryMetadataCandidates(eventId: Int): List<CalendarCleanupMetadataCandidate>?

    fun deleteEvent(calendarEventId: Long)
}

internal fun <T : Any> requireCalendarCleanupQuery(result: T?, source: String): T {
    return result ?: throw IllegalStateException("$source query returned no cursor")
}
