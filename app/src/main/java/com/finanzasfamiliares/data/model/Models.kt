package com.finanzasfamiliares.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
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

@IgnoreExtraProperties
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
    @get:Exclude
    val cardExchangeRate: Double get() = exchangeRate + cardExchangeOffset
    @get:Exclude
    val variableIncomeUYU: Double get() = variableIncomes.sumOf { it.totalUYU(exchangeRate) }
    @get:Exclude
    val primaryIncomeUYU: Double get() = primaryIncomeInUYU(IncomeCurrency.USD)
    @get:Exclude
    val totalIncomeUYU: Double get() = totalIncomeInUYU(IncomeCurrency.USD)
    @get:Exclude
    val donationsUYU: Double get() = donationsInUYU(IncomeCurrency.USD)
    @get:Exclude
    val totalObligationsUYU: Double get() = totalObligationsInUYU(IncomeCurrency.USD)
    @get:Exclude
    val marginUYU: Double get() = marginInUYU(IncomeCurrency.USD)
    @get:Exclude
    val marginPercent: Double get() =
        if (totalIncomeUYU > 0) (marginUYU / totalIncomeUYU) * 100 else 0.0

    @Exclude
    fun primaryIncomeAmount(currency: String): Double =
        if (currency == IncomeCurrency.UYU) primaryIncomeUYUValue else primaryIncomeUSD

    @Exclude
    fun primaryIncomeInUYU(currency: String): Double =
        if (currency == IncomeCurrency.UYU) primaryIncomeUYUValue else primaryIncomeUSD * exchangeRate

    @Exclude
    fun donationsInUYU(currency: String): Double =
        donations.sumOf { it.totalUYU(totalIncomeInUYU(currency), cardExchangeRate) }

    @Exclude
    fun totalIncomeInUYU(currency: String): Double = primaryIncomeInUYU(currency) + variableIncomeUYU

    @Exclude
    fun totalObligationsInUYU(currency: String): Double =
        donationsInUYU(currency) +
        fixedExpenses.sumOf { it.amountUYU } +
        variableExpenses.sumOf { it.totalUYU(cardExchangeRate) } +
        cardExpenses.sumOf { it.totalUYU(cardExchangeRate) } +
        debts.sumOf { it.totalUYU(cardExchangeRate) }

    @Exclude
    fun marginInUYU(currency: String): Double = totalIncomeInUYU(currency) - totalObligationsInUYU(currency)
}

@IgnoreExtraProperties
data class MoneyEntry(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val amountUSD: Double = 0.0,
    val amountUYU: Double = 0.0,
    @get:PropertyName("usd")
    @field:PropertyName("usd")
    val isUSD: Boolean = false,
    val currency: String = "",
    @get:PropertyName("paid")
    @field:PropertyName("paid")
    val isPaid: Boolean = false
) {
    @Exclude
    fun currencyCode(): String = when {
        currency == IncomeCurrency.USD || currency == IncomeCurrency.UYU -> currency
        isUSD -> IncomeCurrency.USD
        amountUSD != 0.0 && amountUYU == 0.0 -> IncomeCurrency.USD
        else -> IncomeCurrency.UYU
    }

    @Exclude
    fun isInUSD(): Boolean = currencyCode() == IncomeCurrency.USD

    @Exclude
    fun totalUYU(exchangeRate: Double): Double =
        if (isInUSD()) amountUSD * exchangeRate else amountUYU

    @Exclude
    fun asPending(): MoneyEntry = copy(isPaid = false)
}

@IgnoreExtraProperties
data class Donation(
    val id: String = "",
    val name: String = "",
    val amountUSD: Double = 0.0,
    val amountUYU: Double = 0.0,
    @get:PropertyName("usd")
    @field:PropertyName("usd")
    val isUSD: Boolean = false,
    val currency: String = "",
    val percentOfPrimaryIncome: Double? = null,
    @get:PropertyName("paid")
    @field:PropertyName("paid")
    val isPaid: Boolean = false
) {
    @Exclude
    fun currencyCode(): String = when {
        currency == IncomeCurrency.USD || currency == IncomeCurrency.UYU -> currency
        isUSD -> IncomeCurrency.USD
        amountUSD != 0.0 && amountUYU == 0.0 -> IncomeCurrency.USD
        else -> IncomeCurrency.UYU
    }

    @Exclude
    fun isInUSD(): Boolean = currencyCode() == IncomeCurrency.USD

    @Exclude
    fun totalUYU(totalIncomeUYU: Double, exchangeRate: Double): Double =
        percentOfPrimaryIncome?.let { totalIncomeUYU * (it / 100.0) }
            ?: if (isInUSD()) amountUSD * exchangeRate else amountUYU

    @Exclude
    fun asPending(): Donation = copy(isPaid = false)
}

