package com.finanzasfamiliares.ui.screens.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzasfamiliares.R
import com.finanzasfamiliares.data.model.FamilyConfig
import com.finanzasfamiliares.data.model.IncomeCurrency
import com.finanzasfamiliares.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupWizardData(
    val incomeCurrency: String,
    val purchaseRate: Double,
    val saleRate: Double,
    val greenThreshold: Double,
    val yellowThreshold: Double,
    val familyCode: String
)

@HiltViewModel
class SetupWizardViewModel @Inject constructor(
    private val repo: FinanceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "finanzas_familiares_prefs"
        private const val PREF_SETUP_COMPLETED = "setup_wizard_completed"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _isCompleted = MutableStateFlow(prefs.getBoolean(PREF_SETUP_COMPLETED, false))
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _joinError = MutableStateFlow<String?>(null)
    val joinError: StateFlow<String?> = _joinError.asStateFlow()

    fun completeSetup(data: SetupWizardData) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _joinError.value = null
            try {
                if (data.familyCode.isNotBlank()) {
                    val result = repo.joinFamily(data.familyCode.trim())
                    if (result.isFailure) {
                        _joinError.value = when (result.exceptionOrNull()?.message) {
                            FinanceRepository.ERROR_INVALID_CODE -> context.getString(R.string.error_invalid_code)
                            else -> result.exceptionOrNull()?.message
                        }
                        return@launch
                    }
                    val joinedConfig = repo.getConfig()
                    repo.initSavingsIfNeeded(joinedConfig)
                } else {
                    val initialConfig = FamilyConfig(
                        incomeCurrency = data.incomeCurrency.ifBlank { IncomeCurrency.USD },
                        defaultExchangeRate = data.purchaseRate,
                        defaultCardExchangeOffset = data.saleRate - data.purchaseRate,
                        marginGreenThresholdPct = data.greenThreshold,
                        marginYellowThresholdPct = data.yellowThreshold
                    )
                    repo.saveConfig(initialConfig)
                    repo.updateExchangeSettingsFromCurrentMonth(
                        rate = initialConfig.defaultExchangeRate,
                        cardExchangeOffset = initialConfig.defaultCardExchangeOffset
                    )
                    repo.initSavingsIfNeeded(initialConfig)
                }

                prefs.edit().putBoolean(PREF_SETUP_COMPLETED, true).apply()
                _isCompleted.value = true
            } finally {
                _isSubmitting.value = false
            }
        }
    }
}
