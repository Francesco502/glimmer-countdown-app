package com.example.timeapk.ui.event

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.room.withTransaction
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.DefaultEventReminderSettings
import com.example.timeapk.data.Event
import com.example.timeapk.data.EventRepository
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.data.UserPreferencesRepository
import com.example.timeapk.data.sanitizeRemindDaysBefore
import com.example.timeapk.data.sanitizeReminderTimeMinutesOfDay
import com.example.timeapk.data.sanitizedReminderConfig
import com.example.timeapk.notifications.ScheduleSyncManager
import com.example.timeapk.notifications.cancelMilestoneReminders
import com.example.timeapk.notifications.cancelReminder
import com.example.timeapk.notifications.scheduleReminder
import com.example.timeapk.notifications.syncMilestoneReminderForEvent
import com.example.timeapk.ui.home.calendarCleanupRequired
import com.example.timeapk.ui.home.eventAfterCleanupAttempt
import com.example.timeapk.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.random.Random

sealed class SaveEventResult {
    object Success : SaveEventResult()
    data class PartialSuccess(val message: String) : SaveEventResult()
    data class Failure(val message: String) : SaveEventResult()
}

private val MIN_SUPPORTED_EVENT_DATE_MILLIS: Long = LocalDate.of(1900, 1, 1)
    .atStartOfDay(ZoneOffset.UTC)
    .toInstant()
    .toEpochMilli()

// 事件卡片默认颜色（与编辑页预设颜色保持一致）
private val DEFAULT_EVENT_COLOR_HEX = listOf(
    "#4A4933",
    "#457080",
    "#5F856B",
    "#AF4E31",
    "#AC8F62",
    "#86351C",
    "#5B8E79",
    "#3A4550",
    "#785B64"
)
private const val TAG = "EventEntryViewModel"

internal fun isEventDateValid(dateMillis: Long): Boolean = dateMillis >= MIN_SUPPORTED_EVENT_DATE_MILLIS

internal fun sanitizeRepeatTypeForLunar(isLunar: Boolean, repeatType: String): String {
    if (!isLunar) return repeatType
    return when (repeatType) {
        REPEAT_NONE, REPEAT_YEARLY -> repeatType
        else -> REPEAT_NONE
    }
}

internal fun supportedRepeatTypes(isLunar: Boolean): List<String> {
    return if (isLunar) {
        listOf(REPEAT_NONE, REPEAT_YEARLY)
    } else {
        listOf(
            REPEAT_NONE,
            REPEAT_YEARLY,
            com.example.timeapk.data.REPEAT_HALF_YEARLY,
            com.example.timeapk.data.REPEAT_MONTHLY,
            com.example.timeapk.data.REPEAT_WEEKLY,
            com.example.timeapk.data.REPEAT_DAILY
        )
    }
}

internal fun calendarCleanupHandledExternallyForSave(syncToScheduleEnabled: Boolean): Boolean =
    !syncToScheduleEnabled

internal fun shouldClearMilestoneCalendarAfterSave(
    syncToScheduleEnabled: Boolean,
    repeatType: String
): Boolean = syncToScheduleEnabled && repeatType != REPEAT_YEARLY && repeatType != REPEAT_NONE

internal fun resolvePartialSaveMessageResId(
    hasGenericFailure: Boolean,
    scheduleSyncError: String?
): Int? {
    if (!hasGenericFailure && scheduleSyncError.isNullOrBlank()) {
        return null
    }
    if (!hasGenericFailure && ScheduleSyncManager.isNoWritableCalendarError(scheduleSyncError)) {
        return R.string.save_event_partial_warning_no_writable_calendar
    }
    return R.string.save_event_partial_warning
}

