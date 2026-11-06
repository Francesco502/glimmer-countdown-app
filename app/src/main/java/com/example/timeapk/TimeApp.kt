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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.timeapk.data.Event
import com.example.timeapk.ui.AppViewModelProvider
import com.example.timeapk.ui.common.LocalCustomCategories
import com.example.timeapk.ui.common.WindowWidthClass
import com.example.timeapk.ui.common.windowWidthClassFor
import com.example.timeapk.ui.detail.DetailScreen
import com.example.timeapk.ui.event.EventEntryScreen
import com.example.timeapk.ui.home.DeleteEventResult
import com.example.timeapk.ui.home.HomeScreen
import com.example.timeapk.ui.home.HomeViewModel
import com.example.timeapk.ui.settings.SettingsScreen
import com.example.timeapk.ui.theme.AnimationSpecs
import com.example.timeapk.ui.theme.SongDesignTokens
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
    // Wide screens keep the list/detail side-by-side selection. Compact screens use the Detail
    // navigation destination instead, so the system back stack owns the detail page.
    var selectedEventIdForDetail by rememberSaveable { mutableStateOf<Int?>(null) }
    val app = LocalContext.current.applicationContext as TimeApplication
    val customCategories by app.categoryRepository.getAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val wideScreen = windowWidthClassFor(screenWidthDp) == WindowWidthClass.Expanded

    val homeViewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val allEventStates by homeViewModel.unfilteredCalendarUiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessageTemplate = stringResource(R.string.event_deleted_named)
    val undoLabel = stringResource(R.string.action_undo)
    val calendarCleanupBlockedMessage = stringResource(R.string.calendar_cleanup_blocked)

    // Shared by the home long-press menu and the detail destination: delete, then offer undo.
    // The snackbar lives above the NavHost, so undo survives navigating back to the list.
    val deleteWithUndo: suspend (Event) -> Boolean = { deletedEvent ->
        when (homeViewModel.deleteEvent(deletedEvent)) {
            DeleteEventResult.Deleted -> {
                if (selectedEventIdForDetail == deletedEvent.id) selectedEventIdForDetail = null
                scope.launch {
                    var restoring = false
                    try {
                        val result = snackbarHostState.showSnackbar(
                            message = String.format(deletedMessageTemplate, deletedEvent.title),
                            actionLabel = undoLabel,
                            withDismissAction = true,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            restoring = true
                            homeViewModel.restoreEvent(deletedEvent)
                        }
                    } finally {
                        if (!restoring) homeViewModel.finishDeletionUndo(deletedEvent)
                    }
                }
                true
            }

            is DeleteEventResult.Blocked -> {
                scope.launch { snackbarHostState.showSnackbar(calendarCleanupBlockedMessage) }
                false
            }
        }
    }

    LaunchedEffect(initialOpenEventId) {
        initialOpenEventId?.let { id ->
            navController.navigate(Routes.Home) {
                popUpTo(Routes.Home) { inclusive = false }
                launchSingleTop = true
            }
            if (wideScreen) {
                selectedEventIdForDetail = id
            } else {
                navController.navigate(Routes.detail(id)) { launchSingleTop = true }
            }
            onOpenEventIdConsumed()
        }
    }
    val startDestination = Routes.Home
    CompositionLocalProvider(LocalCustomCategories provides customCategories) {
        Box(Modifier.fillMaxSize()) {
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
                    // Wide screens still show detail as a pane inside Home; back clears it first.
                    BackHandler(enabled = selectedEventIdForDetail != null) {
                        selectedEventIdForDetail = null
                    }

                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val containerWidth = maxWidth
                        val twoPane = windowWidthClassFor(containerWidth) == WindowWidthClass.Expanded
                        val homeContent: @Composable (Modifier) -> Unit = { homeModifier ->
                            HomeScreen(
                                navigateToItemEntry = { navController.navigate(Routes.Add) },
                                navigateToDetail = { id ->
                                    if (twoPane) {
                                        selectedEventIdForDetail = id
                                    } else {
                                        navController.navigate(Routes.detail(id)) { launchSingleTop = true }
                                    }
                                },
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
                        if (twoPane) {
                            // 宽屏：左侧列表、右侧详情；未选中时右侧显示提示。
                            Row(Modifier.fillMaxSize()) {
                                homeContent(Modifier.width(containerWidth * 0.42f).fillMaxHeight())
                                Box(
                                    Modifier
                                        .width(SongDesignTokens.BorderWidth.dp)
                                        .fillMaxHeight()
                                        .background(
                                            MaterialTheme.colorScheme.outline.copy(
                                                alpha = SongDesignTokens.BorderAlphaSoft
                                            )
                                        )
                                )
                                val detailId = selectedEventIdForDetail
                                Box(Modifier.weight(1f).fillMaxHeight()) {
                                    if (detailId != null) {
                                        DetailPane(
                                            eventId = detailId,
                                            allEventStates = allEventStates,
                                            allEvents = { app.repository.getEventFlow(detailId) },
                                            onNavigateBack = { selectedEventIdForDetail = null },
                                            onEditClick = {
                                                selectedEventIdForDetail = null
                                                navController.navigate(Routes.edit(detailId)) { launchSingleTop = true }
                                            },
                                            onDeleteClick = { event ->
                                                deleteWithUndo(event)
                                            },
                                            showBackButton = false,
                                            modifier = Modifier.fillMaxSize()
                                        )
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
                        }
                    }
                }
                composable(
                    route = Routes.Detail,
                    arguments = listOf(navArgument("eventId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val eventId = backStackEntry.arguments?.getInt("eventId") ?: 0
                    DetailPane(
                        eventId = eventId,
                        allEventStates = allEventStates,
                        allEvents = { app.repository.getEventFlow(eventId) },
                        onNavigateBack = { navController.popBackStack() },
                        onEditClick = {
                            navController.navigate(Routes.edit(eventId)) {
                                popUpTo(Routes.Detail) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onDeleteClick = { event -> deleteWithUndo(event) },
                        showBackButton = true,
                        onDeleted = { navController.popBackStack() },
                        modifier = Modifier.fillMaxSize()
                    )
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
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp)
            )
        }
    }
}

/**
 * Detail body shared by the compact navigation destination and the wide-screen pane. It resolves
 * the live event row and the UI state so both layouts show the same data.
 */
@Composable
private fun DetailPane(
    eventId: Int,
    allEventStates: List<com.example.timeapk.ui.home.EventUiState>,
    allEvents: () -> kotlinx.coroutines.flow.Flow<Event?>,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: suspend (Event) -> Boolean,
    showBackButton: Boolean,
    modifier: Modifier = Modifier,
    onDeleted: () -> Unit = {}
) {
    val eventLoadState by produceState(
        initialValue = false to null as Event?,
        key1 = eventId
    ) {
        allEvents().collect { event -> value = true to event }
    }
    val event = eventLoadState.second
    val eventState = allEventStates.firstOrNull { it.event.id == eventId }
    DetailScreen(
        eventState = eventState,
        eventMissing = eventLoadState.first && event == null,
        onNavigateBack = onNavigateBack,
        onEditClick = onEditClick,
        onDeleteClick = {
            val deletedEvent = eventState?.event
            val deleted = if (deletedEvent == null) false else onDeleteClick(deletedEvent)
            if (deleted) onDeleted()
            deleted
        },
        showBackButton = showBackButton,
        modifier = modifier
    )
}
