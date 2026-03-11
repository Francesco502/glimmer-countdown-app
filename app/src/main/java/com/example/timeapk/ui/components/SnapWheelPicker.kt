package com.example.timeapk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

private const val DEFAULT_VISIBLE_COUNT = 5

@Composable
fun <T> SnapWheelPicker(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = DEFAULT_VISIBLE_COUNT,
    itemHeight: Dp = 36.dp,
    itemLabel: (T) -> String
) {
    val safeVisibleCount = visibleCount.coerceAtLeast(3).let { if (it % 2 == 0) it + 1 else it }
    val paddingCount = safeVisibleCount / 2
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(items, selectedItem) {
        if (items.isEmpty()) return@LaunchedEffect
        val targetIndex = items.indexOf(selectedItem).coerceAtLeast(0)
        val currentCenterIndex = state.firstVisibleItemIndex + paddingCount
        if (currentCenterIndex != targetIndex) {
            isProgrammaticScroll = true
            state.animateScrollToItem(targetIndex)
            isProgrammaticScroll = false
        }
    }

    WheelSnapEffect(
        listState = state,
        enabled = !isProgrammaticScroll
    )
    WheelSelectionEffect(
        listState = state,
        items = items,
        paddingCount = paddingCount,
        enabled = !isProgrammaticScroll,
        onItemSelected = onItemSelected
    )

    Box(
        modifier = modifier.height(itemHeight * safeVisibleCount),
        contentAlignment = Alignment.Center
    ) {
        val totalCount = items.size + paddingCount * 2
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state
        ) {
            items(totalCount) { index ->
                val itemIndex = index - paddingCount
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    if (itemIndex in items.indices) {
                        val alpha by remember {
                            derivedStateOf {
                                val centerIndex = state.firstVisibleItemIndex + paddingCount
                                val distance = kotlin.math.abs(centerIndex - index)
                                when (distance) {
                                    0 -> 1f
                                    1 -> 0.6f
                                    else -> 0.3f
                                }
                            }
                        }
                        Text(
                            text = itemLabel(items[itemIndex]),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                        )
                    }
                }
            }
        }

        val primaryColor = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(primaryColor.copy(alpha = 0.05f))
                .drawBehind {
                    val stroke = 1.dp.toPx()
                    drawLine(
                        color = primaryColor.copy(alpha = 0.3f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = stroke
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.3f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = stroke
                    )
                }
        )
    }
}

@Composable
private fun WheelSnapEffect(
    listState: LazyListState,
    enabled: Boolean
) {
    LaunchedEffect(listState.isScrollInProgress, enabled) {
        if (!enabled || listState.isScrollInProgress) return@LaunchedEffect
        val itemSize = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
        if (itemSize == 0) return@LaunchedEffect

        val firstIndex = listState.firstVisibleItemIndex
        val firstOffset = listState.firstVisibleItemScrollOffset
        val targetIndex = if (firstOffset > itemSize / 2) firstIndex + 1 else firstIndex
        listState.animateScrollToItem(targetIndex)
    }
}

@Composable
private fun <T> WheelSelectionEffect(
    listState: LazyListState,
    items: List<T>,
    paddingCount: Int,
    enabled: Boolean,
    onItemSelected: (T) -> Unit
) {
    LaunchedEffect(listState, items, enabled) {
        if (!enabled) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex + paddingCount }
            .collectLatest { index ->
                if (items.isEmpty()) return@collectLatest
                val itemIndex = (index - paddingCount).coerceIn(0, items.lastIndex)
                onItemSelected(items[itemIndex])
            }
    }
}
