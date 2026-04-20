package com.finanzasfamiliares.ui.screens.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzasfamiliares.data.model.MoneyEntry
import com.finanzasfamiliares.data.model.MonthData
import com.finanzasfamiliares.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModel @Inject constructor(
    private val repo: FinanceRepository
) : ViewModel() {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
    private val _yearMonth = MutableStateFlow(YearMonth.now().format(fmt))
    private val whileSubscribed = SharingStarted.WhileSubscribed(5_000)

    private val config = repo.observeConfig()
        .stateIn(viewModelScope, whileSubscribed, com.finanzasfamiliares.data.model.FamilyConfig())

    val currentYMString: StateFlow<String> = _yearMonth
        .stateIn(viewModelScope, whileSubscribed, YearMonth.now().format(fmt))

    val monthData: StateFlow<MonthData?> = _yearMonth
        .flatMapLatest { repo.observeMonth(it) }
        .stateIn(viewModelScope, whileSubscribed, null)

    val isFutureMonth: StateFlow<Boolean> = _yearMonth
        .map { YearMonth.parse(it, fmt) > YearMonth.now() }
        .stateIn(viewModelScope, whileSubscribed, false)

    val currentConfig: StateFlow<com.finanzasfamiliares.data.model.FamilyConfig> = config

    fun setYearMonth(yearMonth: String) {
        _yearMonth.value = yearMonth
    }

    fun updateExchangeSettings(rate: Double, cardRate: Double, applyToFuture: Boolean = false) {
        viewModelScope.launch {
            repo.updateMonthExchangeSettings(
                yearMonth = _yearMonth.value,
                rate = rate,
                cardExchangeRate = cardRate,
                applyToFuture = applyToFuture
            )
        }
    }

    fun updatePrimaryIncome(amount: Double, applyToFuture: Boolean = false) {
        viewModelScope.launch {
            val isCurrentMonth = _yearMonth.value == YearMonth.now().format(fmt)
            repo.updatePrimaryIncome(
                yearMonth = _yearMonth.value,
                amount = amount,
                currency = config.value.incomeCurrency,
                applyToFuture = applyToFuture || isCurrentMonth
            )
        }
    }

    fun upsertVariableIncome(income: MoneyEntry) {
        viewModelScope.launch { repo.upsertVariableIncome(_yearMonth.value, income) }
    }

    fun deleteVariableIncome(id: String) {
        viewModelScope.launch { repo.deleteVariableIncome(_yearMonth.value, id) }
    }
}
