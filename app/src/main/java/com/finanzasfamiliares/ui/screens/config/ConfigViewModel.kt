package com.finanzasfamiliares.ui.screens.config

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzasfamiliares.R
import com.finanzasfamiliares.data.model.FamilyConfig
import com.finanzasfamiliares.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val repo: FinanceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val config: StateFlow<FamilyConfig> = repo.observeConfig()
        .stateIn(viewModelScope, SharingStarted.Eagerly, FamilyConfig())

    private val _joinCode = MutableStateFlow<String?>(null)
    val joinCode: StateFlow<String?> = _joinCode.asStateFlow()

    private val _joinError = MutableStateFlow<String?>(null)
    val joinError: StateFlow<String?> = _joinError.asStateFlow()

    init {
        viewModelScope.launch {
            _joinCode.value = repo.getCurrentFamily()?.joinCode
        }
    }

    suspend fun saveConfig(updated: FamilyConfig): Result<Unit> {
        return try {
            val previous = config.value
            val shouldPropagateExchangeSettings =
                previous.defaultExchangeRate != updated.defaultExchangeRate ||
                    previous.defaultCardExchangeOffset != updated.defaultCardExchangeOffset

            repo.saveConfig(updated)
            if (shouldPropagateExchangeSettings) {
                viewModelScope.launch {
                    runCatching {
                        repo.updateExchangeSettingsFromCurrentMonth(
                            rate = updated.defaultExchangeRate,
                            cardExchangeOffset = updated.defaultCardExchangeOffset
                        )
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun joinWithCode(code: String) = viewModelScope.launch {
        val result = repo.joinFamily(code)
        if (result.isFailure) {
            _joinError.value = when (result.exceptionOrNull()?.message) {
                FinanceRepository.ERROR_INVALID_CODE -> context.getString(R.string.error_invalid_code)
                else -> result.exceptionOrNull()?.message
            }
        } else {
            val joinedConfig = repo.getConfig()
            repo.initSavingsIfNeeded(joinedConfig)
            _joinError.value = null
            _joinCode.value = repo.getCurrentFamily()?.joinCode
        }
    }

    fun clearJoinError() { _joinError.value = null }
}
