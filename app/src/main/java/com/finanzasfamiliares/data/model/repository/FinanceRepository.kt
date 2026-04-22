package com.finanzasfamiliares.data.repository

import android.content.Context
import com.finanzasfamiliares.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class FinanceRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    companion object {
        const val ERROR_INVALID_CODE = "error_invalid_code"
        const val ERROR_NOT_SHARED_FAMILY = "error_not_shared_family"
        private const val PREFS_NAME = "finanzas_familiares_prefs"
        private const val PREF_ACTIVE_FAMILY_ID = "active_family_id"
        private const val JOIN_CODES_COLLECTION = "familyJoinCodes"
        private const val LEGACY_SAVINGS_COLLECTION = "savings"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val repoScope = CoroutineScope(Dispatchers.IO)
    private var _familyId: String? = null
    private val _activeFamilyId = MutableStateFlow(
        prefs.getString(PREF_ACTIVE_FAMILY_ID, null) ?: auth.currentUser?.uid.orEmpty()
    )
    val activeFamilyId: StateFlow<String> = _activeFamilyId
        .stateIn(repoScope, SharingStarted.Eagerly, _activeFamilyId.value)
    private val familyId: String get() = _familyId ?: _activeFamilyId.value

    fun setFamilyId(id: String) {
        _familyId = id
        _activeFamilyId.value = id
        prefs.edit().putString(PREF_ACTIVE_FAMILY_ID, id).apply()
    }

    fun clearFamilyId() {
        _familyId = null
        _activeFamilyId.value = ""
        prefs.edit().remove(PREF_ACTIVE_FAMILY_ID).apply()
    }

    suspend fun signInAnonymouslyIfNeeded() {
        if (auth.currentUser == null) auth.signInAnonymously().await()
    }

    val isSignedIn: Boolean get() = auth.currentUser != null
    val hasFamilyId: Boolean get() = familyId.isNotBlank()

    suspend fun ensureFamily(): Family {
        val activeId = familyId
        if (activeId.isBlank()) {
            return createFamily()
        }

        val activeFamily = runCatching {
            db.collection("families").document(activeId)
                .get().await()
                .toObject(Family::class.java)
        }.getOrNull()
        if (activeFamily != null) {
            setFamilyId(activeId)
            return activeFamily
        }

        clearFamilyId()

        val ownId = auth.currentUser?.uid.orEmpty()
        if (ownId.isBlank()) {
            return createFamily()
        }

        val ownFamily = runCatching {
            db.collection("families").document(ownId)
                .get().await()
                .toObject(Family::class.java)
        }.getOrNull()
        return if (ownFamily != null) {
            setFamilyId(ownId)
            ownFamily
        } else {
            createFamily()
        }
    }

    suspend fun createFamily(): Family {
        val uid = auth.currentUser!!.uid
        val family = createFamilyDocument(id = uid, uid = uid)
        setFamilyId(uid)
        return family
    }

    suspend fun joinFamily(code: String): Result<Family> {
        return try {
            val uid = auth.currentUser!!.uid
            val joinCodeDoc = db.collection(JOIN_CODES_COLLECTION)
                .document(code)
                .get().await()
            val familyId = joinCodeDoc.getString("familyId")
                ?: return Result.failure(Exception(ERROR_INVALID_CODE))

            db.collection("families").document(familyId)
                .update("memberIds", FieldValue.arrayUnion(uid)).await()

            val family = db.collection("families").document(familyId)
                .get().await()
                .toObject(Family::class.java)
                ?: return Result.failure(Exception(ERROR_INVALID_CODE))

            setFamilyId(familyId)
            Result.success(
                if (uid in family.memberIds) family else family.copy(memberIds = family.memberIds + uid)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveCurrentFamily(): Result<Family> {
        return try {
            val uid = auth.currentUser?.uid.orEmpty()
            if (uid.isBlank()) return Result.failure(Exception(ERROR_NOT_SHARED_FAMILY))

            val previousFamilyId = familyId
            val previousFamily = db.collection("families").document(previousFamilyId)
                .get().await()
                .toObject(Family::class.java)
                ?: return Result.failure(Exception(ERROR_NOT_SHARED_FAMILY))

            if (uid !in previousFamily.memberIds || previousFamily.memberIds.size <= 1) {
                return Result.failure(Exception(ERROR_NOT_SHARED_FAMILY))
            }

            val preservedConfig = getConfig().copy(planningThroughYearMonth = "")
            val newFamily = createFamilyDocument(id = UUID.randomUUID().toString(), uid = uid)
            db.collection("families").document(newFamily.id)
                .collection("config").document("main")
                .set(preservedConfig).await()

            db.collection("families").document(previousFamilyId)
                .update("memberIds", FieldValue.arrayRemove(uid)).await()

            setFamilyId(newFamily.id)
            runCatching { initSavingsIfNeeded(preservedConfig) }

            Result.success(newFamily)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentFamily(): Family? =
        db.collection("families").document(familyId)
            .get().await().toObject(Family::class.java)

    suspend fun ensureJoinCodeLookupForCurrentFamily() {
        val family = getCurrentFamily() ?: return
        ensureJoinCodeLookup(family)
    }

    fun observeMonth(yearMonth: String): Flow<MonthData?> =
        activeFamilyId.flatMapLatest { activeId ->
            if (activeId.isBlank()) return@flatMapLatest flowOf(null)
            callbackFlow {
                val ref = db.collection("families").document(activeId)
                    .collection("months").document(yearMonth)
                val sub = ref.addSnapshotListener { snap, error ->
                    if (error != null) {
                        if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            trySend(null)
                            return@addSnapshotListener
                        }
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(snap?.toObject(MonthData::class.java))
                }
                awaitClose { sub.remove() }
            }
        }

    fun observeAvailableMonths(): Flow<List<String>> =
        activeFamilyId.flatMapLatest { activeId ->
            if (activeId.isBlank()) return@flatMapLatest flowOf(emptyList())
            callbackFlow {
                val ref = db.collection("families").document(activeId)
                    .collection("months")
                    .orderBy(FieldPath.documentId())
                val sub = ref.addSnapshotListener { snap, error ->
                    if (error != null) {
                        if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(snap?.documents?.map { it.id } ?: emptyList())
                }
                awaitClose { sub.remove() }
            }
        }

    suspend fun getMonth(yearMonth: String): MonthData? =
        db.collection("families").document(familyId)
            .collection("months").document(yearMonth)
            .get().await().toObject(MonthData::class.java)

    suspend fun saveMonth(month: MonthData) {
        db.collection("families").document(familyId)
            .collection("months").document(month.yearMonth)
            .set(month).await()
    }

    suspend fun rolloverToNextMonth(currentYearMonth: String): MonthData {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
        val nextKey = YearMonth.parse(currentYearMonth, fmt).plusMonths(1).format(fmt)
        val existing = getMonth(nextKey)
        if (existing != null) return existing
        val current = ensureMonthDocument(currentYearMonth)
        val nextMonth = MonthData(
            yearMonth = nextKey,
            exchangeRate = current.exchangeRate,
            cardExchangeOffset = current.cardExchangeOffset,
            primaryIncomeUSD = current.primaryIncomeUSD,
            primaryIncomeUYUValue = current.primaryIncomeUYUValue,
            donations = current.donations
                .filter { it.percentOfPrimaryIncome != null }
                .map { it.asPending() },
            savings = current.savings,
            fixedExpenses = current.fixedExpenses.map { it.copy(isPaid = false) },
            cardExpenses = current.cardExpenses.mapNotNull { it.advanceToNextMonth() },
            debts = current.debts.mapNotNull { it.advanceToNextMonth() }
        )
        saveMonth(nextMonth)
        return nextMonth
    }

    suspend fun upsertFixedExpense(
        yearMonth: String,
        expense: FixedExpense,
        applyToFuture: Boolean = true
    ) {
        val month = ensureMonthDocument(yearMonth)
        val updated = month.fixedExpenses.toMutableList()
        val idx = updated.indexOfFirst { it.id == expense.id }
        val savedExpense = if (idx >= 0) {
            expense.copy(isPinned = true)
        } else {
            expense.copy(id = UUID.randomUUID().toString(), isPinned = true)
        }
        if (idx >= 0) {
            updated[idx] = savedExpense
        } else {
            updated.add(savedExpense)
        }
        saveMonth(month.copy(fixedExpenses = updated))
        if (applyToFuture) {
            applyFixedExpenseToFuture(yearMonth, savedExpense)
        }
    }

    suspend fun deleteFixedExpense(yearMonth: String, id: String, deleteFuture: Boolean = false) {
        val month = getMonth(yearMonth) ?: return
        saveMonth(month.copy(fixedExpenses = month.fixedExpenses.filter { it.id != id }))
        if (deleteFuture) {
            removeFixedExpenseFromFuture(yearMonth, id)
        }
    }

    suspend fun updateFixedExpensePaid(yearMonth: String, id: String, isPaid: Boolean) {
        val month = getMonth(yearMonth) ?: return
        val updated = month.fixedExpenses.map { expense ->
            if (expense.id == id) expense.copy(isPaid = isPaid) else expense
        }
        saveMonth(month.copy(fixedExpenses = updated))
    }

    suspend fun upsertCardExpense(yearMonth: String, expense: CardExpense, applyToFuture: Boolean = false) {
        val month = ensureMonthDocument(yearMonth)
        val updated = month.cardExpenses.toMutableList()
        val idx = updated.indexOfFirst { it.id == expense.id }
        if (idx >= 0) updated[idx] = expense else updated.add(expense.copy(id = UUID.randomUUID().toString()))
        saveMonth(month.copy(cardExpenses = updated))
        if (applyToFuture) {
            applyCardExpenseToFuture(yearMonth, if (idx >= 0) expense else updated.last())
        }
    }

    suspend fun deleteCardExpense(yearMonth: String, id: String, deleteFuture: Boolean = false) {
        val month = getMonth(yearMonth) ?: return
        saveMonth(month.copy(cardExpenses = month.cardExpenses.filter { it.id != id }))
        if (deleteFuture) {
            removeCardExpenseFromFuture(yearMonth, id)
        }
    }

    suspend fun updateCardExpensePaid(yearMonth: String, id: String, isPaid: Boolean) {
        val month = getMonth(yearMonth) ?: return
        val updated = month.cardExpenses.map { expense ->
            if (expense.id == id) expense.copy(isPaid = isPaid) else expense
        }
        saveMonth(month.copy(cardExpenses = updated))
    }

    suspend fun updateAllCardExpensesPaid(yearMonth: String, isPaid: Boolean) {
        val month = getMonth(yearMonth) ?: return
        if (month.cardExpenses.isEmpty()) return
        saveMonth(
            month.copy(
                cardExpenses = month.cardExpenses.map { it.copy(isPaid = isPaid) }
            )
        )
    }

    suspend fun upsertDebt(yearMonth: String, debt: DebtEntry, applyToFuture: Boolean = false) {
        val month = ensureMonthDocument(yearMonth)
        val updated = month.debts.toMutableList()
        val idx = updated.indexOfFirst { it.id == debt.id }
        if (idx >= 0) updated[idx] = debt else updated.add(debt.copy(id = UUID.randomUUID().toString()))
        saveMonth(month.copy(debts = updated))
        if (applyToFuture) {
            applyDebtToFuture(yearMonth, if (idx >= 0) debt else updated.last())
        }
    }

    suspend fun deleteDebt(yearMonth: String, id: String, deleteFuture: Boolean = false) {
        val month = getMonth(yearMonth) ?: return
        saveMonth(month.copy(debts = month.debts.filter { it.id != id }))
        if (deleteFuture) {
            removeDebtFromFuture(yearMonth, id)
        }
    }

    suspend fun updateDebtPaid(yearMonth: String, id: String, isPaid: Boolean) {
        val month = getMonth(yearMonth) ?: return
        val updated = month.debts.map { debt ->
            if (debt.id == id) debt.copy(isPaid = isPaid) else debt
        }
        saveMonth(month.copy(debts = updated))
    }

    suspend fun upsertVariableIncome(yearMonth: String, income: MoneyEntry) {
        val month = ensureMonthDocument(yearMonth)
        val updated = month.variableIncomes.toMutableList()
        val idx = updated.indexOfFirst { it.id == income.id }
        if (idx >= 0) updated[idx] = income else updated.add(income.copy(id = UUID.randomUUID().toString()))
        saveMonth(month.copy(variableIncomes = updated))
    }

    suspend fun deleteVariableIncome(yearMonth: String, id: String) {
        val month = getMonth(yearMonth) ?: return
        saveMonth(month.copy(variableIncomes = month.variableIncomes.filter { it.id != id }))
    }

    suspend fun deleteVariableIncomes(yearMonth: String, ids: Set<String>) {
        if (ids.isEmpty()) return
        val month = getMonth(yearMonth) ?: return
        saveMonth(month.copy(variableIncomes = month.variableIncomes.filterNot { it.id in ids }))
    }

    suspend fun upsertVariableExpense(yearMonth: String, expense: MoneyEntry) {
        val month = ensureMonthDocument(yearMonth)
        val updated = month.variableExpenses.toMutableList()
        val idx = updated.indexOfFirst { it.id == expense.id }
        if (idx >= 0) updated[idx] = expense else updated.add(expense.copy(id = UUID.randomUUID().toString()))
        saveMonth(month.copy(variableExpenses = updated))
    }

    suspend fun deleteVariableExpense(yearMonth: String, id: String) {
        val month = getMonth(yearMonth) ?: return
        saveMonth(month.copy(variableExpenses = month.variableExpenses.filter { it.id != id }))
    }

    suspend fun updateVariableExpensePaid(yearMonth: String, id: String, isPaid: Boolean) {
        val month = getMonth(yearMonth) ?: return
        val updated = month.variableExpenses.map { expense ->
            if (expense.id == id) expense.copy(isPaid = isPaid) else expense
        }
        saveMonth(month.copy(variableExpenses = updated))
    }

    suspend fun upsertDonation(yearMonth: String, donation: Donation) {
        val month = ensureMonthDocument(yearMonth)
        val updated = month.donations.toMutableList()
        val idx = updated.indexOfFirst { it.id == donation.id }
        if (idx >= 0) updated[idx] = donation else updated.add(donation.copy(id = UUID.randomUUID().toString()))
        saveMonth(month.copy(donations = updated))
    }

    suspend fun deleteDonation(yearMonth: String, id: String) {
        val month = getMonth(yearMonth) ?: return
        saveMonth(month.copy(donations = month.donations.filter { it.id != id }))
    }

    suspend fun updateDonationPaid(yearMonth: String, id: String, isPaid: Boolean) {
        val month = getMonth(yearMonth) ?: return
        val updated = month.donations.map { donation ->
            if (donation.id == id) donation.copy(isPaid = isPaid) else donation
        }
        saveMonth(month.copy(donations = updated))
    }

    suspend fun updatePrimaryIncome(
        yearMonth: String,
        amount: Double,
        currency: String,
        applyToFuture: Boolean = false
    ) {
        val month = ensureMonthDocument(yearMonth)
        val updatedMonth = if (currency == IncomeCurrency.UYU) {
            month.copy(primaryIncomeUYUValue = amount)
        } else {
            month.copy(primaryIncomeUSD = amount)
        }
        saveMonth(updatedMonth)
        if (applyToFuture) {
            val futureMonths = getFutureMonthsFrom(yearMonth)
            futureMonths.forEach { future ->
                val updatedFuture = if (currency == IncomeCurrency.UYU) {
                    future.copy(primaryIncomeUYUValue = amount)
                } else {
                    future.copy(primaryIncomeUSD = amount)
                }
                saveMonth(updatedFuture)
            }
        }
    }

    suspend fun updateExchangeRate(yearMonth: String, rate: Double, applyToFuture: Boolean = false) {
        val month = ensureMonthDocument(yearMonth)
        saveMonth(month.copy(exchangeRate = rate))
        if (applyToFuture) {
            val futureMonths = getFutureMonthsFrom(yearMonth)
            futureMonths.forEach { future ->
                saveMonth(future.copy(exchangeRate = rate))
            }
        }
    }

    suspend fun updateMonthExchangeSettings(
        yearMonth: String,
        rate: Double,
        cardExchangeRate: Double,
        applyToFuture: Boolean = false
    ) {
        val normalizedCardRate = cardExchangeRate.coerceAtLeast(rate)
        val offset = normalizedCardRate - rate
        val month = ensureMonthDocument(yearMonth)
        saveMonth(
            month.copy(
                exchangeRate = rate,
                cardExchangeOffset = offset
            )
        )
        if (applyToFuture) {
            val futureMonths = getFutureMonthsFrom(yearMonth)
            futureMonths.forEach { future ->
                saveMonth(
                    future.copy(
                        exchangeRate = rate,
                        cardExchangeOffset = offset
                    )
                )
            }
        }
    }

    suspend fun updateExchangeSettingsFromCurrentMonth(rate: Double, cardExchangeOffset: Double) {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
        val currentKey = YearMonth.now().format(fmt)
        val currentMonth = getMonth(currentKey) ?: MonthData(yearMonth = currentKey)
        saveMonth(currentMonth.copy(exchangeRate = rate, cardExchangeOffset = cardExchangeOffset))

        val currentAndFutureMonths = db.collection("families").document(familyId)
            .collection("months")
            .whereGreaterThanOrEqualTo(FieldPath.documentId(), currentKey)
            .get().await()
            .documents
            .mapNotNull { it.toObject(MonthData::class.java) }
            .filter { it.yearMonth != currentKey }

        currentAndFutureMonths.forEach { month ->
            saveMonth(month.copy(exchangeRate = rate, cardExchangeOffset = cardExchangeOffset))
        }
    }

    suspend fun generateFutureMonths(fromYearMonth: String, monthsAhead: Int): String {
        if (monthsAhead <= 0) return fromYearMonth
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
        val lastAvailableKey = getAvailableMonthKeys().lastOrNull()
        val startKey = when {
            lastAvailableKey == null -> fromYearMonth
            lastAvailableKey >= fromYearMonth -> lastAvailableKey
            else -> fromYearMonth
        }
        var current = ensureMonthDocument(startKey)
        var lastCreatedKey = current.yearMonth
        repeat(monthsAhead) {
            val nextKey = YearMonth.parse(current.yearMonth, fmt).plusMonths(1).format(fmt)
            val existing = getMonth(nextKey)
            if (existing != null) {
                current = existing
                lastCreatedKey = existing.yearMonth
            } else {
                val nextMonth = MonthData(
                    yearMonth = nextKey,
                    exchangeRate = current.exchangeRate,
                    cardExchangeOffset = current.cardExchangeOffset,
                primaryIncomeUSD = current.primaryIncomeUSD,
                primaryIncomeUYUValue = current.primaryIncomeUYUValue,
                donations = current.donations
                    .filter { it.percentOfPrimaryIncome != null }
                    .map { it.asPending() },
                savings = current.savings,
                fixedExpenses = current.fixedExpenses.map { it.copy(isPaid = false) },
                cardExpenses = current.cardExpenses.mapNotNull { it.advanceToNextMonth() },
                debts = current.debts.mapNotNull { it.advanceToNextMonth() }
            )
                saveMonth(nextMonth)
                current = nextMonth
                lastCreatedKey = nextKey
            }
        }
        val config = getConfig()
        if (config.planningThroughYearMonth < lastCreatedKey) {
            saveConfig(config.copy(planningThroughYearMonth = lastCreatedKey))
        }
        return lastCreatedKey
    }

    suspend fun deleteMonthsFrom(yearMonth: String): String {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
        val currentKey = YearMonth.now().format(fmt)
        val monthKeys = getAvailableMonthKeys()
        val monthsToDelete = monthKeys.filter { it >= yearMonth }
        val remainingMonths = monthKeys.filter { it < yearMonth }
        val nextSelectedMonth = remainingMonths.lastOrNull() ?: currentKey

        monthsToDelete.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { key ->
                val ref = db.collection("families").document(familyId)
                    .collection("months").document(key)
                batch.delete(ref)
            }
            batch.commit().await()
        }

        val config = getConfig()
        val newPlanningLimit = remainingMonths.lastOrNull().orEmpty()
        if (config.planningThroughYearMonth != newPlanningLimit) {
            saveConfig(config.copy(planningThroughYearMonth = newPlanningLimit))
        }

        if (nextSelectedMonth == currentKey && currentKey !in remainingMonths) {
            ensureMonthDocument(currentKey)
        }

        return nextSelectedMonth
    }

    fun observeSavings(yearMonth: String): Flow<List<Saving>> =
        observeMonth(yearMonth).map { it?.savings ?: emptyList() }

    suspend fun upsertSaving(yearMonth: String, saving: Saving, applyToFuture: Boolean = true) {
        val month = ensureMonthDocument(yearMonth)
        val updated = month.savings.toMutableList()
        val idx = updated.indexOfFirst { it.id == saving.id }
        if (idx >= 0) {
            updated[idx] = saving
        } else {
            updated.add(saving)
        }
        saveMonth(month.copy(savings = updated))
        if (applyToFuture) {
            applySavingToFuture(yearMonth, saving)
        }
    }

    suspend fun adjustSaving(
        yearMonth: String,
        saving: Saving,
        newName: String,
        deltaAmount: Double,
        lastUpdated: Timestamp = Timestamp.now()
    ) {
        val month = ensureMonthDocument(yearMonth)
        val updated = month.savings.toMutableList()
        val idx = updated.indexOfFirst { it.id == saving.id }
        val currentSaving = if (idx >= 0) updated[idx] else saving
        val adjustedSaving = currentSaving.applyDelta(
            newName = newName,
            deltaAmount = deltaAmount,
            lastUpdated = lastUpdated
        )

        if (idx >= 0) {
            updated[idx] = adjustedSaving
        } else {
            updated.add(adjustedSaving)
        }

        saveMonth(month.copy(savings = updated))
        applySavingDeltaToFuture(
            yearMonth = yearMonth,
            savingId = adjustedSaving.id,
            newName = newName,
            deltaAmount = deltaAmount,
            currency = adjustedSaving.currencyCode(),
            lastUpdated = lastUpdated
        )
    }

    suspend fun deleteSaving(yearMonth: String, id: String) {
        val month = getMonth(yearMonth) ?: return
        saveMonth(month.copy(savings = month.savings.filter { it.id != id }))
        removeSavingFromFuture(yearMonth, id)
    }

    suspend fun initSavingsIfNeeded(config: FamilyConfig) {
        val currentKey = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val currentMonth = getMonth(currentKey)
        if (currentMonth?.savings?.isNotEmpty() == true) return

        val template = loadSavingsTemplate(currentKey, config)
        val monthToSave = currentMonth ?: MonthData(
            yearMonth = currentKey,
            exchangeRate = config.defaultExchangeRate,
            cardExchangeOffset = config.defaultCardExchangeOffset
        )
        saveMonth(monthToSave.copy(savings = template))
    }

    suspend fun syncSavingsNames(config: FamilyConfig) {
        val byId = mapOf(
            "saving_1" to config.saving1Name,
            "saving_2" to config.saving2Name,
            "saving_3" to config.saving3Name
        )
        val monthKeys = getAvailableMonthKeys()
        monthKeys.forEach { key ->
            val month = getMonth(key) ?: return@forEach
            if (month.savings.isEmpty()) return@forEach
            val updatedSavings = month.savings.map { saving ->
                val newName = byId[saving.id] ?: return@map saving
                if (saving.name == newName) saving else saving.copy(name = newName)
            }
            if (updatedSavings != month.savings) {
                saveMonth(month.copy(savings = updatedSavings))
            }
        }
    }

    fun observeConfig(): Flow<FamilyConfig> =
        activeFamilyId.flatMapLatest { activeId ->
            if (activeId.isBlank()) return@flatMapLatest flowOf(FamilyConfig())
            callbackFlow {
                val ref = db.collection("families").document(activeId)
                    .collection("config").document("main")
                val sub = ref.addSnapshotListener { snap, error ->
                    if (error != null) {
                        if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            trySend(FamilyConfig())
                            return@addSnapshotListener
                        }
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(snap?.toObject(FamilyConfig::class.java) ?: FamilyConfig())
                }
                awaitClose { sub.remove() }
            }
        }

    suspend fun getConfig(): FamilyConfig =
        db.collection("families").document(familyId)
            .collection("config").document("main")
            .get().await().toObject(FamilyConfig::class.java) ?: FamilyConfig()

    suspend fun saveConfig(config: FamilyConfig) {
        db.collection("families").document(familyId)
            .collection("config").document("main")
            .set(config).await()
    }

    suspend fun addExpenseCategory(category: String): List<String> {
        val trimmed = category.trim()
        if (trimmed.isBlank()) return ExpenseCategoryCatalog.mergeWithDefaults(getConfig().expenseCategories)

        val config = getConfig()
        val updatedCategories = ExpenseCategoryCatalog.mergeWithDefaults(config.expenseCategories + trimmed)
        saveConfig(config.copy(expenseCategories = updatedCategories))
        return updatedCategories
    }

    suspend fun ensureMonthDocument(yearMonth: String): MonthData {
        getMonth(yearMonth)?.let { existing ->
            if (existing.savings.isNotEmpty()) return existing

            val template = loadSavingsTemplate(yearMonth)
            val updated = existing.copy(savings = template)
            saveMonth(updated)
            return updated
        }

        val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
        val previousKey = YearMonth.parse(yearMonth, fmt).minusMonths(1).format(fmt)
        val previousMonth = getMonth(previousKey)
        val month = if (previousMonth != null) {
            MonthData(
                yearMonth = yearMonth,
                exchangeRate = previousMonth.exchangeRate,
                cardExchangeOffset = previousMonth.cardExchangeOffset,
                primaryIncomeUSD = previousMonth.primaryIncomeUSD,
                primaryIncomeUYUValue = previousMonth.primaryIncomeUYUValue,
                donations = previousMonth.donations
                    .filter { it.percentOfPrimaryIncome != null }
                    .map { it.asPending() },
                savings = previousMonth.savings,
                fixedExpenses = previousMonth.fixedExpenses.map { it.copy(isPaid = false) },
                cardExpenses = previousMonth.cardExpenses.mapNotNull { it.advanceToNextMonth() },
                debts = previousMonth.debts.mapNotNull { it.advanceToNextMonth() }
            )
        } else {
            val config = getConfig()
            MonthData(
                yearMonth = yearMonth,
                exchangeRate = config.defaultExchangeRate,
                cardExchangeOffset = config.defaultCardExchangeOffset,
                savings = loadSavingsTemplate(yearMonth, config)
            )
        }
        saveMonth(month)
        return month
    }

    private suspend fun ensureJoinCodeLookup(family: Family) {
        val joinCodeRef = db.collection(JOIN_CODES_COLLECTION).document(family.joinCode)
        val existing = joinCodeRef.get().await()
        if (!existing.exists()) {
            joinCodeRef.set(
                mapOf(
                    "familyId" to family.id
                )
            ).await()
        }
    }

    private suspend fun createFamilyDocument(id: String, uid: String): Family {
        val family = Family(
            id = id,
            joinCode = generateUniqueJoinCode(),
            memberIds = listOf(uid)
        )
        db.collection("families").document(id).set(family).await()
        ensureJoinCodeLookup(family)
        return family
    }

    private suspend fun generateUniqueJoinCode(): String {
        repeat(20) {
            val code = (100000..999999).random().toString()
            val exists = db.collection(JOIN_CODES_COLLECTION).document(code)
                .get().await()
                .exists()
            if (!exists) return code
        }
        return UUID.randomUUID().toString().takeLast(6)
    }

    private suspend fun getFutureMonthsFrom(yearMonth: String): List<MonthData> =
        db.collection("families").document(familyId)
            .collection("months")
            .whereGreaterThan(FieldPath.documentId(), yearMonth)
            .get().await()
            .documents
            .mapNotNull { it.toObject(MonthData::class.java) }
            .sortedBy { it.yearMonth }

    private suspend fun getAvailableMonthKeys(): List<String> =
        db.collection("families").document(familyId)
            .collection("months")
            .orderBy(FieldPath.documentId())
            .get().await()
            .documents
            .map { it.id }

    private suspend fun getLegacySavings(): List<Saving> =
        db.collection("families").document(familyId)
            .collection(LEGACY_SAVINGS_COLLECTION)
            .get().await()
            .documents
            .mapNotNull { it.toObject(Saving::class.java) }

    private fun defaultSavings(config: FamilyConfig): List<Saving> = listOf(
        Saving(id = "saving_1", name = config.saving1Name),
        Saving(id = "saving_2", name = config.saving2Name),
        Saving(id = "saving_3", name = config.saving3Name)
    )

    private suspend fun loadSavingsTemplate(
        yearMonth: String,
        config: FamilyConfig? = null
    ): List<Saving> {
        val currentSavings = getMonth(yearMonth)?.savings
        if (!currentSavings.isNullOrEmpty()) return currentSavings

        val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
        val previousKey = YearMonth.parse(yearMonth, fmt).minusMonths(1).format(fmt)
        val previousSavings = getMonth(previousKey)?.savings
        if (!previousSavings.isNullOrEmpty()) return previousSavings

        val legacySavings = getLegacySavings()
        if (legacySavings.isNotEmpty()) return legacySavings

        val resolvedConfig = config ?: getConfig()
        return defaultSavings(resolvedConfig)
    }

    private suspend fun applyFixedExpenseToFuture(yearMonth: String, expense: FixedExpense) {
        val futureMonths = getFutureMonthsFrom(yearMonth)
        futureMonths.forEach { future ->
            val updated = future.fixedExpenses.toMutableList()
            val idx = updated.indexOfFirst { it.id == expense.id }
            if (idx >= 0) {
                updated[idx] = expense.copy(
                    isPinned = true,
                    isPaid = updated[idx].isPaid
                )
            } else {
                updated.add(expense.copy(isPinned = true, isPaid = false))
            }
            saveMonth(future.copy(fixedExpenses = updated))
        }
    }

    private suspend fun removeFixedExpenseFromFuture(yearMonth: String, id: String) {
        val futureMonths = getFutureMonthsFrom(yearMonth)
        futureMonths.forEach { future ->
            val updated = future.fixedExpenses.filter { it.id != id }
            if (updated.size != future.fixedExpenses.size) {
                saveMonth(future.copy(fixedExpenses = updated))
            }
        }
    }

    private suspend fun removeCardExpenseFromFuture(yearMonth: String, id: String) {
        val futureMonths = getFutureMonthsFrom(yearMonth)
        futureMonths.forEach { future ->
            val updated = future.cardExpenses.filter { it.id != id }
            if (updated.size != future.cardExpenses.size) {
                saveMonth(future.copy(cardExpenses = updated))
            }
        }
    }

    private suspend fun removeDebtFromFuture(yearMonth: String, id: String) {
        val futureMonths = getFutureMonthsFrom(yearMonth)
        futureMonths.forEach { future ->
            val updated = future.debts.filter { it.id != id }
            if (updated.size != future.debts.size) {
                saveMonth(future.copy(debts = updated))
            }
        }
    }

    private suspend fun applySavingToFuture(yearMonth: String, saving: Saving) {
        val futureMonths = getFutureMonthsFrom(yearMonth)
        futureMonths.forEach { future ->
            val updated = future.savings.toMutableList()
            val idx = updated.indexOfFirst { it.id == saving.id }
            if (idx >= 0) {
                updated[idx] = saving
            } else {
                updated.add(saving)
            }
            saveMonth(future.copy(savings = updated))
        }
    }

    private suspend fun removeSavingFromFuture(yearMonth: String, id: String) {
        val futureMonths = getFutureMonthsFrom(yearMonth)
        futureMonths.forEach { future ->
            val updated = future.savings.filter { it.id != id }
            if (updated.size != future.savings.size) {
                saveMonth(future.copy(savings = updated))
            }
        }
    }

    private suspend fun applySavingDeltaToFuture(
        yearMonth: String,
        savingId: String,
        newName: String,
        deltaAmount: Double,
        currency: String,
        lastUpdated: Timestamp
    ) {
        val futureMonths = getFutureMonthsFrom(yearMonth)
        futureMonths.forEach { future ->
            val updated = future.savings.toMutableList()
            val idx = updated.indexOfFirst { it.id == savingId }
            if (idx >= 0) {
                updated[idx] = updated[idx].applyDelta(
                    newName = newName,
                    deltaAmount = deltaAmount,
                    lastUpdated = lastUpdated
                )
            } else if (deltaAmount > 0.0) {
                updated.add(
                    Saving(
                        id = savingId,
                        name = newName,
                        amountUYU = if (currency == IncomeCurrency.UYU) deltaAmount else 0.0,
                        amountUSD = if (currency == IncomeCurrency.USD) deltaAmount else 0.0,
                        currency = currency,
                        lastUpdated = lastUpdated
                    )
                )
            }
            saveMonth(future.copy(savings = updated))
        }
    }

    private fun Saving.applyDelta(
        newName: String,
        deltaAmount: Double,
        lastUpdated: Timestamp
    ): Saving {
        return if (isInUSD()) {
            copy(
                name = newName,
                amountUSD = (amountUSD + deltaAmount).coerceAtLeast(0.0),
                lastUpdated = lastUpdated
            )
        } else {
            copy(
                name = newName,
                amountUYU = (amountUYU + deltaAmount).coerceAtLeast(0.0),
                lastUpdated = lastUpdated
            )
        }
    }

    private suspend fun applyCardExpenseToFuture(yearMonth: String, expense: CardExpense) {
        val futureMonths = getFutureMonthsFrom(yearMonth)
        var propagatedExpense = expense.advanceToNextMonth()?.copy(id = expense.id)
        futureMonths.forEach { future ->
            val updated = future.cardExpenses.toMutableList()
            val idx = updated.indexOfFirst { it.id == expense.id }
            var changed = false
            if (propagatedExpense != null) {
                val pendingExpense = if (idx >= 0) {
                    propagatedExpense!!.copy(isPaid = updated[idx].isPaid)
                } else {
                    propagatedExpense!!.copy(isPaid = false)
                }
                if (idx >= 0) {
                    updated[idx] = pendingExpense
                } else {
                    updated.add(pendingExpense)
                }
                changed = true
            } else if (idx >= 0) {
                updated.removeAt(idx)
                changed = true
            }
            if (changed) {
                saveMonth(future.copy(cardExpenses = updated))
            }
            propagatedExpense = propagatedExpense?.advanceToNextMonth()?.copy(id = expense.id)
        }
    }

    private suspend fun applyDebtToFuture(yearMonth: String, debt: DebtEntry) {
        val futureMonths = getFutureMonthsFrom(yearMonth)
        var propagatedDebt = debt.advanceToNextMonth()?.copy(id = debt.id)
        futureMonths.forEach { future ->
            val updated = future.debts.toMutableList()
            val idx = updated.indexOfFirst { it.id == debt.id }
            var changed = false
            if (propagatedDebt != null) {
                val pendingDebt = if (idx >= 0) {
                    propagatedDebt!!.copy(isPaid = updated[idx].isPaid)
                } else {
                    propagatedDebt!!.copy(isPaid = false)
                }
                if (idx >= 0) {
                    updated[idx] = pendingDebt
                } else {
                    updated.add(pendingDebt)
                }
                changed = true
            } else if (idx >= 0) {
                updated.removeAt(idx)
                changed = true
            }
            if (changed) {
                saveMonth(future.copy(debts = updated))
            }
            propagatedDebt = propagatedDebt?.advanceToNextMonth()?.copy(id = debt.id)
        }
    }
}
