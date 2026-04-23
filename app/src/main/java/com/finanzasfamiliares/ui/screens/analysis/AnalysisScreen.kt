package com.finanzasfamiliares.ui.screens.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finanzasfamiliares.R
import com.finanzasfamiliares.data.model.MonthData
import com.finanzasfamiliares.ui.components.FinanceCard
import com.finanzasfamiliares.ui.components.MonthSwipeContainer
import com.finanzasfamiliares.ui.components.SummaryRow
import com.finanzasfamiliares.ui.components.formatUYU
import kotlin.math.abs

private data class ChartItem(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun AnalysisScreen(
    yearMonth: String,
    canGoPreviousMonth: Boolean = false,
    canGoNextMonth: Boolean = false,
    onGoPreviousMonth: () -> Unit = {},
    onGoNextMonth: () -> Unit = {},
    headerPinned: Boolean = true,
    headerContent: @Composable () -> Unit = {},
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    LaunchedEffect(yearMonth) { viewModel.setYearMonth(yearMonth) }

    val config by viewModel.currentConfig.collectAsState()
    val data by viewModel.monthData.collectAsState()
    val previousData by viewModel.previousMonthData.collectAsState()
    val previousMonthLabel by viewModel.previousMonthLabel.collectAsState()

    val incomeCurrency = config.incomeCurrency
    val donationsLabel = stringResource(R.string.summary_donations)
    val fixedLabel = stringResource(R.string.summary_fixed_expenses)
    val variableLabel = stringResource(R.string.summary_variable_expenses)
    val cardLabel = stringResource(R.string.summary_credit_card)
    val debtLabel = stringResource(R.string.summary_debts)
    val uncategorizedLabel = stringResource(R.string.analysis_uncategorized)
    val currentTypeItems = buildTypeItems(
        data = data,
        incomeCurrency = incomeCurrency,
        donationsLabel = donationsLabel,
        fixedLabel = fixedLabel,
        variableLabel = variableLabel,
        cardLabel = cardLabel,
        debtLabel = debtLabel
    )
    val categoryItems = buildCategoryItems(data, uncategorizedLabel)
    val currentIncome = data?.totalIncomeInUYU(incomeCurrency) ?: 0.0
    val previousIncome = previousData?.totalIncomeInUYU(incomeCurrency) ?: 0.0
    val currentObligations = data?.totalObligationsInUYU(incomeCurrency) ?: 0.0
    val previousObligations = previousData?.totalObligationsInUYU(incomeCurrency) ?: 0.0
    val currentMargin = data?.marginInUYU(incomeCurrency) ?: 0.0
    val previousMargin = previousData?.marginInUYU(incomeCurrency) ?: 0.0
    val comparisonItems = listOf(
        stringResource(R.string.analysis_income_metric) to (currentIncome to previousIncome),
        stringResource(R.string.analysis_obligations_metric) to (currentObligations to previousObligations),
        stringResource(R.string.analysis_margin_metric) to (currentMargin to previousMargin)
    )

    MonthSwipeContainer(
        canGoPrevious = canGoPreviousMonth,
        canGoNext = canGoNextMonth,
        onGoPrevious = onGoPreviousMonth,
        onGoNext = onGoNextMonth,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(Modifier.fillMaxSize()) {
            if (headerPinned) {
                headerContent()
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
        item {
            if (!headerPinned) {
                headerContent()
            }
            Text(
                text = stringResource(R.string.analysis_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (data == null) {
            item {
                Text(
                    text = stringResource(R.string.analysis_empty_state),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            item {
                AnalysisCard(
                    title = stringResource(R.string.analysis_types_title),
                    hint = stringResource(R.string.analysis_types_hint)
                ) {
                    SimpleBarChart(items = currentTypeItems)
                }
            }

            item {
                AnalysisCard(
                    title = stringResource(R.string.analysis_categories_title),
                    hint = stringResource(R.string.analysis_categories_hint)
                ) {
                    if (categoryItems.isEmpty()) {
                        Text(
                            stringResource(R.string.analysis_no_expenses_by_category),
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        SimpleBarChart(items = categoryItems)
                    }
                }
            }

            item {
                AnalysisCard(
                    title = stringResource(R.string.analysis_month_compare_title),
                    hint = stringResource(R.string.analysis_month_compare_hint)
                ) {
                    ComparisonBarChart(
                        items = comparisonItems,
                        currentLabel = stringResource(R.string.analysis_current_month),
                        previousLabel = previousMonthLabel
                    )
                }
            }
        }
            }
        }
    }
}

@Composable
private fun AnalysisCard(
    title: String,
    hint: String,
    content: @Composable () -> Unit
) {
    FinanceCard(
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outlineVariant
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        content()
    }
}

@Composable
private fun SimpleBarChart(items: List<ChartItem>) {
    val maxValue = items.maxOfOrNull { abs(it.value) }?.takeIf { it > 0.0 } ?: 1.0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(item.value.formatUYU(), style = MaterialTheme.typography.bodyMedium)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((abs(item.value) / maxValue).toFloat().coerceIn(0f, 1f))
                            .height(12.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(item.color)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonBarChart(
    items: List<Pair<String, Pair<Double, Double>>>,
    currentLabel: String,
    previousLabel: String
) {
    val maxValue = items
        .flatMap { listOf(it.second.first, it.second.second) }
        .maxOfOrNull { abs(it) }
        ?.takeIf { it > 0.0 } ?: 1.0

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items.forEach { item ->
            val label = item.first
            val current = item.second.first
            val previous = item.second.second

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                ComparisonBar(
                    label = currentLabel,
                    value = current,
                    maxValue = maxValue,
                    color = MaterialTheme.colorScheme.primary
                )
                ComparisonBar(
                    label = previousLabel,
                    value = previous,
                    maxValue = maxValue,
                    color = MaterialTheme.colorScheme.tertiary
                )
                SummaryRow(
                    label = stringResource(R.string.analysis_difference_metric),
                    value = (current - previous).formatUYU(),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ComparisonBar(
    label: String,
    value: Double,
    maxValue: Double,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(value.formatUYU(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((abs(value) / maxValue).toFloat().coerceIn(0f, 1f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color)
            )
        }
    }
}

private fun buildTypeItems(
    data: MonthData?,
    incomeCurrency: String,
    donationsLabel: String,
    fixedLabel: String,
    variableLabel: String,
    cardLabel: String,
    debtLabel: String
): List<ChartItem> {
    if (data == null) return emptyList()
    val cardRate = data.cardExchangeRate
    return listOf(
        ChartItem(donationsLabel, data.donationsInUYU(incomeCurrency), Color(0xFF2E7D32)),
        ChartItem(fixedLabel, data.fixedExpenses.sumOf { it.totalUYU(cardRate) }, Color(0xFF1565C0)),
        ChartItem(variableLabel, data.variableExpenses.sumOf { it.totalUYU(cardRate) }, Color(0xFF6A1B9A)),
        ChartItem(cardLabel, data.cardExpenses.sumOf { it.totalUYU(cardRate) }, Color(0xFFEF6C00)),
        ChartItem(debtLabel, data.debts.sumOf { it.totalUYU(cardRate) }, Color(0xFFC62828))
    )
}

private fun buildCategoryItems(data: MonthData?, uncategorizedLabel: String): List<ChartItem> {
    if (data == null) return emptyList()

    val totalsByCategory = linkedMapOf<String, Double>()

    fun add(category: String, amount: Double) {
        val key = category.ifBlank { uncategorizedLabel }
        totalsByCategory[key] = (totalsByCategory[key] ?: 0.0) + amount
    }

    data.fixedExpenses.forEach { add(it.category, it.totalUYU(data.cardExchangeRate)) }
    data.variableExpenses.forEach { add(it.category, it.totalUYU(data.cardExchangeRate)) }
    data.cardExpenses.forEach { add(it.category, it.totalUYU(data.cardExchangeRate)) }
    data.debts.forEach { add(it.category, it.totalUYU(data.cardExchangeRate)) }

    val palette = listOf(
        Color(0xFF00897B),
        Color(0xFF3949AB),
        Color(0xFFD81B60),
        Color(0xFFF4511E),
        Color(0xFF7CB342),
        Color(0xFF5E35B1),
        Color(0xFF039BE5),
        Color(0xFF8E24AA)
    )

    return totalsByCategory
        .filterValues { it > 0.0 }
        .toList()
        .sortedByDescending { it.second }
        .mapIndexed { index, entry ->
            ChartItem(
                label = entry.first,
                value = entry.second,
                color = palette[index % palette.size]
            )
        }
}
