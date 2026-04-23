package com.finanzasfamiliares.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
fun FinanceCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun SoftIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    badgeSize: androidx.compose.ui.unit.Dp = 52.dp,
    iconSize: androidx.compose.ui.unit.Dp = 26.dp
) {
    Surface(
        modifier = modifier.size(badgeSize),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    total: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    expanded: Boolean? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    SoftIconBadge(
                        icon = icon,
                        badgeSize = 40.dp,
                        iconSize = 21.dp
                    )
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
                    Text(
                        total,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                trailingContent?.invoke(this)
            }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectionActionBar(
    selectedCount: Int,
    canSelectAll: Boolean,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedCount <= 0) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                stringResource(R.string.selection_count, selectedCount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                TextButton(onClick = onSelectAll, enabled = canSelectAll) {
                    Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.action_select_all))
                }
                TextButton(onClick = onClearSelection) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.action_clear_selection))
                }
                TextButton(
                    onClick = onDeleteSelected,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.action_delete_selected))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthNavigationHeader(
    currentMonth: YearMonth,
    monthLabel: String,
    availableMonths: List<YearMonth>,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    isCurrentMonth: Boolean,
    isGenerating: Boolean,
    isHeaderPinned: Boolean = true,
    onGoPrevious: () -> Unit,
    onGoNext: () -> Unit,
    onGoToMonth: (YearMonth) -> Unit,
    onGoToCurrentMonth: () -> Unit,
    onGenerateMonths: (Int) -> Unit,
    onToggleHeaderPinned: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showGenerateDialog by remember { mutableStateOf(false) }
    var showMonthPickerDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onGoPrevious, enabled = canGoPrevious) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.summary_prev_month_cd))
                }
                TextButton(
                    onClick = { showMonthPickerDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onGoNext, enabled = canGoNext) {
                    Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.summary_next_month_cd))
                }
                if (onToggleHeaderPinned != null) {
                    IconButton(onClick = onToggleHeaderPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = stringResource(
                                if (isHeaderPinned) R.string.action_unpin_header else R.string.action_pin_header
                            ),
                            tint = if (isHeaderPinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isCurrentMonth) {
                    AssistChip(
                        onClick = onGoToCurrentMonth,
                        label = { Text(stringResource(R.string.summary_go_to_current_month)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Today,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        }
                    )
                }
                AssistChip(
                    onClick = { showGenerateDialog = true },
                    enabled = !isGenerating,
                    label = { Text(stringResource(R.string.summary_generate_months)) }
                )
            }
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
