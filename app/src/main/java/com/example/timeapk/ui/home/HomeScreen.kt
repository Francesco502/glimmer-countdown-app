package com.example.timeapk.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.example.timeapk.ui.theme.SongCalendarCell
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongFilterChip
import com.example.timeapk.ui.theme.SongModeTabRow
import com.example.timeapk.ui.theme.SongPalette
import com.example.timeapk.ui.theme.SongPaperSurface
import com.example.timeapk.ui.utils.formatLunarDateString

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
    val calendarUiState by viewModel.calendarUiState.collectAsState()
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
    val pinnedEventIds by prefs.pinnedEventIdsFlow.collectAsState(initial = emptyList())
    val savedHomeDisplayMode by prefs.homeDisplayModeFlow.collectAsState(initial = 0)
    var homeDisplayMode by remember(savedHomeDisplayMode) { mutableStateOf(savedHomeDisplayMode) }
    var showSortMenu by remember { mutableStateOf(false) }
    val hasActiveFilter = filterType != FilterType.All
    var showFilterPanel by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(searchQuery.isNotBlank()) }
    var timelineFocus by remember { mutableStateOf<TimelineBucketType?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(homeDisplayMode) {
        prefs.setHomeDisplayMode(homeDisplayMode)
    }
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) showSearchBar = true
    }
    val timelineDigest = remember(calendarUiState, today, pinnedEventIds) {
        buildHomeTimelineDigest(
            events = calendarUiState,
            today = today,
            pinnedEventIds = pinnedEventIds
        )
    }
    val focusedTimelineList = remember(timelineFocus, calendarUiState, today) {
        timelineFocus?.let { type ->
            filterEventsForTimelineBucket(calendarUiState, today, type)
        }
    }
    val displayedList = focusedTimelineList ?: homeUiState

    // Reorder list state is seeded synchronously to avoid first-frame jump/glitch.
    val orderedList = remember { mutableStateListOf<EventUiState>() }
    var listInitialized by remember { mutableStateOf(false) }
    var dragInProgress by remember { mutableStateOf(false) }
    var pendingDisplayedList by remember { mutableStateOf<List<EventUiState>?>(null) }

    fun applyDisplayedListSnapshot(target: List<EventUiState>) {
        if (target.isEmpty()) {
            orderedList.clear()
            listInitialized = true
            return
        }

        if (orderedList.isEmpty()) {
            orderedList.addAll(target)
            listInitialized = true
            return
        }

        val currentIds = orderedList.map { it.event.id }
        val targetIds = target.map { it.event.id }
        if (shouldKeepCurrentCustomOrder(currentIds, targetIds, sortType)) {
            orderedList.refreshItemsByKey(target) { it.event.id }
        } else {
            orderedList.replaceWithOrderedItems(target) { it.event.id }
        }
        listInitialized = true
    }

    if (orderedList.isEmpty() && displayedList.isNotEmpty()) {
        orderedList.addAll(displayedList)
        listInitialized = true
    }

    LaunchedEffect(displayedList, dragInProgress, sortType) {
        if (dragInProgress) {
            pendingDisplayedList = displayedList
            return@LaunchedEffect
        }

        val targetList = pendingDisplayedList ?: displayedList
        pendingDisplayedList = null
        applyDisplayedListSnapshot(targetList)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (showSearchBar) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            singleLine = true,
                            shape = RoundedCornerShape(4.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    InlineActionIconButton(
                        icon = Icons.Outlined.Search,
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
                        icon = Icons.Outlined.Tune,
                        contentDescription = stringResource(R.string.home_filter_panel_toggle),
                        active = hasActiveFilter || showFilterPanel,
                        onClick = { showFilterPanel = !showFilterPanel }
                    )
                    Box {
                        InlineActionIconButton(
                            icon = Icons.AutoMirrored.Outlined.Sort,
                            contentDescription = stringResource(R.string.sort_menu),
                            active = sortType != SortType.Custom || showSortMenu,
                            onClick = { showSortMenu = true }
                        )
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_by_created)) },
                                onClick = {
                                    viewModel.updateSortType(SortType.Custom)
                                    showSortMenu = false
                                }
                            )
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
                        }
                    }
                    InlineActionIconButton(
                        icon = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        onClick = navigateToSettings
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
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
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(SongDesignTokens.BorderWidth.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = SongDesignTokens.BorderAlphaStrong)),
                shadowElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
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
            HomeDisplayModeSegmentedControl(
                selectedMode = homeDisplayMode,
                onModeSelected = { homeDisplayMode = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
            HomeTimelineDigestRow(
                digest = timelineDigest,
                selectedBucket = timelineFocus,
                onBucketClick = { bucket ->
                    timelineFocus = if (timelineFocus == bucket) null else bucket
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

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
                        SongFilterChip(
                            selected = filterType == FilterType.All,
                            onClick = { viewModel.updateFilterType(FilterType.All) },
                            label = stringResource(R.string.filter_all)
                        )
                        SongFilterChip(
                            selected = filterType == FilterType.Birthday,
                            onClick = { viewModel.updateFilterType(FilterType.Birthday) },
                            label = stringResource(R.string.category_birthday)
                        )
                        SongFilterChip(
                            selected = filterType == FilterType.Anniversary,
                            onClick = { viewModel.updateFilterType(FilterType.Anniversary) },
                            label = stringResource(R.string.category_anniversary)
                        )
                        SongFilterChip(
                            selected = filterType == FilterType.Other,
                            onClick = { viewModel.updateFilterType(FilterType.Other) },
                            label = stringResource(R.string.category_other)
                        )
                    }
                }
            }
            if (sortType == SortType.Custom && homeDisplayMode != 2 && displayedList.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.home_custom_sort_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
            // Event list / month view
            if (homeDisplayMode == 2) {
                if (calendarUiState.isEmpty()) {
                    EmptyState(modifier = Modifier.fillMaxSize())
                } else {
                    MonthCalendarView(
                        events = calendarUiState,
                        selectedDate = today,
                        onEventClick = { navigateToDetail(it) },
                        onEventLongClick = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                key(homeDisplayMode) {
                    val dragEnabled = homeCardDragSortEnabled(sortType)
                    val longPressEditEnabled = homeCardLongPressEditEnabled(sortType)
                    val tapOnlyInteraction = homeCardUsesTapOnlyInteraction(sortType)
                    val tapNavigationEnabled = homeCardTapNavigationEnabled(sortType)
                    val useListLevelReorderDetection = homeUsesListLevelReorderDetection(sortType)
                    val reorderState = rememberReorderableLazyListState(
                        onMove = { from, to ->
                            if (dragEnabled) {
                                dragInProgress = true
                                val fromIdx = from.index
                                val toIdx = to.index
                                if (fromIdx in orderedList.indices && toIdx in orderedList.indices && fromIdx != toIdx) {
                                    val item = orderedList.removeAt(fromIdx)
                                    orderedList.add(toIdx, item)
                                }
                            }
                        },
                        onDragEnd = { _, _ ->
                            dragInProgress = false
                            if (dragEnabled) {
                                scope.launch { prefs.setCustomEventOrder(orderedList.map { it.event.id }) }
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
                                            if (dragEnabled && useListLevelReorderDetection) {
                                                Modifier
                                                    .reorderable(reorderState)
                                                    .detectReorderAfterLongPress(reorderState)
                                            } else if (dragEnabled) {
                                                Modifier
                                                    .reorderable(reorderState)
                                            } else {
                                                Modifier
                                            }
                                        ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(if (homeDisplayMode == 0) 12.dp else SongDesignTokens.PaddingList.dp)
                                ) {
                                    items(orderedList, key = { it.event.id }) { eventState ->
                                        ReorderableItem(reorderState, key = eventState.event.id) { isDragging ->
                                            val haptic = LocalHapticFeedback.current
                                            LaunchedEffect(isDragging) {
                                                if (isDragging) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .then(
                                                        if (dragEnabled && !useListLevelReorderDetection) {
                                                            Modifier.detectReorderAfterLongPress(reorderState)
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
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
                                                        onLongClick = if (longPressEditEnabled) {
                                                            { navigateToEdit(eventState.event.id) }
                                                        } else {
                                                            null
                                                        },
                                                        tapOnlyInteraction = tapOnlyInteraction,
                                                        tapNavigationEnabled = tapNavigationEnabled,
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
                                                        onLongClick = if (longPressEditEnabled) {
                                                            { navigateToEdit(eventState.event.id) }
                                                        } else {
                                                            null
                                                        },
                                                        tapOnlyInteraction = tapOnlyInteraction,
                                                        tapNavigationEnabled = tapNavigationEnabled,
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
private fun HomeDisplayModeSegmentedControl(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        0 to stringResource(R.string.display_mode_card),
        1 to stringResource(R.string.display_mode_list),
        2 to stringResource(R.string.display_mode_calendar)
    )
    SongModeTabRow(
        options = modes,
        selected = selectedMode,
        onSelected = onModeSelected,
        modifier = modifier
    )
}

@Composable
private fun HomeTimelineDigestRow(
    digest: HomeTimelineDigest,
    selectedBucket: TimelineBucketType?,
    onBucketClick: (TimelineBucketType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimelineDigestChip(
            label = stringResource(R.string.home_timeline_today),
            bucket = digest.today,
            selected = selectedBucket == TimelineBucketType.Today,
            onClick = { onBucketClick(TimelineBucketType.Today) }
        )
        TimelineDigestChip(
            label = stringResource(R.string.home_timeline_seven_days),
            bucket = digest.sevenDays,
            selected = selectedBucket == TimelineBucketType.SevenDays,
            onClick = { onBucketClick(TimelineBucketType.SevenDays) }
        )
        TimelineDigestChip(
            label = stringResource(R.string.home_timeline_month),
            bucket = digest.month,
            selected = selectedBucket == TimelineBucketType.Month,
            onClick = { onBucketClick(TimelineBucketType.Month) }
        )
        TimelineDigestChip(
            label = stringResource(R.string.home_timeline_milestone),
            bucket = digest.milestone,
            selected = selectedBucket == TimelineBucketType.Milestone,
            onClick = { onBucketClick(TimelineBucketType.Milestone) }
        )
    }
}

@Composable
private fun TimelineDigestChip(
    label: String,
    bucket: TimelineBucket,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaSoft)
    }
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor,
        border = BorderStroke(SongDesignTokens.BorderWidth.dp, borderColor),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 96.dp, max = 132.dp)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.home_timeline_count_format, bucket.count),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = bucket.topItem?.event?.title ?: stringResource(R.string.home_timeline_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
        modifier = Modifier.size(40.dp)
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
                imageVector = Icons.Outlined.Add,
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
    onLongClick: (() -> Unit)?,
    tapOnlyInteraction: Boolean = false,
    tapNavigationEnabled: Boolean = true,
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
        targetValue = AnimationSpecs.responsiveScale(if (isDragging) 1.02f else if (isPressed) 0.98f else 1f),
        animationSpec = AnimationSpecs.springItem,
        label = "cardScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = AnimationSpecs.responsiveAlpha(if (isDragging) 0.85f else if (isPressed) 0.92f else 1f),
        animationSpec = AnimationSpecs.mediumTween(),
        label = "cardAlpha"
    )

    val lightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val cardContainerColor = when {
        isPast && lightSurface -> SongPalette.PaperMuted
        isPast -> MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
        else -> MaterialTheme.colorScheme.surface
    }
    val cardContentColor = lerp(MaterialTheme.colorScheme.onSurface, baseCardColor, 0.06f)
    val cardAccentColor = baseCardColor.copy(alpha = if (isPast) 0.30f else 0.58f)
    val cardBorderColor = baseCardColor.copy(alpha = if (isPast) 0.16f else 0.22f)

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
            if (eventState.nextMilestoneDays != null && eventState.nextMilestoneValue != null) {
                displayContent = formatDaysSmart(eventState.nextMilestoneDays, false, locale)
                displayUnit = stringResource(R.string.days_unit)
                val milestoneStr = milestoneLabel(eventState.nextMilestoneValue)
                labelText = stringResource(R.string.milestone_label_prefix, milestoneStr)
            } else {
                displayContent = ""
                displayUnit = ""
                labelText = stringResource(R.string.milestone_none)
            }
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

    SongPaperSurface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = cardAlpha
            }
            .then(
                if (!tapNavigationEnabled) {
                    Modifier
                } else if (tapOnlyInteraction) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick
                    )
                } else {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                }
            )
            .semantics(mergeDescendants = true) { contentDescription = cardDescription },
        backgroundColor = cardContainerColor,
        borderColor = if (isDragging) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
        } else {
            cardBorderColor
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(52.dp)
                    .background(cardAccentColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))

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
                        style = MaterialTheme.typography.titleMedium,
                        color = cardContentColor.copy(alpha = if (isPast) 0.8f else 1.0f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val dateLine = targetLocalDate.format(dateFormatter)
                    val dateLineStyle = if (dateLine.length > 12) {
                        MaterialTheme.typography.labelSmall
                    } else {
                        MaterialTheme.typography.bodySmall
                    }
                    Text(
                        text = dateLine,
                        style = dateLineStyle,
                        color = cardContentColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                    .then(
                        if (tapNavigationEnabled) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current,
                                onClick = onToggleDateDeltaDisplayMode
                            )
                        } else {
                            Modifier
                        }
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
                                style = MaterialTheme.typography.displaySmall,
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
                        letterSpacing = 0.sp
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
    onLongClick: (() -> Unit)?,
    tapOnlyInteraction: Boolean = false,
    tapNavigationEnabled: Boolean = true,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = AnimationSpecs.responsiveScale(if (isDragging) 1.02f else if (isPressed) 0.98f else 1f),
        animationSpec = AnimationSpecs.springItem,
        label = "listItemScale"
    )
    val itemAlpha by animateFloatAsState(
        targetValue = AnimationSpecs.responsiveAlpha(if (isDragging) 0.85f else if (isPressed) 0.92f else 1f),
        animationSpec = AnimationSpecs.mediumTween(),
        label = "listItemAlpha"
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
            if (eventState.nextMilestoneDays != null && eventState.nextMilestoneValue != null) {
                daysDisplay = formatDaysSmart(eventState.nextMilestoneDays, false, locale) + stringResource(R.string.days_unit)
                val milestoneStr = milestoneLabel(eventState.nextMilestoneValue)
                labelText = stringResource(R.string.milestone_label_prefix, milestoneStr)
            } else {
                daysDisplay = ""
                labelText = stringResource(R.string.milestone_none)
            }
        }
        else -> {
            daysDisplay = ""
            labelText = ""
        }
    }

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
        lerp(eventColor, MaterialTheme.colorScheme.onBackground, 0.4f)
    } else {
        lerp(eventColor, MaterialTheme.colorScheme.onBackground, 0.2f)
    }
    val rowBackground = when {
        isDragging -> MaterialTheme.colorScheme.surface
        isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        else -> Color.Transparent
    }

    val baseTextColorListItem = MaterialTheme.colorScheme.onSurface
    val itemContentColor = lerp(baseTextColorListItem, eventColor, 0.04f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = itemAlpha
            }
            .background(color = rowBackground, shape = MaterialTheme.shapes.medium)
            .then(
                if (isDragging) Modifier.border(SongDesignTokens.BorderWidth.dp, MaterialTheme.colorScheme.primary.copy(alpha = SongDesignTokens.BorderAlphaStrong), MaterialTheme.shapes.medium)
                else Modifier
            )
            .then(
                if (!tapNavigationEnabled) {
                    Modifier
                } else if (tapOnlyInteraction) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick
                    )
                } else {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                }
            )
            .semantics(mergeDescendants = true) { contentDescription = itemDescription }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(18.dp)
                    .background(
                        color = eventColor.copy(alpha = if (isPast) 0.26f else 0.50f),
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        letterSpacing = 0.sp
                    ),
                    color = itemContentColor.copy(alpha = if (isPast) 0.84f else 1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
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
                        color = eventColor.copy(alpha = if (isPast) 0.56f else 0.74f),
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
                modifier = Modifier.height(28.dp),
                thickness = 0.6.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .widthIn(min = 96.dp, max = 152.dp)
                    .sizeIn(minHeight = 48.dp)
                    .then(
                        if (tapNavigationEnabled) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current,
                                onClick = onToggleDateDeltaDisplayMode
                            )
                        } else {
                            Modifier
                        }
                    ),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = daysDisplay,
                    style = when {
                        daysDisplay.length > 12 -> MaterialTheme.typography.bodySmall
                        daysDisplay.length > 8 -> MaterialTheme.typography.bodyMedium
                        else -> MaterialTheme.typography.titleMedium
                    },
                    color = if (isPast) displayColor.copy(alpha = 0.82f) else displayColor,
                    maxLines = if (daysDisplay.length > 12) 3 else 2,
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
                        letterSpacing = 0.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 24.dp),
            thickness = SongDesignTokens.BorderWidth.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
    }
}
















