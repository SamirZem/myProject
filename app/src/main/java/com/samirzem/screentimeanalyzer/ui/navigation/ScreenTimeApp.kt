package com.samirzem.screentimeanalyzer.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.samirzem.screentimeanalyzer.permission.UsagePermission
import com.samirzem.screentimeanalyzer.ui.appdetail.AppDetailScreen
import com.samirzem.screentimeanalyzer.ui.applist.AppListScreen
import com.samirzem.screentimeanalyzer.ui.dashboard.DashboardScreen
import com.samirzem.screentimeanalyzer.ui.permission.PermissionScreen
import com.samirzem.screentimeanalyzer.ui.trends.TrendsScreen
import com.samirzem.screentimeanalyzer.worker.UsageSnapshotWorker

private object Routes {
    const val DASHBOARD = "dashboard"
    const val APPS = "apps"
    const val TRENDS = "trends"
    const val APP_DETAIL = "app_detail/{packageName}"

    fun appDetail(packageName: String) = "app_detail/$packageName"
}

private data class BottomDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomDestinations = listOf(
    BottomDestination(Routes.DASHBOARD, "Aujourd'hui", Icons.Filled.Home),
    BottomDestination(Routes.APPS, "Applications", Icons.Filled.Apps),
    BottomDestination(Routes.TRENDS, "Tendances", Icons.Filled.TrendingUp),
)

@Composable
fun ScreenTimeApp() {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(UsagePermission.isGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = UsagePermission.isGranted(context)
                permissionGranted = granted
                if (granted) UsageSnapshotWorker.schedule(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!permissionGranted) {
        PermissionScreen(onGrantClick = { context.startActivity(UsagePermission.settingsIntent(context)) })
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            val showBottomBar = bottomDestinations.any { dest ->
                currentRoute?.hierarchy?.any { it.route == dest.route } == true
            }
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { dest ->
                        val selected = currentRoute?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onAppClick = { pkg -> navController.navigate(Routes.appDetail(pkg)) },
                    onSeeAllClick = { navController.navigate(Routes.APPS) },
                )
            }
            composable(Routes.APPS) {
                AppListScreen(onAppClick = { pkg -> navController.navigate(Routes.appDetail(pkg)) })
            }
            composable(Routes.TRENDS) {
                TrendsScreen()
            }
            composable(Routes.APP_DETAIL) { entry ->
                val packageName = entry.arguments?.getString("packageName").orEmpty()
                AppDetailScreen(packageName = packageName, onBack = { navController.popBackStack() })
            }
        }
    }
}
