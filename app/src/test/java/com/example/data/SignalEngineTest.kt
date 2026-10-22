package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalEngineTest {

    private fun candle(i: Int, close: Double, range: Double = 1.0, volume: Double = 100.0) =
        Candle(
            openTime = i * 3_600_000L,
            open = close,
            high = close + range,
            low = close - range,
            close = close,
            volume = volume,
            closeTime = i * 3_600_000L + 3_599_999L
        )

    private fun series(n: Int, start: Double, step: Double, volume: Double = 100.0) =
        (0 until n).map { candle(it, start + it * step, 1.0, volume) }

    @Test
    fun analyze_returnsNullWhenNotEnoughData() {
        val c1 = series(30, 100.0, 1.0)
        val c4 = series(80, 100.0, 1.0)
        assertNull(SignalEngine.analyze(c1, c4))
    }

    @Test
    fun trend4h_isBuyInUptrend() {
        val c4 = series(80, 100.0, 1.0)
        assertEquals(Side.BUY, SignalEngine.trend4h(c4))
    }

    @Test
    fun trend4h_isSellInDowntrend() {
        val c4 = series(80, 200.0, -1.0)
        assertEquals(Side.SELL, SignalEngine.trend4h(c4))
    }

    @Test
    fun trend4h_isNullWhenNotEnoughData() {
        assertNull(SignalEngine.trend4h(series(30, 100.0, 1.0)))
    }

    @Test
    fun analyze_isNotLiveWithoutMacdCross() {
        // Steady uptrend: no fresh MACD cross on the last closed candle
        val c1 = series(150, 100.0, 1.0)
        val c4 = series(80, 100.0, 1.0)
        val r = SignalEngine.analyze(c1, c4)
        assertNotNull(r)
        assertFalse(r!!.isLive)
    }

    @Test
    fun analyze_stopAndTargetsAreOrderedForBuy() {
        val c1 = series(150, 100.0, 1.0)
        val c4 = series(80, 100.0, 1.0)
        val r = SignalEngine.analyze(c1, c4)!!
        assertEquals(Side.BUY, r.side)
        assertTrue(r.stopLoss < r.price)
        assertTrue(r.price < r.target1)
        assertTrue(r.target1 < r.target2)
        assertTrue(r.target2 < r.target3)
    }

    @Test
    fun analyze_stopAndTargetsAreOrderedForSell() {
        val c1 = series(150, 300.0, -1.0)
        val c4 = series(80, 300.0, -1.0)
        val r = SignalEngine.analyze(c1, c4)!!
        assertEquals(Side.SELL, r.side)
        assertTrue(r.stopLoss > r.price)
        assertTrue(r.price > r.target1)
        assertTrue(r.target1 > r.target2)
        assertTrue(r.target2 > r.target3)
    }

    @Test
    fun analyze_targetDistancesFollowAtrMultipliers() {
        val c1 = series(150, 100.0, 1.0)
        val c4 = series(80, 100.0, 1.0)
        val r = SignalEngine.analyze(c1, c4)!!
        val d1 = r.target1 - r.price
        val d2 = r.target2 - r.price
        val d3 = r.target3 - r.price
        val ds = r.price - r.stopLoss
        assertEquals(2.0, d2 / d1, 1e-9)
        assertEquals(3.0, d3 / d1, 1e-9)
        assertEquals(1.5, ds / d1, 1e-9)
    }

    @Test
    fun analyze_ignoresTheRunningCandle() {
        val base = series(150, 100.0, 1.0)
        val c4 = series(80, 100.0, 1.0)
        // Changing only the running candle's volume/high must not change the ATR-based levels distance
        val spiked = base.dropLast(1) + base.last().copy(high = base.last().high + 500.0)
        val a = SignalEngine.analyze(base, c4)!!
        val b = SignalEngine.analyze(spiked, c4)!!
        assertEquals(a.target1 - a.price, b.target1 - b.price, 1e-9)
    }

    @Test
    fun percentFrom_worksForBothDirections() {
        assertEquals(10.0, SignalEngine.percentFrom(100.0, 110.0), 1e-9)
        assertEquals(10.0, SignalEngine.percentFrom(100.0, 90.0), 1e-9)
        assertEquals(0.0, SignalEngine.percentFrom(0.0, 90.0), 1e-9)
    }
}
