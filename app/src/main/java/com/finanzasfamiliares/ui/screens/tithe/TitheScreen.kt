package com.finanzasfamiliares.ui.screens.tithe

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finanzasfamiliares.R
import com.finanzasfamiliares.data.model.Donation
import com.finanzasfamiliares.data.model.IncomeCurrency
import com.finanzasfamiliares.ui.components.MonthSwipeContainer
import com.finanzasfamiliares.ui.components.MonthHeader
import com.finanzasfamiliares.ui.components.ReadOnlyBanner
import com.finanzasfamiliares.ui.components.SelectionActionBar
import com.finanzasfamiliares.ui.components.SoftIconBadge
import com.finanzasfamiliares.ui.components.clearZeroOnFocus
import com.finanzasfamiliares.ui.components.formatUSD
import com.finanzasfamiliares.ui.components.formatUYU
import com.finanzasfamiliares.ui.components.toInputAmount
import java.util.UUID

private val PaidDonationBackgroundColor = Color(0xFFE0F4E5)
private val PaidDonationContentColor = Color(0xFF173D2B)

private enum class DonationPaymentFilter {
    ALL, PENDING, PAID
}

private sealed interface DonationDeleteRequest {
    data class Single(val donation: Donation) : DonationDeleteRequest
    data class Bulk(val ids: Set<String>) : DonationDeleteRequest
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DonationsScreen(
    yearMonth: String,
    canGoPreviousMonth: Boolean = false,
    canGoNextMonth: Boolean = false,
    onGoPreviousMonth: () -> Unit = {},
    onGoNextMonth: () -> Unit = {},
    headerPinned: Boolean = true,
    headerContent: @Composable () -> Unit = {},
    viewModel: DonationsViewModel = hiltViewModel()
) {
    LaunchedEffect(yearMonth) { viewModel.setYearMonth(yearMonth) }
    val data by viewModel.monthData.collectAsState()
    val isReadOnly by viewModel.isReadOnly.collectAsState()
    val config by viewModel.currentConfig.collectAsState()
    val incomeCurrency = config.incomeCurrency
    val primaryIncomeUYU = data?.primaryIncomeInUYU(incomeCurrency) ?: 0.0
    var showDonationDialog by remember { mutableStateOf(false) }
    var editingDonation by remember { mutableStateOf<Donation?>(null) }
    var selectedDonationIds by remember { mutableStateOf(setOf<String>()) }
    var deleteRequest by remember { mutableStateOf<DonationDeleteRequest?>(null) }
    var paymentFilter by remember { mutableStateOf(DonationPaymentFilter.ALL) }
    val allDonations = data?.donations ?: emptyList()

    fun matchesPayment(isPaid: Boolean): Boolean = when (paymentFilter) {
        DonationPaymentFilter.ALL -> true
        DonationPaymentFilter.PENDING -> !isPaid
        DonationPaymentFilter.PAID -> isPaid
    }

    val filteredDonations = allDonations.filter { matchesPayment(it.isPaid) }
    val selectableDonationIds = remember(filteredDonations) {
        filteredDonations.map { it.id }.toSet()
    }
    val canSelectMoreDonations = selectableDonationIds.any { it !in selectedDonationIds }
    val visibleDonationsUYU = filteredDonations.sumOf { it.totalUYU(primaryIncomeUYU, data?.cardExchangeRate ?: 0.0) }
    val paidDonationsUYU = allDonations.filter { it.isPaid }.sumOf { it.totalUYU(primaryIncomeUYU, data?.cardExchangeRate ?: 0.0) }
    val pendingDonationsUYU = allDonations.filter { !it.isPaid }.sumOf { it.totalUYU(primaryIncomeUYU, data?.cardExchangeRate ?: 0.0) }

    fun toggleSelection(id: String) {
        selectedDonationIds = if (selectedDonationIds.contains(id)) {
            selectedDonationIds - id
        } else {
            selectedDonationIds + id
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (!isReadOnly && selectedDonationIds.isEmpty()) {
                FloatingActionButton(onClick = { showDonationDialog = true }) {
                    Icon(Icons.Default.Add, stringResource(R.string.donations_add_cd))
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
                if (!isReadOnly && selectedDonationIds.isNotEmpty()) {
                    SelectionActionBar(
                        selectedCount = selectedDonationIds.size,
                        canSelectAll = canSelectMoreDonations,
                        onSelectAll = { selectedDonationIds = selectableDonationIds },
                        onClearSelection = { selectedDonationIds = emptySet() },
                        onDeleteSelected = {
                            deleteRequest = DonationDeleteRequest.Bulk(selectedDonationIds)
                        }
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
            if (isReadOnly) {
                item { ReadOnlyBanner() }
            }

            item {
                if (!headerPinned) {
                    headerContent()
                    Spacer(Modifier.height(8.dp))
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    stringResource(R.string.donations_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    visibleDonationsUYU.formatUYU(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            SoftIconBadge(
                                icon = Icons.Default.VolunteerActivism,
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            stringResource(
                                R.string.donations_summary_hint,
                                primaryIncomeUYU.formatUYU()
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = paymentFilter == DonationPaymentFilter.ALL,
                                onClick = { paymentFilter = DonationPaymentFilter.ALL },
                                label = { Text(stringResource(R.string.common_filter_all)) }
                            )
                            FilterChip(
                                selected = paymentFilter == DonationPaymentFilter.PENDING,
                                onClick = { paymentFilter = DonationPaymentFilter.PENDING },
                                label = { Text(stringResource(R.string.common_filter_pending)) }
                            )
                            FilterChip(
                                selected = paymentFilter == DonationPaymentFilter.PAID,
                                onClick = { paymentFilter = DonationPaymentFilter.PAID },
                                label = { Text(stringResource(R.string.common_filter_paid)) }
                            )
                        }
                        DonationSummaryRow(
                            label = stringResource(R.string.donations_paid_total),
                            value = paidDonationsUYU.formatUYU()
                        )
                        DonationSummaryRow(
                            label = stringResource(R.string.donations_pending_total),
                            value = pendingDonationsUYU.formatUYU()
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            items(filteredDonations, key = { it.id }) { donation ->
                DonationRow(
                    donation = donation,
                    primaryIncomeUYU = primaryIncomeUYU,
                    exchangeRate = data?.cardExchangeRate ?: 0.0,
                    readOnly = isReadOnly,
                    selected = selectedDonationIds.contains(donation.id),
                    selectionMode = selectedDonationIds.isNotEmpty(),
                    onToggleSelection = { toggleSelection(donation.id) },
                    onTogglePaid = { viewModel.setDonationPaid(donation.id, it) },
                    onEdit = { editingDonation = donation },
                    onDelete = { deleteRequest = DonationDeleteRequest.Single(donation) }
                )
            }

                if (filteredDonations.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.donations_empty_state),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
    }

    if (showDonationDialog || editingDonation != null) {
        DonationDialog(
            initial = editingDonation,
            onConfirm = {
                viewModel.upsertDonation(it)
                showDonationDialog = false
                editingDonation = null
            },
            onDismiss = {
                showDonationDialog = false
                editingDonation = null
            }
        )
    }
    deleteRequest?.let { request ->
        val message = when (request) {
            is DonationDeleteRequest.Single -> stringResource(
                R.string.delete_confirm_named,
                request.donation.name.ifBlank { stringResource(R.string.donations_item_fallback) }
            )
            is DonationDeleteRequest.Bulk -> stringResource(R.string.delete_confirm_selected)
        }
        DonationDeleteConfirmationDialog(
            message = message,
            onConfirm = {
                when (request) {
                    is DonationDeleteRequest.Single -> viewModel.deleteDonation(request.donation.id)
                    is DonationDeleteRequest.Bulk -> {
                        viewModel.deleteDonations(request.ids)
                        selectedDonationIds = emptySet()
                    }
                }
                deleteRequest = null
            },
            onDismiss = { deleteRequest = null }
        )
    }
}

@Composable
private fun DonationDeleteConfirmationDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_confirm_title)) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
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
private fun DonationSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DonationRow(
    donation: Donation,
    primaryIncomeUYU: Double,
    exchangeRate: Double,
    readOnly: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onTogglePaid: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val supporting = donation.percentOfPrimaryIncome?.let {
        stringResource(R.string.donations_percent_summary, it)
    } ?: if (donation.isInUSD()) {
        "${donation.amountUSD.formatUSD()} × ${"%.2f".format(exchangeRate)}"
    } else {
        stringResource(R.string.donations_manual_summary)
    }

    val containerColor = when {
        selected -> MaterialTheme.colorScheme.secondaryContainer
        donation.isPaid -> PaidDonationBackgroundColor
        else -> MaterialTheme.colorScheme.surface
    }
    val headlineColor = when {
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        donation.isPaid -> PaidDonationContentColor
        else -> MaterialTheme.colorScheme.onSurface
    }
    val supportingColor = when {
        selected -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.74f)
        donation.isPaid -> PaidDonationContentColor.copy(alpha = 0.74f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = { if (!readOnly && selectionMode) onToggleSelection() },
            onLongClick = { if (!readOnly) onToggleSelection() }
        ),
        colors = ListItemDefaults.colors(
            containerColor = containerColor,
            headlineColor = headlineColor,
            supportingColor = supportingColor,
            trailingIconColor = headlineColor
        ),
        headlineContent = {
            Text(donation.name.ifBlank { stringResource(R.string.donations_item_fallback) })
        },
        supportingContent = {
            Text(supporting, style = MaterialTheme.typography.labelSmall)
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    donation.totalUYU(primaryIncomeUYU, exchangeRate).formatUYU(),
                    fontWeight = FontWeight.Medium,
                    color = headlineColor
                )
                if (!selectionMode) {
                    Checkbox(
                        checked = donation.isPaid,
                        onCheckedChange = if (readOnly) null else onTogglePaid
                    )
                }
                if (!readOnly && !selectionMode) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            stringResource(R.string.action_edit),
                            tint = headlineColor.copy(alpha = 0.64f)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    )
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun DonationDialog(
    initial: Donation?,
    onConfirm: (Donation) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var usePercentage by remember { mutableStateOf(initial?.percentOfPrimaryIncome != null) }
    var isUSD by remember { mutableStateOf(initial?.isInUSD() ?: false) }
    var percentage by remember { mutableStateOf(initial?.percentOfPrimaryIncome?.toInputAmount() ?: "10.00") }
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
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.dialog_donation_title_new
                    else R.string.dialog_donation_title_edit
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.common_optional_name)) },
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = usePercentage, onCheckedChange = { usePercentage = it })
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.donations_use_percentage))
                }
                if (usePercentage) {
                    OutlinedTextField(
                        value = percentage,
                        onValueChange = { percentage = it },
                        label = { Text(stringResource(R.string.donations_percentage_label)) },
                        suffix = { Text(stringResource(R.string.suffix_percent)) },
                        supportingText = {
                            Text(stringResource(R.string.donations_percentage_hint))
                        },
                        singleLine = true
                    )
                } else {
                    CurrencySelector(isUSD = isUSD, onChange = { isUSD = it })
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        modifier = Modifier.clearZeroOnFocus(amount) { amount = it },
                        label = { Text(stringResource(R.string.common_amount_label)) },
                        prefix = {
                            Text(
                                stringResource(
                                    if (isUSD) R.string.prefix_usd else R.string.prefix_uyu
                                )
                            )
                        },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedAmount = amount.replace(",", ".").toDoubleOrNull() ?: 0.0
                val parsedPercent = percentage.replace(",", ".").toDoubleOrNull()
                onConfirm(
                    Donation(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        amountUSD = if (!usePercentage && isUSD) parsedAmount else 0.0,
                        amountUYU = if (!usePercentage && !isUSD) parsedAmount else 0.0,
                        isUSD = !usePercentage && isUSD,
                        currency = if (!usePercentage && isUSD) IncomeCurrency.USD else IncomeCurrency.UYU,
                        percentOfPrimaryIncome = if (usePercentage) parsedPercent ?: 0.0 else null,
                        isPaid = initial?.isPaid ?: false
                    )
                )
            }) {
                Text(stringResource(R.string.action_save))
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
private fun CurrencySelector(isUSD: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = !isUSD, onClick = { onChange(false) }, label = { Text(stringResource(R.string.common_currency_uyu_short)) })
        FilterChip(selected = isUSD, onClick = { onChange(true) }, label = { Text(stringResource(R.string.common_currency_usd_short)) })
    }
}
