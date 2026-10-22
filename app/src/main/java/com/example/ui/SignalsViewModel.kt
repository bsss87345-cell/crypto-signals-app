package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Signal
import com.example.model.SignalDirection
import com.example.model.SignalQuality
import com.example.model.SignalStatus
import com.example.data.PriceStream
import com.example.data.SignalRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    USERNAME,
    ACCESS_CODE,
    SIGNALS
}

enum class SignalTab(val labelArabic: String) {
    ALL("الكل"),
    LIVE("مباشر"),
    PENDING("انتظار")
}

data class UiState(
    val currentScreen: AppScreen = AppScreen.USERNAME,
    val telegramUsername: String = "",
    val accessCode: String = "",
    val usernameError: String? = null,
    val accessCodeError: String? = null,
    val selectedTab: SignalTab = SignalTab.ALL,
    val signals: List<Signal> = emptyList(),
    val savedCapital: Double = 10000.0,
    val activeInvestmentSignal: Signal? = null,
    val isRefreshing: Boolean = false,
    val lastUpdateTime: String = ""
)

class SignalsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val repository = SignalRepository()
    private val priceStream = PriceStream()

    // Live prices: updates only currentPrice of each card
    private fun startLivePrices() {
        priceStream.start(SignalRepository.COINS.map { it + "USDT" }.toSet())
        viewModelScope.launch {
            priceStream.prices.collect { prices ->
                if (prices.isEmpty()) return@collect
                _uiState.update { state ->
                    state.copy(
                        signals = state.signals.map { sig ->
                            val p = prices[sig.coinSymbol + "USDT"]
                            if (p != null) sig.copy(currentPrice = p) else sig
                        }
                    )
                }
            }
        }
    }

    override fun onCleared() {
        priceStream.stop()
        super.onCleared()
    }

    init {
        viewModelScope.launch { loadRealSignals() }
        startCountdownTicker()
    }
    private fun sortSignals(list: List<Signal>): List<Signal> =
        list.sortedWith(
            compareBy<Signal> { it.status != SignalStatus.LIVE }
                .thenBy { it.quality != SignalQuality.HIGHER }
        )

    private suspend fun loadRealSignals() {
        _uiState.update { it.copy(isRefreshing = true) }
        val result = try {
            sortSignals(repository.loadSignals())
        } catch (e: Exception) {
            emptyList()
        }
        _uiState.update {
            it.copy(
                signals = result,
                isRefreshing = false,
                lastUpdateTime = getCurrentFormattedTime()
            )
        }
    }

    private fun getCurrentFormattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getInitialSignals(): List<Signal> {
        val rawSignals = listOf(
            Signal(
                id = "eth-1",
                pair = "ETH/USDT",
                coinSymbol = "ETH",
                status = SignalStatus.LIVE,
                direction = SignalDirection.BUY,
                currentPrice = 3420.50,
                change24h = 4.82,
                sparklinePoints = listOf(3310f, 3325f, 3300f, 3340f, 3370f, 3360f, 3395f, 3410f, 3420.5f),
                quality = SignalQuality.HIGHER,
                expectedProfitPercent = 4.66,
                targetPrice = 3580.00,
                target1 = 3460.00,
                target2 = 3520.00,
                target3 = 3580.00,
                stopLoss = 3340.00,
                durationText = "1 - 3 ساعات",
                candleCloseSecondsRemaining = 272,
                isBestOpportunity = true
            ),
            Signal(
                id = "btc-2",
                pair = "BTC/USDT",
                coinSymbol = "BTC",
                status = SignalStatus.LIVE,
                direction = SignalDirection.BUY,
                currentPrice = 68950.00,
                change24h = 2.38,
                sparklinePoints = listOf(67500f, 67800f, 67600f, 68100f, 68350f, 68200f, 68700f, 68950f),
                quality = SignalQuality.STANDARD,
                expectedProfitPercent = 3.26,
                targetPrice = 71200.00,
                stopLoss = 67800.00,
                durationText = "2 - 5 ساعات",
                candleCloseSecondsRemaining = 675,
                isBestOpportunity = false
            ),
            Signal(
                id = "sol-3",
                pair = "SOL/USDT",
                coinSymbol = "SOL",
                status = SignalStatus.LIVE,
                direction = SignalDirection.SELL,
                currentPrice = 182.40,
                change24h = -3.12,
                sparklinePoints = listOf(191f, 189f, 188.5f, 186f, 187f, 184.2f, 183f, 182.4f),
                quality = SignalQuality.HIGHER,
                expectedProfitPercent = 5.98,
                targetPrice = 171.50,
                target1 = 178.00,
                target2 = 174.50,
                target3 = 171.50,
                stopLoss = 187.20,
                durationText = "1 - 4 ساعات",
                candleCloseSecondsRemaining = 525,
                isBestOpportunity = false
            ),
            Signal(
                id = "avax-4",
                pair = "AVAX/USDT",
                coinSymbol = "AVAX",
                status = SignalStatus.PENDING,
                direction = SignalDirection.BUY,
                currentPrice = 29.80,
                change24h = 1.45,
                sparklinePoints = listOf(28.8f, 29.1f, 29.0f, 29.3f, 29.2f, 29.5f, 29.7f, 29.8f),
                quality = SignalQuality.HIGHER,
                expectedProfitPercent = 9.06,
                targetPrice = 32.50,
                stopLoss = 28.50,
                durationText = "3 - 6 ساعات",
                candleCloseSecondsRemaining = 860,
                isBestOpportunity = false
            ),
            Signal(
                id = "link-5",
                pair = "LINK/USDT",
                coinSymbol = "LINK",
                status = SignalStatus.PENDING,
                direction = SignalDirection.SELL,
                currentPrice = 15.20,
                change24h = -1.82,
                sparklinePoints = listOf(15.9f, 15.7f, 15.8f, 15.5f, 15.4f, 15.45f, 15.3f, 15.2f),
                quality = SignalQuality.STANDARD,
                expectedProfitPercent = 7.24,
                targetPrice = 14.10,
                stopLoss = 15.75,
                durationText = "2 - 4 ساعات",
                candleCloseSecondsRemaining = 1085,
                isBestOpportunity = false
            ),
            Signal(
                id = "bnb-6",
                pair = "BNB/USDT",
                coinSymbol = "BNB",
                status = SignalStatus.PENDING,
                direction = SignalDirection.BUY,
                currentPrice = 595.00,
                change24h = 0.92,
                sparklinePoints = listOf(588f, 590f, 589f, 592f, 591f, 593f, 594f, 595f),
                quality = SignalQuality.STANDARD,
                expectedProfitPercent = 4.20,
                targetPrice = 620.00,
                stopLoss = 582.00,
                durationText = "4 - 8 ساعات",
                candleCloseSecondsRemaining = 1360,
                isBestOpportunity = false
            )
        )

        // Sort requirement: live first, then higher quality
        return rawSignals.sortedWith(
            compareBy<Signal> { it.status != SignalStatus.LIVE }
                .thenBy { it.quality != SignalQuality.HIGHER }
                .thenByDescending { it.isBestOpportunity }
        )
    }

    // Ticks countdown timers every second
    private fun startCountdownTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                var shouldReload = false
                _uiState.update { state ->
                    val updatedSignals = state.signals.map { sig ->
                        if (sig.candleCloseSecondsRemaining > 0) {
                            sig.copy(candleCloseSecondsRemaining = sig.candleCloseSecondsRemaining - 1)
                        } else {
                            shouldReload = true
                            sig
                        }
                    }
                    state.copy(signals = updatedSignals)
                }
                if (shouldReload && !_uiState.value.isRefreshing) {
                    delay(3000) // give the exchange a moment to publish the closed candle
                    loadRealSignals()
                }
            }
        }
    }

    fun onUsernameChanged(username: String) {
        _uiState.update { it.copy(telegramUsername = username, usernameError = null) }
    }

    fun submitUsername() {
        val username = _uiState.value.telegramUsername.trim()
        if (username.isEmpty()) {
            _uiState.update { it.copy(usernameError = "يرجى إدخال اسم المستخدم") }
            return
        }
        _uiState.update { it.copy(currentScreen = AppScreen.ACCESS_CODE, usernameError = null) }
    }

    fun onAccessCodeChanged(code: String) {
        // Limit to 8 characters uppercase/alphanumeric
        val sanitized = code.filter { it.isLetterOrDigit() }.take(8).uppercase()
        _uiState.update { it.copy(accessCode = sanitized, accessCodeError = null) }
    }

    fun submitAccessCode() {
        val code = _uiState.value.accessCode.trim()
        if (code.length != 8) {
            _uiState.update { it.copy(accessCodeError = "يجب أن يتكون كود الوصول من 8 خانات") }
            return
        }
        _uiState.update { it.copy(currentScreen = AppScreen.SIGNALS, accessCodeError = null) }
    }

    fun navigateBack() {
        _uiState.update {
            when (it.currentScreen) {
                AppScreen.ACCESS_CODE -> it.copy(currentScreen = AppScreen.USERNAME)
                AppScreen.SIGNALS -> it.copy(currentScreen = AppScreen.ACCESS_CODE)
                AppScreen.USERNAME -> it
            }
        }
    }

    fun selectTab(tab: SignalTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun refreshSignals() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch { loadRealSignals() }
    }

    fun openInvestmentDialog(signal: Signal) {
        _uiState.update { it.copy(activeInvestmentSignal = signal) }
    }

    fun closeInvestmentDialog() {
        _uiState.update { it.copy(activeInvestmentSignal = null) }
    }

    fun updateCapital(capital: Double) {
        _uiState.update { it.copy(savedCapital = capital) }
    }
}
