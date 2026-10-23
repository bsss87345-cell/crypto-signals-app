package com.example.data

enum class TrackStatus { ACTIVE, STOPPED, TARGET3, EXPIRED, NOT_ENTERED }

data class TrackResult(
    val status: TrackStatus,
    val targetsHit: Int // 0..3
)

// Checks what happened to a stored signal using 1h candles (oldest -> newest).
object SignalTracker {

    fun evaluate(s: StoredSignal, candles: List<Candle>, nowMillis: Long): TrackResult {
        val targets = listOf(s.target1, s.target2, s.target3)
        var hits = 0

        val relevant = candles.filter {
            it.openTime >= s.startMillis && it.openTime < s.validUntilMillis
        }

        for (c in relevant) {
            // Stop is checked first: if both happen in the same candle we assume the worst case
            val stopHit = if (s.side == Side.BUY) c.low <= s.stopLoss else c.high >= s.stopLoss
            if (stopHit) return TrackResult(TrackStatus.STOPPED, hits)

            while (hits < 3 && reached(s.side, c, targets[hits])) hits++
            if (hits == 3) return TrackResult(TrackStatus.TARGET3, 3)
        }

        return if (nowMillis >= s.validUntilMillis) {
            TrackResult(TrackStatus.EXPIRED, hits)
        } else {
            TrackResult(TrackStatus.ACTIVE, hits)
        }
    }

    private fun reached(side: Side, c: Candle, target: Double): Boolean =
        if (side == Side.BUY) c.high >= target else c.low <= target
}
