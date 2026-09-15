package com.samirzem.clashanalyzer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.samirzem.clashanalyzer.ui.calibration.CalibrationScreen
import com.samirzem.clashanalyzer.ui.detail.MatchDetailScreen
import com.samirzem.clashanalyzer.ui.history.MatchListScreen
import com.samirzem.clashanalyzer.ui.home.HomeScreen

private object Routes {
    const val HOME = "home"
    const val CALIBRATION = "calibration"
    const val HISTORY = "history"
    const val DETAIL = "detail/{id}"
    fun detail(id: Long) = "detail/$id"
}

@Composable
fun AppNavHost(onRequestCapture: () -> Unit, onStopCapture: () -> Unit) {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onRequestCapture = onRequestCapture,
                onStopCapture = onStopCapture,
                onOpenCalibration = { navController.navigate(Routes.CALIBRATION) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
            )
        }
        composable(Routes.CALIBRATION) { CalibrationScreen() }
        composable(Routes.HISTORY) { MatchListScreen(onOpenDetail = { id -> navController.navigate(Routes.detail(id)) }) }
        composable(Routes.DETAIL) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: return@composable
            MatchDetailScreen(matchId = id)
        }
    }
}
