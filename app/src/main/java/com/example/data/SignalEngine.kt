package com.example.data

import kotlin.math.abs

enum class Side { BUY, SELL }

data class EngineResult(
    val side: Side,
    val isLive: Boolean,
    val isHigherQuality: Boolean,
    val price: Double,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val target3: Double,
    val conditionsMet: Int,
    val score: Int = 0
)

object SignalEngine {

    // Tunable constants (adjust after backtesting)
    const val STOP_ATR_MULT = 1.5
    const val T1_ATR_MULT = 1.0
    const val T2_ATR_MULT = 2.0
    const val T3_ATR_MULT = 3.0
    const val ADX_MIN = 20.0
    const val ADX_HIGH = 25.0
    const val VOLUME_AVG_PERIOD = 20

    // Drops the running (last) candle, keeps only closed ones.
    private fun closedOnly(candles: List<Candle>): List<Candle> =
        if (candles.size > 1) candles.dropLast(1) else emptyList()

     // How many closed candles ago the last MACD cross happened in the given side's direction.
    // 0 = on the last closed candle, up to 3. null if none within the last 4 candles.
    private fun candlesSinceCross(macd: List<Double>, signal: List<Double>, side: Side): Int? {
        val last = macd.lastIndex
        for (age in 0..3) {
            val i = last - age
            if (i < 1) return null
            val up = macd[i - 1] <= signal[i - 1] && macd[i] > signal[i]
            val down = macd[i - 1] >= signal[i - 1] && macd[i] < signal[i]
            if (side == Side.BUY && up) return age
            if (side == Side.SELL && down) return age
        }
        return null
    }

    // Direction of the 4h trend: EMA20 vs EMA50 on closed 4h candles. null if not enough data.
    fun trend4h(candles4h: List<Candle>): Side? {
        val closes = closedOnly(candles4h).map { it.close }
        if (closes.size < 50) return null
        val e20 = Indicators.ema(closes, 20).last()
        val e50 = Indicators.ema(closes, 50).last()
        if (e20.isNaN() || e50.isNaN()) return null
        return if (e20 > e50) Side.BUY else Side.SELL
    }

    // candles1h / candles4h: oldest -> newest, last candle is the running one.
    // Returns null when there is not enough data.
    fun analyze(candles1h: List<Candle>, candles4h: List<Candle>): EngineResult? {
        val closed = closedOnly(candles1h)
        if (closed.size < 60) return null

        val closes = closed.map { it.close }
        val last = closes.lastIndex
        val prev = last - 1

        val ema20 = Indicators.ema(closes, 20)
        val ema50 = Indicators.ema(closes, 50)
        val rsi = Indicators.rsi(closes, 14)
        val macd = Indicators.macd(closes)
        val atr = Indicators.atr(closed, 14)
        val adx = Indicators.adx(closed, 14)

        val values = listOf(
            ema20[last], ema50[last], rsi[last],
            macd.macd[last], macd.signal[last], macd.macd[prev], macd.signal[prev],
            atr[last], adx[last]
        )
        if (values.any { it.isNaN() }) return null

        val crossUp = macd.macd[prev] <= macd.signal[prev] && macd.macd[last] > macd.signal[last]
        val crossDown = macd.macd[prev] >= macd.signal[prev] && macd.macd[last] < macd.signal[last]

        val buyBasics = listOf(
            ema20[last] > ema50[last],
            rsi[last] in 50.0..70.0,
            crossUp
        )
        val sellBasics = listOf(
            ema20[last] < ema50[last],
            rsi[last] in 30.0..50.0,
            crossDown
        )

        val buyCount = buyBasics.count { it }
        val sellCount = sellBasics.count { it }
        // Filters
        val trend = trend4h(candles4h)
        val adxOk = adx[last] > ADX_MIN
        val volumes = closed.map { it.volume }
        val avgVolume = volumes.takeLast(VOLUME_AVG_PERIOD).average()
        val volumeOk = volumes.last() > avgVolume

        val price = candles1h.last().close // latest price (running candle)

        val volumeRatio = if (avgVolume > 0.0) volumes.last() / avgVolume else 0.0
        fun scoreFor(s: Side) = SignalScore.score(
            ScoreInput(
                side = s,
                price = price,
                ema20 = ema20[last],
                ema50 = ema50[last],
                rsi = rsi[last],
                macd = macd.macd[last],
                macdSignal = macd.signal[last],
                candlesSinceCross = candlesSinceCross(macd.macd, macd.signal, s),
                adx = adx[last],
                volumeRatio = volumeRatio,
                trend4hAgrees = trend == s
            )
        )
        val buyScore = scoreFor(Side.BUY)
        val sellScore = scoreFor(Side.SELL)

        val side = if (buyScore >= sellScore) Side.BUY else Side.SELL
        val basicsCount = if (side == Side.BUY) buyCount else sellCount
        val trendOk = trend == side
        val isLive = basicsCount == 3 && trendOk && adxOk && volumeOk

        val rsiNow = rsi[last]
        val rsiComfortable = if (side == Side.BUY) rsiNow in 55.0..65.0 else rsiNow in 35.0..45.0
        val higher = isLive && rsiComfortable && adx[last] > ADX_HIGH

        val a = atr[last]
        val dir = if (side == Side.BUY) 1.0 else -1.0

        return EngineResult(
            side = side,
            isLive = isLive,
            isHigherQuality = higher,
            price = price,
            stopLoss = price - dir * STOP_ATR_MULT * a,
            target1 = price + dir * T1_ATR_MULT * a,
            target2 = price + dir * T2_ATR_MULT * a,
            target3 = price + dir * T3_ATR_MULT * a,
            conditionsMet = basicsCount +
            listOf(trendOk, adxOk, volumeOk).count { it },
            score = if (side == Side.BUY) buyScore else sellScore
        )
    }

    // Helper for tests/UI: distance of a level from price in percent.
    fun percentFrom(price: Double, level: Double): Double =
        if (price <= 0.0) 0.0 else abs(level - price) / price * 100.0
}