object CardExpenseKind {
    const val PUNCTUAL = "PUNCTUAL"
    const val RECURRING = "RECURRING"
    const val INSTALLMENT = "INSTALLMENT"
}

@IgnoreExtraProperties
data class FixedExpense(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val amountUYU: Double = 0.0,
    @get:PropertyName("pinned")
    @field:PropertyName("pinned")
    val isPinned: Boolean = true,
    @get:PropertyName("paid")
    @field:PropertyName("paid")
    val isPaid: Boolean = false
)

@IgnoreExtraProperties
data class CardExpense(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val amountUSD: Double = 0.0,
    val amountUYU: Double = 0.0,
    @get:PropertyName("usd")
    @field:PropertyName("usd")
    val isUSD: Boolean = false,
    val currency: String = "",
    val kind: String = CardExpenseKind.PUNCTUAL,
    val totalInstallments: Int = 1,
    val currentInstallment: Int = 1,
    @get:PropertyName("paid")
    @field:PropertyName("paid")
    val isPaid: Boolean = false
) {
    @Exclude
    fun currencyCode(): String = when {
        currency == IncomeCurrency.USD || currency == IncomeCurrency.UYU -> currency
        isUSD -> IncomeCurrency.USD
        amountUSD != 0.0 && amountUYU == 0.0 -> IncomeCurrency.USD
        else -> IncomeCurrency.UYU
    }

    @Exclude
    fun isInUSD(): Boolean = currencyCode() == IncomeCurrency.USD

    @Exclude
    fun totalUYU(cardExchangeRate: Double): Double =
        if (isInUSD()) amountUSD * cardExchangeRate else amountUYU

    @Exclude
    fun advanceToNextMonth(): CardExpense? {
        if (kind == CardExpenseKind.RECURRING) return copy(isPaid = false)
        if (kind == CardExpenseKind.PUNCTUAL) return null
        val next = currentInstallment + 1
        return if (next > totalInstallments) null else copy(currentInstallment = next, isPaid = false)
    }
}

@IgnoreExtraProperties
data class DebtEntry(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val amountUSD: Double = 0.0,
    val amountUYU: Double = 0.0,
    @get:PropertyName("usd")
    @field:PropertyName("usd")
    val isUSD: Boolean = false,
    val currency: String = "",
    val totalInstallments: Int = 1,
    val currentInstallment: Int = 1,
    @get:PropertyName("paid")
    @field:PropertyName("paid")
    val isPaid: Boolean = false
) {
    @Exclude
    fun currencyCode(): String = when {
        currency == IncomeCurrency.USD || currency == IncomeCurrency.UYU -> currency
        isUSD -> IncomeCurrency.USD
        amountUSD != 0.0 && amountUYU == 0.0 -> IncomeCurrency.USD
        else -> IncomeCurrency.UYU
    }

    @Exclude
    fun isInUSD(): Boolean = currencyCode() == IncomeCurrency.USD

    @Exclude
    fun totalUYU(exchangeRate: Double): Double =
        if (isInUSD()) amountUSD * exchangeRate else amountUYU

    @Exclude
    fun advanceToNextMonth(): DebtEntry? {
        val next = currentInstallment + 1
        return if (next > totalInstallments) null else copy(currentInstallment = next, isPaid = false)
    }
}

@IgnoreExtraProperties
data class Saving(
    val id: String = "",
    val name: String = "",
    val amountUYU: Double = 0.0,
    val amountUSD: Double = 0.0,
    val currency: String = IncomeCurrency.UYU,
    val lastUpdated: Timestamp? = null
) {
    @Exclude
    fun currencyCode(): String = when {
        currency == IncomeCurrency.USD || currency == IncomeCurrency.UYU -> currency
        amountUSD != 0.0 && amountUYU == 0.0 -> IncomeCurrency.USD
        else -> IncomeCurrency.UYU
    }

    @Exclude
    fun isInUSD(): Boolean = currencyCode() == IncomeCurrency.USD

    @Exclude
    fun displayAmount(): Double = if (isInUSD()) amountUSD else amountUYU
}

@IgnoreExtraProperties
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
