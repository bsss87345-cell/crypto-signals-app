package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SignalTrackerEntryTest {

    private val hour = 3_600_000L
    private val start = 10 * hour

    private val buy = StoredSignal("BTC", Side.BUY, 70, 100.0, 102.0, 98.0, 103.0, 105.0, 107.0, "1 - 3", start, start + 3 * hour)
    private val sell = StoredSignal("BTC", Side.SELL, 70, 100.0, 102.0, 104.0, 99.0, 97.0, 95.0, "1 - 3", start, start + 3 * hour)

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
    fun buy_neverTouchesZone_expiresAsNotEntered() {
        val r = SignalTracker.evaluate(buy, listOf(c(0, 99.5, 99.0)), start + 3 * hour)
        assertEquals(TrackStatus.NOT_ENTERED, r.status)
        assertEquals(0, r.targetsHit)
    }

    @Test
    fun buy_notTouchedYet_staysActiveBeforeExpiry() {
        val r = SignalTracker.evaluate(buy, listOf(c(0, 99.5, 99.0)), start + hour)
        assertEquals(TrackStatus.ACTIVE, r.status)
    }

    @Test
    fun buy_runsAwayAboveZone_isNotEnteredImmediately() {
        val r = SignalTracker.evaluate(buy, listOf(c(0, 104.0, 103.2)), start + hour)
        assertEquals(TrackStatus.NOT_ENTERED, r.status)
    }

    @Test
    fun buy_touchesZoneLater_thenHitsTargetOne() {
        val candles = listOf(c(0, 102.8, 102.2), c(1, 103.5, 101.5))
        val r = SignalTracker.evaluate(buy, candles, start + 2 * hour)
        assertEquals(TrackStatus.ACTIVE, r.status)
        assertEquals(1, r.targetsHit)
    }

    @Test
    fun sell_neverTouchesZone_expiresAsNotEntered() {
        val r = SignalTracker.evaluate(sell, listOf(c(0, 103.5, 102.5)), start + 3 * hour)
        assertEquals(TrackStatus.NOT_ENTERED, r.status)
    }

    @Test
    fun sell_stopReachedWithoutEntry_isNotEntered() {
        val r = SignalTracker.evaluate(sell, listOf(c(0, 104.5, 102.5)), start + hour)
        assertEquals(TrackStatus.NOT_ENTERED, r.status)
    }

    @Test
    fun sell_touchesZoneThenStops_isStopped() {
        val r = SignalTracker.evaluate(sell, listOf(c(0, 104.5, 101.0)), start + hour)
        assertEquals(TrackStatus.STOPPED, r.status)
    }
}
