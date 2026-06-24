package com.example.timeapk

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
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
import com.example.timeapk.ui.detail.DetailScreen
import com.example.timeapk.ui.event.EventEntryScreen
import com.example.timeapk.ui.home.EventUiState
import com.example.timeapk.ui.settings.SettingsScreen
import com.example.timeapk.ui.home.HomeScreen
import com.example.timeapk.ui.home.toEventUiState
import com.example.timeapk.ui.splash.SplashScreen
import com.example.timeapk.data.DEFAULT_MILESTONE_DAYS
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

object Routes {
    const val Splash = "Splash"
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

    LaunchedEffect(initialOpenEventId) {
        initialOpenEventId?.let { id ->
            selectedEventIdForDetail = id
            onOpenEventIdConsumed()
        }
    }
    val startDestination = remember { if (initialOpenEventId != null) Routes.Home else Routes.Splash }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = AnimationSpecs.mediumTween()) +
                slideInVertically(animationSpec = AnimationSpecs.mediumTweenIntOffset()) { it / 4 }
        },
        exitTransition = {
            fadeOut(animationSpec = AnimationSpecs.mediumTween()) +
                slideOutVertically(animationSpec = AnimationSpecs.mediumTweenIntOffset()) { -it / 4 }
        },
        popEnterTransition = {
            fadeIn(animationSpec = AnimationSpecs.mediumTween()) +
                slideInVertically(animationSpec = AnimationSpecs.mediumTweenIntOffset()) { it / 4 }
        },
        popExitTransition = {
            fadeOut(animationSpec = AnimationSpecs.mediumTween()) +
                slideOutVertically(animationSpec = AnimationSpecs.mediumTweenIntOffset()) { -it / 4 }
        }
    ) {
        composable(Routes.Splash) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Home) {
            // 当详情覆盖层打开时，优先拦截系统返回键用于关闭详情，而不是退出 Home
            BackHandler(enabled = selectedEventIdForDetail != null) {
                if (selectedEventIdForDetail != null) {
                    selectedEventIdForDetail = null
                }
            }

            val homeSnackbarHostState = remember { SnackbarHostState() }
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val deletedMessage = stringResource(R.string.event_deleted)
            val undoLabel = stringResource(R.string.action_undo)

            Box(Modifier.fillMaxSize()) {
                HomeScreen(
                    navigateToItemEntry = { navController.navigate(Routes.Add) },
                    navigateToDetail = { id -> selectedEventIdForDetail = id },
                    navigateToEdit = { id -> navController.navigate(Routes.edit(id)) },
                    navigateToSettings = { navController.navigate(Routes.Settings) }
                )
                if (selectedEventIdForDetail != null) {
                    val eventId = selectedEventIdForDetail!!
                    val context = LocalContext.current
                    val app = context.applicationContext as TimeApplication
                    val eventLoadState by androidx.compose.runtime.produceState(
                        initialValue = false to null as com.example.timeapk.data.Event?,
                        key1 = eventId
                    ) {
                        app.repository.getEventFlow(eventId).collect { event ->
                            value = true to event
                        }
                    }
                    val event = eventLoadState.second
                    val milestones by app.userPrefs.customMilestonesFlow.collectAsState(initial = DEFAULT_MILESTONE_DAYS)
                    val eventState = event?.toEventUiState(milestones)
                    val homeViewModel: com.example.timeapk.ui.home.HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
                    DetailScreen(
                        eventState = eventState,
                        eventMissing = eventLoadState.first && eventState == null,
                        onNavigateBack = { selectedEventIdForDetail = null },
                        onEditClick = {
                            selectedEventIdForDetail = null
                            navController.navigate(Routes.edit(eventId)) { launchSingleTop = true }
                        },
                        onDeleteClick = {
                            eventState?.event?.let { deletedEvent ->
                                homeViewModel.deleteEvent(deletedEvent)
                                scope.launch {
                                    val result = homeSnackbarHostState.showSnackbar(
                                        message = deletedMessage,
                                        actionLabel = undoLabel,
                                        withDismissAction = true,
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        homeViewModel.restoreEvent(deletedEvent)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
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
