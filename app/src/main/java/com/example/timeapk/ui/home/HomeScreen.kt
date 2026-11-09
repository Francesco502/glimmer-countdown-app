package com.example.timeapk.ui.home

import android.os.Build
import android.view.View
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.ui.common.categoryLabel
import com.example.timeapk.ui.common.categoryOptions
import com.example.timeapk.ui.components.SongConfirmDialog
import com.example.timeapk.ui.sound.SongSoundEffect
import com.example.timeapk.ui.sound.rememberSongSoundscape
import com.example.timeapk.ui.theme.AnimationSpecs
import com.example.timeapk.ui.theme.SongDesignTokens
import com.example.timeapk.ui.theme.SongLineIcon
import com.example.timeapk.ui.theme.SongLineIconKind
import com.example.timeapk.ui.theme.SongModeTabRow
import com.example.timeapk.ui.theme.SongPaperSurface
import com.example.timeapk.ui.theme.SongSealLabel
import com.example.timeapk.ui.utils.getAvailableDisplayModes
import com.example.timeapk.ui.utils.getDisplayDateFormatter
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import java.time.LocalDate
import java.time.ZoneOffset

private const val NEAREST_BANNER_KEY = "home-nearest-banner"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navigateToItemEntry: () -> Unit,
    navigateToDetail: (Int) -> Unit,
    navigateToEdit: (Int) -> Unit,
    navigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    navigateToItemEntryOnDate: (Long) -> Unit = { navigateToItemEntry() },
    onDeleteEvent: suspend (EventUiState) -> Boolean = { false },
    selectedEventId: Int? = null,
    viewModel: HomeViewModel = viewModel(factory = com.example.timeapk.ui.AppViewModelProvider.Factory)
) {
    val homeUiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val calendarUiState by viewModel.calendarUiState.collectAsStateWithLifecycle()
    val unfilteredCalendarUiState by viewModel.unfilteredCalendarUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefs = (context.applicationContext as TimeApplication).userPrefs
    val today by viewModel.today.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showHours by prefs.showHoursFlow.collectAsStateWithLifecycle(initialValue = true)
    val showMilestone by prefs.showMilestoneFlow.collectAsStateWithLifecycle(initialValue = true)
    val homeDensityMode by prefs.homeDensityModeFlow.collectAsStateWithLifecycle(initialValue = 1)
    val dateFormatMode by prefs.dateFormatModeFlow.collectAsStateWithLifecycle(initialValue = 0)
    val dateFormatter = remember(dateFormatMode) { getDisplayDateFormatter(dateFormatMode) }
    val dateDeltaDisplayMode by prefs.dateDeltaDisplayModeFlow.collectAsStateWithLifecycle(initialValue = 0)
    val perEventDateDeltaModes by prefs.perEventDateDeltaDisplayModesFlow.collectAsStateWithLifecycle(initialValue = emptyMap())
    val pinnedEventIds by prefs.pinnedEventIdsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val savedHomeDisplayMode by prefs.homeDisplayModeFlow.collectAsStateWithLifecycle(initialValue = 0)
    val nearestBannerEnabled by prefs.homeNearestBannerEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    // Initial values hide one-time hints until the stored value arrives, so they never flash.
    val reorderHintSeen by prefs.homeReorderHintSeenFlow.collectAsStateWithLifecycle(initialValue = true)
    val timeToggleHintSeen by prefs.homeTimeToggleHintSeenFlow.collectAsStateWithLifecycle(initialValue = true)
    val homeDisplayMode = savedHomeDisplayMode
    var showOverflowMenu by remember { mutableStateOf(false) }
    var timelineFocus by remember { mutableStateOf<TimelineBucketType?>(null) }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var calendarPickedEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val calendarPickedDate = calendarPickedEpochDay?.let(LocalDate::ofEpochDay) ?: today
    var menuEventId by remember { mutableStateOf<Int?>(null) }
    var pendingDeleteEvent by remember { mutableStateOf<EventUiState?>(null) }
    var deleteInProgress by remember { mutableStateOf(false) }
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
    val calendarEvents = focusedTimelineList ?: calendarUiState
    val emptyStateKind = resolveHomeEmptyStateKind(unfilteredCalendarUiState.isEmpty())
    val clearEmptyStateConstraints: () -> Unit = {
        viewModel.updateSearchQuery("")
        viewModel.updateFilterType(FilterType.All)
        timelineFocus = null
        searchExpanded = false
    }
    val nearestEvent = remember(unfilteredCalendarUiState, today, pinnedEventIds) {
        pickNearestUpcomingEvent(unfilteredCalendarUiState, today, pinnedEventIds)
    }
    val showNearestBanner = nearestBannerEnabled &&
        homeDisplayMode == 0 &&
        nearestEvent != null &&
        categoryFilter == null &&
        timelineFocus == null &&
        searchQuery.isBlank()

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

    BackHandler(enabled = searchExpanded) {
        viewModel.updateSearchQuery("")
        searchExpanded = false
    }

    pendingDeleteEvent?.let { target ->
        SongConfirmDialog(
            title = stringResource(R.string.delete_confirm_title),
            message = stringResource(R.string.delete_confirm_message, target.event.title),
            confirmText = stringResource(
                if (deleteInProgress) R.string.delete_in_progress else R.string.delete_confirm_ok
            ),
            dismissText = stringResource(R.string.delete_confirm_cancel),
            destructiveConfirm = true,
            confirmEnabled = !deleteInProgress,
            onConfirm = {
                if (!deleteInProgress) {
                    deleteInProgress = true
                    scope.launch {
                        try {
                            if (onDeleteEvent(target)) pendingDeleteEvent = null
                        } finally {
                            deleteInProgress = false
                        }
                    }
                }
            },
            onDismiss = { if (!deleteInProgress) pendingDeleteEvent = null }
        )
    }

    Scaffold(
        // The caller sizes the whole screen (the two-pane layout gives it a fixed width).
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (searchExpanded) {
                        HomeSearchField(
                            searchQuery = searchQuery,
                            onSearchQueryChange = viewModel::updateSearchQuery
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
                    val soundscape = rememberSongSoundscape()
                    if (searchExpanded) {
                        InlineActionIconButton(
                            icon = SongLineIconKind.Close,
                            contentDescription = stringResource(R.string.home_search_close),
                            onClick = {
                                viewModel.updateSearchQuery("")
                                searchExpanded = false
                            }
                        )
                    } else {
                        InlineActionIconButton(
                            icon = SongLineIconKind.Search,
                            contentDescription = stringResource(R.string.home_search_open),
                            active = searchQuery.isNotBlank(),
                            onClick = {
                                soundscape.play(SongSoundEffect.Action)
                                showOverflowMenu = false
                                searchExpanded = true
                            }
                        )
                    }
                    HomeOverflowActionMenu(
                        selectedBucket = timelineFocus,
                        categoryFilter = categoryFilter,
                        expanded = showOverflowMenu,
                        onExpandedChange = { showOverflowMenu = it }
                    )
                    InlineActionIconButton(
                        icon = SongLineIconKind.Settings,
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
        floatingActionButtonPosition = if (homeDisplayMode == 2) FabPosition.End else FabPosition.Center,
        floatingActionButton = {
            if (emptyStateKind != HomeEmptyStateKind.FirstEvent) {
                val isEmpty = displayedList.isEmpty()
                val fabScale by animateFloatAsState(
                    if (isEmpty) 1.03f else 1f,
                    animationSpec = AnimationSpecs.springButton,
                    label = "fabScale"
                )
                val addForPickedDate: () -> Unit = {
                    navigateToItemEntryOnDate(
                        calendarPickedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                    )
                }
                HomeAddButton(
                    onClick = if (homeDisplayMode == 2) addForPickedDate else navigateToItemEntry,
                    contentDescription = stringResource(
                        if (homeDisplayMode == 2) R.string.cd_add_event_on_date else R.string.cd_add_event
                    ),
                    modifier = Modifier.graphicsLayer { scaleX = fabScale; scaleY = fabScale }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HomeDisplayModeSegmentedControl(
                    selectedMode = homeDisplayMode,
                    onModeSelected = { mode -> scope.launch { prefs.setHomeDisplayMode(mode) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                val categoryFilterLabel = categoryFilter?.let { categoryLabel(it) }
                val activeFilters = buildList {
                    categoryFilterLabel?.let(::add)
                    timelineFocus?.let {
                        add(stringResource(when (it) {
                            TimelineBucketType.Today -> R.string.home_timeline_today
                            TimelineBucketType.SevenDays -> R.string.home_timeline_seven_days
                            TimelineBucketType.Month -> R.string.home_timeline_month
                            TimelineBucketType.Milestone -> R.string.home_timeline_milestone
                        }))
                    }
                    searchQuery.trim().takeIf(String::isNotEmpty)?.let(::add)
                }
                if (activeFilters.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeFilters.joinToString(" 路 "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = clearEmptyStateConstraints) {
                            Text(stringResource(R.string.home_clear_filters))
                        }
                    }
                }

                val reorderableCount = displayedList.count { it.event.id !in pinnedEventIds }
                if (sortType == SortType.Custom && homeDisplayMode != 2 && reorderableCount > 1 && !reorderHintSeen) {
                    HomeOneTimeHint(
                        text = stringResource(R.string.home_reorder_hint),
                        onDismiss = { scope.launch { prefs.markHomeReorderHintSeen() } }
                    )
                } else if (sortType != SortType.ByDays && homeDisplayMode != 2 &&
                    displayedList.isNotEmpty() && !timeToggleHintSeen
                ) {
                    HomeOneTimeHint(
                        text = stringResource(R.string.home_time_toggle_hint),
                        onDismiss = { scope.launch { prefs.markHomeTimeToggleHintSeen() } }
                    )
                }

            // Event list / month view
            if (homeDisplayMode == 2) {
                if (calendarEvents.isEmpty() && emptyStateKind == HomeEmptyStateKind.FirstEvent) {
                    EmptyState(
                        kind = emptyStateKind,
                        onFirstEventClick = navigateToItemEntry,
                        onClearConstraints = clearEmptyStateConstraints,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MonthCalendarView(
                        events = calendarEvents,
                        selectedDate = today,
                        pickedDate = calendarPickedDate,
                        onPickedDateChange = { calendarPickedEpochDay = it.toEpochDay() },
                        onEventClick = { navigateToDetail(it) },
                        onEventLongClick = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                key(homeDisplayMode) {
                    val dragEnabled = canStartHomeReorder(sortType, pendingLocalReorder) &&
                        reorderableCount > 1
                    val latestDragEnabled by rememberUpdatedState(dragEnabled)
                    val latestDisplayedIds by rememberUpdatedState(
                        displayedList.map { it.event.id }
                    )
                    val latestDisplayedItems by rememberUpdatedState(displayedList)
                    val latestPinnedEventIds by rememberUpdatedState(pinnedEventIds)
                    val latestSortType by rememberUpdatedState(sortType)
                    val latestViewModel by rememberUpdatedState(viewModel)
                    var visibleIdsAtDragStart by remember { mutableStateOf<List<Int>?>(null) }
                    val finishReorder: () -> Unit = {
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
                    val reorderState = rememberReorderableLazyListState(
                        canDragOver = { draggedOver, dragging ->
                            val fromId = dragging.key as? Int
                            val toId = draggedOver.key as? Int
                            latestDragEnabled && fromId != null && toId != null &&
                                homeReorderAllowed(fromId, toId, latestPinnedEventIds)
                        },
                        onMove = { from, to ->
                            if (latestDragEnabled && pendingLocalReorder == null) {
                                if (visibleIdsAtDragStart == null) {
                                    visibleIdsAtDragStart = latestDisplayedIds
                                }
                                dragInProgress = true
                                // Resolve by key: the optional nearest banner occupies a list slot.
                                val fromIdx = orderedList.indexOfFirst { it.event.id == from.key }
                                val toIdx = orderedList.indexOfFirst { it.event.id == to.key }
                                if (fromIdx in orderedList.indices && toIdx in orderedList.indices &&
                                    homeReorderAllowed(orderedList[fromIdx].event.id, orderedList[toIdx].event.id, latestPinnedEventIds)) {
                                    val item = orderedList.removeAt(fromIdx)
                                    orderedList.add(toIdx, item)
                                }
                            }
                        },
                        onDragEnd = { _, _ -> finishReorder() }
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
                            EmptyState(
                                kind = emptyStateKind,
                                onFirstEventClick = navigateToItemEntry,
                                onClearConstraints = clearEmptyStateConstraints,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CompositionLocalProvider(LocalOverscrollFactory provides null) {
                                LazyColumn(
                                    state = reorderState.listState,
                                    // Once long press starts reordering, its pointer must not be
                                    // consumed by normal list scrolling. Programmatic edge scroll remains enabled.
                                    userScrollEnabled = reorderState.draggingItemIndex == null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(if (dragEnabled) Modifier.reorderable(reorderState) else Modifier),
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 12.dp,
                                        bottom = SongDesignTokens.FabClearance.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(if (homeDisplayMode == 0) 12.dp else SongDesignTokens.PaddingList.dp)
                                ) {
                                    nearestEvent?.takeIf { showNearestBanner }?.let { nearest ->
                                        item(key = NEAREST_BANNER_KEY) {
                                            NearestEventBanner(
                                                eventState = nearest,
                                                today = today,
                                                dateFormatter = dateFormatter,
                                                showHours = showHours,
                                                pinned = nearest.event.id in pinnedEventIds,
                                                onClick = { navigateToDetail(nearest.event.id) }
                                            )
                                        }
                                    }
                                    items(orderedList, key = { it.event.id }) { eventState ->
                                        ReorderableItem(reorderState, key = eventState.event.id) { isDragging ->
                                            val eventId = eventState.event.id
                                            val itemDragging = isDragging
                                            val isPinned = eventId in pinnedEventIds
                                            val rowDragEnabled = dragEnabled && !isPinned
                                            val moveUpLabel = stringResource(R.string.home_move_up)
                                            val moveDownLabel = stringResource(R.string.home_move_down)
                                            val reorderingLabel = stringResource(R.string.home_reordering)
                                            fun moveAccessible(direction: Int): Boolean {
                                                if (!latestDragEnabled || dragInProgress || pendingLocalReorder != null) return false
                                                val fromIndex = orderedList.indexOfFirst { it.event.id == eventId }
                                                val toIndex = fromIndex + direction
                                                if (fromIndex !in orderedList.indices || toIndex !in orderedList.indices) return false
                                                if (!homeReorderAllowed(eventId, orderedList[toIndex].event.id, latestPinnedEventIds)) return false
                                                visibleIdsAtDragStart = latestDisplayedIds
                                                val item = orderedList.removeAt(fromIndex)
                                                orderedList.add(toIndex, item)
                                                finishReorder()
                                                return true
                                            }
                                            val reorderAccessibility = if (rowDragEnabled) Modifier.semantics {
                                                if (itemDragging) stateDescription = reorderingLabel
                                                customActions = buildList {
                                                    val index = orderedList.indexOfFirst { it.event.id == eventId }
                                                    if (index > 0 && homeReorderAllowed(eventId, orderedList[index - 1].event.id, pinnedEventIds)) {
                                                        add(CustomAccessibilityAction(moveUpLabel) { moveAccessible(-1) })
                                                    }
                                                    if (index in 0 until orderedList.lastIndex &&
                                                        homeReorderAllowed(eventId, orderedList[index + 1].event.id, pinnedEventIds)) {
                                                        add(CustomAccessibilityAction(moveDownLabel) { moveAccessible(1) })
                                                    }
                                                }
                                            } else Modifier
                                            val haptic = LocalHapticFeedback.current
                                            LaunchedEffect(itemDragging) {
                                                if (itemDragging) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            }
                                            val openMenu: (() -> Unit)? = if (rowDragEnabled) {
                                                null
                                            } else {
                                                {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuEventId = eventId
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .then(
                                                        if (listInitialized) Modifier.animateItem(placementSpec = AnimationSpecs.springItemPlacement)
                                                        else Modifier
                                                    )
                                                    .then(
                                                        if (rowDragEnabled) {
                                                            Modifier.detectReorderAfterLongPress(reorderState)
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                            ) {
                                                val showDetail = homeDensityMode == 1
                                                val persistedMode = perEventDateDeltaModes[eventState.event.id] ?: dateDeltaDisplayMode
                                                var itemDisplayMode by remember(eventState.event.id, persistedMode) {
                                                    mutableStateOf(persistedMode)
                                                }
                                                val toggleDisplayMode: () -> Unit = {
                                                    val availableModes = getAvailableDisplayModes(eventState, showMilestone = showMilestone)
                                                    val currentModeIndex = availableModes.indexOf(itemDisplayMode)
                                                    val nextModeIndex = (if (currentModeIndex != -1) currentModeIndex + 1 else 1) % availableModes.size
                                                    val nextMode = availableModes[nextModeIndex]
                                                    itemDisplayMode = nextMode
                                                    (context.applicationContext as TimeApplication).saveEventDisplayMode(
                                                        eventState.event.id, nextMode, markHomeHintSeen = true
                                                    )
                                                }
                                                if (homeDisplayMode == 0) {
                                                    EventCard(
                                                        eventState = eventState,
                                                        today = today,
                                                        dateFormatter = dateFormatter,
                                                        sortType = sortType,
                                                        dateDeltaDisplayMode = itemDisplayMode,
                                                        onToggleDateDeltaDisplayMode = toggleDisplayMode,
                                                        onClick = { navigateToDetail(eventState.event.id) },
                                                        onLongClick = openMenu,
                                                        showHours = showHours,
                                                        showMilestone = showMilestone,
                                                        showDetail = showDetail,
                                                        isDragging = itemDragging,
                                                        pinned = isPinned,
                                                        selected = eventId == selectedEventId,
                                                        modifier = reorderAccessibility
                                                    )
                                                } else {
                                                    EventListItem(
                                                        eventState = eventState,
                                                        today = today,
                                                        sortType = sortType,
                                                        dateDeltaDisplayMode = itemDisplayMode,
                                                        onToggleDateDeltaDisplayMode = toggleDisplayMode,
                                                        onClick = { navigateToDetail(eventState.event.id) },
                                                        onLongClick = openMenu,
                                                        showHours = showHours,
                                                        showMilestone = showMilestone,
                                                        isDragging = itemDragging,
                                                        pinned = isPinned,
                                                        selected = eventId == selectedEventId,
                                                        modifier = reorderAccessibility
                                                    )
                                                }
                                                HomeEventActionMenu(
                                                    expanded = menuEventId == eventId,
                                                    pinned = isPinned,
                                                    onDismiss = { menuEventId = null },
                                                    onTogglePin = {
                                                        menuEventId = null
                                                        viewModel.togglePinned(eventId)
                                                    },
                                                    onEdit = {
                                                        menuEventId = null
                                                        navigateToEdit(eventId)
                                                    },
                                                    onDelete = {
                                                        menuEventId = null
                                                        pendingDeleteEvent = eventState
                                                    }
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
            if (showOverflowMenu) {
                Popup(
                    alignment = Alignment.TopEnd,
                    onDismissRequest = { showOverflowMenu = false },
                    properties = PopupProperties(focusable = true)
                ) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        HomeToolsPopupBackHandler { showOverflowMenu = false }
                    }
                    HomeOverflowPanel(
                        digest = timelineDigest,
                        selectedBucket = timelineFocus,
                        categoryFilter = categoryFilter,
                        sortType = sortType,
                        onBucketClick = { bucket ->
                            timelineFocus = if (timelineFocus == bucket) null else bucket
                            showOverflowMenu = false
                        },
                        onCategoryClick = { id ->
                            viewModel.updateCategoryFilter(id)
                            showOverflowMenu = false
                        },
                        onSortClick = { type ->
                            viewModel.updateSortType(type)
                            showOverflowMenu = false
                        },
                        modifier = Modifier.padding(top = 8.dp, end = 16.dp)
                    )
                }
            }
        }
    }
}
