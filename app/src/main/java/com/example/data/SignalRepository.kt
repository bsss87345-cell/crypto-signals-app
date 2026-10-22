package com.example.data

import com.example.model.Signal
import com.example.model.SignalDirection
import com.example.model.SignalQuality
import com.example.model.SignalStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.abs

class SignalRepository(
    private val api: ExchangeApi = ExchangeApi()
) {
    companion object {
        val COINS = listOf(
            "BTC", "ETH", "BNB", "SOL", "XRP", "ADA", "DOGE", "AVAX", "LINK", "DOT",
            "TRX", "POL", "LTC", "BCH", "ATOM", "NEAR", "UNI", "APT", "ARB", "OP",
            "SUI", "INJ", "FIL", "ETC", "XLM", "HBAR", "AAVE", "ICP", "ALGO", "VET"
        )
        private const val MAX_PARALLEL = 10
    }

    // Fetches all coins, runs the engine, returns signals. Failed coins are skipped.
    suspend fun loadSignals(nowMillis: Long = System.currentTimeMillis()): List<Signal> =
        withContext(Dispatchers.IO) {
            val gate = Semaphore(MAX_PARALLEL)
            coroutineScope {
                COINS.map { coin ->
                    async {
                        gate.withPermit {
                            try {
                                buildSignal(coin, nowMillis)
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                }.awaitAll().filterNotNull()
            }
        }

   private suspend fun buildSignal(coin: String, nowMillis: Long): Signal? { 
        val symbol = coin + "USDT"
        val c1 = api.getCandles(symbol, "1h", 200)
        val c4 = api.getCandles(symbol, "4h", 200)
        val r = SignalEngine.analyze(c1, c4) ?: return null
        if (r.score < SignalScore.MIN_VISIBLE) return null
        val price = r.price
        val closes = c1.map { it.close }
        // 24h change: price now vs close 24 candles ago (1h candles)
        val ref = if (closes.size > 24) closes[closes.size - 25] else closes.first()
        val change24 = if (ref > 0) (price - ref) / ref * 100.0 else 0.0
        val spark = closes.takeLast(24).map { it.toFloat() }

        val runningClose = c1.last().closeTime
        val secondsLeft = ((runningClose - nowMillis) / 1000L).coerceAtLeast(0L)

        val profit3 = abs(r.target3 - price) / price * 100.0

        return Signal(
            id = coin.lowercase() + "-1h",
            pair = "$coin/USDT",
            coinSymbol = coin,
            status = if (SignalScore.isSignal(r.score)) SignalStatus.LIVE else SignalStatus.PENDING,
            direction = if (r.side == Side.BUY) SignalDirection.BUY else SignalDirection.SELL,
            currentPrice = price,
            change24h = Math.round(change24 * 100.0) / 100.0,
            sparklinePoints = spark,
            quality = if (r.score >= 70) SignalQuality.HIGHER else SignalQuality.STANDARD,
            score = r.score,
            expectedProfitPercent = profit3,
            targetPrice = r.target3,
            target1 = r.target1,
            target2 = r.target2,
            target3 = r.target3,
            stopLoss = r.stopLoss,
            durationText = "1 - 3 ساعات",
            candleCloseSecondsRemaining = secondsLeft,
            isBestOpportunity = false
        )
    }
}
