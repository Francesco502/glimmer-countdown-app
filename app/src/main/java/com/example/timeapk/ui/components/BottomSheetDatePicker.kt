package com.example.timeapk.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.timeapk.R
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongFilterChip
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.nlf.calendar.Lunar
import com.nlf.calendar.LunarMonth
import com.nlf.calendar.LunarYear
import com.nlf.calendar.Solar
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@Composable
fun SongDateWheelPickerDialog(
    initialDateMillis: Long,
    initialIsLunar: Boolean = false,
    onDismissRequest: () -> Unit,
    onConfirm: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    title: String,
    yearRange: IntRange = 1900..2100
) {
    val initialDate = remember(initialDateMillis) {
        eventDateToLocalDate(initialDateMillis)
    }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var isLunarMode by remember { mutableStateOf(initialIsLunar) }
    var yearInput by remember { mutableStateOf(selectedDate.year.toString()) }
    var monthInput by remember { mutableStateOf(selectedDate.monthValue.toString().padStart(2, '0')) }
    var dayInput by remember { mutableStateOf(selectedDate.dayOfMonth.toString().padStart(2, '0')) }
    var lunarYearInput by remember { mutableStateOf(selectedDate.year.toString()) }
    var solarInputError by remember { mutableStateOf<String?>(null) }
    var lunarInputError by remember { mutableStateOf<String?>(null) }
    var isYearPickerScrolling by remember { mutableStateOf(false) }
    var isMonthPickerScrolling by remember { mutableStateOf(false) }
    var isDayPickerScrolling by remember { mutableStateOf(false) }
    val invalidDateInputMessage = stringResource(R.string.date_picker_invalid_input)
    val focusManager = LocalFocusManager.current

    val years = remember(yearRange.first, yearRange.last) { yearRange.toList() }
    val solarMonths = remember { (1..12).toList() }
    val daysInMonth = remember(selectedDate.year, selectedDate.monthValue) {
        YearMonth.of(selectedDate.year, selectedDate.monthValue).lengthOfMonth()
    }
    val solarDays = remember(daysInMonth) { (1..daysInMonth).toList() }
    val currentLunar: Lunar? = remember(selectedDate) {
        try {
            Solar.fromYmd(
                selectedDate.year,
                selectedDate.monthValue,
                selectedDate.dayOfMonth
            ).lunar
        } catch (_: Throwable) {
            null
        }
    }
    val lunarMonths = remember(currentLunar?.year) {
        currentLunar?.let { lunar ->
            try {
                @Suppress("UNCHECKED_CAST")
                (LunarYear.fromYear(lunar.year).monthsInYear as List<LunarMonth>).map { it.month }
            } catch (_: Throwable) {
                emptyList()
            }
        } ?: emptyList()
    }
    val lunarDayCount = remember(currentLunar?.year, currentLunar?.month) {
        currentLunar?.let { lunar ->
            try {
                LunarMonth.fromYm(lunar.year, lunar.month).dayCount
            } catch (_: Throwable) {
                0
            }
        } ?: 0
    }
    val lunarDays = remember(lunarDayCount) {
        if (lunarDayCount <= 0) emptyList() else (1..lunarDayCount).toList()
    }
    val monthItems = if (isLunarMode && currentLunar != null && lunarMonths.isNotEmpty()) {
        lunarMonths
    } else {
        solarMonths
    }
    val dayItems = if (isLunarMode && currentLunar != null && lunarDays.isNotEmpty()) {
        lunarDays
    } else {
        solarDays
    }
    val lunarMonthSuffix = stringResource(R.string.lunar_month_suffix)

    fun syncInputFields(date: LocalDate = selectedDate) {
        yearInput = date.year.toString()
        monthInput = date.monthValue.toString().padStart(2, '0')
        dayInput = date.dayOfMonth.toString().padStart(2, '0')
    }

    fun commitSolarDate(year: Int, month: Int, day: Int) {
        val safeMonth = month.coerceIn(1, 12)
        val ym = YearMonth.of(year.coerceIn(yearRange), safeMonth)
        val updated = LocalDate.of(ym.year, ym.monthValue, day.coerceIn(1, ym.lengthOfMonth()))
        selectedDate = updated
        syncInputFields(updated)
        solarInputError = null
        lunarInputError = null
    }

    fun lunarToSolarDate(year: Int, month: Int, day: Int): LocalDate? {
        val monthObj = try {
            LunarMonth.fromYm(year, month)
        } catch (_: Throwable) {
            null
        }
        val safeDay = if (monthObj != null) {
            day.coerceIn(1, monthObj.dayCount)
        } else {
            day.coerceAtLeast(1)
        }
        val solar = try {
            Lunar.fromYmd(year, month, safeDay).solar
        } catch (_: Throwable) {
            return null
        }
        return runCatching {
            LocalDate.of(solar.year, solar.month, solar.day)
        }.getOrNull()
    }

    fun commitLunarDate(year: Int, month: Int, day: Int): Boolean {
        val updated = lunarToSolarDate(year, month, day) ?: return false
        selectedDate = updated
        syncInputFields(updated)
        lunarYearInput = year.toString()
        solarInputError = null
        lunarInputError = null
        return true
    }

    fun monthLabel(value: Int): String {
        val lunar = currentLunar
        return if (!isLunarMode || lunar == null || lunarMonths.isEmpty()) {
            value.toString().padStart(2, '0')
        } else {
            try {
                Lunar.fromYmd(lunar.year, value, 1).monthInChinese + lunarMonthSuffix
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
                Lunar.fromYmd(lunar.year, lunar.month, value).dayInChinese
            } catch (_: Throwable) {
                value.toString()
            }
        }
    }

    val selectedWheelYear = if (isLunarMode && currentLunar != null) {
        currentLunar.year.coerceIn(yearRange)
    } else {
        selectedDate.year.coerceIn(yearRange)
    }
    val selectedWheelMonth = if (isLunarMode && currentLunar != null) {
        currentLunar.month.takeIf { it in monthItems } ?: monthItems.first()
    } else {
        selectedDate.monthValue.takeIf { it in monthItems } ?: monthItems.first()
    }
    val selectedWheelDay = if (isLunarMode && currentLunar != null) {
        currentLunar.day.takeIf { it in dayItems } ?: dayItems.first()
    } else {
        selectedDate.dayOfMonth.takeIf { it in dayItems } ?: dayItems.first()
    }

    LaunchedEffect(isLunarMode, currentLunar?.year) {
        if (isLunarMode) {
            lunarYearInput = currentLunar?.year?.toString() ?: selectedDate.year.toString()
            lunarInputError = null
        }
    }

    fun validateAndSyncInputWheels(): Boolean {
        if (isLunarMode) {
            val targetYear = lunarYearInput.toIntOrNull()
            if (targetYear == null || targetYear !in yearRange ||
                !commitLunarDate(targetYear, selectedWheelMonth, selectedWheelDay)
            ) {
                lunarInputError = invalidDateInputMessage
                return false
            }
            return true
        }

        val parsedDate = parseSolarDateInput(yearInput, monthInput, dayInput, yearRange)
        if (parsedDate == null) {
            solarInputError = invalidDateInputMessage
            return false
        }
        selectedDate = parsedDate
        syncInputFields(parsedDate)
        solarInputError = null
        return true
    }

    SongWheelPickerDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirmClick@{
            focusManager.clearFocus()
            if (!validateAndSyncInputWheels()) {
                return@onConfirmClick
            }
            val millis = selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
            onConfirm(millis, isLunarMode)
            onDismissRequest()
        },
        modifier = modifier,
        confirmEnabled = !isYearPickerScrolling && !isMonthPickerScrolling && !isDayPickerScrolling
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongFilterChip(
                selected = !isLunarMode,
                onClick = {
                    isLunarMode = false
                    solarInputError = null
                    lunarInputError = null
                    syncInputFields()
                },
                label = stringResource(R.string.solar_calendar)
            )
            SongFilterChip(
                selected = isLunarMode,
                onClick = {
                    isLunarMode = true
                    lunarYearInput = currentLunar?.year?.toString() ?: selectedDate.year.toString()
                    solarInputError = null
                    lunarInputError = null
                },
                label = stringResource(R.string.lunar_calendar)
            )
        }

        if (!isLunarMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DatePartField(
                    label = stringResource(R.string.date_part_year),
                    value = lunarYearInput,
                    onValueChange = { new ->
                        lunarInputError = null
                        lunarYearInput = new.filter { it.isDigit() }.take(4)
                    },
                    onDone = {
                        focusManager.clearFocus()
                        validateAndSyncInputWheels()
                    },
                    isError = lunarInputError != null,
                    modifier = Modifier.weight(1.4f)
                )
                Text(
                    text = currentLunar?.let { lunar ->
                        "${monthLabel(lunar.month)} ${dayLabel(lunar.day)}"
                    }.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(2f)
                )
            }
            lunarInputError?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SnapWheelPicker(
                items = years,
                selectedItem = selectedWheelYear,
                onItemSelected = { year ->
                    if (isLunarMode && currentLunar != null) {
                        commitLunarDate(year, selectedWheelMonth, selectedWheelDay)
                    } else {
                        commitSolarDate(year, selectedDate.monthValue, selectedDate.dayOfMonth)
                    }
                },
                onScrollStateChanged = { isYearPickerScrolling = it },
                modifier = Modifier.weight(1.4f),
                itemLabel = { it.toString() }
            )
            SnapWheelPicker(
                items = monthItems,
                selectedItem = selectedWheelMonth,
                onItemSelected = { month ->
                    if (isLunarMode && currentLunar != null) {
                        commitLunarDate(selectedWheelYear, month, selectedWheelDay)
                    } else {
                        commitSolarDate(selectedDate.year, month, selectedDate.dayOfMonth)
                    }
                },
                onScrollStateChanged = { isMonthPickerScrolling = it },
                modifier = Modifier.weight(1f),
                itemLabel = { monthLabel(it) }
            )
            SnapWheelPicker(
                items = dayItems,
                selectedItem = selectedWheelDay,
                onItemSelected = { day ->
                    if (isLunarMode && currentLunar != null) {
                        commitLunarDate(selectedWheelYear, selectedWheelMonth, day)
                    } else {
                        commitSolarDate(selectedDate.year, selectedDate.monthValue, day)
                    }
                },
                onScrollStateChanged = { isDayPickerScrolling = it },
                modifier = Modifier.weight(1f),
                itemLabel = { dayLabel(it) }
            )
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
        shape = MaterialTheme.shapes.small,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaStrong),
            disabledIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaSoft)
        ),
        modifier = modifier.onFocusChanged { state ->
            val nowFocused = state.isFocused
            if (hasFocus && !nowFocused) {
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
