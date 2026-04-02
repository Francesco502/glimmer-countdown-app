package com.example.timeapk.ui.home

internal fun shouldKeepCurrentCustomOrder(
    currentIds: List<Int>,
    targetIds: List<Int>,
    sortType: SortType
): Boolean {
    return sortType == SortType.Custom &&
        currentIds.size == targetIds.size &&
        currentIds != targetIds &&
        currentIds.toSet() == targetIds.toSet()
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
