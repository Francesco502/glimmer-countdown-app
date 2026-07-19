package com.example.timeapk.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.DEFAULT_MILESTONE_DAYS
import com.example.timeapk.data.Event
import com.example.timeapk.data.EventRepository
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.data.UserPreferencesRepository
import com.example.timeapk.notifications.ScheduleSyncManager
import com.example.timeapk.notifications.eventAfterScheduleSyncAttempt
import com.example.timeapk.notifications.cancelMilestoneReminders
import com.example.timeapk.notifications.cancelReminder
import com.example.timeapk.notifications.rescheduleMilestoneReminders
import com.example.timeapk.notifications.scheduleReminder
import com.example.timeapk.notifications.syncMilestoneReminderForEvent
import com.example.timeapk.notifications.enqueueMilestoneScheduleRetry
import com.example.timeapk.notifications.eventAfterMilestoneScheduleSyncAttempt
import com.example.timeapk.notifications.requestMilestoneScheduleRetryOnFailure
import com.example.timeapk.notifications.recordManagedCalendarCleanupForMilestoneOwnership
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.getNextLunarOccurrence
import com.example.timeapk.ui.utils.getPreviousLunarOccurrence
import com.example.timeapk.ui.utils.nextOccurrenceDate
import com.example.timeapk.ui.utils.previousOccurrenceDate
import com.example.timeapk.widget.WidgetUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

    val nextMilestone = when {
        repeatType == REPEAT_NONE && !isPast -> {
            computeNextCountdownMilestone(
                milestones = milestones,
                daysRemaining = daysRemainingAbs,
                smartMilestonesEnabled = smartMilestonesEnabled,
                category = category
            )
        }
        else -> {
            computeNextMilestone(
                milestones = milestones,
                current = milestoneCurrent,
                smartMilestonesEnabled = smartMilestonesEnabled,
                category = category
            )
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
        nextMilestoneDays = nextMilestone?.daysUntil,
        nextMilestoneValue = nextMilestone?.value,
        nextOccurrenceDate = nextTargetDate,
        nextMilestoneReason = nextMilestone?.reason
    )
}

private fun computeNextMilestone(
    milestones: List<Long>,
    current: Long,
    smartMilestonesEnabled: Boolean,
    category: String
): MilestoneSelection? {
    val candidates = buildProgressMilestoneCandidates(
        customMilestones = milestones,
        current = current,
        smartEnabled = smartMilestonesEnabled,
        category = category
    )
    val next = candidates
        .filter { it.value > current }
        .sortedWith(
            compareByDescending<MilestoneCandidate> { progressMilestoneScore(it, current) }
                .thenBy { it.value - current }
        )
        .firstOrNull() ?: return null

    return MilestoneSelection(
        daysUntil = next.value - current,
        value = next.value,
        reason = next.reason
    )
}

private fun computeNextCountdownMilestone(
    milestones: List<Long>,
    daysRemaining: Long,
    smartMilestonesEnabled: Boolean,
    category: String
): MilestoneSelection? {
    val candidates = buildCountdownMilestoneCandidates(
        customMilestones = milestones,
        daysRemaining = daysRemaining,
        smartEnabled = smartMilestonesEnabled,
        category = category
    )
    val next = candidates
        .filter { it.value in 1..daysRemaining }
        .sortedWith(
            compareByDescending<MilestoneCandidate> { countdownMilestoneScore(it, daysRemaining) }
                .thenBy { daysRemaining - it.value }
        )
        .firstOrNull() ?: return null

    return MilestoneSelection(
        daysUntil = daysRemaining - next.value,
        value = next.value,
        reason = next.reason
    )
}

