package com.finanzasfamiliares.ui.screens.expenses

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pets
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finanzasfamiliares.R
import com.finanzasfamiliares.data.model.CardExpense
import com.finanzasfamiliares.data.model.CardExpenseKind
import com.finanzasfamiliares.data.model.DebtEntry
import com.finanzasfamiliares.data.model.FixedExpense
import com.finanzasfamiliares.data.model.IncomeCurrency
import com.finanzasfamiliares.data.model.MoneyEntry
import com.finanzasfamiliares.ui.components.FinanceCard
import com.finanzasfamiliares.ui.components.MonthHeader
import com.finanzasfamiliares.ui.components.MonthSwipeContainer
import com.finanzasfamiliares.ui.components.ReadOnlyBanner
import com.finanzasfamiliares.ui.components.SectionHeader
import com.finanzasfamiliares.ui.components.SelectionActionBar
import com.finanzasfamiliares.ui.components.SoftIconBadge
import com.finanzasfamiliares.ui.components.clearZeroOnFocus
import com.finanzasfamiliares.ui.components.formatUSD
import com.finanzasfamiliares.ui.components.formatUYU
import com.finanzasfamiliares.ui.components.toInputAmount
import java.text.Normalizer
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private enum class ExpenseSection {
    FIXED, VARIABLE, CARD, DEBT
}

private val DefaultCollapsedExpenseSections = setOf(
    ExpenseSection.FIXED,
    ExpenseSection.VARIABLE,
    ExpenseSection.CARD,
    ExpenseSection.DEBT
)

private enum class PaymentFilter {
    ALL, PENDING, PAID
}

private val PaidBackgroundColor = Color(0xFFE0F4E5)
private val PaidExpenseContentColor = Color(0xFF173D2B)

private sealed interface ExpenseDeleteRequest {
    data class Fixed(val expense: FixedExpense) : ExpenseDeleteRequest
    data class Variable(val expense: MoneyEntry) : ExpenseDeleteRequest
    data class Card(val expense: CardExpense) : ExpenseDeleteRequest
    data class Debt(val debt: DebtEntry) : ExpenseDeleteRequest
    data class Bulk(val keys: Set<String>) : ExpenseDeleteRequest
}

private data class DuplicateExpenseCandidate(
    val normalizedName: String,
    val typeLabel: String,
    val amountLabel: String
)

private data class DuplicateExpenseMatch(
    val typeLabel: String,
    val amountLabel: String
)

private val DuplicateWhitespaceRegex = "\\s+".toRegex()
private val DuplicateDiacriticsRegex = "\\p{Mn}+".toRegex()

private fun normalizeExpenseNameForDuplicateCheck(value: String): String {
    val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
    return normalized
        .replace(DuplicateDiacriticsRegex, "")
        .lowercase()
        .replace(DuplicateWhitespaceRegex, " ")
        .trim()
}

private fun amountSearchTokens(value: Double): Set<String> {
    val formatted = String.format(Locale.US, "%.2f", value)
    val trimmed = formatted.removeSuffix(".00")
    val integerPart = formatted.substringBefore(".")
    return buildSet {
        add(formatted)
        add(formatted.replace(".", ","))
        add(trimmed)
        add(trimmed.replace(".", ","))
        add(integerPart)
    }
}

