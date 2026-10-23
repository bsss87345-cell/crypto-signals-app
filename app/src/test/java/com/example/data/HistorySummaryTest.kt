package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HistorySummaryTest {

    private fun entry(status: TrackStatus, result: Double) = HistoryEntry(
        coin = "BTC", side = Side.BUY, score = 70,
        entryLow = 100.0, entryHigh = 102.0, stopLoss = 98.0,
        target1 = 103.0, target2 = 105.0, target3 = 107.0,
        targetsHit = 0, status = status, resultPercent = result,
        startMillis = 0L, endMillis = 1L, sparkline = emptyList()
    )

    @Test
    fun emptyList_givesZeros() {
        val s = HistoryStats.summarize(emptyList())
        assertEquals(0, s.entered)
        assertEquals(0, s.notEntered)
        assertEquals(0.0, s.averagePercent, 1e-9)
    }

    @Test
    fun mixedEntries_areCountedSeparately() {
        val s = HistoryStats.summarize(
            listOf(
                entry(TrackStatus.TARGET3, 2.0),
                entry(TrackStatus.STOPPED, -1.0),
                entry(TrackStatus.EXPIRED, 0.0),
                entry(TrackStatus.NOT_ENTERED, 0.0)
            )
        )
        assertEquals(3, s.entered)
        assertEquals(1, s.wins)
        assertEquals(1, s.losses)
        assertEquals(1, s.flat)
        assertEquals(1, s.notEntered)
        assertEquals(1.0 / 3.0, s.averagePercent, 1e-9)
    }

    @Test
    fun notEntered_isExcludedFromAverage() {
        val s = HistoryStats.summarize(
            listOf(entry(TrackStatus.TARGET3, 3.0), entry(TrackStatus.NOT_ENTERED, 0.0))
        )
        assertEquals(3.0, s.averagePercent, 1e-9)
    }
}
