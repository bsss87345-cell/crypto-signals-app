package com.example.data

import android.content.Context
import org.json.JSONObject

// A signal whose levels are frozen until it expires or is closed.
data class StoredSignal(
    val coin: String,
    val side: Side,
    val score: Int,
    val entryLow: Double,
    val entryHigh: Double,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val target3: Double,
    val durationText: String,
    val startMillis: Long,
    val validUntilMillis: Long
)

// Local storage on this phone only (SharedPreferences). One active signal per coin.
class SignalStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("signals_store", Context.MODE_PRIVATE)

    fun get(coin: String): StoredSignal? {
        val raw = prefs.getString(key(coin), null) ?: return null
        return try {
            fromJson(JSONObject(raw))
        } catch (e: Exception) {
            null
        }
    }

    fun put(signal: StoredSignal) {
        prefs.edit().putString(key(signal.coin), toJson(signal).toString()).apply()
    }

    fun remove(coin: String) {
        prefs.edit().remove(key(coin)).apply()
    }

    private fun key(coin: String) = "active_$coin"

    private fun toJson(s: StoredSignal): JSONObject = JSONObject().apply {
        put("coin", s.coin)
        put("side", s.side.name)
        put("score", s.score)
        put("entryLow", s.entryLow)
        put("entryHigh", s.entryHigh)
        put("stopLoss", s.stopLoss)
        put("target1", s.target1)
        put("target2", s.target2)
        put("target3", s.target3)
        put("durationText", s.durationText)
        put("startMillis", s.startMillis)
        put("validUntilMillis", s.validUntilMillis)
    }

    private fun fromJson(o: JSONObject): StoredSignal = StoredSignal(
        coin = o.getString("coin"),
        side = Side.valueOf(o.getString("side")),
        score = o.getInt("score"),
        entryLow = o.getDouble("entryLow"),
        entryHigh = o.getDouble("entryHigh"),
        stopLoss = o.getDouble("stopLoss"),
        target1 = o.getDouble("target1"),
        target2 = o.getDouble("target2"),
        target3 = o.getDouble("target3"),
        durationText = o.getString("durationText"),
        startMillis = o.getLong("startMillis"),
        validUntilMillis = o.getLong("validUntilMillis")
    )
}
