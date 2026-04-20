package com.finanzasfamiliares.ui.screens.expenses

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finanzasfamiliares.R
import com.finanzasfamiliares.data.model.CardExpense
import com.finanzasfamiliares.data.model.CardExpenseKind
import com.finanzasfamiliares.data.model.DebtEntry
import com.finanzasfamiliares.data.model.FixedExpense
import com.finanzasfamiliares.data.model.IncomeCurrency
import com.finanzasfamiliares.data.model.MoneyEntry
import com.finanzasfamiliares.ui.components.MonthHeader
import com.finanzasfamiliares.ui.components.ReadOnlyBanner
import com.finanzasfamiliares.ui.components.SectionHeader
import com.finanzasfamiliares.ui.components.formatUSD
import com.finanzasfamiliares.ui.components.formatUYU
import com.finanzasfamiliares.ui.components.toInputAmount
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

private enum class ExpenseSection {
    FIXED, VARIABLE, CARD, DEBT
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpensesScreen(yearMonth: String, viewModel: ExpensesViewModel = hiltViewModel()) {
    LaunchedEffect(yearMonth) { viewModel.setYearMonth(yearMonth) }
    val data by viewModel.monthData.collectAsState()
    val isReadOnly by viewModel.isReadOnly.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showAddFixed by remember { mutableStateOf(false) }
    var showAddVariable by remember { mutableStateOf(false) }
    var showAddCard by remember { mutableStateOf(false) }
    var showAddDebt by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var editFixed by remember { mutableStateOf<FixedExpense?>(null) }
    var editVariable by remember { mutableStateOf<MoneyEntry?>(null) }
    var editCard by remember { mutableStateOf<CardExpense?>(null) }
    var editDebt by remember { mutableStateOf<DebtEntry?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var collapsedSections by remember { mutableStateOf(setOf<ExpenseSection>()) }
    var selectedExpenseKeys by remember { mutableStateOf(setOf<String>()) }
    val cardRate = data?.cardExchangeRate ?: 0.0
    val normalizedQuery = searchQuery.trim().lowercase()
    val allSections = remember {
        setOf(
            ExpenseSection.FIXED,
            ExpenseSection.VARIABLE,
            ExpenseSection.CARD,
            ExpenseSection.DEBT
        )
    }
    val areAllCollapsed = collapsedSections.size == allSections.size

    fun matchesQuery(name: String, category: String): Boolean =
        normalizedQuery.isBlank() ||
            name.lowercase().contains(normalizedQuery) ||
            category.lowercase().contains(normalizedQuery)

    val filteredFixedExpenses = (data?.fixedExpenses ?: emptyList()).filter {
        matchesQuery(it.name, it.category)
    }
    val filteredVariableExpenses = (data?.variableExpenses ?: emptyList()).filter {
        matchesQuery(it.name, it.category)
    }
    val filteredCardExpenses = (data?.cardExpenses ?: emptyList()).filter {
        matchesQuery(it.name, it.category)
    }
    val filteredDebts = (data?.debts ?: emptyList()).filter {
        matchesQuery(it.name, it.category)
    }
    val fixedExpanded = !collapsedSections.contains(ExpenseSection.FIXED)
    val variableExpanded = !collapsedSections.contains(ExpenseSection.VARIABLE)
    val cardExpanded = !collapsedSections.contains(ExpenseSection.CARD)
    val debtExpanded = !collapsedSections.contains(ExpenseSection.DEBT)
    val visibleTotalUYU =
        (if (fixedExpanded) filteredFixedExpenses.sumOf { it.amountUYU } else 0.0) +
        (if (variableExpanded) filteredVariableExpenses.filter { !it.isInUSD() }.sumOf { it.amountUYU } else 0.0) +
        (if (cardExpanded) filteredCardExpenses.filter { !it.isInUSD() }.sumOf { it.amountUYU } else 0.0) +
        (if (debtExpanded) filteredDebts.filter { !it.isInUSD() }.sumOf { it.amountUYU } else 0.0)
    val visibleTotalUSD =
        (if (variableExpanded) filteredVariableExpenses.filter { it.isInUSD() }.sumOf { it.amountUSD } else 0.0) +
        (if (cardExpanded) filteredCardExpenses.filter { it.isInUSD() }.sumOf { it.amountUSD } else 0.0) +
        (if (debtExpanded) filteredDebts.filter { it.isInUSD() }.sumOf { it.amountUSD } else 0.0)
    val visibleTotalCalculated = visibleTotalUYU + (visibleTotalUSD * cardRate)

    fun toggleSection(section: ExpenseSection) {
        collapsedSections = if (collapsedSections.contains(section)) {
            collapsedSections - section
        } else {
            collapsedSections + section
        }
    }

    fun toggleSelection(key: String) {
        selectedExpenseKeys = if (selectedExpenseKeys.contains(key)) {
            selectedExpenseKeys - key
        } else {
            selectedExpenseKeys + key
        }
    }

    Scaffold(
        floatingActionButton = {
            if (!isReadOnly && selectedExpenseKeys.isEmpty()) {
                Column(horizontalAlignment = Alignment.End) {
                    DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.expenses_menu_add_fixed)) }, onClick = { showAddMenu = false; showAddFixed = true })
                        DropdownMenuItem(text = { Text(stringResource(R.string.expenses_menu_add_variable)) }, onClick = { showAddMenu = false; showAddVariable = true })
                        DropdownMenuItem(text = { Text(stringResource(R.string.expenses_menu_add_card)) }, onClick = { showAddMenu = false; showAddCard = true })
                        DropdownMenuItem(text = { Text(stringResource(R.string.expenses_menu_add_debt)) }, onClick = { showAddMenu = false; showAddDebt = true })
                    }
                    FloatingActionButton(onClick = { showAddMenu = true }) {
                        Icon(Icons.Default.Add, stringResource(R.string.expenses_add_open_menu_cd))
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            if (isReadOnly) item { ReadOnlyBanner() }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MonthHeader(yearMonth = yearMonth)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.isNotBlank()) collapsedSections = emptySet()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.expenses_search_label)) },
                        singleLine = true
                    )
                    if (!isReadOnly && selectedExpenseKeys.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.selection_count, selectedExpenseKeys.size),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(
                                onClick = {
                                    viewModel.deleteSelected(selectedExpenseKeys)
                                    selectedExpenseKeys = emptySet()
                                }
                            ) {
                                Text(stringResource(R.string.action_delete_selected))
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = {
                            collapsedSections = if (areAllCollapsed) emptySet() else allSections
                        }) {
                            Text(
                                stringResource(
                                    if (areAllCollapsed) R.string.expenses_expand_all
                                    else R.string.expenses_collapse_all
                                )
                            )
                        }
                    }
                    SummaryRowCompact(
                        label = stringResource(R.string.expenses_filtered_total_uyu),
                        value = visibleTotalUYU.formatUYU()
                    )
                    SummaryRowCompact(
                        label = stringResource(R.string.expenses_filtered_total_usd),
                        value = visibleTotalUSD.formatUSD()
                    )
                    SummaryRowCompact(
                        label = stringResource(R.string.expenses_filtered_total),
                        value = visibleTotalCalculated.formatUYU()
                    )
                }
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.expenses_section_fixed),
                    total = filteredFixedExpenses.sumOf { it.amountUYU }.formatUYU(),
                    expanded = fixedExpanded,
                    onClick = { toggleSection(ExpenseSection.FIXED) }
                )
            }
            if (fixedExpanded) {
                items(filteredFixedExpenses, key = { it.id }) { expense ->
                    val selectionKey = "fixed:${expense.id}"
                    FixedExpenseRow(
                        expense = expense,
                        readOnly = isReadOnly,
                        selected = selectedExpenseKeys.contains(selectionKey),
                        selectionMode = selectedExpenseKeys.isNotEmpty(),
                        onToggleSelection = { toggleSelection(selectionKey) },
                        onEdit = { editFixed = expense },
                        onDelete = { viewModel.deleteFixed(expense.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)); HorizontalDivider() }

            item {
                SectionHeader(
                    title = stringResource(R.string.expenses_section_variable),
                    total = filteredVariableExpenses.sumOf { it.totalUYU(cardRate) }.formatUYU(),
                    expanded = variableExpanded,
                    onClick = { toggleSection(ExpenseSection.VARIABLE) }
                )
            }
            if (variableExpanded) {
                items(filteredVariableExpenses, key = { it.id }) { expense ->
                    val selectionKey = "variable:${expense.id}"
                    MoneyEntryRow(
                        entry = expense,
                        exchangeRate = cardRate,
                        fallback = stringResource(R.string.expenses_variable_item_fallback),
                        readOnly = isReadOnly,
                        selected = selectedExpenseKeys.contains(selectionKey),
                        selectionMode = selectedExpenseKeys.isNotEmpty(),
                        onToggleSelection = { toggleSelection(selectionKey) },
                        onEdit = { editVariable = expense },
                        onDelete = { viewModel.deleteVariableExpense(expense.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)); HorizontalDivider() }

            item {
                SectionHeader(
                    title = stringResource(R.string.expenses_section_card),
                    total = filteredCardExpenses.sumOf { it.totalUYU(cardRate) }.formatUYU(),
                    expanded = cardExpanded,
                    onClick = { toggleSection(ExpenseSection.CARD) }
                )
            }
            if (cardExpanded) {
                items(filteredCardExpenses, key = { it.id }) { expense ->
                    val selectionKey = "card:${expense.id}"
                    CardExpenseRow(
                        expense = expense,
                        cardRate = cardRate,
                        readOnly = isReadOnly,
                        selected = selectedExpenseKeys.contains(selectionKey),
                        selectionMode = selectedExpenseKeys.isNotEmpty(),
                        onToggleSelection = { toggleSelection(selectionKey) },
                        onEdit = { editCard = expense },
                        onDelete = { viewModel.deleteCard(expense.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)); HorizontalDivider() }

            item {
                SectionHeader(
                    title = stringResource(R.string.expenses_section_debt),
                    total = filteredDebts.sumOf { it.totalUYU(cardRate) }.formatUYU(),
                    expanded = debtExpanded,
                    onClick = { toggleSection(ExpenseSection.DEBT) }
                )
            }
            if (debtExpanded) {
                items(filteredDebts, key = { it.id }) { debt ->
                    val selectionKey = "debt:${debt.id}"
                    DebtRow(
                        debt = debt,
                        exchangeRate = cardRate,
                        readOnly = isReadOnly,
                        selected = selectedExpenseKeys.contains(selectionKey),
                        selectionMode = selectedExpenseKeys.isNotEmpty(),
                        onToggleSelection = { toggleSelection(selectionKey) },
                        onEdit = { editDebt = debt },
                        onDelete = { viewModel.deleteDebt(debt.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAddFixed || editFixed != null) {
        FixedExpenseDialog(
            initial = editFixed,
            categories = categories,
            onCreateCategory = viewModel::addCategory,
            onConfirm = { expense -> viewModel.upsertFixed(expense); showAddFixed = false; editFixed = null },
            onDismiss = { showAddFixed = false; editFixed = null }
        )
    }
    if (showAddVariable || editVariable != null) {
        MoneyEntryDialog(
            title = stringResource(if (editVariable == null) R.string.dialog_variable_expense_title_new else R.string.dialog_variable_expense_title_edit),
            initial = editVariable,
            categories = categories,
            onCreateCategory = viewModel::addCategory,
            allowApplyToFuture = false,
            onConfirm = { expense, _ -> viewModel.upsertVariableExpense(expense); showAddVariable = false; editVariable = null },
            onDismiss = { showAddVariable = false; editVariable = null }
        )
    }
    if (showAddCard || editCard != null) {
        CardExpenseDialog(
            initial = editCard,
            categories = categories,
            onCreateCategory = viewModel::addCategory,
            onConfirm = { expense -> viewModel.upsertCard(expense); showAddCard = false; editCard = null },
            onDismiss = { showAddCard = false; editCard = null }
        )
    }
    if (showAddDebt || editDebt != null) {
        DebtDialog(
            initial = editDebt,
            categories = categories,
            onCreateCategory = viewModel::addCategory,
            onConfirm = { debt -> viewModel.upsertDebt(debt); showAddDebt = false; editDebt = null },
            onDismiss = { showAddDebt = false; editDebt = null }
        )
    }
}

@Composable
private fun SummaryRowCompact(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FixedExpenseRow(
    expense: FixedExpense,
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
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        ),
        leadingContent = {
            Icon(
                imageVector = categoryIconFor(expense.category),
                contentDescription = expense.category.ifBlank { null },
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = { Text(expense.name) },
        supportingContent = {
            Column {
                if (expense.category.isNotBlank()) {
                    Text(expense.category, style = MaterialTheme.typography.labelSmall)
                }
                Text(stringResource(R.string.expenses_fixed_recurring_hint), style = MaterialTheme.typography.labelSmall)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(expense.amountUYU.formatUYU(), fontWeight = FontWeight.Medium)
                if (!readOnly && !selectionMode) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(R.string.action_edit), Modifier.size(18.dp)) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, stringResource(R.string.action_delete), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    )
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MoneyEntryRow(
    entry: MoneyEntry,
    exchangeRate: Double,
    fallback: String,
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
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        ),
        leadingContent = {
            Icon(
                imageVector = categoryIconFor(entry.category),
                contentDescription = entry.category.ifBlank { null },
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = { Text(entry.name.ifBlank { fallback }) },
        supportingContent = {
            Column {
                if (entry.category.isNotBlank()) {
                    Text(entry.category, style = MaterialTheme.typography.labelSmall)
                }
                if (entry.isInUSD()) {
                    Text("${entry.amountUSD.formatUSD()} x ${"%.2f".format(exchangeRate)}", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.totalUYU(exchangeRate).formatUYU(), fontWeight = FontWeight.Medium)
                if (!readOnly && !selectionMode) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(R.string.action_edit), Modifier.size(18.dp)) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, stringResource(R.string.action_delete), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    )
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun CardExpenseRow(
    expense: CardExpense,
    cardRate: Double,
    readOnly: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val supporting = when (expense.kind) {
        CardExpenseKind.RECURRING -> stringResource(R.string.card_kind_recurring)
        CardExpenseKind.INSTALLMENT -> stringResource(R.string.card_installment_label, expense.currentInstallment, expense.totalInstallments)
        else -> stringResource(R.string.card_kind_punctual)
    }
    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = { if (!readOnly && selectionMode) onToggleSelection() },
            onLongClick = { if (!readOnly) onToggleSelection() }
        ),
        colors = ListItemDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        ),
        leadingContent = {
            Icon(
                imageVector = categoryIconFor(expense.category),
                contentDescription = expense.category.ifBlank { null },
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = { Text(expense.name) },
        supportingContent = {
            Column {
                if (expense.category.isNotBlank()) {
                    Text(expense.category, style = MaterialTheme.typography.labelSmall)
                }
                Text(supporting, style = MaterialTheme.typography.labelSmall)
                if (expense.isInUSD()) Text("${expense.amountUSD.formatUSD()} x ${"%.2f".format(cardRate)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(expense.totalUYU(cardRate).formatUYU(), fontWeight = FontWeight.Medium)
                if (!readOnly && !selectionMode) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(R.string.action_edit), Modifier.size(18.dp)) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, stringResource(R.string.action_delete), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    )
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DebtRow(
    debt: DebtEntry,
    exchangeRate: Double,
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
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        ),
        leadingContent = {
            Icon(
                imageVector = categoryIconFor(debt.category),
                contentDescription = debt.category.ifBlank { null },
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = { Text(debt.name) },
        supportingContent = {
            Column {
                if (debt.category.isNotBlank()) {
                    Text(debt.category, style = MaterialTheme.typography.labelSmall)
                }
                Text(stringResource(R.string.debt_installment_label, debt.currentInstallment, debt.totalInstallments), style = MaterialTheme.typography.labelSmall)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(debt.totalUYU(exchangeRate).formatUYU(), fontWeight = FontWeight.Medium)
                if (!readOnly && !selectionMode) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(R.string.action_edit), Modifier.size(18.dp)) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, stringResource(R.string.action_delete), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    )
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun FixedExpenseDialog(
    initial: FixedExpense?,
    categories: List<String>,
    onCreateCategory: (String) -> Unit,
    onConfirm: (FixedExpense) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var amount by remember { mutableStateOf(initial?.amountUYU?.toInputAmount() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.dialog_fixed_title_new else R.string.dialog_fixed_title_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.dialog_fixed_name_label)) }, singleLine = true)
                CategoryInput(
                    value = category,
                    onValueChange = { category = it },
                    categories = categories
                )
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(stringResource(R.string.dialog_fixed_amount_label)) }, prefix = { Text(stringResource(R.string.prefix_uyu)) }, singleLine = true)
                Text(stringResource(R.string.dialog_fixed_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        },
        confirmButton = {
            Button(onClick = {
                val resolvedCategory = category.trim()
                if (resolvedCategory.isNotBlank()) onCreateCategory(resolvedCategory)
                onConfirm(FixedExpense(id = initial?.id ?: UUID.randomUUID().toString(), name = name, category = resolvedCategory, amountUYU = amount.replace(",", ".").toDoubleOrNull() ?: 0.0, isPinned = true))
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun MoneyEntryDialog(
    title: String,
    initial: MoneyEntry?,
    categories: List<String>,
    onCreateCategory: (String) -> Unit,
    allowApplyToFuture: Boolean,
    onConfirm: (MoneyEntry, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
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
    var applyFuture by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.common_optional_name)) }, singleLine = true)
                CategoryInput(
                    value = category,
                    onValueChange = { category = it },
                    categories = categories
                )
                CurrencySelector(isUSD = isUSD, onChange = { isUSD = it })
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(stringResource(R.string.common_amount_label)) }, prefix = { Text(stringResource(if (isUSD) R.string.prefix_usd else R.string.prefix_uyu)) }, singleLine = true)
                if (allowApplyToFuture) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = applyFuture, onCheckedChange = { applyFuture = it })
                        Text(stringResource(R.string.common_apply_to_future))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = amount.replace(",", ".").toDoubleOrNull() ?: 0.0
                val resolvedCategory = category.trim()
                if (resolvedCategory.isNotBlank()) onCreateCategory(resolvedCategory)
                onConfirm(
                    MoneyEntry(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        category = resolvedCategory,
                        amountUSD = if (isUSD) parsed else 0.0,
                        amountUYU = if (isUSD) 0.0 else parsed,
                        isUSD = isUSD,
                        currency = if (isUSD) IncomeCurrency.USD else IncomeCurrency.UYU
                    ),
                    applyFuture
                )
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun CardExpenseDialog(
    initial: CardExpense?,
    categories: List<String>,
    onCreateCategory: (String) -> Unit,
    onConfirm: (CardExpense) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
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
    var kind by remember { mutableStateOf(initial?.kind ?: CardExpenseKind.PUNCTUAL) }
    var totalInst by remember { mutableStateOf(initial?.totalInstallments?.toString() ?: "1") }
    var currentInst by remember { mutableStateOf(initial?.currentInstallment?.toString() ?: "1") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.dialog_card_title_new else R.string.dialog_card_title_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.dialog_card_name_label)) }, singleLine = true)
                CategoryInput(
                    value = category,
                    onValueChange = { category = it },
                    categories = categories
                )
                CurrencySelector(isUSD = isUSD, onChange = { isUSD = it })
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(stringResource(R.string.dialog_card_amount_label)) }, prefix = { Text(stringResource(if (isUSD) R.string.prefix_usd else R.string.prefix_uyu)) }, singleLine = true)
                Text(stringResource(R.string.dialog_card_kind_label), style = MaterialTheme.typography.labelMedium)
                CardKindSelector(kind = kind, onSelected = { kind = it })
                if (kind == CardExpenseKind.INSTALLMENT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = currentInst, onValueChange = { currentInst = it }, label = { Text(stringResource(R.string.dialog_card_installment_number)) }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = totalInst, onValueChange = { totalInst = it }, label = { Text(stringResource(R.string.dialog_card_installment_total)) }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = amount.replace(",", ".").toDoubleOrNull() ?: 0.0
                val resolvedCategory = category.trim()
                if (resolvedCategory.isNotBlank()) onCreateCategory(resolvedCategory)
                onConfirm(
                    CardExpense(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        category = resolvedCategory,
                        amountUSD = if (isUSD) value else 0.0,
                        amountUYU = if (isUSD) 0.0 else value,
                        isUSD = isUSD,
                        currency = if (isUSD) IncomeCurrency.USD else IncomeCurrency.UYU,
                        kind = kind,
                        totalInstallments = if (kind == CardExpenseKind.INSTALLMENT) totalInst.toIntOrNull() ?: 1 else 1,
                        currentInstallment = if (kind == CardExpenseKind.INSTALLMENT) currentInst.toIntOrNull() ?: 1 else 1
                    )
                )
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun CardKindSelector(kind: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CardKindOption(kind == CardExpenseKind.PUNCTUAL, stringResource(R.string.card_kind_punctual)) { onSelected(CardExpenseKind.PUNCTUAL) }
        CardKindOption(kind == CardExpenseKind.RECURRING, stringResource(R.string.card_kind_recurring)) { onSelected(CardExpenseKind.RECURRING) }
        CardKindOption(kind == CardExpenseKind.INSTALLMENT, stringResource(R.string.card_kind_installment)) { onSelected(CardExpenseKind.INSTALLMENT) }
    }
}

@Composable
private fun CardKindOption(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = selected, onCheckedChange = { if (it) onClick() })
        Text(label)
    }
}

@Composable
private fun DebtDialog(
    initial: DebtEntry?,
    categories: List<String>,
    onCreateCategory: (String) -> Unit,
    onConfirm: (DebtEntry) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
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
    var totalInst by remember { mutableStateOf(initial?.totalInstallments?.toString() ?: "1") }
    var currentInst by remember { mutableStateOf(initial?.currentInstallment?.toString() ?: "1") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.dialog_debt_title_new else R.string.dialog_debt_title_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.dialog_debt_name_label)) }, singleLine = true)
                CategoryInput(
                    value = category,
                    onValueChange = { category = it },
                    categories = categories
                )
                CurrencySelector(isUSD = isUSD, onChange = { isUSD = it })
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(stringResource(R.string.dialog_debt_amount_label)) }, prefix = { Text(stringResource(if (isUSD) R.string.prefix_usd else R.string.prefix_uyu)) }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = currentInst, onValueChange = { currentInst = it }, label = { Text(stringResource(R.string.dialog_debt_installment_number)) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = totalInst, onValueChange = { totalInst = it }, label = { Text(stringResource(R.string.dialog_debt_installment_total)) }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = amount.replace(",", ".").toDoubleOrNull() ?: 0.0
                val resolvedCategory = category.trim()
                if (resolvedCategory.isNotBlank()) onCreateCategory(resolvedCategory)
                onConfirm(
                    DebtEntry(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        category = resolvedCategory,
                        amountUSD = if (isUSD) value else 0.0,
                        amountUYU = if (isUSD) 0.0 else value,
                        isUSD = isUSD,
                        currency = if (isUSD) IncomeCurrency.USD else IncomeCurrency.UYU,
                        totalInstallments = totalInst.toIntOrNull() ?: 1,
                        currentInstallment = currentInst.toIntOrNull() ?: 1
                    )
                )
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun CategoryInput(
    value: String,
    onValueChange: (String) -> Unit,
    categories: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    var useCustomCategory by remember(value) {
        mutableStateOf(value.isNotBlank() && categories.none { it.equals(value, ignoreCase = true) })
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                label = { Text(stringResource(R.string.common_category_label)) },
                supportingText = {
                    Text(
                        if (useCustomCategory) {
                            stringResource(R.string.common_category_custom_hint)
                        } else {
                            stringResource(R.string.common_category_hint)
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = categoryIconFor(value),
                        contentDescription = value.ifBlank { null }
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.common_category_open))
                    }
                },
                singleLine = true
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = true }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        leadingIcon = {
                            Icon(
                                imageVector = categoryIconFor(category),
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onValueChange(category)
                            useCustomCategory = false
                            expanded = false
                        }
                    )
                }
            }
        }
        TextButton(onClick = { useCustomCategory = !useCustomCategory }) {
            Text(
                stringResource(
                    if (useCustomCategory) R.string.common_category_use_list
                    else R.string.common_category_new
                )
            )
        }
        if (useCustomCategory) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.common_category_custom_label)) },
                leadingIcon = {
                    Icon(
                        imageVector = categoryIconFor(value),
                        contentDescription = value.ifBlank { null }
                    )
                },
                singleLine = true
            )
        }
    }
}

private fun categoryIconFor(category: String): ImageVector {
    return when (category.trim().lowercase()) {
        "comida" -> Icons.Default.Restaurant
        "ropa" -> Icons.Default.ShoppingCart
        "transporte" -> Icons.Default.DirectionsCar
        "entretenimiento" -> Icons.Default.Movie
        "servicios públicos", "servicios publicos" -> Icons.Default.Bolt
        "salud" -> Icons.Default.LocalHospital
        "streaming" -> Icons.Default.Subscriptions
        "educación", "educacion" -> Icons.Default.School
        "higiene" -> Icons.Default.CleaningServices
        "belleza" -> Icons.Default.Face
        "reparaciones" -> Icons.Default.Build
        "hogar", "alquiler" -> Icons.Default.Home
        "impuestos" -> Icons.Default.ReceiptLong
        "mascotas" -> Icons.Default.Pets
        else -> Icons.Default.Category
    }
}

@Composable
private fun CurrencySelector(isUSD: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = !isUSD, onClick = { onChange(false) }, label = { Text(stringResource(R.string.common_currency_uyu_short)) })
        FilterChip(selected = isUSD, onClick = { onChange(true) }, label = { Text(stringResource(R.string.common_currency_usd_short)) })
    }
}
