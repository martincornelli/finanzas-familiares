package com.finanzasfamiliares.ui.screens.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzasfamiliares.data.model.FamilyConfig
import com.finanzasfamiliares.data.model.MonthData
import com.finanzasfamiliares.data.repository.FinanceRepository
import com.finanzasfamiliares.ui.components.formatMonthYearLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModel @Inject constructor(
    private val repo: FinanceRepository
) : ViewModel() {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
    private val _yearMonth = MutableStateFlow(YearMonth.now().format(fmt))
    private val whileSubscribed = SharingStarted.WhileSubscribed(5_000)

    val currentConfig: StateFlow<FamilyConfig> = repo.observeConfig()
        .stateIn(viewModelScope, whileSubscribed, FamilyConfig())

    val monthData: StateFlow<MonthData?> = _yearMonth.flatMapLatest { repo.observeMonth(it) }
        .stateIn(viewModelScope, whileSubscribed, null)

    private val previousYearMonth: StateFlow<String> = _yearMonth
        .map { current -> YearMonth.parse(current, fmt).minusMonths(1).format(fmt) }
        .stateIn(viewModelScope, whileSubscribed, YearMonth.now().minusMonths(1).format(fmt))

    val previousMonthData: StateFlow<MonthData?> = previousYearMonth
        .flatMapLatest { repo.observeMonth(it) }
        .stateIn(viewModelScope, whileSubscribed, null)

    val previousMonthLabel: StateFlow<String> = previousYearMonth
        .map(::formatMonthYearLabel)
        .stateIn(
            viewModelScope,
            whileSubscribed,
            formatMonthYearLabel(YearMonth.now().minusMonths(1).format(fmt))
        )

    fun setYearMonth(yearMonth: String) {
        _yearMonth.value = yearMonth
    }
}
