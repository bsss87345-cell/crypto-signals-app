package com.example.data

enum class ScoreLabel(val arabic: String) {
    EXCELLENT("ممتاز"),
    VERY_GOOD("جيد جداً"),
    GOOD("جيد"),
    MEDIUM("متوسط"),
    LOW("منخفض"),
    CLOSE("قريب"),
    WAIT("انتظار")
}

data class ScoreInput(
    val side: Side,
    val price: Double,
    val ema20: Double,
    val ema50: Double,
    val rsi: Double,
    val macd: Double,
    val macdSignal: Double,
    // how many closed candles ago the last MACD cross happened in this side's direction (0 = last candle), null if none recently
    val candlesSinceCross: Int?,
    val adx: Double,
    val volumeRatio: Double,
    val trend4hAgrees: Boolean
)

object SignalScore {

    const val W_EMA = 20.0
    const val W_MACD = 20.0
    const val W_TREND4H = 20.0
    const val W_RSI = 15.0
    const val W_ADX = 15.0
    const val W_VOLUME = 10.0

    const val MIN_VISIBLE = 30
    const val MIN_SIGNAL = 50

    private fun clamp01(x: Double) = if (x < 0.0) 0.0 else if (x > 1.0) 1.0 else x

    fun score(i: ScoreInput): Int {
        val dir = if (i.side == Side.BUY) 1.0 else -1.0

        // EMA: separation in the side's direction, relative to price; full at 1%
        val sep = if (i.price > 0) (i.ema20 - i.ema50) / i.price * dir else 0.0
        val ema = clamp01(sep / 0.01)

        // MACD
        val macd = when (i.candlesSinceCross) {
            0 -> 1.0
            1 -> 0.75
            2 -> 0.5
            3 -> 0.25
            else -> if ((i.macd - i.macdSignal) * dir > 0) 0.25 else 0.0
        }

        val trend = if (i.trend4hAgrees) 1.0 else 0.0

        // RSI: mirror sell side so the same curve applies (buy: full 55..65, zero at 40 and 75)
        val r = if (i.side == Side.BUY) i.rsi else 100.0 - i.rsi
        val rsi = when {
            r in 55.0..65.0 -> 1.0
            r < 55.0 -> clamp01((r - 40.0) / 15.0)
            else -> clamp01((75.0 - r) / 10.0)
        }

        val adx = clamp01((i.adx - 15.0) / 10.0)
        val vol = clamp01((i.volumeRatio - 0.8) / 0.7)

        val total = ema * W_EMA + macd * W_MACD + trend * W_TREND4H +
            rsi * W_RSI + adx * W_ADX + vol * W_VOLUME
        return Math.round(total).toInt().coerceIn(0, 100)
    }

    // null = hidden (below 30)
    fun label(score: Int): ScoreLabel? = when {
        score >= 90 -> ScoreLabel.EXCELLENT
        score >= 80 -> ScoreLabel.VERY_GOOD
        score >= 70 -> ScoreLabel.GOOD
        score >= 60 -> ScoreLabel.MEDIUM
        score >= 50 -> ScoreLabel.LOW
        score >= 40 -> ScoreLabel.CLOSE
        score >= MIN_VISIBLE -> ScoreLabel.WAIT
        else -> null
    }

    fun isSignal(score: Int): Boolean = score >= MIN_SIGNAL
}
