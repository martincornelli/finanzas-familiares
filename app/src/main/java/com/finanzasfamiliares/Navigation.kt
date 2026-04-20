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
import com.finanzasfamiliares.ui.components.MonthNavigationHeader
import com.finanzasfamiliares.ui.screens.analysis.AnalysisScreen
import com.finanzasfamiliares.ui.screens.config.ConfigScreen
import com.finanzasfamiliares.ui.screens.expenses.ExpensesScreen
import com.finanzasfamiliares.ui.screens.savings.SavingsScreen
import com.finanzasfamiliares.ui.screens.summary.SummaryScreen
import com.finanzasfamiliares.ui.screens.tithe.DonationsScreen
import java.time.YearMonth

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
                Screen.Summary.route -> SummaryRoute(sharedMonth = sharedMonth)
                Screen.Expenses.route -> ExpensesRoute(sharedMonth = sharedMonth)
                Screen.Tithe.route -> DonationsRoute(sharedMonth = sharedMonth)
                Screen.Savings.route -> SavingsRoute(sharedMonth = sharedMonth)
                Screen.Analysis.route -> AnalysisRoute(sharedMonth = sharedMonth)
                Screen.Config.route -> ConfigScreen()
                else -> SummaryRoute(sharedMonth = sharedMonth)
            }
        }
    }
}

@Composable
private fun SummaryRoute(sharedMonth: SharedMonthViewModel) {
    val currentYearMonth by sharedMonth.yearMonth.collectAsState()
    val monthLabel by sharedMonth.monthLabel.collectAsState()
    val availableMonths by sharedMonth.availableMonths.collectAsState()
    val canGoPreviousMonth by sharedMonth.canGoPreviousMonth.collectAsState()
    val canGoNextMonth by sharedMonth.canGoNextMonth.collectAsState()
    val isCurrentMonth by sharedMonth.isCurrentMonth.collectAsState()
    val isGenerating by sharedMonth.isGenerating.collectAsState()
    SummaryScreen(
        yearMonth = currentYearMonth,
        monthLabel = monthLabel,
        availableMonths = availableMonths,
        canGoPreviousMonth = canGoPreviousMonth,
        canGoNextMonth = canGoNextMonth,
        isCurrentMonthSelected = isCurrentMonth,
        isGeneratingMonths = isGenerating,
        onGoPreviousMonth = sharedMonth::goToPreviousAvailableMonth,
        onGoNextMonth = sharedMonth::goToNextAvailableMonth,
        onGoToMonth = sharedMonth::goToMonth,
        onGoToCurrentMonth = sharedMonth::resetToCurrentMonth,
        onGenerateMonths = sharedMonth::generateFutureMonths
    )
}

@Composable
private fun ExpensesRoute(sharedMonth: SharedMonthViewModel) {
    val currentYearMonth by sharedMonth.yearMonth.collectAsState()
    val monthLabel by sharedMonth.monthLabel.collectAsState()
    val availableMonths by sharedMonth.availableMonths.collectAsState()
    val canGoPreviousMonth by sharedMonth.canGoPreviousMonth.collectAsState()
    val canGoNextMonth by sharedMonth.canGoNextMonth.collectAsState()
    val isCurrentMonth by sharedMonth.isCurrentMonth.collectAsState()
    val isGenerating by sharedMonth.isGenerating.collectAsState()
    ExpensesScreen(
        yearMonth = currentYearMonth,
        canGoPreviousMonth = canGoPreviousMonth,
        canGoNextMonth = canGoNextMonth,
        onGoPreviousMonth = sharedMonth::goToPreviousAvailableMonth,
        onGoNextMonth = sharedMonth::goToNextAvailableMonth,
        headerContent = {
            SharedMonthHeader(
                currentYearMonth = currentYearMonth,
                monthLabel = monthLabel,
                availableMonths = availableMonths,
                canGoPreviousMonth = canGoPreviousMonth,
                canGoNextMonth = canGoNextMonth,
                isCurrentMonth = isCurrentMonth,
                isGenerating = isGenerating,
                sharedMonth = sharedMonth
            )
        }
    )
}

