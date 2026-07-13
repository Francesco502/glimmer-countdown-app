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

internal fun isManagedReminderMetadataKind(kind: String?): Boolean {
    return kind == null || kind == "reminder_v2" || kind == "reminder_rrule_v2"
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

    private data class ExistingReminderEntry(
        val calendarEventId: Long,
        val key: String?
    )

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

    fun hasWritableCalendar(context: Context): Boolean = getWritableCalendars(context).isNotEmpty()

    internal fun isNoWritableCalendarError(error: String?): Boolean {
        return error == ERROR_NO_WRITABLE_CALENDAR
    }

    fun getWritableCalendars(context: Context): List<CalendarOption> {
        return try {
            queryCalendarsWithDetails(context).ifEmpty {
                queryCalendarsWithLegacyProjection(context)
            }.also { selectedCalendars ->
                if (selectedCalendars.isEmpty()) {
                    Log.w(TAG, "Calendar provider returned no calendars")
                } else if (selectedCalendars.none { it.isMarkedWritable }) {
                    Log.w(
                        TAG,
                        "No calendars were marked writable by provider; falling back to available calendars: ${selectedCalendars.joinToString { it.debugSummary() }}"
                    )
                }
            }
        } catch (t: SecurityException) {
            Log.w(TAG, "No calendar permission when querying writable calendars", t)
            emptyList()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to query writable calendars", t)
            emptyList()
        }
    }

    fun getDefaultCalendarId(context: Context): Long? = getWritableCalendars(context).firstOrNull()?.id

    private fun resolveTargetCalendarId(context: Context, event: Event, preferredCalendarId: Long?): Long? {
        val writable = getWritableCalendars(context)
        if (writable.isEmpty()) return null
        val writableIds = writable.map { it.id }.toSet()

        val preferred = preferredCalendarId?.takeIf { it in writableIds }
        if (preferred != null) return preferred

        val eventTarget = event.targetCalendarId?.takeIf { it in writableIds }
        if (eventTarget != null) return eventTarget

        return writable.first().id
    }

    fun syncReminderSeries(
        context: Context,
        event: Event,
        preferredCalendarId: Long?,
        useRRuleSync: Boolean
    ): ScheduleSyncResult {
        val lastSyncAt = System.currentTimeMillis()
        val sanitizedEvent = event.sanitizedReminderConfig()

        return try {
            if (!sanitizedEvent.syncToScheduleEnabled) {
                removeScheduleReminder(context, sanitizedEvent.scheduleEventId)
                removeScheduleReminderByEventId(context, sanitizedEvent.id)
                ScheduleSyncResult(
                    primaryScheduleEventId = null,
                    targetCalendarId = null,
                    lastSyncAt = lastSyncAt,
                    error = null
                )
            } else {
                val targetCalendarId = resolveTargetCalendarId(context, sanitizedEvent, preferredCalendarId)
                if (targetCalendarId == null) {
                    removeScheduleReminder(context, sanitizedEvent.scheduleEventId)
                    removeScheduleReminderByEventId(context, sanitizedEvent.id)
                    ScheduleSyncResult(
                        primaryScheduleEventId = null,
                        targetCalendarId = null,
                        lastSyncAt = lastSyncAt,
                        error = ERROR_NO_WRITABLE_CALENDAR
                    )
                } else {
                    val expectedEntries = buildExpectedReminderEntries(
                        context = context,
                        event = sanitizedEvent,
                        useRRuleSync = useRRuleSync
                    )
                    var metadataWarnings = 0
                    val existing = loadExistingReminderEntries(context, sanitizedEvent.id)
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
                        val eventId = if (existingId != null && updateEventAndReminder(context, existingId, values)) {
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

                    (allExistingIds - usedIds).forEach { staleId ->
                        removeScheduleReminder(context, staleId)
                    }
                    if (expectedEntries.isEmpty()) {
                        removeScheduleReminder(context, sanitizedEvent.scheduleEventId)
                        removeScheduleReminderByEventId(context, sanitizedEvent.id)
                    }
                    if (metadataWarnings > 0) {
                        Log.w(
                            TAG,
                            "Synced calendar event(s) for eventId=${sanitizedEvent.id} with $metadataWarnings extended-property warning(s); falling back to description markers"
                        )
                    }
                    if (expectedEntries.isNotEmpty() && primaryId == null) {
                        tryLegacyReminderInsert(
                            context = context,
                            event = sanitizedEvent,
                            preferredCalendarId = preferredCalendarId,
                            useRRuleSync = useRRuleSync,
                            lastSyncAt = lastSyncAt,
                            reason = "Series sync produced no calendar event"
                        )?.let { return it }
                        Log.w(
                            TAG,
                            "All calendar inserts failed for eventId=${sanitizedEvent.id} on calendarId=$targetCalendarId; treating as no writable calendar"
                        )
                        return ScheduleSyncResult(
                            primaryScheduleEventId = null,
                            targetCalendarId = targetCalendarId,
                            lastSyncAt = lastSyncAt,
                            error = ERROR_NO_WRITABLE_CALENDAR
                        )
                    }

                    ScheduleSyncResult(
                        primaryScheduleEventId = primaryId,
                        targetCalendarId = targetCalendarId,
                        lastSyncAt = lastSyncAt,
                        error = null
                    )
                }
            }
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

    fun upsertScheduleReminder(
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

    fun insertScheduleReminder(context: Context, event: Event): Long? {
        return upsertScheduleReminder(
            context = context,
            event = event,
            currentScheduleEventId = event.scheduleEventId
        )
    }

    fun insertMilestoneScheduleReminder(
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

    fun insertMilestoneScheduleReminderWithStatus(
        context: Context,
        eventId: Int,
        title: String,
        description: String,
        triggerAtMillis: Long,
        targetCalendarId: Long? = null
    ): MilestoneScheduleSyncResult {
        val lastSyncAt = System.currentTimeMillis()
        return try {
            val resolvedCalendarId = targetCalendarId ?: getDefaultCalendarId(context)
                ?: return MilestoneScheduleSyncResult(
                    scheduleEventId = null,
                    targetCalendarId = null,
                    lastSyncAt = lastSyncAt,
                    error = ERROR_NO_WRITABLE_CALENDAR
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
            val insertedId = insertEventAndReminder(context, values)
            if (insertedId == null) {
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
        } catch (t: SecurityException) {
            MilestoneScheduleSyncResult(
                scheduleEventId = null,
                targetCalendarId = null,
                lastSyncAt = lastSyncAt,
                error = "Calendar permission denied"
            )
        } catch (t: Throwable) {
            MilestoneScheduleSyncResult(
                scheduleEventId = null,
                targetCalendarId = null,
                lastSyncAt = lastSyncAt,
                error = (t.message ?: "Unknown milestone schedule sync error").take(180)
            )
        }
    }

    fun clearAllMilestoneScheduleReminders(context: Context) {
        if (!hasCalendarReadAccess(context)) return
        val idsByKind = findEventIdsByExtendedProperty(context, META_NAME_KIND, META_KIND_MILESTONE)
        removeEventsByIds(context, idsByKind)
        removeEventsByDescriptionLike(context, "$MILESTONE_MARKER_PREFIX%")
    }

    fun clearMilestoneScheduleRemindersByEventId(context: Context, eventId: Int): CalendarCleanupResult {
        return cleanupManagedCalendarEntries(
            context = context,
            eventId = eventId,
            calendarEventId = null,
            includeReminders = false,
            includeMilestones = true
        )
    }

    fun removeScheduleReminderByEventId(context: Context, eventId: Int): CalendarCleanupResult {
        return cleanupManagedCalendarEntries(
            context = context,
            eventId = eventId,
            calendarEventId = null,
            includeReminders = true,
            includeMilestones = false
        )
    }

    fun removeScheduleReminder(context: Context, calendarEventId: Long?): CalendarCleanupResult {
        if (calendarEventId == null) return CalendarCleanupResult.RemovedOrNotPresent
        return try {
            if (!hasCalendarWriteAccess(context)) {
                return CalendarCleanupResult.PermissionRequired
            }
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventId)
            context.contentResolver.delete(uri, null, null)
            CalendarCleanupResult.RemovedOrNotPresent
        } catch (t: SecurityException) {
            calendarCleanupFailureFor(t)
        } catch (t: Exception) {
            calendarCleanupFailureFor(t)
        }
    }

    fun removeManagedCalendarEntries(
        context: Context,
        eventId: Int,
        calendarEventId: Long?
    ): CalendarCleanupResult {
        return cleanupManagedCalendarEntries(
            context = context,
            eventId = eventId,
            calendarEventId = calendarEventId,
            includeReminders = true,
            includeMilestones = true
        )
    }

    private fun cleanupManagedCalendarEntries(
        context: Context,
        eventId: Int,
        calendarEventId: Long?,
        includeReminders: Boolean,
        includeMilestones: Boolean
    ): CalendarCleanupResult {
        return try {
            if (
                !hasRequiredCalendarCleanupPermissions(
                    readGranted = hasPermission(context, Manifest.permission.READ_CALENDAR),
                    writeGranted = hasPermission(context, Manifest.permission.WRITE_CALENDAR)
                )
            ) {
                return CalendarCleanupResult.PermissionRequired
            }
            val ids = linkedSetOf<Long>()
            calendarEventId?.let(ids::add)
            val markerPrefixes = buildList {
                if (includeReminders) add(REMINDER_MARKER_PREFIX)
                if (includeMilestones) add(MILESTONE_MARKER_PREFIX)
            }

            requireCalendarCleanupQuery(
                context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    arrayOf(CalendarContract.Events._ID, CalendarContract.Events.DESCRIPTION),
                    markerPrefixes.joinToString(" OR ") { "${CalendarContract.Events.DESCRIPTION} LIKE ?" },
                    markerPrefixes.map { "$it:$eventId%" }.toTypedArray(),
                    null
                ),
                "Calendar events"
            ).use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val descriptionIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
                while (cursor.moveToNext()) {
                    val description = cursor.getString(descriptionIndex).orEmpty()
                    if (
                        isManagedCalendarDescriptionForEvent(
                            description = description,
                            eventId = eventId,
                            includeReminders = includeReminders,
                            includeMilestones = includeMilestones
                        )
                    ) {
                        ids += cursor.getLong(idIndex)
                    }
                }
            }

            val metadataCandidateIds = linkedSetOf<Long>()
            requireCalendarCleanupQuery(
                context.contentResolver.query(
                    CalendarContract.ExtendedProperties.CONTENT_URI,
                    arrayOf(CalendarContract.ExtendedProperties.EVENT_ID),
                    "${CalendarContract.ExtendedProperties.NAME} = ? AND ${CalendarContract.ExtendedProperties.VALUE} = ?",
                    arrayOf(META_NAME_EVENT_ID, eventId.toString()),
                    null
                ),
                "Calendar extended-properties event ID"
            ).use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.ExtendedProperties.EVENT_ID)
                while (cursor.moveToNext()) {
                    metadataCandidateIds += cursor.getLong(idIndex)
                }
            }
            metadataCandidateIds.forEach { candidateId ->
                var managedKind = false
                requireCalendarCleanupQuery(
                    context.contentResolver.query(
                        CalendarContract.ExtendedProperties.CONTENT_URI,
                        arrayOf(CalendarContract.ExtendedProperties.VALUE),
                        "${CalendarContract.ExtendedProperties.EVENT_ID} = ? AND ${CalendarContract.ExtendedProperties.NAME} = ?",
                        arrayOf(candidateId.toString(), META_NAME_KIND),
                        null
                    ),
                    "Calendar extended-properties kind"
                ).use { cursor ->
                    val valueIndex = cursor.getColumnIndexOrThrow(CalendarContract.ExtendedProperties.VALUE)
                    while (cursor.moveToNext()) {
                        if (
                            isManagedCalendarMetadataKind(
                                kind = cursor.getString(valueIndex),
                                includeReminders = includeReminders,
                                includeMilestones = includeMilestones
                            )
                        ) {
                            managedKind = true
                        }
                    }
                }
                if (managedKind) {
                    ids += candidateId
                }
            }

            ids.forEach { id ->
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)
                context.contentResolver.delete(uri, null, null)
            }
            CalendarCleanupResult.RemovedOrNotPresent
        } catch (t: SecurityException) {
            calendarCleanupFailureFor(t)
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
        val prefixes = buildList {
            if (includeReminders) add(REMINDER_MARKER_PREFIX)
            if (includeMilestones) add(MILESTONE_MARKER_PREFIX)
        }
        return prefixes.any { prefix ->
            val baseMarker = "$prefix:$eventId"
            marker == baseMarker || marker.startsWith("$baseMarker:")
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

    private fun buildReminderEntryKey(
        occurrenceEpochDay: Long,
        daysLeft: Int,
        useRRule: Boolean
    ): String = "$occurrenceEpochDay:$daysLeft:${if (useRRule) 1 else 0}"

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

    private fun loadExistingReminderEntries(context: Context, eventId: Int): List<ExistingReminderEntry> {
        if (!hasCalendarReadAccess(context)) return emptyList()
        return try {
            val entries = mutableListOf<ExistingReminderEntry>()

            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(CalendarContract.Events._ID, CalendarContract.Events.DESCRIPTION),
                "${CalendarContract.Events.DESCRIPTION} LIKE ?",
                arrayOf("$REMINDER_MARKER_PREFIX:$eventId%"),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(CalendarContract.Events._ID)
                val descIndex = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val description = if (descIndex >= 0) cursor.getString(descIndex).orEmpty() else ""
                    val markerLine = description.lineSequence().firstOrNull().orEmpty()
                    entries += ExistingReminderEntry(
                        calendarEventId = id,
                        key = parseReminderKeyFromMarker(markerLine)
                    )
                }
            }

            val idsByMeta = findEventIdsByExtendedProperty(context, META_NAME_EVENT_ID, eventId.toString())
            idsByMeta.forEach { id ->
                if (entries.any { it.calendarEventId == id }) return@forEach
                val props = readExtendedProperties(context, id)
                val kind = props[META_NAME_KIND]
                if (!isManagedReminderMetadataKind(kind)) return@forEach
                val occ = props[META_NAME_OCC_EPOCH_DAY]?.toLongOrNull()
                val days = props[META_NAME_DAYS_LEFT]?.toIntOrNull()
                val key = if (occ != null && days != null) {
                    buildReminderEntryKey(
                        occurrenceEpochDay = occ,
                        daysLeft = days,
                        useRRule = kind == META_KIND_REMINDER_RRULE
                    )
                } else {
                    null
                }
                entries += ExistingReminderEntry(calendarEventId = id, key = key)
            }

            entries
        } catch (t: SecurityException) {
            Log.w(TAG, "No calendar permission when loading existing reminder entries for eventId=$eventId", t)
            emptyList()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to load existing reminder entries for eventId=$eventId", t)
            emptyList()
        }
    }

    private fun findManagedReminderEventIds(context: Context, eventId: Int): Set<Long> {
        if (!hasCalendarReadAccess(context)) return emptySet()
        return try {
            val ids = mutableSetOf<Long>()
            ids += findEventIdsByExtendedProperty(context, META_NAME_EVENT_ID, eventId.toString())
                .filter { id ->
                    isManagedReminderMetadataKind(readExtendedProperties(context, id)[META_NAME_KIND])
                }
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(CalendarContract.Events._ID),
                "${CalendarContract.Events.DESCRIPTION} LIKE ?",
                arrayOf("$REMINDER_MARKER_PREFIX:$eventId%"),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(CalendarContract.Events._ID)
                while (cursor.moveToNext()) {
                    ids += cursor.getLong(idIndex)
                }
            }
            ids
        } catch (t: SecurityException) {
            Log.w(TAG, "No calendar permission when finding managed reminder events for eventId=$eventId", t)
            emptySet()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to find managed reminder events for eventId=$eventId", t)
            emptySet()
        }
    }

    private fun findManagedMilestoneEventIdsByEventId(context: Context, eventId: Int): Set<Long> {
        if (!hasCalendarReadAccess(context)) return emptySet()
        return try {
            val ids = mutableSetOf<Long>()
            ids += findEventIdsByExtendedProperty(context, META_NAME_EVENT_ID, eventId.toString())
                .filter { id ->
                    val kind = readExtendedProperties(context, id)[META_NAME_KIND]
                    kind == META_KIND_MILESTONE
                }
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(CalendarContract.Events._ID),
                "${CalendarContract.Events.DESCRIPTION} LIKE ?",
                arrayOf("$MILESTONE_MARKER_PREFIX:$eventId%"),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(CalendarContract.Events._ID)
                while (cursor.moveToNext()) {
                    ids += cursor.getLong(idIndex)
                }
            }
            ids
        } catch (t: SecurityException) {
            Log.w(TAG, "No calendar permission when finding managed milestone events for eventId=$eventId", t)
            emptySet()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to find managed milestone events for eventId=$eventId", t)
            emptySet()
        }
    }

    private fun findEventIdsByExtendedProperty(context: Context, name: String, value: String): Set<Long> {
        if (!hasCalendarReadAccess(context)) return emptySet()
        return try {
            val ids = mutableSetOf<Long>()
            context.contentResolver.query(
                CalendarContract.ExtendedProperties.CONTENT_URI,
                arrayOf(CalendarContract.ExtendedProperties.EVENT_ID),
                "${CalendarContract.ExtendedProperties.NAME} = ? AND ${CalendarContract.ExtendedProperties.VALUE} = ?",
                arrayOf(name, value),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(CalendarContract.ExtendedProperties.EVENT_ID)
                while (cursor.moveToNext()) {
                    ids += cursor.getLong(idIndex)
                }
            }
            ids
        } catch (t: SecurityException) {
            Log.w(TAG, "No calendar permission when reading extended properties", t)
            emptySet()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to query extended properties for $name=$value", t)
            emptySet()
        }
    }

    private fun readExtendedProperties(context: Context, eventId: Long): Map<String, String> {
        if (!hasCalendarReadAccess(context)) return emptyMap()
        return try {
            val result = mutableMapOf<String, String>()
            context.contentResolver.query(
                CalendarContract.ExtendedProperties.CONTENT_URI,
                arrayOf(CalendarContract.ExtendedProperties.NAME, CalendarContract.ExtendedProperties.VALUE),
                "${CalendarContract.ExtendedProperties.EVENT_ID} = ?",
                arrayOf(eventId.toString()),
                null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(CalendarContract.ExtendedProperties.NAME)
                val valueIndex = cursor.getColumnIndex(CalendarContract.ExtendedProperties.VALUE)
                while (cursor.moveToNext()) {
                    val key = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else ""
                    val value = if (valueIndex >= 0) cursor.getString(valueIndex).orEmpty() else ""
                    if (key.isNotBlank()) {
                        result[key] = value
                    }
                }
            }
            result
        } catch (t: SecurityException) {
            Log.w(TAG, "No calendar permission when reading extended properties for eventId=$eventId", t)
            emptyMap()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to read extended properties for eventId=$eventId", t)
            emptyMap()
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

    private fun removeEventsByIds(context: Context, ids: Iterable<Long>) {
        if (!hasCalendarWriteAccess(context)) return
        ids.forEach { id ->
            try {
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)
                context.contentResolver.delete(uri, null, null)
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }
    }

    private fun removeEventsByDescriptionLike(context: Context, pattern: String) {
        if (!hasCalendarReadAccess(context)) return
        try {
            val projection = arrayOf(CalendarContract.Events._ID)
            val selection = "${CalendarContract.Events.DESCRIPTION} LIKE ?"
            val args = arrayOf(pattern)
            val ids = mutableListOf<Long>()

            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                args,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(CalendarContract.Events._ID)
                while (cursor.moveToNext()) {
                    ids += cursor.getLong(idIndex)
                }
            }

            removeEventsByIds(context, ids)
        } catch (_: SecurityException) {
        } catch (_: Exception) {
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
        val targetCalendarId = resolveTargetCalendarId(context, event, preferredCalendarId) ?: return null
        return try {
            removeScheduleReminder(context, event.scheduleEventId)
            removeScheduleReminderByEventId(context, event.id)
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

    private fun queryCalendarsWithDetails(context: Context): List<CalendarOption> {
        return try {
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
            )
            val calendars = mutableListOf<CalendarOption>()
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
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
            selectCalendarsForSync(calendars)
        } catch (t: Throwable) {
            if (t is SecurityException) {
                throw t
            }
            Log.w(TAG, "Detailed calendar query failed, falling back to legacy projection", t)
            emptyList()
        }
    }

    private fun queryCalendarsWithLegacyProjection(context: Context): List<CalendarOption> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val calendars = mutableListOf<CalendarOption>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
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
