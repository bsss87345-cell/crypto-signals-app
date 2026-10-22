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
    private val api: ExchangeApi = ExchangeApi(),
    private val store: SignalStore? = null
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
        val (c1, c4) = coroutineScope {
        val a = async { api.getCandles(symbol, "1h", 200) }
        val b = async { api.getCandles(symbol, "4h", 200) }
        Pair(a.await(), b.await())
        }
                // Keep an active stored signal frozen until it expires or is closed
        val stored = store?.get(coin)
        if (stored != null) {
            val tracked = SignalTracker.evaluate(stored, c1, nowMillis)
            if (tracked.status == TrackStatus.ACTIVE) {
                return signalFromStored(stored, c1, nowMillis)
            }
            store?.remove(coin)
        }

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

        // Validity window by score: stronger score = shorter trade
        val (durationLabel, validHours) = when {
            r.score >= 80 -> Pair("1 - 2 ساعات", 2L)
            r.score >= 60 -> Pair("1 - 3 ساعات", 3L)
            else -> Pair("2 - 4 ساعات", 4L)
        }

        store?.put(
            StoredSignal(
                coin = coin,
                side = r.side,
                score = r.score,
                entryLow = r.entryLow,
                entryHigh = r.entryHigh,
                stopLoss = r.stopLoss,
                target1 = r.target1,
                target2 = r.target2,
                target3 = r.target3,
                durationText = durationLabel,
                startMillis = nowMillis,
                validUntilMillis = nowMillis + validHours * 3_600_000L
            )
        )

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
            durationText = durationLabel,
            candleCloseSecondsRemaining = secondsLeft,
            isBestOpportunity = false,
            entryLow = r.entryLow,
            entryHigh = r.entryHigh,
                        validUntilMillis = nowMillis + validHours * 3_600_000L
        )
    }

    // Rebuilds a card from a stored (frozen) signal. Only price, chart and 24h change are live.
    private fun signalFromStored(s: StoredSignal, c1: List<Candle>, nowMillis: Long): Signal {
        val price = c1.last().close
        val closes = c1.map { it.close }
        val ref = if (closes.size > 24) closes[closes.size - 25] else closes.first()
        val change24 = if (ref > 0) (price - ref) / ref * 100.0 else 0.0
        val secondsLeft = ((c1.last().closeTime - nowMillis) / 1000L).coerceAtLeast(0L)
        val mid = (s.entryLow + s.entryHigh) / 2.0
        val profit3 = if (mid > 0) abs(s.target3 - mid) / mid * 100.0 else 0.0

        return Signal(
            id = s.coin.lowercase() + "-1h",
            pair = "${s.coin}/USDT",
            coinSymbol = s.coin,
            status = if (SignalScore.isSignal(s.score)) SignalStatus.LIVE else SignalStatus.PENDING,
            direction = if (s.side == Side.BUY) SignalDirection.BUY else SignalDirection.SELL,
            currentPrice = price,
            change24h = Math.round(change24 * 100.0) / 100.0,
            sparklinePoints = closes.takeLast(24).map { it.toFloat() },
            quality = if (s.score >= 70) SignalQuality.HIGHER else SignalQuality.STANDARD,
            score = s.score,
            expectedProfitPercent = profit3,
            targetPrice = s.target3,
            target1 = s.target1,
            target2 = s.target2,
            target3 = s.target3,
            stopLoss = s.stopLoss,
            durationText = s.durationText,
            candleCloseSecondsRemaining = secondsLeft,
            isBestOpportunity = false,
            entryLow = s.entryLow,
            entryHigh = s.entryHigh,
            validUntilMillis = s.validUntilMillis
        )
    }
}
