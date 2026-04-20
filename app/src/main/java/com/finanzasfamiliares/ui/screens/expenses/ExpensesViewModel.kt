package com.finanzasfamiliares.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzasfamiliares.data.model.CardExpense
import com.finanzasfamiliares.data.model.DebtEntry
import com.finanzasfamiliares.data.model.ExpenseCategoryCatalog
import com.finanzasfamiliares.data.model.FixedExpense
import com.finanzasfamiliares.data.model.MoneyEntry
import com.finanzasfamiliares.data.model.MonthData
import com.finanzasfamiliares.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModel @Inject constructor(private val repo: FinanceRepository) : ViewModel() {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
    private val _yearMonth = MutableStateFlow(YearMonth.now().format(fmt))
    private val whileSubscribed = SharingStarted.WhileSubscribed(5_000)

    val monthData: StateFlow<MonthData?> = _yearMonth.flatMapLatest { repo.observeMonth(it) }
        .stateIn(viewModelScope, whileSubscribed, null)

    val isReadOnly: StateFlow<Boolean> = _yearMonth.map {
        YearMonth.parse(it, fmt) < YearMonth.now()
    }.stateIn(viewModelScope, whileSubscribed, false)

    val categories: StateFlow<List<String>> = repo.observeConfig()
        .map { config -> ExpenseCategoryCatalog.mergeWithDefaults(config.expenseCategories) }
        .stateIn(viewModelScope, whileSubscribed, ExpenseCategoryCatalog.defaultCategories)

    fun setYearMonth(ym: String) { _yearMonth.value = ym }

    fun addCategory(category: String) = viewModelScope.launch {
        repo.addExpenseCategory(category)
    }

    fun upsertFixed(expense: FixedExpense, applyToFuture: Boolean = true) = viewModelScope.launch {
        repo.upsertFixedExpense(_yearMonth.value, expense, applyToFuture)
    }
    fun deleteFixed(id: String, deleteFuture: Boolean = false) = viewModelScope.launch {
        repo.deleteFixedExpense(_yearMonth.value, id, deleteFuture)
    }

    fun setFixedPaid(id: String, isPaid: Boolean) = viewModelScope.launch {
        repo.updateFixedExpensePaid(_yearMonth.value, id, isPaid)
    }

    fun upsertCard(expense: CardExpense, applyToFuture: Boolean = true) = viewModelScope.launch {
        repo.upsertCardExpense(_yearMonth.value, expense, applyToFuture = applyToFuture)
    }
    fun deleteCard(id: String, deleteFuture: Boolean = false) = viewModelScope.launch {
        repo.deleteCardExpense(_yearMonth.value, id, deleteFuture)
    }

    fun setCardPaid(id: String, isPaid: Boolean) = viewModelScope.launch {
        repo.updateCardExpensePaid(_yearMonth.value, id, isPaid)
    }

    fun setAllCardPaid(isPaid: Boolean) = viewModelScope.launch {
        repo.updateAllCardExpensesPaid(_yearMonth.value, isPaid)
    }

    fun upsertVariableExpense(expense: MoneyEntry) = viewModelScope.launch {
        repo.upsertVariableExpense(_yearMonth.value, expense)
    }

    fun deleteVariableExpense(id: String) = viewModelScope.launch {
        repo.deleteVariableExpense(_yearMonth.value, id)
    }

    fun setVariableExpensePaid(id: String, isPaid: Boolean) = viewModelScope.launch {
        repo.updateVariableExpensePaid(_yearMonth.value, id, isPaid)
    }

    fun upsertDebt(debt: DebtEntry, applyToFuture: Boolean = true) = viewModelScope.launch {
        repo.upsertDebt(_yearMonth.value, debt, applyToFuture = applyToFuture)
    }

    fun deleteDebt(id: String, deleteFuture: Boolean = false) = viewModelScope.launch {
        repo.deleteDebt(_yearMonth.value, id, deleteFuture)
    }

    fun setDebtPaid(id: String, isPaid: Boolean) = viewModelScope.launch {
        repo.updateDebtPaid(_yearMonth.value, id, isPaid)
    }

    fun deleteSelected(keys: Set<String>) = viewModelScope.launch {
        keys.forEach { key ->
            val parts = key.split(":", limit = 2)
            if (parts.size != 2) return@forEach
            val (type, id) = parts
            when (type) {
                "fixed" -> repo.deleteFixedExpense(_yearMonth.value, id)
                "variable" -> repo.deleteVariableExpense(_yearMonth.value, id)
                "card" -> repo.deleteCardExpense(_yearMonth.value, id)
                "debt" -> repo.deleteDebt(_yearMonth.value, id)
            }
        }
    }
}
