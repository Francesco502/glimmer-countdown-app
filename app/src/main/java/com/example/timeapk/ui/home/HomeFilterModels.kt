package com.example.timeapk.ui.home

import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event

enum class FilterType { All, Birthday, Anniversary, Other }

enum class SortType { ByDays, ByDate, Custom }

internal fun homeCardLongPressEditEnabled(sortType: SortType): Boolean = false

internal fun homeCardUsesTapOnlyInteraction(sortType: SortType): Boolean = false

internal fun homeCardTapNavigationEnabled(sortType: SortType): Boolean = true

internal fun homeCardDragSortEnabled(sortType: SortType): Boolean = sortType == SortType.Custom

internal fun homeUsesListLevelReorderDetection(sortType: SortType): Boolean =
    false

internal fun mergeVisibleOrderIntoGlobalOrder(
    globalIds: List<Int>,
    visibleIds: List<Int>,
    reorderedVisibleIds: List<Int>
): List<Int> {
    val canonicalGlobal = globalIds.distinct()
    val canonicalVisible = visibleIds.distinct()
    val completeGlobal = (canonicalGlobal + canonicalVisible).distinct()

    val reorderedDistinct = reorderedVisibleIds.distinct()
    val isValidReorder = visibleIds.size == canonicalVisible.size &&
        reorderedVisibleIds.size == reorderedDistinct.size &&
        reorderedDistinct.size == canonicalVisible.size &&
        reorderedDistinct.toSet() == canonicalVisible.toSet()
    if (!isValidReorder) return completeGlobal

    val visibleSet = canonicalVisible.toSet()
    val replacements = reorderedDistinct.iterator()
    return completeGlobal.map { id ->
        if (id in visibleSet) replacements.next() else id
    }
}

internal fun defaultCustomEventOrderIds(events: List<Event>): List<Int> =
    events.sortedWith(
        compareByDescending<Event> { it.createdAt }
            .thenBy { it.id }
    ).map { it.id }

internal fun applyHomeSort(
    list: List<EventUiState>,
    sortType: SortType,
    customEventOrderIds: List<Int>,
    pinnedEventIds: List<Int>
): List<EventUiState> {
    val sorted = when (sortType) {
        SortType.ByDays -> list.sortedBy { it.daysRemaining }
        SortType.ByDate -> list.sortedBy { it.event.date }
        SortType.Custom -> {
            val base = list.sortedWith(
                compareByDescending<EventUiState> { it.event.createdAt }
                    .thenBy { it.event.id }
            )
            if (customEventOrderIds.isEmpty()) {
                base
            } else {
                base.sortedBy { item ->
                    val index = customEventOrderIds.indexOf(item.event.id)
                    if (index < 0) Int.MAX_VALUE else index
                }
            }
        }
    }

    if (pinnedEventIds.isEmpty()) return sorted

    val pinnedSet = pinnedEventIds.toSet()
    val pinned = pinnedEventIds.mapNotNull { id -> sorted.find { it.event.id == id } }
    val unpinned = sorted.filter { it.event.id !in pinnedSet }
    return pinned + unpinned
}

internal fun buildHomeVisibleList(
    all: List<EventUiState>,
    filterType: FilterType,
    sortType: SortType,
    query: String,
    customEventOrderIds: List<Int>,
    pinnedEventIds: List<Int>
): List<EventUiState> {
    val categoryFiltered = when (filterType) {
        FilterType.All -> all
        FilterType.Birthday -> all.filter { it.event.category == CATEGORY_BIRTHDAY }
        FilterType.Anniversary -> all.filter { it.event.category == CATEGORY_ANNIVERSARY }
        FilterType.Other -> all.filter { it.event.category == CATEGORY_OTHER }
    }
    val normalizedQuery = query.trim().lowercase()
    val searched = if (normalizedQuery.isEmpty()) {
        categoryFiltered
    } else {
        categoryFiltered.filter { state ->
            state.event.title.lowercase().contains(normalizedQuery) ||
                state.event.note.lowercase().contains(normalizedQuery) ||
                state.event.category.lowercase().contains(normalizedQuery)
        }
    }
    return applyHomeSort(
        list = searched,
        sortType = sortType,
        customEventOrderIds = customEventOrderIds,
        pinnedEventIds = pinnedEventIds
    )
}
