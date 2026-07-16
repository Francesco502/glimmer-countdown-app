package com.example.timeapk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.timeapk.ui.theme.AnimationSpecs
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DEFAULT_VISIBLE_COUNT = 5

internal data class WheelVisibleItemInfo(
    val index: Int,
    val offset: Int,
    val size: Int
)

internal fun centeredVisibleItemIndex(
    viewportStartOffset: Int,
    viewportEndOffset: Int,
    visibleItems: List<WheelVisibleItemInfo>
): Int? {
    if (visibleItems.isEmpty()) return null
    val center = (viewportStartOffset + viewportEndOffset) / 2
    return visibleItems.minByOrNull { info ->
        abs((info.offset + info.size / 2) - center)
    }?.index
}

internal fun itemIndexFromVisibleIndex(
    visibleIndex: Int,
    paddingCount: Int,
    itemCount: Int
): Int {
    if (itemCount <= 0) return 0
    return (visibleIndex - paddingCount).coerceIn(0, itemCount - 1)
}

internal fun shouldSyncToSelectedItem(
    currentCenteredVisibleIndex: Int?,
    targetItemIndex: Int,
    paddingCount: Int,
    itemCount: Int
): Boolean {
    if (itemCount <= 0) return false
    if (currentCenteredVisibleIndex == null) return true
    return itemIndexFromVisibleIndex(
        visibleIndex = currentCenteredVisibleIndex,
        paddingCount = paddingCount,
        itemCount = itemCount
    ) != targetItemIndex.coerceIn(0, itemCount - 1)
}

internal fun wheelTargetIndex(progress: Float, itemCount: Int): Int? {
    if (itemCount <= 0 || !progress.isFinite()) return null
    return progress.roundToInt().coerceIn(0, itemCount - 1)
}

private fun centeredVisibleItemIndex(listState: LazyListState): Int? {
    return centeredVisibleItemIndex(
        viewportStartOffset = listState.layoutInfo.viewportStartOffset,
        viewportEndOffset = listState.layoutInfo.viewportEndOffset,
        visibleItems = listState.layoutInfo.visibleItemsInfo.map { info ->
            WheelVisibleItemInfo(
                index = info.index,
                offset = info.offset,
                size = info.size
            )
        }
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun <T> SnapWheelPicker(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    onScrollStateChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    visibleCount: Int = DEFAULT_VISIBLE_COUNT,
    itemHeight: Dp = 48.dp,
    accessibilityLabel: String,
    itemLabel: (T) -> String
) {
    val safeVisibleCount = visibleCount.coerceAtLeast(3).let { if (it % 2 == 0) it + 1 else it }
    val safeItemHeight = itemHeight.coerceAtLeast(48.dp)
    val paddingCount = safeVisibleCount / 2
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)
    val selectedItemLabel = items.getOrNull(selectedIndex)?.let(itemLabel).orEmpty()
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    var isProgrammaticScroll by remember { mutableStateOf(false) }
    val itemHeightPx = with(LocalDensity.current) { safeItemHeight.toPx() }

    LaunchedEffect(items, selectedItem) {
        if (items.isEmpty()) return@LaunchedEffect
        val targetIndex = items.indexOf(selectedItem).coerceAtLeast(0)
        val currentCenteredVisibleIndex = centeredVisibleItemIndex(state)
        if (shouldSyncToSelectedItem(
                currentCenteredVisibleIndex = currentCenteredVisibleIndex,
                targetItemIndex = targetIndex,
                paddingCount = paddingCount,
                itemCount = items.size
            )
        ) {
            isProgrammaticScroll = true
            if (currentCenteredVisibleIndex != null) {
                val targetVisibleIndex = targetIndex + paddingCount
                val scrollDelta = (targetVisibleIndex - currentCenteredVisibleIndex) * itemHeightPx
                state.animateScrollBy(
                    value = scrollDelta,
                    animationSpec = AnimationSpecs.handscrollTween()
                )
            } else {
                state.animateScrollToItem(targetIndex)
            }
            isProgrammaticScroll = false
        }
    }

    WheelSelectionEffect(
        listState = state,
        items = items,
        paddingCount = paddingCount,
        enabled = !isProgrammaticScroll,
        onItemSelected = onItemSelected,
        onScrollStateChanged = onScrollStateChanged
    )

    Box(
        modifier = modifier
            .height(safeItemHeight * safeVisibleCount)
            .clearAndSetSemantics {
                contentDescription = accessibilityLabel
                if (items.isNotEmpty()) {
                    stateDescription = selectedItemLabel
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = selectedIndex.toFloat(),
                        range = 0f..items.lastIndex.toFloat(),
                        steps = (items.size - 2).coerceAtLeast(0)
                    )
                    setProgress { targetValue ->
                        val targetIndex = wheelTargetIndex(targetValue, items.size)
                            ?: return@setProgress false
                        onItemSelected(items[targetIndex])
                        true
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val totalCount = items.size + paddingCount * 2
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
        ) {
            items(totalCount) { index ->
                val itemIndex = index - paddingCount
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(safeItemHeight),
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
                .height(safeItemHeight)
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
private fun <T> WheelSelectionEffect(
    listState: LazyListState,
    items: List<T>,
    paddingCount: Int,
    enabled: Boolean,
    onItemSelected: (T) -> Unit,
    onScrollStateChanged: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var lastSelectedIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(listState, items, enabled) {
        if (!enabled) return@LaunchedEffect
        snapshotFlow {
            listState.isScrollInProgress to centeredVisibleItemIndex(listState)
        }
            .distinctUntilChanged()
            .collectLatest { (isScrolling, visibleIndex) ->
                if (items.isEmpty()) {
                    onScrollStateChanged(false)
                    return@collectLatest
                }
                if (isScrolling) {
                    onScrollStateChanged(true)
                    return@collectLatest
                }
                val safeIndex = visibleIndex ?: run {
                    onScrollStateChanged(false)
                    return@collectLatest
                }
                val itemIndex = itemIndexFromVisibleIndex(
                    visibleIndex = safeIndex,
                    paddingCount = paddingCount,
                    itemCount = items.size
                )
                if (itemIndex != lastSelectedIndex) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastSelectedIndex = itemIndex
                }
                onItemSelected(items[itemIndex])
                onScrollStateChanged(false)
            }
    }
}
