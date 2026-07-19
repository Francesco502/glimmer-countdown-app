package com.example.timeapk.notifications

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.timeapk.R
import com.example.timeapk.data.Event
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_WEEKLY
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.data.sanitizedReminderConfig
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun isManagedReminderMetadataKind(kind: String?): Boolean {
    return kind == null || kind == "reminder_v2" || kind == "reminder_rrule_v2"
}

internal data class ExistingReminderEntry(
    val calendarEventId: Long,
    val key: String?
)

internal data class ReminderDiscoveryDescriptionCandidate(
    val calendarEventId: Long,
    val description: String
)

internal data class ReminderDiscoveryMetadataCandidate(
    val calendarEventId: Long,
    val kind: String?,
    val occurrenceEpochDay: Long?,
    val daysLeft: Int?
)

internal sealed interface ReminderDiscoveryResult {
    data class Success(val entries: List<ExistingReminderEntry>) : ReminderDiscoveryResult
    data class Failure(val error: String) : ReminderDiscoveryResult
}

internal fun discoverExistingReminderEntries(
    readGranted: Boolean,
    queryDescriptionCandidates: () -> List<ReminderDiscoveryDescriptionCandidate>?,
    queryMetadataCandidates: () -> List<ReminderDiscoveryMetadataCandidate>?,
    eventId: Int
): ReminderDiscoveryResult {
    if (!readGranted) {
        return ReminderDiscoveryResult.Failure("Calendar read permission required")
    }
    return try {
        val descriptions = queryDescriptionCandidates()
            ?: return ReminderDiscoveryResult.Failure("Calendar events query returned no cursor")
        val metadata = queryMetadataCandidates()
            ?: return ReminderDiscoveryResult.Failure(
                "Calendar extended-properties query returned no cursor"
            )
        val entries = mutableListOf<ExistingReminderEntry>()
        descriptions.forEach { candidate ->
            if (
                ScheduleSyncManager.isManagedCalendarDescriptionForEvent(
                    description = candidate.description,
                    eventId = eventId,
                    includeReminders = true,
                    includeMilestones = false
                )
            ) {
                entries += ExistingReminderEntry(
                    calendarEventId = candidate.calendarEventId,
                    key = ScheduleSyncManager.reminderKeyFromDescription(candidate.description)
                )
            }
        }
        metadata.forEach { candidate ->
            if (entries.any { it.calendarEventId == candidate.calendarEventId }) return@forEach
            if (!isManagedReminderMetadataKind(candidate.kind)) return@forEach
            val key = if (candidate.occurrenceEpochDay != null && candidate.daysLeft != null) {
                ScheduleSyncManager.buildReminderEntryKey(
                    occurrenceEpochDay = candidate.occurrenceEpochDay,
                    daysLeft = candidate.daysLeft,
                    useRRule = candidate.kind == "reminder_rrule_v2"
                )
            } else {
                null
            }
            entries += ExistingReminderEntry(candidate.calendarEventId, key)
        }
        ReminderDiscoveryResult.Success(entries)
    } catch (_: SecurityException) {
        ReminderDiscoveryResult.Failure("Calendar permission denied")
    } catch (t: Throwable) {
        ReminderDiscoveryResult.Failure(
            (t.message ?: "Calendar provider discovery failed").take(180)
        )
    }
}

internal fun syncAfterReminderDiscovery(
    event: Event,
    lastSyncAt: Long,
    discovery: ReminderDiscoveryResult,
    sync: (List<ExistingReminderEntry>) -> ScheduleSyncManager.ScheduleSyncResult
): ScheduleSyncManager.ScheduleSyncResult = when (discovery) {
    is ReminderDiscoveryResult.Success -> sync(discovery.entries)
    is ReminderDiscoveryResult.Failure -> ScheduleSyncManager.ScheduleSyncResult(
        primaryScheduleEventId = event.scheduleEventId,
        targetCalendarId = event.targetCalendarId,
        lastSyncAt = lastSyncAt,
        error = discovery.error
    )
}

internal sealed interface WritableCalendarDiscoveryResult {
    data class Success(
        val calendars: List<ScheduleSyncManager.CalendarOption>
    ) : WritableCalendarDiscoveryResult

    data class Failure(val error: String) : WritableCalendarDiscoveryResult
}

