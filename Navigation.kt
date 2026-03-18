// ─────────────────────────────────────────────
// Navigation.kt
// ─────────────────────────────────────────────
package com.studypulse.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.studypulse.app.ui.screens.dashboard.DashboardScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Timer : Screen("timer")
    object Subjects : Screen("subjects")
    object Goals : Screen("goals")
    object Flashcards : Screen("flashcards")
    object Analytics : Screen("analytics")
    object Achievements : Screen("achievements")
    object Settings : Screen("settings")
}

@Composable
fun StudyPulseNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(Screen.Timer.route) {
            // TimerScreen() — build next
        }
        composable(Screen.Subjects.route) {
            // SubjectsScreen()
        }
        composable(Screen.Goals.route) {
            // GoalsScreen()
        }
        composable(Screen.Flashcards.route) {
            // FlashcardsScreen()
        }
        composable(Screen.Analytics.route) {
            // AnalyticsScreen()
        }
        composable(Screen.Achievements.route) {
            // AchievementsScreen()
        }
        composable(Screen.Settings.route) {
            // SettingsScreen()
        }
    }
}
