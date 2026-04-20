package com.finanzasfamiliares

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.finanzasfamiliares.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.finanzasfamiliares.ui.screens.setup.SetupWizardScreen
import com.finanzasfamiliares.ui.screens.setup.SetupWizardViewModel
import com.finanzasfamiliares.ui.startup.StartupViewModel
import com.finanzasfamiliares.ui.theme.FinanzasTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinanzasTheme {
                val startupViewModel: StartupViewModel = hiltViewModel()
                val setupViewModel: SetupWizardViewModel = hiltViewModel()
                val isReady by startupViewModel.isReady.collectAsState()
                val setupCompleted by setupViewModel.isCompleted.collectAsState()

                when {
                    !isReady -> StartupLoadingScreen()
                    setupCompleted -> FinanzasApp()
                    else -> SetupWizardScreen()
                }
            }
        }
    }
}

@Composable
private fun StartupLoadingScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.startup_loading),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