private fun matchesExpenseSearch(
    rawQuery: String,
    textFields: List<String>,
    numericFields: List<Double> = emptyList()
): Boolean {
    val query = rawQuery.trim()
    if (query.isBlank()) return true

    val normalizedQuery = normalizeExpenseNameForDuplicateCheck(query)
    if (textFields.any { normalizeExpenseNameForDuplicateCheck(it).contains(normalizedQuery) }) {
        return true
    }

    val amountQuery = query
        .lowercase(Locale.ROOT)
        .replace("u\$s", "")
        .replace("us\$", "")
        .replace("$", "")
        .replace(" ", "")

    if (amountQuery.isBlank()) return false

    return numericFields.any { amount ->
        amountSearchTokens(amount).any { token -> token.contains(amountQuery) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpensesScreen(
    yearMonth: String,
    canGoPreviousMonth: Boolean = false,
    canGoNextMonth: Boolean = false,
    onGoPreviousMonth: () -> Unit = {},
    onGoNextMonth: () -> Unit = {},
    headerPinned: Boolean = true,
    headerContent: @Composable () -> Unit = {},
    viewModel: ExpensesViewModel = hiltViewModel()
) {
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
    var deleteRequest by remember { mutableStateOf<ExpenseDeleteRequest?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var paymentFilter by remember { mutableStateOf(PaymentFilter.ALL) }
    var collapsedSections by remember { mutableStateOf(DefaultCollapsedExpenseSections) }
    var selectedExpenseKeys by remember { mutableStateOf(setOf<String>()) }
    val cardRate = data?.cardExchangeRate ?: 0.0
    val normalizedQuery = searchQuery.trim()
    val allSections = remember {
        setOf(
            ExpenseSection.FIXED,
            ExpenseSection.VARIABLE,
            ExpenseSection.CARD,
            ExpenseSection.DEBT
        )
    }

    fun matchesPayment(isPaid: Boolean): Boolean = when (paymentFilter) {
        PaymentFilter.ALL -> true
        PaymentFilter.PENDING -> !isPaid
        PaymentFilter.PAID -> isPaid
    }

    val fixedExpenses = data?.fixedExpenses ?: emptyList()
    val variableExpenses = data?.variableExpenses ?: emptyList()
    val cardExpenses = data?.cardExpenses ?: emptyList()
    val debts = data?.debts ?: emptyList()
    val fixedTypeLabel = stringResource(R.string.expenses_section_fixed)
    val variableTypeLabel = stringResource(R.string.expenses_section_variable)
    val cardTypeLabel = stringResource(R.string.expenses_section_card)
    val debtTypeLabel = stringResource(R.string.expenses_section_debt)
    val cardPunctualLabel = stringResource(R.string.card_kind_punctual)
    val cardRecurringLabel = stringResource(R.string.card_kind_recurring)
    val cardInstallmentLabel = stringResource(R.string.card_kind_installment)
    val usdShortLabel = stringResource(R.string.common_currency_usd_short)
    val uyuShortLabel = stringResource(R.string.common_currency_uyu_short)
    val debtInstallmentsLabel = stringResource(R.string.dialog_debt_installment_total)

    fun formatDuplicateAmount(isUSD: Boolean, amountUSD: Double, totalUYU: Double): String =
        if (isUSD) {
            "${amountUSD.formatUSD()} (${totalUYU.formatUYU()})"
        } else {
            totalUYU.formatUYU()
        }

    val duplicateCandidates = remember(
        fixedExpenses,
        variableExpenses,
        cardExpenses,
        debts,
        cardRate,
        fixedTypeLabel,
        variableTypeLabel,
        cardTypeLabel,
        debtTypeLabel,
        cardPunctualLabel,
        cardRecurringLabel,
        cardInstallmentLabel
    ) {
        buildList {
            fixedExpenses.forEach { expense ->
                add(
                    DuplicateExpenseCandidate(
                        normalizedName = normalizeExpenseNameForDuplicateCheck(expense.name),
                        typeLabel = fixedTypeLabel,
                        amountLabel = formatDuplicateAmount(expense.isInUSD(), expense.amountUSD, expense.totalUYU(cardRate))
                    )
                )
            }
            variableExpenses.forEach { expense ->
                add(
                    DuplicateExpenseCandidate(
                        normalizedName = normalizeExpenseNameForDuplicateCheck(expense.name),
                        typeLabel = variableTypeLabel,
                        amountLabel = formatDuplicateAmount(expense.isInUSD(), expense.amountUSD, expense.totalUYU(cardRate))
                    )
                )
            }
            cardExpenses.forEach { expense ->
                val cardKindLabel = when (expense.kind) {
                    CardExpenseKind.RECURRING -> cardRecurringLabel
                    CardExpenseKind.INSTALLMENT -> cardInstallmentLabel
                    else -> cardPunctualLabel
                }
                add(
                    DuplicateExpenseCandidate(
                        normalizedName = normalizeExpenseNameForDuplicateCheck(expense.name),
                        typeLabel = "$cardTypeLabel - $cardKindLabel",
                        amountLabel = formatDuplicateAmount(expense.isInUSD(), expense.amountUSD, expense.totalUYU(cardRate))
                    )
                )
            }
            debts.forEach { debt ->
                add(
                    DuplicateExpenseCandidate(
                        normalizedName = normalizeExpenseNameForDuplicateCheck(debt.name),
                        typeLabel = debtTypeLabel,
                        amountLabel = formatDuplicateAmount(debt.isInUSD(), debt.amountUSD, debt.totalUYU(cardRate))
                    )
                )
            }
        }.filter { it.normalizedName.isNotBlank() }
    }

    fun findDuplicateMatches(name: String): List<DuplicateExpenseMatch> {
        val normalizedName = normalizeExpenseNameForDuplicateCheck(name)
        if (normalizedName.isBlank()) return emptyList()
        return duplicateCandidates
            .filter { it.normalizedName == normalizedName }
            .map { DuplicateExpenseMatch(typeLabel = it.typeLabel, amountLabel = it.amountLabel) }
    }

    val matchedFixedExpenses = remember(
        fixedExpenses,
        normalizedQuery,
        cardRate,
        fixedTypeLabel,
        usdShortLabel,
        uyuShortLabel
    ) {
        fixedExpenses.filter {
            matchesExpenseSearch(
                rawQuery = normalizedQuery,
                textFields = listOf(
                    it.name,
                    it.category,
                    fixedTypeLabel,
                    if (it.isInUSD()) usdShortLabel else uyuShortLabel
                ),
                numericFields = buildList {
                    add(it.amountUYU)
                    add(it.totalUYU(cardRate))
                    if (it.isInUSD()) add(it.amountUSD)
                }
            )
        }
    }
    val matchedVariableExpenses = remember(
        variableExpenses,
        normalizedQuery,
        cardRate,
        variableTypeLabel,
        usdShortLabel,
        uyuShortLabel
    ) {
        variableExpenses.filter {
            matchesExpenseSearch(
                rawQuery = normalizedQuery,
                textFields = listOf(
                    it.name,
                    it.category,
                    variableTypeLabel,
                    if (it.isInUSD()) usdShortLabel else uyuShortLabel
                ),
                numericFields = buildList {
                    add(it.amountUYU)
                    add(it.totalUYU(cardRate))
                    if (it.isInUSD()) add(it.amountUSD)
                }
            )
        }
    }
    val matchedCardExpenses = remember(
        cardExpenses,
        normalizedQuery,
        cardRate,
        cardTypeLabel,
        cardPunctualLabel,
        cardRecurringLabel,
        cardInstallmentLabel,
        usdShortLabel,
        uyuShortLabel
    ) {
        cardExpenses.filter { expense ->
            val kindLabel = when (expense.kind) {
                CardExpenseKind.RECURRING -> cardRecurringLabel
                CardExpenseKind.INSTALLMENT -> cardInstallmentLabel
                else -> cardPunctualLabel
            }
            matchesExpenseSearch(
                rawQuery = normalizedQuery,
                textFields = listOf(
                    expense.name,
                    expense.category,
                    cardTypeLabel,
                    kindLabel,
                    if (expense.isInUSD()) usdShortLabel else uyuShortLabel
                ),
                numericFields = buildList {
                    add(expense.amountUYU)
                    add(expense.totalUYU(cardRate))
                    if (expense.isInUSD()) add(expense.amountUSD)
                }
            )
        }
    }
    val matchedDebts = remember(
        debts,
        normalizedQuery,
        cardRate,
        debtTypeLabel,
        debtInstallmentsLabel,
        usdShortLabel,
        uyuShortLabel
    ) {
        debts.filter {
            matchesExpenseSearch(
                rawQuery = normalizedQuery,
                textFields = listOf(
                    it.name,
                    it.category,
                    debtTypeLabel,
                    debtInstallmentsLabel,
                    if (it.isInUSD()) usdShortLabel else uyuShortLabel
                ),
                numericFields = buildList {
                    add(it.amountUYU)
                    add(it.totalUYU(cardRate))
                    if (it.isInUSD()) add(it.amountUSD)
                }
            )
        }
    }
    val filteredFixedExpenses = remember(matchedFixedExpenses, paymentFilter) {
        matchedFixedExpenses.filter { matchesPayment(it.isPaid) }
    }
    val filteredVariableExpenses = remember(matchedVariableExpenses, paymentFilter) {
        matchedVariableExpenses.filter { matchesPayment(it.isPaid) }
    }
    val filteredCardExpenses = remember(matchedCardExpenses, paymentFilter) {
        matchedCardExpenses.filter { matchesPayment(it.isPaid) }
    }
    val filteredDebts = remember(matchedDebts, paymentFilter) {
        matchedDebts.filter { matchesPayment(it.isPaid) }
    }
    val hasActiveVisibilityFilter = normalizedQuery.isNotBlank() || paymentFilter != PaymentFilter.ALL
    val sectionsWithVisibleResults = remember(
        filteredFixedExpenses,
        filteredVariableExpenses,
        filteredCardExpenses,
        filteredDebts
    ) {
        buildSet {
            if (filteredFixedExpenses.isNotEmpty()) add(ExpenseSection.FIXED)
            if (filteredVariableExpenses.isNotEmpty()) add(ExpenseSection.VARIABLE)
            if (filteredCardExpenses.isNotEmpty()) add(ExpenseSection.CARD)
            if (filteredDebts.isNotEmpty()) add(ExpenseSection.DEBT)
        }
    }
    val effectiveCollapsedSections = remember(
        collapsedSections,
        hasActiveVisibilityFilter,
        allSections,
        sectionsWithVisibleResults
    ) {
        if (hasActiveVisibilityFilter) {
            allSections - sectionsWithVisibleResults
        } else {
            collapsedSections
        }
    }
    val allMonthCardExpenses = cardExpenses
    val areAllCollapsed = effectiveCollapsedSections.size == allSections.size
    val fixedExpanded = !effectiveCollapsedSections.contains(ExpenseSection.FIXED)
    val variableExpanded = !effectiveCollapsedSections.contains(ExpenseSection.VARIABLE)
    val cardExpanded = !effectiveCollapsedSections.contains(ExpenseSection.CARD)
    val debtExpanded = !effectiveCollapsedSections.contains(ExpenseSection.DEBT)
    val allCardPaid = allMonthCardExpenses.isNotEmpty() && allMonthCardExpenses.all { it.isPaid }
    val anyCardPaid = allMonthCardExpenses.any { it.isPaid }
    val visibleTotalUYU = remember(
        filteredFixedExpenses,
        filteredVariableExpenses,
        filteredCardExpenses,
        filteredDebts,
        fixedExpanded,
        variableExpanded,
        cardExpanded,
        debtExpanded
    ) {
        (if (fixedExpanded) filteredFixedExpenses.filter { !it.isInUSD() }.sumOf { it.amountUYU } else 0.0) +
            (if (variableExpanded) filteredVariableExpenses.filter { !it.isInUSD() }.sumOf { it.amountUYU } else 0.0) +
            (if (cardExpanded) filteredCardExpenses.filter { !it.isInUSD() }.sumOf { it.amountUYU } else 0.0) +
            (if (debtExpanded) filteredDebts.filter { !it.isInUSD() }.sumOf { it.amountUYU } else 0.0)
    }
    val visibleTotalUSD = remember(
        filteredFixedExpenses,
        filteredVariableExpenses,
        filteredCardExpenses,
        filteredDebts,
        fixedExpanded,
        variableExpanded,
        cardExpanded,
        debtExpanded
    ) {
        (if (fixedExpanded) filteredFixedExpenses.filter { it.isInUSD() }.sumOf { it.amountUSD } else 0.0) +
        (if (variableExpanded) filteredVariableExpenses.filter { it.isInUSD() }.sumOf { it.amountUSD } else 0.0) +
            (if (cardExpanded) filteredCardExpenses.filter { it.isInUSD() }.sumOf { it.amountUSD } else 0.0) +
            (if (debtExpanded) filteredDebts.filter { it.isInUSD() }.sumOf { it.amountUSD } else 0.0)
    }
    val visibleTotalCalculated = remember(visibleTotalUYU, visibleTotalUSD, cardRate) {
        visibleTotalUYU + (visibleTotalUSD * cardRate)
    }
    val paidTotalCalculated = remember(
        matchedFixedExpenses,
        matchedVariableExpenses,
        matchedCardExpenses,
        matchedDebts,
        fixedExpanded,
        variableExpanded,
        cardExpanded,
        debtExpanded,
        cardRate
    ) {
        (if (fixedExpanded) matchedFixedExpenses.filter { it.isPaid }.sumOf { it.totalUYU(cardRate) } else 0.0) +
            (if (variableExpanded) matchedVariableExpenses.filter { it.isPaid }.sumOf { it.totalUYU(cardRate) } else 0.0) +
            (if (cardExpanded) matchedCardExpenses.filter { it.isPaid }.sumOf { it.totalUYU(cardRate) } else 0.0) +
            (if (debtExpanded) matchedDebts.filter { it.isPaid }.sumOf { it.totalUYU(cardRate) } else 0.0)
    }
    val pendingTotalCalculated = remember(
        matchedFixedExpenses,
        matchedVariableExpenses,
        matchedCardExpenses,
        matchedDebts,
        fixedExpanded,
        variableExpanded,
        cardExpanded,
        debtExpanded,
        cardRate
    ) {
        (if (fixedExpanded) matchedFixedExpenses.filter { !it.isPaid }.sumOf { it.totalUYU(cardRate) } else 0.0) +
            (if (variableExpanded) matchedVariableExpenses.filter { !it.isPaid }.sumOf { it.totalUYU(cardRate) } else 0.0) +
            (if (cardExpanded) matchedCardExpenses.filter { !it.isPaid }.sumOf { it.totalUYU(cardRate) } else 0.0) +
            (if (debtExpanded) matchedDebts.filter { !it.isPaid }.sumOf { it.totalUYU(cardRate) } else 0.0)
    }
    val fixedSectionTotal = remember(filteredFixedExpenses, cardRate) {
        filteredFixedExpenses.sumOf { it.totalUYU(cardRate) }.formatUYU()
    }
    val variableSectionTotal = remember(filteredVariableExpenses, cardRate) {
        filteredVariableExpenses.sumOf { it.totalUYU(cardRate) }.formatUYU()
    }
    val cardSectionTotal = remember(filteredCardExpenses, cardRate) {
        filteredCardExpenses.sumOf { it.totalUYU(cardRate) }.formatUYU()
    }
    val debtSectionTotal = remember(filteredDebts, cardRate) {
        filteredDebts.sumOf { it.totalUYU(cardRate) }.formatUYU()
    }
    val selectableExpenseKeys = remember(
        filteredFixedExpenses,
        filteredVariableExpenses,
        filteredCardExpenses,
        filteredDebts
    ) {
        buildSet {
            filteredFixedExpenses.forEach { add("fixed:${it.id}") }
            filteredVariableExpenses.forEach { add("variable:${it.id}") }
            filteredCardExpenses.forEach { add("card:${it.id}") }
            filteredDebts.forEach { add("debt:${it.id}") }
        }
    }
    val canSelectMoreExpenses = selectableExpenseKeys.any { it !in selectedExpenseKeys }

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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
        MonthSwipeContainer(
            canGoPrevious = canGoPreviousMonth,
            canGoNext = canGoNextMonth,
            onGoPrevious = onGoPreviousMonth,
            onGoNext = onGoNextMonth,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(Modifier.fillMaxSize()) {
                if (headerPinned) {
                    headerContent()
                }
                if (!isReadOnly && selectedExpenseKeys.isNotEmpty()) {
                    SelectionActionBar(
                        selectedCount = selectedExpenseKeys.size,
                        canSelectAll = canSelectMoreExpenses,
                        onSelectAll = {
                            collapsedSections = emptySet()
                            selectedExpenseKeys = selectableExpenseKeys
                        },
                        onClearSelection = { selectedExpenseKeys = emptySet() },
                        onDeleteSelected = {
                            deleteRequest = ExpenseDeleteRequest.Bulk(selectedExpenseKeys)
                        }
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
            if (isReadOnly) item(contentType = "read_only") { ReadOnlyBanner() }

            item(contentType = "controls") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!headerPinned) {
                        headerContent()
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.expenses_search_label)) },
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = paymentFilter == PaymentFilter.ALL,
                            onClick = { paymentFilter = PaymentFilter.ALL },
                            label = { Text(stringResource(R.string.common_filter_all)) }
                        )
                        FilterChip(
                            selected = paymentFilter == PaymentFilter.PENDING,
                            onClick = { paymentFilter = PaymentFilter.PENDING },
                            label = { Text(stringResource(R.string.common_filter_pending)) }
                        )
                        FilterChip(
                            selected = paymentFilter == PaymentFilter.PAID,
                            onClick = { paymentFilter = PaymentFilter.PAID },
                            label = { Text(stringResource(R.string.common_filter_paid)) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = {
                            collapsedSections = if (hasActiveVisibilityFilter) {
                                allSections - sectionsWithVisibleResults
                            } else if (areAllCollapsed) {
                                emptySet()
                            } else {
                                allSections
                            }
                        }) {
                            Text(
                                stringResource(
                                    if (areAllCollapsed) R.string.expenses_expand_all
                                    else R.string.expenses_collapse_all
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExpenseTotalCard(
                            label = stringResource(R.string.expenses_filtered_total_uyu),
                            value = visibleTotalUYU.formatUYU(),
                            modifier = Modifier.weight(1f)
                        )
                        ExpenseTotalCard(
                            label = stringResource(R.string.expenses_filtered_total_usd),
                            value = visibleTotalUSD.formatUSD(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FinanceCard(
                        containerColor = MaterialTheme.colorScheme.surface,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        SummaryRowCompact(
                            label = stringResource(R.string.expenses_filtered_total),
                            value = visibleTotalCalculated.formatUYU()
                        )
                        SummaryRowCompact(
                            label = stringResource(R.string.expenses_paid_total),
                            value = paidTotalCalculated.formatUYU()
                        )
                        SummaryRowCompact(
                            label = stringResource(R.string.expenses_pending_total),
                            value = pendingTotalCalculated.formatUYU()
                        )
                    }
                }
            }

            item(contentType = "fixed_header") {
                SectionHeader(
                    title = stringResource(R.string.expenses_section_fixed),
                    total = fixedSectionTotal,
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    expanded = fixedExpanded,
                    onClick = { toggleSection(ExpenseSection.FIXED) }
                )
            }
            if (fixedExpanded) {
                items(filteredFixedExpenses, key = { it.id }, contentType = { "fixed" }) { expense ->
                    val selectionKey = "fixed:${expense.id}"
                    FixedExpenseRow(
                        expense = expense,
                        exchangeRate = cardRate,
                        readOnly = isReadOnly,
                        selected = selectedExpenseKeys.contains(selectionKey),
                        selectionMode = selectedExpenseKeys.isNotEmpty(),
                        onToggleSelection = { toggleSelection(selectionKey) },
                        onTogglePaid = { viewModel.setFixedPaid(expense.id, it) },
                        onEdit = { editFixed = expense },
                        onDelete = { deleteRequest = ExpenseDeleteRequest.Fixed(expense) }
                    )
                }
            }

            item(contentType = "section_gap") { Spacer(Modifier.height(10.dp)) }

            item(contentType = "variable_header") {
                SectionHeader(
                    title = stringResource(R.string.expenses_section_variable),
                    total = variableSectionTotal,
                    icon = Icons.Default.ShoppingCart,
                    expanded = variableExpanded,
                    onClick = { toggleSection(ExpenseSection.VARIABLE) }
                )
            }
            if (variableExpanded) {
                items(filteredVariableExpenses, key = { it.id }, contentType = { "variable" }) { expense ->
                    val selectionKey = "variable:${expense.id}"
                    MoneyEntryRow(
                        entry = expense,
                        exchangeRate = cardRate,
                        fallback = stringResource(R.string.expenses_variable_item_fallback),
                        readOnly = isReadOnly,
                        selected = selectedExpenseKeys.contains(selectionKey),
                        selectionMode = selectedExpenseKeys.isNotEmpty(),
                        onToggleSelection = { toggleSelection(selectionKey) },
                        onTogglePaid = { viewModel.setVariableExpensePaid(expense.id, it) },
                        onEdit = { editVariable = expense },
                        onDelete = { deleteRequest = ExpenseDeleteRequest.Variable(expense) }
                    )
                }
            }

            item(contentType = "section_gap") { Spacer(Modifier.height(10.dp)) }

            item(contentType = "card_header") {
                CardSectionHeader(
                    title = stringResource(R.string.expenses_section_card),
                    total = cardSectionTotal,
                    expanded = cardExpanded,
                    showPaymentToggle = (!isReadOnly && allMonthCardExpenses.isNotEmpty()) || (isReadOnly && anyCardPaid),
                    paymentChecked = allCardPaid,
                    paymentEnabled = !isReadOnly && allMonthCardExpenses.isNotEmpty(),
                    onPaymentCheckedChange = { checked -> viewModel.setAllCardPaid(checked) },
                    onClick = { toggleSection(ExpenseSection.CARD) }
                )
            }
            if (cardExpanded) {
                items(filteredCardExpenses, key = { it.id }, contentType = { "card" }) { expense ->
                    val selectionKey = "card:${expense.id}"
                    CardExpenseRow(
                        expense = expense,
                        cardRate = cardRate,
                        readOnly = isReadOnly,
                        selected = selectedExpenseKeys.contains(selectionKey),
                        selectionMode = selectedExpenseKeys.isNotEmpty(),
                        onToggleSelection = { toggleSelection(selectionKey) },
                        onTogglePaid = { viewModel.setCardPaid(expense.id, it) },
                        onEdit = { editCard = expense },
                        onDelete = { deleteRequest = ExpenseDeleteRequest.Card(expense) }
                    )
                }
            }

            item(contentType = "section_gap") { Spacer(Modifier.height(10.dp)) }

            item(contentType = "debt_header") {
                SectionHeader(
                    title = stringResource(R.string.expenses_section_debt),
                    total = debtSectionTotal,
                    icon = Icons.Default.AccountBalance,
                    expanded = debtExpanded,
                    onClick = { toggleSection(ExpenseSection.DEBT) }
                )
            }
            if (debtExpanded) {
                items(filteredDebts, key = { it.id }, contentType = { "debt" }) { debt ->
                    val selectionKey = "debt:${debt.id}"
                    DebtRow(
                        debt = debt,
                        exchangeRate = cardRate,
                        readOnly = isReadOnly,
                        selected = selectedExpenseKeys.contains(selectionKey),
                        selectionMode = selectedExpenseKeys.isNotEmpty(),
                        onToggleSelection = { toggleSelection(selectionKey) },
                        onTogglePaid = { viewModel.setDebtPaid(debt.id, it) },
                        onEdit = { editDebt = debt },
                        onDelete = { deleteRequest = ExpenseDeleteRequest.Debt(debt) }
                    )
                }
            }
            }
            }
        }
    }

    if (showAddFixed || editFixed != null) {
        FixedExpenseDialog(
            initial = editFixed,
            categories = categories,
            onCreateCategory = viewModel::addCategory,
            shouldCheckDuplicates = editFixed == null,
            findDuplicates = ::findDuplicateMatches,
            onConfirm = { expense, applyToFuture ->
                viewModel.upsertFixed(expense, applyToFuture)
                showAddFixed = false
                editFixed = null
            },
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
            shouldCheckDuplicates = editVariable == null,
            findDuplicates = ::findDuplicateMatches,
            onConfirm = { expense, _ -> viewModel.upsertVariableExpense(expense); showAddVariable = false; editVariable = null },
            onDismiss = { showAddVariable = false; editVariable = null }
        )
    }
    if (showAddCard || editCard != null) {
        CardExpenseDialog(
            initial = editCard,
            categories = categories,
            onCreateCategory = viewModel::addCategory,
            shouldCheckDuplicates = editCard == null,
            findDuplicates = ::findDuplicateMatches,
            onConfirm = { expense, applyToFuture ->
                viewModel.upsertCard(expense, applyToFuture)
                showAddCard = false
                editCard = null
            },
            onDismiss = { showAddCard = false; editCard = null }
        )
    }
    if (showAddDebt || editDebt != null) {
        DebtDialog(
            initial = editDebt,
            categories = categories,
            onCreateCategory = viewModel::addCategory,
            shouldCheckDuplicates = editDebt == null,
            findDuplicates = ::findDuplicateMatches,
            onConfirm = { debt, applyToFuture ->
                viewModel.upsertDebt(debt, applyToFuture)
                showAddDebt = false
                editDebt = null
            },
            onDismiss = { showAddDebt = false; editDebt = null }
        )
    }
    deleteRequest?.let { request ->
        val allowFutureDelete = when (request) {
            is ExpenseDeleteRequest.Fixed -> true
            is ExpenseDeleteRequest.Card -> request.expense.kind != CardExpenseKind.PUNCTUAL
            is ExpenseDeleteRequest.Debt -> true
            else -> false
        }
        val itemName = when (request) {
            is ExpenseDeleteRequest.Fixed -> request.expense.name
            is ExpenseDeleteRequest.Variable -> request.expense.name
            is ExpenseDeleteRequest.Card -> request.expense.name
            is ExpenseDeleteRequest.Debt -> request.debt.name
            is ExpenseDeleteRequest.Bulk -> ""
        }.ifBlank { stringResource(R.string.expenses_delete_item_fallback) }
        val message = when (request) {
            is ExpenseDeleteRequest.Bulk -> stringResource(R.string.delete_confirm_selected)
            else -> stringResource(R.string.delete_confirm_named, itemName)
        }
        DeleteConfirmationDialog(
            message = message,
            allowFutureDelete = allowFutureDelete,
            onConfirm = { deleteFuture ->
                when (request) {
                    is ExpenseDeleteRequest.Fixed -> viewModel.deleteFixed(request.expense.id, deleteFuture)
                    is ExpenseDeleteRequest.Variable -> viewModel.deleteVariableExpense(request.expense.id)
                    is ExpenseDeleteRequest.Card -> viewModel.deleteCard(request.expense.id, deleteFuture)
                    is ExpenseDeleteRequest.Debt -> viewModel.deleteDebt(request.debt.id, deleteFuture)
                    is ExpenseDeleteRequest.Bulk -> {
                        viewModel.deleteSelected(request.keys)
                        selectedExpenseKeys = emptySet()
                    }
                }
                deleteRequest = null
            },
            onDismiss = { deleteRequest = null }
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    message: String,
    allowFutureDelete: Boolean,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var deleteFuture by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message)
                if (allowFutureDelete) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = deleteFuture,
                            onCheckedChange = { deleteFuture = it }
                        )
                        Text(stringResource(R.string.delete_confirm_future))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(deleteFuture) }) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun DuplicateExpenseDialog(
    expenseName: String,
    matches: List<DuplicateExpenseMatch>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.expenses_duplicate_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.expenses_duplicate_message, expenseName))
                matches.forEach { match ->
                    Text(
                        stringResource(
                            R.string.expenses_duplicate_item,
                            match.typeLabel,
                            match.amountLabel
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.expenses_duplicate_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun ExpenseTotalCard(label: String, value: String, modifier: Modifier = Modifier) {
    FinanceCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outlineVariant,
        contentPadding = PaddingValues(14.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Clip
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
private fun CardSectionHeader(
    title: String,
    total: String,
    expanded: Boolean,
    showPaymentToggle: Boolean,
    paymentChecked: Boolean,
    paymentEnabled: Boolean,
    onPaymentCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    FinanceCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        borderColor = MaterialTheme.colorScheme.outlineVariant,
        contentPadding = PaddingValues(14.dp, 12.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val titleMaxWidth = if (showPaymentToggle) maxWidth * 0.46f else maxWidth * 0.62f
        val totalMinWidth = if (showPaymentToggle) 96.dp else 88.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.widthIn(max = titleMaxWidth),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SoftIconBadge(
                    icon = Icons.Default.CreditCard,
                    badgeSize = 40.dp,
                    iconSize = 21.dp
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (showPaymentToggle) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.expenses_card_paid_label),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                    Checkbox(
                        checked = paymentChecked,
                        onCheckedChange = if (paymentEnabled) {
                            { checked: Boolean -> onPaymentCheckedChange(checked) }
                        } else {
                            null
                        }
                    )
                }
            }
            Text(
                text = total,
                modifier = Modifier.widthIn(min = totalMinWidth),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}
}

private fun paymentRowColor(
    selected: Boolean,
    isPaid: Boolean,
    surface: androidx.compose.ui.graphics.Color,
    selectedColor: androidx.compose.ui.graphics.Color,
    paidColor: androidx.compose.ui.graphics.Color
): androidx.compose.ui.graphics.Color {
    return when {
        selected -> selectedColor
        isPaid -> paidColor
        else -> surface
    }
}

@Composable
private fun paymentRowHeadlineColor(selected: Boolean, isPaid: Boolean): Color = when {
    selected -> MaterialTheme.colorScheme.onSecondaryContainer
    isPaid -> PaidExpenseContentColor
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun paymentRowSupportingColor(selected: Boolean, isPaid: Boolean): Color = when {
    selected -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.74f)
    isPaid -> PaidExpenseContentColor.copy(alpha = 0.74f)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun paymentRowAccentColor(selected: Boolean, isPaid: Boolean): Color = when {
    selected -> MaterialTheme.colorScheme.onSecondaryContainer
    isPaid -> PaidExpenseContentColor
    else -> MaterialTheme.colorScheme.primary
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FixedExpenseRow(
    expense: FixedExpense,
    exchangeRate: Double,
    readOnly: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onTogglePaid: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val containerColor = paymentRowColor(
        selected = selected,
        isPaid = expense.isPaid,
        surface = MaterialTheme.colorScheme.surface,
        selectedColor = MaterialTheme.colorScheme.secondaryContainer,
        paidColor = PaidBackgroundColor
    )
    val headlineColor = paymentRowHeadlineColor(selected, expense.isPaid)
    val supportingColor = paymentRowSupportingColor(selected, expense.isPaid)
    val accentColor = paymentRowAccentColor(selected, expense.isPaid)
    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = { if (!readOnly && selectionMode) onToggleSelection() },
            onLongClick = { if (!readOnly) onToggleSelection() }
        ),
        colors = ListItemDefaults.colors(
            containerColor = containerColor,
            headlineColor = headlineColor,
            supportingColor = supportingColor,
            leadingIconColor = accentColor,
            trailingIconColor = headlineColor
        ),
        leadingContent = {
            Icon(
                imageVector = categoryIconFor(expense.category),
                contentDescription = expense.category.ifBlank { null },
                tint = accentColor
            )
        },
        headlineContent = { Text(expense.name) },
        supportingContent = {
            Column {
                if (expense.category.isNotBlank()) {
                    Text(expense.category, style = MaterialTheme.typography.labelSmall)
                }
                Text(stringResource(R.string.expenses_fixed_recurring_hint), style = MaterialTheme.typography.labelSmall)
                if (expense.isInUSD()) {
                    Text(
                        "${expense.amountUSD.formatUSD()} x ${"%.2f".format(exchangeRate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = supportingColor
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    expense.totalUYU(exchangeRate).formatUYU(),
                    fontWeight = FontWeight.Medium,
                    color = headlineColor
                )
                if (!selectionMode) {
                    Checkbox(
                        checked = expense.isPaid,
                        onCheckedChange = if (readOnly) null else onTogglePaid
                    )
                }
                if (!readOnly && !selectionMode) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(R.string.action_edit), Modifier.size(18.dp), tint = accentColor.copy(alpha = 0.74f)) }
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
    onTogglePaid: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val containerColor = paymentRowColor(
        selected = selected,
        isPaid = entry.isPaid,
        surface = MaterialTheme.colorScheme.surface,
        selectedColor = MaterialTheme.colorScheme.secondaryContainer,
        paidColor = PaidBackgroundColor
    )
    val headlineColor = paymentRowHeadlineColor(selected, entry.isPaid)
    val supportingColor = paymentRowSupportingColor(selected, entry.isPaid)
    val accentColor = paymentRowAccentColor(selected, entry.isPaid)
    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = { if (!readOnly && selectionMode) onToggleSelection() },
            onLongClick = { if (!readOnly) onToggleSelection() }
        ),
        colors = ListItemDefaults.colors(
            containerColor = containerColor,
            headlineColor = headlineColor,
            supportingColor = supportingColor,
            leadingIconColor = accentColor,
            trailingIconColor = headlineColor
        ),
        leadingContent = {
            Icon(
                imageVector = categoryIconFor(entry.category),
                contentDescription = entry.category.ifBlank { null },
                tint = accentColor
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
                Text(
                    entry.totalUYU(exchangeRate).formatUYU(),
                    fontWeight = FontWeight.Medium,
                    color = headlineColor
                )
                if (!selectionMode) {
                    Checkbox(
                        checked = entry.isPaid,
                        onCheckedChange = if (readOnly) null else onTogglePaid
                    )
                }
                if (!readOnly && !selectionMode) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(R.string.action_edit), Modifier.size(18.dp), tint = accentColor.copy(alpha = 0.74f)) }
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
    onTogglePaid: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val supporting = when (expense.kind) {
        CardExpenseKind.RECURRING -> stringResource(R.string.card_kind_recurring)
        CardExpenseKind.INSTALLMENT -> stringResource(R.string.card_installment_label, expense.currentInstallment, expense.totalInstallments)
        else -> stringResource(R.string.card_kind_punctual)
    }
    val containerColor = paymentRowColor(
        selected = selected,
        isPaid = expense.isPaid,
        surface = MaterialTheme.colorScheme.surface,
        selectedColor = MaterialTheme.colorScheme.secondaryContainer,
        paidColor = PaidBackgroundColor
    )
    val headlineColor = paymentRowHeadlineColor(selected, expense.isPaid)
    val supportingColor = paymentRowSupportingColor(selected, expense.isPaid)
    val accentColor = paymentRowAccentColor(selected, expense.isPaid)
    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = { if (!readOnly && selectionMode) onToggleSelection() },
            onLongClick = { if (!readOnly) onToggleSelection() }
        ),
        colors = ListItemDefaults.colors(
            containerColor = containerColor,
            headlineColor = headlineColor,
            supportingColor = supportingColor,
            leadingIconColor = accentColor,
            trailingIconColor = headlineColor
        ),
        leadingContent = {
            Icon(
                imageVector = categoryIconFor(expense.category),
                contentDescription = expense.category.ifBlank { null },
                tint = accentColor
            )
        },
        headlineContent = { Text(expense.name) },
        supportingContent = {
            Column {
                if (expense.category.isNotBlank()) {
                    Text(expense.category, style = MaterialTheme.typography.labelSmall)
                }
                Text(supporting, style = MaterialTheme.typography.labelSmall)
                if (expense.isInUSD()) Text("${expense.amountUSD.formatUSD()} x ${"%.2f".format(cardRate)}", style = MaterialTheme.typography.labelSmall, color = supportingColor)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    expense.totalUYU(cardRate).formatUYU(),
                    fontWeight = FontWeight.Medium,
                    color = headlineColor
                )
                if (!selectionMode) {
                    Checkbox(
                        checked = expense.isPaid,
                        onCheckedChange = if (readOnly) null else onTogglePaid
                    )
                }
                if (!readOnly && !selectionMode) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(R.string.action_edit), Modifier.size(18.dp), tint = accentColor.copy(alpha = 0.74f)) }
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
    onTogglePaid: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val containerColor = paymentRowColor(
        selected = selected,
        isPaid = debt.isPaid,
        surface = MaterialTheme.colorScheme.surface,
        selectedColor = MaterialTheme.colorScheme.secondaryContainer,
        paidColor = PaidBackgroundColor
    )
    val headlineColor = paymentRowHeadlineColor(selected, debt.isPaid)
    val supportingColor = paymentRowSupportingColor(selected, debt.isPaid)
    val accentColor = paymentRowAccentColor(selected, debt.isPaid)
    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = { if (!readOnly && selectionMode) onToggleSelection() },
            onLongClick = { if (!readOnly) onToggleSelection() }
        ),
        colors = ListItemDefaults.colors(
            containerColor = containerColor,
            headlineColor = headlineColor,
            supportingColor = supportingColor,
            leadingIconColor = accentColor,
            trailingIconColor = headlineColor
        ),
        leadingContent = {
            Icon(
                imageVector = categoryIconFor(debt.category),
                contentDescription = debt.category.ifBlank { null },
                tint = accentColor
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
                Text(
                    debt.totalUYU(exchangeRate).formatUYU(),
                    fontWeight = FontWeight.Medium,
                    color = headlineColor
                )
                if (!selectionMode) {
                    Checkbox(
                        checked = debt.isPaid,
                        onCheckedChange = if (readOnly) null else onTogglePaid
                    )
                }
                if (!readOnly && !selectionMode) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(R.string.action_edit), Modifier.size(18.dp), tint = accentColor.copy(alpha = 0.74f)) }
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
    shouldCheckDuplicates: Boolean,
    findDuplicates: (String) -> List<DuplicateExpenseMatch>,
    onConfirm: (FixedExpense, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var isUSD by remember { mutableStateOf(initial?.isInUSD() ?: false) }
    var applyFuture by remember { mutableStateOf(false) }
    var duplicateMatches by remember { mutableStateOf<List<DuplicateExpenseMatch>?>(null) }
    var amount by remember {
        mutableStateOf(
            when {
                initial?.isInUSD() == true -> initial.amountUSD.toInputAmount()
                initial != null -> initial.amountUYU.toInputAmount()
                else -> ""
            }
        )
    }

    fun buildExpense(): FixedExpense {
        val parsedAmount = amount.replace(",", ".").toDoubleOrNull() ?: 0.0
        val resolvedCategory = category.trim()
        if (resolvedCategory.isNotBlank()) onCreateCategory(resolvedCategory)
        return FixedExpense(
            id = initial?.id ?: UUID.randomUUID().toString(),
            name = name,
            category = resolvedCategory,
            amountUSD = if (isUSD) parsedAmount else 0.0,
            amountUYU = if (isUSD) 0.0 else parsedAmount,
            isUSD = isUSD,
            currency = if (isUSD) IncomeCurrency.USD else IncomeCurrency.UYU,
            isPinned = true,
            isPaid = initial?.isPaid ?: false
        )
    }

    fun saveExpense() {
        val matches = if (shouldCheckDuplicates) findDuplicates(name) else emptyList()
        if (matches.isNotEmpty()) {
            duplicateMatches = matches
            return
        }
        onConfirm(buildExpense(), if (initial == null) true else applyFuture)
    }

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
                CurrencySelector(isUSD = isUSD, onChange = { isUSD = it })
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.clearZeroOnFocus(amount) { amount = it },
                    label = { Text(stringResource(R.string.dialog_fixed_amount_label)) },
                    prefix = { Text(stringResource(if (isUSD) R.string.prefix_usd else R.string.prefix_uyu)) },
                    singleLine = true
                )
                if (initial != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = applyFuture, onCheckedChange = { applyFuture = it })
                        Text(stringResource(R.string.common_apply_to_future))
                    }
                }
                Text(stringResource(R.string.dialog_fixed_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        },
        confirmButton = {
            Button(onClick = ::saveExpense) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )

    duplicateMatches?.let { matches ->
        DuplicateExpenseDialog(
            expenseName = name.ifBlank { stringResource(R.string.expenses_delete_item_fallback) },
            matches = matches,
            onConfirm = {
                duplicateMatches = null
                onConfirm(buildExpense(), if (initial == null) true else applyFuture)
            },
            onDismiss = { duplicateMatches = null }
        )
    }
}

@Composable
private fun MoneyEntryDialog(
    title: String,
    initial: MoneyEntry?,
    categories: List<String>,
    onCreateCategory: (String) -> Unit,
    allowApplyToFuture: Boolean,
    shouldCheckDuplicates: Boolean,
    findDuplicates: (String) -> List<DuplicateExpenseMatch>,
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
    var duplicateMatches by remember { mutableStateOf<List<DuplicateExpenseMatch>?>(null) }

    fun buildExpense(): MoneyEntry {
        val parsed = amount.replace(",", ".").toDoubleOrNull() ?: 0.0
        val resolvedCategory = category.trim()
        if (resolvedCategory.isNotBlank()) onCreateCategory(resolvedCategory)
        return MoneyEntry(
            id = initial?.id ?: UUID.randomUUID().toString(),
            name = name,
            category = resolvedCategory,
            amountUSD = if (isUSD) parsed else 0.0,
            amountUYU = if (isUSD) 0.0 else parsed,
            isUSD = isUSD,
            currency = if (isUSD) IncomeCurrency.USD else IncomeCurrency.UYU,
            isPaid = initial?.isPaid ?: false
        )
    }

    fun saveExpense() {
        val matches = if (shouldCheckDuplicates) findDuplicates(name) else emptyList()
        if (matches.isNotEmpty()) {
            duplicateMatches = matches
            return
        }
        onConfirm(buildExpense(), applyFuture)
    }

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
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.clearZeroOnFocus(amount) { amount = it },
                    label = { Text(stringResource(R.string.common_amount_label)) },
                    prefix = { Text(stringResource(if (isUSD) R.string.prefix_usd else R.string.prefix_uyu)) },
                    singleLine = true
                )
                if (allowApplyToFuture) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = applyFuture, onCheckedChange = { applyFuture = it })
                        Text(stringResource(R.string.common_apply_to_future))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = ::saveExpense) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )

    duplicateMatches?.let { matches ->
        DuplicateExpenseDialog(
            expenseName = name.ifBlank { stringResource(R.string.expenses_delete_item_fallback) },
            matches = matches,
            onConfirm = {
                duplicateMatches = null
                onConfirm(buildExpense(), applyFuture)
            },
            onDismiss = { duplicateMatches = null }
        )
    }
}

