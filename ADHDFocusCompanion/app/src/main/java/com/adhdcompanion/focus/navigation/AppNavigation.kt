package com.adhdcompanion.focus.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.adhdcompanion.focus.di.AppContainer
import com.adhdcompanion.focus.ui.components.FocusBottomBar
import com.adhdcompanion.focus.ui.screens.BrainDumpScreen
import com.adhdcompanion.focus.ui.screens.NowBriefingScreen
import com.adhdcompanion.focus.ui.screens.PlannerScreen
import com.adhdcompanion.focus.ui.screens.SettingsScreen
import com.adhdcompanion.focus.ui.screens.StuckScreen

sealed class FocusDestination(val route: String) {
    data object Now : FocusDestination("now")
    data object Stuck : FocusDestination("stuck")
    data object Planner : FocusDestination("planner")
    data object BrainDump : FocusDestination("brain_dump")
    data object Settings : FocusDestination("settings")
}

@Composable
fun AppNavigation(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route

    val currentTab = when (currentRoute) {
        FocusDestination.Stuck.route -> FocusDestination.Stuck
        FocusDestination.Planner.route -> FocusDestination.Planner
        FocusDestination.BrainDump.route -> FocusDestination.BrainDump
        else -> FocusDestination.Now
    }

    Scaffold(
        bottomBar = {
            FocusBottomBar(current = currentTab) { destination ->
                navController.navigate(destination.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = FocusDestination.Now.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(FocusDestination.Now.route) {
                NowBriefingScreen(
                    container = container,
                    onOpenSettings = { navController.navigate(FocusDestination.Settings.route) }
                )
            }
            composable(FocusDestination.Stuck.route) {
                StuckScreen(container = container)
            }
            composable(FocusDestination.Planner.route) {
                PlannerScreen(container = container)
            }
            composable(FocusDestination.BrainDump.route) {
                BrainDumpScreen(container = container)
            }
            composable(FocusDestination.Settings.route) {
                SettingsScreen(container = container, onBack = { navController.popBackStack() })
            }
        }
    }
}
