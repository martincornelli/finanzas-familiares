package com.finanzasfamiliares.ui.screens.summary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finanzasfamiliares.R
import com.finanzasfamiliares.data.model.IncomeCurrency
import com.finanzasfamiliares.data.model.MoneyEntry
import com.finanzasfamiliares.ui.components.MonthNavigationHeader
import com.finanzasfamiliares.ui.components.MonthSwipeContainer
import com.finanzasfamiliares.ui.components.SelectionActionBar
import com.finanzasfamiliares.ui.components.SummaryRow
import com.finanzasfamiliares.ui.components.clearZeroOnFocus
import com.finanzasfamiliares.ui.components.formatUSD
import com.finanzasfamiliares.ui.components.formatUYU
import com.finanzasfamiliares.ui.components.toInputAmount
import com.finanzasfamiliares.ui.theme.marginColors
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

@Composable
fun SummaryScreen(
    yearMonth: String,
    monthLabel: String,
    availableMonths: List<YearMonth>,
    canGoPreviousMonth: Boolean,
    canGoNextMonth: Boolean,
    isCurrentMonthSelected: Boolean,
    isGeneratingMonths: Boolean,
    isDeletingMonths: Boolean,
    isMonthHeaderPinned: Boolean = true,
    onGoPreviousMonth: () -> Unit,
    onGoNextMonth: () -> Unit,
    onGoToMonth: (YearMonth) -> Unit,
    onGoToCurrentMonth: () -> Unit,
    onGenerateMonths: (Int) -> Unit,
    onDeleteMonthAndFuture: () -> Unit,
    onToggleMonthHeaderPinned: () -> Unit = {},
    viewModel: SummaryViewModel = hiltViewModel()
) {
    LaunchedEffect(yearMonth) { viewModel.setYearMonth(yearMonth) }
    val data by viewModel.monthData.collectAsState()
    val isFutureMonth by viewModel.isFutureMonth.collectAsState()
    val currentYMString by viewModel.currentYMString.collectAsState()
    val config by viewModel.currentConfig.collectAsState()
    val incomeCurrency = config.incomeCurrency
    val primaryIncomeAmount = data?.primaryIncomeAmount(incomeCurrency) ?: 0.0
    val primaryIncomeUYU = data?.primaryIncomeInUYU(incomeCurrency) ?: 0.0
    val donationsUYU = data?.donationsInUYU(incomeCurrency) ?: 0.0
    val totalObligationsUYU = data?.totalObligationsInUYU(incomeCurrency) ?: 0.0
    val margin = data?.marginInUYU(incomeCurrency) ?: 0.0

    var showIncomeDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var showVariableIncomeDialog by remember { mutableStateOf(false) }
    var showDeleteMonthDialog by remember { mutableStateOf(false) }
    var editingVariableIncome by remember { mutableStateOf<MoneyEntry?>(null) }
    var deletingVariableIncome by remember { mutableStateOf<MoneyEntry?>(null) }
    var selectedVariableIncomeIds by remember { mutableStateOf(setOf<String>()) }
    var deletingVariableIncomeIds by remember { mutableStateOf<Set<String>?>(null) }
    var variableIncomeExpanded by remember(currentYMString) { mutableStateOf(false) }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM") }
    val selectedYearMonth = remember(yearMonth, monthFormatter) { YearMonth.parse(yearMonth, monthFormatter) }
    val hasFutureMonths = remember(availableMonths, selectedYearMonth) {
        availableMonths.any { it > selectedYearMonth }
    }
    val variableIncomes = data?.variableIncomes ?: emptyList()
    val selectableVariableIncomeIds = remember(variableIncomes) {
        variableIncomes.map { it.id }.toSet()
    }
    val canSelectMoreVariableIncomes = selectableVariableIncomeIds.any { it !in selectedVariableIncomeIds }

    fun toggleVariableIncomeSelection(id: String) {
        selectedVariableIncomeIds = if (selectedVariableIncomeIds.contains(id)) {
            selectedVariableIncomeIds - id
        } else {
            selectedVariableIncomeIds + id
        }
    }

    val monthHeader: @Composable () -> Unit = {
        MonthNavigationHeader(
            currentMonth = selectedYearMonth,
            monthLabel = monthLabel,
            availableMonths = availableMonths,
            canGoPrevious = canGoPreviousMonth,
            canGoNext = canGoNextMonth,
            isCurrentMonth = isCurrentMonthSelected,
            isGenerating = isGeneratingMonths,
            isHeaderPinned = isMonthHeaderPinned,
            onGoPrevious = onGoPreviousMonth,
            onGoNext = onGoNextMonth,
            onGoToMonth = onGoToMonth,
            onGoToCurrentMonth = onGoToCurrentMonth,
            onGenerateMonths = onGenerateMonths,
            onToggleHeaderPinned = onToggleMonthHeaderPinned
        )
    }

    MonthSwipeContainer(
        canGoPrevious = canGoPreviousMonth,
        canGoNext = canGoNextMonth,
        onGoPrevious = onGoPreviousMonth,
        onGoNext = onGoNextMonth,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(Modifier.fillMaxSize()) {
            if (isMonthHeaderPinned) {
                monthHeader()
            }
            if (selectedVariableIncomeIds.isNotEmpty()) {
                SelectionActionBar(
                    selectedCount = selectedVariableIncomeIds.size,
                    canSelectAll = canSelectMoreVariableIncomes,
                    onSelectAll = { selectedVariableIncomeIds = selectableVariableIncomeIds },
                    onClearSelection = { selectedVariableIncomeIds = emptySet() },
                    onDeleteSelected = {
                        deletingVariableIncomeIds = selectedVariableIncomeIds
                    }
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
            ) {
        item {
            if (!isMonthHeaderPinned) {
                monthHeader()
            }

            val colors = marginColors(
                amount = margin,
                greenThreshold = config.marginGreenThresholdPct,
                yellowThreshold = config.marginYellowThresholdPct
            )

            Box(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.summary_margin_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.content
                    )
                    Text(
                        margin.formatUYU(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.content
                    )
                    Text(
                        stringResource(R.string.summary_margin_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.content
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = colors.content.copy(alpha = 0.22f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HeroMiniMetric(
                            label = stringResource(R.string.analysis_income_metric),
                            value = primaryIncomeUYU.formatUYU(),
                            color = colors.content
                        )
                        HeroMiniMetric(
                            label = stringResource(R.string.analysis_obligations_metric),
                            value = totalObligationsUYU.formatUYU(),
                            color = colors.content,
                            alignEnd = true
                        )
                    }
                }
            }

            SummaryPanel {
                SummaryRow(stringResource(R.string.config_base_rate_label), "$ ${"%.2f".format(data?.exchangeRate ?: 0.0)}")
                SummaryRow(stringResource(R.string.config_card_offset_label), "$ ${"%.2f".format(data?.cardExchangeRate ?: 0.0)}")
                TextButton(onClick = { showRateDialog = true }, Modifier.padding(start = 8.dp)) {
                    Text(stringResource(R.string.summary_change_rate), style = MaterialTheme.typography.labelSmall)
                }
            }

            SummaryPanel {
                SummaryRow(
                    stringResource(R.string.summary_primary_income),
                    if (incomeCurrency == IncomeCurrency.UYU) primaryIncomeAmount.formatUYU() else primaryIncomeAmount.formatUSD()
                )
                if (incomeCurrency == IncomeCurrency.USD) {
                    SummaryRow(stringResource(R.string.summary_primary_income_in_pesos), primaryIncomeUYU.formatUYU())
                }
                TextButton(onClick = { showIncomeDialog = true }, Modifier.padding(start = 8.dp)) {
                    Text(stringResource(R.string.summary_edit_primary_income), style = MaterialTheme.typography.labelSmall)
                }
            }

            SummaryPanel {
                SummaryRow(stringResource(R.string.summary_variable_income), (data?.variableIncomeUYU ?: 0.0).formatUYU())
                if (variableIncomes.isNotEmpty()) {
                    TextButton(
                        onClick = { variableIncomeExpanded = !variableIncomeExpanded },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            stringResource(
                                if (variableIncomeExpanded) R.string.summary_variable_income_collapse
                                else R.string.summary_variable_income_expand
                            ),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                TextButton(onClick = { showVariableIncomeDialog = true }, modifier = Modifier.padding(start = 8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.summary_add_variable_income), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (variableIncomeExpanded) {
            items(variableIncomes, key = { it.id }) { income ->
                MoneyEntryRow(
                    entry = income,
                    exchangeRate = data?.exchangeRate ?: 0.0,
                    titleFallback = stringResource(R.string.summary_variable_income_item_fallback),
                    readOnly = false,
                    selected = selectedVariableIncomeIds.contains(income.id),
                    selectionMode = selectedVariableIncomeIds.isNotEmpty(),
                    onToggleSelection = { toggleVariableIncomeSelection(income.id) },
                    onEdit = { editingVariableIncome = income },
                    onDelete = { deletingVariableIncome = income }
                )
            }
        }

        item {
            SummaryPanel {
                SummaryRow(stringResource(R.string.summary_donations), donationsUYU.formatUYU())
                SummaryRow(stringResource(R.string.summary_fixed_expenses), (data?.fixedExpenses?.sumOf { it.totalUYU(data?.cardExchangeRate ?: 0.0) } ?: 0.0).formatUYU())
                SummaryRow(stringResource(R.string.summary_variable_expenses), (data?.variableExpenses?.sumOf { it.totalUYU(data?.cardExchangeRate ?: 0.0) } ?: 0.0).formatUYU())
                SummaryRow(stringResource(R.string.summary_credit_card), (data?.cardExpenses?.sumOf { it.totalUYU(data?.cardExchangeRate ?: 0.0) } ?: 0.0).formatUYU())
                SummaryRow(stringResource(R.string.summary_debts), (data?.debts?.sumOf { it.totalUYU(data?.cardExchangeRate ?: 0.0) } ?: 0.0).formatUYU())
                HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                SummaryRow(stringResource(R.string.summary_total_obligations), totalObligationsUYU.formatUYU())
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { showDeleteMonthDialog = true },
                enabled = !isDeletingMonths,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(
                    stringResource(
                        if (hasFutureMonths) R.string.summary_delete_months_button
                        else R.string.summary_delete_month_button
                    )
                )
            }
        }
            }
        }
    }

    if (showIncomeDialog) {
        PrimaryIncomeDialog(
            initialValue = primaryIncomeAmount,
            incomeCurrency = incomeCurrency,
            allowApplyToFuture = isFutureMonth,
            onConfirm = { amount, applyToFuture ->
                viewModel.updatePrimaryIncome(amount, applyToFuture)
                showIncomeDialog = false
            },
            onDismiss = { showIncomeDialog = false }
        )
    }
    if (showRateDialog) {
        ExchangeSettingsDialog(
            title = stringResource(R.string.dialog_rate_title),
            purchaseLabel = stringResource(R.string.config_base_rate_label),
            saleLabel = stringResource(R.string.config_card_offset_label),
            initialPurchaseValue = data?.exchangeRate ?: 0.0,
            initialSaleValue = data?.cardExchangeRate ?: 0.0,
            allowApplyToFuture = isFutureMonth,
            onConfirm = { purchase, sale, apply ->
                viewModel.updateExchangeSettings(purchase, sale, apply)
                showRateDialog = false
            },
            onDismiss = { showRateDialog = false }
        )
    }
    if (showVariableIncomeDialog || editingVariableIncome != null) {
        MoneyEntryDialog(
            title = stringResource(if (editingVariableIncome == null) R.string.dialog_variable_income_title_new else R.string.dialog_variable_income_title_edit),
            initial = editingVariableIncome,
            onConfirm = {
                viewModel.upsertVariableIncome(it)
                showVariableIncomeDialog = false
                editingVariableIncome = null
            },
            onDismiss = {
                showVariableIncomeDialog = false
                editingVariableIncome = null
            }
        )
    }
    deletingVariableIncome?.let { income ->
        AlertDialog(
            onDismissRequest = { deletingVariableIncome = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.delete_confirm_named,
                        income.name.ifBlank { stringResource(R.string.summary_variable_income_item_fallback) }
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteVariableIncome(income.id)
                    deletingVariableIncome = null
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingVariableIncome = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
    deletingVariableIncomeIds?.let { ids ->
        AlertDialog(
            onDismissRequest = { deletingVariableIncomeIds = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_selected)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteVariableIncomes(ids)
                    selectedVariableIncomeIds = emptySet()
                    deletingVariableIncomeIds = null
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingVariableIncomeIds = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
    if (showDeleteMonthDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteMonthDialog = false },
            title = {
                Text(
                    stringResource(
                        if (hasFutureMonths) R.string.summary_delete_months_title
                        else R.string.summary_delete_month_title
                    )
                )
            },
            text = {
                Text(
                    stringResource(
                        if (hasFutureMonths) R.string.summary_delete_months_message
                        else R.string.summary_delete_month_message,
                        monthLabel
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMonthAndFuture()
                        showDeleteMonthDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(
                        stringResource(
                            if (hasFutureMonths) R.string.summary_delete_months_confirm
                            else R.string.summary_delete_month_confirm
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMonthDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
    if (isDeletingMonths) {
        DeletingMonthsDialog()
    }
}

@Composable
private fun HeroMiniMetric(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    alignEnd: Boolean = false
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.78f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun SummaryPanel(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
private fun DeletingMonthsDialog() {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text(stringResource(R.string.summary_deleting_months)) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                Text(stringResource(R.string.summary_deleting_months_hint))
            }
        }
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MoneyEntryRow(
    entry: MoneyEntry,
    exchangeRate: Double,
    titleFallback: String,
    readOnly: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = { if (!readOnly && selectionMode) onToggleSelection() },
            onLongClick = { if (!readOnly) onToggleSelection() }
        ),
        colors = ListItemDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        headlineContent = { Text(entry.name.ifBlank { titleFallback }) },
        supportingContent = {
            if (entry.isInUSD()) {
                Text("${entry.amountUSD.formatUSD()} x ${"%.2f".format(exchangeRate)}", style = MaterialTheme.typography.labelSmall)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.totalUYU(exchangeRate).formatUYU(), fontWeight = FontWeight.Medium)
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onToggleSelection() }
                    )
                } else if (!readOnly) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit)) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    )
}

@Composable
private fun MoneyEntryDialog(title: String, initial: MoneyEntry?, onConfirm: (MoneyEntry) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var isUSD by remember { mutableStateOf(initial?.isInUSD() ?: false) }
    var amount by remember {
        mutableStateOf(
            when {
                initial?.isInUSD() == true -> initial.amountUSD.toInputAmount()
                initial != null -> initial.amountUYU.toInputAmount()
                else -> ""
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.common_optional_name)) }, singleLine = true)
                CurrencySelector(isUSD = isUSD, onChange = { isUSD = it })
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.clearZeroOnFocus(amount) { amount = it },
                    label = { Text(stringResource(R.string.common_amount_label)) },
                    prefix = { Text(stringResource(if (isUSD) R.string.prefix_usd else R.string.prefix_uyu)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = amount.replace(",", ".").toDoubleOrNull() ?: 0.0
                onConfirm(
                    MoneyEntry(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        amountUSD = if (isUSD) parsed else 0.0,
                        amountUYU = if (isUSD) 0.0 else parsed,
                        isUSD = isUSD,
                        currency = if (isUSD) IncomeCurrency.USD else IncomeCurrency.UYU
                    )
                )
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun PrimaryIncomeDialog(
    initialValue: Double,
    incomeCurrency: String,
    allowApplyToFuture: Boolean,
    onConfirm: (Double, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf(if (initialValue > 0) initialValue.toInputAmount() else "") }
    var applyToFuture by remember { mutableStateOf(false) }
    val isIncomeInUYU = incomeCurrency == IncomeCurrency.UYU

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_primary_income_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.clearZeroOnFocus(amount) { amount = it },
                    label = {
                        Text(
                            stringResource(
                                if (isIncomeInUYU) R.string.dialog_primary_income_label_uyu
                                else R.string.dialog_primary_income_label
                            )
                        )
                    },
                    prefix = { Text(stringResource(if (isIncomeInUYU) R.string.prefix_uyu else R.string.prefix_usd)) },
                    singleLine = true
                )
                if (allowApplyToFuture) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = applyToFuture, onCheckedChange = { applyToFuture = it })
                        Text(stringResource(R.string.common_apply_to_future))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(amount.replace(",", ".").toDoubleOrNull() ?: 0.0, applyToFuture) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun ExchangeSettingsDialog(
    title: String,
    purchaseLabel: String,
    saleLabel: String,
    initialPurchaseValue: Double,
    initialSaleValue: Double,
    allowApplyToFuture: Boolean,
    onConfirm: (Double, Double, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var purchaseValue by remember {
        mutableStateOf(if (initialPurchaseValue > 0) initialPurchaseValue.toInputAmount() else "")
    }
    var saleValue by remember {
        mutableStateOf(if (initialSaleValue > 0) initialSaleValue.toInputAmount() else "")
    }
    var applyToFuture by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = purchaseValue,
                    onValueChange = { purchaseValue = it },
                    modifier = Modifier.clearZeroOnFocus(purchaseValue) { purchaseValue = it },
                    label = { Text(purchaseLabel) },
                    prefix = { Text(stringResource(R.string.prefix_uyu)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = saleValue,
                    onValueChange = { saleValue = it },
                    modifier = Modifier.clearZeroOnFocus(saleValue) { saleValue = it },
                    label = { Text(saleLabel) },
                    prefix = { Text(stringResource(R.string.prefix_uyu)) },
                    singleLine = true
                )
                if (allowApplyToFuture) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = applyToFuture, onCheckedChange = { applyToFuture = it })
                        Text(stringResource(R.string.common_apply_to_future))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val purchase = purchaseValue.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val sale = saleValue.replace(",", ".").toDoubleOrNull() ?: purchase
                    onConfirm(purchase, sale, applyToFuture)
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun CurrencySelector(isUSD: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = !isUSD, onClick = { onChange(false) }, label = { Text(stringResource(R.string.common_currency_uyu_short)) })
        FilterChip(selected = isUSD, onClick = { onChange(true) }, label = { Text(stringResource(R.string.common_currency_usd_short)) })
    }
}
