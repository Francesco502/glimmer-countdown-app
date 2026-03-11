package com.example.timeapk.ui.event

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.data.EventRepository
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.data.sanitizedReminderConfig
import com.example.timeapk.notifications.ScheduleSyncManager
import com.example.timeapk.notifications.cancelMilestoneReminders
import com.example.timeapk.notifications.cancelReminder
import com.example.timeapk.notifications.scheduleReminder
import com.example.timeapk.notifications.syncMilestoneReminderForEvent
import com.example.timeapk.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class SaveEventResult {
    object Success : SaveEventResult()
    data class PartialSuccess(val message: String) : SaveEventResult()
    data class Failure(val message: String) : SaveEventResult()
}

class EventEntryViewModel(
    private val application: Application,
    private val repository: EventRepository
) : AndroidViewModel(application) {
    private val _eventUiState = MutableStateFlow(EventEntryUiState())
    val eventUiState: StateFlow<EventEntryUiState> = _eventUiState.asStateFlow()

    fun updateUiState(eventDetails: EventDetails) {
        _eventUiState.update {
            it.copy(eventDetails = eventDetails, isEntryValid = validateInput(eventDetails))
        }
    }

    fun loadEvent(id: Int) {
        viewModelScope.launch {
            val event = repository.getEvent(id)
            if (event != null) {
                _eventUiState.update {
                    it.copy(
                        eventDetails = event.toEventDetails(),
                        isEntryValid = validateInput(event.toEventDetails()),
                        loadError = false
                    )
                }
            } else {
                _eventUiState.update { it.copy(loadError = true) }
            }
        }
    }

    suspend fun saveEvent(): SaveEventResult {
        val details = _eventUiState.value.eventDetails
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

        var hasSideEffectFailure = false

        var updatedEvent = persistedEvent

        try {
            cancelReminder(application, persistedEvent.id)
            if (persistedEvent.remindEnabled) {
                scheduleReminder(application, persistedEvent)
            }
        } catch (_: Exception) {
            hasSideEffectFailure = true
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
                    hasSideEffectFailure = true
                }
                persistedEvent.copy(
                    scheduleEventId = syncResult.primaryScheduleEventId,
                    targetCalendarId = syncResult.targetCalendarId,
                    lastScheduleSyncAt = syncResult.lastSyncAt,
                    lastScheduleSyncError = syncResult.error
                )
            } catch (_: Exception) {
                hasSideEffectFailure = true
                persistedEvent.copy(
                    scheduleEventId = null,
                    targetCalendarId = null,
                    lastScheduleSyncAt = System.currentTimeMillis(),
                    lastScheduleSyncError = "Schedule sync failed"
                )
            }
        } else {
            try {
                ScheduleSyncManager.removeScheduleReminder(application, persistedEvent.scheduleEventId)
                ScheduleSyncManager.removeScheduleReminderByEventId(application, persistedEvent.id)
            } catch (_: Exception) {
                hasSideEffectFailure = true
            }
            persistedEvent.copy(
                scheduleEventId = null,
                targetCalendarId = null,
                lastScheduleSyncAt = System.currentTimeMillis(),
                lastScheduleSyncError = null
            )
        }

        if (updatedEvent != persistedEvent) {
            try {
                repository.updateEvent(updatedEvent)
            } catch (_: Exception) {
                hasSideEffectFailure = true
            }
        }

        try {
            syncMilestoneReminderForEvent(application, updatedEvent)
            if (updatedEvent.repeatType != REPEAT_YEARLY && updatedEvent.repeatType != REPEAT_NONE) {
                cancelMilestoneReminders(application, updatedEvent.id)
                ScheduleSyncManager.clearMilestoneScheduleRemindersByEventId(application, updatedEvent.id)
            }
        } catch (_: Exception) {
            hasSideEffectFailure = true
        }

        try {
            WidgetUpdater.refreshCountdownWidgets(application)
        } catch (_: Exception) {
            hasSideEffectFailure = true
        }

        return if (hasSideEffectFailure) {
            SaveEventResult.PartialSuccess(application.getString(R.string.save_event_partial_warning))
        } else {
            SaveEventResult.Success
        }
    }

    private fun validateInput(uiState: EventDetails = _eventUiState.value.eventDetails): Boolean {
        return uiState.title.isNotBlank() && uiState.date > 0
    }
}

data class EventEntryUiState(
    val eventDetails: EventDetails = EventDetails(),
    val isEntryValid: Boolean = false,
    val loadError: Boolean = false
)

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
    repeatType = repeatType,
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
    repeatType = repeatType,
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
