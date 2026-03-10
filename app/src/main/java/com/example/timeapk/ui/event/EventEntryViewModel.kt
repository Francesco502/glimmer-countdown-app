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
import com.example.timeapk.data.normalizeTags
import com.example.timeapk.data.sanitizedReminderConfig
import com.example.timeapk.notifications.ScheduleSyncManager
import com.example.timeapk.notifications.cancelReminder
import com.example.timeapk.notifications.rescheduleMilestoneReminders
import com.example.timeapk.notifications.scheduleReminder
import com.example.timeapk.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

        try {
            cancelReminder(application, persistedEvent.id)
        } catch (_: Exception) {
            hasSideEffectFailure = true
        }

        if (persistedEvent.remindEnabled) {
            try {
                scheduleReminder(application, persistedEvent)
            } catch (_: Exception) {
                hasSideEffectFailure = true
            }
        }

        val newScheduleId = if (persistedEvent.syncToScheduleEnabled) {
            try {
                ScheduleSyncManager.upsertScheduleReminder(
                    context = application,
                    event = persistedEvent,
                    currentScheduleEventId = persistedEvent.scheduleEventId
                ).also {
                    if (it == null) hasSideEffectFailure = true
                }
            } catch (_: Exception) {
                hasSideEffectFailure = true
                null
            }
        } else {
            try {
                ScheduleSyncManager.removeScheduleReminder(application, persistedEvent.scheduleEventId)
                ScheduleSyncManager.removeScheduleReminderByEventId(application, persistedEvent.id)
            } catch (_: Exception) {
                hasSideEffectFailure = true
            }
            null
        }

        if (newScheduleId != persistedEvent.scheduleEventId) {
            try {
                repository.updateEvent(persistedEvent.copy(scheduleEventId = newScheduleId))
            } catch (_: Exception) {
                hasSideEffectFailure = true
            }
        }

        try {
            rescheduleMilestoneReminders(application)
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
    val tags: String = "",
    val colorHex: String? = null,
    val repeatType: String = REPEAT_NONE,
    val remindDaysBefore: Int = 0,
    val reminderTimeMinutesOfDay: Int = 480,
    val remindEnabled: Boolean = false,
    val syncToScheduleEnabled: Boolean = true,
    val scheduleEventId: Long? = null,
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
    tags = normalizeTags(tags),
    colorHex = colorHex,
    repeatType = repeatType,
    remindDaysBefore = remindDaysBefore,
    reminderTimeMinutesOfDay = reminderTimeMinutesOfDay,
    remindEnabled = remindEnabled,
    syncToScheduleEnabled = syncToScheduleEnabled,
    scheduleEventId = scheduleEventId,
    createdAt = createdAt,
    isLunar = isLunar
)

fun Event.toEventDetails(): EventDetails = EventDetails(
    id = id,
    title = title,
    date = date,
    category = category,
    note = note,
    tags = tags,
    colorHex = colorHex,
    repeatType = repeatType,
    remindDaysBefore = remindDaysBefore,
    reminderTimeMinutesOfDay = reminderTimeMinutesOfDay,
    remindEnabled = remindEnabled,
    syncToScheduleEnabled = syncToScheduleEnabled,
    scheduleEventId = scheduleEventId,
    createdAt = createdAt,
    isLunar = isLunar
)
