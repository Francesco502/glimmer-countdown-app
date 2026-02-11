package com.example.timeapk

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import com.example.timeapk.ui.theme.AnimationSpecs
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel

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
    LaunchedEffect(initialOpenEventId) {
        initialOpenEventId?.let { id ->
            navController.navigate(Routes.detail(id)) {
                launchSingleTop = true
                popUpTo(Routes.Home) { inclusive = false }
            }
            onOpenEventIdConsumed()
        }
    }
    NavHost(
        navController = navController,
        startDestination = Routes.Home,
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
        composable(Routes.Home) {
            HomeScreen(
                navigateToItemEntry = { navController.navigate(Routes.Add) },
                navigateToDetail = { id -> navController.navigate(Routes.detail(id)) },
                navigateToEdit = { id -> navController.navigate(Routes.edit(id)) },
                navigateToSettings = { navController.navigate(Routes.Settings) }
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
        composable(
            route = Routes.Detail,
            arguments = listOf(navArgument("eventId") { type = NavType.IntType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getInt("eventId") ?: 0
            val context = LocalContext.current
            val repository = (context.applicationContext as TimeApplication).repository
            // 使用 Flow 观察：编辑返回后自动刷新数据
            val event by repository.getEventFlow(eventId).collectAsState(initial = null)
            val eventState = event?.toEventUiState()
            val homeViewModel: com.example.timeapk.ui.home.HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)

            DetailScreen(
                eventState = eventState,
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { navController.navigate(Routes.edit(eventId)) { launchSingleTop = true } },
                onDeleteClick = { eventState?.event?.let { homeViewModel.deleteEvent(it) } }
            )
        }
    }
}
