package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SignalTrackerTest {

    private val hour = 3_600_000L
    private val start = 10 * hour

    private fun stored(side: Side) =
        if (side == Side.BUY) {
            StoredSignal("BTC", Side.BUY, 70, 100.0, 102.0, 98.0, 103.0, 105.0, 107.0, "1 - 3", start, start + 3 * hour)
        } else {
            StoredSignal("BTC", Side.SELL, 70, 100.0, 102.0, 104.0, 99.0, 97.0, 95.0, "1 - 3", start, start + 3 * hour)
        }

    private fun c(i: Int, high: Double, low: Double) = Candle(
        openTime = start + i * hour,
        open = (high + low) / 2,
        high = high,
        low = low,
        close = (high + low) / 2,
        volume = 100.0,
        closeTime = start + i * hour + hour - 1
    )

    @Test
    fun active_whenNothingTouchedAndNotExpired() {
        val r = SignalTracker.evaluate(stored(Side.BUY), listOf(c(0, 101.0, 100.0)), start + hour)
        assertEquals(TrackStatus.ACTIVE, r.status)
        assertEquals(0, r.targetsHit)
    }

    @Test
    fun buy_targetOneOnly_staysActiveWithOneHit() {
        val r = SignalTracker.evaluate(stored(Side.BUY), listOf(c(0, 103.5, 100.0)), start + hour)
        assertEquals(TrackStatus.ACTIVE, r.status)
        assertEquals(1, r.targetsHit)
    }

    @Test
    fun buy_targetThree_completes() {
        val r = SignalTracker.evaluate(stored(Side.BUY), listOf(c(0, 108.0, 100.0)), start + hour)
        assertEquals(TrackStatus.TARGET3, r.status)
        assertEquals(3, r.targetsHit)
    }

    @Test
    fun buy_stopHit_stopsWithZeroHits() {
        val r = SignalTracker.evaluate(stored(Side.BUY), listOf(c(0, 101.0, 97.0)), start + hour)
        assertEquals(TrackStatus.STOPPED, r.status)
        assertEquals(0, r.targetsHit)
    }

    @Test
    fun stopAndTargetInSameCandle_stopWins() {
        val r = SignalTracker.evaluate(stored(Side.BUY), listOf(c(0, 108.0, 97.0)), start + hour)
        assertEquals(TrackStatus.STOPPED, r.status)
        assertEquals(0, r.targetsHit)
    }

    @Test
    fun buy_targetOneThenStop_keepsOneHit() {
        val candles = listOf(c(0, 103.5, 100.0), c(1, 101.0, 97.0))
        val r = SignalTracker.evaluate(stored(Side.BUY), candles, start + 2 * hour)
        assertEquals(TrackStatus.STOPPED, r.status)
        assertEquals(1, r.targetsHit)
    }

    @Test
    fun expired_keepsHitsReached() {
        val r = SignalTracker.evaluate(stored(Side.BUY), listOf(c(0, 105.5, 100.0)), start + 3 * hour)
        assertEquals(TrackStatus.EXPIRED, r.status)
        assertEquals(2, r.targetsHit)
    }

    @Test
    fun candlesBeforeStart_areIgnored() {
        val r = SignalTracker.evaluate(stored(Side.BUY), listOf(c(-1, 101.0, 90.0)), start + hour)
        assertEquals(TrackStatus.ACTIVE, r.status)
        assertEquals(0, r.targetsHit)
    }

    @Test
    fun sell_targetTwo_staysActiveWithTwoHits() {
        val r = SignalTracker.evaluate(stored(Side.SELL), listOf(c(0, 102.0, 96.5)), start + hour)
        assertEquals(TrackStatus.ACTIVE, r.status)
        assertEquals(2, r.targetsHit)
    }

    @Test
    fun sell_stopHit_stops() {
        val r = SignalTracker.evaluate(stored(Side.SELL), listOf(c(0, 104.5, 100.0)), start + hour)
        assertEquals(TrackStatus.STOPPED, r.status)
    }
}
