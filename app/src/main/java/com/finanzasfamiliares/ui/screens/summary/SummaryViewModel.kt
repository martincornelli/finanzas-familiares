package com.finanzasfamiliares.ui.screens.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzasfamiliares.data.model.MoneyEntry
import com.finanzasfamiliares.data.model.MonthData
import com.finanzasfamiliares.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModel @Inject constructor(private val repo: FinanceRepository) : ViewModel() {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
    private val _currentYM = MutableStateFlow(YearMonth.now())
    private val whileSubscribed = SharingStarted.WhileSubscribed(5_000)
    private val config = repo.observeConfig()
        .stateIn(viewModelScope, whileSubscribed, com.finanzasfamiliares.data.model.FamilyConfig())
    private val availableMonthKeys = repo.observeAvailableMonths()
        .stateIn(viewModelScope, whileSubscribed, emptyList())
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    val monthLabel: StateFlow<String> = _currentYM.map { ym ->
        val month = ym.month.getDisplayName(TextStyle.FULL, Locale("es", "UY"))
            .replaceFirstChar { it.uppercase() }
        "$month ${ym.year}"
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val currentYMString: StateFlow<String> = _currentYM.map { it.format(fmt) }
        .stateIn(viewModelScope, whileSubscribed, YearMonth.now().format(fmt))

    val monthData: StateFlow<MonthData?> = _currentYM.flatMapLatest { ym ->
        repo.observeMonth(ym.format(fmt))
    }.stateIn(viewModelScope, whileSubscribed, null)

    val availableMonths: StateFlow<List<YearMonth>> =
        availableMonthKeys.combine(_currentYM) { monthKeys, currentYm ->
            (monthKeys + currentYm.format(fmt))
                .distinct()
                .map { YearMonth.parse(it, fmt) }
                .sorted()
        }
            .stateIn(viewModelScope, whileSubscribed, listOf(YearMonth.now()))

    val isCurrentMonth: StateFlow<Boolean> = _currentYM.map { it == YearMonth.now() }
        .stateIn(viewModelScope, whileSubscribed, true)

    val isFutureMonth: StateFlow<Boolean> = _currentYM.map { it > YearMonth.now() }
        .stateIn(viewModelScope, whileSubscribed, false)

    init {
        ensureMonthExistsIfNeeded(_currentYM.value)
    }

    fun goToPreviousMonth() {
        _currentYM.value = _currentYM.value.minusMonths(1)
    }

    fun goToNextMonth() {
        val next = _currentYM.value.plusMonths(1)
        val limit = config.value.planningThroughYearMonth.takeIf { it.isNotBlank() }?.let {
            YearMonth.parse(it, fmt)
        } ?: YearMonth.now()
        if (next <= maxOf(YearMonth.now(), limit)) {
            _currentYM.value = next
            ensureMonthExistsIfNeeded(next)
        }
    }

    fun goToMonth(target: YearMonth) {
        _currentYM.value = target
        ensureMonthExistsIfNeeded(target)
    }

    private fun ensureMonthExistsIfNeeded(target: YearMonth) {
        if (target < YearMonth.now()) return
        viewModelScope.launch {
            val key = target.format(fmt)
            if (key in availableMonthKeys.value) return@launch
            repo.ensureMonthDocument(key)
        }
    }

    fun updateExchangeSettings(rate: Double, cardRate: Double, applyToFuture: Boolean = false) {
        viewModelScope.launch {
            repo.updateMonthExchangeSettings(
                yearMonth = _currentYM.value.format(fmt),
                rate = rate,
                cardExchangeRate = cardRate,
                applyToFuture = applyToFuture
            )
        }
    }

    fun updatePrimaryIncome(usd: Double, applyToFuture: Boolean = false) {
        viewModelScope.launch {
            val shouldApply = applyToFuture || _currentYM.value == YearMonth.now()
            repo.updatePrimaryIncome(
                yearMonth = _currentYM.value.format(fmt),
                amount = usd,
                currency = config.value.incomeCurrency,
                applyToFuture = shouldApply
            )
        }
    }

    fun upsertVariableIncome(income: MoneyEntry) {
        viewModelScope.launch { repo.upsertVariableIncome(_currentYM.value.format(fmt), income) }
    }

    fun deleteVariableIncome(id: String) {
        viewModelScope.launch { repo.deleteVariableIncome(_currentYM.value.format(fmt), id) }
    }

    fun generateFutureMonths(monthsAhead: Int) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                repo.generateFutureMonths(_currentYM.value.format(fmt), monthsAhead)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    val currentConfig: StateFlow<com.finanzasfamiliares.data.model.FamilyConfig> = config
}
