package com.finanzasfamiliares.ui.screens.summary

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finanzasfamiliares.R
import com.finanzasfamiliares.data.model.IncomeCurrency
import com.finanzasfamiliares.data.model.MoneyEntry
import com.finanzasfamiliares.ui.components.SummaryRow
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
    onYearMonthChange: (String) -> Unit = {},
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val label by viewModel.monthLabel.collectAsState()
    val data by viewModel.monthData.collectAsState()
    val isCurrentMonth by viewModel.isCurrentMonth.collectAsState()
    val isFutureMonth by viewModel.isFutureMonth.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val currentYMString by viewModel.currentYMString.collectAsState()
    val config by viewModel.currentConfig.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val incomeCurrency = config.incomeCurrency
    val primaryIncomeAmount = data?.primaryIncomeAmount(incomeCurrency) ?: 0.0
    val primaryIncomeUYU = data?.primaryIncomeInUYU(incomeCurrency) ?: 0.0
    val donationsUYU = data?.donationsInUYU(incomeCurrency) ?: 0.0
    val totalObligationsUYU = data?.totalObligationsInUYU(incomeCurrency) ?: 0.0
    val margin = data?.marginInUYU(incomeCurrency) ?: 0.0

    LaunchedEffect(currentYMString) {
        onYearMonthChange(currentYMString)
        viewModel.ensureMonthExists()
    }

    var showIncomeDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }
    var showVariableIncomeDialog by remember { mutableStateOf(false) }
    var showGenerateDialog by remember { mutableStateOf(false) }
    var showMonthPickerDialog by remember { mutableStateOf(false) }
    var editingVariableIncome by remember { mutableStateOf<MoneyEntry?>(null) }
    var variableIncomeExpanded by remember(currentYMString) { mutableStateOf(false) }
    var navigationDirection by remember { mutableStateOf(1) }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM") }
    val selectedYearMonth = remember(currentYMString) { YearMonth.parse(currentYMString, monthFormatter) }
    val goPrevious = {
        navigationDirection = -1
        viewModel.goToPreviousMonth()
    }
    val goNext = {
        navigationDirection = 1
        viewModel.goToNextMonth()
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(currentYMString) {
                var accumulatedDrag = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        accumulatedDrag += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        when {
                            accumulatedDrag > 80f -> goPrevious()
                            accumulatedDrag < -80f -> goNext()
                        }
                        accumulatedDrag = 0f
                    },
                    onDragCancel = { accumulatedDrag = 0f }
                )
            }
    ) {
    AnimatedContent(
        targetState = currentYMString,
        transitionSpec = {
            if (navigationDirection >= 0) {
                slideInHorizontally { it / 3 } + fadeIn() togetherWith
                    slideOutHorizontally { -it / 3 } + fadeOut()
            } else {
                slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                    slideOutHorizontally { it / 3 } + fadeOut()
            }
        },
        label = "monthTransition"
    ) { animatedMonthKey ->
    key(animatedMonthKey) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = goPrevious) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.summary_prev_month_cd))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { showMonthPickerDialog = true }) {
                        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (!isCurrentMonth) {
                        FilterChip(
                            selected = true,
                            onClick = {
                                navigationDirection = if (selectedYearMonth <= YearMonth.now()) 1 else -1
                                viewModel.goToMonth(YearMonth.now())
                            },
                            label = { Text(stringResource(R.string.summary_go_to_current_month)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Today,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        )
                    }
                    TextButton(onClick = { showGenerateDialog = true }) {
                        Text(stringResource(R.string.summary_generate_months))
                    }
                }
                IconButton(onClick = goNext) {
                    Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.summary_next_month_cd))
                }
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
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.summary_margin_label), style = MaterialTheme.typography.labelLarge, color = colors.content)
                    Spacer(Modifier.height(8.dp))
                    Text(margin.formatUYU(), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = colors.content)
                    Text(stringResource(R.string.summary_margin_hint), style = MaterialTheme.typography.bodySmall, color = colors.content)
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            SummaryRow(stringResource(R.string.summary_exchange_rate), "$ ${"%.2f".format(data?.exchangeRate ?: 0.0)}")
            TextButton(onClick = { showRateDialog = true }, Modifier.padding(start = 8.dp)) {
                Text(stringResource(R.string.summary_change_rate), style = MaterialTheme.typography.labelSmall)
            }

            HorizontalDivider()

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

            HorizontalDivider()

            SummaryRow(stringResource(R.string.summary_variable_income), (data?.variableIncomeUYU ?: 0.0).formatUYU())
            if (!(data?.variableIncomes ?: emptyList()).isEmpty()) {
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
        }

        if (variableIncomeExpanded) {
            items(data?.variableIncomes ?: emptyList(), key = { it.id }) { income ->
                MoneyEntryRow(
                    entry = income,
                    exchangeRate = data?.exchangeRate ?: 0.0,
                    titleFallback = stringResource(R.string.summary_variable_income_item_fallback),
                    readOnly = false,
                    onEdit = { editingVariableIncome = income },
                    onDelete = { viewModel.deleteVariableIncome(income.id) }
                )
            }
        }

        item {
            TextButton(onClick = { showVariableIncomeDialog = true }, modifier = Modifier.padding(start = 8.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.summary_add_variable_income), style = MaterialTheme.typography.labelSmall)
            }

            HorizontalDivider()

            SummaryRow(stringResource(R.string.summary_donations), donationsUYU.formatUYU())
            SummaryRow(stringResource(R.string.summary_fixed_expenses), (data?.fixedExpenses?.sumOf { it.amountUYU } ?: 0.0).formatUYU())
            SummaryRow(stringResource(R.string.summary_variable_expenses), (data?.variableExpenses?.sumOf { it.totalUYU(data?.cardExchangeRate ?: 0.0) } ?: 0.0).formatUYU())
            SummaryRow(stringResource(R.string.summary_credit_card), (data?.cardExpenses?.sumOf { it.totalUYU(data?.cardExchangeRate ?: 0.0) } ?: 0.0).formatUYU())
            SummaryRow(stringResource(R.string.summary_debts), (data?.debts?.sumOf { it.totalUYU(data?.cardExchangeRate ?: 0.0) } ?: 0.0).formatUYU())
            SummaryRow(stringResource(R.string.summary_total_obligations), totalObligationsUYU.formatUYU())
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
        ExchangeRateDialog(
            title = stringResource(R.string.dialog_rate_title),
            label = stringResource(R.string.dialog_rate_label),
            initialValue = data?.exchangeRate ?: 0.0,
            prefix = stringResource(R.string.prefix_uyu),
            allowApplyToFuture = isFutureMonth,
            onConfirm = { amount, apply ->
                viewModel.updateExchangeRate(amount, apply)
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
    if (showGenerateDialog) {
        GenerateMonthsDialog(
            onConfirm = {
                viewModel.generateFutureMonths(it)
                showGenerateDialog = false
            },
            onDismiss = { showGenerateDialog = false }
        )
    }
    if (showMonthPickerDialog) {
        MonthPickerDialog(
            initialMonth = selectedYearMonth,
            availableMonths = availableMonths,
            onConfirm = {
                navigationDirection = if (it >= selectedYearMonth) 1 else -1
                viewModel.goToMonth(it)
                showMonthPickerDialog = false
            },
            onDismiss = { showMonthPickerDialog = false }
        )
    }
    if (isGenerating) {
        LoadingDialog(stringResource(R.string.summary_generating_months))
    }
}

@Composable
private fun MoneyEntryRow(
    entry: MoneyEntry,
    exchangeRate: Double,
    titleFallback: String,
    readOnly: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = { Text(entry.name.ifBlank { titleFallback }) },
        supportingContent = {
            if (entry.isInUSD()) {
                Text("${entry.amountUSD.formatUSD()} x ${"%.2f".format(exchangeRate)}", style = MaterialTheme.typography.labelSmall)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.totalUYU(exchangeRate).formatUYU(), fontWeight = FontWeight.Medium)
                if (!readOnly) {
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
private fun GenerateMonthsDialog(onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var months by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.summary_generate_months)) },
        text = {
            OutlinedTextField(
                value = months,
                onValueChange = { months = it.filter(Char::isDigit) },
                label = { Text(stringResource(R.string.summary_generate_months_label)) },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(months.toIntOrNull() ?: 0) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MonthPickerDialog(
    initialMonth: YearMonth,
    availableMonths: List<YearMonth>,
    onConfirm: (YearMonth) -> Unit,
    onDismiss: () -> Unit
) {
    val currentMonth = remember { YearMonth.now() }
    val monthOptions = remember(availableMonths, initialMonth) {
        availableMonths.ifEmpty { listOf(initialMonth) }.sorted()
    }
    val monthsByYear = remember(monthOptions) { monthOptions.groupBy { it.year } }
    var selectedMonth by remember(initialMonth, monthOptions) {
        mutableStateOf(
            monthOptions.firstOrNull { it == initialMonth }
                ?: monthOptions.lastOrNull()
                ?: initialMonth
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.summary_pick_month_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (monthOptions.isEmpty()) {
                    Text(stringResource(R.string.summary_pick_month_empty))
                } else {
                    monthOptions.firstOrNull { it == currentMonth }?.let {
                        TextButton(
                            onClick = { selectedMonth = currentMonth },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(R.string.summary_go_to_current_month))
                        }
                    }
                    monthsByYear.forEach { (year, months) ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = year.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                months.sorted().forEach { month ->
                                    FilterChip(
                                        selected = selectedMonth == month,
                                        onClick = { selectedMonth = month },
                                        label = {
                                            Text(
                                                month.month
                                                    .getDisplayName(TextStyle.SHORT, Locale("es", "UY"))
                                                    .replaceFirstChar { it.uppercase() }
                                            )
                                        },
                                        leadingIcon = if (month == currentMonth) {
                                            {
                                                Icon(
                                                    Icons.Default.Today,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                )
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedMonth) }) {
                Text(stringResource(R.string.action_go))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun ExchangeRateDialog(
    title: String,
    label: String,
    initialValue: Double,
    prefix: String,
    allowApplyToFuture: Boolean,
    onConfirm: (Double, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(if (initialValue > 0) initialValue.toInputAmount() else "") }
    var applyToFuture by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(label) }, prefix = { Text(prefix) }, singleLine = true)
                if (allowApplyToFuture) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = applyToFuture, onCheckedChange = { applyToFuture = it })
                        Text(stringResource(R.string.common_apply_to_future))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(value.replace(",", ".").toDoubleOrNull() ?: 0.0, applyToFuture) }) { Text(stringResource(R.string.action_save)) } },
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

@Composable
private fun LoadingDialog(message: String) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text(message) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                Text(stringResource(R.string.summary_generating_months_hint))
            }
        }
    )
}