private fun buildProgressMilestoneCandidates(
    customMilestones: List<Long>,
    current: Long,
    smartEnabled: Boolean,
    category: String
): List<MilestoneCandidate> {
    val custom = customMilestones
        .filter { it > 0 }
        .map { MilestoneCandidate(it, MilestoneReason.CUSTOM, priority = 82) }
    if (!smartEnabled) return dedupeMilestoneCandidates(custom)

    val dynamic = mutableListOf<MilestoneCandidate>()
    val step = when {
        current < 30 -> 1L
        current < 200 -> 5L
        current < 1000 -> 10L
        current < 5000 -> 50L
        else -> 100L
    }
    val start = ((current / step) + 1) * step
    repeat(8) { idx ->
        dynamic += MilestoneCandidate(
            value = start + idx * step,
            reason = MilestoneReason.DYNAMIC_STEP,
            priority = 24
        )
    }

    val base = SMART_BASE_MILESTONES.map {
        MilestoneCandidate(
            value = it,
            reason = MilestoneReason.ROUND_NUMBER,
            priority = progressBasePriority(it)
        )
    }
    val typed = when (category) {
        CATEGORY_BIRTHDAY -> buildYearCycleMilestones(
            current = current,
            yearReason = MilestoneReason.BIRTHDAY_YEAR,
            halfYearReason = MilestoneReason.BIRTHDAY_HALF_YEAR,
            yearPriority = 96,
            halfYearPriority = 86
        )
        CATEGORY_ANNIVERSARY -> buildYearCycleMilestones(
            current = current,
            yearReason = MilestoneReason.ANNIVERSARY_YEAR,
            halfYearReason = MilestoneReason.ANNIVERSARY_HALF_YEAR,
            yearPriority = 98,
            halfYearPriority = 74
        )
        else -> emptyList()
    }

    return dedupeMilestoneCandidates(custom + base + typed + dynamic)
}

private fun buildCountdownMilestoneCandidates(
    customMilestones: List<Long>,
    daysRemaining: Long,
    smartEnabled: Boolean,
    category: String
): List<MilestoneCandidate> {
    if (daysRemaining <= 0) return emptyList()

    val custom = customMilestones
        .filter { it > 0 && it <= daysRemaining }
        .map { MilestoneCandidate(it, MilestoneReason.CUSTOM, priority = 84) }
    if (!smartEnabled) return dedupeMilestoneCandidates(custom)

    val dynamic = mutableListOf<MilestoneCandidate>()
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
        if (value > 0) {
            dynamic += MilestoneCandidate(
                value = value,
                reason = MilestoneReason.DYNAMIC_STEP,
                priority = 18
            )
        }
    }

    val base = SMART_BASE_MILESTONES
        .filter { it <= daysRemaining }
        .map {
            MilestoneCandidate(
                value = it,
                reason = MilestoneReason.COUNTDOWN_THRESHOLD,
                priority = countdownBasePriority(it, category)
            )
        }

    return dedupeMilestoneCandidates(custom + base + dynamic)
        .filter { it.value <= daysRemaining }
}

private fun buildYearCycleMilestones(
    current: Long,
    yearReason: MilestoneReason,
    halfYearReason: MilestoneReason,
    yearPriority: Int,
    halfYearPriority: Int
): List<MilestoneCandidate> {
    val startCycle = maxOf(0L, current / 365L - 1L)
    return (startCycle..startCycle + 6L).flatMap { cycle ->
        listOf(
            MilestoneCandidate(
                value = cycle * 365L + 183L,
                reason = halfYearReason,
                priority = halfYearPriority
            ),
            MilestoneCandidate(
                value = (cycle + 1L) * 365L,
                reason = yearReason,
                priority = yearPriority
            )
        )
    }
}

private fun dedupeMilestoneCandidates(candidates: List<MilestoneCandidate>): List<MilestoneCandidate> {
    val byValue = linkedMapOf<Long, MilestoneCandidate>()
    candidates.filter { it.value > 0 }.forEach { candidate ->
        val existing = byValue[candidate.value]
        if (existing == null || candidate.priority > existing.priority) {
            byValue[candidate.value] = candidate
        }
    }
    return byValue.values.toList()
}

private fun progressBasePriority(value: Long): Int = when (value) {
    100L, 365L, 520L, 1000L -> 78
    7L, 30L, 90L, 180L, 730L -> 66
    else -> 52
}

private fun countdownBasePriority(value: Long, category: String): Int {
    val categoryBoost = when (category) {
        CATEGORY_BIRTHDAY, CATEGORY_ANNIVERSARY -> 4
        else -> 0
    }
    return when (value) {
        1L, 3L, 7L, 14L, 30L, 60L, 90L, 100L -> 82 + categoryBoost
        180L, 270L, 365L -> 72 + categoryBoost
        else -> 62 + categoryBoost
    }
}

