package com.example.data

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalEngineScoreTest {

    private fun candle(i: Int, close: Double, volume: Double = 100.0) =
        Candle(
            openTime = i * 3_600_000L,
            open = close,
            high = close + 1.0,
            low = close - 1.0,
            close = close,
            volume = volume,
            closeTime = i * 3_600_000L + 3_599_999L
        )

    private fun series(n: Int, start: Double, step: Double) =
        (0 until n).map { candle(it, start + it * step) }

    @Test
    fun score_isBetweenZeroAndHundredInUptrend() {
        val r = SignalEngine.analyze(series(150, 100.0, 1.0), series(80, 100.0, 1.0))
        assertNotNull(r)
        assertTrue(r!!.score in 0..100)
    }

    @Test
    fun score_isBetweenZeroAndHundredInDowntrend() {
        val r = SignalEngine.analyze(series(150, 300.0, -1.0), series(80, 300.0, -1.0))
        assertNotNull(r)
        assertTrue(r!!.score in 0..100)
    }

    @Test
    fun score_isHigherWhenFourHourTrendAgrees() {
        val c1 = series(150, 100.0, 1.0)
        val agree = SignalEngine.analyze(c1, series(80, 100.0, 1.0))!!
        val disagree = SignalEngine.analyze(c1, series(80, 300.0, -1.0))!!
        assertTrue(agree.score > disagree.score)
    }
}
