package com.example.timeapk.ui.event

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timeapk.data.Event
import com.example.timeapk.notifications.scheduleReminder
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
            repository.getEvent(id)?.let { event ->
                _eventUiState.value = EventEntryUiState(
                    eventDetails = event.toEventDetails(),
                    isEntryValid = validateInput(event.toEventDetails())
                )
            }
        }
    }

    suspend fun saveEvent() {
        val details = _eventUiState.value.eventDetails
        if (!validateInput(details)) return
        val event = details.toEvent()
        if (event.id != 0) {
            repository.updateEvent(event)
            scheduleReminder(application, event)
        } else {
            // insertEvent 返回 Room 自动生成的真实 ID
            // 必须用真实 ID 调度提醒，否则 tag="remind_0" 导致所有新建事件共用同一提醒
            val generatedId = repository.insertEvent(event)
            scheduleReminder(application, event.copy(id = generatedId.toInt()))
        }
    }

    private fun validateInput(uiState: EventDetails = _eventUiState.value.eventDetails): Boolean {
        return uiState.title.isNotBlank() && uiState.date > 0
    }
}

data class EventEntryUiState(
    val eventDetails: EventDetails = EventDetails(),
    val isEntryValid: Boolean = false
)

data class EventDetails(
    val id: Int = 0,
    val title: String = "",
    val date: Long = System.currentTimeMillis(),
    val category: String = "其他",
    val note: String = "",
    val colorHex: String? = null,
    val repeatType: String = REPEAT_NONE,
    val remindDaysBefore: Int = 0,
    val reminderTimeMinutesOfDay: Int = 480,
    val remindEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

fun EventDetails.toEvent(): Event = Event(
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
    createdAt = createdAt
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
    createdAt = createdAt
)
