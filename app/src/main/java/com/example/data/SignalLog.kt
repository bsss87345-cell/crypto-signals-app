package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// Snapshot of market conditions at the moment a signal appeared.
// Stored once per signal; the outcome is filled in when the signal closes.
data class SignalSnapshot(
    val id: String,
    val coin: String,
    val side: Side,
    val score: Int,
    val price: Double,
    val entryLow: Double,
    val entryHigh: Double,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val target3: Double,
    val ema20: Double?,
    val ema50: Double?,
    val rsi: Double?,
    val adx: Double?,
    val volumeRatio: Double?,
    val trend4hAgrees: Boolean?,
    val appearedAtMillis: Long,
    val outcome: String?,
    val outcomeAtMillis: Long?
)

// Keeps the last 500 signal snapshots, newest first. Local to this phone.
class SignalLog(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("signal_log", Context.MODE_PRIVATE)

    companion object {
        private const val KEY = "snapshots"
        private const val MAX_ENTRIES = 500
    }

    fun all(): List<SignalSnapshot> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Adds only if this id is not logged yet, so the same signal is never saved twice.
    fun addIfNew(s: SignalSnapshot): Boolean {
        val current = all()
        if (current.any { it.id == s.id }) return false
        val updated = (listOf(s) + current).take(MAX_ENTRIES)
        save(updated)
        return true
    }

    // Writes the final outcome of a signal that was logged earlier.
    fun setOutcome(id: String, outcome: String, atMillis: Long) {
        val updated = all().map {
            if (it.id == id && it.outcome == null) it.copy(outcome = outcome, outcomeAtMillis = atMillis) else it
        }
        save(updated)
    }

    private fun save(list: List<SignalSnapshot>) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun toJson(s: SignalSnapshot): JSONObject = JSONObject().apply {
        put("id", s.id)
        put("coin", s.coin)
        put("side", s.side.name)
        put("score", s.score)
        put("price", s.price)
        put("entryLow", s.entryLow)
        put("entryHigh", s.entryHigh)
        put("stopLoss", s.stopLoss)
        put("target1", s.target1)
        put("target2", s.target2)
        put("target3", s.target3)
        put("ema20", s.ema20 ?: JSONObject.NULL)
        put("ema50", s.ema50 ?: JSONObject.NULL)
        put("rsi", s.rsi ?: JSONObject.NULL)
        put("adx", s.adx ?: JSONObject.NULL)
        put("volumeRatio", s.volumeRatio ?: JSONObject.NULL)
        put("trend4hAgrees", s.trend4hAgrees ?: JSONObject.NULL)
        put("appearedAtMillis", s.appearedAtMillis)
        put("outcome", s.outcome ?: JSONObject.NULL)
        put("outcomeAtMillis", s.outcomeAtMillis ?: JSONObject.NULL)
    }

    private fun fromJson(o: JSONObject): SignalSnapshot = SignalSnapshot(
        id = o.getString("id"),
        coin = o.getString("coin"),
        side = Side.valueOf(o.getString("side")),
        score = o.getInt("score"),
        price = o.getDouble("price"),
        entryLow = o.getDouble("entryLow"),
        entryHigh = o.getDouble("entryHigh"),
        stopLoss = o.getDouble("stopLoss"),
        target1 = o.getDouble("target1"),
        target2 = o.getDouble("target2"),
        target3 = o.getDouble("target3"),
        ema20 = if (o.isNull("ema20")) null else o.getDouble("ema20"),
        ema50 = if (o.isNull("ema50")) null else o.getDouble("ema50"),
        rsi = if (o.isNull("rsi")) null else o.getDouble("rsi"),
        adx = if (o.isNull("adx")) null else o.getDouble("adx"),
        volumeRatio = if (o.isNull("volumeRatio")) null else o.getDouble("volumeRatio"),
        trend4hAgrees = if (o.isNull("trend4hAgrees")) null else o.getBoolean("trend4hAgrees"),
        appearedAtMillis = o.getLong("appearedAtMillis"),
        outcome = if (o.isNull("outcome")) null else o.getString("outcome"),
        outcomeAtMillis = if (o.isNull("outcomeAtMillis")) null else o.getLong("outcomeAtMillis")
    )
}
