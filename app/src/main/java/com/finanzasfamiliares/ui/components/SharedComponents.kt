package com.finanzasfamiliares.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