internal fun discoverWritableCalendars(
    queryDetailed: () -> List<ScheduleSyncManager.CalendarOption>?,
    queryLegacy: () -> List<ScheduleSyncManager.CalendarOption>?
): WritableCalendarDiscoveryResult {
    return try {
        val detailed = queryDetailed()
            ?: return WritableCalendarDiscoveryResult.Failure(
                "Calendar query returned no cursor"
            )
        if (detailed.isNotEmpty()) {
            WritableCalendarDiscoveryResult.Success(detailed)
        } else {
            val legacy = queryLegacy()
                ?: return WritableCalendarDiscoveryResult.Failure(
                    "Calendar query returned no cursor"
                )
            WritableCalendarDiscoveryResult.Success(legacy)
        }
    } catch (_: SecurityException) {
        WritableCalendarDiscoveryResult.Failure("Calendar permission denied")
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (t: Throwable) {
        WritableCalendarDiscoveryResult.Failure(
            (t.message ?: "Calendar provider discovery failed").take(180)
        )
    }
}

internal fun syncAfterWritableCalendarDiscovery(
    event: Event,
    preferredCalendarId: Long?,
    lastSyncAt: Long,
    discovery: WritableCalendarDiscoveryResult,
    onNoWritableCalendar: () -> ScheduleSyncManager.ScheduleSyncResult,
    onWritableCalendar: (Long) -> ScheduleSyncManager.ScheduleSyncResult
): ScheduleSyncManager.ScheduleSyncResult = when (discovery) {
    is WritableCalendarDiscoveryResult.Failure -> ScheduleSyncManager.ScheduleSyncResult(
        primaryScheduleEventId = event.scheduleEventId,
        targetCalendarId = event.targetCalendarId,
        lastSyncAt = lastSyncAt,
        error = discovery.error
    )

    is WritableCalendarDiscoveryResult.Success -> {
        val writableIds = discovery.calendars.mapTo(mutableSetOf()) { it.id }
        val resolvedCalendarId = preferredCalendarId?.takeIf { it in writableIds }
            ?: event.targetCalendarId?.takeIf { it in writableIds }
            ?: discovery.calendars.firstOrNull()?.id
        if (resolvedCalendarId == null) {
            onNoWritableCalendar()
        } else {
            onWritableCalendar(resolvedCalendarId)
        }
    }
}

object ScheduleSyncManager {

    internal const val ERROR_NO_WRITABLE_CALENDAR = "No writable calendar"

    data class CalendarOption(
        val id: Long,
        val displayName: String,
        val accountName: String?,
        internal val accessLevel: Int? = null,
        internal val isMarkedWritable: Boolean = false
    ) {
        val label: String
            get() = accountName?.takeIf { it.isNotBlank() }?.let { "$displayName ($it)" } ?: displayName
    }

    data class ScheduleSyncResult(
        val primaryScheduleEventId: Long?,
        val targetCalendarId: Long?,
        val lastSyncAt: Long,
        val error: String?
    )

    data class MilestoneScheduleSyncResult(
        val scheduleEventId: Long?,
        val targetCalendarId: Long?,
        val lastSyncAt: Long,
        val error: String?
    )

    private const val MILESTONE_MARKER_PREFIX = "[TimeAPK][Milestone]"
    private const val REMINDER_MARKER_PREFIX = "[TimeAPK][Reminder]"
    private val REMINDER_V2_SUFFIX = Regex(""":v2:occ=(-?\d+):days=(-?\d+):rr=[01]""")
    private val MILESTONE_V2_SUFFIX = Regex(""":v2:trigger=(-?\d+)""")

    private const val META_NAME_KIND = "timeapk_kind"
    private const val META_NAME_EVENT_ID = "timeapk_event_id"
    private const val META_NAME_OCC_EPOCH_DAY = "timeapk_occ_epoch_day"
    private const val META_NAME_DAYS_LEFT = "timeapk_days_left"
    private const val META_NAME_SCHEMA_VERSION = "timeapk_schema_version"

    private const val META_KIND_REMINDER = "reminder_v2"
    private const val META_KIND_REMINDER_RRULE = "reminder_rrule_v2"
    private const val META_KIND_MILESTONE = "milestone_v2"
    private const val META_SCHEMA_VERSION = "2"
    private const val TAG = "ScheduleSyncManager"

    private data class ExpectedReminderEntry(
        val key: String,
        val triggerAtMillis: Long,
        val occurrenceEpochDay: Long,
        val daysLeft: Int,
        val useRRule: Boolean,
        val title: String,
        val marker: String,
        val note: String
    )

    internal data class ReminderSyncPlanEntry(
        val key: String,
        val triggerAtMillis: Long,
        val occurrenceEpochDay: Long,
        val daysLeft: Int,
        val useRRule: Boolean
    )

    private fun milestoneMarker(eventId: Int, triggerEpochDay: Long): String =
        "$MILESTONE_MARKER_PREFIX:$eventId:v2:trigger=$triggerEpochDay"

    private fun reminderMarker(
        eventId: Int,
        occurrenceEpochDay: Long,
        daysLeft: Int,
        useRRule: Boolean
    ): String {
        val rr = if (useRRule) 1 else 0
        return "$REMINDER_MARKER_PREFIX:$eventId:v2:occ=$occurrenceEpochDay:days=$daysLeft:rr=$rr"
    }

    suspend fun hasWritableCalendar(context: Context): Boolean = getWritableCalendars(context).isNotEmpty()

    internal fun isNoWritableCalendarError(error: String?): Boolean {
        return error == ERROR_NO_WRITABLE_CALENDAR
    }

    suspend fun getWritableCalendars(context: Context): List<CalendarOption> = withContext(Dispatchers.IO) {
        when (val discovery = loadWritableCalendars(context)) {
            is WritableCalendarDiscoveryResult.Success -> discovery.calendars.also { calendars ->
                if (calendars.isEmpty()) {
                    Log.w(TAG, "Calendar provider returned no calendars")
                } else if (calendars.none { it.isMarkedWritable }) {
                    Log.w(
                        TAG,
                        "No calendars were marked writable by provider; falling back to available calendars: ${calendars.joinToString { it.debugSummary() }}"
                    )
                }
            }

            is WritableCalendarDiscoveryResult.Failure -> {
                Log.w(TAG, "Failed to query writable calendars: ${discovery.error}")
                emptyList()
            }
        }
    }

    suspend fun getDefaultCalendarId(context: Context): Long? =
        getWritableCalendars(context).firstOrNull()?.id

    internal fun cleanupReminderSeriesEntries(
        expectedEntriesPresent: Boolean,
        staleIds: Iterable<Long>,
        cleanupWholeSeries: () -> CalendarCleanupResult,
        cleanupSingleEntry: (Long) -> CalendarCleanupResult
    ): CalendarCleanupResult {
        if (!expectedEntriesPresent) return cleanupWholeSeries()

        var firstFailure: CalendarCleanupResult? = null
        staleIds.forEach { staleId ->
            val result = cleanupSingleEntry(staleId)
            if (!result.isSuccess && firstFailure == null) {
                firstFailure = result
            }
        }
        return firstFailure ?: CalendarCleanupResult.RemovedOrNotPresent
    }

    internal fun scheduleSyncResultAfterCleanup(
        event: Event,
        primaryScheduleEventId: Long?,
        targetCalendarId: Long?,
        lastSyncAt: Long,
        cleanupResult: CalendarCleanupResult,
        baseError: String? = null
    ): ScheduleSyncResult {
        val cleanupFailed = !cleanupResult.isSuccess
        val error = listOfNotNull(baseError, cleanupResult.message)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("; ")
            .ifBlank { null }
        return ScheduleSyncResult(
            primaryScheduleEventId = if (cleanupFailed) {
                primaryScheduleEventId ?: event.scheduleEventId
            } else {
                primaryScheduleEventId
            },
            targetCalendarId = if (cleanupFailed) {
                targetCalendarId ?: event.targetCalendarId
            } else {
                targetCalendarId
            },
            lastSyncAt = lastSyncAt,
            error = error
        )
    }

    suspend fun syncReminderSeries(
        context: Context,
        event: Event,
        preferredCalendarId: Long?,
        useRRuleSync: Boolean
    ): ScheduleSyncResult = withScheduleEventProviderLock(event.id) {
        syncReminderSeriesLocked(context, event, preferredCalendarId, useRRuleSync)
    }

    private fun syncReminderSeriesLocked(
        context: Context,
        event: Event,
        preferredCalendarId: Long?,
        useRRuleSync: Boolean
    ): ScheduleSyncResult {
        val lastSyncAt = System.currentTimeMillis()
        val sanitizedEvent = event.sanitizedReminderConfig()

        return try {
            if (!sanitizedEvent.syncToScheduleEnabled) {
                val cleanupResult = cleanupReminderSeriesEntries(
                    expectedEntriesPresent = false,
                    staleIds = emptySet(),
                    cleanupWholeSeries = {
                        removeReminderSeriesEntries(
                            context = context,
                            eventId = sanitizedEvent.id,
                            calendarEventId = sanitizedEvent.scheduleEventId
                        )
                    },
                    cleanupSingleEntry = { staleId -> removeScheduleReminder(context, staleId) }
                )
                scheduleSyncResultAfterCleanup(
                    event = sanitizedEvent,
                    primaryScheduleEventId = null,
                    targetCalendarId = null,
                    lastSyncAt = lastSyncAt,
                    cleanupResult = cleanupResult
                )
            } else {
                syncAfterWritableCalendarDiscovery(
                    event = sanitizedEvent,
                    preferredCalendarId = preferredCalendarId,
                    lastSyncAt = lastSyncAt,
                    discovery = loadWritableCalendars(context),
                    onNoWritableCalendar = {
                        val cleanupResult = cleanupReminderSeriesEntries(
                            expectedEntriesPresent = false,
                            staleIds = emptySet(),
                            cleanupWholeSeries = {
                                removeReminderSeriesEntries(
                                    context = context,
                                    eventId = sanitizedEvent.id,
                                    calendarEventId = sanitizedEvent.scheduleEventId
                                )
                            },
                            cleanupSingleEntry = { staleId -> removeScheduleReminder(context, staleId) }
                        )
                        scheduleSyncResultAfterCleanup(
                            event = sanitizedEvent,
                            primaryScheduleEventId = null,
                            targetCalendarId = null,
                            lastSyncAt = lastSyncAt,
                            cleanupResult = cleanupResult,
                            baseError = ERROR_NO_WRITABLE_CALENDAR
                        )
                    },
                    onWritableCalendar = { targetCalendarId ->
                        val expectedEntries = buildExpectedReminderEntries(
                            context = context,
                            event = sanitizedEvent,
                            useRRuleSync = useRRuleSync
                        )
                        val discovery = loadExistingReminderEntries(context, sanitizedEvent.id)
                        syncAfterReminderDiscovery(
                            event = sanitizedEvent,
                            lastSyncAt = lastSyncAt,
                            discovery = discovery
                        ) { existing ->
                        var metadataWarnings = 0
                        val existingByKey = existing.mapNotNull { entry ->
                            entry.key?.let { key -> key to entry.calendarEventId }
                        }.toMap()
                        val allExistingIds = existing.map { it.calendarEventId }.toMutableSet()
                        val usedIds = mutableSetOf<Long>()
                        var primaryId: Long? = null

                        expectedEntries.sortedBy { it.triggerAtMillis }.forEach { entry ->
                            val values = ContentValues().apply {
                                put(CalendarContract.Events.DTSTART, entry.triggerAtMillis)
                                put(CalendarContract.Events.DTEND, entry.triggerAtMillis + 60 * 60 * 1000L)
                                put(CalendarContract.Events.TITLE, entry.title)
                                put(CalendarContract.Events.DESCRIPTION, buildMarkedDescription(entry.marker, entry.note))
                                put(CalendarContract.Events.CALENDAR_ID, targetCalendarId)
                                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                                if (entry.useRRule) {
                                    put(CalendarContract.Events.RRULE, buildRRuleForEvent(sanitizedEvent))
                                } else {
                                    putNull(CalendarContract.Events.RRULE)
                                }
                            }

                            val existingId = existingByKey[entry.key]
                            val eventId = if (
                                existingId != null &&
                                updateEventAndReminder(context, existingId, values)
                            ) {
                                existingId
                            } else {
                                insertEventAndReminder(context, values)
                            }

                            if (eventId != null) {
                                val metadataSaved = upsertExtendedProperties(
                                    context = context,
                                    eventId = eventId,
                                    properties = mapOf(
                                        META_NAME_KIND to if (entry.useRRule) META_KIND_REMINDER_RRULE else META_KIND_REMINDER,
                                        META_NAME_EVENT_ID to sanitizedEvent.id.toString(),
                                        META_NAME_OCC_EPOCH_DAY to entry.occurrenceEpochDay.toString(),
                                        META_NAME_DAYS_LEFT to entry.daysLeft.toString(),
                                        META_NAME_SCHEMA_VERSION to META_SCHEMA_VERSION
                                    )
                                )
                                if (!metadataSaved) {
                                    metadataWarnings += 1
                                }
                                usedIds += eventId
                                if (primaryId == null) {
                                    primaryId = eventId
                                }
                            }
                        }

                        val cleanupResult = cleanupReminderSeriesEntries(
                            expectedEntriesPresent = expectedEntries.isNotEmpty(),
                            staleIds = allExistingIds - usedIds,
                            cleanupWholeSeries = {
                                removeReminderSeriesEntries(
                                    context = context,
                                    eventId = sanitizedEvent.id,
                                    calendarEventId = sanitizedEvent.scheduleEventId
                                )
                            },
                            cleanupSingleEntry = { staleId -> removeScheduleReminder(context, staleId) }
                        )
                        if (!cleanupResult.isSuccess) {
                            return@syncAfterReminderDiscovery scheduleSyncResultAfterCleanup(
                                event = sanitizedEvent,
                                primaryScheduleEventId = primaryId,
                                targetCalendarId = targetCalendarId,
                                lastSyncAt = lastSyncAt,
                                cleanupResult = cleanupResult
                            )
                        }
                        if (metadataWarnings > 0) {
                            Log.w(
                                TAG,
                                "Synced calendar event(s) for eventId=${sanitizedEvent.id} with $metadataWarnings extended-property warning(s); falling back to description markers"
                            )
                        }
                        if (expectedEntries.isNotEmpty() && primaryId == null) {
                            val legacyResult = tryLegacyReminderInsert(
                                context = context,
                                event = sanitizedEvent,
                                preferredCalendarId = preferredCalendarId,
                                useRRuleSync = useRRuleSync,
                                lastSyncAt = lastSyncAt,
                                reason = "Series sync produced no calendar event"
                            )
                            if (legacyResult != null) {
                                return@syncAfterReminderDiscovery legacyResult
                            }
                            Log.w(
                                TAG,
                                "All calendar inserts failed for eventId=${sanitizedEvent.id} on calendarId=$targetCalendarId; treating as no writable calendar"
                            )
                            return@syncAfterReminderDiscovery ScheduleSyncResult(
                                primaryScheduleEventId = null,
                                targetCalendarId = targetCalendarId,
                                lastSyncAt = lastSyncAt,
                                error = ERROR_NO_WRITABLE_CALENDAR
                            )
                        }

                        scheduleSyncResultAfterCleanup(
                            event = sanitizedEvent,
                            primaryScheduleEventId = primaryId,
                            targetCalendarId = targetCalendarId.takeIf { expectedEntries.isNotEmpty() },
                            lastSyncAt = lastSyncAt,
                            cleanupResult = cleanupResult
                        )
                        }
                    }
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            ScheduleSyncResult(
                primaryScheduleEventId = null,
                targetCalendarId = null,
                lastSyncAt = lastSyncAt,
                error = "Calendar permission denied"
            )
        } catch (t: Throwable) {
            tryLegacyReminderInsert(
                context = context,
                event = sanitizedEvent,
                preferredCalendarId = preferredCalendarId,
                useRRuleSync = useRRuleSync,
                lastSyncAt = lastSyncAt,
                reason = t.message ?: t::class.java.simpleName
            )?.let { return it }
            ScheduleSyncResult(
                primaryScheduleEventId = null,
                targetCalendarId = null,
                lastSyncAt = lastSyncAt,
                error = (t.message ?: "Unknown schedule sync error").take(180)
            )
        }
    }

    suspend fun upsertScheduleReminder(
        context: Context,
        event: Event,
        currentScheduleEventId: Long? = event.scheduleEventId,
        preferredCalendarId: Long? = null,
        useRRuleSync: Boolean = true
    ): Long? {
        val result = syncReminderSeries(
            context = context,
            event = event.copy(scheduleEventId = currentScheduleEventId),
            preferredCalendarId = preferredCalendarId,
            useRRuleSync = useRRuleSync
        )
        return result.primaryScheduleEventId
    }

    suspend fun insertScheduleReminder(context: Context, event: Event): Long? {
        return upsertScheduleReminder(
            context = context,
            event = event,
            currentScheduleEventId = event.scheduleEventId
        )
    }

    suspend fun insertMilestoneScheduleReminder(
        context: Context,
        eventId: Int,
        title: String,
        description: String,
        triggerAtMillis: Long,
        targetCalendarId: Long? = null
    ): Long? {
        return insertMilestoneScheduleReminderWithStatus(
            context = context,
            eventId = eventId,
            title = title,
            description = description,
            triggerAtMillis = triggerAtMillis,
            targetCalendarId = targetCalendarId
        ).scheduleEventId
    }

    suspend fun insertMilestoneScheduleReminderWithStatus(
        context: Context,
        eventId: Int,
        title: String,
        description: String,
        triggerAtMillis: Long,
        targetCalendarId: Long? = null
    ): MilestoneScheduleSyncResult = insertMilestoneScheduleReminderAttempt(
        context = context,
        eventId = eventId,
        title = title,
        description = description,
        triggerAtMillis = triggerAtMillis,
        targetCalendarId = targetCalendarId
    ).result

    internal suspend fun insertMilestoneScheduleReminderAttempt(
        context: Context,
        eventId: Int,
        title: String,
        description: String,
        triggerAtMillis: Long,
        targetCalendarId: Long? = null
    ): MilestoneCalendarInsertionAttempt = withScheduleEventProviderLock(eventId) {
        insertMilestoneScheduleReminderAttemptLocked(
            context = context,
            eventId = eventId,
            title = title,
            description = description,
            triggerAtMillis = triggerAtMillis,
            targetCalendarId = targetCalendarId
        )
    }

    private fun insertMilestoneScheduleReminderAttemptLocked(
        context: Context,
        eventId: Int,
        title: String,
        description: String,
        triggerAtMillis: Long,
        targetCalendarId: Long? = null
    ): MilestoneCalendarInsertionAttempt {
        val lastSyncAt = System.currentTimeMillis()
        var providerEventMayExist = false
        return try {
            val resolvedCalendarId = if (targetCalendarId != null) {
                targetCalendarId
            } else {
                when (val discovery = loadWritableCalendars(context)) {
                    is WritableCalendarDiscoveryResult.Success -> discovery.calendars.firstOrNull()?.id
                    is WritableCalendarDiscoveryResult.Failure -> {
                        return MilestoneCalendarInsertionAttempt(
                            result = MilestoneScheduleSyncResult(
                                scheduleEventId = null,
                                targetCalendarId = null,
                                lastSyncAt = lastSyncAt,
                                error = discovery.error
                            ),
                            providerEventMayExist = false
                        )
                    }
                }
            } ?: return MilestoneCalendarInsertionAttempt(
                    result = MilestoneScheduleSyncResult(
                        scheduleEventId = null,
                        targetCalendarId = null,
                        lastSyncAt = lastSyncAt,
                        error = ERROR_NO_WRITABLE_CALENDAR
                    ),
                    providerEventMayExist = false
                )
            val triggerDate = Instant.ofEpochMilli(triggerAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            val marker = milestoneMarker(eventId, triggerDate.toEpochDay())
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, triggerAtMillis)
                put(CalendarContract.Events.DTEND, triggerAtMillis + 60 * 60 * 1000L)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, buildMarkedDescription(marker, description))
                put(CalendarContract.Events.CALENDAR_ID, resolvedCalendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }
            val insertedId = insertMilestoneEventAndReminder(context, values) {
                providerEventMayExist = true
            }
            val result = if (insertedId == null) {
                MilestoneScheduleSyncResult(
                    scheduleEventId = null,
                    targetCalendarId = resolvedCalendarId,
                    lastSyncAt = lastSyncAt,
                    error = "Milestone schedule insert failed"
                )
            } else {
                val metadataSaved = upsertExtendedProperties(
                    context = context,
                    eventId = insertedId,
                    properties = mapOf(
                        META_NAME_KIND to META_KIND_MILESTONE,
                        META_NAME_EVENT_ID to eventId.toString(),
                        META_NAME_OCC_EPOCH_DAY to triggerDate.toEpochDay().toString(),
                        META_NAME_DAYS_LEFT to "0",
                        META_NAME_SCHEMA_VERSION to META_SCHEMA_VERSION
                    )
                )
                if (!metadataSaved) {
                    Log.w(
                        TAG,
                        "Synced milestone calendar event for eventId=$eventId without extended properties; falling back to description markers"
                    )
                }
                MilestoneScheduleSyncResult(
                    scheduleEventId = insertedId,
                    targetCalendarId = resolvedCalendarId,
                    lastSyncAt = lastSyncAt,
                    error = null
                )
            }
            MilestoneCalendarInsertionAttempt(result, providerEventMayExist)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: SecurityException) {
            MilestoneCalendarInsertionAttempt(
                result = MilestoneScheduleSyncResult(
                    scheduleEventId = null,
                    targetCalendarId = null,
                    lastSyncAt = lastSyncAt,
                    error = "Calendar permission denied"
                ),
                providerEventMayExist = providerEventMayExist
            )
        } catch (t: Throwable) {
            MilestoneCalendarInsertionAttempt(
                result = MilestoneScheduleSyncResult(
                    scheduleEventId = null,
                    targetCalendarId = null,
                    lastSyncAt = lastSyncAt,
                    error = (t.message ?: "Unknown milestone schedule sync error").take(180)
                ),
                providerEventMayExist = providerEventMayExist
            )
        }
    }

    suspend fun clearAllMilestoneScheduleReminders(context: Context): CalendarCleanupResult =
        withContext(Dispatchers.IO) {
            clearAllMilestoneScheduleRemindersBlocking(context)
        }

    internal fun clearAllMilestoneScheduleRemindersBlocking(
        context: Context
    ): CalendarCleanupResult {
        return cleanupAllMilestoneEntries(
            readGranted = hasPermission(context, Manifest.permission.READ_CALENDAR),
            writeGranted = hasCalendarWriteAccess(context),
            queryMetadataIds = { queryMilestoneIdsByExtendedProperty(context) },
            queryDescriptionIds = { queryMilestoneIdsByDescription(context) },
            deleteEvent = { id ->
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)
                context.contentResolver.delete(uri, null, null)
            }
        )
    }

    internal fun cleanupAllMilestoneEntries(
        readGranted: Boolean,
        writeGranted: Boolean,
        queryMetadataIds: () -> Iterable<Long>?,
        queryDescriptionIds: () -> Iterable<Long>?,
        deleteEvent: (Long) -> Unit
    ): CalendarCleanupResult {
        return try {
            if (!hasRequiredCalendarCleanupPermissions(readGranted, writeGranted)) {
                return CalendarCleanupResult.PermissionRequired
            }
            val ids = linkedSetOf<Long>()
            ids += requireCalendarCleanupQuery(
                queryMetadataIds(),
                "Calendar extended-properties"
            )
            ids += requireCalendarCleanupQuery(
                queryDescriptionIds(),
                "Calendar events"
            )
            ids.forEach(deleteEvent)
            CalendarCleanupResult.RemovedOrNotPresent
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Exception) {
            calendarCleanupFailureFor(t)
        }
    }

    suspend fun clearMilestoneScheduleRemindersByEventId(
        context: Context,
        eventId: Int
    ): CalendarCleanupResult {
        return withScheduleEventProviderLock(eventId) {
            clearMilestoneScheduleRemindersByEventIdLocked(context, eventId)
        }
    }

    internal fun clearMilestoneScheduleRemindersByEventIdLocked(
        context: Context,
        eventId: Int
    ): CalendarCleanupResult = cleanupManagedCalendarEntries(
        gateway = ContextCalendarCleanupGateway(context),
        eventId = eventId,
        calendarEventId = null,
        includeReminders = false,
        includeMilestones = true
    )

    suspend fun removeScheduleReminderByEventId(
        context: Context,
        eventId: Int
    ): CalendarCleanupResult {
        return withScheduleEventProviderLock(eventId) {
            removeScheduleReminderByEventId(
                gateway = ContextCalendarCleanupGateway(context),
                eventId = eventId
            )
        }
    }

    private fun removeReminderSeriesEntries(
        context: Context,
        eventId: Int,
        calendarEventId: Long?
    ): CalendarCleanupResult {
        return cleanupManagedCalendarEntries(
            gateway = ContextCalendarCleanupGateway(context),
            eventId = eventId,
            calendarEventId = calendarEventId,
            includeReminders = true,
            includeMilestones = false
        )
    }

    internal fun removeScheduleReminderByEventId(
        gateway: CalendarCleanupGateway,
        eventId: Int
    ): CalendarCleanupResult {
        return cleanupManagedCalendarEntries(
            gateway = gateway,
            eventId = eventId,
            calendarEventId = null,
            includeReminders = true,
            includeMilestones = false
        )
    }

    private fun removeScheduleReminder(context: Context, calendarEventId: Long?): CalendarCleanupResult {
        if (calendarEventId == null) return CalendarCleanupResult.RemovedOrNotPresent
        return try {
            if (!hasCalendarWriteAccess(context)) {
                return CalendarCleanupResult.PermissionRequired
            }
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventId)
            context.contentResolver.delete(uri, null, null)
            CalendarCleanupResult.RemovedOrNotPresent
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Exception) {
            calendarCleanupFailureFor(t)
        }
    }

    suspend fun removeManagedCalendarEntries(
        context: Context,
        eventId: Int,
        calendarEventId: Long?
    ): CalendarCleanupResult {
        return withScheduleEventProviderLock(eventId) {
            removeManagedCalendarEntries(
                gateway = ContextCalendarCleanupGateway(context),
                eventId = eventId,
                calendarEventId = calendarEventId
            )
        }
    }

    internal fun removeManagedCalendarEntries(
        gateway: CalendarCleanupGateway,
        eventId: Int,
        calendarEventId: Long?
    ): CalendarCleanupResult {
        return cleanupManagedCalendarEntries(
            gateway = gateway,
            eventId = eventId,
            calendarEventId = calendarEventId,
            includeReminders = true,
            includeMilestones = true
        )
    }

    private fun cleanupManagedCalendarEntries(
        gateway: CalendarCleanupGateway,
        eventId: Int,
        calendarEventId: Long?,
        includeReminders: Boolean,
        includeMilestones: Boolean
    ): CalendarCleanupResult {
        return try {
            if (
                !hasRequiredCalendarCleanupPermissions(
                    readGranted = gateway.hasReadPermission(),
                    writeGranted = gateway.hasWritePermission()
                )
            ) {
                return CalendarCleanupResult.PermissionRequired
            }
            val ids = linkedSetOf<Long>()
            calendarEventId?.let(ids::add)
            requireCalendarCleanupQuery(
                gateway.queryDescriptionCandidates(eventId, includeReminders, includeMilestones),
                "Calendar events"
            ).forEach { candidate ->
                if (
                    isManagedCalendarDescriptionForEvent(
                        description = candidate.description,
                        eventId = eventId,
                        includeReminders = includeReminders,
                        includeMilestones = includeMilestones
                    )
                ) {
                    ids += candidate.calendarEventId
                }
            }

            requireCalendarCleanupQuery(
                gateway.queryMetadataCandidates(eventId),
                "Calendar extended-properties"
            ).forEach { candidate ->
                if (
                    isManagedCalendarMetadataKind(
                        kind = candidate.kind,
                        includeReminders = includeReminders,
                        includeMilestones = includeMilestones
                    )
                ) {
                    ids += candidate.calendarEventId
                }
            }

            ids.forEach(gateway::deleteEvent)
            CalendarCleanupResult.RemovedOrNotPresent
        } catch (t: Exception) {
            calendarCleanupFailureFor(t)
        }
    }

    internal fun isManagedCalendarDescriptionForEvent(
        description: String,
        eventId: Int,
        includeReminders: Boolean = true,
        includeMilestones: Boolean = true
    ): Boolean {
        val marker = description.lineSequence().firstOrNull().orEmpty().removeSuffix("\r")
        return (includeReminders && isManagedReminderMarker(marker, eventId)) ||
            (includeMilestones && isManagedMilestoneMarker(marker, eventId))
    }

    private fun isManagedReminderMarker(marker: String, eventId: Int): Boolean {
        val baseMarker = "$REMINDER_MARKER_PREFIX:$eventId"
        if (marker == baseMarker) return true
        val match = REMINDER_V2_SUFFIX.matchEntire(marker.removePrefix(baseMarker)) ?: return false
        return marker.startsWith(baseMarker) &&
            match.groupValues[1].toLongOrNull() != null &&
            match.groupValues[2].toIntOrNull() != null
    }

    private fun isManagedMilestoneMarker(marker: String, eventId: Int): Boolean {
        val baseMarker = "$MILESTONE_MARKER_PREFIX:$eventId"
        if (marker == baseMarker) return true
        val match = MILESTONE_V2_SUFFIX.matchEntire(marker.removePrefix(baseMarker)) ?: return false
        return marker.startsWith(baseMarker) && match.groupValues[1].toLongOrNull() != null
    }

    private class ContextCalendarCleanupGateway(
        private val context: Context
    ) : CalendarCleanupGateway {
        override fun hasReadPermission(): Boolean {
            return ScheduleSyncManager.hasPermission(context, Manifest.permission.READ_CALENDAR)
        }

        override fun hasWritePermission(): Boolean {
            return ScheduleSyncManager.hasPermission(context, Manifest.permission.WRITE_CALENDAR)
        }

        override fun queryDescriptionCandidates(
            eventId: Int,
            includeReminders: Boolean,
            includeMilestones: Boolean
        ): List<CalendarCleanupDescriptionCandidate>? {
            val markerPrefixes = buildList {
                if (includeReminders) add(REMINDER_MARKER_PREFIX)
                if (includeMilestones) add(MILESTONE_MARKER_PREFIX)
            }
            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(CalendarContract.Events._ID, CalendarContract.Events.DESCRIPTION),
                markerPrefixes.joinToString(" OR ") { "${CalendarContract.Events.DESCRIPTION} LIKE ?" },
                markerPrefixes.map { "$it:$eventId%" }.toTypedArray(),
                null
            ) ?: return null
            return cursor.use {
                val idIndex = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val descriptionIndex = it.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
                buildList {
                    while (it.moveToNext()) {
                        add(
                            CalendarCleanupDescriptionCandidate(
                                calendarEventId = it.getLong(idIndex),
                                description = it.getString(descriptionIndex).orEmpty()
                            )
                        )
                    }
                }
            }
        }

        override fun queryMetadataCandidates(eventId: Int): List<CalendarCleanupMetadataCandidate>? {
            val candidateIdsCursor = context.contentResolver.query(
                CalendarContract.ExtendedProperties.CONTENT_URI,
                arrayOf(CalendarContract.ExtendedProperties.EVENT_ID),
                "${CalendarContract.ExtendedProperties.NAME} = ? AND ${CalendarContract.ExtendedProperties.VALUE} = ?",
                arrayOf(META_NAME_EVENT_ID, eventId.toString()),
                null
            ) ?: return null
            val candidateIds = candidateIdsCursor.use {
                val idIndex = it.getColumnIndexOrThrow(CalendarContract.ExtendedProperties.EVENT_ID)
                buildList {
                    while (it.moveToNext()) add(it.getLong(idIndex))
                }
            }
            val candidates = mutableListOf<CalendarCleanupMetadataCandidate>()
            for (candidateId in candidateIds) {
                val kindCursor = context.contentResolver.query(
                    CalendarContract.ExtendedProperties.CONTENT_URI,
                    arrayOf(CalendarContract.ExtendedProperties.VALUE),
                    "${CalendarContract.ExtendedProperties.EVENT_ID} = ? AND ${CalendarContract.ExtendedProperties.NAME} = ?",
                    arrayOf(candidateId.toString(), META_NAME_KIND),
                    null
                ) ?: return null
                kindCursor.use {
                    val valueIndex = it.getColumnIndexOrThrow(CalendarContract.ExtendedProperties.VALUE)
                    while (it.moveToNext()) {
                        val kind = it.getString(valueIndex)
                        if (isManagedCalendarMetadataKind(kind, true, true)) {
                            candidates += CalendarCleanupMetadataCandidate(
                                calendarEventId = candidateId,
                                kind = kind
                            )
                        }
                    }
                }
            }
            return candidates
        }

        override fun deleteEvent(calendarEventId: Long) {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventId)
            context.contentResolver.delete(uri, null, null)
        }
    }

    internal fun isManagedCalendarMetadataKind(
        kind: String?,
        includeReminders: Boolean,
        includeMilestones: Boolean
    ): Boolean {
        return (includeReminders && kind == META_KIND_REMINDER) ||
            (includeReminders && kind == META_KIND_REMINDER_RRULE) ||
            (includeMilestones && kind == META_KIND_MILESTONE)
    }

    internal fun hasRequiredCalendarCleanupPermissions(
        readGranted: Boolean,
        writeGranted: Boolean
    ): Boolean = readGranted && writeGranted

    internal fun calendarCleanupFailureFor(error: Exception): CalendarCleanupResult {
        return if (error is SecurityException) {
            CalendarCleanupResult.PermissionRequired
        } else {
            CalendarCleanupResult.ProviderFailure(
                (error.message ?: "Calendar provider cleanup failed").take(180)
            )
        }
    }

    private fun buildExpectedReminderEntries(
        context: Context,
        event: Event,
        useRRuleSync: Boolean
    ): List<ExpectedReminderEntry> {
        return buildReminderSyncPlan(
            event = event,
            useRRuleSync = useRRuleSync
        ).map { entry ->
            ExpectedReminderEntry(
                key = entry.key,
                triggerAtMillis = entry.triggerAtMillis,
                occurrenceEpochDay = entry.occurrenceEpochDay,
                daysLeft = entry.daysLeft,
                useRRule = entry.useRRule,
                title = buildReminderTitle(context, event, entry.daysLeft),
                marker = reminderMarker(event.id, entry.occurrenceEpochDay, entry.daysLeft, entry.useRRule),
                note = event.note
            )
        }
    }

    internal fun buildReminderSyncPlan(
        event: Event,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        useRRuleSync: Boolean
    ): List<ReminderSyncPlanEntry> {
        val useRRule = shouldUseRRuleSync(event, useRRuleSync)

        if (useRRule) {
            val nextTrigger = computeNextReminderTrigger(event, nowMillis, zoneId) ?: return emptyList()
            return listOf(
                ReminderSyncPlanEntry(
                    key = buildReminderEntryKey(
                        occurrenceEpochDay = 0L,
                        daysLeft = 0,
                        useRRule = true
                    ),
                    triggerAtMillis = nextTrigger.triggerAtMillis,
                    occurrenceEpochDay = 0L,
                    daysLeft = 0,
                    useRRule = true
                )
            )
        }

        val series = computeUpcomingReminderSeries(event, nowMillis, zoneId) ?: return emptyList()
        val reminderHour = event.reminderTimeMinutesOfDay / 60
        val reminderMinute = event.reminderTimeMinutesOfDay % 60
        val occurrenceEpochDay = series.occurrenceDate.toEpochDay()
        return series.entries.map { entry ->
            val remindAtMillis = entry.reminderDate
                .atTime(reminderHour, reminderMinute)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()

            ReminderSyncPlanEntry(
                key = buildReminderEntryKey(
                    occurrenceEpochDay = occurrenceEpochDay,
                    daysLeft = entry.daysLeft,
                    useRRule = false
                ),
                triggerAtMillis = remindAtMillis,
                occurrenceEpochDay = occurrenceEpochDay,
                daysLeft = entry.daysLeft,
                useRRule = false
            )
        }
            .filter { it.triggerAtMillis > nowMillis || it.daysLeft == 0 }
            .sortedBy { it.triggerAtMillis }
    }

    private fun shouldUseRRuleSync(event: Event, useRRuleSync: Boolean): Boolean {
        if (!useRRuleSync) return false
        if (event.isLunar) return false
        if (event.remindDaysBefore != 0) return false
        return event.repeatType in setOf(REPEAT_WEEKLY, REPEAT_MONTHLY, REPEAT_YEARLY)
    }

    private fun buildRRuleForEvent(event: Event): String {
        return when (event.repeatType) {
            REPEAT_WEEKLY -> "FREQ=WEEKLY;INTERVAL=1"
            REPEAT_MONTHLY -> "FREQ=MONTHLY;INTERVAL=1"
            REPEAT_YEARLY -> "FREQ=YEARLY;INTERVAL=1"
            else -> "FREQ=DAILY;INTERVAL=1"
        }
    }

    private fun buildReminderTitle(context: Context, event: Event, daysLeft: Int): String {
        return if (daysLeft == 0) {
            context.getString(R.string.schedule_reminder_title_today, event.title)
        } else {
            context.resources.getQuantityString(
                R.plurals.schedule_reminder_title_format,
                daysLeft,
                event.title,
                daysLeft
            )
        }
    }

    internal fun buildReminderEntryKey(
        occurrenceEpochDay: Long,
        daysLeft: Int,
        useRRule: Boolean
    ): String = "$occurrenceEpochDay:$daysLeft:${if (useRRule) 1 else 0}"

    internal fun reminderKeyFromDescription(description: String): String? {
        val marker = description.lineSequence().firstOrNull().orEmpty().removeSuffix("\r")
        return parseReminderKeyFromMarker(marker)
    }

    private fun parseReminderKeyFromMarker(marker: String): String? {
        val occ = Regex("""occ=(-?\d+)""").find(marker)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return null
        val days = Regex("""days=(-?\d+)""").find(marker)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
        val rr = Regex("""rr=(\d+)""").find(marker)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        return buildReminderEntryKey(
            occurrenceEpochDay = occ,
            daysLeft = days,
            useRRule = rr == 1
        )
    }

    private fun loadExistingReminderEntries(context: Context, eventId: Int): ReminderDiscoveryResult {
        return discoverExistingReminderEntries(
            readGranted = hasCalendarReadAccess(context),
            queryDescriptionCandidates = { queryReminderDescriptionCandidates(context, eventId) },
            queryMetadataCandidates = { queryReminderMetadataCandidates(context, eventId) },
            eventId = eventId
        ).also { result ->
            if (result is ReminderDiscoveryResult.Failure) {
                Log.w(
                    TAG,
                    "Failed to load existing reminder entries for eventId=$eventId: ${result.error}"
                )
            }
        }
    }

    private fun queryReminderDescriptionCandidates(
        context: Context,
        eventId: Int
    ): List<ReminderDiscoveryDescriptionCandidate>? {
        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID, CalendarContract.Events.DESCRIPTION),
            "${CalendarContract.Events.DESCRIPTION} LIKE ?",
            arrayOf("$REMINDER_MARKER_PREFIX:$eventId%"),
            null
        ) ?: return null
        return cursor.use {
            val idIndex = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val descriptionIndex = it.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
            buildList {
                while (it.moveToNext()) {
                    add(
                        ReminderDiscoveryDescriptionCandidate(
                            calendarEventId = it.getLong(idIndex),
                            description = it.getString(descriptionIndex).orEmpty()
                        )
                    )
                }
            }
        }
    }

    private fun queryReminderMetadataCandidates(
        context: Context,
        eventId: Int
    ): List<ReminderDiscoveryMetadataCandidate>? {
        val candidateIdsCursor = context.contentResolver.query(
            CalendarContract.ExtendedProperties.CONTENT_URI,
            arrayOf(CalendarContract.ExtendedProperties.EVENT_ID),
            "${CalendarContract.ExtendedProperties.NAME} = ? AND ${CalendarContract.ExtendedProperties.VALUE} = ?",
            arrayOf(META_NAME_EVENT_ID, eventId.toString()),
            null
        ) ?: return null
        val candidateIds = candidateIdsCursor.use {
            val idIndex = it.getColumnIndexOrThrow(CalendarContract.ExtendedProperties.EVENT_ID)
            buildList {
                while (it.moveToNext()) add(it.getLong(idIndex))
            }
        }
        return buildList {
            for (candidateId in candidateIds) {
                val propertiesCursor = context.contentResolver.query(
                    CalendarContract.ExtendedProperties.CONTENT_URI,
                    arrayOf(
                        CalendarContract.ExtendedProperties.NAME,
                        CalendarContract.ExtendedProperties.VALUE
                    ),
                    "${CalendarContract.ExtendedProperties.EVENT_ID} = ?",
                    arrayOf(candidateId.toString()),
                    null
                ) ?: return null
                val properties = propertiesCursor.use {
                    val nameIndex = it.getColumnIndexOrThrow(CalendarContract.ExtendedProperties.NAME)
                    val valueIndex = it.getColumnIndexOrThrow(CalendarContract.ExtendedProperties.VALUE)
                    buildMap {
                        while (it.moveToNext()) {
                            put(it.getString(nameIndex), it.getString(valueIndex))
                        }
                    }
                }
                add(
                    ReminderDiscoveryMetadataCandidate(
                        calendarEventId = candidateId,
                        kind = properties[META_NAME_KIND],
                        occurrenceEpochDay = properties[META_NAME_OCC_EPOCH_DAY]?.toLongOrNull(),
                        daysLeft = properties[META_NAME_DAYS_LEFT]?.toIntOrNull()
                    )
                )
            }
        }
    }

    private fun upsertExtendedProperties(context: Context, eventId: Long, properties: Map<String, String>): Boolean {
        if (!hasCalendarWriteAccess(context)) return false
        return try {
            properties.forEach { (name, value) ->
                context.contentResolver.delete(
                    CalendarContract.ExtendedProperties.CONTENT_URI,
                    "${CalendarContract.ExtendedProperties.EVENT_ID} = ? AND ${CalendarContract.ExtendedProperties.NAME} = ?",
                    arrayOf(eventId.toString(), name)
                )
                val values = ContentValues().apply {
                    put(CalendarContract.ExtendedProperties.EVENT_ID, eventId)
                    put(CalendarContract.ExtendedProperties.NAME, name)
                    put(CalendarContract.ExtendedProperties.VALUE, value)
                }
                val inserted = context.contentResolver.insert(CalendarContract.ExtendedProperties.CONTENT_URI, values)
                if (inserted == null) {
                    return false
                }
            }
            true
        } catch (t: SecurityException) {
            Log.w(TAG, "No calendar permission when writing extended properties", t)
            false
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to write extended properties for eventId=$eventId", t)
            false
        }
    }

    private fun insertEventAndReminder(context: Context, values: ContentValues): Long? {
        if (!hasCalendarWriteAccess(context)) return null
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return null
        val eventId = ContentUris.parseId(uri)
        upsertReminderAlert(context, eventId)
        return eventId
    }

    private fun insertMilestoneEventAndReminder(
        context: Context,
        values: ContentValues,
        onProviderEventCreated: () -> Unit
    ): Long? {
        if (!hasCalendarWriteAccess(context)) return null
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return null
        onProviderEventCreated()
        val eventId = ContentUris.parseId(uri)
        upsertReminderAlert(context, eventId)
        return eventId
    }

    private fun updateEventAndReminder(context: Context, eventId: Long, values: ContentValues): Boolean {
        if (!hasCalendarWriteAccess(context)) return false
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val affected = context.contentResolver.update(uri, values, null, null)
        if (affected <= 0) return false
        upsertReminderAlert(context, eventId)
        return true
    }

    private fun upsertReminderAlert(context: Context, eventId: Long) {
        if (!hasCalendarWriteAccess(context)) return
        val selection = "${CalendarContract.Reminders.EVENT_ID} = ?"
        val args = arrayOf(eventId.toString())
        context.contentResolver.delete(CalendarContract.Reminders.CONTENT_URI, selection, args)

        val reminderValues = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            put(CalendarContract.Reminders.MINUTES, 0)
        }
        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
    }

    private fun buildMarkedDescription(marker: String, note: String): String {
        return listOf(marker, note.takeIf { it.isNotBlank() }).joinToString("\n")
    }

    private fun queryMilestoneIdsByExtendedProperty(context: Context): List<Long>? {
        return context.contentResolver.query(
            CalendarContract.ExtendedProperties.CONTENT_URI,
            arrayOf(CalendarContract.ExtendedProperties.EVENT_ID),
            "${CalendarContract.ExtendedProperties.NAME} = ? AND ${CalendarContract.ExtendedProperties.VALUE} = ?",
            arrayOf(META_NAME_KIND, META_KIND_MILESTONE),
            null
        )?.use { cursor ->
            val ids = mutableListOf<Long>()
            val idIndex = cursor.getColumnIndex(CalendarContract.ExtendedProperties.EVENT_ID)
            while (cursor.moveToNext()) {
                ids += cursor.getLong(idIndex)
            }
            ids
        }
    }

    private fun queryMilestoneIdsByDescription(context: Context): List<Long>? {
        return context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID),
            "${CalendarContract.Events.DESCRIPTION} LIKE ?",
            arrayOf("$MILESTONE_MARKER_PREFIX%"),
            null
        )?.use { cursor ->
            val ids = mutableListOf<Long>()
            val idIndex = cursor.getColumnIndex(CalendarContract.Events._ID)
            while (cursor.moveToNext()) {
                ids += cursor.getLong(idIndex)
            }
            ids
        }
    }

    private fun hasCalendarReadAccess(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.READ_CALENDAR) ||
            hasPermission(context, Manifest.permission.WRITE_CALENDAR)
    }

    private fun hasCalendarWriteAccess(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.WRITE_CALENDAR)
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun CalendarOption.debugSummary(): String {
        val accountPart = accountName?.takeIf { it.isNotBlank() } ?: "no-account"
        val accessPart = accessLevel?.toString() ?: "unknown"
        return "${label}[id=$id,access=$accessPart,markedWritable=$isMarkedWritable,account=$accountPart]"
    }

    private fun tryLegacyReminderInsert(
        context: Context,
        event: Event,
        preferredCalendarId: Long?,
        useRRuleSync: Boolean,
        lastSyncAt: Long,
        reason: String
    ): ScheduleSyncResult? {
        val writableCalendars = when (val discovery = loadWritableCalendars(context)) {
            is WritableCalendarDiscoveryResult.Success -> discovery.calendars
            is WritableCalendarDiscoveryResult.Failure -> return null
        }
        val writableIds = writableCalendars.mapTo(mutableSetOf()) { it.id }
        val targetCalendarId = preferredCalendarId?.takeIf { it in writableIds }
            ?: event.targetCalendarId?.takeIf { it in writableIds }
            ?: writableCalendars.firstOrNull()?.id
            ?: return null
        return try {
            val cleanupResult = cleanupReminderSeriesEntries(
                expectedEntriesPresent = false,
                staleIds = emptySet(),
                cleanupWholeSeries = {
                    removeReminderSeriesEntries(context, event.id, event.scheduleEventId)
                },
                cleanupSingleEntry = { staleId -> removeScheduleReminder(context, staleId) }
            )
            if (!cleanupResult.isSuccess) {
                return scheduleSyncResultAfterCleanup(
                    event = event,
                    primaryScheduleEventId = null,
                    targetCalendarId = targetCalendarId,
                    lastSyncAt = lastSyncAt,
                    cleanupResult = cleanupResult
                )
            }
            val insertedId = insertLegacyReminderSeries(
                context = context,
                event = event,
                targetCalendarId = targetCalendarId,
                useRRuleSync = useRRuleSync
            ) ?: return null
            Log.w(
                TAG,
                "Modern calendar sync failed for eventId=${event.id}; legacy reminder-series fallback succeeded. reason=$reason"
            )
            ScheduleSyncResult(
                primaryScheduleEventId = insertedId,
                targetCalendarId = targetCalendarId,
                lastSyncAt = lastSyncAt,
                error = null
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            Log.w(
                TAG,
                "Legacy reminder-series fallback also failed for eventId=${event.id}. reason=$reason",
                t
            )
            null
        }
    }

    private fun insertLegacyReminderSeries(
        context: Context,
        event: Event,
        targetCalendarId: Long,
        useRRuleSync: Boolean
    ): Long? {
        val expectedEntries = buildExpectedReminderEntries(
            context = context,
            event = event,
            useRRuleSync = useRRuleSync
        )
        var primaryId: Long? = null

        expectedEntries.forEach { entry ->
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, entry.triggerAtMillis)
                put(CalendarContract.Events.DTEND, entry.triggerAtMillis + 60 * 60 * 1000L)
                put(CalendarContract.Events.TITLE, entry.title)
                put(CalendarContract.Events.DESCRIPTION, buildMarkedDescription(entry.marker, entry.note))
                put(CalendarContract.Events.CALENDAR_ID, targetCalendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                if (entry.useRRule) {
                    put(CalendarContract.Events.RRULE, buildRRuleForEvent(event))
                } else {
                    putNull(CalendarContract.Events.RRULE)
                }
            }
            val insertedId = insertEventAndReminder(context, values)
            if (primaryId == null && insertedId != null) {
                primaryId = insertedId
            }
        }

        return primaryId
    }

    internal fun buildReminderMarkerForTest(eventId: Int): String = "$REMINDER_MARKER_PREFIX:$eventId"

    internal fun buildReminderTitleForTest(event: Event): String {
        return if (event.remindDaysBefore == 0) event.title else "${event.title} (-${event.remindDaysBefore}d)"
    }

    internal fun isCalendarAccessLevelMarkedWritable(accessLevel: Int?): Boolean {
        val level = accessLevel ?: return true
        return level >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
    }

    private fun loadWritableCalendars(context: Context): WritableCalendarDiscoveryResult =
        discoverWritableCalendars(
            queryDetailed = { queryCalendarsWithDetails(context) },
            queryLegacy = { queryCalendarsWithLegacyProjection(context) }
        )

    private fun queryCalendarsWithDetails(context: Context): List<CalendarOption>? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        ) ?: return null
        val calendars = mutableListOf<CalendarOption>()
        cursor.use {
            val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
            val nameIndex = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountIndex = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
            val accessIndex = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val displayName = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else id.toString()
                val accountName = if (accountIndex >= 0) cursor.getString(accountIndex) else null
                val accessLevel = if (accessIndex >= 0) cursor.getInt(accessIndex) else null
                calendars += CalendarOption(
                    id = id,
                    displayName = displayName.ifBlank { id.toString() },
                    accountName = accountName,
                    accessLevel = accessLevel,
                    isMarkedWritable = accessIndex < 0 || isCalendarAccessLevelMarkedWritable(accessLevel)
                )
            }
        }
        return selectCalendarsForSync(calendars)
    }

    private fun queryCalendarsWithLegacyProjection(context: Context): List<CalendarOption>? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val calendars = mutableListOf<CalendarOption>()
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        ) ?: return null
        cursor.use {
            val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
            val accessIndex = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val accessLevel = if (accessIndex >= 0) cursor.getInt(accessIndex) else null
                calendars += CalendarOption(
                    id = id,
                    displayName = id.toString(),
                    accountName = null,
                    accessLevel = accessLevel,
                    isMarkedWritable = accessIndex < 0 || isCalendarAccessLevelMarkedWritable(accessLevel)
                )
            }
        }
        return selectCalendarsForSync(calendars)
    }

    internal fun selectCalendarsForSync(calendars: List<CalendarOption>): List<CalendarOption> {
        val explicitlyWritable = calendars.filter { it.isMarkedWritable }
        if (explicitlyWritable.isNotEmpty()) return explicitlyWritable
        val unknownAccess = calendars.filter { it.accessLevel == null }
        return unknownAccess
    }

    internal fun buildMarkedDescriptionForTest(marker: String, note: String): String =
        buildMarkedDescription(marker, note)
}
