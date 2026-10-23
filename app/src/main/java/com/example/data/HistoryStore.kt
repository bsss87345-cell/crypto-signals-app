package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// A finished trade kept in the history (stored locally on this phone).
data class HistoryEntry(
    val coin: String,
    val side: Side,
    val score: Int,
    val entryLow: Double,
    val entryHigh: Double,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val target3: Double,
    val targetsHit: Int,
    val status: TrackStatus,
    val resultPercent: Double,
    val startMillis: Long,
    val endMillis: Long,
    val sparkline: List<Float>
)

// Result of a trade in percent from the entry-zone midpoint.
// Exit split: 40% at target 1, 30% at target 2, 30% at target 3.
// Targets not reached close at the stop (STOPPED) or at the last close (EXPIRED).
// Stop is never moved. Fees and slippage are not included.
object HistoryCalc {
    private val shares = listOf(0.4, 0.3, 0.3)

    fun resultPercent(
        side: Side,
        entryMid: Double,
        targets: List<Double>,
        stopLoss: Double,
        targetsHit: Int,
        status: TrackStatus,
        lastClose: Double
    ): Double {
        if (entryMid <= 0.0) return 0.0
        fun pct(exit: Double): Double =
            if (side == Side.BUY) (exit - entryMid) / entryMid * 100.0
            else (entryMid - exit) / entryMid * 100.0

        val remainingExit = when (status) {
            TrackStatus.STOPPED -> stopLoss
            TrackStatus.EXPIRED -> lastClose
            else -> entryMid
        }
        var total = 0.0
        for (i in 0..2) {
            total += shares[i] * (if (i < targetsHit) pct(targets[i]) else pct(remainingExit))
        }
        return total
    }
}

// Keeps the last 50 finished trades, newest first.
class HistoryStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("history_store", Context.MODE_PRIVATE)

    companion object {
        private const val KEY = "entries"
        private const val MAX_ENTRIES = 50
    }

    fun all(): List<HistoryEntry> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(entry: HistoryEntry) {
        val updated = (listOf(entry) + all()).take(MAX_ENTRIES)
        val arr = JSONArray()
        updated.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun toJson(e: HistoryEntry): JSONObject = JSONObject().apply {
        put("coin", e.coin)
        put("side", e.side.name)
        put("score", e.score)
        put("entryLow", e.entryLow)
        put("entryHigh", e.entryHigh)
        put("stopLoss", e.stopLoss)
        put("target1", e.target1)
        put("target2", e.target2)
        put("target3", e.target3)
        put("targetsHit", e.targetsHit)
        put("status", e.status.name)
        put("resultPercent", e.resultPercent)
        put("startMillis", e.startMillis)
        put("endMillis", e.endMillis)
        val sp = JSONArray()
        e.sparkline.forEach { sp.put(it.toDouble()) }
        put("sparkline", sp)
    }

    private fun fromJson(o: JSONObject): HistoryEntry {
        val sp = o.getJSONArray("sparkline")
        return HistoryEntry(
            coin = o.getString("coin"),
            side = Side.valueOf(o.getString("side")),
            score = o.getInt("score"),
            entryLow = o.getDouble("entryLow"),
            entryHigh = o.getDouble("entryHigh"),
            stopLoss = o.getDouble("stopLoss"),
            target1 = o.getDouble("target1"),
            target2 = o.getDouble("target2"),
            target3 = o.getDouble("target3"),
            targetsHit = o.getInt("targetsHit"),
            status = TrackStatus.valueOf(o.getString("status")),
            resultPercent = o.getDouble("resultPercent"),
            startMillis = o.getLong("startMillis"),
            endMillis = o.getLong("endMillis"),
            sparkline = (0 until sp.length()).map { sp.getDouble(it).toFloat() }
        )
    }
}
