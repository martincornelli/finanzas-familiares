package com.finanzasfamiliares

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.saveable.rememberSaveable
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
    val sharedMonth: SharedMonthViewModel = hiltViewModel()
    var currentRoute by rememberSaveable { mutableStateOf(Screen.Summary.route) }

    LaunchedEffect(Unit) {
        sharedMonth.resetToCurrentMonth()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
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
                        selected = currentRoute == screen.route,
                        onClick = {
                            currentRoute = screen.route
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentRoute) {
                Screen.Summary.route -> SummaryScreen(onYearMonthChange = sharedMonth::set)
                Screen.Expenses.route -> ExpensesRoute(sharedMonth = sharedMonth)
                Screen.Tithe.route -> DonationsRoute(sharedMonth = sharedMonth)
                Screen.Savings.route -> SavingsRoute(sharedMonth = sharedMonth)
                Screen.Analysis.route -> AnalysisRoute(sharedMonth = sharedMonth)
                Screen.Config.route -> ConfigScreen()
                else -> SummaryScreen(onYearMonthChange = sharedMonth::set)
            }
        }
    }
}

@Composable
private fun ExpensesRoute(sharedMonth: SharedMonthViewModel) {
    val currentYearMonth by sharedMonth.yearMonth.collectAsState()
    ExpensesScreen(yearMonth = currentYearMonth)
}

@Composable
private fun DonationsRoute(sharedMonth: SharedMonthViewModel) {
    val currentYearMonth by sharedMonth.yearMonth.collectAsState()
    DonationsScreen(yearMonth = currentYearMonth)
}

@Composable
private fun SavingsRoute(sharedMonth: SharedMonthViewModel) {
    val currentYearMonth by sharedMonth.yearMonth.collectAsState()
    SavingsScreen(yearMonth = currentYearMonth)
}

@Composable
private fun AnalysisRoute(sharedMonth: SharedMonthViewModel) {
    val currentYearMonth by sharedMonth.yearMonth.collectAsState()
    AnalysisScreen(yearMonth = currentYearMonth)
}
