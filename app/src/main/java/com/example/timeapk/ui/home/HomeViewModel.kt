package com.example.timeapk.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timeapk.data.Event
import com.example.timeapk.notifications.cancelReminder
import com.example.timeapk.notifications.scheduleReminder
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.data.EventRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun Event.toEventUiState(): EventUiState {
    val today = LocalDate.now()
    val targetDate = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()
    var nextTargetDate = targetDate
    when (repeatType) {
        REPEAT_YEARLY -> {
            val currentYearDate = targetDate.withYear(today.year)
            nextTargetDate = if (currentYearDate.isBefore(today)) {
                targetDate.withYear(today.year + 1)
            } else {
                currentYearDate
            }
        }
        REPEAT_HALF_YEARLY -> {
            var next = targetDate
            while (next.isBefore(today)) {
                next = next.plusMonths(6)
            }
            nextTargetDate = next
        }
        REPEAT_MONTHLY -> {
            var next = targetDate
            while (next.isBefore(today)) {
                next = next.plusMonths(1)
            }
            nextTargetDate = next
        }
        else -> { /* REPEAT_NONE: use targetDate as-is */ }
    }
    val daysDiff = ChronoUnit.DAYS.between(today, nextTargetDate)
    val nextTargetInstant = nextTargetDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
    val isPast = daysDiff < 0
    // 仅在距离目标小于 24 小时时显示“小时”，避免远期事件显示 23 小时的错误
    val totalHours = ChronoUnit.HOURS.between(Instant.now(), nextTargetInstant)
    val hoursRemaining = if (!isPast && totalHours in 0..23) {
        totalHours
    } else {
        0L
    }
    return EventUiState(
        event = this,
        daysRemaining = kotlin.math.abs(daysDiff),
        isPast = isPast,
        hoursRemaining = hoursRemaining
    )
}

data class EventUiState(
    val event: Event,
    val daysRemaining: Long,
    val isPast: Boolean,
    val hoursRemaining: Long = 0
)

class HomeViewModel(
    private val application: Application,
    private val repository: EventRepository
) : AndroidViewModel(application) {
    companion object {
        /** 列表分页：最多展示的未来事件数量 */
        private const val MAX_UPCOMING_ITEMS = 100

        /** 列表分页：最多展示的最近已过事件数量 */
        private const val MAX_PAST_ITEMS = 50
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            cancelReminder(application, event.id)
            repository.deleteEvent(event)
        }
    }

    fun restoreEvent(event: Event) {
        viewModelScope.launch {
            repository.insertEvent(event)
            scheduleReminder(application, event)
        }
    }

    val homeUiState: StateFlow<List<EventUiState>> = repository.getAllEvents()
        .map { events ->
            val all = events.map { it.toEventUiState() }
            val upcoming = all
                .filter { !it.isPast }
                .sortedBy { it.daysRemaining }
                .take(MAX_UPCOMING_ITEMS)
            val past = all
                .filter { it.isPast }
                .sortedBy { it.daysRemaining } // daysRemaining 为绝对值，越小表示离今天越近
                .take(MAX_PAST_ITEMS)
            upcoming + past
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
