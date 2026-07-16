package com.example.timeapk.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.data.REPEAT_DAILY
import com.example.timeapk.data.REPEAT_HALF_YEARLY
import com.example.timeapk.data.REPEAT_MONTHLY
import com.example.timeapk.data.REPEAT_WEEKLY
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.ui.components.SongDateWheelPickerDialog
import com.example.timeapk.ui.utils.formatBetweenAsYMD
import com.example.timeapk.ui.utils.formatDaysSmart
import com.example.timeapk.ui.utils.getDisplayDateFormatter
import com.example.timeapk.ui.utils.parseEventColorOrFallback
import com.example.timeapk.ui.utils.eventDateToLocalDate
import com.example.timeapk.ui.utils.DisplayModes
import com.example.timeapk.ui.utils.getAvailableDisplayModes
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.ui.sound.SongSoundEffect
import com.example.timeapk.ui.sound.rememberSongSoundscape
import com.example.timeapk.ui.theme.AnimationSpecs
import com.example.timeapk.ui.theme.SongCalendarCell
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind
import com.example.timeapk.ui.theme.SongModeTabRow
import com.example.timeapk.ui.theme.SongPalette
import com.example.timeapk.ui.theme.SongPaperSurface
import com.example.timeapk.ui.theme.SongSealLabel
import com.example.timeapk.ui.utils.formatLunarDateString

import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.OverscrollConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import org.burnoutcrew.reorderable.ReorderableItem