@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthCalendarView(
    events: List<EventUiState>,
    selectedDate: LocalDate,
    onEventClick: (Int) -> Unit,
    onEventLongClick: ((Int) -> Unit)?,
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
    val occurrences = remember(events, currentMonth) {
        calendarOccurrencesForMonth(events, currentMonth)
    }
    val monthHighlights = remember(occurrences) {
        monthHighlightsForOccurrences(occurrences)
    }
    val eventsByDate = remember(occurrences) { occurrences.groupBy { it.date } }

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
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_prev_month))
            }
            Text(
                text = currentMonth.atDay(1).format(monthFormatter),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next_month))
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
                    if (date == null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                        )
                    } else {
                        val cellContent = calendarDayCellContent(date, dayEvents)
                        SongCalendarCell(
                            dayText = cellContent.dayText,
                            eventIndicatorText = cellContent.eventIndicatorText,
                            selected = isSelected,
                            today = isToday,
                            hasEvents = hasEvents,
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            onClick = { pickedDate = date }
                        )
                    }
                }
            }
        }

        MonthHighlightsSection(
            highlights = monthHighlights,
            onEventClick = onEventClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )

        Column(
            modifier = Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.calendar_selected_date_events, pickedDate.format(selectedDateFormatter)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.calendar_selected_date_lunar,
                    formatLunarDateString(pickedDate, context)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val selectedEvents = eventsByDate[pickedDate].orEmpty()
        if (selectedEvents.isEmpty()) {
            Text(
                text = stringResource(R.string.calendar_no_events),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(selectedEvents, key = { "${it.eventState.event.id}-${it.date}" }) { occurrence ->
                    CalendarOccurrenceRow(
                        occurrence = occurrence,
                        onEventClick = onEventClick,
                        onEventLongClick = onEventLongClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthHighlightsSection(
    highlights: MonthHighlightSummary,
    onEventClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (
        highlights.birthdays.totalCount == 0 &&
        highlights.anniversaries.totalCount == 0 &&
        highlights.countdowns.totalCount == 0 &&
        highlights.milestones.totalCount == 0
    ) {
        return
    }

    Surface(
        modifier = modifier,
        color = Color.Transparent,
        shape = RoundedCornerShape(3.dp),
        border = BorderStroke(
            SongDesignTokens.BorderWidth.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaSoft)
        ),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.month_highlights_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            MonthHighlightGroupRow(
                label = stringResource(R.string.month_highlights_birthdays),
                group = highlights.birthdays,
                onEventClick = onEventClick
            )
            MonthHighlightGroupRow(
                label = stringResource(R.string.month_highlights_anniversaries),
                group = highlights.anniversaries,
                onEventClick = onEventClick
            )
            MonthHighlightGroupRow(
                label = stringResource(R.string.month_highlights_countdowns),
                group = highlights.countdowns,
                onEventClick = onEventClick
            )
            MonthHighlightGroupRow(
                label = stringResource(R.string.month_highlights_milestones),
                group = highlights.milestones,
                onEventClick = onEventClick
            )
        }
    }
}

@Composable
private fun MonthHighlightGroupRow(
    label: String,
    group: MonthHighlightGroup,
    onEventClick: (Int) -> Unit
) {
    if (group.totalCount == 0) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.86f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.home_timeline_count_format, group.totalCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        group.items.forEach { occurrence ->
            Text(
                text = occurrence.eventState.event.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEventClick(occurrence.eventState.event.id) }
                    .padding(vertical = 2.dp)
            )
        }
        val hiddenCount = group.totalCount - group.items.size
        if (hiddenCount > 0) {
            Text(
                text = stringResource(R.string.month_highlights_more_format, hiddenCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarOccurrenceRow(
    occurrence: CalendarEventOccurrence,
    onEventClick: (Int) -> Unit,
    onEventLongClick: ((Int) -> Unit)?
) {
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
                .heightIn(min = 44.dp)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(14.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                        RoundedCornerShape(1.dp)
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = occurrence.eventState.event.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(
            thickness = SongDesignTokens.BorderWidth.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
        )
    }
}
