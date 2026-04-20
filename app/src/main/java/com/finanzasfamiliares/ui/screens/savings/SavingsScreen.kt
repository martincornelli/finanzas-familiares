package com.finanzasfamiliares.ui.screens.savings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finanzasfamiliares.R
import com.finanzasfamiliares.data.model.IncomeCurrency
import com.finanzasfamiliares.data.model.Saving
import com.finanzasfamiliares.ui.components.*
import java.text.SimpleDateFormat
import java.util.Locale

private sealed interface SavingDeleteRequest {
    data class Single(val saving: Saving) : SavingDeleteRequest
    data class Bulk(val ids: Set<String>) : SavingDeleteRequest
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SavingsScreen(
    yearMonth: String,
    canGoPreviousMonth: Boolean = false,
    canGoNextMonth: Boolean = false,
    onGoPreviousMonth: () -> Unit = {},
    onGoNextMonth: () -> Unit = {},
    headerContent: @Composable () -> Unit = {},
    viewModel: SavingsViewModel = hiltViewModel()
) {
    LaunchedEffect(yearMonth) { viewModel.setYearMonth(yearMonth) }
    val savings by viewModel.savings.collectAsState()
    val totalUYU by viewModel.totalUYU.collectAsState()
    val totalUSD by viewModel.totalUSD.collectAsState()
    var showAddSaving by remember { mutableStateOf(false) }
    var editingSaving by remember { mutableStateOf<Saving?>(null) }
    var selectedSavingIds by remember { mutableStateOf(setOf<String>()) }
    var deleteRequest by remember { mutableStateOf<SavingDeleteRequest?>(null) }
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "UY")) }

    fun toggleSelection(id: String) {
        selectedSavingIds = if (selectedSavingIds.contains(id)) {
            selectedSavingIds - id
        } else {
            selectedSavingIds + id
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (selectedSavingIds.isEmpty()) {
                FloatingActionButton(onClick = { showAddSaving = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.savings_add_cd))
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
            item {
                headerContent()
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.savings_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
            }

            if (selectedSavingIds.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.selection_count, selectedSavingIds.size),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = {
                            deleteRequest = SavingDeleteRequest.Bulk(selectedSavingIds)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_delete_selected))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            items(savings, key = { it.id }) { saving ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (selectedSavingIds.isNotEmpty()) toggleSelection(saving.id)
                                else editingSaving = saving
                            },
                            onLongClick = { toggleSelection(saving.id) }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedSavingIds.contains(saving.id)) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(saving.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val updatedText = saving.lastUpdated?.toDate()?.let {
                                stringResource(R.string.savings_updated_at, dateFmt.format(it))
                            } ?: stringResource(R.string.savings_never_updated)
                            Text(
                                updatedText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (saving.isInUSD()) saving.displayAmount().formatUSD() else saving.displayAmount().formatUYU(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (selectedSavingIds.isEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Edit,
                                    stringResource(R.string.savings_update_cd),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

                item {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    SummaryRow(stringResource(R.string.savings_total_label_uyu), totalUYU.formatUYU())
                    SummaryRow(stringResource(R.string.savings_total_label_usd), totalUSD.formatUSD())
                    Spacer(Modifier.height(64.dp))
                }
            }
        }
    }

    if (showAddSaving) {
        SavingDialog(
            initial = null,
            onConfirm = { name, amount, currency, _ ->
                viewModel.createSaving(name, amount, currency)
                showAddSaving = false
            },
            onDismiss = { showAddSaving = false }
        )
    }

    editingSaving?.let { saving ->
        SavingDialog(
            initial = saving,
            onConfirm = { name, amount, currency, editMode ->
                viewModel.updateSaving(saving, name, amount, currency, editMode)
                editingSaving = null
            },
            onDismiss = { editingSaving = null }
        )
    }
    deleteRequest?.let { request ->
        AlertDialog(
            onDismissRequest = { deleteRequest = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = {
                Text(
                    when (request) {
                        is SavingDeleteRequest.Single -> stringResource(
                            R.string.delete_confirm_named,
                            request.saving.name.ifBlank { stringResource(R.string.savings_title) }
                        )
                        is SavingDeleteRequest.Bulk -> stringResource(R.string.delete_confirm_selected)
                    }
                )
            },
            confirmButton = {
                Button(onClick = {
                    when (request) {
                        is SavingDeleteRequest.Single -> viewModel.deleteSaving(request.saving.id)
                        is SavingDeleteRequest.Bulk -> {
                            viewModel.deleteSavings(request.ids)
                            selectedSavingIds = emptySet()
                        }
                    }
                    deleteRequest = null
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteRequest = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavingDialog(
    initial: Saving?,
    onConfirm: (String, Double, String, SavingEditMode) -> Unit,
    onDismiss: () -> Unit
) {
    val initialCurrency = initial?.currencyCode() ?: IncomeCurrency.UYU
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var isUSD by remember { mutableStateOf(initial?.isInUSD() ?: false) }
    var amount by remember { mutableStateOf(initial?.displayAmount()?.toInputAmount() ?: "") }
    var editMode by remember { mutableStateOf(SavingEditMode.REPLACE) }

    LaunchedEffect(editMode, initial?.id) {
        if (initial == null) return@LaunchedEffect
        when (editMode) {
            SavingEditMode.REPLACE -> {
                isUSD = initial.isInUSD()
                amount = initial.displayAmount().toInputAmount()
            }
            SavingEditMode.ADD, SavingEditMode.SUBTRACT -> {
                isUSD = initial.isInUSD()
                amount = ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.dialog_saving_title_new
                    else R.string.dialog_saving_title_edit
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.dialog_saving_name_label)) },
                    singleLine = true
                )
                if (initial != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.savings_edit_mode_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = editMode == SavingEditMode.REPLACE,
                                onClick = { editMode = SavingEditMode.REPLACE },
                                label = { Text(stringResource(R.string.savings_edit_mode_replace)) }
                            )
                            FilterChip(
                                selected = editMode == SavingEditMode.ADD,
                                onClick = { editMode = SavingEditMode.ADD },
                                label = { Text(stringResource(R.string.savings_edit_mode_add)) }
                            )
                            FilterChip(
                                selected = editMode == SavingEditMode.SUBTRACT,
                                onClick = { editMode = SavingEditMode.SUBTRACT },
                                label = { Text(stringResource(R.string.savings_edit_mode_subtract)) }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isUSD,
                        onClick = { if (initial == null || editMode == SavingEditMode.REPLACE) isUSD = false },
                        enabled = initial == null || editMode == SavingEditMode.REPLACE,
                        label = { Text(stringResource(R.string.common_currency_uyu_short)) }
                    )
                    FilterChip(
                        selected = isUSD,
                        onClick = { if (initial == null || editMode == SavingEditMode.REPLACE) isUSD = true },
                        enabled = initial == null || editMode == SavingEditMode.REPLACE,
                        label = { Text(stringResource(R.string.common_currency_usd_short)) }
                    )
                }
                if (initial != null && editMode != SavingEditMode.REPLACE) {
                    Text(
                        stringResource(R.string.savings_edit_mode_currency_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.clearZeroOnFocus(amount) { amount = it },
                    label = {
                        Text(
                            stringResource(
                                when (editMode) {
                                    SavingEditMode.REPLACE -> R.string.dialog_saving_label
                                    SavingEditMode.ADD -> R.string.dialog_saving_label_add
                                    SavingEditMode.SUBTRACT -> R.string.dialog_saving_label_subtract
                                }
                            )
                        )
                    },
                    prefix = {
                        Text(
                            stringResource(
                                if (initial != null && editMode != SavingEditMode.REPLACE) {
                                    if (initialCurrency == IncomeCurrency.USD) R.string.prefix_usd else R.string.prefix_uyu
                                } else if (isUSD) {
                                    R.string.prefix_usd
                                } else {
                                    R.string.prefix_uyu
                                }
                            )
                        )
                    },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedAmount = (amount.replace(",", ".").toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                val resolvedCurrency = if (initial != null && editMode != SavingEditMode.REPLACE) {
                    initialCurrency
                } else if (isUSD) {
                    IncomeCurrency.USD
                } else {
                    IncomeCurrency.UYU
                }
                onConfirm(
                    name,
                    parsedAmount,
                    resolvedCurrency,
                    editMode
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
