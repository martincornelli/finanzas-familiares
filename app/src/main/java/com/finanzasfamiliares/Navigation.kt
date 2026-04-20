package com.finanzasfamiliares

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.finanzasfamiliares.ui.SharedMonthViewModel
import com.finanzasfamiliares.ui.screens.analysis.AnalysisScreen
import com.finanzasfamiliares.ui.screens.config.ConfigScreen
import com.finanzasfamiliares.ui.screens.expenses.ExpensesScreen
import com.finanzasfamiliares.ui.screens.savings.SavingsScreen
import com.finanzasfamiliares.ui.screens.summary.SummaryScreen
import com.finanzasfamiliares.ui.screens.tithe.DonationsScreen

sealed class Screen(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    object Summary  : Screen("summary",  R.string.nav_summary,  Icons.Default.Home)
    object Expenses : Screen("expenses", R.string.nav_expenses, Icons.Default.Receipt)
    object Tithe    : Screen("tithe",    R.string.nav_donations,    Icons.Default.VolunteerActivism)
    object Savings  : Screen("savings",  R.string.nav_savings,  Icons.Default.Savings)
    object Analysis : Screen("analysis", R.string.nav_analysis, Icons.Default.Insights)
    object Config   : Screen("config",   R.string.nav_config,   Icons.Default.Settings)
}

val bottomNavItems = listOf(Screen.Summary, Screen.Expenses, Screen.Tithe, Screen.Savings, Screen.Analysis, Screen.Config)

@Composable
fun FinanzasApp() {
    val navController = rememberNavController()
    val sharedMonth: SharedMonthViewModel = hiltViewModel()
    val currentYearMonth by sharedMonth.yearMonth.collectAsState()

    LaunchedEffect(Unit) {
        sharedMonth.resetToCurrentMonth()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = null
                            )
                        },
                        label = null,
                        alwaysShowLabel = false,
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, Screen.Summary.route, Modifier.padding(innerPadding)) {
            composable(Screen.Summary.route)  { SummaryScreen(onYearMonthChange = sharedMonth::set) }
            composable(Screen.Expenses.route) { ExpensesScreen(yearMonth = currentYearMonth) }
            composable(Screen.Tithe.route)    { DonationsScreen(yearMonth = currentYearMonth) }
            composable(Screen.Savings.route)  { SavingsScreen(yearMonth = currentYearMonth) }
            composable(Screen.Analysis.route) { AnalysisScreen(yearMonth = currentYearMonth) }
            composable(Screen.Config.route)   { ConfigScreen() }
        }
    }
}
