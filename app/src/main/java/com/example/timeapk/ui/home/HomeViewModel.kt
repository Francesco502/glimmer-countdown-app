package com.example.timeapk.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.DEFAULT_MILESTONE_DAYS
import com.example.timeapk.data.Event
import com.example.timeapk.data.EventRepository
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.data.UserPreferencesRepository
import com.example.timeapk.data.hasTag
import com.example.timeapk.data.structuredTags
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private val SMART_BASE_MILESTONES = listOf(
    1L, 3L, 7L, 14L, 30L, 60L, 90L, 100L,
    180L, 270L, 365L, 520L, 730L, 1000L,
    1500L, 2000L, 3000L, 5000L, 10000L
)

fun Event.toEventUiState(
    milestones: List<Long> = DEFAULT_MILESTONE_DAYS,
    smartMilestonesEnabled: Boolean = true
): EventUiState {
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
    val daysRemainingAbs = abs(daysDiff)
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
        repeatType == REPEAT_NONE && !isPast -> {
            computeNextCountdownMilestone(milestones, daysRemainingAbs, smartMilestonesEnabled)
        }
        else -> {
            computeNextMilestone(milestones, milestoneCurrent, smartMilestonesEnabled)
        }
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

private fun computeNextMilestone(
    milestones: List<Long>,
    current: Long,
    smartMilestonesEnabled: Boolean
): Pair<Long?, Long?> {
    val list = buildProgressMilestonePool(milestones, current, smartMilestonesEnabled)
    if (list.isEmpty()) return null to null
    val next = list.firstOrNull { it > current } ?: return null to null
    return (next - current) to next
}

private fun computeNextCountdownMilestone(
    milestones: List<Long>,
    daysRemaining: Long,
    smartMilestonesEnabled: Boolean
): Pair<Long?, Long?> {
    val list = buildCountdownMilestonePool(milestones, daysRemaining, smartMilestonesEnabled)
    if (list.isEmpty()) return null to null
    val next = list.maxOrNull() ?: return null to null
    return (daysRemaining - next) to next
}

private fun buildProgressMilestonePool(
    customMilestones: List<Long>,
    current: Long,
    smartEnabled: Boolean
): List<Long> {
    val base = customMilestones.filter { it > 0 }
    if (!smartEnabled) return base.distinct().sorted()

    val dynamic = mutableListOf<Long>()
    val step = when {
        current < 30 -> 1L
        current < 200 -> 5L
        current < 1000 -> 10L
        current < 5000 -> 50L
        else -> 100L
    }
    val start = ((current / step) + 1) * step
    repeat(8) { idx -> dynamic += start + idx * step }

    return (base + SMART_BASE_MILESTONES + dynamic)
        .filter { it > 0 }
        .distinct()
        .sorted()
}

private fun buildCountdownMilestonePool(
    customMilestones: List<Long>,
    daysRemaining: Long,
    smartEnabled: Boolean
): List<Long> {
    if (daysRemaining <= 0) return emptyList()

    val base = customMilestones.filter { it > 0 && it <= daysRemaining }
    if (!smartEnabled) return base.distinct().sorted()

    val dynamic = mutableListOf<Long>()
    val step = when {
        daysRemaining < 30 -> 1L
        daysRemaining < 200 -> 5L
        daysRemaining < 1000 -> 10L
        daysRemaining < 5000 -> 50L
        else -> 100L
    }
    val floor = (daysRemaining / step) * step
    repeat(8) { idx ->
        val value = floor - idx * step
        if (value > 0) dynamic += value
    }

    return (base + SMART_BASE_MILESTONES.filter { it <= daysRemaining } + dynamic)
        .filter { it > 0 && it <= daysRemaining }
        .distinct()
        .sorted()
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

    private val _searchQuery = MutableStateFlow("")
    private val _selectedTag = MutableStateFlow<String?>(null)

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    val filterType: StateFlow<FilterType> = userPrefs.filterTypeFlow
        .map { FilterType.entries.getOrNull(it) ?: FilterType.All }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FilterType.All)

    val sortType: StateFlow<SortType> = userPrefs.sortTypeFlow
        .map { SortType.entries.getOrNull(it) ?: SortType.ByDays }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SortType.ByDays)

    val smartMilestonesEnabled: StateFlow<Boolean> = userPrefs.smartMilestonesEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val baseHomeUiState: StateFlow<List<EventUiState>> = combine(
        repository.getAllEvents(),
        userPrefs.customMilestonesFlow,
        smartMilestonesEnabled,
        minuteTickerFlow()
    ) { events: List<Event>, milestones: List<Long>, smartEnabled: Boolean, _: Long ->
        val all = events.map { it.toEventUiState(milestones, smartEnabled) }
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

    val availableTags: StateFlow<List<String>> = baseHomeUiState
        .map { states ->
            states
                .flatMap { it.event.structuredTags().map { tag -> tag.label } }
                .distinctBy { it.lowercase() }
                .sortedBy { it.lowercase() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class FilterInput(
        val filterType: FilterType,
        val sortType: SortType,
        val query: String,
        val selectedTag: String?
    )

    private data class OrderInput(
        val customEventOrderIds: List<Int>,
        val pinnedEventIds: List<Int>
    )

    private val filterInputFlow: Flow<FilterInput> = combine(
        filterType,
        sortType,
        searchQuery,
        selectedTag
    ) { filter, sort, query, tag ->
        FilterInput(
            filterType = filter,
            sortType = sort,
            query = query,
            selectedTag = tag
        )
    }

    private val orderInputFlow: Flow<OrderInput> = combine(
        userPrefs.customEventOrderFlow,
        userPrefs.pinnedEventIdsFlow
    ) { customOrderIds, pinnedIds ->
        OrderInput(
            customEventOrderIds = customOrderIds,
            pinnedEventIds = pinnedIds
        )
    }

    val homeUiState: StateFlow<List<EventUiState>> = combine(
        baseHomeUiState,
        filterInputFlow,
        orderInputFlow
    ) { base, filterInput, orderInput ->
        var list = when (filterInput.filterType) {
            FilterType.All -> base
            FilterType.Birthday -> base.filter { it.event.category == CATEGORY_BIRTHDAY }
            FilterType.Anniversary -> base.filter { it.event.category == CATEGORY_ANNIVERSARY }
            FilterType.Other -> base.filter { it.event.category == CATEGORY_OTHER }
        }

        val selectedTagKey = filterInput.selectedTag?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        if (selectedTagKey != null) {
            list = list.filter { state -> state.event.hasTag(selectedTagKey) }
        }

        val q = filterInput.query.trim().lowercase()
        if (q.isNotBlank()) {
            list = list.filter { state ->
                state.event.title.lowercase().contains(q) ||
                    state.event.note.lowercase().contains(q) ||
                    state.event.category.lowercase().contains(q) ||
                    state.event.structuredTags().any { it.normalized.contains(q) }
            }
        }

        list = when (filterInput.sortType) {
            SortType.ByDays -> list.sortedBy { it.daysRemaining }
            SortType.ByDate -> list.sortedBy { it.event.date }
            SortType.ByCreated -> list.sortedByDescending { it.event.createdAt }
        }

        if (orderInput.customEventOrderIds.isNotEmpty() && filterInput.sortType == SortType.ByCreated) {
            list = list.sortedBy { item ->
                val index = orderInput.customEventOrderIds.indexOf(item.event.id)
                if (index < 0) Int.MAX_VALUE else index
            }
        }

        if (orderInput.pinnedEventIds.isNotEmpty()) {
            val pinnedSet = orderInput.pinnedEventIds.toSet()
            val pinned = orderInput.pinnedEventIds.mapNotNull { id -> list.find { it.event.id == id } }
            val unpinned = list.filter { it.event.id !in pinnedSet }
            list = pinned + unpinned
        }

        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            combine(availableTags, selectedTag) { tags, selected ->
                selected != null && tags.none { it.equals(selected, ignoreCase = true) }
            }.collect { invalid ->
                if (invalid) {
                    _selectedTag.value = null
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedTag(tag: String?) {
        _selectedTag.value = tag?.takeIf { it.isNotBlank() }
    }

    fun updateFilterType(type: FilterType) {
        viewModelScope.launch {
            userPrefs.setFilterType(type.ordinal)
        }
    }

    fun updateSortType(type: SortType) {
        viewModelScope.launch {
            userPrefs.setSortType(type.ordinal)
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            cancelReminder(application, event.id)
            cancelMilestoneReminders(application, event.id)
            ScheduleSyncManager.removeScheduleReminder(application, event.scheduleEventId)
            ScheduleSyncManager.removeScheduleReminderByEventId(application, event.id)
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
                    ScheduleSyncManager.upsertScheduleReminder(
                        context = application,
                        event = savedEvent,
                        currentScheduleEventId = savedEvent.scheduleEventId
                    )
                } else {
                    ScheduleSyncManager.removeScheduleReminderByEventId(application, savedEvent.id)
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

    fun setSmartMilestonesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setSmartMilestonesEnabled(enabled)
            rescheduleMilestoneReminders(application)
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
}
