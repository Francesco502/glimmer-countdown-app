package com.example.timeapk

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import com.example.timeapk.ui.theme.AnimationSpecs
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                    val event by app.repository.getEventFlow(eventId).collectAsState(initial = null)
                    val milestones by app.userPrefs.customMilestonesFlow.collectAsState(initial = DEFAULT_MILESTONE_DAYS)
                    val eventState = event?.toEventUiState(milestones)
                    val homeViewModel: com.example.timeapk.ui.home.HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
                    DetailScreen(
                        eventState = eventState,
                        onNavigateBack = { selectedEventIdForDetail = null },
                        onEditClick = {
                            selectedEventIdForDetail = null
                            navController.navigate(Routes.edit(eventId)) { launchSingleTop = true }
                        },
                        onDeleteClick = { eventState?.event?.let { homeViewModel.deleteEvent(it) } },
                        modifier = Modifier.fillMaxSize()
                    )
                }
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
