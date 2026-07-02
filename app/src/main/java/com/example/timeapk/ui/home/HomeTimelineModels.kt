package com.example.timeapk.ui.home

import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class HomeTimelineDigest(
    val today: TimelineBucket,
    val sevenDays: TimelineBucket,
    val month: TimelineBucket,
    val milestone: TimelineBucket
)

data class TimelineBucket(
    val type: TimelineBucketType,
    val count: Int,
    val topItem: EventUiState?
)

enum class TimelineBucketType {
    Today,
    SevenDays,
    Month,
    Milestone
}

fun buildHomeTimelineDigest(
    events: List<EventUiState>,
    today: LocalDate,
    month: YearMonth = YearMonth.from(today),
    query: String = "",
    filterType: FilterType = FilterType.All,
    pinnedEventIds: List<Int> = emptyList()
): HomeTimelineDigest {
    val filtered = events
        .filter { !it.isPast }
        .filter { it.nextOccurrenceDate >= today }
        .filter { it.matchesFilter(filterType) }
        .filter { it.matchesQuery(query) }

    val todayItems = filterEventsForTimelineBucket(filtered, today, TimelineBucketType.Today, month)
    val sevenDayItems = filterEventsForTimelineBucket(filtered, today, TimelineBucketType.SevenDays, month)
    val monthItems = filterEventsForTimelineBucket(filtered, today, TimelineBucketType.Month, month)
    val milestoneItems = filterEventsForTimelineBucket(filtered, today, TimelineBucketType.Milestone, month)

    return HomeTimelineDigest(
        today = TimelineBucket(
            type = TimelineBucketType.Today,
            count = todayItems.size,
            topItem = todayItems.topTimelineItem(today, pinnedEventIds)
        ),
        sevenDays = TimelineBucket(
            type = TimelineBucketType.SevenDays,
            count = sevenDayItems.size,
            topItem = sevenDayItems.topTimelineItem(today, pinnedEventIds)
        ),
        month = TimelineBucket(
            type = TimelineBucketType.Month,
            count = monthItems.size,
            topItem = monthItems.topTimelineItem(today, pinnedEventIds)
        ),
        milestone = TimelineBucket(
            type = TimelineBucketType.Milestone,
            count = milestoneItems.size,
            topItem = milestoneItems.topMilestoneItem(pinnedEventIds)
        )
    )
}

fun filterEventsForTimelineBucket(
    events: List<EventUiState>,
    today: LocalDate,
    type: TimelineBucketType,
    month: YearMonth = YearMonth.from(today)
): List<EventUiState> {
    val upcoming = events
        .filter { !it.isPast }
        .filter { it.nextOccurrenceDate >= today }

    return when (type) {
        TimelineBucketType.Today -> upcoming.filter { it.nextOccurrenceDate == today }
        TimelineBucketType.SevenDays -> upcoming.filter {
            it.nextOccurrenceDate.isAfter(today) &&
                !it.nextOccurrenceDate.isAfter(today.plusDays(7))
        }
        TimelineBucketType.Month -> upcoming.filter {
            YearMonth.from(it.nextOccurrenceDate) == month
        }
        TimelineBucketType.Milestone -> upcoming.filter {
            val days = it.nextMilestoneDays
            days != null && days in 0L..30L
        }
    }
}

private fun EventUiState.matchesFilter(filterType: FilterType): Boolean {
    return when (filterType) {
        FilterType.All -> true
        FilterType.Birthday -> event.category == CATEGORY_BIRTHDAY
        FilterType.Anniversary -> event.category == CATEGORY_ANNIVERSARY
        FilterType.Other -> event.category == CATEGORY_OTHER
    }
}

private fun EventUiState.matchesQuery(query: String): Boolean {
    val normalized = query.trim().lowercase()
    if (normalized.isBlank()) return true
    return event.title.lowercase().contains(normalized) ||
        event.note.lowercase().contains(normalized) ||
        event.category.lowercase().contains(normalized)
}

private fun List<EventUiState>.topTimelineItem(
    today: LocalDate,
    pinnedEventIds: List<Int>
): EventUiState? {
    if (isEmpty()) return null
    return sortedWith(
        compareBy<EventUiState> { ChronoUnit.DAYS.between(today, it.nextOccurrenceDate) }
            .thenBy { categoryPriority(it.event.category) }
            .thenBy { pinnedPriority(it.event.id, pinnedEventIds) }
            .thenByDescending { it.event.createdAt }
            .thenBy { it.event.id }
    ).first()
}

private fun List<EventUiState>.topMilestoneItem(
    pinnedEventIds: List<Int>
): EventUiState? {
    if (isEmpty()) return null
    return sortedWith(
        compareBy<EventUiState> { it.nextMilestoneDays ?: Long.MAX_VALUE }
            .thenBy { categoryPriority(it.event.category) }
            .thenBy { pinnedPriority(it.event.id, pinnedEventIds) }
            .thenByDescending { it.event.createdAt }
            .thenBy { it.event.id }
    ).first()
}

private fun categoryPriority(category: String): Int {
    return when (category) {
        CATEGORY_BIRTHDAY -> 0
        CATEGORY_ANNIVERSARY -> 1
        else -> 2
    }
}

private fun pinnedPriority(eventId: Int, pinnedEventIds: List<Int>): Int {
    val index = pinnedEventIds.indexOf(eventId)
    return if (index >= 0) index else Int.MAX_VALUE
}
