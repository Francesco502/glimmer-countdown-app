package com.example.timeapk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.timeapk.R
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.nlf.calendar.Lunar
import com.nlf.calendar.LunarMonth
import com.nlf.calendar.LunarYear
import com.nlf.calendar.Solar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

private const val WHEEL_VISIBLE_COUNT = 5

/**
 * 底部弹窗日期选择器（Bottom Sheet Date Picker）
 *
 * - 顶部操作区：取消 / 标题 / 确定
 * - 中间：年/月/日三个数字输入框（键盘输入）
 * - 下方：年 / 月 / 日三级滚轮，支持双向联动
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDatePicker(
    initialDateMillis: Long,
    initialIsLunar: Boolean = false,
    onDismissRequest: () -> Unit,
    onConfirm: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    title: String,
    yearRange: IntRange = 1900..2100
) {
    val initialDate = remember(initialDateMillis) {
        // 与全局 eventDateToLocalDate 一致：按 UTC 午夜解释，避免时区偏移
        eventDateToLocalDate(initialDateMillis)
    }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var isLunarMode by remember { mutableStateOf(initialIsLunar) }

    // 输入框状态
    var yearInput by remember { mutableStateOf(selectedDate.year.toString()) }
    var monthInput by remember { mutableStateOf(selectedDate.monthValue.toString().padStart(2, '0')) }
    var dayInput by remember { mutableStateOf(selectedDate.dayOfMonth.toString().padStart(2, '0')) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // 滚轮数据（公历）
    val years = remember(yearRange.first, yearRange.last) { yearRange.toList() }
    val daysInMonth by remember(selectedDate.year, selectedDate.monthValue) {
        mutableStateOf(YearMonth.of(selectedDate.year, selectedDate.monthValue).lengthOfMonth())
    }
    val solarMonths = remember { (1..12).toList() }
    val solarDays = remember(daysInMonth) { (1..daysInMonth).toList() }

    // 当前选中日期对应的农历
    val currentLunar: Lunar? = remember(selectedDate) {
        try {
            val solar = Solar.fromYmd(
                selectedDate.year,
                selectedDate.monthValue,
                selectedDate.dayOfMonth
            )
            solar.lunar
        } catch (_: Throwable) {
            null
        }
    }

    // 滚轮数据（农历）
    val lunarMonths: List<Int> = remember(currentLunar?.year) {
        val lunar = currentLunar
        if (lunar == null) {
            emptyList()
        } else {
            try {
                val yearObj = LunarYear.fromYear(lunar.year)
                @Suppress("UNCHECKED_CAST")
                (yearObj.monthsInYear as List<LunarMonth>).map { it.month }
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    val lunarDayCount: Int = remember(currentLunar?.year, currentLunar?.month) {
        val lunar = currentLunar
        if (lunar == null) {
            0
        } else {
            try {
                LunarMonth.fromYm(lunar.year, lunar.month).dayCount
            } catch (_: Throwable) {
                0
            }
        }
    }
    val lunarDays: List<Int> = remember(lunarDayCount, currentLunar?.year, currentLunar?.month) {
        if (lunarDayCount <= 0) emptyList() else (1..lunarDayCount).toList()
    }

    val monthItems: List<Int> =
        if (!isLunarMode || currentLunar == null || lunarMonths.isEmpty()) solarMonths else lunarMonths
    val dayItems: List<Int> =
        if (!isLunarMode || currentLunar == null || lunarDays.isEmpty()) solarDays else lunarDays

    fun monthLabel(value: Int): String {
        val lunar = currentLunar
        return if (!isLunarMode || lunar == null || lunarMonths.isEmpty()) {
            value.toString().padStart(2, '0')
        } else {
            try {
                val l = Lunar.fromYmd(lunar.year, value, 1)
                l.monthInChinese + "月"
            } catch (_: Throwable) {
                value.toString()
            }
        }
    }

    fun dayLabel(value: Int): String {
        val lunar = currentLunar
        return if (!isLunarMode || lunar == null || lunarDays.isEmpty()) {
            value.toString().padStart(2, '0')
        } else {
            try {
                val l = Lunar.fromYmd(lunar.year, lunar.month, value)
                l.dayInChinese
            } catch (_: Throwable) {
                value.toString()
            }
        }
    }

    val paddingCount = WHEEL_VISIBLE_COUNT / 2

    // 滚轮状态（考虑顶部 padding）
    val yearState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedDate.year - yearRange.first).coerceAtLeast(0)
    )
    val monthState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedDate.monthValue - 1
    )
    val dayState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedDate.dayOfMonth - 1
    )

    // 当滚轮变化时更新选中日期（滚轮 → 输入框）
    WheelSyncEffect(
        listState = yearState,
        items = years,
        paddingCount = paddingCount,
        enabled = !isProgrammaticScroll,
        onItemSelected = { year ->
            if (!isLunarMode || currentLunar == null) {
                val ym = YearMonth.of(year, selectedDate.monthValue)
                val day = selectedDate.dayOfMonth.coerceAtMost(ym.lengthOfMonth())
                selectedDate = LocalDate.of(year, selectedDate.monthValue, day)
            } else {
                val lunarYear = year
                val lunarMonth = currentLunar.month
                val monthObj = try {
                    LunarMonth.fromYm(lunarYear, lunarMonth)
                } catch (_: Throwable) {
                    null
                }
                val safeDay = if (monthObj != null) {
                    currentLunar.day.coerceAtMost(monthObj.dayCount)
                } else {
                    currentLunar.day
                }
                try {
                    val lunar = Lunar.fromYmd(lunarYear, lunarMonth, safeDay)
                    val solar = lunar.solar
                    selectedDate = LocalDate.of(solar.year, solar.month, solar.day)
                } catch (_: Throwable) {
                    // ignore
                }
            }
            yearInput = selectedDate.year.toString()
            monthInput = selectedDate.monthValue.toString().padStart(2, '0')
            dayInput = selectedDate.dayOfMonth.toString().padStart(2, '0')
        }
    )
    WheelSyncEffect(
        listState = monthState,
        items = monthItems,
        paddingCount = paddingCount,
        enabled = !isProgrammaticScroll,
        onItemSelected = { monthValue ->
            if (!isLunarMode || currentLunar == null || lunarMonths.isEmpty()) {
                val ym = YearMonth.of(selectedDate.year, monthValue)
                val day = selectedDate.dayOfMonth.coerceAtMost(ym.lengthOfMonth())
                selectedDate = LocalDate.of(selectedDate.year, monthValue, day)
            } else {
                val lunarYear = currentLunar.year
                val lunarMonth = monthValue
                val monthObj = try {
                    LunarMonth.fromYm(lunarYear, lunarMonth)
                } catch (_: Throwable) {
                    null
                }
                val safeDay = if (monthObj != null) {
                    currentLunar.day.coerceAtMost(monthObj.dayCount)
                } else {
                    currentLunar.day
                }
                try {
                    val lunar = Lunar.fromYmd(lunarYear, lunarMonth, safeDay)
                    val solar = lunar.solar
                    selectedDate = LocalDate.of(solar.year, solar.month, solar.day)
                } catch (_: Throwable) {
                    // ignore
                }
            }
            yearInput = selectedDate.year.toString()
            monthInput = selectedDate.monthValue.toString().padStart(2, '0')
            dayInput = selectedDate.dayOfMonth.toString().padStart(2, '0')
        }
    )
    WheelSyncEffect(
        listState = dayState,
        items = dayItems,
        paddingCount = paddingCount,
        enabled = !isProgrammaticScroll,
        onItemSelected = { dayValue ->
            if (!isLunarMode || currentLunar == null || lunarDays.isEmpty()) {
                selectedDate = LocalDate.of(selectedDate.year, selectedDate.monthValue, dayValue)
            } else {
                val lunarYear = currentLunar.year
                val lunarMonth = currentLunar.month
                val monthObj = try {
                    LunarMonth.fromYm(lunarYear, lunarMonth)
                } catch (_: Throwable) {
                    null
                }
                val safeDay = if (monthObj != null) {
                    dayValue.coerceIn(1, monthObj.dayCount)
                } else {
                    dayValue
                }
                try {
                    val lunar = Lunar.fromYmd(lunarYear, lunarMonth, safeDay)
                    val solar = lunar.solar
                    selectedDate = LocalDate.of(solar.year, solar.month, solar.day)
                } catch (_: Throwable) {
                    // ignore
                }
            }
            yearInput = selectedDate.year.toString()
            monthInput = selectedDate.monthValue.toString().padStart(2, '0')
            dayInput = selectedDate.dayOfMonth.toString().padStart(2, '0')
        }
    )

    // 输入框 → 滚轮 & 日期
    fun applyInputAndSyncWheels() {
        if (isLunarMode) {
            // 农历模式下暂不支持通过输入框直接修改联动，直接还原为当前选中日期
            yearInput = selectedDate.year.toString()
            monthInput = selectedDate.monthValue.toString().padStart(2, '0')
            dayInput = selectedDate.dayOfMonth.toString().padStart(2, '0')
            return
        }

        val year = yearInput.toIntOrNull()
        val month = monthInput.toIntOrNull()
        val day = dayInput.toIntOrNull()

        if (year == null || month == null || day == null) {
            // 非法输入：还原为当前选中日期
            yearInput = selectedDate.year.toString()
            monthInput = selectedDate.monthValue.toString().padStart(2, '0')
            dayInput = selectedDate.dayOfMonth.toString().padStart(2, '0')
            return
        }

        if (year !in yearRange || month !in 1..12) {
            yearInput = selectedDate.year.toString()
            monthInput = selectedDate.monthValue.toString().padStart(2, '0')
            dayInput = selectedDate.dayOfMonth.toString().padStart(2, '0')
            return
        }

        val ym = YearMonth.of(year, month)
        if (day !in 1..ym.lengthOfMonth()) {
            yearInput = selectedDate.year.toString()
            monthInput = selectedDate.monthValue.toString().padStart(2, '0')
            dayInput = selectedDate.dayOfMonth.toString().padStart(2, '0')
            return
        }

        // 合法输入：更新日期并滚动滚轮
        selectedDate = LocalDate.of(year, month, day)
        scope.launch {
            isProgrammaticScroll = true
            yearState.animateScrollToItem((year - yearRange.first).coerceAtLeast(0))
            monthState.animateScrollToItem(month - 1)
            dayState.animateScrollToItem(day - 1)
            isProgrammaticScroll = false
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 顶部操作区
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    onDismissRequest()
                }) {
                    Text(text = stringResource(R.string.date_picker_cancel))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = {
                    focusManager.clearFocus()
                    // 使用当前选中日期
                    val millis = selectedDate
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                    onConfirm(millis, isLunarMode)
                    onDismissRequest()
                }) {
                    Text(text = stringResource(R.string.date_picker_ok))
                }
            }

            // 公历 / 农历 模式切换（当前仅标记类型，日期依然使用公历滚轮选择）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                AssistChip(
                    onClick = { isLunarMode = false },
                    label = { Text(stringResource(R.string.solar_calendar), style = MaterialTheme.typography.labelLarge) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (!isLunarMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        labelColor = if (!isLunarMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (!isLunarMode) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    shape = MaterialTheme.shapes.small
                )
                Spacer(modifier = Modifier.width(16.dp))
                AssistChip(
                    onClick = { isLunarMode = true },
                    label = { Text(stringResource(R.string.lunar_calendar), style = MaterialTheme.typography.labelLarge) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isLunarMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        labelColor = if (isLunarMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isLunarMode) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    shape = MaterialTheme.shapes.small
                )
            }

            // 手动输入区（年 / 月 / 日）
            if (!isLunarMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DatePartField(
                        label = stringResource(R.string.date_part_year),
                        value = yearInput,
                        onValueChange = { new ->
                            yearInput = new.filter { it.isDigit() }.take(4)
                        },
                        onDone = {
                            focusManager.clearFocus()
                            applyInputAndSyncWheels()
                        },
                        modifier = Modifier.weight(1.4f)
                    )
                    DatePartField(
                        label = stringResource(R.string.date_part_month),
                        value = monthInput,
                        onValueChange = { new ->
                            monthInput = new.filter { it.isDigit() }.take(2)
                        },
                        onDone = {
                            focusManager.clearFocus()
                            applyInputAndSyncWheels()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    DatePartField(
                        label = stringResource(R.string.date_part_day),
                        value = dayInput,
                        onValueChange = { new ->
                            dayInput = new.filter { it.isDigit() }.take(2)
                        },
                        onDone = {
                            focusManager.clearFocus()
                            applyInputAndSyncWheels()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // 农历模式下隐藏手动输入框，仅保留占位以防高度突变，或者直接移除
                // 这里选择直接移除，让滚轮区域更大或者保持紧凑
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 滚轮选择区
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WheelColumn(
                    modifier = Modifier.weight(1.4f),
                    state = yearState,
                    items = years,
                    itemLabel = { it.toString() }
                )
                WheelColumn(
                    modifier = Modifier.weight(1f),
                    state = monthState,
            items = monthItems,
            itemLabel = { value: Int -> monthLabel(value) }
                )
                WheelColumn(
                    modifier = Modifier.weight(1f),
                    state = dayState,
            items = dayItems,
            itemLabel = { value: Int -> dayLabel(value) }
                )
            }
        }
    }
}

@Composable
private fun DatePartField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var hasFocus by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                onDone()
                focusManager.clearFocus()
            }
        ),
        modifier = modifier.onFocusChanged { state ->
            val nowFocused = state.isFocused
            if (hasFocus && !nowFocused) {
                // 失去焦点时触发一次校验
                onDone()
            }
            hasFocus = nowFocused
        }
    )
}

@Composable
private fun <T> WheelColumn(
    modifier: Modifier,
    state: LazyListState,
    items: List<T>,
    itemLabel: (T) -> String
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val paddingCount = WHEEL_VISIBLE_COUNT / 2
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
                        .height(36.dp),
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

        // 中间选中区域的“宋式”边框高亮
        val primaryColor = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(primaryColor.copy(alpha = 0.04f))
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    drawLine(
                        color = primaryColor.copy(alpha = 0.3f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.3f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokeWidth
                    )
                }
        )
    }
}

@Composable
private fun <T> WheelSyncEffect(
    listState: LazyListState,
    items: List<T>,
    paddingCount: Int,
    enabled: Boolean,
    onItemSelected: (T) -> Unit
) {
    // 滚动停止时的吸附逻辑
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && enabled) {
            val firstVisibleItemIndex = listState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset
            val layoutInfo = listState.layoutInfo
            val itemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
            
            if (itemSize > 0) {
                val targetIndex = if (firstVisibleItemScrollOffset > itemSize / 2) {
                    firstVisibleItemIndex + 1
                } else {
                    firstVisibleItemIndex
                }
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    LaunchedEffect(listState, items, enabled) {
        if (!enabled) return@LaunchedEffect
        snapshotFlow {
            // 取中间那一行对应的 index
            listState.firstVisibleItemIndex + paddingCount
        }.collectLatest { index ->
            if (items.isNotEmpty()) {
                val itemIndex = index - paddingCount
                if (itemIndex in items.indices) {
                    onItemSelected(items[itemIndex])
                }
            }
        }
    }
}

