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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.timeapk.R
import com.example.timeapk.TimeApplication
import com.example.timeapk.data.Event
import com.example.timeapk.ui.theme.AnimationSpecs

enum class FilterType { All, Upcoming, Past }
enum class SortType { ByDays, ByDate, ByCreated }

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
    var filterType by remember(savedFilter) { mutableStateOf(FilterType.entries.getOrNull(savedFilter) ?: FilterType.All) }
    var sortType by remember(savedSort) { mutableStateOf(SortType.entries.getOrNull(savedSort) ?: SortType.ByDays) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showCardMenu by remember { mutableStateOf<EventUiState?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Event?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(filterType) {
        prefs.setFilterType(filterType.ordinal)
    }
    LaunchedEffect(sortType) {
        prefs.setSortType(sortType.ordinal)
    }

    val displayedList = remember(homeUiState, filterType, sortType) {
        var list = when (filterType) {
            FilterType.All -> homeUiState
            FilterType.Upcoming -> homeUiState.filter { !it.isPast }
            FilterType.Past -> homeUiState.filter { it.isPast }
        }
        list = when (sortType) {
            SortType.ByDays -> list.sortedBy { it.daysRemaining }
            SortType.ByDate -> list.sortedBy { it.event.date }
            SortType.ByCreated -> list.sortedByDescending { it.event.createdAt }
        }
        list
    }

    val deletedSnackbarText = stringResource(R.string.deleted_snackbar)
    val undoLabel = stringResource(R.string.undo)
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_message, showDeleteConfirm!!.title)) },
                confirmButton = {
                    TextButton(onClick = {
                    val toDelete = showDeleteConfirm!!
                    viewModel.deleteEvent(toDelete)
                    showDeleteConfirm = null
                    showCardMenu = null
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = deletedSnackbarText,
                            actionLabel = undoLabel
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restoreEvent(toDelete)
                        }
                    }
                }) {
                    Text(stringResource(R.string.delete_confirm_ok), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null; showCardMenu = null }) {
                    Text(stringResource(R.string.delete_confirm_cancel))
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // 与新建事件页一致
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
                .background(MaterialTheme.colorScheme.background) // 使用定义好的复古背景色，移除渐变
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chip 位于 background 上 → 使用 onBackground 色系
                // 选中态：实心 primary (砖红) + 白字 → 高对比
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
            }
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
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            EventCard(
                                eventState = eventState,
                                onClick = { navigateToDetail(eventState.event.id) },
                                onLongClick = { showCardMenu = eventState },
                                showHours = showHours
                            )
                        }
                    }
                }
            }
            }
        }
    }

    showCardMenu?.let { state ->
        AlertDialog(
            onDismissRequest = { showCardMenu = null },
            title = { Text(state.event.title) },
            text = {
                Column {
                    Text(state.event.category, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = {
                        showCardMenu = null
                        showDeleteConfirm = state.event
                    }) {
                        Text(stringResource(R.string.button_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCardMenu = null
                    navigateToEdit(state.event.id)
                }) { Text(stringResource(R.string.button_edit)) }
            },
            dismissButton = {
                TextButton(onClick = { showCardMenu = null }) {
                    Text(stringResource(R.string.delete_confirm_cancel))
                }
            }
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
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showHours: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.98f else 1f,
        animationSpec = AnimationSpecs.springButton,
        label = "cardScale"
    )
    val cardColor = eventState.event.colorHex?.let { hex ->
        try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) {
            MaterialTheme.colorScheme.primary
        }
    } ?: MaterialTheme.colorScheme.primary

    val isPast = eventState.isPast
    // 票据/胶片样式：直角或微圆角 (4dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = null
            ),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            // 过期事件：用 lerp 混合生成实心减淡色（alpha 半透明 + 白字半透明 = 对比度双重衰减）
            containerColor = if (isPast)
                lerp(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.background, 0.15f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        // 票据描边：使用主题 outline (半透明次色)
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f) // 略微提升边框可见度
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // 左侧颜色标识条：用事件自选色做视觉锚点
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(cardColor.copy(alpha = if (isPast) 0.4f else 0.7f))
            )
            // 内容区域
            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isPast) 0.02f else 0.06f),
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.0f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = eventState.event.title,
                            style = MaterialTheme.typography.titleLarge,
                            // 卡片背景 = surfaceVariant → 文字必须用 onSurfaceVariant
                            color = if (isPast) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = eventState.event.category,
                            style = MaterialTheme.typography.labelMedium, // 使用标签样式
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isPast) 0.72f else 0.7f)
                        )
                        // 具体日期展示
                        run {
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            val dateStr = Instant.ofEpochMilli(eventState.event.date)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .format(formatter)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        if (eventState.event.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = eventState.event.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    // 右侧天数区 (已正确使用 onSurfaceVariant)
                    Column(
                        modifier = Modifier
                            .padding(start = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${eventState.daysRemaining}",
                            style = MaterialTheme.typography.displayMedium,
                            color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isPast) stringResource(R.string.days_past_label)
                            else stringResource(R.string.days_left_label),
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.sp,
                            color = if (isPast) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            }
                        )
                        if (showHours && !isPast && eventState.hoursRemaining > 0L) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.hours_remaining, eventState.hoursRemaining.toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