private fun progressMilestoneScore(candidate: MilestoneCandidate, current: Long): Double {
    val daysUntil = candidate.value - current
    val distancePenalty = when {
        daysUntil <= 7L -> daysUntil * 0.75
        daysUntil <= 30L -> daysUntil * 0.55
        daysUntil <= 120L -> daysUntil * 0.38
        else -> daysUntil * 0.18
    }
    return candidate.priority - distancePenalty
}

private fun countdownMilestoneScore(candidate: MilestoneCandidate, daysRemaining: Long): Double {
    val daysUntil = daysRemaining - candidate.value
    val distancePenalty = when {
        daysUntil <= 7L -> daysUntil * 0.75
        daysUntil <= 30L -> daysUntil * 0.48
        daysUntil <= 120L -> daysUntil * 0.28
        else -> daysUntil * 0.14
    }
    return candidate.priority - distancePenalty
}

private data class MilestoneCandidate(
    val value: Long,
    val reason: MilestoneReason,
    val priority: Int
)

private data class MilestoneSelection(
    val daysUntil: Long,
    val value: Long,
    val reason: MilestoneReason
)

enum class MilestoneReason {
    CUSTOM,
    DYNAMIC_STEP,
    ROUND_NUMBER,
    BIRTHDAY_HALF_YEAR,
    BIRTHDAY_YEAR,
    ANNIVERSARY_HALF_YEAR,
    ANNIVERSARY_YEAR,
    COUNTDOWN_THRESHOLD
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
    val nextOccurrenceDate: LocalDate,
    val nextMilestoneReason: MilestoneReason? = null
)

