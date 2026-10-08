package com.twedmark.app.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.twedmark.app.BuildConfig
import com.twedmark.app.feature.main.MainScreen
import com.twedmark.app.feature.settings.crash.CrashLogScreen
import com.twedmark.app.feature.settings.debug.DebugPathsScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Main
    ) {
        composable<Main> {
            MainScreen(
                onNavigateToCrashLog = { navController.navigate(CrashLog) },
                onNavigateToDebugPaths = { navController.navigate(DebugPaths) }
            )
        }

        composable<CrashLog> {
            CrashLogScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        if (BuildConfig.DEBUG) {
            composable<DebugPaths> {
                DebugPathsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}