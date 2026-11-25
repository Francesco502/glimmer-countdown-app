package com.example.timeapk.ui.home

import android.content.Context
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.Event
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.formatLunarDateString
import com.example.timeapk.ui.utils.formatLunarMonthDay

enum class FilterType { All, Birthday, Anniversary, Other }

/** 旧筛选枚举对应的分类 id；null 表示全部。 */
fun FilterType.toCategoryId(): String? = when (this) {
    FilterType.All -> null
    FilterType.Birthday -> CATEGORY_BIRTHDAY
    FilterType.Anniversary -> CATEGORY_ANNIVERSARY
    FilterType.Other -> CATEGORY_OTHER
}

internal fun EventUiState.matchesCategoryFilter(categoryId: String?): Boolean =
    categoryId == null || event.category == categoryId

enum class SortType { ByDays, ByDate, Custom }

internal fun homeCardDragSortEnabled(sortType: SortType): Boolean = sortType == SortType.Custom

internal fun homeReorderAllowed(fromId: Int, toId: Int, pinnedEventIds: List<Int>): Boolean =
    fromId != toId && fromId !in pinnedEventIds && toId !in pinnedEventIds


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
        SortType.ByDays -> list.sortedWith(compareBy<EventUiState> { it.isPast }.thenBy { it.daysRemaining })
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

    val normalizedPinnedEventIds = pinnedEventIds.distinct()
    val pinnedSet = normalizedPinnedEventIds.toSet()
    val pinned = normalizedPinnedEventIds.mapNotNull { id -> sorted.find { it.event.id == id } }
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
): List<EventUiState> = buildHomeVisibleList(
    all = all,
    categoryFilter = filterType.toCategoryId(),
    sortType = sortType,
    query = query,
    customEventOrderIds = customEventOrderIds,
    pinnedEventIds = pinnedEventIds
)

internal fun buildHomeVisibleList(
    all: List<EventUiState>,
    categoryFilter: String?,
    sortType: SortType,
    query: String,
    customEventOrderIds: List<Int>,
    pinnedEventIds: List<Int>,
    categoryLabels: Map<String, String> = emptyMap(),
    context: Context? = null
): List<EventUiState> {
    val categoryFiltered = all.filter { !it.event.isArchived && it.matchesCategoryFilter(categoryFilter) }
    val normalizedQuery = query.trim().lowercase()
    val searched = if (normalizedQuery.isEmpty()) {
        categoryFiltered
    } else {
        categoryFiltered.filter { state ->
            state.event.title.lowercase().contains(normalizedQuery) ||
                state.event.note.lowercase().contains(normalizedQuery) ||
                state.event.category.lowercase().contains(normalizedQuery) ||
                categoryLabels[state.event.category]?.lowercase()?.contains(normalizedQuery) == true ||
                state.matchesSearchDate(normalizedQuery, context)
        }
    }
    return applyHomeSort(
        list = searched,
        sortType = sortType,
        customEventOrderIds = customEventOrderIds,
        pinnedEventIds = pinnedEventIds
    )
}

private fun EventUiState.matchesSearchDate(query: String, context: Context?): Boolean =
    setOf(eventDateToLocalDate(event.date), nextOccurrenceDate).any { date ->
        val iso = date.toString()
        sequenceOf(
            iso,
            iso.replace('-', '.'),
            iso.replace('-', '/'),
            "${date.monthValue}/${date.dayOfMonth}",
            "${date.year}年${date.monthValue}月${date.dayOfMonth}日",
            formatLunarMonthDay(date).orEmpty(),
            formatLunarDateString(date, context)
        ).any { it.lowercase().contains(query) }
    }
