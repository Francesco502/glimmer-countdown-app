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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.ui.utils.formatDays
import com.example.timeapk.ui.utils.formatBetweenAsYMD
import com.example.timeapk.ui.utils.formatDaysSmart
import com.example.timeapk.ui.utils.getDisplayDateFormatter
import com.example.timeapk.ui.utils.parseEventColorOrFallback
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.Event
import com.example.timeapk.ui.theme.AnimationSpecs

enum class FilterType { All, Upcoming, Past }
enum class SortType { ByDays, ByDate, ByCreated }

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
    val scope = rememberCoroutineScope()
    val savedFilter by prefs.filterTypeFlow.collectAsState(initial = 0)
    val savedSort by prefs.sortTypeFlow.collectAsState(initial = 0)
    val showHours by prefs.showHoursFlow.collectAsState(initial = true)
    val showMilestone by prefs.showMilestoneFlow.collectAsState(initial = true)
    val homeDensityMode by prefs.homeDensityModeFlow.collectAsState(initial = 1)
    val dateFormatMode by prefs.dateFormatModeFlow.collectAsState(initial = 0)
    val dateFormatter = remember(dateFormatMode) { getDisplayDateFormatter(dateFormatMode) }
    val hasSeenSwipeHint by prefs.hasSeenSwipeHintFlow.collectAsState(initial = false)
    val savedHomeDisplayMode by prefs.homeDisplayModeFlow.collectAsState(initial = 0)
    var homeDisplayMode by remember(savedHomeDisplayMode) { mutableStateOf(savedHomeDisplayMode) }
    var filterType by remember(savedFilter) { mutableStateOf(FilterType.entries.getOrNull(savedFilter) ?: FilterType.All) }
    var sortType by remember(savedSort) { mutableStateOf(SortType.entries.getOrNull(savedSort) ?: SortType.ByDays) }
    var showSortMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(filterType) {
        prefs.setFilterType(filterType.ordinal)
    }
    LaunchedEffect(sortType) {
        prefs.setSortType(sortType.ordinal)
    }
    LaunchedEffect(homeDisplayMode) {
        prefs.setHomeDisplayMode(homeDisplayMode)
    }

    val displayedList = remember(homeUiState, filterType, sortType, homeDisplayMode) {
        var list = when (filterType) {
            FilterType.All -> homeUiState
            FilterType.Upcoming -> homeUiState.filter { !it.isPast }
            FilterType.Past -> homeUiState.filter { it.isPast }
        }
        val effectiveSort = if (homeDisplayMode == 1) SortType.ByDays else sortType
        list = when (effectiveSort) {
            SortType.ByDays -> list.sortedBy { it.daysRemaining }
            SortType.ByDate -> list.sortedBy { it.event.date }
            SortType.ByCreated -> list.sortedByDescending { it.event.createdAt }
        }
        list
    }

    val deletedSnackbarText = stringResource(R.string.deleted_snackbar)
    val undoLabel = stringResource(R.string.undo)
    LaunchedEffect(displayedList.isNotEmpty(), hasSeenSwipeHint) {
        if (displayedList.isNotEmpty() && !hasSeenSwipeHint) {
            snackbarHostState.showSnackbar(context.getString(R.string.hint_swipe_delete))
            scope.launch { prefs.setHasSeenSwipeHint(true) }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 22.sp,
                            lineHeight = 28.sp
                        )
                    )
                },
                actions = {
                    IconButton(
                        onClick = navigateToSettings
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                    IconButton(
                        onClick = { showSortMenu = true }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort_menu))
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_days)) },
                            onClick = { sortType = SortType.ByDays; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_date)) },
                            onClick = { sortType = SortType.ByDate; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_by_created)) },
                            onClick = { sortType = SortType.ByCreated; showSortMenu = false }
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
        floatingActionButton = {
            val isEmpty = displayedList.isEmpty()
            val fabScale by animateFloatAsState(
                if (isEmpty) 1.08f else 1f,
                animationSpec = AnimationSpecs.springButton,
                label = "fabScale"
            )
            FloatingActionButton(
                onClick = navigateToItemEntry,
                modifier = Modifier
                    .shadow(4.dp, shape = RoundedCornerShape(4.dp), clip = false)
                    .graphicsLayer { scaleX = fabScale; scaleY = fabScale },
                shape = RoundedCornerShape(4.dp),
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_add_event)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Filter chips and view mode toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chipColors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    labelColor = MaterialTheme.colorScheme.onBackground
                )
                FilterChip(
                    selected = filterType == FilterType.All,
                    onClick = { filterType = FilterType.All },
                    label = { Text(stringResource(R.string.filter_all)) },
                    shape = RoundedCornerShape(4.dp),
                    colors = chipColors
                )
                FilterChip(
                    selected = filterType == FilterType.Upcoming,
                    onClick = { filterType = FilterType.Upcoming },
                    label = { Text(stringResource(R.string.filter_upcoming)) },
                    shape = RoundedCornerShape(4.dp),
                    colors = chipColors
                )
                FilterChip(
                    selected = filterType == FilterType.Past,
                    onClick = { filterType = FilterType.Past },
                    label = { Text(stringResource(R.string.filter_past)) },
                    shape = RoundedCornerShape(4.dp),
                    colors = chipColors
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { homeDisplayMode = 0 },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ViewModule,
                            contentDescription = stringResource(R.string.display_mode_card),
                            tint = if (homeDisplayMode == 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(
                        onClick = { homeDisplayMode = 1 },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = stringResource(R.string.display_mode_list),
                            tint = if (homeDisplayMode == 1) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Event list
            AnimatedContent(
                targetState = displayedList.isEmpty(),
                transitionSpec = {
                    (fadeIn(animationSpec = AnimationSpecs.mediumTween()) + slideInVertically(animationSpec = AnimationSpecs.mediumTweenIntOffset()) { it / 4 })
                        .togetherWith(
                            fadeOut(animationSpec = AnimationSpecs.mediumTween()) + slideOutVertically(animationSpec = AnimationSpecs.mediumTweenIntOffset()) { -it / 4 }
                        )
                },
                label = "listOrEmpty"
            ) { isEmpty ->
                if (isEmpty) {
                    EmptyState(modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(if (homeDisplayMode == 0) 12.dp else 6.dp)
                    ) {
                        items(displayedList, key = { it.event.id }) { eventState ->
                            val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    val event = eventState.event
                                    viewModel.deleteEvent(event)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = deletedSnackbarText,
                                            actionLabel = undoLabel
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restoreEvent(event)
                                        }
                                    }
                                    true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement(animationSpec = AnimationSpecs.springItemPlacement),
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.error)
                                            .padding(horizontal = 24.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.cd_delete),
                                            tint = MaterialTheme.colorScheme.onError
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        ) {
                            val showDetail = homeDensityMode == 1
                            if (homeDisplayMode == 0) {
                                EventCard(
                                    eventState = eventState,
                                    dateFormatter = dateFormatter,
                                    onClick = { navigateToDetail(eventState.event.id) },
                                    onLongClick = { navigateToEdit(eventState.event.id) },
                                    showHours = showHours,
                                    showMilestone = showMilestone,
                                    showDetail = showDetail
                                )
                            } else {
                                EventListItem(
                                    eventState = eventState,
                                    dateFormatter = dateFormatter,
                                    onClick = { navigateToDetail(eventState.event.id) },
                                    onLongClick = { navigateToEdit(eventState.event.id) },
                                    showHours = false,
                                    showMilestone = false,
                                    showDetail = true
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
    dateFormatter: DateTimeFormatter,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showHours: Boolean = true,
    showMilestone: Boolean = true,
    showDetail: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.98f else 1f,
        animationSpec = AnimationSpecs.springButton,
        label = "cardScale"
    )
    val isPast = eventState.isPast
    val baseCardColor = parseEventColorOrFallback(
        hex = eventState.event.colorHex,
        fallback = MaterialTheme.colorScheme.primary
    )

    // Song Aesthetics: "Juanben" (Silk Scroll) Texture
    // Adjusted transparency to accommodate the lighter, elegant base colors.
    val juanbenTint = if (isPast) 0.5f else 0.85f
    val cardContainerColor = baseCardColor.copy(alpha = juanbenTint)
    
    // Content color: "Jiao Mo" (Burnt Ink) - Darker, richer text to contrast with the deeper background
    val cardContentColor = MaterialTheme.colorScheme.onSurface

    val view = androidx.compose.ui.platform.LocalView.current
    val today = remember { LocalDate.now() }

    // 缓存日期计算
    val targetLocalDate = remember(eventState.event.date) {
        Instant.ofEpochMilli(eventState.event.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    // Logic Migration: Use repeatType instead of category
    val isYearly = eventState.event.repeatType == REPEAT_YEARLY
    
    // Age Mode: Shows "X years Y months" for any yearly event that has started
    val isAgeMode = remember(isPast, isYearly, targetLocalDate, today) {
        !isPast && isYearly && !targetLocalDate.isAfter(today)
    }
    
    // Milestones: Show for all yearly events (as requested by user to cover Anniversaries)
    // "Only Anniversary triggers milestones" -> We treat all Yearly events as Anniversaries for milestone purposes
    val isAnniversary = isYearly

    // Display Mode Logic: 0 = Remaining, 1 = Elapsed(Days), 2 = Elapsed(YMD)
    // Default: AgeMode -> YMD(2), Past -> Days(1), Future -> Remaining(0)
    val initialMode = remember(eventState.event.id, isAgeMode, isPast) {
        if (isAgeMode) 2 else if (isPast) 1 else 0
    }
    var displayMode by remember(eventState.event.id, isAgeMode, isPast) { mutableIntStateOf(initialMode) }
    
    // Cycle through modes: 
    // If AgeMode: 0 -> 2 -> 1 -> 0
    // If Past: 1 -> 0 -> 1 (or 1 -> 2 -> 0 -> 1 if we want YMD for past events too)
    // If Future: 0 -> 1 -> 0
    fun cycleMode() {
        displayMode = when (displayMode) {
            0 -> if (isAgeMode) 2 else 1
            1 -> 0
            2 -> 1
            else -> 0
        }
    }

    // Days Display Logic with Units
    // We separate the number and the unit for styling if needed, or keep them together string
    // Requirement: Always show units (Days, Years/Months/Days)
    val isToday = eventState.daysRemaining == 0L
    val todayLabel = stringResource(R.string.days_today_label)
    val mainDisplayPair = if (isToday) {
        todayLabel to ""
    } else when (displayMode) {
        2 -> {
            formatBetweenAsYMD(targetLocalDate, today) to ""
        }
        1 -> {
            formatDays(eventState.daysPassed) to stringResource(R.string.days_unit)
        }
        else -> {
            formatDays(eventState.daysRemaining) to stringResource(R.string.days_unit)
        }
    }
    
    val displayContent = mainDisplayPair.first
    val displayUnit = mainDisplayPair.second

    val cardDescription = buildString {
        append(eventState.event.title)
        append(", ")
        // Add accessibility description based on current mode
        if (isToday) {
            append(todayLabel)
        } else when (displayMode) {
            2, 1 -> {
                append(stringResource(R.string.days_past_label)).append(" ")
                append(displayContent).append(displayUnit)
            }
            else -> {
                append(stringResource(R.string.days_left_label)).append(" ").append(displayContent).append(displayUnit)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp) // 固定高度，确保极度一致的韵律感
            .semantics(mergeDescendants = true) { contentDescription = cardDescription }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                },
                interactionSource = interactionSource,
                indication = null
            ),
        shape = RoundedCornerShape(2.dp), // 极小圆角，模拟纸张折叠
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        border = BorderStroke(
            width = 0.5.dp, // 极细淡墨边框
            color = MaterialTheme.colorScheme.outline
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 24.dp, vertical = 16.dp), // 增加留白，更显疏朗
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color indicator dot - Removed to let the whole card color speak
            // Instead, we use the whole card background as the indicator
            
            // Main content: title, category, date
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween, // 垂直两端对齐
                horizontalAlignment = Alignment.Start
            ) {
                // Title row
                Text(
                    text = eventState.event.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        lineHeight = 28.sp
                    ),
                    // "Nong Mo" (Thick Ink): High opacity for strong contrast against the silk background
                    color = cardContentColor.copy(alpha = if (isPast) 0.6f else 0.95f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Date: "Dan Mo" (Pale Ink)
                Text(
                    text = targetLocalDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodyMedium, // labelMedium -> bodyMedium (larger)
                    color = cardContentColor.copy(alpha = 0.65f) // Slightly increased alpha for better readability
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right side: Days display
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { cycleMode() }
                    ),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                // Days number with unit
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = displayContent,
                        style = MaterialTheme.typography.displaySmall.copy( // 统一使用大字号 displaySmall
                            fontSize = 28.sp, // 统一设定为 28sp，兼顾长短文本
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = if (isPast) cardContentColor.copy(alpha = 0.6f) else baseCardColor,
                        maxLines = 1,
                        overflow = TextOverflow.Visible // 允许溢出，但通常固定高度下不会
                    )
                    
                    if (displayUnit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = displayUnit,
                            style = MaterialTheme.typography.titleSmall, // 单位统一使用小号标题字
                            color = if (isPast) cardContentColor.copy(alpha = 0.5f) else baseCardColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 6.dp) // 统一基线对齐
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Label
                Text(
                    text = if (isToday) ""
                        else when (displayMode) {
                            2, 1 -> stringResource(R.string.days_past_label)
                            else -> stringResource(R.string.days_left_label)
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = cardContentColor.copy(alpha = 0.5f),
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventListItem(
    eventState: EventUiState,
    dateFormatter: DateTimeFormatter,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showHours: Boolean = true,
    showMilestone: Boolean = true,
    showDetail: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.99f else 1f,
        animationSpec = AnimationSpecs.springButton,
        label = "listItemScale"
    )
    val isPast = eventState.isPast
    val listView = androidx.compose.ui.platform.LocalView.current
    val today = LocalDate.now()
    // 缓存日期计算
    val targetLocalDate = remember(eventState.event.date) {
        Instant.ofEpochMilli(eventState.event.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    // Logic Migration: Use repeatType instead of category
    val isYearly = eventState.event.repeatType == REPEAT_YEARLY
    
    // Age Mode: Shows "X years Y months" for any yearly event that has started
    val isAgeMode = remember(isPast, isYearly, targetLocalDate, today) {
        !isPast && isYearly && !targetLocalDate.isAfter(today)
    }
    
    // Milestones: Show for all yearly events (as requested by user to cover Anniversaries)
    // "Only Anniversary triggers milestones" -> We treat all Yearly events as Anniversaries for milestone purposes
    val isAnniversary = isYearly

    // Display Mode Logic: 0 = Remaining, 1 = Elapsed(Days), 2 = Elapsed(YMD)
    val initialMode = remember(eventState.event.id, isAgeMode, isPast) {
        if (isAgeMode) 2 else if (isPast) 1 else 0
    }
    var displayMode by remember(eventState.event.id, isAgeMode, isPast) { mutableIntStateOf(initialMode) }

    fun cycleMode() {
        displayMode = when (displayMode) {
            0 -> if (isAgeMode) 2 else 1
            1 -> 0
            2 -> 1
            else -> 0
        }
    }

    val isToday = eventState.daysRemaining == 0L
    val todayLabel = stringResource(R.string.days_today_label)
    val daysDisplay = if (isToday) todayLabel else when (displayMode) {
        2 -> formatBetweenAsYMD(targetLocalDate, today)
        1 -> formatDays(eventState.daysPassed)
        else -> formatDays(eventState.daysRemaining)
    }

    val itemContentColor = MaterialTheme.colorScheme.onSurface
    val eventColor = parseEventColorOrFallback(
        hex = eventState.event.colorHex,
        fallback = MaterialTheme.colorScheme.primary
    )
    val itemDescription = buildString {
        append(eventState.event.title)
        append(", ")
        if (isToday) {
            append(todayLabel)
        } else when (displayMode) {
            2, 1 -> {
                append(stringResource(R.string.days_past_label)).append(" ")
                append(daysDisplay)
            }
            else -> {
                append(stringResource(R.string.days_left_label)).append(" ").append(daysDisplay)
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp) // 固定高度，保持列表整齐
            .semantics(mergeDescendants = true) { contentDescription = itemDescription }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    listView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                },
                interactionSource = interactionSource,
                indication = null
            )
            .background(
                color = MaterialTheme.colorScheme.surface, // 统一使用 Surface (Paper) 背景
                shape = RoundedCornerShape(2.dp)
            )
            .border(
                width = 0.5.dp, // 极细边框
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(2.dp)
            )
            .padding(horizontal = 16.dp, vertical = 0.dp), // 垂直方向由 Row 的 Arrangement 控制
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    eventColor.copy(alpha = if (isPast) 0.5f else 0.9f),
                    androidx.compose.foundation.shape.CircleShape
                )
        )
        Spacer(modifier = Modifier.width(16.dp))

        // Title and category
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = eventState.event.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                    letterSpacing = 0.5.sp
                ),
                color = itemContentColor.copy(alpha = if (isPast) 0.6f else 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showDetail) {
                Text(
                    text = targetLocalDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = itemContentColor.copy(alpha = 0.4f)
                )
            }
        }

        // Days display
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { cycleMode() }
                ),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = daysDisplay,
                style = if (daysDisplay.length > 5)
                    MaterialTheme.typography.titleMedium
                else
                    MaterialTheme.typography.titleLarge,
                color = if (isPast) itemContentColor.copy(alpha = 0.4f) else eventColor
            )
            Text(
                text = if (isToday) ""
                    else when (displayMode) {
                        2, 1 -> stringResource(R.string.days_past_label)
                        else -> stringResource(R.string.days_left_label)
                    },
                style = MaterialTheme.typography.labelSmall,
                color = itemContentColor.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )
        }
    }
}
