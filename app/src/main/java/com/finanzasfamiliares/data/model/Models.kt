package com.finanzasfamiliares.data.model

import com.google.firebase.Timestamp

data class Family(
    val id: String = "",
    val joinCode: String = "",
    val memberIds: List<String> = emptyList()
)

object IncomeCurrency {
    const val USD = "USD"
    const val UYU = "UYU"
}

object ExpenseCategoryCatalog {
    val defaultCategories = listOf(
        "Comida",
        "Ropa",
        "Transporte",
        "Entretenimiento",
        "Servicios Públicos",
        "Salud",
        "Streaming",
        "Educación",
        "Higiene",
        "Belleza",
        "Reparaciones",
        "Hogar",
        "Alquiler",
        "Impuestos",
        "Mascotas"
    )

    fun mergeWithDefaults(customCategories: List<String>): List<String> =
        (defaultCategories + customCategories)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
}

data class MonthData(
    val yearMonth: String = "",
    val exchangeRate: Double = 0.0,
    val cardExchangeOffset: Double = 2.0,
    val primaryIncomeUSD: Double = 0.0,
    val primaryIncomeUYUValue: Double = 0.0,
    val variableIncomes: List<MoneyEntry> = emptyList(),
    val donations: List<Donation> = emptyList(),
    val savings: List<Saving> = emptyList(),
    val fixedExpenses: List<FixedExpense> = emptyList(),
    val variableExpenses: List<MoneyEntry> = emptyList(),
    val cardExpenses: List<CardExpense> = emptyList(),
    val debts: List<DebtEntry> = emptyList()
) {
    val cardExchangeRate: Double get() = exchangeRate + cardExchangeOffset
    val variableIncomeUYU: Double get() = variableIncomes.sumOf { it.totalUYU(exchangeRate) }
    val primaryIncomeUYU: Double get() = primaryIncomeInUYU(IncomeCurrency.USD)
    val totalIncomeUYU: Double get() = totalIncomeInUYU(IncomeCurrency.USD)
    val donationsUYU: Double get() = donationsInUYU(IncomeCurrency.USD)
    val totalObligationsUYU: Double get() = totalObligationsInUYU(IncomeCurrency.USD)
    val marginUYU: Double get() = marginInUYU(IncomeCurrency.USD)
    val marginPercent: Double get() =
        if (totalIncomeUYU > 0) (marginUYU / totalIncomeUYU) * 100 else 0.0

    fun primaryIncomeAmount(currency: String): Double =
        if (currency == IncomeCurrency.UYU) primaryIncomeUYUValue else primaryIncomeUSD

    fun primaryIncomeInUYU(currency: String): Double =
        if (currency == IncomeCurrency.UYU) primaryIncomeUYUValue else primaryIncomeUSD * exchangeRate

    fun donationsInUYU(currency: String): Double =
        donations.sumOf { it.totalUYU(totalIncomeInUYU(currency), cardExchangeRate) }

    fun totalIncomeInUYU(currency: String): Double = primaryIncomeInUYU(currency) + variableIncomeUYU

    fun totalObligationsInUYU(currency: String): Double =
        donationsInUYU(currency) +
        fixedExpenses.sumOf { it.amountUYU } +
        variableExpenses.sumOf { it.totalUYU(cardExchangeRate) } +
        cardExpenses.sumOf { it.totalUYU(cardExchangeRate) } +
        debts.sumOf { it.totalUYU(cardExchangeRate) }

    fun marginInUYU(currency: String): Double = totalIncomeInUYU(currency) - totalObligationsInUYU(currency)
}

data class MoneyEntry(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val amountUSD: Double = 0.0,
    val amountUYU: Double = 0.0,
    val isUSD: Boolean = false,
    val currency: String = ""
) {
    fun currencyCode(): String = when {
        currency == IncomeCurrency.USD || currency == IncomeCurrency.UYU -> currency
        isUSD -> IncomeCurrency.USD
        amountUSD != 0.0 && amountUYU == 0.0 -> IncomeCurrency.USD
        else -> IncomeCurrency.UYU
    }

    fun isInUSD(): Boolean = currencyCode() == IncomeCurrency.USD

    fun totalUYU(exchangeRate: Double): Double =
        if (isInUSD()) amountUSD * exchangeRate else amountUYU
}

