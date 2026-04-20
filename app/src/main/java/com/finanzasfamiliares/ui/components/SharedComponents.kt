package com.finanzasfamiliares.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.finanzasfamiliares.R
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun Double.formatUYU(): String = "$ ${"%,.2f".format(this)}"
fun Double.formatUSD(): String = "U\$S ${"%.2f".format(this)}"
fun Double.toInputAmount(): String = String.format(Locale.US, "%.2f", this)
fun String.isZeroLikeAmountInput(): Boolean =
    trim()
        .replace(",", ".")
        .toDoubleOrNull() == 0.0

fun Modifier.clearZeroOnFocus(
    value: String,
    onValueChange: (String) -> Unit
): Modifier = onFocusChanged { focusState ->
    if (focusState.isFocused && value.isZeroLikeAmountInput()) {
        onValueChange("")
    }
}

fun formatMonthYearLabel(yearMonth: String): String {
    val ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"))
    val month = ym.month.getDisplayName(TextStyle.FULL, Locale("es", "UY"))
        .replaceFirstChar { it.uppercase() }
    return "$month ${ym.year}"
}

@Composable
fun MonthHeader(yearMonth: String, modifier: Modifier = Modifier) {
    Text(
        text = formatMonthYearLabel(yearMonth),
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

@Composable
fun AmountDialog(
    title: String,
    label: String,
    initialValue: Double = 0.0,
    isUSD: Boolean = false,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(if (initialValue > 0) initialValue.toString() else "") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.clearZeroOnFocus(text) { text = it },
                    label = { Text(label) },
                    prefix = { Text(stringResource(if (isUSD) R.string.prefix_usd else R.string.prefix_uyu)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        onConfirm(text.replace(",", ".").toDoubleOrNull() ?: 0.0)
                    }) { Text(stringResource(R.string.action_save)) }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    total: String? = null,
    modifier: Modifier = Modifier,
    expanded: Boolean? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (expanded != null) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (total != null) {
                Text(total, style = MaterialTheme.typography.titleSmall)
            }
            trailingContent?.invoke(this)
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ReadOnlyBanner() {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.banner_read_only),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MonthNavigationHeader(
    currentMonth: YearMonth,
    monthLabel: String,
    availableMonths: List<YearMonth>,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    isCurrentMonth: Boolean,
    isGenerating: Boolean,
    onGoPrevious: () -> Unit,
    onGoNext: () -> Unit,
    onGoToMonth: (YearMonth) -> Unit,
    onGoToCurrentMonth: () -> Unit,
    onGenerateMonths: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showGenerateDialog by remember { mutableStateOf(false) }
    var showMonthPickerDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onGoPrevious, enabled = canGoPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.summary_prev_month_cd))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextButton(onClick = { showMonthPickerDialog = true }) {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!isCurrentMonth) {
                FilterChip(
                    selected = true,
                    onClick = onGoToCurrentMonth,
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
        IconButton(onClick = onGoNext, enabled = canGoNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.summary_next_month_cd))
        }
    }

    if (showGenerateDialog) {
        GenerateMonthsDialog(
            onConfirm = {
                onGenerateMonths(it)
                showGenerateDialog = false
            },
            onDismiss = { showGenerateDialog = false }
        )
    }
    if (showMonthPickerDialog) {
        MonthPickerDialog(
            initialMonth = currentMonth,
            availableMonths = availableMonths,
            onConfirm = {
                onGoToMonth(it)
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
fun MonthSwipeContainer(
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onGoPrevious: () -> Unit,
    onGoNext: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.pointerInput(canGoPrevious, canGoNext) {
            var accumulatedDrag = 0f
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, dragAmount ->
                    accumulatedDrag += dragAmount
                    change.consume()
                },
                onDragEnd = {
                    when {
                        accumulatedDrag > 80f && canGoPrevious -> onGoPrevious()
                        accumulatedDrag < -80f && canGoNext -> onGoNext()
                    }
                    accumulatedDrag = 0f
                },
                onDragCancel = { accumulatedDrag = 0f }
            )
        }
    ) {
        content()
    }
}

@Composable
fun GenerateMonthsDialog(onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthPickerDialog(
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
fun LoadingDialog(message: String) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text(message) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                Text(stringResource(R.string.summary_generating_months_hint))
            }
        }
    )
}
