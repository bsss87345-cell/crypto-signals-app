package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.AppScreen
import com.example.ui.SignalsViewModel
import com.example.ui.screens.AccessCodeScreen
import com.example.ui.screens.SignalsScreen
import com.example.ui.screens.UsernameScreen
import com.example.ui.theme.CryptoSignalsTheme
import com.example.ui.theme.DarkBg

class MainActivity : ComponentActivity() {

    private val viewModel: SignalsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CryptoSignalsTheme {
                val uiState by viewModel.uiState.collectAsState()

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBg)
                        .navigationBarsPadding(),
                    containerColor = DarkBg
                ) { _ ->

                    AnimatedContent(
                        targetState = uiState.currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.USERNAME -> {
                                UsernameScreen(
                                    username = uiState.telegramUsername,
                                    errorMessage = uiState.usernameError,
                                    onUsernameChange = viewModel::onUsernameChanged,
                                    onSubmit = viewModel::submitUsername
                                )
                            }
                            AppScreen.ACCESS_CODE -> {
                                AccessCodeScreen(
                                    accessCode = uiState.accessCode,
                                    errorMessage = uiState.accessCodeError,
                                    onCodeChange = viewModel::onAccessCodeChanged,
                                    onSubmit = viewModel::submitAccessCode,
                                    onBack = viewModel::navigateBack
                                )
                            }
                            AppScreen.SIGNALS -> {
                                BackHandler {
                                    viewModel.navigateBack()
                                }
                                SignalsScreen(
                                    signals = uiState.signals,
                                    selectedTab = uiState.selectedTab,
                                    lastUpdateTime = uiState.lastUpdateTime,
                                    isRefreshing = uiState.isRefreshing,
                                    savedCapital = uiState.savedCapital,
                                    activeInvestmentSignal = uiState.activeInvestmentSignal,
                                    onTabSelected = viewModel::selectTab,
                                    onRefresh = viewModel::refreshSignals,
                                    onOpenInvestmentDialog = viewModel::openInvestmentDialog,
                                    onCloseInvestmentDialog = viewModel::closeInvestmentDialog,
                                    onCapitalChanged = viewModel::updateCapital
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
