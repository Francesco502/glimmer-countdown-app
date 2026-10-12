package com.example.timeapk

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.timeapk.ui.theme.AnimationSpecs
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.timeapk.ui.AppViewModelProvider
import com.example.timeapk.ui.common.EventImageStore
import com.example.timeapk.ui.common.LocalCustomCategories
import com.example.timeapk.ui.common.WindowWidthClass
import com.example.timeapk.ui.common.windowWidthClassFor
import com.example.timeapk.ui.detail.DetailScreen
import com.example.timeapk.ui.event.EventEntryScreen
import com.example.timeapk.ui.home.EventUiState
import com.example.timeapk.ui.home.DeleteEventResult
import com.example.timeapk.ui.settings.SettingsScreen
import com.example.timeapk.ui.home.HomeScreen
import com.example.timeapk.ui.theme.SongDesignTokens
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

object Routes {
    const val Home = "Home"
    const val Add = "Add"
    const val Edit = "Edit/{eventId}"
    const val Detail = "Detail/{eventId}"
    const val Settings = "Settings"

    fun edit(eventId: Int) = "Edit/$eventId"
    fun detail(eventId: Int) = "Detail/$eventId"
}

@Composable
fun TimeApp(
    navController: NavHostController = rememberNavController(),
    initialOpenEventId: Int? = null,
    onOpenEventIdConsumed: () -> Unit = {}
) {
    // 记忆并在旋转等配置变更后恢复当前详情事件 ID，避免详情层意外消失
    var selectedEventIdForDetail by rememberSaveable { mutableStateOf<Int?>(null) }
    val app = LocalContext.current.applicationContext as TimeApplication
    val customCategories by app.categoryRepository.getAll().collectAsState(initial = emptyList())

    LaunchedEffect(initialOpenEventId) {
        initialOpenEventId?.let { id ->
            selectedEventIdForDetail = id
            navController.navigate(Routes.Home) {
                popUpTo(Routes.Home) { inclusive = false }
                launchSingleTop = true
            }
            onOpenEventIdConsumed()
        }
    }
    LaunchedEffect(Unit) {
        // 撤销窗口早已过去的配图（被删除事件遗留的文件）在启动时清理。
        val referenced = app.repository.getAllEventsSnapshot().mapNotNullTo(mutableSetOf()) { it.imagePath }
        EventImageStore.cleanupOrphans(app, referenced)
    }
    val startDestination = Routes.Home
    CompositionLocalProvider(LocalCustomCategories provides customCategories) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = AnimationSpecs.mistDissolveTween()) +
                slideInVertically(animationSpec = AnimationSpecs.handscrollTweenIntOffset()) { it / 5 }
        },
        exitTransition = {
            fadeOut(animationSpec = AnimationSpecs.mistDissolveTween()) +
                slideOutVertically(animationSpec = AnimationSpecs.handscrollTweenIntOffset()) { -it / 5 }
        },
        popEnterTransition = {
            fadeIn(animationSpec = AnimationSpecs.mistDissolveTween()) +
                slideInVertically(animationSpec = AnimationSpecs.handscrollTweenIntOffset()) { it / 5 }
        },
        popExitTransition = {
            fadeOut(animationSpec = AnimationSpecs.mistDissolveTween()) +
                slideOutVertically(animationSpec = AnimationSpecs.handscrollTweenIntOffset()) { -it / 5 }
        }
    ) {
        composable(Routes.Home) {
            // 当详情覆盖层打开时，优先拦截系统返回键用于关闭详情，而不是退出 Home
            BackHandler(enabled = selectedEventIdForDetail != null) {
                if (selectedEventIdForDetail != null) {
                    selectedEventIdForDetail = null
                }
            }

            val homeSnackbarHostState = remember { SnackbarHostState() }
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val deletedMessageTemplate = stringResource(R.string.event_deleted_named)
            val undoLabel = stringResource(R.string.action_undo)
            val calendarCleanupBlockedMessage = stringResource(R.string.calendar_cleanup_blocked)
            val homeViewModel: com.example.timeapk.ui.home.HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val allEventStates by homeViewModel.unfilteredCalendarUiState.collectAsState()

            // Shared by the detail page and the home long-press menu: delete, then offer undo.
            val deleteWithUndo: suspend (com.example.timeapk.data.Event) -> Boolean = { deletedEvent ->
                when (homeViewModel.deleteEvent(deletedEvent)) {
                    DeleteEventResult.Deleted -> {
                        if (selectedEventIdForDetail == deletedEvent.id) selectedEventIdForDetail = null
                        scope.launch {
                            val result = homeSnackbarHostState.showSnackbar(
                                message = String.format(deletedMessageTemplate, deletedEvent.title),
                                actionLabel = undoLabel,
                                withDismissAction = true,
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                homeViewModel.restoreEvent(deletedEvent)
                            }
                        }
                        true
                    }

                    is DeleteEventResult.Blocked -> {
                        scope.launch { homeSnackbarHostState.showSnackbar(calendarCleanupBlockedMessage) }
                        false
                    }
                }
            }

            BoxWithConstraints(Modifier.fillMaxSize()) {
                val containerWidth = maxWidth
                val widthClass = windowWidthClassFor(containerWidth)
                val twoPane = widthClass == WindowWidthClass.Expanded
                val homeContent: @Composable (Modifier) -> Unit = { homeModifier ->
                    HomeScreen(
                        navigateToItemEntry = { navController.navigate(Routes.Add) },
                        navigateToDetail = { id -> selectedEventIdForDetail = id },
                        navigateToEdit = { id -> navController.navigate(Routes.edit(id)) },
                        navigateToSettings = { navController.navigate(Routes.Settings) },
                        navigateToItemEntryOnDate = { millis ->
                            app.initialDateForAdd = millis
                            navController.navigate(Routes.Add)
                        },
                        onDeleteEvent = { state -> deleteWithUndo(state.event) },
                        selectedEventId = if (twoPane) selectedEventIdForDetail else null,
                        modifier = homeModifier,
                        viewModel = homeViewModel
                    )
                }
                val detailContent: @Composable (Int, Modifier) -> Unit = { eventId, detailModifier ->
                    val eventLoadState by androidx.compose.runtime.produceState(
                        initialValue = false to null as com.example.timeapk.data.Event?,
                        key1 = eventId
                    ) {
                        app.repository.getEventFlow(eventId).collect { event ->
                            value = true to event
                        }
                    }
                    val event = eventLoadState.second
                    val eventState = allEventStates.firstOrNull { it.event.id == eventId }
                    DetailScreen(
                        eventState = eventState,
                        eventMissing = eventLoadState.first && event == null,
                        onNavigateBack = { selectedEventIdForDetail = null },
                        onEditClick = {
                            if (!twoPane) selectedEventIdForDetail = null
                            navController.navigate(Routes.edit(eventId)) { launchSingleTop = true }
                        },
                        onDeleteClick = {
                            val deletedEvent = eventState?.event
                            if (deletedEvent == null) false else deleteWithUndo(deletedEvent)
                        },
                        showBackButton = !twoPane,
                        modifier = detailModifier
                    )
                }

                if (twoPane) {
                    // 宽屏：左侧列表、右侧详情；未选中时右侧显示提示。
                    Row(Modifier.fillMaxSize()) {
                        homeContent(Modifier.width(containerWidth * 0.42f).fillMaxHeight())
                        Box(
                            Modifier
                                .width(SongDesignTokens.BorderWidth.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = SongDesignTokens.BorderAlphaSoft))
                        )
                        val detailId = selectedEventIdForDetail
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            if (detailId != null) {
                                detailContent(detailId, Modifier.fillMaxSize())
                            } else {
                                Text(
                                    text = stringResource(R.string.two_pane_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                                )
                            }
                        }
                    }
                } else {
                    homeContent(Modifier)
                    selectedEventIdForDetail?.let { eventId ->
                        detailContent(eventId, Modifier.fillMaxSize())
                    }
                }
                SnackbarHost(
                    hostState = homeSnackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 88.dp)
                )
            }
        }
        composable(Routes.Settings) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.Add) {
            EventEntryScreen(
                eventId = null,
                navigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.Edit,
            arguments = listOf(navArgument("eventId") { type = NavType.IntType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getInt("eventId") ?: 0
            EventEntryScreen(
                eventId = eventId,
                navigateBack = { navController.popBackStack() }
            )
        }
    }
    }
}
