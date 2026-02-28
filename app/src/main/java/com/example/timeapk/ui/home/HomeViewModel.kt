package com.example.timeapk.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timeapk.data.DEFAULT_MILESTONE_DAYS
import com.example.timeapk.data.Event
import com.example.timeapk.data.UserPreferencesRepository
import com.example.timeapk.notifications.cancelReminder
import com.example.timeapk.notifications.scheduleReminder
import com.example.timeapk.widget.WidgetUpdater
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.data.EventRepository
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
    val targetDate = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()
    val hasStarted = !targetDate.isAfter(today)
    var nextTargetDate = targetDate
    // 上次发生日：用于计算重复类型事件「已经过去多少天」
    var prevTargetDate: LocalDate? = null

    when (repeatType) {
        REPEAT_YEARLY -> {
            if (hasStarted) {
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
        REPEAT_HALF_YEARLY -> {
            var next = targetDate
            while (next.isBefore(today)) {
                next = next.plusMonths(6)
            }
            nextTargetDate = next
            prevTargetDate = if (next != targetDate) next.minusMonths(6) else null
        }
        REPEAT_MONTHLY -> {
            var next = targetDate
            while (next.isBefore(today)) {
                next = next.plusMonths(1)
            }
            nextTargetDate = next
            prevTargetDate = if (next != targetDate) next.minusMonths(1) else null
        }
        else -> { /* REPEAT_NONE: prevTargetDate remains null */ }
    }

    val daysDiff = ChronoUnit.DAYS.between(today, nextTargetDate)
    val nextTargetInstant = nextTargetDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
    val isPast = daysDiff < 0
    val daysRemainingAbs = kotlin.math.abs(daysDiff)
    val daysElapsed = if (isPast) daysRemainingAbs else 0L
    val daysLeft = if (isPast) 0L else daysRemainingAbs
    // 重复事件未到时：daysPassed = 距上次发生已过天数；一次性事件已过时 = daysRemainingAbs；否则 = 0
    val daysPassed = when {
        isPast -> daysRemainingAbs
        repeatType == REPEAT_YEARLY && hasStarted -> maxOf(0L, ChronoUnit.DAYS.between(targetDate, today))
        prevTargetDate != null -> maxOf(0L, ChronoUnit.DAYS.between(prevTargetDate, today))
        else -> 0L
    }
    // 仅在距离目标小于 24 小时时显示"小时"，避免远期事件显示 23 小时的错误
    val totalHours = ChronoUnit.HOURS.between(Instant.now(), nextTargetInstant)
    val hoursRemaining = if (!isPast && totalHours in 0..23) {
        totalHours
    } else {
        0L
    }
    val (nextMilestoneDays, nextMilestoneValue) = computeNextMilestone(
        milestones = milestones,
        isPast = isPast,
        daysRemainingAbs = daysRemainingAbs,
        daysElapsed = daysElapsed
    )
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

/**
 * 安全地将日期更换年份，处理闰日（如 2000-02-29）在非闰年时自动回退到 02-28。
 */
private fun safeWithYear(date: LocalDate, year: Int): LocalDate? {
    return try {
        date.withYear(year)
    } catch (_: Exception) {
        try { LocalDate.of(year, date.monthValue, 28) } catch (_: Exception) { null }
    }
}

private fun computeNextMilestone(
    milestones: List<Long>,
    isPast: Boolean,
    daysRemainingAbs: Long,
    daysElapsed: Long
): Pair<Long?, Long?> {
    val list = milestones.filter { it > 0 }.distinct().sorted()
    if (list.isEmpty()) return null to null
    // 若已经开始（包括进行中的纪念日/生日），以已过天数为基准；否则以剩余天数为基准
    val current = if (daysElapsed > 0) daysElapsed else daysRemainingAbs
    val next = list.firstOrNull { it > current }
    if (next == null) return null to null
    val daysUntilMilestone = next - current
    return daysUntilMilestone to next
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
        /** 列表分页：最多展示的未来事件数量 */
        private const val MAX_UPCOMING_ITEMS = 100

        /** 列表分页：最多展示的最近已过事件数量 */
        private const val MAX_PAST_ITEMS = 50
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            cancelReminder(application, event.id)
            repository.deleteEvent(event)
            WidgetUpdater.refreshCountdownWidgets(application)
        }
    }

    fun restoreEvent(event: Event) {
        viewModelScope.launch {
            repository.insertEvent(event)
            scheduleReminder(application, event)
            WidgetUpdater.refreshCountdownWidgets(application)
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