private val HomeOverflowActionItemHeight = 42.dp

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
    var showOverflowMenu by remember { mutableStateOf(false) }
    var timelineFocus by remember { mutableStateOf<TimelineBucketType?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(homeDisplayMode) {
        prefs.setHomeDisplayMode(homeDisplayMode)
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
    var pendingLocalReorder by remember { mutableStateOf<PendingLocalReorderSnapshot?>(null) }

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
        val syncDecision = decideHomeListTargetSync(
            currentIds = currentIds,
            targetIds = targetIds,
            sortType = sortType,
            pending = pendingLocalReorder
        )
        if (syncDecision.retainCurrentOrder) {
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

    LaunchedEffect(displayedList, dragInProgress, sortType, pendingLocalReorder) {
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
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    HomeOverflowActionMenu(
                        selectedBucket = timelineFocus,
                        searchQuery = searchQuery,
                        filterType = filterType,
                        sortType = sortType,
                        expanded = showOverflowMenu,
                        onExpandedChange = { showOverflowMenu = it }
                    )
                    val soundscape = rememberSongSoundscape()
                    InlineActionIconButton(
                        icon = SongLineIconKind.Ruyi,
                        contentDescription = stringResource(R.string.settings_title),
                        active = false,
                        onClick = {
                            soundscape.play(SongSoundEffect.Action)
                            showOverflowMenu = false
                            navigateToSettings()
                        }
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
                    SongLineIcon(
                        kind = SongLineIconKind.Add,
                        contentDescription = stringResource(R.string.cd_add_event),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        size = 24.dp
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HomeDisplayModeSegmentedControl(
                    selectedMode = homeDisplayMode,
                    onModeSelected = { homeDisplayMode = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

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
                    val dragEnabled = canStartHomeReorder(sortType, pendingLocalReorder)
                    val longPressEditEnabled = homeCardLongPressEditEnabled(sortType)
                    val tapOnlyInteraction = homeCardUsesTapOnlyInteraction(sortType)
                    val tapNavigationEnabled = homeCardTapNavigationEnabled(sortType)
                    val useListLevelReorderDetection = homeUsesListLevelReorderDetection(sortType)
                    val latestDragEnabled by rememberUpdatedState(dragEnabled)
                    val latestDisplayedIds by rememberUpdatedState(
                        displayedList.map { it.event.id }
                    )
                    val latestDisplayedItems by rememberUpdatedState(displayedList)
                    val latestPinnedEventIds by rememberUpdatedState(pinnedEventIds)
                    val latestSortType by rememberUpdatedState(sortType)
                    val latestViewModel by rememberUpdatedState(viewModel)
                    var visibleIdsAtDragStart by remember { mutableStateOf<List<Int>?>(null) }
                    val reorderState = rememberReorderableLazyListState(
                        onMove = { from, to ->
                            if (latestDragEnabled && pendingLocalReorder == null) {
                                if (visibleIdsAtDragStart == null) {
                                    visibleIdsAtDragStart = latestDisplayedIds
                                }
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
                            val visibleIds = visibleIdsAtDragStart
                            visibleIdsAtDragStart = null
                            val reorderedVisibleIds = orderedList.map { it.event.id }
                            val persistenceRequest = homeReorderPersistenceRequestOrNull(
                                dragEnabledAtEnd = latestDragEnabled,
                                visibleIds = visibleIds,
                                reorderedVisibleIds = reorderedVisibleIds
                            )
                            if (persistenceRequest != null) {
                                pendingLocalReorder = persistenceRequest.snapshot
                            }
                            dragInProgress = false
                            if (persistenceRequest != null) {
                                latestViewModel.updateCustomEventOrder(
                                    visibleIds = persistenceRequest.visibleIds,
                                    reorderedVisibleIds = persistenceRequest.reorderedVisibleIds,
                                    onPersistenceResult = { persistedMergedIds ->
                                        if (pendingLocalReorder == persistenceRequest.snapshot) {
                                            val authoritativeItems = if (
                                                persistedMergedIds != null && latestSortType == SortType.Custom
                                            ) {
                                                settlePersistedHomeReorder(
                                                    displayedItems = latestDisplayedItems,
                                                    persistedMergedIds = persistedMergedIds,
                                                    pinnedEventIds = latestPinnedEventIds,
                                                    sortType = latestSortType
                                                )
                                            } else {
                                                latestDisplayedItems
                                            }
                                            orderedList.replaceWithOrderedItems(authoritativeItems) {
                                                it.event.id
                                            }
                                            pendingDisplayedList = null
                                            pendingLocalReorder = null
                                            listInitialized = true
                                        }
                                    }
                                )
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
            AnimatedVisibility(
                visible = showOverflowMenu,
                enter = fadeIn(animationSpec = AnimationSpecs.mistDissolveTween()) +
                    slideInVertically(animationSpec = AnimationSpecs.handscrollTweenIntOffset()) { -it / 10 },
                exit = fadeOut(animationSpec = AnimationSpecs.mistDissolveTween()) +
                    slideOutVertically(animationSpec = AnimationSpecs.handscrollTweenIntOffset()) { -it / 10 },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 16.dp)
                    .zIndex(2f)
            ) {
                HomeOverflowPanel(
                    digest = timelineDigest,
                    selectedBucket = timelineFocus,
                    searchQuery = searchQuery,
                    filterType = filterType,
                    sortType = sortType,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onBucketClick = { bucket ->
                        timelineFocus = if (timelineFocus == bucket) null else bucket
                        showOverflowMenu = false
                    },
                    onFilterClick = { type ->
                        viewModel.updateFilterType(type)
                        showOverflowMenu = false
                    },
                    onSortClick = { type ->
                        viewModel.updateSortType(type)
                        showOverflowMenu = false
                    }
                )
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
private fun HomeOverflowActionMenu(
    selectedBucket: TimelineBucketType?,
    searchQuery: String,
    filterType: FilterType,
    sortType: SortType,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val hasActiveTool = selectedBucket != null ||
        searchQuery.isNotBlank() ||
        filterType != FilterType.All ||
        sortType != SortType.Custom
    val soundscape = rememberSongSoundscape()
    Box {
        InlineActionIconButton(
            icon = SongLineIconKind.More,
            contentDescription = stringResource(R.string.home_timeline_action_menu),
            active = hasActiveTool || expanded,
            onClick = {
                soundscape.play(SongSoundEffect.Action)
                onExpandedChange(!expanded)
            }
        )
    }
}

@Composable
private fun HomeOverflowPanel(
    digest: HomeTimelineDigest,
    selectedBucket: TimelineBucketType?,
    searchQuery: String,
    filterType: FilterType,
    sortType: SortType,
    onSearchQueryChange: (String) -> Unit,
    onBucketClick: (TimelineBucketType) -> Unit,
    onFilterClick: (FilterType) -> Unit,
    onSortClick: (SortType) -> Unit,
    modifier: Modifier = Modifier
) {
    val soundscape = rememberSongSoundscape()
    fun playActionThen(action: () -> Unit) {
        soundscape.play(SongSoundEffect.Action)
        action()
    }

    SongActionSlip(
        modifier = modifier,
        content = {
            HomeMenuSectionLabel(text = stringResource(R.string.home_timeline_action_menu))
            TimelineActionTileGrid(
                items = listOf(
                    TimelineActionTileSpec(
                        label = stringResource(R.string.home_timeline_today),
                        bucket = digest.today,
                        selected = selectedBucket == TimelineBucketType.Today,
                        onClick = { playActionThen { onBucketClick(TimelineBucketType.Today) } }
                    ),
                    TimelineActionTileSpec(
                        label = stringResource(R.string.home_timeline_seven_days),
                        bucket = digest.sevenDays,
                        selected = selectedBucket == TimelineBucketType.SevenDays,
                        onClick = { playActionThen { onBucketClick(TimelineBucketType.SevenDays) } }
                    ),
                    TimelineActionTileSpec(
                        label = stringResource(R.string.home_timeline_month),
                        bucket = digest.month,
                        selected = selectedBucket == TimelineBucketType.Month,
                        onClick = { playActionThen { onBucketClick(TimelineBucketType.Month) } }
                    ),
                    TimelineActionTileSpec(
                        label = stringResource(R.string.home_timeline_milestone),
                        bucket = digest.milestone,
                        selected = selectedBucket == TimelineBucketType.Milestone,
                        onClick = { playActionThen { onBucketClick(TimelineBucketType.Milestone) } }
                    )
                )
            )
            SongActionSlipDivider()
            HomeMenuSectionLabel(text = stringResource(R.string.home_filter_panel_toggle))
            SongActionOptionGrid(
                items = listOf(
                    SongActionOptionSpec(
                        label = stringResource(R.string.filter_all),
                        selected = filterType == FilterType.All,
                        onClick = { playActionThen { onFilterClick(FilterType.All) } }
                    ),
                    SongActionOptionSpec(
                        label = stringResource(R.string.category_birthday),
                        selected = filterType == FilterType.Birthday,
                        onClick = { playActionThen { onFilterClick(FilterType.Birthday) } }
                    ),
                    SongActionOptionSpec(
                        label = stringResource(R.string.category_anniversary),
                        selected = filterType == FilterType.Anniversary,
                        onClick = { playActionThen { onFilterClick(FilterType.Anniversary) } }
                    ),
                    SongActionOptionSpec(
                        label = stringResource(R.string.category_other),
                        selected = filterType == FilterType.Other,
                        onClick = { playActionThen { onFilterClick(FilterType.Other) } }
                    )
                )
            )
            SongActionSlipDivider()
            HomeMenuSectionLabel(text = stringResource(R.string.sort_menu))
            SongActionOptionGrid(
                items = listOf(
                    SongActionOptionSpec(
                        label = stringResource(R.string.sort_by_created),
                        selected = sortType == SortType.Custom,
                        onClick = { playActionThen { onSortClick(SortType.Custom) } }
                    ),
                    SongActionOptionSpec(
                        label = stringResource(R.string.sort_by_days),
                        selected = sortType == SortType.ByDays,
                        onClick = { playActionThen { onSortClick(SortType.ByDays) } }
                    ),
                    SongActionOptionSpec(
                        label = stringResource(R.string.sort_by_date),
                        selected = sortType == SortType.ByDate,
                        onClick = { playActionThen { onSortClick(SortType.ByDate) } }
                    )
                )
            )
        },
        footer = {
            HomeOverflowSearchField(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange
            )
        }
    )
}

@Composable
private fun HomeOverflowSearchField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    val shape = RoundedCornerShape(3.dp)
    val textColor = MaterialTheme.colorScheme.onSurface
    val borderColor = if (searchQuery.isNotBlank()) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
    }
    val backgroundColor = if (searchQuery.isNotBlank()) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.22f)
    }
    BasicTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(BorderStroke(SongDesignTokens.BorderWidth.dp, borderColor), shape)
            .background(backgroundColor, shape)
            .padding(horizontal = 10.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                SongLineIcon(
                    kind = SongLineIconKind.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f),
                    size = 18.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isBlank()) {
                        Text(
                            text = stringResource(R.string.search_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
                if (searchQuery.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = { onSearchQueryChange("") }),
                        contentAlignment = Alignment.Center
                    ) {
                        SongLineIcon(
                            kind = SongLineIconKind.Close,
                            contentDescription = stringResource(R.string.cd_clear_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f),
                            size = 18.dp
                        )
                    }
                }
            }
        }
    )
}

private data class TimelineActionTileSpec(
    val label: String,
    val bucket: TimelineBucket,
    val selected: Boolean,
    val onClick: () -> Unit
)

private data class SongActionOptionSpec(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun SongActionSlip(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
    footer: (@Composable ColumnScope.() -> Unit)? = null
) {
    SongPaperSurface(
        modifier = modifier
            .widthIn(min = 228.dp, max = 276.dp)
            .heightIn(max = 420.dp)
            .padding(end = 8.dp),
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)
    ) {
        Box {
            SongActionSlipFoldDecoration(modifier = Modifier.matchParentSize())
            Column(
                modifier = Modifier
                    .padding(start = 12.dp, top = 12.dp, end = 18.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    content = content
                )
                footer?.let { footerContent ->
                    SongActionSlipDivider()
                    footerContent()
                }
            }
        }
    }
}

@Composable
private fun SongActionSlipFoldDecoration(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    Canvas(modifier = modifier) {
        val fold = 30.dp.toPx()
        val right = size.width
        val path = Path().apply {
            moveTo(right - fold, 0f)
            lineTo(right, 0f)
            lineTo(right, fold)
            close()
        }
        drawPath(path, primary.copy(alpha = 0.055f))
        drawLine(
            color = primary.copy(alpha = 0.22f),
            start = Offset(right - fold, 0f),
            end = Offset(right, fold),
            strokeWidth = SongDesignTokens.BorderWidth.dp.toPx()
        )
        drawLine(
            color = outline.copy(alpha = 0.10f),
            start = Offset(right - 8.dp.toPx(), fold + 8.dp.toPx()),
            end = Offset(right - 8.dp.toPx(), size.height - 10.dp.toPx()),
            strokeWidth = SongDesignTokens.BorderWidth.dp.toPx()
        )
        drawPath(
            Path().apply {
                moveTo(14.dp.toPx(), 8.dp.toPx())
                cubicTo(
                    64.dp.toPx(), 4.dp.toPx(),
                    size.width - 72.dp.toPx(), 10.dp.toPx(),
                    size.width - 44.dp.toPx(), 6.dp.toPx()
                )
            },
            color = outline.copy(alpha = 0.12f),
            style = Stroke(width = SongDesignTokens.BorderWidth.dp.toPx())
        )
    }
}

@Composable
private fun SongActionSlipDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(0.72f),
            thickness = SongDesignTokens.BorderWidth.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.13f)
        )
    }
}

@Composable
private fun HomeMenuSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun TimelineActionTileGrid(items: List<TimelineActionTileSpec>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    TimelineActionTile(
                        spec = item,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TimelineActionTile(
    spec: TimelineActionTileSpec,
    modifier: Modifier = Modifier
) {
    val color = if (spec.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(3.dp)
    Column(
        modifier = modifier
            .border(
                BorderStroke(
                    SongDesignTokens.BorderWidth.dp,
                    if (spec.selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                ),
                shape
            )
            .background(
                if (spec.selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.075f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
                shape
            )
            .clickable(onClick = spec.onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = spec.label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            SongSealLabel(
                text = pluralStringResource(
                    R.plurals.home_timeline_count_format,
                    spec.bucket.count,
                    spec.bucket.count
                ),
                color = color.copy(alpha = if (spec.selected) 0.88f else 0.62f)
            )
            spec.bucket.topItem?.event?.title?.let { title ->
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SongActionOptionGrid(items: List<SongActionOptionSpec>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    SongActionOptionTile(
                        spec = item,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SongActionOptionTile(
    spec: SongActionOptionSpec,
    modifier: Modifier = Modifier
) {
    val color = if (spec.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(3.dp)
    Row(
        modifier = modifier
            .heightIn(min = HomeOverflowActionItemHeight)
            .border(
                BorderStroke(
                    SongDesignTokens.BorderWidth.dp,
                    if (spec.selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
                ),
                shape
            )
            .background(
                if (spec.selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
                shape
            )
            .clickable(onClick = spec.onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = spec.label,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (spec.selected) {
            Spacer(modifier = Modifier.width(8.dp))
            SongLineIcon(
                kind = SongLineIconKind.Seal,
                tint = MaterialTheme.colorScheme.primary,
                size = 14.dp
            )
        }
    }
}

@Composable
private fun SongActionSlipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
    trailing: String? = null
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(3.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.055f) else Color.Transparent,
                shape
            )
            .border(
                BorderStroke(
                    SongDesignTokens.BorderWidth.dp,
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent
                ),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            meta?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            SongSealLabel(
                text = trailing,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (selected) {
            Spacer(modifier = Modifier.width(12.dp))
            SongLineIcon(
                kind = SongLineIconKind.Seal,
                tint = MaterialTheme.colorScheme.primary,
                size = 16.dp
            )
        }
    }
}

@Composable
private fun InlineActionIconButton(
    icon: SongLineIconKind,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        SongLineIcon(
            kind = icon,
            contentDescription = contentDescription,
            tint = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f)
            },
            size = 18.dp
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SongLineIcon(
                kind = SongLineIconKind.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                size = 64.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
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
    val lightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val cardContainerColor = when {
        isPast && lightSurface -> SongPalette.PaperMuted
        isPast -> MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
        else -> MaterialTheme.colorScheme.surface
    }
    val cardContentColor = lerp(MaterialTheme.colorScheme.onSurface, baseCardColor, 0.06f)
    val cardAccentColor = baseCardColor.copy(alpha = if (isPast) 0.30f else 0.58f)
    val cardBorderColor = baseCardColor.copy(alpha = if (isPast) 0.16f else 0.22f)
    val effectiveCardBackground = HomeEventColorPolicy.compositeOver(
        foreground = cardContainerColor,
        background = MaterialTheme.colorScheme.background
    )
    val cardOnSurfaceColor = MaterialTheme.colorScheme.onSurface
    val cardAuxiliaryColor = if (lightSurface) {
        baseCardColor.copy(alpha = if (isPast) 0.54f else 0.74f)
    } else {
        remember(baseCardColor, cardOnSurfaceColor, effectiveCardBackground) {
            HomeEventColorPolicy.ensureTextContrast(
                eventColor = baseCardColor,
                onSurface = cardOnSurfaceColor,
                background = effectiveCardBackground
            )
        }
    }

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
    val cardAuxiliaryLine = buildList {
        add(categoryLabel)
        if (eventState.event.isLunar) add(stringResource(R.string.lunar_calendar))
        repeatLabel?.let(::add)
        if (eventState.event.remindEnabled) add(stringResource(R.string.field_remind))
    }.joinToString(" · ")
    val toggleDateDeltaDescription = stringResource(R.string.cd_toggle_date_delta_display)

    SongPaperSurface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
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
                    if (showDetail && cardAuxiliaryLine.isNotBlank()) {
                        Text(
                            text = cardAuxiliaryLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = cardAuxiliaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = toggleDateDeltaDescription
                    },
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
    val itemDescription = buildString {
        append(eventState.event.title)
        append(", ")
        if (isToday) append(todayLabel)
        else append(labelText).append(" ").append(daysDisplay)
    }
    val isLightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val rowBackground = when {
        isDragging -> MaterialTheme.colorScheme.surface
        isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        else -> Color.Transparent
    }
    val effectiveListBackground = HomeEventColorPolicy.compositeOver(
        foreground = rowBackground,
        background = MaterialTheme.colorScheme.background
    )
    val listOnBackgroundColor = MaterialTheme.colorScheme.onBackground
    val displayColor = if (isLightSurface) {
        lerp(eventColor, listOnBackgroundColor, 0.4f)
    } else {
        remember(eventColor, listOnBackgroundColor, effectiveListBackground) {
            HomeEventColorPolicy.ensureTextContrast(
                eventColor = eventColor,
                onSurface = listOnBackgroundColor,
                background = effectiveListBackground
            )
        }
    }

    val baseTextColorListItem = MaterialTheme.colorScheme.onSurface
    val itemContentColor = lerp(baseTextColorListItem, eventColor, 0.04f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
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
                .heightIn(min = 52.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
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
            Text(
                text = eventState.event.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    letterSpacing = 0.sp
                ),
                color = itemContentColor.copy(alpha = if (isPast) 0.84f else 1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            CompactEventTime(
                daysDisplay = daysDisplay,
                labelText = labelText,
                timeColor = if (isPast && isLightSurface) {
                    displayColor.copy(alpha = 0.82f)
                } else {
                    displayColor
                },
                labelColor = itemContentColor.copy(alpha = 0.64f),
                contentDescription = stringResource(R.string.cd_toggle_date_delta_display),
                enabled = tapNavigationEnabled,
                onClick = onToggleDateDeltaDisplayMode
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 24.dp),
            thickness = SongDesignTokens.BorderWidth.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
    }
}

@Composable
private fun CompactEventTime(
    daysDisplay: String,
    labelText: String,
    timeColor: Color,
    labelColor: Color,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(min = 82.dp, max = 138.dp)
            .heightIn(min = 42.dp)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            },
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center
    ) {
        if (daysDisplay.isNotBlank()) {
            Text(
                text = daysDisplay,
                style = when {
                    daysDisplay.length > 12 -> MaterialTheme.typography.bodySmall
                    daysDisplay.length > 8 -> MaterialTheme.typography.bodyMedium
                    else -> MaterialTheme.typography.titleMedium
                },
                color = timeColor,
                maxLines = if (daysDisplay.length > 12) 3 else 2,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (labelText.isNotBlank()) {
            if (daysDisplay.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                letterSpacing = 0.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
    var showMonthPicker by remember { mutableStateOf(false) }
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
    val currentMonthTitle = currentMonth.atDay(1).format(monthFormatter)
    val monthPickerInitialDate = remember(currentMonth, pickedDate) {
        val safeDay = pickedDate.dayOfMonth.coerceIn(1, currentMonth.lengthOfMonth())
        currentMonth.atDay(safeDay)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }
    val occurrences = remember(events, currentMonth) {
        calendarOccurrencesForMonth(events, currentMonth)
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
                pickedDate = pickedMonth.atDay(pickedLocalDate.dayOfMonth.coerceIn(1, pickedMonth.lengthOfMonth()))
                showMonthPicker = false
            }
        )
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
                SongLineIcon(
                    kind = SongLineIconKind.ChevronLeft,
                    contentDescription = stringResource(R.string.calendar_prev_month),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.74f)
                )
            }
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
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                SongLineIcon(
                    kind = SongLineIconKind.ChevronRight,
                    contentDescription = stringResource(R.string.calendar_next_month),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.74f)
                )
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
	                                .heightIn(min = 48.dp, max = 72.dp)
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
	                                .heightIn(min = 48.dp, max = 72.dp),
                            onClick = { pickedDate = date }
                        )
                    }
                }
            }
        }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarOccurrenceRow(
    occurrence: CalendarEventOccurrence,
    onEventClick: (Int) -> Unit,
    onEventLongClick: ((Int) -> Unit)?
) {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val today = LocalDate.now()
    val selectedDatePattern = stringResource(R.string.calendar_selected_date_pattern)
    val rowDateFormatter = remember(selectedDatePattern, locale) {
        DateTimeFormatter.ofPattern(selectedDatePattern, locale)
    }
    val categoryLabel = when (occurrence.eventState.event.category) {
        CATEGORY_BIRTHDAY -> stringResource(R.string.category_birthday)
        CATEGORY_ANNIVERSARY -> stringResource(R.string.category_anniversary)
        else -> stringResource(R.string.category_other)
    }
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
        daysFromToday == 0L -> stringResource(R.string.days_today_label)
        daysFromToday > 0L -> buildString {
            append(stringResource(R.string.days_left_label))
            append(" ")
            append(formatDaysSmart(daysFromToday, false, locale))
            append(stringResource(R.string.days_unit))
        }
        else -> context.resources.getQuantityString(
            R.plurals.days_elapsed_format,
            (-daysFromToday).toInt(),
            (-daysFromToday).toInt()
        )
    }
    val calendarMetaLine = buildList {
        add(categoryLabel)
        repeatLabel?.let(::add)
        if (occurrence.eventState.event.remindEnabled) add(stringResource(R.string.field_remind))
    }.joinToString(" · ")
    val calendarTimeLine = buildList {
        add(occurrence.date.format(rowDateFormatter))
        add(stringResource(R.string.calendar_selected_date_lunar, formatLunarDateString(occurrence.date, context)))
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
                .heightIn(min = 68.dp)
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                        RoundedCornerShape(1.dp)
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = occurrence.eventState.event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = calendarTimeLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
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
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
        )
    }
}
