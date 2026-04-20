package com.finanzasfamiliares.ui.screens.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzasfamiliares.data.model.IncomeCurrency
import com.finanzasfamiliares.data.model.Saving
import com.finanzasfamiliares.data.repository.FinanceRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SavingsViewModel @Inject constructor(private val repo: FinanceRepository) : ViewModel() {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
    private val _yearMonth = MutableStateFlow(YearMonth.now().format(fmt))
    private val whileSubscribed = SharingStarted.WhileSubscribed(5_000)

    val savings: StateFlow<List<Saving>> = _yearMonth.flatMapLatest { repo.observeSavings(it) }
        .stateIn(viewModelScope, whileSubscribed, emptyList())
    val totalUYU: StateFlow<Double> = savings.map { list ->
        list.filter { !it.isInUSD() }.sumOf { it.amountUYU }
    }
        .stateIn(viewModelScope, whileSubscribed, 0.0)
    val totalUSD: StateFlow<Double> = savings.map { list ->
        list.filter { it.isInUSD() }.sumOf { it.amountUSD }
    }
        .stateIn(viewModelScope, whileSubscribed, 0.0)

    fun setYearMonth(yearMonth: String) {
        _yearMonth.value = yearMonth
        viewModelScope.launch {
            repo.ensureMonthDocument(yearMonth)
        }
    }

    fun updateSaving(saving: Saving, newName: String, newAmount: Double, currency: String) = viewModelScope.launch {
        repo.upsertSaving(
            _yearMonth.value,
            saving.copy(
                name = newName,
                amountUYU = if (currency == IncomeCurrency.UYU) newAmount else 0.0,
                amountUSD = if (currency == IncomeCurrency.USD) newAmount else 0.0,
                currency = currency,
                lastUpdated = Timestamp.now()
            )
        )
    }

    fun createSaving(name: String, amount: Double, currency: String) = viewModelScope.launch {
        repo.upsertSaving(
            _yearMonth.value,
            Saving(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                amountUYU = if (currency == IncomeCurrency.UYU) amount else 0.0,
                amountUSD = if (currency == IncomeCurrency.USD) amount else 0.0,
                currency = currency,
                lastUpdated = Timestamp.now()
            )
        )
    }

    fun deleteSaving(id: String) = viewModelScope.launch {
        repo.deleteSaving(_yearMonth.value, id)
    }

    fun deleteSavings(ids: Set<String>) = viewModelScope.launch {
        ids.forEach { repo.deleteSaving(_yearMonth.value, it) }
    }
}
