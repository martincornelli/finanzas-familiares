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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finanzasfamiliares.R
import com.finanzasfamiliares.data.model.IncomeCurrency
import com.finanzasfamiliares.ui.components.clearZeroOnFocus
import com.finanzasfamiliares.ui.components.toInputAmount
import com.finanzasfamiliares.ui.theme.AppAccentColor
import com.finanzasfamiliares.ui.theme.AppThemeMode
import com.finanzasfamiliares.ui.theme.AppearanceViewModel
import com.finanzasfamiliares.ui.theme.Green700
import com.finanzasfamiliares.ui.theme.Yellow700
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    viewModel: ConfigViewModel = hiltViewModel(),
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsState()
    val themeMode by appearanceViewModel.themeMode.collectAsState()
    val accentColor by appearanceViewModel.accentColor.collectAsState()
    val joinCode by viewModel.joinCode.collectAsState()
    val joinError by viewModel.joinError.collectAsState()
    val familyActionError by viewModel.familyActionError.collectAsState()
    val canLeaveFamily by viewModel.canLeaveFamily.collectAsState()
    val isLeavingFamily by viewModel.isLeavingFamily.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        }.getOrDefault("1.0")
    }

    var incomeCurrency by remember(config) { mutableStateOf(config.incomeCurrency) }
    var defaultRate by remember(config) { mutableStateOf(config.defaultExchangeRate.toInputAmount()) }
    var saleRate by remember(config) { mutableStateOf((config.defaultExchangeRate + config.defaultCardExchangeOffset).toInputAmount()) }
    var greenAmount by remember(config) { mutableStateOf(config.marginGreenThresholdPct.toInputAmount()) }
    var yellowAmount by remember(config) { mutableStateOf(config.marginYellowThresholdPct.toInputAmount()) }
    var joinCodeInput by remember { mutableStateOf("") }
    var showLeaveFamilyDialog by remember { mutableStateOf(false) }
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

        SectionTitle(stringResource(R.string.config_section_appearance))

        Text(
            stringResource(R.string.config_theme_mode_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeModeChip(
                selected = themeMode == AppThemeMode.SYSTEM,
                label = stringResource(R.string.config_theme_system),
                onClick = { appearanceViewModel.setThemeMode(AppThemeMode.SYSTEM) }
            )
            ThemeModeChip(
                selected = themeMode == AppThemeMode.LIGHT,
                label = stringResource(R.string.config_theme_light),
                onClick = { appearanceViewModel.setThemeMode(AppThemeMode.LIGHT) }
            )
            ThemeModeChip(
                selected = themeMode == AppThemeMode.DARK,
                label = stringResource(R.string.config_theme_dark),
                onClick = { appearanceViewModel.setThemeMode(AppThemeMode.DARK) }
            )
        }

        Text(
            stringResource(R.string.config_accent_color_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccentChip(
                selected = accentColor == AppAccentColor.GREEN,
                label = stringResource(R.string.config_accent_green),
                swatch = Color(0xFF176B5A),
                onClick = { appearanceViewModel.setAccentColor(AppAccentColor.GREEN) }
            )
            AccentChip(
                selected = accentColor == AppAccentColor.BLUE,
                label = stringResource(R.string.config_accent_blue),
                swatch = Color(0xFF2563EB),
                onClick = { appearanceViewModel.setAccentColor(AppAccentColor.BLUE) }
            )
            AccentChip(
                selected = accentColor == AppAccentColor.TEAL,
                label = stringResource(R.string.config_accent_teal),
                swatch = Color(0xFF0F766E),
                onClick = { appearanceViewModel.setAccentColor(AppAccentColor.TEAL) }
            )
            AccentChip(
                selected = accentColor == AppAccentColor.INDIGO,
                label = stringResource(R.string.config_accent_indigo),
                swatch = Color(0xFF4F46E5),
                onClick = { appearanceViewModel.setAccentColor(AppAccentColor.INDIGO) }
            )
            AccentChip(
                selected = accentColor == AppAccentColor.VIOLET,
                label = stringResource(R.string.config_accent_violet),
                swatch = Color(0xFF7C3AED),
                onClick = { appearanceViewModel.setAccentColor(AppAccentColor.VIOLET) }
            )
            AccentChip(
                selected = accentColor == AppAccentColor.SLATE,
                label = stringResource(R.string.config_accent_slate),
                swatch = Color(0xFF475569),
                onClick = { appearanceViewModel.setAccentColor(AppAccentColor.SLATE) }
            )
        }

        HorizontalDivider()

        SectionTitle(stringResource(R.string.config_section_income_currency))

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

        SectionTitle(stringResource(R.string.config_section_exchange))

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

        SectionTitle(stringResource(R.string.config_section_margin))

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

        SectionTitle(stringResource(R.string.config_section_family))

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

        if (familyActionError != null) {
            Text(familyActionError!!, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        if (canLeaveFamily) {
            HorizontalDivider()

            Text(
                stringResource(R.string.config_leave_family_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = { showLeaveFamilyDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLeavingFamily,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isLeavingFamily) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.config_leave_family_button))
            }
        }

        Text(
            text = stringResource(R.string.config_version_label, versionName),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(Modifier.height(40.dp))
    }
    }

    if (showLeaveFamilyDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveFamilyDialog = false },
            title = { Text(stringResource(R.string.config_leave_family_title)) },
            text = { Text(stringResource(R.string.config_leave_family_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.leaveFamily()
                        showLeaveFamilyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.config_leave_family_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveFamilyDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ThemeModeChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun AccentChip(
    selected: Boolean,
    label: String,
    swatch: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(swatch)
            )
        }
    )
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
        modifier = modifier.clearZeroOnFocus(value, onValueChange),
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
