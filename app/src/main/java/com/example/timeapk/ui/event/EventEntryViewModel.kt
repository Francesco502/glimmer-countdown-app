package com.example.timeapk.ui.event

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timeapk.data.Event
import com.example.timeapk.notifications.rescheduleMilestoneReminders
import com.example.timeapk.notifications.scheduleReminder
import com.example.timeapk.notifications.ScheduleSyncManager
import com.example.timeapk.notifications.cancelReminder
import com.example.timeapk.widget.WidgetUpdater
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.EventRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    suspend fun saveEvent(): Boolean {
        val details = _eventUiState.value.eventDetails
        if (!validateInput(details)) return false
        return try {
            withContext(NonCancellable) {
                var event = details.toEvent()
                if (event.id != 0) {
                    repository.updateEvent(event)
                    cancelReminder(application, event.id)
                    ScheduleSyncManager.removeScheduleReminder(application, event.scheduleEventId)
                    scheduleReminder(application, event)
                } else {
                    val generatedId = repository.insertEvent(event)
                    event = event.copy(id = generatedId.toInt())
                    scheduleReminder(application, event)
                }
                if (event.remindEnabled && event.syncToScheduleEnabled) {
                    val scheduleId = ScheduleSyncManager.insertScheduleReminder(application, event)
                    if (scheduleId != null) {
                        repository.updateEvent(event.copy(scheduleEventId = scheduleId))
                    }
                } else if (event.scheduleEventId != null) {
                    repository.updateEvent(event.copy(scheduleEventId = null))
                }
                rescheduleMilestoneReminders(application)
                WidgetUpdater.refreshCountdownWidgets(application)
            }
            true
        } catch (_: Exception) {
            false
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
    val createdAt: Long = System.currentTimeMillis(),
    val isLunar: Boolean = false
)

fun EventDetails.toEvent(): Event = Event(
    id = id,
    title = title,
    date = date,
    category = category.takeIf { it in listOf(CATEGORY_BIRTHDAY, CATEGORY_ANNIVERSARY, CATEGORY_OTHER) } ?: CATEGORY_OTHER,
    note = note,
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
