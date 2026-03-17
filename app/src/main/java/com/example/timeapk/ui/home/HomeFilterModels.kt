package com.example.timeapk.ui.home

enum class FilterType { All, Birthday, Anniversary, Other }

enum class SortType { ByDays, ByDate, Custom }

internal fun homeCardLongPressEditEnabled(sortType: SortType): Boolean = false

internal fun homeCardUsesTapOnlyInteraction(sortType: SortType): Boolean = false

internal fun homeCardTapNavigationEnabled(sortType: SortType): Boolean = true

internal fun homeCardDragSortEnabled(sortType: SortType): Boolean = sortType == SortType.Custom

internal fun homeUsesListLevelReorderDetection(sortType: SortType): Boolean = false

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
            val base = list.sortedByDescending { it.event.createdAt }
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
