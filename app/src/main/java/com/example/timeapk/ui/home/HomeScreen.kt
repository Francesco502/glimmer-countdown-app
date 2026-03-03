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
import com.example.timeapk.ui.utils.DisplayModes
import com.example.timeapk.ui.utils.getAvailableDisplayModes
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.Event
import com.example.timeapk.data.CATEGORY_ANNIVERSARY
import com.example.timeapk.data.CATEGORY_BIRTHDAY
import com.example.timeapk.data.CATEGORY_OTHER
import com.example.timeapk.ui.theme.AnimationSpecs

import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.OverscrollConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import org.burnoutcrew.reorderable.ReorderableItem

enum class FilterType { All, Birthday, Anniversary, Other }
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
    val dateDeltaDisplayMode by prefs.dateDeltaDisplayModeFlow.collectAsState(initial = 0)
    val perEventDateDeltaModes by prefs.perEventDateDeltaDisplayModesFlow.collectAsState(initial = emptyMap())
    val customEventOrderIds by prefs.customEventOrderFlow.collectAsState(initial = emptyList())
    val pinnedEventIds by prefs.pinnedEventIdsFlow.collectAsState(initial = emptyList())
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

    val displayedList = remember(homeUiState, filterType, sortType, homeDisplayMode, customEventOrderIds, pinnedEventIds) {
        var list = when (filterType) {
            FilterType.All -> homeUiState
            FilterType.Birthday -> homeUiState.filter { it.event.category == CATEGORY_BIRTHDAY }
            FilterType.Anniversary -> homeUiState.filter { it.event.category == CATEGORY_ANNIVERSARY }
            FilterType.Other -> homeUiState.filter { it.event.category == CATEGORY_OTHER }
        }
        val effectiveSort = sortType
        list = when (effectiveSort) {
            SortType.ByDays -> list.sortedBy { it.daysRemaining }
            SortType.ByDate -> list.sortedBy { it.event.date }
            SortType.ByCreated -> list.sortedByDescending { it.event.createdAt }
        }
        // 仅在选择“按创建时间”时应用拖拽产生的自定义顺序；
        // 其余排序模式下，显式排序优先生效，避免看起来“排序按钮没反应”。
        if (customEventOrderIds.isNotEmpty() && sortType == SortType.ByCreated) {
            list = list.sortedBy { id ->
                val i = customEventOrderIds.indexOf(id.event.id)
                if (i < 0) Int.MAX_VALUE else i
            }
        }
        // 置顶事件始终排在最前，顺序按 pinnedEventIds
        if (pinnedEventIds.isNotEmpty()) {
            val pinnedSet = pinnedEventIds.toSet()
            val pinned = pinnedEventIds.mapNotNull { id -> list.find { it.event.id == id } }
            val unpinned = list.filter { it.event.id !in pinnedSet }
            list = pinned + unpinned
        }
        list
    }

    // ── 拖拽排序列表 ──
    // 必须在 composition 阶段同步填充，不能用 LaunchedEffect（会延迟一帧导致全部卡片同时飞入）
    val orderedList = remember { mutableStateListOf<EventUiState>() }
    // 标记是否完成过首次初始化，用于跳过首次 animateItemPlacement
    var listInitialized by remember { mutableStateOf(false) }

    // 同步初始化：在 composition 阶段立即填充，确保 LazyColumn 首帧就有数据
    // 不在此处设置 listInitialized，避免从详情返回时首帧触发 animateItemPlacement 产生拖动动画
    if (orderedList.isEmpty() && displayedList.isNotEmpty()) {
        orderedList.addAll(displayedList)
    }

    // 后续数据变化（增删/筛选/排序/数据刷新）在 LaunchedEffect 增量同步
    LaunchedEffect(displayedList) {
        if (orderedList.isEmpty()) return@LaunchedEffect
        val targetIds = displayedList.map { it.event.id }
        val currentIds = orderedList.map { it.event.id }
        if (targetIds == currentIds) {
            // 仅数据更新（如天数变化），就地替换，不触发位移动画
            displayedList.forEachIndexed { i, newItem ->
                if (i < orderedList.size && orderedList[i] != newItem) {
                    orderedList[i] = newItem
                }
            }
        } else {
            // 结构变化（增删/排序变更）：全量替换
            orderedList.clear()
            orderedList.addAll(displayedList)
        }
        listInitialized = true
    }

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            val fromIdx = from.index
            val toIdx = to.index
            if (fromIdx in orderedList.indices && toIdx in orderedList.indices && fromIdx != toIdx) {
                val item = orderedList.removeAt(fromIdx)
                orderedList.add(toIdx, item)
                scope.launch { prefs.setCustomEventOrder(orderedList.map { it.event.id }) }
            }
        }
    )

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
                        onClick = navigateToSettings
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort_menu))
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_by_days)) },
                                onClick = {
                                    sortType = SortType.ByDays
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_by_date)) },
                                onClick = {
                                    sortType = SortType.ByDate
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_by_created)) },
                                onClick = {
                                    sortType = SortType.ByCreated
                                    showSortMenu = false
                                }
                            )
                        }
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
                    selected = filterType == FilterType.Birthday,
                    onClick = { filterType = FilterType.Birthday },
                    label = { Text(stringResource(R.string.category_birthday)) },
                    shape = RoundedCornerShape(4.dp),
                    colors = chipColors
                )
                FilterChip(
                    selected = filterType == FilterType.Anniversary,
                    onClick = { filterType = FilterType.Anniversary },
                    label = { Text(stringResource(R.string.category_anniversary)) },
                    shape = RoundedCornerShape(4.dp),
                    colors = chipColors
                )
                FilterChip(
                    selected = filterType == FilterType.Other,
                    onClick = { filterType = FilterType.Other },
                    label = { Text(stringResource(R.string.category_other)) },
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
                            state = reorderState.listState,
                            modifier = Modifier
                                .reorderable(reorderState)
                                .detectReorderAfterLongPress(reorderState),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(if (homeDisplayMode == 0) 12.dp else 6.dp)
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
                                // 本地 UI 状态：确保点击后立即切换显示，即便偏好存储有延迟
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
    val dragElevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "dragElevation"
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
            displayContent = formatDaysSmart(days, false)
            displayUnit = stringResource(R.string.days_unit)
            labelText = stringResource(R.string.days_past_label)
        }
        DisplayModes.PAST_YMD -> {
            val start = targetLocalDate
            val end = today
            displayContent = formatBetweenAsYMD(start, end)
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
                displayContent = formatDaysSmart(days, false)
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
                displayContent = formatBetweenAsYMD(today, end)
                displayUnit = ""
                labelText = com.example.timeapk.ui.utils.getUntilLabel(androidx.compose.ui.platform.LocalContext.current, eventState)
            }
        }
        DisplayModes.MILESTONE -> {
            displayContent = formatDaysSmart(eventState.nextMilestoneDays ?: 0L, false)
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
            // 整张卡片可点击进入详情，避免只有标题区域有点击效果
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics(mergeDescendants = true) { contentDescription = cardDescription },
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
            
            // 标题与日期区域：不再单独处理点击，交由整卡片的 clickable 统一处理
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

            // 已历/剩余时间区域：仅此处点击切换「xx天」/「x年x月x天」，热区不小于 48dp 便于点击；靠右对齐使「已经/剩余」多卡对齐
            val timeColor = if (isPast) cardContentColor.copy(alpha = 0.85f) else cardContentColor
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = 72.dp)
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
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
    val listDragElevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "listDragElevation"
    )
    val isPast = eventState.isPast
    val targetLocalDate = remember(eventState.event.date) {
        eventDateToLocalDate(eventState.event.date)
    }

    val isRepeating = eventState.event.repeatType != REPEAT_NONE
    val isToday = eventState.daysRemaining == 0L && !eventState.isPast
    val todayLabel = stringResource(R.string.days_today_label)

    val availableModes = getAvailableDisplayModes(eventState, showMilestone = true)
    val modeIndex = availableModes.indexOf(dateDeltaDisplayMode)
    val mode = if (modeIndex != -1) dateDeltaDisplayMode else availableModes.first()

    val labelText: String
    val daysDisplay: String

    when (mode) {
        DisplayModes.PAST_DAYS -> {
            val days = if (isRepeating) eventState.daysPassed else eventState.daysElapsed
            daysDisplay = formatDaysSmart(days, false) + stringResource(R.string.days_unit)
            labelText = stringResource(R.string.days_past_label)
        }
        DisplayModes.PAST_YMD -> {
            val start = targetLocalDate
            val end = today
            daysDisplay = formatBetweenAsYMD(start, end)
            labelText = stringResource(R.string.days_past_label)
        }
        DisplayModes.UNTIL_DAYS -> {
            if (isToday) {
                daysDisplay = todayLabel
                labelText = ""
            } else {
                val days = if (isRepeating) eventState.daysLeft else eventState.daysRemaining
                daysDisplay = formatDaysSmart(days, false) + stringResource(R.string.days_unit)
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
                daysDisplay = formatBetweenAsYMD(today, end)
                labelText = com.example.timeapk.ui.utils.getUntilLabel(androidx.compose.ui.platform.LocalContext.current, eventState)
            }
        }
        DisplayModes.MILESTONE -> {
            daysDisplay = formatDaysSmart(eventState.nextMilestoneDays ?: 0L, false) + stringResource(R.string.days_unit)
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
            .shadow(listDragElevation, RoundedCornerShape(2.dp), spotColor = Color.Black.copy(alpha = 0.2f))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = itemAlpha
            }
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(2.dp)
            )
            .border(
                width = 0.5.dp,
                color = eventColor.copy(alpha = if (isPast) 0.3f else 0.8f),
                shape = RoundedCornerShape(2.dp)
            )
            .padding(horizontal = 24.dp, vertical = 0.dp)
            // 整行列表项可点击进入详情，避免只有文字区域可点
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics(mergeDescendants = true) { contentDescription = itemDescription },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 标题与日期区域：点击行为由整行 Row 统一处理
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
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

        // 已历/剩余时间区域：仅此处点击切换显示模式，热区不小于 48dp
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
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
