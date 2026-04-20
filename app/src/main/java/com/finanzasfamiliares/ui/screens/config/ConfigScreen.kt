package com.finanzasfamiliares.ui.screens.config

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finanzasfamiliares.R
import com.finanzasfamiliares.data.model.IncomeCurrency
import com.finanzasfamiliares.ui.components.toInputAmount
import com.finanzasfamiliares.ui.theme.Green700
import com.finanzasfamiliares.ui.theme.Yellow700
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(viewModel: ConfigViewModel = hiltViewModel()) {
    val config by viewModel.config.collectAsState()
    val joinCode by viewModel.joinCode.collectAsState()
    val joinError by viewModel.joinError.collectAsState()
    val scope = rememberCoroutineScope()

    var incomeCurrency by remember(config) { mutableStateOf(config.incomeCurrency) }
    var defaultRate by remember(config) { mutableStateOf(config.defaultExchangeRate.toInputAmount()) }
    var saleRate by remember(config) { mutableStateOf((config.defaultExchangeRate + config.defaultCardExchangeOffset).toInputAmount()) }
    var greenAmount by remember(config) { mutableStateOf(config.marginGreenThresholdPct.toInputAmount()) }
    var yellowAmount by remember(config) { mutableStateOf(config.marginYellowThresholdPct.toInputAmount()) }
    var joinCodeInput by remember { mutableStateOf("") }
    var purchaseRateSuccessTick by remember { mutableIntStateOf(0) }
    var saleRateSuccessTick by remember { mutableIntStateOf(0) }
    var greenAmountSuccessTick by remember { mutableIntStateOf(0) }
    var yellowAmountSuccessTick by remember { mutableIntStateOf(0) }

    val saleRateValue = saleRate.parseFlexibleDouble() ?: 0.0
    val saveConfig: () -> Unit = {
        val purchaseRateValue = defaultRate.parseFlexibleDouble()
        val saleRateParsed = saleRate.parseFlexibleDouble()
        val greenAmountValue = greenAmount.parseFlexibleDouble()
        val yellowAmountValue = yellowAmount.parseFlexibleDouble()
        val purchaseChanged = purchaseRateValue?.isDifferentFrom(config.defaultExchangeRate) == true
        val saleChanged = saleRateParsed?.isDifferentFrom(
            config.defaultExchangeRate + config.defaultCardExchangeOffset
        ) == true
        val greenAmountChanged = greenAmountValue?.isDifferentFrom(config.marginGreenThresholdPct) == true
        val yellowAmountChanged = yellowAmountValue?.isDifferentFrom(config.marginYellowThresholdPct) == true
        val incomeCurrencyChanged = incomeCurrency != config.incomeCurrency
        val hasAnyChange = purchaseChanged || saleChanged || greenAmountChanged || yellowAmountChanged || incomeCurrencyChanged

        if (
            purchaseRateValue != null &&
            saleRateParsed != null &&
            greenAmountValue != null &&
            yellowAmountValue != null &&
            saleRateParsed >= purchaseRateValue &&
            yellowAmountValue <= greenAmountValue &&
            hasAnyChange
        ) {
            scope.launch {
                val result = viewModel.saveConfig(
                    config.copy(
                        incomeCurrency = incomeCurrency,
                        defaultExchangeRate = purchaseRateValue,
                        defaultCardExchangeOffset = saleRateParsed - purchaseRateValue,
                        marginGreenThresholdPct = greenAmountValue,
                        marginYellowThresholdPct = yellowAmountValue
                    )
                )
                if (result.isSuccess) {
                    if (purchaseChanged) purchaseRateSuccessTick += 1
                    if (saleChanged) saleRateSuccessTick += 1
                    if (greenAmountChanged) greenAmountSuccessTick += 1
                    if (yellowAmountChanged) yellowAmountSuccessTick += 1
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(stringResource(R.string.config_title)) },
                actions = {
                    TextButton(onClick = saveConfig) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            )
        }
    ) { innerPadding ->
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(innerPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Text(stringResource(R.string.config_section_income_currency),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = incomeCurrency == IncomeCurrency.UYU,
                onClick = { incomeCurrency = IncomeCurrency.UYU },
                label = { Text(stringResource(R.string.common_currency_uyu_short)) }
            )
            FilterChip(
                selected = incomeCurrency == IncomeCurrency.USD,
                onClick = { incomeCurrency = IncomeCurrency.USD },
                label = { Text(stringResource(R.string.common_currency_usd_short)) }
            )
        }

        HorizontalDivider()

        Text(stringResource(R.string.config_section_exchange),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

        ValidatedOutlinedTextField(
            value = defaultRate,
            onValueChange = { defaultRate = it },
            label = stringResource(R.string.config_base_rate_label),
            prefix = stringResource(R.string.prefix_uyu),
            successTick = purchaseRateSuccessTick,
            modifier = Modifier.fillMaxWidth()
        )

        ValidatedOutlinedTextField(
            value = saleRate,
            onValueChange = { saleRate = it },
            label = stringResource(R.string.config_card_offset_label),
            prefix = stringResource(R.string.prefix_uyu),
            successTick = saleRateSuccessTick,
            modifier = Modifier.fillMaxWidth(),
            supportingText = stringResource(R.string.config_card_offset_hint, saleRateValue)
        )

        HorizontalDivider()

        Text(stringResource(R.string.config_section_margin),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ValidatedOutlinedTextField(
                value = greenAmount,
                onValueChange = { greenAmount = it },
                label = stringResource(R.string.config_margin_green_label),
                successTick = greenAmountSuccessTick,
                modifier = Modifier.weight(1f)
            )
            ValidatedOutlinedTextField(
                value = yellowAmount,
                onValueChange = { yellowAmount = it },
                label = stringResource(R.string.config_margin_yellow_label),
                successTick = yellowAmountSuccessTick,
                modifier = Modifier.weight(1f)
            )
        }
        MarginLegend(color = Green700, text = stringResource(R.string.config_margin_green_hint, greenAmount.parseFlexibleDouble() ?: 0.0))
        MarginLegend(color = Yellow700, text = stringResource(R.string.config_margin_yellow_hint, yellowAmount.parseFlexibleDouble() ?: 0.0))
        MarginLegend(color = Color(0xFFD32F2F), text = stringResource(R.string.config_margin_red_hint, yellowAmount.parseFlexibleDouble() ?: 0.0))

        HorizontalDivider()

        Text(stringResource(R.string.config_section_family),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

        joinCode?.let { currentJoinCode ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.config_join_code_prompt),
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(currentJoinCode, style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        OutlinedTextField(joinCodeInput, { joinCodeInput = it }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.config_join_field_label)) },
            supportingText = { Text(stringResource(R.string.config_join_field_hint)) },
            singleLine = true)

        if (joinError != null) {
            Text(joinError!!, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        Button(onClick = { viewModel.joinWithCode(joinCodeInput) },
            Modifier.fillMaxWidth(), enabled = joinCodeInput.length == 6) {
            Text(stringResource(R.string.config_join_button))
        }

        Spacer(Modifier.height(40.dp))
    }
    }
}

@Composable
private fun MarginLegend(color: androidx.compose.ui.graphics.Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ValidatedOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    supportingText: String? = null,
    successTick: Int = 0
) {
    var showSuccess by remember { mutableStateOf(false) }
    val successGreen = Color(0xFF128A3E)

    LaunchedEffect(successTick) {
        if (successTick > 0) {
            showSuccess = true
            delay(900)
            showSuccess = false
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        prefix = prefix?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        trailingIcon = if (showSuccess) {
            {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = successGreen
                )
            }
        } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (showSuccess) successGreen else MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = if (showSuccess) successGreen else MaterialTheme.colorScheme.outline,
            focusedLabelColor = if (showSuccess) successGreen else MaterialTheme.colorScheme.primary,
            focusedTrailingIconColor = successGreen,
            unfocusedTrailingIconColor = successGreen
        )
    )
}

private fun String.parseFlexibleDouble(): Double? =
    trim()
        .replace(",", ".")
        .toDoubleOrNull()

private fun Double.isDifferentFrom(other: Double): Boolean = abs(this - other) > 0.0001
