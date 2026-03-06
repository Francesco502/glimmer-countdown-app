package com.example.timeapk.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timeapk.data.DEFAULT_MILESTONE_DAYS
import com.example.timeapk.data.Event
import com.example.timeapk.data.EventRepository
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.data.UserPreferencesRepository
import com.example.timeapk.notifications.ScheduleSyncManager
import com.example.timeapk.notifications.cancelMilestoneReminders
import com.example.timeapk.notifications.cancelReminder
import com.example.timeapk.notifications.rescheduleMilestoneReminders
import com.example.timeapk.notifications.scheduleReminder
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.getNextLunarOccurrence
import com.example.timeapk.ui.utils.getPreviousLunarOccurrence
import com.example.timeapk.ui.utils.nextOccurrenceDate
import com.example.timeapk.ui.utils.previousOccurrenceDate
import com.example.timeapk.widget.WidgetUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

fun Event.toEventUiState(milestones: List<Long> = DEFAULT_MILESTONE_DAYS): EventUiState {
    val today = LocalDate.now()
    val targetDate = eventDateToLocalDate(date)
    val hasStarted = !targetDate.isAfter(today)
    var nextTargetDate = targetDate
    var prevTargetDate: LocalDate? = null

    when {
        repeatType == REPEAT_YEARLY && isLunar && hasStarted -> {
            nextTargetDate = getNextLunarOccurrence(targetDate, today)
            prevTargetDate = getPreviousLunarOccurrence(targetDate, today)
        }

        repeatType != REPEAT_NONE && hasStarted -> {
            nextTargetDate = nextOccurrenceDate(targetDate, today, repeatType)
            prevTargetDate = previousOccurrenceDate(targetDate, today, repeatType)
        }
    }

    val daysDiff = ChronoUnit.DAYS.between(today, nextTargetDate)
    val nextTargetInstant = nextTargetDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
    val isPast = daysDiff < 0
    val daysRemainingAbs = kotlin.math.abs(daysDiff)
    val daysElapsed = if (isPast) daysRemainingAbs else 0L
    val daysLeft = if (isPast) 0L else daysRemainingAbs
    val daysPassed = when {
        isPast -> daysRemainingAbs
        repeatType == REPEAT_YEARLY && hasStarted -> maxOf(0L, ChronoUnit.DAYS.between(targetDate, today))
        prevTargetDate != null -> maxOf(0L, ChronoUnit.DAYS.between(prevTargetDate, today))
        else -> 0L
    }
    val totalHours = ChronoUnit.HOURS.between(Instant.now(), nextTargetInstant)
    val hoursRemaining = if (!isPast && totalHours in 0..23) {
        totalHours
    } else {
        0L
    }
    val milestoneCurrent = when {
        repeatType == REPEAT_YEARLY && hasStarted -> daysPassed
        isPast -> daysElapsed
        else -> daysPassed
    }
    val (nextMilestoneDays, nextMilestoneValue) = when {
        repeatType == REPEAT_NONE && !isPast -> computeNextCountdownMilestone(milestones, daysRemainingAbs)
        else -> computeNextMilestone(milestones, milestoneCurrent)
    }
    return EventUiState(
        event = this,
        daysRemaining = daysRemainingAbs,
        daysElapsed = daysElapsed,
        daysLeft = daysLeft,
        daysPassed = daysPassed,
        isPast = isPast,
        hoursRemaining = hoursRemaining,
        nextMilestoneDays = nextMilestoneDays,
        nextMilestoneValue = nextMilestoneValue,
        nextOccurrenceDate = nextTargetDate
    )
}

private fun computeNextMilestone(milestones: List<Long>, current: Long): Pair<Long?, Long?> {
    val list = milestones.filter { it > 0 }.distinct().sorted()
    if (list.isEmpty()) return null to null
    val next = list.firstOrNull { it > current }
    if (next == null) return null to null
    val daysUntilMilestone = next - current
    return daysUntilMilestone to next
}

private fun computeNextCountdownMilestone(milestones: List<Long>, daysRemaining: Long): Pair<Long?, Long?> {
    val list = milestones.filter { it > 0 }.distinct().sorted()
    if (list.isEmpty()) return null to null
    val next = list.filter { it <= daysRemaining }.maxOrNull() ?: return null to null
    return (daysRemaining - next) to next
}

data class EventUiState(
    val event: Event,
    val daysRemaining: Long,
    val daysElapsed: Long = 0,
    val daysLeft: Long = 0,
    val daysPassed: Long = 0,
    val isPast: Boolean,
    val hoursRemaining: Long = 0,
    val nextMilestoneDays: Long? = null,
    val nextMilestoneValue: Long? = null,
    val nextOccurrenceDate: LocalDate
)

class HomeViewModel(
    private val application: Application,
    private val repository: EventRepository,
    private val userPrefs: UserPreferencesRepository
) : AndroidViewModel(application) {
    companion object {
        private const val MAX_UPCOMING_ITEMS = 100
        private const val MAX_PAST_ITEMS = 50
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            cancelReminder(application, event.id)
            cancelMilestoneReminders(application, event.id)
            ScheduleSyncManager.removeScheduleReminder(application, event.scheduleEventId)
            repository.deleteEvent(event)
            rescheduleMilestoneReminders(application)
            WidgetUpdater.refreshCountdownWidgets(application)
        }
    }

    fun restoreEvent(event: Event) {
        viewModelScope.launch {
            try {
                val newId = repository.insertEvent(event)
                val savedEvent = event.copy(id = newId.toInt())
                scheduleReminder(application, savedEvent)
                val scheduleId = if (savedEvent.remindEnabled && savedEvent.syncToScheduleEnabled) {
                    ScheduleSyncManager.insertScheduleReminder(application, savedEvent)
                } else {
                    null
                }
                repository.updateEvent(savedEvent.copy(scheduleEventId = scheduleId))
                rescheduleMilestoneReminders(application)
                WidgetUpdater.refreshCountdownWidgets(application)
            } catch (_: Exception) {
                // Ignore duplicate key or persistence failures during restore.
            }
        }
    }

    private fun minuteTickerFlow(): Flow<Long> = flow {
        emit(System.currentTimeMillis())
        while (true) {
            val now = ZonedDateTime.now()
            val nextMinute = now.plusMinutes(1).withSecond(0).withNano(0)
            val delayMillis = java.time.Duration.between(now, nextMinute).toMillis().coerceAtLeast(500L)
            delay(delayMillis)
            emit(System.currentTimeMillis())
        }
    }

    val homeUiState: StateFlow<List<EventUiState>> = combine(
        repository.getAllEvents(),
        userPrefs.customMilestonesFlow,
        minuteTickerFlow()
    ) { events: List<Event>, milestones: List<Long>, _: Long ->
        val all = events.map { it.toEventUiState(milestones) }
        val upcoming = all
            .filter { !it.isPast }
            .sortedBy { it.daysRemaining }
            .take(MAX_UPCOMING_ITEMS)
        val past = all
            .filter { it.isPast }
            .sortedBy { it.daysRemaining }
            .take(MAX_PAST_ITEMS)
        upcoming + past
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}
