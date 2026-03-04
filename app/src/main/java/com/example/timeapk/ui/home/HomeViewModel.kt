package com.example.timeapk.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timeapk.data.DEFAULT_MILESTONE_DAYS
import com.example.timeapk.data.Event
import com.example.timeapk.data.UserPreferencesRepository
import com.example.timeapk.notifications.cancelMilestoneReminders
import com.example.timeapk.notifications.cancelReminder
import com.example.timeapk.notifications.rescheduleMilestoneReminders
import com.example.timeapk.notifications.scheduleReminder
import com.example.timeapk.notifications.ScheduleSyncManager
import com.example.timeapk.widget.WidgetUpdater
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.data.EventRepository
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.getNextLunarOccurrence
import com.example.timeapk.ui.utils.getPreviousLunarOccurrence
import com.example.timeapk.ui.utils.safeWithYear
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun Event.toEventUiState(milestones: List<Long> = DEFAULT_MILESTONE_DAYS): EventUiState {
    val today = LocalDate.now()
    val targetDate = eventDateToLocalDate(date)
    val hasStarted = !targetDate.isAfter(today)
    var nextTargetDate = targetDate
    var prevTargetDate: LocalDate? = null

    when (repeatType) {
        REPEAT_YEARLY -> {
            if (hasStarted) {
                if (isLunar) {
                    val originDate = targetDate
                    nextTargetDate = getNextLunarOccurrence(originDate, today)
                    prevTargetDate = getPreviousLunarOccurrence(originDate, today)
                } else {
                    val currentYearDate = safeWithYear(targetDate, today.year)
                    if (currentYearDate != null && currentYearDate.isBefore(today)) {
                        nextTargetDate = safeWithYear(targetDate, today.year + 1) ?: targetDate
                        prevTargetDate = currentYearDate
                    } else if (currentYearDate != null) {
                        nextTargetDate = currentYearDate
                        prevTargetDate = safeWithYear(targetDate, today.year - 1)
                    }
                }
            }
        }
        REPEAT_HALF_YEARLY -> {
            if (hasStarted) {
                val monthsBetween = ChronoUnit.MONTHS.between(targetDate, today)
                val periods = ((monthsBetween / 6) + 1) * 6
                var next = targetDate.plusMonths(periods)
                if (next.isBefore(today)) next = next.plusMonths(6)
                nextTargetDate = next
                prevTargetDate = next.minusMonths(6)
            }
        }
        REPEAT_MONTHLY -> {
            if (hasStarted) {
                val monthsBetween = ChronoUnit.MONTHS.between(targetDate, today)
                var next = targetDate.plusMonths(monthsBetween + 1)
                if (next.isBefore(today)) next = next.plusMonths(1)
                nextTargetDate = next
                prevTargetDate = next.minusMonths(1)
            }
        }
        else -> { /* REPEAT_NONE: prevTargetDate remains null */ }
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
        nextMilestoneValue = nextMilestoneValue
    )
}

/** @param current 当前基准天数：自开始日起天数（始终递增，里程碑才有意义） */
private fun computeNextMilestone(milestones: List<Long>, current: Long): Pair<Long?, Long?> {
    val list = milestones.filter { it > 0 }.distinct().sorted()
    if (list.isEmpty()) return null to null
    val next = list.firstOrNull { it > current }
    if (next == null) return null to null
    val daysUntilMilestone = next - current
    return daysUntilMilestone to next
}

/** 未来非重复事件：按剩余天数倒计时节点，取「下一个将到达」的节点（剩余天数 ≤ 当前剩余的天数中最大者） */
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
    val nextMilestoneValue: Long? = null
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
                if (savedEvent.remindEnabled && savedEvent.syncToScheduleEnabled) {
                    val scheduleId = ScheduleSyncManager.insertScheduleReminder(application, savedEvent)
                    if (scheduleId != null) {
                        repository.updateEvent(savedEvent.copy(scheduleEventId = scheduleId))
                    }
                }
                rescheduleMilestoneReminders(application)
                WidgetUpdater.refreshCountdownWidgets(application)
            } catch (_: Exception) {
                // 主键冲突或其它异常时静默忽略，避免崩溃
            }
        }
    }

    val homeUiState: StateFlow<List<EventUiState>> = combine(
        repository.getAllEvents(),
        userPrefs.customMilestonesFlow
    ) { events: List<Event>, milestones: List<Long> ->
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
