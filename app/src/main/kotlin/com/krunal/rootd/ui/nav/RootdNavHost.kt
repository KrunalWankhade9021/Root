package com.krunal.rootd.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.krunal.rootd.ui.catalog.AllowListEditScreen
import com.krunal.rootd.ui.catalog.AllowListsScreen
import com.krunal.rootd.ui.catalog.AlwaysAllowedScreen
import com.krunal.rootd.ui.catalog.TagsScreen
import com.krunal.rootd.ui.dashboard.DashboardScreen
import com.krunal.rootd.ui.focus.FocusingScreen
import com.krunal.rootd.ui.focus.SessionResultScreen
import com.krunal.rootd.ui.focus.StartScreen
import com.krunal.rootd.ui.history.HistoryScreen
import com.krunal.rootd.ui.history.SessionDetailScreen
import com.krunal.rootd.ui.insights.WeeklyInsightsScreen
import com.krunal.rootd.ui.onboarding.OnboardingScreen
import com.krunal.rootd.ui.settings.SettingsScreen
import com.krunal.rootd.ui.shop.ShopScreen

private data class Tab(val route: Route, val label: String, val icon: ImageVector)

private fun tabFor(route: Route): Tab = when (route) {
    Route.Start -> Tab(route, "Focus", Icons.Filled.Park)
    Route.Dashboard -> Tab(route, "Dashboard", Icons.Filled.BarChart)
    Route.Shop -> Tab(route, "Shop", Icons.Filled.Storefront)
    Route.Settings -> Tab(route, "Settings", Icons.Filled.Settings)
    else -> error("$route is not a bottom tab")
}

/** App shell: bottom navigation (Start · Dashboard · Shop · Settings) over the full route graph. */
@Composable
fun RootdNavHost(navController: NavHostController = rememberNavController()) {
    val navigate: (Route) -> Unit = { route -> navController.navigate(route.path) }
    val backStack by navController.currentBackStackEntryAsState()
    val currentPath = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Route.tabs.map(::tabFor).forEach { tab ->
                    NavigationBarItem(
                        selected = currentPath == tab.route.path,
                        onClick = {
                            navController.navigate(tab.route.path) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Start.path,
            modifier = Modifier.padding(padding),
        ) {
            composable(Route.Onboarding.path) { OnboardingScreen(onNavigate = navigate) }
            composable(Route.Start.path) { StartScreen(onNavigate = navigate) }
            composable(Route.Focusing.path) { FocusingScreen(onNavigate = navigate) }
            composable(
                Route.SessionResult.PATTERN,
                arguments = listOf(navArgument(Route.ARG_SESSION_ID) { type = NavType.LongType }),
            ) { entry ->
                SessionResultScreen(sessionId = entry.arguments!!.getLong(Route.ARG_SESSION_ID), onNavigate = navigate)
            }
            composable(Route.Dashboard.path) { DashboardScreen(onNavigate = navigate) }
            composable(
                Route.SessionDetail.PATTERN,
                arguments = listOf(navArgument(Route.ARG_SESSION_ID) { type = NavType.LongType }),
            ) { entry ->
                SessionDetailScreen(sessionId = entry.arguments!!.getLong(Route.ARG_SESSION_ID), onNavigate = navigate)
            }
            composable(Route.History.path) { HistoryScreen(onNavigate = navigate) }
            composable(Route.Shop.path) { ShopScreen(onNavigate = navigate) }
            composable(Route.Tags.path) { TagsScreen(onNavigate = navigate) }
            composable(Route.AllowLists.path) { AllowListsScreen(onNavigate = navigate) }
            composable(
                Route.AllowListEdit.PATTERN,
                arguments = listOf(navArgument(Route.ARG_ID) { type = NavType.LongType }),
            ) { entry ->
                AllowListEditScreen(id = entry.arguments!!.getLong(Route.ARG_ID), onNavigate = navigate)
            }
            composable(Route.AlwaysAllowed.path) { AlwaysAllowedScreen(onNavigate = navigate) }
            composable(Route.Settings.path) { SettingsScreen(onNavigate = navigate) }
            composable(Route.WeeklyInsights.path) { WeeklyInsightsScreen(onNavigate = navigate) }
        }
    }
}
