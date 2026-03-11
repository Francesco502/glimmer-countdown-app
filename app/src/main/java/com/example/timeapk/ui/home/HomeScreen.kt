package com.example.timeapk.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_DAILY
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_WEEKLY
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.ui.utils.formatDays
import com.example.timeapk.ui.utils.formatBetweenAsYMD
import com.example.timeapk.ui.utils.formatDaysSmart
import com.example.timeapk.ui.utils.getDisplayDateFormatter
import com.example.timeapk.ui.utils.parseEventColorOrFallback
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.DisplayModes
import com.example.timeapk.ui.utils.getAvailableDisplayModes
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.Event
import com.example.timeapk.ui.theme.AnimationSpecs

import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.OverscrollConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import org.burnoutcrew.reorderable.ReorderableItem

// Event category type for unified display
sealed class EventType {
    object Birthday : EventType()
    object Anniversary : EventType()
    object Regular : EventType()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navigateToItemEntry: () -> Unit,
    navigateToDetail: (Int) -> Unit,
    navigateToEdit: (Int) -> Unit,
    navigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = com.example.timeapk.ui.AppViewModelProvider.Factory)
) {
    val homeUiState by viewModel.homeUiState.collectAsState()
    val context = LocalContext.current
    val prefs = (context.applicationContext as TimeApplication).userPrefs
    val today = LocalDate.now()
    val scope = rememberCoroutineScope()
    val filterType by viewModel.filterType.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showHours by prefs.showHoursFlow.collectAsState(initial = true)
    val showMilestone by prefs.showMilestoneFlow.collectAsState(initial = true)
    val homeDensityMode by prefs.homeDensityModeFlow.collectAsState(initial = 1)
    val dateFormatMode by prefs.dateFormatModeFlow.collectAsState(initial = 0)
    val dateFormatter = remember(dateFormatMode) { getDisplayDateFormatter(dateFormatMode) }
    val dateDeltaDisplayMode by prefs.dateDeltaDisplayModeFlow.collectAsState(initial = 0)
    val perEventDateDeltaModes by prefs.perEventDateDeltaDisplayModesFlow.collectAsState(initial = emptyMap())
    val savedHomeDisplayMode by prefs.homeDisplayModeFlow.collectAsState(initial = 0)
    var homeDisplayMode by remember(savedHomeDisplayMode) { mutableStateOf(savedHomeDisplayMode) }
    var showSortMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(homeDisplayMode) {
        prefs.setHomeDisplayMode(homeDisplayMode)
    }
    val displayedList = homeUiState

    // Reorder list state is seeded synchronously to avoid first-frame jump/glitch.
    val orderedList = remember { mutableStateListOf<EventUiState>() }
    var listInitialized by remember { mutableStateOf(false) }

    if (orderedList.isEmpty() && displayedList.isNotEmpty()) {
        orderedList.addAll(displayedList)
    }

    LaunchedEffect(displayedList) {
        if (orderedList.isEmpty()) return@LaunchedEffect
        val targetIds = displayedList.map { it.event.id }
        val currentIds = orderedList.map { it.event.id }
        if (targetIds == currentIds) {
            displayedList.forEachIndexed { i, newItem ->
                if (i < orderedList.size && orderedList[i] != newItem) {
                    orderedList[i] = newItem
                }
            }
        } else {
            orderedList.clear()
            orderedList.addAll(displayedList)
        }
        listInitialized = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(
                        onClick = navigateToSettings,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            val isEmpty = displayedList.isEmpty()
            val fabScale by animateFloatAsState(
                if (isEmpty) 1.03f else 1f,
                animationSpec = AnimationSpecs.springButton,
                label = "fabScale"
            )
            Surface(
                onClick = navigateToItemEntry,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer { scaleX = fabScale; scaleY = fabScale },
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_add_event),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val chipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                labelColor = MaterialTheme.colorScheme.onBackground
            )
            val nextMode = (homeDisplayMode + 1) % 3
            val hasActiveFilter = filterType != FilterType.All
            var showFilterPanel by remember { mutableStateOf(hasActiveFilter) }
            var showSearchBar by remember { mutableStateOf(searchQuery.isNotBlank()) }
            LaunchedEffect(hasActiveFilter) {
                if (hasActiveFilter) showFilterPanel = true
            }
            LaunchedEffect(searchQuery) {
                if (searchQuery.isNotBlank()) showSearchBar = true
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .wrapContentWidth(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InlineActionIconButton(
                    icon = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_hint),
                    active = showSearchBar || searchQuery.isNotBlank(),
                    onClick = {
                        if (showSearchBar && searchQuery.isBlank()) {
                            showSearchBar = false
                        } else {
                            showSearchBar = !showSearchBar
                        }
                    }
                )
                InlineActionIconButton(
                    icon = Icons.Default.Tune,
                    contentDescription = stringResource(R.string.home_filter_panel_toggle),
                    active = hasActiveFilter || showFilterPanel,
                    onClick = { showFilterPanel = !showFilterPanel }
                )
                Box {
                    InlineActionIconButton(
                        icon = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.sort_menu),
                        active = sortType != SortType.ByDays || showSortMenu,
                        onClick = { showSortMenu = true }
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_days)) },
                            onClick = {
                                viewModel.updateSortType(SortType.ByDays)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_date)) },
                            onClick = {
                                viewModel.updateSortType(SortType.ByDate)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_created)) },
                            onClick = {
                                viewModel.updateSortType(SortType.ByCreated)
                                showSortMenu = false
                            }
                        )
                    }
                }
                InlineActionIconButton(
                    icon = when (nextMode) {
                        0 -> Icons.Default.ViewModule
                        1 -> Icons.AutoMirrored.Filled.ViewList
                        else -> Icons.Default.CalendarMonth
                    },
                    contentDescription = when (nextMode) {
                        0 -> stringResource(R.string.display_mode_card)
                        1 -> stringResource(R.string.display_mode_list)
                        else -> stringResource(R.string.display_mode_calendar)
                    },
                    active = homeDisplayMode == 2,
                    onClick = { homeDisplayMode = nextMode }
                )
            }

            if (hasActiveFilter) {
                val activeLabel = when {
                    filterType == FilterType.Birthday -> stringResource(R.string.category_birthday)
                    filterType == FilterType.Anniversary -> stringResource(R.string.category_anniversary)
                    filterType == FilterType.Other -> stringResource(R.string.category_other)
                    else -> stringResource(R.string.filter_all)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    AssistChip(
                        onClick = { showFilterPanel = true },
                        label = { Text(activeLabel) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            AnimatedVisibility(visible = showSearchBar) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp)
                )
            }

            AnimatedVisibility(visible = showFilterPanel) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterType == FilterType.All,
                            onClick = { viewModel.updateFilterType(FilterType.All) },
                            label = { Text(stringResource(R.string.filter_all)) },
                            shape = RoundedCornerShape(4.dp),
                            colors = chipColors
                        )
                        FilterChip(
                            selected = filterType == FilterType.Birthday,
                            onClick = { viewModel.updateFilterType(FilterType.Birthday) },
                            label = { Text(stringResource(R.string.category_birthday)) },
                            shape = RoundedCornerShape(4.dp),
                            colors = chipColors
                        )
                        FilterChip(
                            selected = filterType == FilterType.Anniversary,
                            onClick = { viewModel.updateFilterType(FilterType.Anniversary) },
                            label = { Text(stringResource(R.string.category_anniversary)) },
                            shape = RoundedCornerShape(4.dp),
                            colors = chipColors
                        )
                        FilterChip(
                            selected = filterType == FilterType.Other,
                            onClick = { viewModel.updateFilterType(FilterType.Other) },
                            label = { Text(stringResource(R.string.category_other)) },
                            shape = RoundedCornerShape(4.dp),
                            colors = chipColors
                        )
                    }
                }
            }
            // Event list / month view
            if (homeDisplayMode == 2) {
                if (displayedList.isEmpty()) {
                    EmptyState(modifier = Modifier.fillMaxSize())
                } else {
                    MonthCalendarView(
                        events = displayedList,
                        selectedDate = today,
                        onEventClick = { navigateToDetail(it) },
                        onEventLongClick = { navigateToEdit(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                key(homeDisplayMode) {
                    val dragEnabled = sortType == SortType.ByCreated
                    val reorderState = rememberReorderableLazyListState(
                        onMove = { from, to ->
                            if (dragEnabled) {
                                val fromIdx = from.index
                                val toIdx = to.index
                                if (fromIdx in orderedList.indices && toIdx in orderedList.indices && fromIdx != toIdx) {
                                    val item = orderedList.removeAt(fromIdx)
                                    orderedList.add(toIdx, item)
                                    scope.launch { prefs.setCustomEventOrder(orderedList.map { it.event.id }) }
                                }
                            }
                        }
                    )

                    AnimatedContent(
                        targetState = displayedList.isEmpty(),
                        transitionSpec = {
                            (fadeIn(animationSpec = AnimationSpecs.mediumTween()) + slideInVertically(animationSpec = AnimationSpecs.mediumTweenIntOffset()) { it / 8 })
                                .togetherWith(
                                    fadeOut(animationSpec = AnimationSpecs.mediumTween()) + slideOutVertically(animationSpec = AnimationSpecs.mediumTweenIntOffset()) { -it / 8 }
                                )
                        },
                        label = "listOrEmpty"
                    ) { isEmpty ->
                        if (isEmpty) {
                            EmptyState(modifier = Modifier.fillMaxSize())
                        } else {
                            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                                LazyColumn(
                                    state = reorderState.listState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(
                                            if (dragEnabled) {
                                                Modifier
                                                    .reorderable(reorderState)
                                                    .detectReorderAfterLongPress(reorderState)
                                            } else {
                                                Modifier
                                            }
                                        ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(if (homeDisplayMode == 0) 12.dp else 0.dp)
                                ) {
                                    items(orderedList, key = { it.event.id }) { eventState ->
                                        ReorderableItem(reorderState, key = eventState.event.id) { isDragging ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .then(
                                                        if (listInitialized) Modifier.animateItemPlacement(animationSpec = AnimationSpecs.springItemPlacement)
                                                        else Modifier
                                                    )
                                            ) {
                                                val showDetail = homeDensityMode == 1
                                                if (homeDisplayMode == 0) {
                                                    val persistedMode = perEventDateDeltaModes[eventState.event.id] ?: dateDeltaDisplayMode
                                                    var cardDisplayMode by remember(eventState.event.id, persistedMode) {
                                                        mutableStateOf(persistedMode)
                                                    }
                                                    EventCard(
                                                        eventState = eventState,
                                                        today = today,
                                                        dateFormatter = dateFormatter,
                                                        dateDeltaDisplayMode = cardDisplayMode,
                                                        onToggleDateDeltaDisplayMode = {
                                                            val availableModes = getAvailableDisplayModes(eventState, showMilestone = true)
                                                            val currentModeIndex = availableModes.indexOf(cardDisplayMode)
                                                            val nextModeIndex = (if (currentModeIndex != -1) currentModeIndex + 1 else 1) % availableModes.size
                                                            val nextMode = availableModes[nextModeIndex]
                                                            cardDisplayMode = nextMode
                                                            scope.launch {
                                                                prefs.setDateDeltaDisplayModeForEvent(eventState.event.id, nextMode)
                                                            }
                                                        },
                                                        onClick = { navigateToDetail(eventState.event.id) },
                                                        onLongClick = { navigateToEdit(eventState.event.id) },
                                                        showHours = showHours,
                                                        showMilestone = showMilestone,
                                                        showDetail = showDetail,
                                                        isDragging = isDragging
                                                    )
                                                } else {
                                                    val persistedMode = perEventDateDeltaModes[eventState.event.id] ?: dateDeltaDisplayMode
                                                    var itemDisplayMode by remember(eventState.event.id, persistedMode) {
                                                        mutableStateOf(persistedMode)
                                                    }
                                                    EventListItem(
                                                        eventState = eventState,
                                                        today = today,
                                                        dateFormatter = dateFormatter,
                                                        dateDeltaDisplayMode = itemDisplayMode,
                                                        onToggleDateDeltaDisplayMode = {
                                                            val availableModes = getAvailableDisplayModes(eventState, showMilestone = true)
                                                            val currentModeIndex = availableModes.indexOf(itemDisplayMode)
                                                            val nextModeIndex = (if (currentModeIndex != -1) currentModeIndex + 1 else 1) % availableModes.size
                                                            val nextMode = availableModes[nextModeIndex]
                                                            itemDisplayMode = nextMode
                                                            scope.launch {
                                                                prefs.setDateDeltaDisplayModeForEvent(eventState.event.id, nextMode)
                                                            }
                                                        },
                                                        onClick = { navigateToDetail(eventState.event.id) },
                                                        onLongClick = { navigateToEdit(eventState.event.id) },
                                                        isDragging = isDragging
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f)
            },
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Add,
                contentDescription = stringResource(R.string.cd_add_event),
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventCard(
    eventState: EventUiState,
    today: LocalDate,
    dateFormatter: DateTimeFormatter,
    dateDeltaDisplayMode: Int,
    onToggleDateDeltaDisplayMode: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showHours: Boolean = true,
    showMilestone: Boolean = true,
    showDetail: Boolean = true,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isPast = eventState.isPast
    val baseCardColor = parseEventColorOrFallback(
        hex = eventState.event.colorHex,
        fallback = MaterialTheme.colorScheme.primary
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = AnimationSpecs.springButton,
        label = "cardScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = AnimationSpecs.microTween(),
        label = "cardAlpha"
    )
    val dragElevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 0.dp,
        animationSpec = AnimationSpecs.smallTweenDp(),
        label = "dragElevation"
    )

    val juanbenTint = if (isPast) 0.85f else 1.0f
    val cardContainerColor = baseCardColor.copy(alpha = juanbenTint)

    // Keep high contrast for readability on both light and dark card colors.
    val isLight = cardContainerColor.luminance() > 0.45f
    val cardContentColor = if (isLight) {
        lerp(Color(0xFF141618), baseCardColor, 0.1f)
    } else {
        lerp(Color(0xFFF9F7F2), baseCardColor, 0.05f)
    }

    val view = androidx.compose.ui.platform.LocalView.current
    val locale = androidx.compose.ui.platform.LocalContext.current.resources.configuration.locales[0]

    val targetLocalDate = remember(eventState.event.date) {
        eventDateToLocalDate(eventState.event.date)
    }

    val isRepeating = eventState.event.repeatType != REPEAT_NONE
    val isToday = eventState.daysRemaining == 0L && !eventState.isPast
    val todayLabel = stringResource(R.string.days_today_label)

    val availableModes = getAvailableDisplayModes(eventState, showMilestone)
    val modeIndex = availableModes.indexOf(dateDeltaDisplayMode)
    val mode = if (modeIndex != -1) dateDeltaDisplayMode else availableModes.first()

    val displayContent: String
    val displayUnit: String
    val labelText: String

    when (mode) {
        DisplayModes.PAST_DAYS -> {
            val days = if (isRepeating) eventState.daysPassed else eventState.daysElapsed
            displayContent = formatDaysSmart(days, false, locale)
            displayUnit = stringResource(R.string.days_unit)
            labelText = stringResource(R.string.days_past_label)
        }
        DisplayModes.PAST_YMD -> {
            val start = targetLocalDate
            val end = today
            displayContent = formatBetweenAsYMD(start, end, locale)
            displayUnit = ""
            labelText = stringResource(R.string.days_past_label)
        }
        DisplayModes.UNTIL_DAYS -> {
            if (isToday) {
                displayContent = todayLabel
                displayUnit = ""
                labelText = ""
            } else {
                val days = if (isRepeating) eventState.daysLeft else eventState.daysRemaining
                displayContent = formatDaysSmart(days, false, locale)
                displayUnit = stringResource(R.string.days_unit)
                labelText = com.example.timeapk.ui.utils.getUntilLabel(androidx.compose.ui.platform.LocalContext.current, eventState)
            }
        }
        DisplayModes.UNTIL_YMD -> {
            if (isToday) {
                displayContent = todayLabel
                displayUnit = ""
                labelText = ""
            } else {
                val days = if (isRepeating) eventState.daysLeft else eventState.daysRemaining
                val end = today.plusDays(days)
                displayContent = formatBetweenAsYMD(today, end, locale)
                displayUnit = ""
                labelText = com.example.timeapk.ui.utils.getUntilLabel(androidx.compose.ui.platform.LocalContext.current, eventState)
            }
        }
        DisplayModes.MILESTONE -> {
            displayContent = formatDaysSmart(eventState.nextMilestoneDays ?: 0L, false, locale)
            displayUnit = stringResource(R.string.days_unit)
            val milestoneVal = eventState.nextMilestoneValue ?: 0L
            val milestoneStr = milestoneLabel(milestoneVal)
            labelText = stringResource(R.string.milestone_label_prefix, milestoneStr)
        }
        else -> {
            displayContent = ""
            displayUnit = ""
            labelText = ""
        }
    }

    val cardDescription = buildString {
        append(eventState.event.title)
        append(", ")
        if (isToday) append(todayLabel)
        else append(labelText).append(" ").append(displayContent).append(displayUnit)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp)
            .shadow(dragElevation, RoundedCornerShape(2.dp), spotColor = Color.Black.copy(alpha = 0.25f))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = cardAlpha
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics(mergeDescendants = true) { contentDescription = cardDescription },
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        border = BorderStroke(
            width = 0.5.dp, // 鏋佺粏娣″ⅷ杈规
            color = baseCardColor.copy(alpha = if (isPast) 0.3f else 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color indicator dot - Removed to let the whole card color speak
            // Instead, we use the whole card background as the indicator
            
            // 鏍囬涓庢棩鏈熷尯鍩燂細涓嶅啀鍗曠嫭澶勭悊鐐瑰嚮锛屼氦鐢辨暣鍗＄墖鐨?clickable 缁熶竴澶勭悊
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = eventState.event.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                        ),
                        color = cardContentColor.copy(alpha = if (isPast) 0.8f else 1.0f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = targetLocalDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = cardContentColor.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            val timeColor = if (isPast) cardContentColor.copy(alpha = 0.85f) else cardContentColor
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = 72.dp)
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onClick = onToggleDateDeltaDisplayMode
                    ),
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = displayContent,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                            ),
                            color = timeColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (displayUnit.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = displayUnit,
                                style = MaterialTheme.typography.bodyMedium,
                                color = timeColor.copy(alpha = 1.0f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.bodySmall,
                        color = timeColor.copy(alpha = 0.85f),
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventListItem(
    eventState: EventUiState,
    today: LocalDate,
    dateFormatter: DateTimeFormatter,
    dateDeltaDisplayMode: Int,
    onToggleDateDeltaDisplayMode: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = AnimationSpecs.springButton,
        label = "listItemScale"
    )
    val itemAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = AnimationSpecs.microTween(),
        label = "listItemAlpha"
    )
    val listDragElevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        animationSpec = AnimationSpecs.smallTweenDp(),
        label = "listDragElevation"
    )
    val isPast = eventState.isPast
    val targetLocalDate = remember(eventState.event.date) {
        eventDateToLocalDate(eventState.event.date)
    }

    val isRepeating = eventState.event.repeatType != REPEAT_NONE
    val isToday = eventState.daysRemaining == 0L && !eventState.isPast
    val todayLabel = stringResource(R.string.days_today_label)
    val locale = androidx.compose.ui.platform.LocalContext.current.resources.configuration.locales[0]

    val availableModes = getAvailableDisplayModes(eventState, showMilestone = true)
    val modeIndex = availableModes.indexOf(dateDeltaDisplayMode)
    val mode = if (modeIndex != -1) dateDeltaDisplayMode else availableModes.first()

    val labelText: String
    val daysDisplay: String

    when (mode) {
        DisplayModes.PAST_DAYS -> {
            val days = if (isRepeating) eventState.daysPassed else eventState.daysElapsed
            daysDisplay = formatDaysSmart(days, false, locale) + stringResource(R.string.days_unit)
            labelText = stringResource(R.string.days_past_label)
        }
        DisplayModes.PAST_YMD -> {
            val start = targetLocalDate
            val end = today
            daysDisplay = formatBetweenAsYMD(start, end, locale)
            labelText = stringResource(R.string.days_past_label)
        }
        DisplayModes.UNTIL_DAYS -> {
            if (isToday) {
                daysDisplay = todayLabel
                labelText = ""
            } else {
                val days = if (isRepeating) eventState.daysLeft else eventState.daysRemaining
                daysDisplay = formatDaysSmart(days, false, locale) + stringResource(R.string.days_unit)
                labelText = com.example.timeapk.ui.utils.getUntilLabel(androidx.compose.ui.platform.LocalContext.current, eventState)
            }
        }
        DisplayModes.UNTIL_YMD -> {
            if (isToday) {
                daysDisplay = todayLabel
                labelText = ""
            } else {
                val days = if (isRepeating) eventState.daysLeft else eventState.daysRemaining
                val end = today.plusDays(days)
                daysDisplay = formatBetweenAsYMD(today, end, locale)
                labelText = com.example.timeapk.ui.utils.getUntilLabel(androidx.compose.ui.platform.LocalContext.current, eventState)
            }
        }
        DisplayModes.MILESTONE -> {
            daysDisplay = formatDaysSmart(eventState.nextMilestoneDays ?: 0L, false, locale) + stringResource(R.string.days_unit)
            val milestoneVal = eventState.nextMilestoneValue ?: 0L
            val milestoneStr = milestoneLabel(milestoneVal)
            labelText = stringResource(R.string.milestone_label_prefix, milestoneStr)
        }
        else -> {
            daysDisplay = ""
            labelText = ""
        }
    }

    val itemContentColor = MaterialTheme.colorScheme.onSurface
    val eventColor = parseEventColorOrFallback(
        hex = eventState.event.colorHex,
        fallback = MaterialTheme.colorScheme.primary
    )
    val categoryLabel = when (eventState.event.category) {
        CATEGORY_BIRTHDAY -> stringResource(R.string.category_birthday)
        CATEGORY_ANNIVERSARY -> stringResource(R.string.category_anniversary)
        else -> stringResource(R.string.category_other)
    }
    val repeatLabel = when (eventState.event.repeatType) {
        REPEAT_DAILY -> stringResource(R.string.repeat_daily)
        REPEAT_WEEKLY -> stringResource(R.string.repeat_weekly)
        REPEAT_MONTHLY -> stringResource(R.string.repeat_monthly)
        REPEAT_HALF_YEARLY -> stringResource(R.string.repeat_half_yearly)
        REPEAT_YEARLY -> stringResource(R.string.repeat_yearly)
        else -> null
    }
    val dateLine = targetLocalDate.format(dateFormatter)
    val dateLineStyle = if (dateLine.length > 12) {
        MaterialTheme.typography.labelSmall
    } else {
        MaterialTheme.typography.bodySmall
    }
    val metaLine = buildList {
        add(categoryLabel)
        if (eventState.event.isLunar) add(stringResource(R.string.lunar_calendar))
        repeatLabel?.let(::add)
    }.joinToString(" · ")
    val supportLine = buildList {
        if (eventState.event.remindEnabled) add(stringResource(R.string.field_remind))
    }.joinToString(" · ")
    val itemDescription = buildString {
        append(eventState.event.title)
        append(", ")
        if (isToday) append(todayLabel)
        else append(labelText).append(" ").append(daysDisplay)
    }
    val isLightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val displayColor = if (isLightSurface) {
        lerp(eventColor, Color.Black, 0.4f)
    } else {
        lerp(eventColor, Color.White, 0.4f)
    }
    val rowBackground = when {
        isDragging -> MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        else -> Color.Transparent
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(listDragElevation, RoundedCornerShape(2.dp), spotColor = Color.Black.copy(alpha = 0.18f))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = itemAlpha
            }
            .background(color = rowBackground, shape = RoundedCornerShape(2.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics(mergeDescendants = true) { contentDescription = itemDescription }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .background(
                        color = eventColor.copy(alpha = if (isPast) 0.45f else 0.85f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = eventState.event.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    ),
                    color = itemContentColor.copy(alpha = if (isPast) 0.84f else 1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateLine,
                    style = dateLineStyle,
                    color = itemContentColor.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                if (metaLine.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = metaLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = eventColor.copy(alpha = if (isPast) 0.68f else 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (supportLine.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = supportLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = itemContentColor.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            VerticalDivider(
                modifier = Modifier.height(34.dp),
                thickness = 0.6.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .width(96.dp)
                    .sizeIn(minHeight = 48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onClick = onToggleDateDeltaDisplayMode
                    ),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = daysDisplay,
                    style = if (daysDisplay.length > 8) {
                        MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    } else {
                        MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    },
                    color = if (isPast) displayColor.copy(alpha = 0.82f) else displayColor,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                if (labelText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.labelSmall,
                        color = itemContentColor.copy(alpha = 0.64f),
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 0.6.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
        )
    }
}
















@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthCalendarView(
    events: List<EventUiState>,
    selectedDate: LocalDate,
    onEventClick: (Int) -> Unit,
    onEventLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }
    var pickedDate by remember(selectedDate) { mutableStateOf(selectedDate) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val locale = context.resources.configuration.locales[0]
    val monthPattern = stringResource(R.string.calendar_month_title_pattern)
    val selectedDatePattern = stringResource(R.string.calendar_selected_date_pattern)
    val monthFormatter = remember(locale, monthPattern) {
        DateTimeFormatter.ofPattern(monthPattern, locale)
    }
    val selectedDateFormatter = remember(locale, selectedDatePattern) {
        DateTimeFormatter.ofPattern(selectedDatePattern, locale)
    }
    val eventsByDate = remember(events) { events.groupBy { it.nextOccurrenceDate } }

    val daysInMonth = currentMonth.lengthOfMonth()
    val monthStart = currentMonth.atDay(1)
    val firstDayOffset = monthStart.dayOfWeek.value - 1 // Monday = 0
    val cells = remember(currentMonth) {
        buildList {
            repeat(firstDayOffset.coerceAtLeast(0)) { add(null) }
            for (day in 1..daysInMonth) {
                add(currentMonth.atDay(day))
            }
            while (size % 7 != 0) {
                add(null)
            }
        }
    }

    if (YearMonth.from(pickedDate) != currentMonth) {
        pickedDate = currentMonth.atDay(1)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_prev_month))
            }
            Text(
                text = currentMonth.atDay(1).format(monthFormatter),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next_month))
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
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
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    val dayEvents = date?.let { eventsByDate[it] }.orEmpty()
                    val isSelected = date != null && date == pickedDate
                    val isToday = date != null && date == selectedDate
                    val hasEvents = dayEvents.isNotEmpty()
                    val dayPreview = if (dayEvents.isEmpty()) {
                        ""
                    } else {
                        val firstTitle = dayEvents.first().event.title
                        if (dayEvents.size > 1) "$firstTitle +${dayEvents.size - 1}" else firstTitle
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(62.dp)
                            .clickable(enabled = date != null) { if (date != null) pickedDate = date },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        } else if (hasEvents) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = when {
                                isSelected || isToday -> 1.dp
                                hasEvents -> 0.8.dp
                                else -> 0.6.dp
                            },
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                hasEvents -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            }
                        )
                    ) {
                        if (date != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (hasEvents) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (hasEvents) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                        )
                                        Text(
                                            text = dayPreview,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.calendar_selected_date_events, pickedDate.format(selectedDateFormatter)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        val selectedEvents = eventsByDate[pickedDate].orEmpty().sortedBy { it.daysRemaining }
        if (selectedEvents.isEmpty()) {
            Text(
                text = stringResource(R.string.calendar_no_events),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(selectedEvents, key = { it.event.id }) { state ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onEventClick(state.event.id) },
                                onLongClick = { onEventLongClick(state.event.id) }
                            ),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.event.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}













