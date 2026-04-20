package com.finanzasfamiliares.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class SharedMonthViewModel @Inject constructor() : ViewModel() {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
    private val _yearMonth = MutableStateFlow(YearMonth.now().format(fmt))
    val yearMonth: StateFlow<String> = _yearMonth.asStateFlow()
    fun set(ym: String) { _yearMonth.value = ym }
    fun resetToCurrentMonth() { _yearMonth.value = YearMonth.now().format(fmt) }
}
