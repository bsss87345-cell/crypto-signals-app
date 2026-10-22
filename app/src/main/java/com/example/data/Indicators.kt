package com.example.data

import kotlin.math.abs
import kotlin.math.max

object Indicators {

    // EMA: first value = SMA of first `period` values. Earlier entries are NaN.
    fun ema(values: List<Double>, period: Int): List<Double> {
        val out = MutableList(values.size) { Double.NaN }
        if (period <= 0 || values.size < period) return out
        val k = 2.0 / (period + 1)
        var prev = values.take(period).average()
        out[period - 1] = prev
        for (i in period until values.size) {
            prev = values[i] * k + prev * (1 - k)
            out[i] = prev
        }
        return out
    }

    // RSI (Wilder smoothing). Earlier entries are NaN.
    fun rsi(closes: List<Double>, period: Int = 14): List<Double> {
        val out = MutableList(closes.size) { Double.NaN }
        if (closes.size <= period) return out
        var gain = 0.0
        var loss = 0.0
        for (i in 1..period) {
            val d = closes[i] - closes[i - 1]
            if (d >= 0) gain += d else loss -= d
        }
        var avgGain = gain / period
        var avgLoss = loss / period
        out[period] = rsiValue(avgGain, avgLoss)
        for (i in period + 1 until closes.size) {
            val d = closes[i] - closes[i - 1]
            val g = if (d > 0) d else 0.0
            val l = if (d < 0) -d else 0.0
            avgGain = (avgGain * (period - 1) + g) / period
            avgLoss = (avgLoss * (period - 1) + l) / period
            out[i] = rsiValue(avgGain, avgLoss)
        }
        return out
    }

    private fun rsiValue(avgGain: Double, avgLoss: Double): Double {
        if (avgLoss == 0.0) return if (avgGain == 0.0) 50.0 else 100.0
        val rs = avgGain / avgLoss
        return 100.0 - 100.0 / (1 + rs)
    }

    data class MacdResult(
        val macd: List<Double>,
        val signal: List<Double>,
        val histogram: List<Double>
    )

    // MACD 12/26/9
    fun macd(closes: List<Double>, fast: Int = 12, slow: Int = 26, signalPeriod: Int = 9): MacdResult {
        val n = closes.size
        val fastEma = ema(closes, fast)
        val slowEma = ema(closes, slow)
        val macdLine = MutableList(n) { i ->
            if (fastEma[i].isNaN() || slowEma[i].isNaN()) Double.NaN else fastEma[i] - slowEma[i]
        }
        val signal = MutableList(n) { Double.NaN }
        val start = macdLine.indexOfFirst { !it.isNaN() }
        if (start >= 0) {
            val valid = macdLine.subList(start, n)
            val sig = ema(valid, signalPeriod)
            for (i in sig.indices) signal[start + i] = sig[i]
        }
        val hist = MutableList(n) { i ->
            if (macdLine[i].isNaN() || signal[i].isNaN()) Double.NaN else macdLine[i] - signal[i]
        }
        return MacdResult(macdLine, signal, hist)
    }

    // True range list (first entry = high - low)
    private fun trueRange(candles: List<Candle>): List<Double> =
        candles.mapIndexed { i, c ->
            if (i == 0) c.high - c.low
            else {
                val pc = candles[i - 1].close
                max(c.high - c.low, max(abs(c.high - pc), abs(c.low - pc)))
            }
        }

    // ATR (Wilder). Earlier entries are NaN.
    fun atr(candles: List<Candle>, period: Int = 14): List<Double> {
        val out = MutableList(candles.size) { Double.NaN }
        if (candles.size <= period) return out
        val tr = trueRange(candles)
        var prev = tr.subList(1, period + 1).average()
        out[period] = prev
        for (i in period + 1 until candles.size) {
            prev = (prev * (period - 1) + tr[i]) / period
            out[i] = prev
        }
        return out
    }

    // ADX (Wilder). Earlier entries are NaN.
    fun adx(candles: List<Candle>, period: Int = 14): List<Double> {
        val n = candles.size
        val out = MutableList(n) { Double.NaN }
        if (n <= period * 2) return out
        val tr = trueRange(candles)
        val plusDm = MutableList(n) { 0.0 }
        val minusDm = MutableList(n) { 0.0 }
        for (i in 1 until n) {
            val up = candles[i].high - candles[i - 1].high
            val down = candles[i - 1].low - candles[i].low
            plusDm[i] = if (up > down && up > 0) up else 0.0
            minusDm[i] = if (down > up && down > 0) down else 0.0
        }
        var smTr = tr.subList(1, period + 1).sum()
        var smPlus = plusDm.subList(1, period + 1).sum()
        var smMinus = minusDm.subList(1, period + 1).sum()
        val dx = MutableList(n) { Double.NaN }
        dx[period] = dxValue(smPlus, smMinus, smTr)
        for (i in period + 1 until n) {
            smTr = smTr - smTr / period + tr[i]
            smPlus = smPlus - smPlus / period + plusDm[i]
            smMinus = smMinus - smMinus / period + minusDm[i]
            dx[i] = dxValue(smPlus, smMinus, smTr)
        }
        var adxPrev = dx.subList(period, period * 2).average()
        out[period * 2 - 1] = adxPrev
        for (i in period * 2 until n) {
            adxPrev = (adxPrev * (period - 1) + dx[i]) / period
            out[i] = adxPrev
        }
        return out
    }

    private fun dxValue(smPlus: Double, smMinus: Double, smTr: Double): Double {
        if (smTr == 0.0) return 0.0
        val plusDi = 100.0 * smPlus / smTr
        val minusDi = 100.0 * smMinus / smTr
        val sum = plusDi + minusDi
        return if (sum == 0.0) 0.0 else 100.0 * abs(plusDi - minusDi) / sum
    }
}
