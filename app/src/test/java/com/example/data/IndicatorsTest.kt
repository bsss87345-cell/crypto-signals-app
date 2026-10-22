package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IndicatorsTest {

    private fun candle(i: Int, high: Double, low: Double, close: Double) =
        Candle(
            openTime = i * 3_600_000L,
            open = close,
            high = high,
            low = low,
            close = close,
            volume = 100.0,
            closeTime = i * 3_600_000L + 3_599_999L
        )

    @Test
    fun ema_firstValueIsSmaAndEarlierAreNaN() {
        val out = Indicators.ema(listOf(1.0, 2.0, 3.0, 4.0, 5.0), 3)
        assertTrue(out[0].isNaN())
        assertTrue(out[1].isNaN())
        assertEquals(2.0, out[2], 1e-9)
        // k = 0.5 -> 4*0.5 + 2*0.5 = 3.0, then 5*0.5 + 3*0.5 = 4.0
        assertEquals(3.0, out[3], 1e-9)
        assertEquals(4.0, out[4], 1e-9)
    }

    @Test
    fun ema_returnsAllNaNWhenNotEnoughData() {
        val out = Indicators.ema(listOf(1.0, 2.0), 5)
        assertEquals(2, out.size)
        assertTrue(out.all { it.isNaN() })
    }

    @Test
    fun rsi_isHundredWhenOnlyGains() {
        val closes = (1..30).map { it.toDouble() }
        val out = Indicators.rsi(closes, 14)
        assertTrue(out[13].isNaN())
        assertEquals(100.0, out[14], 1e-9)
        assertEquals(100.0, out.last(), 1e-9)
    }

    @Test
    fun rsi_isZeroWhenOnlyLosses() {
        val closes = (30 downTo 1).map { it.toDouble() }
        val out = Indicators.rsi(closes, 14)
        assertEquals(0.0, out.last(), 1e-9)
    }

    @Test
    fun rsi_staysBetweenZeroAndHundred() {
        val closes = (0 until 100).map { 100.0 + Math.sin(it / 3.0) * 10 }
        val valid = Indicators.rsi(closes, 14).filter { !it.isNaN() }
        assertTrue(valid.isNotEmpty())
        assertTrue(valid.all { it in 0.0..100.0 })
    }

    @Test
    fun macd_sizesMatchAndFlatSeriesGivesZero() {
        val closes = List(80) { 50.0 }
        val r = Indicators.macd(closes)
        assertEquals(80, r.macd.size)
        assertEquals(80, r.signal.size)
        assertEquals(80, r.histogram.size)
        assertEquals(0.0, r.macd.last(), 1e-9)
        assertEquals(0.0, r.signal.last(), 1e-9)
        assertEquals(0.0, r.histogram.last(), 1e-9)
    }

    @Test
    fun macd_isPositiveInUptrend() {
        val closes = (1..120).map { it.toDouble() }
        val r = Indicators.macd(closes)
        assertTrue(r.macd.last() > 0.0)
    }

    @Test
    fun atr_equalsConstantRangeWhenRangeIsConstant() {
        // every candle: high-low = 2, no gaps -> true range = 2
        val candles = (0 until 40).map { candle(it, 101.0, 99.0, 100.0) }
        val out = Indicators.atr(candles, 14)
        assertTrue(out[13].isNaN())
        assertEquals(2.0, out[14], 1e-9)
        assertEquals(2.0, out.last(), 1e-9)
    }

    @Test
    fun adx_isHighInStrongTrend() {
        val candles = (0 until 80).map {
            val base = 100.0 + it * 2.0
            candle(it, base + 1.0, base - 1.0, base)
        }
        val out = Indicators.adx(candles, 14)
        assertEquals(80, out.size)
        assertTrue(out.last() > 25.0)
    }

    @Test
    fun adx_returnsNaNWhenNotEnoughData() {
        val candles = (0 until 20).map { candle(it, 101.0, 99.0, 100.0) }
        val out = Indicators.adx(candles, 14)
        assertTrue(out.all { it.isNaN() })
    }
}