@Composable
private fun DonationsRoute(sharedMonth: SharedMonthViewModel) {
    val currentYearMonth by sharedMonth.yearMonth.collectAsState()
    val monthLabel by sharedMonth.monthLabel.collectAsState()
    val availableMonths by sharedMonth.availableMonths.collectAsState()
    val canGoPreviousMonth by sharedMonth.canGoPreviousMonth.collectAsState()
    val canGoNextMonth by sharedMonth.canGoNextMonth.collectAsState()
    val isCurrentMonth by sharedMonth.isCurrentMonth.collectAsState()
    val isGenerating by sharedMonth.isGenerating.collectAsState()
    DonationsScreen(
        yearMonth = currentYearMonth,
        canGoPreviousMonth = canGoPreviousMonth,
        canGoNextMonth = canGoNextMonth,
        onGoPreviousMonth = sharedMonth::goToPreviousAvailableMonth,
        onGoNextMonth = sharedMonth::goToNextAvailableMonth,
        headerContent = {
            SharedMonthHeader(
                currentYearMonth = currentYearMonth,
                monthLabel = monthLabel,
                availableMonths = availableMonths,
                canGoPreviousMonth = canGoPreviousMonth,
                canGoNextMonth = canGoNextMonth,
                isCurrentMonth = isCurrentMonth,
                isGenerating = isGenerating,
                sharedMonth = sharedMonth
            )
        }
    )
}

@Composable
private fun SavingsRoute(sharedMonth: SharedMonthViewModel) {
    val currentYearMonth by sharedMonth.yearMonth.collectAsState()
    val monthLabel by sharedMonth.monthLabel.collectAsState()
    val availableMonths by sharedMonth.availableMonths.collectAsState()
    val canGoPreviousMonth by sharedMonth.canGoPreviousMonth.collectAsState()
    val canGoNextMonth by sharedMonth.canGoNextMonth.collectAsState()
    val isCurrentMonth by sharedMonth.isCurrentMonth.collectAsState()
    val isGenerating by sharedMonth.isGenerating.collectAsState()
    SavingsScreen(
        yearMonth = currentYearMonth,
        canGoPreviousMonth = canGoPreviousMonth,
        canGoNextMonth = canGoNextMonth,
        onGoPreviousMonth = sharedMonth::goToPreviousAvailableMonth,
        onGoNextMonth = sharedMonth::goToNextAvailableMonth,
        headerContent = {
            SharedMonthHeader(
                currentYearMonth = currentYearMonth,
                monthLabel = monthLabel,
                availableMonths = availableMonths,
                canGoPreviousMonth = canGoPreviousMonth,
                canGoNextMonth = canGoNextMonth,
                isCurrentMonth = isCurrentMonth,
                isGenerating = isGenerating,
                sharedMonth = sharedMonth
            )
        }
    )
}

@Composable
private fun AnalysisRoute(sharedMonth: SharedMonthViewModel) {
    val currentYearMonth by sharedMonth.yearMonth.collectAsState()
    val monthLabel by sharedMonth.monthLabel.collectAsState()
    val availableMonths by sharedMonth.availableMonths.collectAsState()
    val canGoPreviousMonth by sharedMonth.canGoPreviousMonth.collectAsState()
    val canGoNextMonth by sharedMonth.canGoNextMonth.collectAsState()
    val isCurrentMonth by sharedMonth.isCurrentMonth.collectAsState()
    val isGenerating by sharedMonth.isGenerating.collectAsState()
    AnalysisScreen(
        yearMonth = currentYearMonth,
        canGoPreviousMonth = canGoPreviousMonth,
        canGoNextMonth = canGoNextMonth,
        onGoPreviousMonth = sharedMonth::goToPreviousAvailableMonth,
        onGoNextMonth = sharedMonth::goToNextAvailableMonth,
        headerContent = {
            SharedMonthHeader(
                currentYearMonth = currentYearMonth,
                monthLabel = monthLabel,
                availableMonths = availableMonths,
                canGoPreviousMonth = canGoPreviousMonth,
                canGoNextMonth = canGoNextMonth,
                isCurrentMonth = isCurrentMonth,
                isGenerating = isGenerating,
                sharedMonth = sharedMonth
            )
        }
    )
}

@Composable
private fun SharedMonthHeader(
    currentYearMonth: String,
    monthLabel: String,
    availableMonths: List<YearMonth>,
    canGoPreviousMonth: Boolean,
    canGoNextMonth: Boolean,
    isCurrentMonth: Boolean,
    isGenerating: Boolean,
    sharedMonth: SharedMonthViewModel
) {
    MonthNavigationHeader(
        currentMonth = YearMonth.parse(currentYearMonth),
        monthLabel = monthLabel,
        availableMonths = availableMonths,
        canGoPrevious = canGoPreviousMonth,
        canGoNext = canGoNextMonth,
        isCurrentMonth = isCurrentMonth,
        isGenerating = isGenerating,
        onGoPrevious = sharedMonth::goToPreviousAvailableMonth,
        onGoNext = sharedMonth::goToNextAvailableMonth,
        onGoToMonth = sharedMonth::goToMonth,
        onGoToCurrentMonth = sharedMonth::resetToCurrentMonth,
        onGenerateMonths = sharedMonth::generateFutureMonths
    )
}
