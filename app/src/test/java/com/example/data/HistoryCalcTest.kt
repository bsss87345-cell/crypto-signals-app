package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryCalcTest {

    private val buyTargets = listOf(101.0, 102.0, 103.0)
    private val sellTargets = listOf(99.0, 98.0, 97.0)

    @Test
    fun buy_allTargets_isWeightedAverage() {
        val r = HistoryCalc.resultPercent(Side.BUY, 100.0, buyTargets, 98.0, 3, TrackStatus.TARGET3, 100.0)
        assertEquals(1.9, r, 1e-9)
    }

    @Test
    fun buy_stoppedWithNoTargets_isStopLoss() {
        val r = HistoryCalc.resultPercent(Side.BUY, 100.0, buyTargets, 98.0, 0, TrackStatus.STOPPED, 100.0)
        assertEquals(-2.0, r, 1e-9)
    }

    @Test
    fun buy_targetOneThenStopped_mixesGainAndLoss() {
        val r = HistoryCalc.resultPercent(Side.BUY, 100.0, buyTargets, 98.0, 1, TrackStatus.STOPPED, 100.0)
        assertEquals(-0.8, r, 1e-9)
    }

    @Test
    fun buy_expiredAfterTwoTargets_usesLastCloseForRest() {
        val r = HistoryCalc.resultPercent(Side.BUY, 100.0, buyTargets, 98.0, 2, TrackStatus.EXPIRED, 100.5)
        assertEquals(1.15, r, 1e-9)
    }

    @Test
    fun sell_allTargets_isWeightedAverage() {
        val r = HistoryCalc.resultPercent(Side.SELL, 100.0, sellTargets, 102.0, 3, TrackStatus.TARGET3, 100.0)
        assertEquals(1.9, r, 1e-9)
    }

    @Test
    fun sell_stoppedWithNoTargets_isStopLoss() {
        val r = HistoryCalc.resultPercent(Side.SELL, 100.0, sellTargets, 102.0, 0, TrackStatus.STOPPED, 100.0)
        assertEquals(-2.0, r, 1e-9)
    }

    @Test
    fun zeroEntry_returnsZero() {
        val r = HistoryCalc.resultPercent(Side.BUY, 0.0, buyTargets, 98.0, 3, TrackStatus.TARGET3, 100.0)
        assertEquals(0.0, r, 1e-9)
    }
}
