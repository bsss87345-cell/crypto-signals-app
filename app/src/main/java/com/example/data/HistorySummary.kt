package com.example.data

data class HistorySummary(
    val entered: Int,        // trades where entry was activated
    val wins: Int,           // result above 0
    val losses: Int,         // result below 0
    val flat: Int,           // result exactly 0
    val notEntered: Int,     // entry zone never touched
    val averagePercent: Double // average result of entered trades
)

object HistoryStats {
    fun summarize(entries: List<HistoryEntry>): HistorySummary {
        val enteredList = entries.filter { it.status != TrackStatus.NOT_ENTERED }
        val average = if (enteredList.isEmpty()) 0.0
        else enteredList.sumOf { it.resultPercent } / enteredList.size
        return HistorySummary(
            entered = enteredList.size,
            wins = enteredList.count { it.resultPercent > 0.0 },
            losses = enteredList.count { it.resultPercent < 0.0 },
            flat = enteredList.count { it.resultPercent == 0.0 },
            notEntered = entries.size - enteredList.size,
            averagePercent = average
        )
    }
}