@Composable
private fun CardExpenseDialog(
    initial: CardExpense?,
    categories: List<String>,
    onCreateCategory: (String) -> Unit,
    shouldCheckDuplicates: Boolean,
    findDuplicates: (String) -> List<DuplicateExpenseMatch>,
    onConfirm: (CardExpense, Boolean) -> Unit,
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
    var applyFuture by remember { mutableStateOf(false) }
    var duplicateMatches by remember { mutableStateOf<List<DuplicateExpenseMatch>?>(null) }

    fun buildExpense(): CardExpense {
        val value = amount.replace(",", ".").toDoubleOrNull() ?: 0.0
        val resolvedCategory = category.trim()
        if (resolvedCategory.isNotBlank()) onCreateCategory(resolvedCategory)
        return CardExpense(
            id = initial?.id ?: UUID.randomUUID().toString(),
            name = name,
            category = resolvedCategory,
            amountUSD = if (isUSD) value else 0.0,
            amountUYU = if (isUSD) 0.0 else value,
            isUSD = isUSD,
            currency = if (isUSD) IncomeCurrency.USD else IncomeCurrency.UYU,
            kind = kind,
            totalInstallments = if (kind == CardExpenseKind.INSTALLMENT) totalInst.toIntOrNull() ?: 1 else 1,
            currentInstallment = if (kind == CardExpenseKind.INSTALLMENT) currentInst.toIntOrNull() ?: 1 else 1,
            isPaid = initial?.isPaid ?: false
        )
    }

    fun saveExpense() {
        val matches = if (shouldCheckDuplicates) findDuplicates(name) else emptyList()
        if (matches.isNotEmpty()) {
            duplicateMatches = matches
            return
        }
        val shouldApplyToFuture = if (initial == null) {
            kind != CardExpenseKind.PUNCTUAL
        } else {
            applyFuture
        }
        onConfirm(buildExpense(), shouldApplyToFuture)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.dialog_card_title_new else R.string.dialog_card_title_edit)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.dialog_card_name_label)) }, singleLine = true)
                CategoryInput(
                    value = category,
                    onValueChange = { category = it },
                    categories = categories
                )
                CurrencySelector(isUSD = isUSD, onChange = { isUSD = it })
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.clearZeroOnFocus(amount) { amount = it },
                    label = { Text(stringResource(R.string.dialog_card_amount_label)) },
                    prefix = { Text(stringResource(if (isUSD) R.string.prefix_usd else R.string.prefix_uyu)) },
                    singleLine = true
                )
                Text(stringResource(R.string.dialog_card_kind_label), style = MaterialTheme.typography.labelMedium)
                CardKindSelector(kind = kind, onSelected = { kind = it })
                if (kind == CardExpenseKind.INSTALLMENT) {
                    InstallmentFields(
                        currentValue = currentInst,
                        onCurrentValueChange = { currentInst = it },
                        totalValue = totalInst,
                        onTotalValueChange = { totalInst = it },
                        currentLabel = stringResource(R.string.dialog_card_installment_number),
                        totalLabel = stringResource(R.string.dialog_card_installment_total)
                    )
                }
                if (initial != null && (initial.kind != CardExpenseKind.PUNCTUAL || kind != CardExpenseKind.PUNCTUAL)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = applyFuture, onCheckedChange = { applyFuture = it })
                        Text(stringResource(R.string.common_apply_to_future))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = ::saveExpense) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )

    duplicateMatches?.let { matches ->
        DuplicateExpenseDialog(
            expenseName = name.ifBlank { stringResource(R.string.expenses_delete_item_fallback) },
            matches = matches,
            onConfirm = {
                duplicateMatches = null
                val shouldApplyToFuture = if (initial == null) {
                    kind != CardExpenseKind.PUNCTUAL
                } else {
                    applyFuture
                }
                onConfirm(buildExpense(), shouldApplyToFuture)
            },
            onDismiss = { duplicateMatches = null }
        )
    }
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
    shouldCheckDuplicates: Boolean,
    findDuplicates: (String) -> List<DuplicateExpenseMatch>,
    onConfirm: (DebtEntry, Boolean) -> Unit,
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
    var applyFuture by remember { mutableStateOf(false) }
    var duplicateMatches by remember { mutableStateOf<List<DuplicateExpenseMatch>?>(null) }

    fun buildDebt(): DebtEntry {
        val value = amount.replace(",", ".").toDoubleOrNull() ?: 0.0
        val resolvedCategory = category.trim()
        if (resolvedCategory.isNotBlank()) onCreateCategory(resolvedCategory)
        return DebtEntry(
            id = initial?.id ?: UUID.randomUUID().toString(),
            name = name,
            category = resolvedCategory,
            amountUSD = if (isUSD) value else 0.0,
            amountUYU = if (isUSD) 0.0 else value,
            isUSD = isUSD,
            currency = if (isUSD) IncomeCurrency.USD else IncomeCurrency.UYU,
            totalInstallments = totalInst.toIntOrNull() ?: 1,
            currentInstallment = currentInst.toIntOrNull() ?: 1,
            isPaid = initial?.isPaid ?: false
        )
    }

    fun saveDebt() {
        val matches = if (shouldCheckDuplicates) findDuplicates(name) else emptyList()
        if (matches.isNotEmpty()) {
            duplicateMatches = matches
            return
        }
        onConfirm(buildDebt(), if (initial == null) true else applyFuture)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.dialog_debt_title_new else R.string.dialog_debt_title_edit)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.dialog_debt_name_label)) }, singleLine = true)
                CategoryInput(
                    value = category,
                    onValueChange = { category = it },
                    categories = categories
                )
                CurrencySelector(isUSD = isUSD, onChange = { isUSD = it })
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.clearZeroOnFocus(amount) { amount = it },
                    label = { Text(stringResource(R.string.dialog_debt_amount_label)) },
                    prefix = { Text(stringResource(if (isUSD) R.string.prefix_usd else R.string.prefix_uyu)) },
                    singleLine = true
                )
                InstallmentFields(
                    currentValue = currentInst,
                    onCurrentValueChange = { currentInst = it },
                    totalValue = totalInst,
                    onTotalValueChange = { totalInst = it },
                    currentLabel = stringResource(R.string.dialog_debt_installment_number),
                    totalLabel = stringResource(R.string.dialog_debt_installment_total)
                )
                if (initial != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = applyFuture, onCheckedChange = { applyFuture = it })
                        Text(stringResource(R.string.common_apply_to_future))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = ::saveDebt) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )

    duplicateMatches?.let { matches ->
        DuplicateExpenseDialog(
            expenseName = name.ifBlank { stringResource(R.string.expenses_delete_item_fallback) },
            matches = matches,
            onConfirm = {
                duplicateMatches = null
                onConfirm(buildDebt(), if (initial == null) true else applyFuture)
            },
            onDismiss = { duplicateMatches = null }
        )
    }
}