data class Donation(
    val id: String = "",
    val name: String = "",
    val amountUSD: Double = 0.0,
    val amountUYU: Double = 0.0,
    val isUSD: Boolean = false,
    val currency: String = "",
    val percentOfPrimaryIncome: Double? = null
) {
    fun currencyCode(): String = when {
        currency == IncomeCurrency.USD || currency == IncomeCurrency.UYU -> currency
        isUSD -> IncomeCurrency.USD
        amountUSD != 0.0 && amountUYU == 0.0 -> IncomeCurrency.USD
        else -> IncomeCurrency.UYU
    }

    fun isInUSD(): Boolean = currencyCode() == IncomeCurrency.USD

    fun totalUYU(totalIncomeUYU: Double, exchangeRate: Double): Double =
        percentOfPrimaryIncome?.let { totalIncomeUYU * (it / 100.0) }
            ?: if (isInUSD()) amountUSD * exchangeRate else amountUYU
}

object CardExpenseKind {
    const val PUNCTUAL = "PUNCTUAL"
    const val RECURRING = "RECURRING"
    const val INSTALLMENT = "INSTALLMENT"
}

data class FixedExpense(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val amountUYU: Double = 0.0,
    val isPinned: Boolean = true
)

data class CardExpense(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val amountUSD: Double = 0.0,
    val amountUYU: Double = 0.0,
    val isUSD: Boolean = false,
    val currency: String = "",
    val kind: String = CardExpenseKind.PUNCTUAL,
    val totalInstallments: Int = 1,
    val currentInstallment: Int = 1
) {
    fun currencyCode(): String = when {
        currency == IncomeCurrency.USD || currency == IncomeCurrency.UYU -> currency
        isUSD -> IncomeCurrency.USD
        amountUSD != 0.0 && amountUYU == 0.0 -> IncomeCurrency.USD
        else -> IncomeCurrency.UYU
    }

    fun isInUSD(): Boolean = currencyCode() == IncomeCurrency.USD

    fun totalUYU(cardExchangeRate: Double): Double =
        if (isInUSD()) amountUSD * cardExchangeRate else amountUYU

    fun advanceToNextMonth(): CardExpense? {
        if (kind == CardExpenseKind.RECURRING) return copy()
        if (kind == CardExpenseKind.PUNCTUAL) return null
        val next = currentInstallment + 1
        return if (next > totalInstallments) null else copy(currentInstallment = next)
    }
}

data class DebtEntry(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val amountUSD: Double = 0.0,
    val amountUYU: Double = 0.0,
    val isUSD: Boolean = false,
    val currency: String = "",
    val totalInstallments: Int = 1,
    val currentInstallment: Int = 1
) {
    fun currencyCode(): String = when {
        currency == IncomeCurrency.USD || currency == IncomeCurrency.UYU -> currency
        isUSD -> IncomeCurrency.USD
        amountUSD != 0.0 && amountUYU == 0.0 -> IncomeCurrency.USD
        else -> IncomeCurrency.UYU
    }

    fun isInUSD(): Boolean = currencyCode() == IncomeCurrency.USD

    fun totalUYU(exchangeRate: Double): Double =
        if (isInUSD()) amountUSD * exchangeRate else amountUYU

    fun advanceToNextMonth(): DebtEntry? {
        val next = currentInstallment + 1
        return if (next > totalInstallments) null else copy(currentInstallment = next)
    }
}

data class Saving(
    val id: String = "",
    val name: String = "",
    val amountUYU: Double = 0.0,
    val amountUSD: Double = 0.0,
    val currency: String = IncomeCurrency.UYU,
    val lastUpdated: Timestamp? = null
) {
    fun currencyCode(): String = when {
        currency == IncomeCurrency.USD || currency == IncomeCurrency.UYU -> currency
        amountUSD != 0.0 && amountUYU == 0.0 -> IncomeCurrency.USD
        else -> IncomeCurrency.UYU
    }

    fun isInUSD(): Boolean = currencyCode() == IncomeCurrency.USD

    fun displayAmount(): Double = if (isInUSD()) amountUSD else amountUYU
}

data class FamilyConfig(
    val incomeCurrency: String = IncomeCurrency.USD,
    val defaultExchangeRate: Double = 0.0,
    val defaultCardExchangeOffset: Double = 0.0,
    val marginGreenThresholdPct: Double = 20000.0,
    val marginYellowThresholdPct: Double = 5000.0,
    val expenseCategories: List<String> = ExpenseCategoryCatalog.defaultCategories,
    val planningThroughYearMonth: String = "",
    val saving1Name: String = "Ahorro 1",
    val saving2Name: String = "Ahorro 2",
    val saving3Name: String = "Ahorro 3"
)
