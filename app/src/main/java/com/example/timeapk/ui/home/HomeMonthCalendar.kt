package com.example.timeapk.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.timeapk.R
import com.example.timeapk.ui.common.LocalCustomCategories
import com.example.timeapk.ui.common.categoryColor
import com.example.timeapk.ui.components.SongDateWheelPickerDialog
import com.example.timeapk.ui.theme.InkRuleLine
import com.example.timeapk.ui.theme.SongCalendarCell
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind
import com.example.timeapk.ui.theme.SongLunarCellKind
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.formatLunarDateString
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
internal fun MonthCalendarView(
    events: List<EventUiState>,
    selectedDate: LocalDate,
    onEventClick: (Int) -> Unit,
    onEventLongClick: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
    pickedDate: LocalDate? = null,
    onPickedDateChange: ((LocalDate) -> Unit)? = null
) {
    // selectedDate is today. The picked day can be hoisted to prefill a new event.
    var internalPickedDate by remember(selectedDate) { mutableStateOf(selectedDate) }
    val currentPickedDate = pickedDate ?: internalPickedDate
    val updatePickedDate: (LocalDate) -> Unit = { date ->
        if (onPickedDateChange != null) onPickedDateChange(date) else internalPickedDate = date
    }
    var currentMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(currentPickedDate)) }
    var showMonthPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val showLunarInCells = locale.language != "en"
    val monthPattern = stringResource(R.string.calendar_month_title_pattern)
    val selectedDatePattern = stringResource(R.string.calendar_selected_date_pattern)
    val monthFormatter = remember(locale, monthPattern) { DateTimeFormatter.ofPattern(monthPattern, locale) }
    val selectedDateFormatter = remember(locale, selectedDatePattern) {
        DateTimeFormatter.ofPattern(selectedDatePattern, locale)
    }
    val currentMonthTitle = currentMonth.atDay(1).format(monthFormatter)
    val lunarMonthTitle = if (showLunarInCells) remember(currentMonth) { calendarLunarMonthTitle(currentMonth) } else null
    val eventsByDate = remember(events, currentMonth) {
        calendarOccurrencesForMonth(events, currentMonth).groupBy { it.date }
    }
    val customCategories = LocalCustomCategories.current
    val colors = MaterialTheme.colorScheme

    fun showMonth(month: YearMonth) {
        currentMonth = month
        if (YearMonth.from(currentPickedDate) != month) {
            updatePickedDate(if (YearMonth.from(selectedDate) == month) selectedDate else month.atDay(1))
        }
    }

    val latestMonth by rememberUpdatedState(currentMonth)
    val latestShowMonth by rememberUpdatedState<(YearMonth) -> Unit>(::showMonth)
    if (showMonthPicker) {
        val monthPickerInitialDate = remember(currentMonth, currentPickedDate) {
            val day = currentPickedDate.dayOfMonth.coerceAtMost(currentMonth.lengthOfMonth())
            currentMonth.atDay(day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        SongDateWheelPickerDialog(
            initialDateMillis = monthPickerInitialDate,
            initialIsLunar = false,
            title = stringResource(R.string.field_date),
            onDismissRequest = { showMonthPicker = false },
            onConfirm = { millis, _ ->
                val picked = eventDateToLocalDate(millis)
                currentMonth = YearMonth.from(picked)
                updatePickedDate(picked)
                showMonthPicker = false
            }
        )
    }

    val selectedEvents = eventsByDate[currentPickedDate].orEmpty()
    val swipeThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = SongDesignTokens.FabClearance.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showMonth(currentMonth.minusMonths(1)) }) {
                    SongLineIcon(
                        kind = SongLineIconKind.ChevronLeft,
                        contentDescription = stringResource(R.string.calendar_prev_month),
                        tint = colors.onBackground
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clickable { showMonthPicker = true }
                        .semantics {
                            role = Role.Button
                            contentDescription = currentMonthTitle
                        }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (lunarMonthTitle != null) {
                        Text(lunarMonthTitle, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                    Text(
                        currentMonthTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
                IconButton(onClick = { showMonth(currentMonth.plusMonths(1)) }) {
                    SongLineIcon(
                        kind = SongLineIconKind.ChevronRight,
                        contentDescription = stringResource(R.string.calendar_next_month),
                        tint = colors.onBackground
                    )
                }
            }
            if (currentPickedDate != selectedDate) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = {
                        showMonth(YearMonth.from(selectedDate))
                        updatePickedDate(selectedDate)
                    }) {
                        Text(stringResource(R.string.calendar_back_to_today))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf(
                    R.string.weekday_mon, R.string.weekday_tue, R.string.weekday_wed,
                    R.string.weekday_thu, R.string.weekday_fri, R.string.weekday_sat, R.string.weekday_sun
                ).forEach { labelRes ->
                    Text(
                        text = stringResource(labelRes).removePrefix("周"),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            InkRuleLine()

            AnimatedContent(
                targetState = currentMonth,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { width -> if (forward) width / 4 else -width / 4 } + fadeIn())
                        .togetherWith(
                            slideOutHorizontally { width -> if (forward) -width / 4 else width / 4 } + fadeOut()
                        )
                },
                label = "calendarMonth",
                modifier = Modifier.pointerInput(swipeThresholdPx) {
                    var accumulated = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { accumulated = 0f },
                        onDragEnd = {
                            if (abs(accumulated) > swipeThresholdPx) {
                                latestShowMonth(latestMonth.plusMonths(if (accumulated < 0) 1 else -1))
                            }
                            accumulated = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            accumulated += dragAmount
                            change.consume()
                        }
                    )
                }
            ) { month ->
                val weeks = remember(month) {
                    buildList {
                        repeat(month.atDay(1).dayOfWeek.value - 1) { add(null) }
                        for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
                        while (size % 7 != 0) add(null)
                    }.chunked(7)
                }
                Column {
                    weeks.forEachIndexed { index, week ->
                        if (index > 0) InkRuleLine()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            week.forEach { date ->
                                if (date == null) {
                                    Spacer(Modifier.weight(1f).height(SongDesignTokens.CalendarCellMinHeight.dp))
                                } else {
                                    val dayEvents = eventsByDate[date].orEmpty()
                                    val isSelected = date == currentPickedDate
                                    val isToday = date == selectedDate
                                    val cellContent = calendarDayCellContent(date, dayEvents)
                                    val eventCount = pluralStringResource(
                                        R.plurals.calendar_day_event_count, dayEvents.size, dayEvents.size
                                    )
                                    val dayState = buildList {
                                        if (isToday) add(stringResource(R.string.calendar_day_today))
                                        if (isSelected) add(stringResource(R.string.calendar_day_selected))
                                        add(eventCount)
                                    }.joinToString(stringResource(R.string.calendar_day_state_separator))
                                    val dayAccessibilityLabel = stringResource(
                                        R.string.calendar_day_accessibility,
                                        date.format(selectedDateFormatter),
                                        dayState
                                    )
                                    val lunar = if (showLunarInCells) remember(date) { calendarLunarCellLabel(date) } else null
                                    val bars = calendarCellBarCategories(dayEvents).map { category ->
                                        HomeEventColorPolicy.categoryMarker(
                                            categoryColor(category, customCategories), colors.onBackground, colors.background
                                        )
                                    }
                                    SongCalendarCell(
                                        dayText = cellContent.dayText,
                                        eventIndicatorText = cellContent.eventIndicatorText,
                                        selected = isSelected,
                                        today = isToday,
                                        hasEvents = dayEvents.isNotEmpty(),
                                        lunarText = lunar?.text,
                                        lunarKind = lunar?.kind ?: SongLunarCellKind.Day,
                                        eventBarColors = bars,
                                        modifier = Modifier.weight(1f).semantics {
                                            contentDescription = dayAccessibilityLabel
                                        },
                                        onClick = { updatePickedDate(date) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.calendar_selected_date_events, currentPickedDate.format(selectedDateFormatter)
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface
                )
                Text(
                    text = stringResource(
                        R.string.calendar_selected_date_lunar, formatLunarDateString(currentPickedDate, context)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (selectedEvents.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.calendar_no_events),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        } else {
            items(selectedEvents, key = { "${it.eventState.event.id}-${it.date}" }) { occurrence ->
                val eventId = occurrence.eventState.event.id
                CalendarOccurrenceRow(
                    occurrence = occurrence,
                    today = selectedDate,
                    onClick = { onEventClick(eventId) },
                    onLongClick = onEventLongClick?.let { action -> { action(eventId) } }
                )
            }
        }
    }
}
