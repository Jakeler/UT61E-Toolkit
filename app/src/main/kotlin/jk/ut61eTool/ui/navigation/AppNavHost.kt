package jk.ut61eTool.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import jk.ut61eTool.ui.home.HomeScreen
import jk.ut61eTool.ui.live.LiveScreen
import jk.ut61eTool.ui.logfiles.LogFilesScreen
import jk.ut61eTool.ui.scan.ScanScreen
import jk.ut61eTool.ui.settings.SettingsScreen
import jk.ut61eTool.ui.viewer.ViewerScreen

/**
 * Single navigation graph of the app. Screens take plain lambdas so they stay
 * independent of the navigation library; only this file knows about [Routes].
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onConnect = { navController.navigate(Routes.SCAN) },
                onViewLogs = { navController.navigate(Routes.LOG_FILES) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onResumeSession = { address, name ->
                    navController.navigate(Routes.live(address, name))
                },
            )
        }

        composable(Routes.SCAN) {
            ScanScreen(
                onDeviceSelected = { address, name ->
                    // The scan screen is a step, not a destination to come back to.
                    navController.navigate(Routes.live(address, name)) {
                        popUpTo(Routes.SCAN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.LOG_FILES) {
            LogFilesScreen(
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onFileSelected = { uri -> navController.navigate(Routes.viewer(uri)) },
            )
        }

        composable(
            route = Routes.LIVE,
            arguments = listOf(
                navArgument("address") { type = NavType.StringType },
                navArgument("name") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            // Arguments reach LiveViewModel through its SavedStateHandle.
            LiveScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.VIEWER,
            arguments = listOf(
                navArgument("fileUri") { type = NavType.StringType },
            ),
        ) {
            // Argument reaches ViewerViewModel through its SavedStateHandle.
            ViewerScreen(onBack = { navController.popBackStack() })
        }
    }
}