class HomeViewModel(
    private val application: Application,
    private val repository: EventRepository,
    private val userPrefs: UserPreferencesRepository
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            userPrefs.migrateHomeSortToCustomIfNeeded()
        }
    }

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filterType: StateFlow<FilterType> = userPrefs.filterTypeFlow
        .map { FilterType.entries.getOrNull(it) ?: FilterType.All }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FilterType.All)

    val sortType: StateFlow<SortType> = userPrefs.sortTypeFlow
        .map { SortType.entries.getOrNull(it) ?: SortType.Custom }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SortType.Custom)

    val smartMilestonesEnabled: StateFlow<Boolean> = userPrefs.smartMilestonesEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val allHomeUiState: StateFlow<List<EventUiState>> = combine(
        repository.getAllEvents(),
        userPrefs.customMilestonesFlow,
        smartMilestonesEnabled,
        minuteTickerFlow()
    ) { events: List<Event>, milestones: List<Long>, smartEnabled: Boolean, _: Long ->
        events.map { it.toEventUiState(milestones, smartEnabled) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private data class FilterInput(
        val filterType: FilterType,
        val sortType: SortType,
        val query: String
    )

    private data class OrderInput(
        val customEventOrderIds: List<Int>,
        val pinnedEventIds: List<Int>
    )

    private val filterInputFlow: Flow<FilterInput> = combine(
        filterType,
        sortType,
        searchQuery
    ) { filter, sort, query ->
        FilterInput(
            filterType = filter,
            sortType = sort,
            query = query
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
        allHomeUiState,
        filterInputFlow,
        orderInputFlow
    ) { all, filterInput, orderInput ->
        buildHomeVisibleList(
            all = all,
            filterType = filterInput.filterType,
            sortType = filterInput.sortType,
            query = filterInput.query,
            customEventOrderIds = orderInput.customEventOrderIds,
            pinnedEventIds = orderInput.pinnedEventIds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val calendarUiState: StateFlow<List<EventUiState>> = homeUiState
    val unfilteredCalendarUiState: StateFlow<List<EventUiState>> = allHomeUiState

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilterType(type: FilterType) {
        viewModelScope.launch {
            userPrefs.setFilterType(type.ordinal)
        }
    }

    fun updateSortType(type: SortType) {
        viewModelScope.launch {
            userPrefs.setSortType(type.ordinal)
            WidgetUpdater.refreshCountdownWidgets(application)
        }
    }

    fun updateCustomEventOrder(
        visibleIds: List<Int>,
        reorderedVisibleIds: List<Int>,
        onPersistenceResult: (List<Int>?) -> Unit = {}
    ) {
        viewModelScope.launch {
            var persisted = false
            try {
                val allEvents = repository.getAllEventsSnapshot()
                val activeIds = allEvents.mapTo(mutableSetOf()) { it.id }
                val storedIds = userPrefs.customEventOrderFlow.first()
                val defaultIds = defaultCustomEventOrderIds(allEvents)
                val globalIds = (storedIds + defaultIds)
                    .filter { it in activeIds }
                    .distinct()
                val activeVisibleIds = visibleIds.filter { it in activeIds }
                val activeReorderedIds = reorderedVisibleIds.filter { it in activeIds }
                val mergedIds = mergeVisibleOrderIntoGlobalOrder(
                    globalIds = globalIds,
                    visibleIds = activeVisibleIds,
                    reorderedVisibleIds = activeReorderedIds
                )
                userPrefs.setCustomEventOrder(mergedIds)
                persisted = true
                onPersistenceResult(mergedIds)
                WidgetUpdater.refreshCountdownWidgets(application)
            } catch (cancelled: CancellationException) {
                if (!persisted) onPersistenceResult(null)
                throw cancelled
            } catch (_: Exception) {
                if (!persisted) onPersistenceResult(null)
            }
        }
    }

    suspend fun deleteEvent(event: Event): DeleteEventResult = deleteEventRecoverably(
        event = event,
        nowMillis = System::currentTimeMillis,
        cleanup = { target ->
            recordManagedCalendarCleanupForMilestoneOwnership(
                context = application,
                eventId = target.id,
                result = ScheduleSyncManager.removeManagedCalendarEntries(
                    context = application,
                    eventId = target.id,
                    calendarEventId = target.scheduleEventId
                )
            )
        },
        update = repository::updateEvent,
        cancelReminder = { target -> cancelReminder(application, target.id) },
        cancelMilestones = { target -> cancelMilestoneReminders(application, target.id) },
        delete = repository::deleteEvent,
        refreshWidgets = { WidgetUpdater.refreshCountdownWidgets(application) }
    )

    fun restoreEvent(event: Event) {
        viewModelScope.launch {
            try {
                val newId = repository.insertEvent(event)
                val savedEvent = event.copy(id = newId.toInt())
                scheduleReminder(application, savedEvent)
                val updatedEvent = if (savedEvent.syncToScheduleEnabled) {
                    val preferredCalendarId = userPrefs.scheduleTargetCalendarIdFlow.first()
                    val useRRuleSync = userPrefs.scheduleUseRRuleSyncFlow.first()
                    val syncResult = ScheduleSyncManager.syncReminderSeries(
                        context = application,
                        event = savedEvent,
                        preferredCalendarId = preferredCalendarId,
                        useRRuleSync = useRRuleSync
                    )
                    eventAfterScheduleSyncAttempt(savedEvent, syncResult)
                } else {
                    val cleanup = ScheduleSyncManager.removeScheduleReminderByEventId(
                        application,
                        savedEvent.id
                    )
                    eventAfterCleanupAttempt(
                        event = savedEvent,
                        result = cleanup,
                        nowMillis = System.currentTimeMillis()
                    )
                }
                repository.updateEvent(updatedEvent)
                val milestoneResult = try {
                    syncMilestoneReminderForEvent(application, updatedEvent)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    enqueueMilestoneScheduleRetry(application)
                    throw error
                }
                val milestoneUpdatedEvent = eventAfterMilestoneScheduleSyncAttempt(
                    updatedEvent,
                    milestoneResult
                )
                requestMilestoneScheduleRetryOnFailure(milestoneResult?.error) {
                    enqueueMilestoneScheduleRetry(application)
                }
                if (milestoneUpdatedEvent != updatedEvent) {
                    repository.updateEvent(milestoneUpdatedEvent)
                }
                WidgetUpdater.refreshCountdownWidgets(application)
            } catch (cancelled: CancellationException) {
                throw cancelled
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