@Composable
private fun InstallmentFields(
    currentValue: String,
    onCurrentValueChange: (String) -> Unit,
    totalValue: String,
    onTotalValueChange: (String) -> Unit,
    currentLabel: String,
    totalLabel: String
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val useVerticalLayout = maxWidth < 360.dp
        if (useVerticalLayout) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = { onCurrentValueChange(it.filter(Char::isDigit)) },
                    label = { Text(currentLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = totalValue,
                    onValueChange = { onTotalValueChange(it.filter(Char::isDigit)) },
                    label = { Text(totalLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = { onCurrentValueChange(it.filter(Char::isDigit)) },
                    label = { Text(currentLabel) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = totalValue,
                    onValueChange = { onTotalValueChange(it.filter(Char::isDigit)) },
                    label = { Text(totalLabel) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun CategoryInput(
    value: String,
    onValueChange: (String) -> Unit,
    categories: List<String>
) {
    var showCategoryPicker by remember { mutableStateOf(false) }
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
                    IconButton(onClick = { showCategoryPicker = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.common_category_open))
                    }
                },
                singleLine = true
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showCategoryPicker = true }
            )
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
    if (showCategoryPicker && !useCustomCategory) {
        AlertDialog(
            onDismissRequest = { showCategoryPicker = false },
            title = { Text(stringResource(R.string.common_category_label)) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    items(categories, key = { it }) { category ->
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
                                showCategoryPicker = false
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCategoryPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
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
        "impuestos" -> Icons.AutoMirrored.Filled.ReceiptLong
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
