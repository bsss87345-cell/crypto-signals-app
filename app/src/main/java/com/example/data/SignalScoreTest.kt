package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalScoreTest {

    private fun best(side: Side) = ScoreInput(
        side = side,
        price = 100.0,
        ema20 = if (side == Side.BUY) 102.0 else 98.0,
        ema50 = 100.0,
        rsi = if (side == Side.BUY) 60.0 else 40.0,
        macd = if (side == Side.BUY) 1.0 else -1.0,
        macdSignal = 0.0,
        candlesSinceCross = 0,
        adx = 30.0,
        volumeRatio = 2.0,
        trend4hAgrees = true
    )

    @Test
    fun perfectBuyScoresHundred() {
        assertEquals(100, SignalScore.score(best(Side.BUY)))
    }

    @Test
    fun perfectSellScoresHundred() {
        assertEquals(100, SignalScore.score(best(Side.SELL)))
    }

    @Test
    fun worstCaseScoresZero() {
        val s = ScoreInput(
            side = Side.BUY, price = 100.0, ema20 = 98.0, ema50 = 100.0,
            rsi = 20.0, macd = -1.0, macdSignal = 0.0, candlesSinceCross = null,
            adx = 10.0, volumeRatio = 0.5, trend4hAgrees = false
        )
        assertEquals(0, SignalScore.score(s))
    }

    @Test
    fun trendDisagreementRemovesTwentyPoints() {
        val a = SignalScore.score(best(Side.BUY))
        val b = SignalScore.score(best(Side.BUY).copy(trend4hAgrees = false))
        assertEquals(20, a - b)
    }

    @Test
    fun macdScoreDecaysWithAge() {
        val s0 = SignalScore.score(best(Side.BUY).copy(candlesSinceCross = 0))
        val s1 = SignalScore.score(best(Side.BUY).copy(candlesSinceCross = 1))
        val s2 = SignalScore.score(best(Side.BUY).copy(candlesSinceCross = 2))
        assertTrue(s0 > s1 && s1 > s2)
        assertEquals(5, s0 - s1)
    }

    @Test
    fun rsiOutsideComfortRangeLowersScore() {
        val inside = SignalScore.score(best(Side.BUY).copy(rsi = 60.0))
        val outside = SignalScore.score(best(Side.BUY).copy(rsi = 75.0))
        assertEquals(15, inside - outside)
    }

    @Test
    fun adxScalesBetweenFifteenAndTwentyFive() {
        val low = SignalScore.score(best(Side.BUY).copy(adx = 15.0))
        val mid = SignalScore.score(best(Side.BUY).copy(adx = 20.0))
        val high = SignalScore.score(best(Side.BUY).copy(adx = 25.0))
        assertEquals(15, high - low)
        assertTrue(mid in (low + 1)..(high - 1))
    }

    @Test
    fun labelsFollowTheTable() {
        assertEquals(ScoreLabel.EXCELLENT, SignalScore.label(100))
        assertEquals(ScoreLabel.EXCELLENT, SignalScore.label(90))
        assertEquals(ScoreLabel.VERY_GOOD, SignalScore.label(89))
        assertEquals(ScoreLabel.VERY_GOOD, SignalScore.label(80))
        assertEquals(ScoreLabel.GOOD, SignalScore.label(79))
        assertEquals(ScoreLabel.GOOD, SignalScore.label(70))
        assertEquals(ScoreLabel.MEDIUM, SignalScore.label(69))
        assertEquals(ScoreLabel.MEDIUM, SignalScore.label(60))
        assertEquals(ScoreLabel.LOW, SignalScore.label(59))
        assertEquals(ScoreLabel.LOW, SignalScore.label(50))
        assertEquals(ScoreLabel.CLOSE, SignalScore.label(49))
        assertEquals(ScoreLabel.CLOSE, SignalScore.label(40))
        assertEquals(ScoreLabel.WAIT, SignalScore.label(39))
        assertEquals(ScoreLabel.WAIT, SignalScore.label(30))
    }

    @Test
    fun belowThirtyIsHidden() {
        assertNull(SignalScore.label(29))
        assertNull(SignalScore.label(0))
    }

    @Test
    fun isSignalStartsAtFifty() {
        assertTrue(!SignalScore.isSignal(49))
        assertTrue(SignalScore.isSignal(50))
    }
}
