package com.finanzasfamiliares.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzasfamiliares.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SharedMonthViewModel @Inject constructor(
    private val repo: FinanceRepository
) : ViewModel() {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
    private val _yearMonth = MutableStateFlow(YearMonth.now().format(fmt))
    private val _isGenerating = MutableStateFlow(false)
    private val whileSubscribed = SharingStarted.WhileSubscribed(5_000)

    val yearMonth: StateFlow<String> = _yearMonth.asStateFlow()
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    val monthLabel: StateFlow<String> = _yearMonth
        .map { yearMonth ->
            val ym = YearMonth.parse(yearMonth, fmt)
            val month = ym.month
                .getDisplayName(TextStyle.FULL, Locale("es", "UY"))
                .replaceFirstChar { it.uppercase() }
            "$month ${ym.year}"
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val availableMonths: StateFlow<List<YearMonth>> = repo.observeAvailableMonths()
        .combine(_yearMonth) { monthKeys, currentYm ->
            (monthKeys + currentYm)
                .distinct()
                .map { YearMonth.parse(it, fmt) }
                .sorted()
        }
        .stateIn(viewModelScope, whileSubscribed, listOf(YearMonth.now()))

    val canGoPreviousMonth: StateFlow<Boolean> = combine(availableMonths, _yearMonth) { months, currentYm ->
        val current = YearMonth.parse(currentYm, fmt)
        months.any { it < current }
    }.stateIn(viewModelScope, whileSubscribed, false)

    val canGoNextMonth: StateFlow<Boolean> = combine(availableMonths, _yearMonth) { months, currentYm ->
        val current = YearMonth.parse(currentYm, fmt)
        months.any { it > current }
    }.stateIn(viewModelScope, whileSubscribed, false)

    val isCurrentMonth: StateFlow<Boolean> = _yearMonth
        .map { it == YearMonth.now().format(fmt) }
        .stateIn(viewModelScope, whileSubscribed, true)

    init {
        ensureCurrentMonthDocument(YearMonth.now().format(fmt))
    }

    fun set(yearMonth: String) {
        _yearMonth.value = yearMonth
        ensureCurrentMonthDocument(yearMonth)
    }

    fun goToMonth(target: YearMonth) {
        val yearMonth = target.format(fmt)
        _yearMonth.value = yearMonth
        ensureCurrentMonthDocument(yearMonth)
    }

    fun goToPreviousAvailableMonth() {
        val current = YearMonth.parse(_yearMonth.value, fmt)
        availableMonths.value.lastOrNull { it < current }?.let { target ->
            _yearMonth.value = target.format(fmt)
        }
    }

    fun goToNextAvailableMonth() {
        val current = YearMonth.parse(_yearMonth.value, fmt)
        availableMonths.value.firstOrNull { it > current }?.let { target ->
            _yearMonth.value = target.format(fmt)
        }
    }

    fun resetToCurrentMonth() {
        val currentMonth = YearMonth.now().format(fmt)
        _yearMonth.value = currentMonth
        ensureCurrentMonthDocument(currentMonth)
    }

    fun generateFutureMonths(monthsAhead: Int) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                repo.generateFutureMonths(_yearMonth.value, monthsAhead)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun ensureCurrentMonthDocument(yearMonth: String) {
        if (yearMonth != YearMonth.now().format(fmt)) return
        viewModelScope.launch {
            repo.ensureMonthDocument(yearMonth)
        }
    }
}
