package com.finanzasfamiliares.ui.screens.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finanzasfamiliares.R
import com.finanzasfamiliares.data.model.IncomeCurrency
import com.finanzasfamiliares.ui.components.clearZeroOnFocus

@Composable
fun SetupWizardScreen(viewModel: SetupWizardViewModel = hiltViewModel()) {
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val joinError by viewModel.joinError.collectAsState()

    var step by rememberSaveable { mutableIntStateOf(0) }
    var incomeCurrency by rememberSaveable { mutableStateOf("") }
    var purchaseRate by rememberSaveable { mutableStateOf("") }
    var saleRate by rememberSaveable { mutableStateOf("") }
    var greenThreshold by rememberSaveable { mutableStateOf("") }
    var yellowThreshold by rememberSaveable { mutableStateOf("") }
    var wantsToJoinFamily by rememberSaveable { mutableStateOf(false) }
    var familyCode by rememberSaveable { mutableStateOf("") }

    val purchaseRateValue = purchaseRate.parseFlexibleDouble()
    val saleRateValue = saleRate.parseFlexibleDouble()
    val greenThresholdValue = greenThreshold.parseFlexibleDouble()
    val yellowThresholdValue = yellowThreshold.parseFlexibleDouble()
    val totalSteps = if (wantsToJoinFamily) 1 else 4

    val isCurrentStepValid = when (step) {
        0 -> !wantsToJoinFamily || familyCode.isBlank() || familyCode.length == 6
        1 -> incomeCurrency.isNotBlank()
        2 -> purchaseRateValue != null && saleRateValue != null && saleRateValue >= purchaseRateValue
        else -> greenThresholdValue != null && yellowThresholdValue != null && yellowThresholdValue <= greenThresholdValue
    }

    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.setup_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    stringResource(R.string.setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                LinearProgressIndicator(
                    progress = { stepProgress(step, wantsToJoinFamily) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    stringResource(R.string.setup_step_counter, currentStepNumber(step, wantsToJoinFamily), totalSteps),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (step) {
                            0 -> {
                                StepTitle(
                                    title = stringResource(R.string.setup_family_title),
                                    body = stringResource(R.string.setup_family_body)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.setup_family_toggle))
                                    Switch(
                                        checked = wantsToJoinFamily,
                                        onCheckedChange = {
                                            wantsToJoinFamily = it
                                            if (!it) familyCode = ""
                                        }
                                    )
                                }
                                if (wantsToJoinFamily) {
                                    OutlinedTextField(
                                        value = familyCode,
                                        onValueChange = { familyCode = it.filter(Char::isDigit).take(6) },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(stringResource(R.string.config_join_field_label)) },
                                        supportingText = {
                                            Text(stringResource(R.string.setup_family_optional_help))
                                        },
                                        singleLine = true
                                    )
                                    if (joinError != null) {
                                        Text(
                                            text = joinError ?: "",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            1 -> {
                                StepTitle(
                                    title = stringResource(R.string.setup_income_title),
                                    body = stringResource(R.string.setup_income_body)
                                )
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
                            }

                            2 -> {
                                StepTitle(
                                    title = stringResource(R.string.setup_exchange_title),
                                    body = stringResource(R.string.setup_exchange_body)
                                )
                                WizardAmountField(
                                    value = purchaseRate,
                                    onValueChange = { purchaseRate = it },
                                    label = stringResource(R.string.config_base_rate_label),
                                    prefix = stringResource(R.string.prefix_uyu)
                                )
                                WizardAmountField(
                                    value = saleRate,
                                    onValueChange = { saleRate = it },
                                    label = stringResource(R.string.config_card_offset_label),
                                    prefix = stringResource(R.string.prefix_uyu)
                                )
                                if (purchaseRateValue != null && saleRateValue != null && saleRateValue < purchaseRateValue) {
                                    Text(
                                        stringResource(R.string.setup_exchange_error),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            else -> {
                                StepTitle(
                                    title = stringResource(R.string.setup_margin_title),
                                    body = stringResource(R.string.setup_margin_body)
                                )
                                WizardAmountField(
                                    value = greenThreshold,
                                    onValueChange = { greenThreshold = it },
                                    label = stringResource(R.string.config_margin_green_label),
                                    prefix = stringResource(R.string.prefix_uyu)
                                )
                                WizardAmountField(
                                    value = yellowThreshold,
                                    onValueChange = { yellowThreshold = it },
                                    label = stringResource(R.string.config_margin_yellow_label),
                                    prefix = stringResource(R.string.prefix_uyu)
                                )
                                Text(
                                    stringResource(R.string.setup_margin_help_green),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    stringResource(R.string.setup_margin_help_yellow),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    stringResource(R.string.setup_margin_help_red),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0 && !wantsToJoinFamily) {
                        TextButton(
                            onClick = { step -= 1 },
                            enabled = !isSubmitting
                        ) {
                            Text(stringResource(R.string.setup_back))
                        }
                    } else {
                        Spacer(Modifier)
                    }

                    if (step == 0 && wantsToJoinFamily) {
                        Button(
                            onClick = {
                                viewModel.completeSetup(
                                    SetupWizardData(
                                        incomeCurrency = "",
                                        purchaseRate = 0.0,
                                        saleRate = 0.0,
                                        greenThreshold = 0.0,
                                        yellowThreshold = 0.0,
                                        familyCode = familyCode
                                    )
                                )
                            },
                            enabled = isCurrentStepValid && !isSubmitting
                        ) {
                            Text(stringResource(R.string.setup_finish))
                        }
                    } else if (step < 3) {
                        Button(
                            onClick = { step += 1 },
                            enabled = isCurrentStepValid && !isSubmitting
                        ) {
                            Text(stringResource(R.string.setup_next))
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.completeSetup(
                                    SetupWizardData(
                                        incomeCurrency = incomeCurrency,
                                        purchaseRate = purchaseRateValue ?: 0.0,
                                        saleRate = saleRateValue ?: 0.0,
                                        greenThreshold = greenThresholdValue ?: 0.0,
                                        yellowThreshold = yellowThresholdValue ?: 0.0,
                                        familyCode = if (wantsToJoinFamily) familyCode else ""
                                    )
                                )
                            },
                            enabled = isCurrentStepValid && !isSubmitting
                        ) {
                            Text(stringResource(R.string.setup_finish))
                        }
                    }
                }
            }

            if (isSubmitting) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun StepTitle(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun WizardAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    prefix: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clearZeroOnFocus(value, onValueChange),
        label = { Text(label) },
        prefix = { Text(prefix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

private fun String.parseFlexibleDouble(): Double? =
    trim()
        .replace(",", ".")
        .toDoubleOrNull()

private fun stepProgress(step: Int, wantsToJoinFamily: Boolean): Float =
    if (wantsToJoinFamily) 1f else (step + 1) / 4f

private fun currentStepNumber(step: Int, wantsToJoinFamily: Boolean): Int =
    if (wantsToJoinFamily) 1 else step + 1
