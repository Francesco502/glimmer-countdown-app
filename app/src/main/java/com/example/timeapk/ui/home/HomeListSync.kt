package com.example.timeapk.ui.home

internal data class PendingLocalReorderSnapshot(
    val upstreamIds: List<Int>,
    val reorderedIds: List<Int>
)

internal fun pendingLocalReorderSnapshotOrNull(
    upstreamIds: List<Int>?,
    reorderedIds: List<Int>
): PendingLocalReorderSnapshot? {
    if (upstreamIds.isNullOrEmpty() || reorderedIds.isEmpty()) return null
    if (upstreamIds == reorderedIds) return null
    if (upstreamIds.size != reorderedIds.size) return null
    if (upstreamIds.distinct().size != upstreamIds.size) return null
    if (reorderedIds.distinct().size != reorderedIds.size) return null
    if (upstreamIds.toSet() != reorderedIds.toSet()) return null

    return PendingLocalReorderSnapshot(
        upstreamIds = upstreamIds,
        reorderedIds = reorderedIds
    )
}

internal fun canStartHomeReorder(
    sortType: SortType,
    pending: PendingLocalReorderSnapshot?
): Boolean = homeCardDragSortEnabled(sortType) && pending == null

internal data class HomeListTargetSyncDecision(
    val retainCurrentOrder: Boolean,
    val pendingAfterSync: PendingLocalReorderSnapshot?
)

internal fun decideHomeListTargetSync(
    currentIds: List<Int>,
    targetIds: List<Int>,
    sortType: SortType,
    pending: PendingLocalReorderSnapshot?
): HomeListTargetSyncDecision = HomeListTargetSyncDecision(
    retainCurrentOrder = shouldRetainPendingLocalReorder(
        currentIds = currentIds,
        targetIds = targetIds,
        sortType = sortType,
        pending = pending
    ),
    pendingAfterSync = pending
)

internal fun settlePersistedHomeReorder(
    displayedItems: List<EventUiState>,
    persistedMergedIds: List<Int>?,
    pinnedEventIds: List<Int>,
    sortType: SortType
): List<EventUiState> {
    if (persistedMergedIds == null || sortType != SortType.Custom) return displayedItems

    return applyHomeSort(
        list = displayedItems,
        sortType = SortType.Custom,
        customEventOrderIds = persistedMergedIds,
        pinnedEventIds = pinnedEventIds
    )
}

internal fun shouldRetainPendingLocalReorder(
    currentIds: List<Int>,
    targetIds: List<Int>,
    sortType: SortType,
    pending: PendingLocalReorderSnapshot?
): Boolean {
    if (pending == null || sortType != SortType.Custom) return false
    if (pending.upstreamIds == pending.reorderedIds) return false
    if (pending.upstreamIds.size != pending.reorderedIds.size) return false
    if (pending.upstreamIds.toSet() != pending.reorderedIds.toSet()) return false

    return currentIds == pending.reorderedIds &&
        targetIds == pending.upstreamIds
}

internal fun <T, K> MutableList<T>.refreshItemsByKey(
    target: List<T>,
    keyOf: (T) -> K
) {
    if (isEmpty() || target.isEmpty()) return
    val targetByKey = target.associateBy(keyOf)
    forEachIndexed { index, currentItem ->
        val updatedItem = targetByKey[keyOf(currentItem)] ?: return@forEachIndexed
        if (updatedItem != currentItem) {
            this[index] = updatedItem
        }
    }
}

internal fun <T, K> MutableList<T>.replaceWithOrderedItems(
    target: List<T>,
    keyOf: (T) -> K
) {
    var index = 0
    while (index < target.size) {
        val targetItem = target[index]
        if (index >= size) {
            add(targetItem)
            index += 1
            continue
        }

        val targetKey = keyOf(targetItem)
        if (keyOf(this[index]) == targetKey) {
            if (this[index] != targetItem) {
                this[index] = targetItem
            }
            index += 1
            continue
        }

        val existingIndex = indexOfFirstFrom(index + 1) { keyOf(it) == targetKey }
        if (existingIndex >= 0) {
            removeAt(existingIndex)
        }
        add(index, targetItem)
        index += 1
    }

    while (size > target.size) {
        removeAt(lastIndex)
    }
}

private inline fun <T> List<T>.indexOfFirstFrom(
    startIndex: Int,
    predicate: (T) -> Boolean
): Int {
    for (index in startIndex until size) {
        if (predicate(this[index])) return index
    }
    return -1
}