internal fun buildNewEventDetails(
    defaultReminderSettings: DefaultEventReminderSettings,
    initialCategory: String? = null,
    nowMillis: Long = System.currentTimeMillis()
): EventDetails {
    val resolvedCategory = initialCategory
        .takeIf { it in listOf(CATEGORY_BIRTHDAY, CATEGORY_ANNIVERSARY, CATEGORY_OTHER) }
        ?: CATEGORY_OTHER
    return EventDetails(
        date = nowMillis,
        category = resolvedCategory,
        remindDaysBefore = sanitizeRemindDaysBefore(defaultReminderSettings.daysBefore),
        reminderTimeMinutesOfDay = sanitizeReminderTimeMinutesOfDay(defaultReminderSettings.timeMinutesOfDay),
        remindEnabled = defaultReminderSettings.enabled,
        createdAt = nowMillis
    )
}

class EventEntryViewModel(
    private val application: Application,
    private val repository: EventRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : AndroidViewModel(application) {
    private val _eventUiState = MutableStateFlow(EventEntryUiState())
    val eventUiState: StateFlow<EventEntryUiState> = _eventUiState.asStateFlow()

    fun updateUiState(eventDetails: EventDetails) {
        _eventUiState.update {
            it.copy(eventDetails = eventDetails, isEntryValid = validateInput(eventDetails))
        }
    }

    suspend fun prepareForEvent(eventId: Int?) {
        if (eventId != null && eventId != 0) {
            loadExistingEvent(eventId)
            return
        }

        val app = application as? TimeApplication
        val initialCategory = app?.initialCategoryForAdd
        if (app != null) {
            app.initialCategoryForAdd = null
        }

        val defaultReminderSettings = runCatching {
            userPreferencesRepository.getDefaultEventReminderSettings()
        }.getOrDefault(DefaultEventReminderSettings())
        val newDetails = buildNewEventDetails(
            defaultReminderSettings = defaultReminderSettings,
            initialCategory = initialCategory
        )
        _eventUiState.update {
            it.copy(
                eventDetails = newDetails,
                initialEventDetails = newDetails,
                isEntryValid = validateInput(newDetails),
                loadError = false
            )
        }
    }

    private suspend fun loadExistingEvent(id: Int) {
        val event = repository.getEvent(id)
        if (event != null) {
            val loadedDetails = event.toEventDetails()
            _eventUiState.update {
                it.copy(
                    eventDetails = loadedDetails,
                    initialEventDetails = loadedDetails,
                    isEntryValid = validateInput(loadedDetails),
                    loadError = false
                )
            }
        } else {
            _eventUiState.update { it.copy(loadError = true) }
        }
    }

    suspend fun saveEvent(): SaveEventResult {
        val rawDetails = _eventUiState.value.eventDetails
        val details = if (rawDetails.id == 0 && rawDetails.colorHex.isNullOrBlank()) {
            // 新建事件且未选择卡片颜色时，在默认颜色中随机选一个
            rawDetails.copy(colorHex = DEFAULT_EVENT_COLOR_HEX.random())
        } else {
            rawDetails
        }
        if (!validateInput(details)) {
            return SaveEventResult.Failure(application.getString(R.string.save_event_failed))
        }

        val app = application as? TimeApplication
            ?: return SaveEventResult.Failure(application.getString(R.string.save_event_failed))

        val persistedEvent = try {
            var event = details.toEvent().sanitizedReminderConfig()
            app.database.withTransaction {
                if (event.id != 0) {
                    repository.updateEvent(event)
                } else {
                    val generatedId = repository.insertEvent(event)
                    event = event.copy(id = generatedId.toInt(), scheduleEventId = null)
                }
            }
            event
        } catch (_: Exception) {
            return SaveEventResult.Failure(application.getString(R.string.save_event_failed))
        }

        var hasGenericSideEffectFailure = false
        var scheduleSyncError: String? = null

        var updatedEvent = persistedEvent

        try {
            cancelReminder(application, persistedEvent.id)
            if (persistedEvent.remindEnabled) {
                scheduleReminder(application, persistedEvent)
            }
        } catch (t: Exception) {
            Log.w(TAG, "Failed to schedule app reminder for eventId=${persistedEvent.id}", t)
            hasGenericSideEffectFailure = true
        }

        updatedEvent = if (persistedEvent.syncToScheduleEnabled) {
            try {
                val preferredCalendarId = app.userPrefs.scheduleTargetCalendarIdFlow.first()
                val useRRuleSync = app.userPrefs.scheduleUseRRuleSyncFlow.first()
                val syncResult = ScheduleSyncManager.syncReminderSeries(
                    context = application,
                    event = persistedEvent,
                    preferredCalendarId = preferredCalendarId,
                    useRRuleSync = useRRuleSync
                )
                if (syncResult.error != null) {
                    Log.w(
                        TAG,
                        "Calendar sync returned warning for eventId=${persistedEvent.id}: ${syncResult.error}"
                    )
                    scheduleSyncError = syncResult.error
                }
                persistedEvent.copy(
                    scheduleEventId = syncResult.primaryScheduleEventId,
                    targetCalendarId = syncResult.targetCalendarId,
                    lastScheduleSyncAt = syncResult.lastSyncAt,
                    lastScheduleSyncError = syncResult.error
                )
            } catch (t: Exception) {
                Log.w(TAG, "Calendar sync crashed for eventId=${persistedEvent.id}", t)
                scheduleSyncError = "Schedule sync failed"
                persistedEvent.copy(
                    scheduleEventId = null,
                    targetCalendarId = null,
                    lastScheduleSyncAt = System.currentTimeMillis(),
                    lastScheduleSyncError = scheduleSyncError
                )
            }
        } else {
            if (calendarCleanupRequired(persistedEvent)) {
                val cleanup = ScheduleSyncManager.removeManagedCalendarEntries(
                    context = application,
                    eventId = persistedEvent.id,
                    calendarEventId = persistedEvent.scheduleEventId
                )
                scheduleSyncError = cleanup.message
                eventAfterCleanupAttempt(
                    event = persistedEvent,
                    result = cleanup,
                    nowMillis = System.currentTimeMillis()
                )
            } else {
                // No provider attempt occurred, so preserve the previous sync timestamp.
                persistedEvent
            }
        }

        if (updatedEvent != persistedEvent) {
            try {
                repository.updateEvent(updatedEvent)
            } catch (t: Exception) {
                Log.w(TAG, "Failed to persist sync status for eventId=${updatedEvent.id}", t)
            }
        }

        try {
            syncMilestoneReminderForEvent(
                application = application,
                event = updatedEvent,
                calendarCleanupHandledExternally = calendarCleanupHandledExternallyForSave(
                    updatedEvent.syncToScheduleEnabled
                )
            )
            if (
                shouldClearMilestoneCalendarAfterSave(
                    updatedEvent.syncToScheduleEnabled,
                    updatedEvent.repeatType
                )
            ) {
                cancelMilestoneReminders(application, updatedEvent.id)
                ScheduleSyncManager.clearMilestoneScheduleRemindersByEventId(application, updatedEvent.id)
            }
        } catch (t: Exception) {
            Log.w(TAG, "Failed to sync milestone reminders for eventId=${updatedEvent.id}", t)
        }

        try {
            WidgetUpdater.refreshCountdownWidgets(application)
        } catch (t: Exception) {
            Log.w(TAG, "Failed to refresh widgets after saving eventId=${updatedEvent.id}", t)
        }

        val savedDetails = updatedEvent.toEventDetails()
        _eventUiState.update {
            it.copy(
                eventDetails = savedDetails,
                initialEventDetails = savedDetails,
                isEntryValid = validateInput(savedDetails),
                loadError = false
            )
        }

        val partialMessageResId = resolvePartialSaveMessageResId(
            hasGenericFailure = hasGenericSideEffectFailure,
            scheduleSyncError = scheduleSyncError
        )

        return if (partialMessageResId != null) {
            Log.w(
                TAG,
                "Saved eventId=${updatedEvent.id} with partial side-effect failure. hasGenericFailure=$hasGenericSideEffectFailure, scheduleSyncError=$scheduleSyncError"
            )
            SaveEventResult.PartialSuccess(application.getString(partialMessageResId))
        } else {
            SaveEventResult.Success
        }
    }

    private fun validateInput(uiState: EventDetails = _eventUiState.value.eventDetails): Boolean {
        return uiState.title.isNotBlank() && isEventDateValid(uiState.date)
    }
}

private fun EventDetails.hasSameEditableContent(other: EventDetails): Boolean {
    return title == other.title &&
        date == other.date &&
        category == other.category &&
        note == other.note &&
        colorHex == other.colorHex &&
        repeatType == other.repeatType &&
        remindDaysBefore == other.remindDaysBefore &&
        reminderTimeMinutesOfDay == other.reminderTimeMinutesOfDay &&
        remindEnabled == other.remindEnabled &&
        syncToScheduleEnabled == other.syncToScheduleEnabled &&
        isLunar == other.isLunar
}

data class EventEntryUiState(
    val eventDetails: EventDetails = EventDetails(),
    val isEntryValid: Boolean = false,
    val loadError: Boolean = false,
    val initialEventDetails: EventDetails = eventDetails
) {
    fun hasUnsavedChanges(): Boolean = !eventDetails.hasSameEditableContent(initialEventDetails)
}

data class EventDetails(
    val id: Int = 0,
    val title: String = "",
    val date: Long = System.currentTimeMillis(),
    val category: String = CATEGORY_OTHER,
    val note: String = "",
    val colorHex: String? = null,
    val repeatType: String = REPEAT_NONE,
    val remindDaysBefore: Int = 0,
    val reminderTimeMinutesOfDay: Int = 480,
    val remindEnabled: Boolean = false,
    val syncToScheduleEnabled: Boolean = true,
    val scheduleEventId: Long? = null,
    val targetCalendarId: Long? = null,
    val lastScheduleSyncAt: Long? = null,
    val lastScheduleSyncError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isLunar: Boolean = false
)

fun EventDetails.toEvent(): Event = Event(
    id = id,
    title = title,
    date = date,
    category = category.takeIf { it in listOf(CATEGORY_BIRTHDAY, CATEGORY_ANNIVERSARY, CATEGORY_OTHER) }
        ?: CATEGORY_OTHER,
    note = note,
    colorHex = colorHex,
    repeatType = sanitizeRepeatTypeForLunar(isLunar = isLunar, repeatType = repeatType),
    remindDaysBefore = remindDaysBefore,
    reminderTimeMinutesOfDay = reminderTimeMinutesOfDay,
    remindEnabled = remindEnabled,
    syncToScheduleEnabled = syncToScheduleEnabled,
    scheduleEventId = scheduleEventId,
    targetCalendarId = targetCalendarId,
    lastScheduleSyncAt = lastScheduleSyncAt,
    lastScheduleSyncError = lastScheduleSyncError,
    createdAt = createdAt,
    isLunar = isLunar
)

fun Event.toEventDetails(): EventDetails = EventDetails(
    id = id,
    title = title,
    date = date,
    category = category,
    note = note,
    colorHex = colorHex,
    repeatType = sanitizeRepeatTypeForLunar(isLunar = isLunar, repeatType = repeatType),
    remindDaysBefore = remindDaysBefore,
    reminderTimeMinutesOfDay = reminderTimeMinutesOfDay,
    remindEnabled = remindEnabled,
    syncToScheduleEnabled = syncToScheduleEnabled,
    scheduleEventId = scheduleEventId,
    targetCalendarId = targetCalendarId,
    lastScheduleSyncAt = lastScheduleSyncAt,
    lastScheduleSyncError = lastScheduleSyncError,
    createdAt = createdAt,
    isLunar = isLunar
)
