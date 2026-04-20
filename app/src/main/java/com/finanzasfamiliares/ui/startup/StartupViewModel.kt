package com.finanzasfamiliares.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzasfamiliares.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val repo: FinanceRepository
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            repo.signInAnonymouslyIfNeeded()
            repo.ensureFamily()
            _isReady.value = true
            launch {
                val config = repo.getConfig()
                repo.ensureJoinCodeLookupForCurrentFamily()
                repo.initSavingsIfNeeded(config)
            }
        }
    }
}
