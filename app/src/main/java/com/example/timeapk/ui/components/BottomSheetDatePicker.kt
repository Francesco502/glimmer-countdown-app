package com.example.timeapk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
 * Bottom-sheet date picker with wheel columns and optional lunar mode.
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
        // Keep UTC midnight conversion consistent with event date storage.
        eventDateToLocalDate(initialDateMillis)
    }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var isLunarMode by remember { mutableStateOf(initialIsLunar) }

    // Numeric input field states.
    var yearInput by remember { mutableStateOf(selectedDate.year.toString()) }
    var monthInput by remember { mutableStateOf(selectedDate.monthValue.toString().padStart(2, '0')) }
    var dayInput by remember { mutableStateOf(selectedDate.dayOfMonth.toString().padStart(2, '0')) }
    var solarInputError by remember { mutableStateOf<String?>(null) }
    val invalidDateInputMessage = stringResource(R.string.date_picker_invalid_input)

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // Wheel data (solar calendar).
    val years = remember(yearRange.first, yearRange.last) { yearRange.toList() }
    val daysInMonth by remember(selectedDate.year, selectedDate.monthValue) {
        mutableStateOf(YearMonth.of(selectedDate.year, selectedDate.monthValue).lengthOfMonth())
    }
    val solarMonths = remember { (1..12).toList() }
    val solarDays = remember(daysInMonth) { (1..daysInMonth).toList() }

    // Lunar date corresponding to current solar selection.
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

    // Wheel data (lunar calendar).
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

    val lunarMonthSuffix = stringResource(R.string.lunar_month_suffix)

    fun monthLabel(value: Int): String {
        val lunar = currentLunar
        return if (!isLunarMode || lunar == null || lunarMonths.isEmpty()) {
            value.toString().padStart(2, '0')
        } else {
            try {
                val l = Lunar.fromYmd(lunar.year, value, 1)
                l.monthInChinese + lunarMonthSuffix
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

    // 婊氳疆鐘舵€侊紙鑰冭檻椤堕儴 padding锛?
    val yearState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedDate.year - yearRange.first).coerceAtLeast(0)
    )
    val monthState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedDate.monthValue - 1
    )
    val dayState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedDate.dayOfMonth - 1
    )

    // 褰撴粴杞彉鍖栨椂鏇存柊閫変腑鏃ユ湡锛堟粴杞?鈫?杈撳叆妗嗭級
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

    // 杈撳叆妗?鈫?婊氳疆 & 鏃ユ湡
    fun applyInputAndSyncWheels() {
        if (isLunarMode) {
            // 鍐滃巻妯″紡涓嬫殏涓嶆敮鎸侀€氳繃杈撳叆妗嗙洿鎺ヤ慨鏀硅仈鍔紝鐩存帴杩樺師涓哄綋鍓嶉€変腑鏃ユ湡
            yearInput = selectedDate.year.toString()
            monthInput = selectedDate.monthValue.toString().padStart(2, '0')
            dayInput = selectedDate.dayOfMonth.toString().padStart(2, '0')
            return
        }

        val year = yearInput.toIntOrNull()
        val month = monthInput.toIntOrNull()
        val day = dayInput.toIntOrNull()

        if (year == null || month == null || day == null) {
            // 闈炴硶杈撳叆锛氳繕鍘熶负褰撳墠閫変腑鏃ユ湡
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

        // 鍚堟硶杈撳叆锛氭洿鏂版棩鏈熷苟婊氬姩婊氳疆
        selectedDate = LocalDate.of(year, month, day)
        scope.launch {
            isProgrammaticScroll = true
            yearState.animateScrollToItem((year - yearRange.first).coerceAtLeast(0))
            monthState.animateScrollToItem(month - 1)
            dayState.animateScrollToItem(day - 1)
            isProgrammaticScroll = false
        }
    }

    fun syncInputFieldsToSelectedDate() {
        yearInput = selectedDate.year.toString()
        monthInput = selectedDate.monthValue.toString().padStart(2, '0')
        dayInput = selectedDate.dayOfMonth.toString().padStart(2, '0')
    }

    fun validateAndSyncInputWheels(): Boolean {
        if (isLunarMode) {
            solarInputError = null
            syncInputFieldsToSelectedDate()
            return true
        }

        val parsedDate = parseSolarDateInput(yearInput, monthInput, dayInput, yearRange)
        if (parsedDate == null) {
            solarInputError = invalidDateInputMessage
            return false
        }

        solarInputError = null
        selectedDate = parsedDate
        syncInputFieldsToSelectedDate()
        scope.launch {
            isProgrammaticScroll = true
            yearState.animateScrollToItem((parsedDate.year - yearRange.first).coerceAtLeast(0))
            monthState.animateScrollToItem(parsedDate.monthValue - 1)
            dayState.animateScrollToItem(parsedDate.dayOfMonth - 1)
            isProgrammaticScroll = false
        }
        return true
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(
            topStart = com.example.timeapk.ui.theme.SongDesignTokens.RadiusLg.dp,
            topEnd = com.example.timeapk.ui.theme.SongDesignTokens.RadiusLg.dp
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 椤堕儴鎿嶄綔鍖?
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
                    if (!isLunarMode && !validateAndSyncInputWheels()) {
                        return@TextButton
                    }
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

            // 鍏巻 / 鍐滃巻 妯″紡鍒囨崲锛堝綋鍓嶄粎鏍囪绫诲瀷锛屾棩鏈熶緷鐒朵娇鐢ㄥ叕鍘嗘粴杞€夋嫨锛?
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

            // 鎵嬪姩杈撳叆鍖猴紙骞?/ 鏈?/ 鏃ワ級
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
                            solarInputError = null
                            yearInput = new.filter { it.isDigit() }.take(4)
                        },
                        onDone = {
                            focusManager.clearFocus()
                            validateAndSyncInputWheels()
                        },
                        isError = solarInputError != null,
                        modifier = Modifier.weight(1.4f)
                    )
                    DatePartField(
                        label = stringResource(R.string.date_part_month),
                        value = monthInput,
                        onValueChange = { new ->
                            solarInputError = null
                            monthInput = new.filter { it.isDigit() }.take(2)
                        },
                        onDone = {
                            focusManager.clearFocus()
                            validateAndSyncInputWheels()
                        },
                        isError = solarInputError != null,
                        modifier = Modifier.weight(1f)
                    )
                    DatePartField(
                        label = stringResource(R.string.date_part_day),
                        value = dayInput,
                        onValueChange = { new ->
                            solarInputError = null
                            dayInput = new.filter { it.isDigit() }.take(2)
                        },
                        onDone = {
                            focusManager.clearFocus()
                            validateAndSyncInputWheels()
                        },
                        isError = solarInputError != null,
                        modifier = Modifier.weight(1f)
                    )
                }
                solarInputError?.let { errorMessage ->
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                    )
                }
            } else {
                // 鍐滃巻妯″紡涓嬮殣钘忔墜鍔ㄨ緭鍏ユ锛屼粎淇濈暀鍗犱綅浠ラ槻楂樺害绐佸彉锛屾垨鑰呯洿鎺ョЩ闄?
                // 杩欓噷閫夋嫨鐩存帴绉婚櫎锛岃婊氳疆鍖哄煙鏇村ぇ鎴栬€呬繚鎸佺揣鍑?
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 婊氳疆閫夋嫨鍖?
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
    isError: Boolean,
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
        isError = isError,
        modifier = modifier.onFocusChanged { state ->
            val nowFocused = state.isFocused
            if (hasFocus && !nowFocused) {
                // 澶卞幓鐒︾偣鏃惰Е鍙戜竴娆℃牎楠?
                onDone()
            }
            hasFocus = nowFocused
        }
    )
}

internal fun parseSolarDateInput(
    yearInput: String,
    monthInput: String,
    dayInput: String,
    yearRange: IntRange
): LocalDate? {
    val year = yearInput.toIntOrNull() ?: return null
    val month = monthInput.toIntOrNull() ?: return null
    val day = dayInput.toIntOrNull() ?: return null
    if (year !in yearRange || month !in 1..12) return null

    val yearMonth = runCatching { YearMonth.of(year, month) }.getOrNull() ?: return null
    if (day !in 1..yearMonth.lengthOfMonth()) return null

    return LocalDate.of(year, month, day)
}

@OptIn(ExperimentalFoundationApi::class)
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
            state = state,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
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

        // 涓棿閫変腑鍖哄煙鐨勨€滃畫寮忊€濊竟妗嗛珮浜?
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
    LaunchedEffect(listState, items, enabled) {
        if (!enabled) return@LaunchedEffect
        snapshotFlow {
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



