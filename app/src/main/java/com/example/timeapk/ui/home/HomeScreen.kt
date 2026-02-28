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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.timeapk.data.REPEAT_NONE
import com.example.timeapk.ui.utils.findActivity
import com.example.timeapk.data.REPEAT_YEARLY
import com.example.timeapk.ui.utils.formatDays
import com.example.timeapk.ui.utils.formatBetweenAsYMD
import com.example.timeapk.ui.utils.formatDaysSmart
import com.example.timeapk.ui.utils.getDisplayDateFormatter
import com.example.timeapk.ui.utils.parseEventColorOrFallback
import com.example.timeapk.ui.utils.eventDateToLocalDate
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.Event
import com.example.timeapk.ui.theme.AnimationSpecs

import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.OverscrollConfiguration
import androidx.compose.runtime.CompositionLocalProvider

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
    var today by remember { mutableStateOf(LocalDate.now()) }
    val activity = context.findActivity()
    val lifecycle = (activity as? LifecycleOwner)?.lifecycle
    DisposableEffect(lifecycle) {
        if (lifecycle != null) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) today = LocalDate.now()
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        } else {
            onDispose { }
        }
    }
    val scope = rememberCoroutineScope()
    val savedFilter by prefs.filterTypeFlow.collectAsState(initial = 0)
    val savedSort by prefs.sortTypeFlow.collectAsState(initial = 0)
    val showHours by prefs.showHoursFlow.collectAsState(initial = true)
    val showMilestone by prefs.showMilestoneFlow.collectAsState(initial = true)
    val homeDensityMode by prefs.homeDensityModeFlow.collectAsState(initial = 1)
    val dateFormatMode by prefs.dateFormatModeFlow.collectAsState(initial = 0)
    val dateFormatter = remember(dateFormatMode) { getDisplayDateFormatter(dateFormatMode) }
    val hasSeenSwipeHint by prefs.hasSeenSwipeHintFlow.collectAsState(initial = false)
    val dateDeltaDisplayMode by prefs.dateDeltaDisplayModeFlow.collectAsState(initial = 0)
    val perEventDateDeltaModes by prefs.perEventDateDeltaDisplayModesFlow.collectAsState(initial = emptyMap())
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
                            lineHeight = 28.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Normal
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
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            val isEmpty = displayedList.isEmpty()
            val fabScale by animateFloatAsState(
                if (isEmpty) 1.08f else 1f,
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
                    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
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
                                val cardDisplayMode = perEventDateDeltaModes[eventState.event.id] ?: dateDeltaDisplayMode
                                EventCard(
                                    eventState = eventState,
                                    today = today,
                                    dateFormatter = dateFormatter,
                                    dateDeltaDisplayMode = cardDisplayMode,
                                    onToggleDateDeltaDisplayMode = {
                                        scope.launch {
                                            prefs.setDateDeltaDisplayModeForEvent(eventState.event.id, if (cardDisplayMode == 0) 1 else 0)
                                        }
                                    },
                                    onClick = { navigateToDetail(eventState.event.id) },
                                    onLongClick = { navigateToEdit(eventState.event.id) },
                                    showHours = showHours,
                                    showMilestone = showMilestone,
                                    showDetail = showDetail
                                )
                            } else {
                                val itemDisplayMode = perEventDateDeltaModes[eventState.event.id] ?: dateDeltaDisplayMode
                                EventListItem(
                                    eventState = eventState,
                                    today = today,
                                    dateFormatter = dateFormatter,
                                    dateDeltaDisplayMode = itemDisplayMode,
                                    onToggleDateDeltaDisplayMode = {
                                        scope.launch {
                                            prefs.setDateDeltaDisplayModeForEvent(eventState.event.id, if (itemDisplayMode == 0) 1 else 0)
                                        }
                                    },
                                    onClick = { navigateToDetail(eventState.event.id) },
                                    onLongClick = { navigateToEdit(eventState.event.id) }
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
    modifier: Modifier = Modifier
) {
    val isPast = eventState.isPast
    val baseCardColor = parseEventColorOrFallback(
        hex = eventState.event.colorHex,
        fallback = MaterialTheme.colorScheme.primary
    )

    // 交互动画：在缩放的基础上增加轻微的透明度反馈
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "cardScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = androidx.compose.animation.core.tween(150),
        label = "cardAlpha"
    )

    // 原始首页卡片：使用事件色大面积铺色（降低透明度，加深颜色实感）
    val juanbenTint = if (isPast) 0.85f else 1.0f // 过期颜色微降，保持整体色彩浓郁
    val cardContainerColor = baseCardColor.copy(alpha = juanbenTint)

    // 内容文字颜色：绝对清晰的高对比度宋式用色
    val isLight = cardContainerColor.luminance() > 0.45f
    val cardContentColor = if (isLight) {
        // 浅色底：用极深的焦墨色，带一点点原色倾向
        lerp(Color(0xFF141618), baseCardColor, 0.1f)
    } else {
        // 深色底：用带暖调的宣纸霜白，防刺眼且极其清晰
        lerp(Color(0xFFF9F7F2), baseCardColor, 0.05f)
    }

    val view = androidx.compose.ui.platform.LocalView.current

    val targetLocalDate = remember(eventState.event.date) {
        eventDateToLocalDate(eventState.event.date)
    }

    val isRepeating = eventState.event.repeatType != REPEAT_NONE
    val isYearly = eventState.event.repeatType == REPEAT_YEARLY
    val isAnniversary = isYearly

    val isToday = eventState.daysRemaining == 0L && !eventState.isPast
    val todayLabel = stringResource(R.string.days_today_label)
    val isShowUntil = isRepeating || !isPast
    val labelText = when {
        isToday -> ""
        isAnniversary -> stringResource(R.string.days_past_label)
        isShowUntil -> stringResource(R.string.days_until_label)
        else -> stringResource(R.string.days_past_label)
    }
    var dayCount = if (!isRepeating && isPast) eventState.daysElapsed else eventState.daysRemaining
    // 修正：状态为 0 但目标日期并非今天时，按本地日期重算，修复多卡片误显示 0 天
    if (dayCount == 0L && !isToday) {
        dayCount = if (targetLocalDate.isBefore(today)) ChronoUnit.DAYS.between(targetLocalDate, today)
        else ChronoUnit.DAYS.between(today, targetLocalDate)
    }
    val displayContent: String
    val displayUnit: String
    if (isToday) {
        displayContent = todayLabel
        displayUnit = ""
    } else if (isAnniversary) {
        // 纪念日首页：显示“已经 XX 天”（自缘起日至今的累计天数，用 daysPassed）
        displayContent = formatDaysSmart(eventState.daysPassed, false)
        displayUnit = stringResource(R.string.days_unit)
    } else if (dateDeltaDisplayMode == 0) {
        displayContent = formatDaysSmart(dayCount, false)
        displayUnit = stringResource(R.string.days_unit)
    } else {
        val start = if (!isRepeating && isPast) today.minusDays(dayCount) else today
        val end = if (!isRepeating && isPast) today else today.plusDays(dayCount)
        displayContent = formatBetweenAsYMD(start, end)
        displayUnit = ""
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
            .semantics(mergeDescendants = true) { contentDescription = cardDescription }
            .graphicsLayer { 
                scaleX = scale
                scaleY = scale
                alpha = cardAlpha
            }
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
            color = baseCardColor.copy(alpha = if (isPast) 0.3f else 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(24.dp), // 增加留白，更显疏朗
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color indicator dot - Removed to let the whole card color speak
            // Instead, we use the whole card background as the indicator
            
            // 标题
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween, // 垂直两端对齐
                horizontalAlignment = Alignment.Start
            ) {
                // Title row（略增字号以便阅读）
                Text(
                    text = eventState.event.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal, // 去掉粗体，回归宋式瘦硬
                        letterSpacing = 0.5.sp,
                        lineHeight = 24.sp,
                        fontSize = 18.sp
                    ),
                    color = cardContentColor.copy(alpha = if (isPast) 0.8f else 1.0f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Date: "Dan Mo" (Pale Ink)
                Text(
                    text = targetLocalDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = cardContentColor.copy(alpha = 0.8f)
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
                        onClick = onToggleDateDeltaDisplayMode
                    ),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                // Days number with unit（使用卡片高对比度内容色，确保在深色铺色下依然绝对清晰）
                val timeColor = if (isPast) cardContentColor.copy(alpha = 0.85f) else cardContentColor
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = displayContent,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 24.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                            letterSpacing = (-0.5).sp
                        ),
                        color = timeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (displayUnit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = displayUnit,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = timeColor.copy(alpha = 1.0f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Label（剩余/已经）
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = timeColor.copy(alpha = 0.85f),
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
    today: LocalDate,
    dateFormatter: DateTimeFormatter,
    dateDeltaDisplayMode: Int,
    onToggleDateDeltaDisplayMode: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "listItemScale"
    )
    val itemAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = androidx.compose.animation.core.tween(150),
        label = "listItemAlpha"
    )
    val isPast = eventState.isPast
    val listView = androidx.compose.ui.platform.LocalView.current
    val targetLocalDate = remember(eventState.event.date) {
        eventDateToLocalDate(eventState.event.date)
    }

    val isRepeating = eventState.event.repeatType != REPEAT_NONE
    val isYearly = eventState.event.repeatType == REPEAT_YEARLY
    val isAnniversary = isYearly
    val isToday = eventState.daysRemaining == 0L && !eventState.isPast
    val todayLabel = stringResource(R.string.days_today_label)
    val isShowUntil = isRepeating || !isPast
    val labelText = when {
        isToday -> ""
        isAnniversary -> stringResource(R.string.days_past_label)
        isShowUntil -> stringResource(R.string.days_until_label)
        else -> stringResource(R.string.days_past_label)
    }
    var dayCount = if (!isRepeating && isPast) eventState.daysElapsed else eventState.daysRemaining
    if (dayCount == 0L && !isToday) {
        dayCount = if (targetLocalDate.isBefore(today)) ChronoUnit.DAYS.between(targetLocalDate, today)
        else ChronoUnit.DAYS.between(today, targetLocalDate)
    }
    val daysDisplay = if (isToday) {
        todayLabel
    } else if (isAnniversary || dateDeltaDisplayMode == 0) {
        val dc = if (isAnniversary) eventState.daysPassed else dayCount
        formatDaysSmart(dc, false) + stringResource(R.string.days_unit)
    } else {
        val start = if (!isRepeating && isPast) today.minusDays(dayCount) else today
        val end = if (!isRepeating && isPast) today else today.plusDays(dayCount)
        formatBetweenAsYMD(start, end)
    }

    val itemContentColor = MaterialTheme.colorScheme.onSurface
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .semantics(mergeDescendants = true) { contentDescription = itemDescription }
            .graphicsLayer { 
                scaleX = scale
                scaleY = scale
                alpha = itemAlpha
            }
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
                color = eventColor.copy(alpha = if (isPast) 0.3f else 0.8f),
                shape = RoundedCornerShape(2.dp)
            )
            .padding(horizontal = 24.dp, vertical = 0.dp), // 垂直方向由 Row 的 Arrangement 控制
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title and category
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = eventState.event.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = itemContentColor.copy(alpha = if (isPast) 0.85f else 1.0f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = targetLocalDate.format(dateFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = itemContentColor.copy(alpha = 0.7f)
            )
        }

        // Days display
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleDateDeltaDisplayMode
                ),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            val isLightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
            val displayColor = if (isLightSurface) {
                lerp(eventColor, Color.Black, 0.4f) // 在浅色模式下加深主题色以确保显示清晰
            } else {
                lerp(eventColor, Color.White, 0.4f) // 在深色模式下提亮主题色
            }
            Text(
                text = daysDisplay,
                style = if (daysDisplay.length > 5)
                    MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                else
                    MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = if (isPast) displayColor.copy(alpha = 0.85f) else displayColor
            )
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelSmall,
                color = itemContentColor.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
        }
    }
}
