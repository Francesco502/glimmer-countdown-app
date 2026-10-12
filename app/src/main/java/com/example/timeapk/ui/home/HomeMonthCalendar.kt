package com.example.timeapk.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.timeapk.R
import com.example.timeapk.ui.common.categoryColor
import com.example.timeapk.ui.common.categoryLabel
import com.example.timeapk.ui.common.LocalCustomCategories
import com.example.timeapk.ui.components.SongDateWheelPickerDialog
import com.example.timeapk.ui.theme.SongCalendarCell
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.formatDaysSmart
import com.example.timeapk.ui.utils.formatLunarDateString
import com.example.timeapk.data.REPEAT_DAILY
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_WEEKLY
import com.example.timeapk.data.REPEAT_YEARLY
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
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
    // `selectedDate` is today; the picked day can be hoisted so the add button can prefill it.
    var internalPickedDate by remember(selectedDate) { mutableStateOf(selectedDate) }
    val currentPickedDate = pickedDate ?: internalPickedDate
    val updatePickedDate: (LocalDate) -> Unit = { date ->
        if (onPickedDateChange != null) onPickedDateChange(date) else internalPickedDate = date
    }
    var currentMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(currentPickedDate)) }
    var showMonthPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val locale = context.resources.configuration.locales[0]
    val showLunarInCells = locale.language != "en"
    val monthPattern = stringResource(R.string.calendar_month_title_pattern)
    val selectedDatePattern = stringResource(R.string.calendar_selected_date_pattern)
    val monthFormatter = remember(locale, monthPattern) {
        DateTimeFormatter.ofPattern(monthPattern, locale)
    }
    val selectedDateFormatter = remember(locale, selectedDatePattern) {
        DateTimeFormatter.ofPattern(selectedDatePattern, locale)
    }
    val currentMonthTitle = currentMonth.atDay(1).format(monthFormatter)
    val monthPickerInitialDate = remember(currentMonth, currentPickedDate) {
        val safeDay = currentPickedDate.dayOfMonth.coerceIn(1, currentMonth.lengthOfMonth())
        currentMonth.atDay(safeDay)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }
    val occurrences = remember(events, currentMonth) {
        calendarOccurrencesForMonth(events, currentMonth)
    }
    val eventsByDate = remember(occurrences) { occurrences.groupBy { it.date } }
    val customCategories = LocalCustomCategories.current

    fun showMonth(month: YearMonth) {
        currentMonth = month
        if (YearMonth.from(currentPickedDate) != month) {
            updatePickedDate(if (YearMonth.from(selectedDate) == month) selectedDate else month.atDay(1))
        }
    }

    val latestMonth by rememberUpdatedState(currentMonth)
    val latestShowMonth by rememberUpdatedState<(YearMonth) -> Unit>(::showMonth)

    if (showMonthPicker) {
        SongDateWheelPickerDialog(
            initialDateMillis = monthPickerInitialDate,
            initialIsLunar = false,
            title = stringResource(R.string.field_date),
            onDismissRequest = { showMonthPicker = false },
            onConfirm = { millis, _ ->
                val pickedLocalDate = eventDateToLocalDate(millis)
                val pickedMonth = YearMonth.from(pickedLocalDate)
                currentMonth = pickedMonth
                updatePickedDate(pickedMonth.atDay(pickedLocalDate.dayOfMonth.coerceIn(1, pickedMonth.lengthOfMonth())))
                showMonthPicker = false
            }
        )
    }

    val selectedEvents = eventsByDate[currentPickedDate].orEmpty()
    val swipeThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = SongDesignTokens.FabClearance.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showMonth(currentMonth.minusMonths(1)) }) {
                SongLineIcon(
                    kind = SongLineIconKind.ChevronLeft,
                    contentDescription = stringResource(R.string.calendar_prev_month),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.74f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentMonthTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .clickable { showMonthPicker = true }
                        .semantics {
                            role = Role.Button
                            this.contentDescription = currentMonthTitle
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                if (currentMonth != YearMonth.from(selectedDate) || currentPickedDate != selectedDate) {
                    TextButton(onClick = {
                        currentMonth = YearMonth.from(selectedDate)
                        updatePickedDate(selectedDate)
                    }) {
                        Text(stringResource(R.string.calendar_back_to_today))
                    }
                }
            }
            IconButton(onClick = { showMonth(currentMonth.plusMonths(1)) }) {
                SongLineIcon(
                    kind = SongLineIconKind.ChevronRight,
                    contentDescription = stringResource(R.string.calendar_next_month),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.74f)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                R.string.weekday_mon,
                R.string.weekday_tue,
                R.string.weekday_wed,
                R.string.weekday_thu,
                R.string.weekday_fri,
                R.string.weekday_sat,
                R.string.weekday_sun
            ).forEach { labelRes ->
                Text(
                    text = stringResource(labelRes),
                    modifier = Modifier.weight(1f).padding(start = 5.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedContent(
            targetState = currentMonth,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally { width -> if (forward) width / 4 else -width / 4 } +
                    androidx.compose.animation.fadeIn())
                    .togetherWith(
                        slideOutHorizontally { width -> if (forward) -width / 4 else width / 4 } +
                            androidx.compose.animation.fadeOut()
                    )
            },
            label = "calendarMonth",
            modifier = Modifier.pointerInput(Unit) {
                var accumulated = 0f
                detectHorizontalDragGestures(
                    onDragStart = { accumulated = 0f },
                    onDragEnd = {
                        if (abs(accumulated) > swipeThresholdPx) {
                            latestShowMonth(if (accumulated < 0) latestMonth.plusMonths(1) else latestMonth.minusMonths(1))
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
            val monthStart = month.atDay(1)
            val firstDayOffset = monthStart.dayOfWeek.value - 1 // Monday = 0
            val cells = remember(month) {
                buildList {
                    repeat(firstDayOffset.coerceAtLeast(0)) { add(null) }
                    for (day in 1..month.lengthOfMonth()) {
                        add(month.atDay(day))
                    }
                    while (size % 7 != 0) {
                        add(null)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    val dayEvents = date?.let { eventsByDate[it] }.orEmpty()
                    val isSelected = date != null && date == currentPickedDate
                    val isToday = date != null && date == selectedDate
                    val hasEvents = dayEvents.isNotEmpty()
                    if (date == null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp, max = 72.dp)
                        )
                    } else {
                        val cellContent = calendarDayCellContent(date, dayEvents)
                        val (dotCategories, overflow) = calendarCellDotCategories(dayEvents)
                        val eventCountLabel = pluralStringResource(
                            R.plurals.calendar_day_event_count,
                            dayEvents.size,
                            dayEvents.size
                        )
                        val dayStateLabel = buildList {
                            if (isToday) add(stringResource(R.string.calendar_day_today))
                            if (isSelected) add(stringResource(R.string.calendar_day_selected))
                            add(eventCountLabel)
                        }.joinToString(stringResource(R.string.calendar_day_state_separator))
                        val dayAccessibilityLabel = stringResource(
                            R.string.calendar_day_accessibility,
                            date.format(selectedDateFormatter),
                            dayStateLabel
                        )
                        val lunarText = if (showLunarInCells) remember(date) { calendarLunarCellLabel(date) } else null
                        SongCalendarCell(
                            dayText = cellContent.dayText,
                            eventIndicatorText = if (overflow > 0) "+$overflow" else null,
                            selected = isSelected,
                            today = isToday,
                            hasEvents = hasEvents,
                            lunarText = lunarText,
                            eventDotColors = dotCategories.map { categoryColor(it, customCategories) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp, max = 72.dp)
                                .semantics {
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
            modifier = Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.calendar_selected_date_events, currentPickedDate.format(selectedDateFormatter)),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(
                    R.string.calendar_selected_date_lunar,
                    formatLunarDateString(currentPickedDate, context)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

            }
        }
        if (selectedEvents.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.calendar_no_events),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(selectedEvents, key = { "${it.eventState.event.id}-${it.date}" }) { occurrence ->
                CalendarOccurrenceRow(
                    occurrence = occurrence,
                    today = selectedDate,
                    onEventClick = onEventClick,
                    onEventLongClick = onEventLongClick
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarOccurrenceRow(
    occurrence: CalendarEventOccurrence,
    today: LocalDate,
    onEventClick: (Int) -> Unit,
    onEventLongClick: ((Int) -> Unit)?
) {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val selectedDatePattern = stringResource(R.string.calendar_selected_date_pattern)
    val rowDateFormatter = remember(selectedDatePattern, locale) {
        DateTimeFormatter.ofPattern(selectedDatePattern, locale)
    }
    val categoryText = categoryLabel(occurrence.eventState.event.category)
    val accent = categoryColor(occurrence.eventState.event.category)
    val repeatLabel = when (occurrence.eventState.event.repeatType) {
        REPEAT_DAILY -> stringResource(R.string.repeat_daily)
        REPEAT_WEEKLY -> stringResource(R.string.repeat_weekly)
        REPEAT_MONTHLY -> stringResource(R.string.repeat_monthly)
        REPEAT_HALF_YEARLY -> stringResource(R.string.repeat_half_yearly)
        REPEAT_YEARLY -> stringResource(R.string.repeat_yearly)
        else -> null
    }
    val daysFromToday = ChronoUnit.DAYS.between(today, occurrence.date)
    val relativeDateLabel = when {
        daysFromToday == 0L -> stringResource(R.string.home_today_seal)
        daysFromToday > 0L -> buildString {
            append(stringResource(R.string.days_left_label))
            append(" ")
            append(stringResource(R.string.quantity_with_unit, formatDaysSmart(daysFromToday, false, locale),
                pluralStringResource(R.plurals.days_unit, daysFromToday.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt())))
        }
        else -> context.resources.getQuantityString(
            R.plurals.days_elapsed_format,
            (-daysFromToday).toInt(),
            (-daysFromToday).toInt()
        )
    }
    val calendarMetaLine = buildList {
        add(categoryText)
        repeatLabel?.let(::add)
        if (occurrence.eventState.event.remindEnabled) add(stringResource(R.string.field_remind))
    }.joinToString(" · ")
    val calendarTimeLine = buildList {
        add(occurrence.date.format(rowDateFormatter))
        add(relativeDateLabel)
    }.joinToString(" · ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onEventLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onEventClick(occurrence.eventState.event.id) },
                        onLongClick = { onEventLongClick(occurrence.eventState.event.id) }
                    )
                } else {
                    Modifier.clickable { onEventClick(occurrence.eventState.event.id) }
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .background(accent.copy(alpha = 0.62f), RoundedCornerShape(1.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = occurrence.eventState.event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = calendarTimeLine,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = calendarMetaLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        HorizontalDivider(
            thickness = SongDesignTokens.BorderWidth.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
    }
}
