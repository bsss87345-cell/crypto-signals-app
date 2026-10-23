package com.example.data

enum class TrackStatus { ACTIVE, STOPPED, TARGET3, EXPIRED, NOT_ENTERED }

data class TrackResult(
    val status: TrackStatus,
    val targetsHit: Int // 0..3
)

// Checks what happened to a stored signal using 1h candles (oldest -> newest).
// Targets and stop only count after price touched the entry zone.
object SignalTracker {

    fun evaluate(s: StoredSignal, candles: List<Candle>, nowMillis: Long): TrackResult {
        val targets = listOf(s.target1, s.target2, s.target3)
        var hits = 0
        var entered = false

        val relevant = candles.filter {
            it.openTime >= s.startMillis && it.openTime < s.validUntilMillis
        }

        for (c in relevant) {
            val stopHit = if (s.side == Side.BUY) c.low <= s.stopLoss else c.high >= s.stopLoss

            if (!entered) {
                val touchesZone = c.low <= s.entryHigh && c.high >= s.entryLow
                if (touchesZone) {
                    entered = true
                } else {
                    // Price moved away without touching the zone: the entry was missed
                    val ranAway = reached(s.side, c, targets[0])
                    if (stopHit || ranAway) return TrackResult(TrackStatus.NOT_ENTERED, 0)
                    continue
                }
            }

            // Stop is checked first: if both happen in the same candle we assume the worst case
            if (stopHit) return TrackResult(TrackStatus.STOPPED, hits)

            while (hits < 3 && reached(s.side, c, targets[hits])) hits++
            if (hits == 3) return TrackResult(TrackStatus.TARGET3, 3)
        }

        if (!entered) {
            return if (nowMillis >= s.validUntilMillis) {
                TrackResult(TrackStatus.NOT_ENTERED, 0)
            } else {
                TrackResult(TrackStatus.ACTIVE, 0)
            }
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
