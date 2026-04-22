package com.finanzasfamiliares

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanzasfamiliares.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.finanzasfamiliares.ui.screens.setup.SetupWizardScreen
import com.finanzasfamiliares.ui.screens.setup.SetupWizardViewModel
import com.finanzasfamiliares.ui.startup.StartupViewModel
import com.finanzasfamiliares.ui.theme.AppearanceViewModel
import com.finanzasfamiliares.ui.theme.FinanzasTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appearanceViewModel: AppearanceViewModel = hiltViewModel()
            val themeMode by appearanceViewModel.themeMode.collectAsState()
            val accentColor by appearanceViewModel.accentColor.collectAsState()

            FinanzasTheme(
                themeMode = themeMode,
                accentColor = accentColor
            ) {
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
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(136.dp)
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = stringResource(R.string.startup_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
            )
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
        }
    }
}
