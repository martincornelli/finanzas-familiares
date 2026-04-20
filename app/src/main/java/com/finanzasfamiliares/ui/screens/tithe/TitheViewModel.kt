package com.finanzasfamiliares.ui.screens.tithe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzasfamiliares.data.model.Donation
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
class DonationsViewModel @Inject constructor(private val repo: FinanceRepository) : ViewModel() {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
    private val _yearMonth = MutableStateFlow(YearMonth.now().format(fmt))
    private val whileSubscribed = SharingStarted.WhileSubscribed(5_000)

    val monthData: StateFlow<MonthData?> = _yearMonth.flatMapLatest { repo.observeMonth(it) }
        .stateIn(viewModelScope, whileSubscribed, null)

    val isReadOnly: StateFlow<Boolean> = _yearMonth.map {
        YearMonth.parse(it, fmt) < YearMonth.now()
    }.stateIn(viewModelScope, whileSubscribed, false)
    val currentConfig = repo.observeConfig()
        .stateIn(viewModelScope, whileSubscribed, com.finanzasfamiliares.data.model.FamilyConfig())

    fun setYearMonth(ym: String) { _yearMonth.value = ym }

    fun upsertDonation(donation: Donation) = viewModelScope.launch {
        repo.upsertDonation(_yearMonth.value, donation)
    }

    fun deleteDonation(id: String) = viewModelScope.launch {
        repo.deleteDonation(_yearMonth.value, id)
    }

    fun deleteDonations(ids: Set<String>) = viewModelScope.launch {
        ids.forEach { repo.deleteDonation(_yearMonth.value, it) }
    }
}
