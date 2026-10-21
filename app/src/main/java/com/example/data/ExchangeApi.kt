package com.example.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

data class Candle(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long
)

class ExchangeApi(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val baseUrl = "https://data-api.binance.vision"

    // interval: "1h" or "4h". Returns oldest -> newest, includes the running candle at the end.
    fun getCandles(symbol: String, interval: String, limit: Int = 200): List<Candle> {
        val url = "$baseUrl/api/v3/klines?symbol=$symbol&interval=$interval&limit=$limit"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("HTTP ${response.code}")
            }
            val body = response.body?.string() ?: throw java.io.IOException("Empty body")
            val arr = JSONArray(body)
            return (0 until arr.length()).map { i ->
                val k = arr.getJSONArray(i)
                Candle(
                    openTime = k.getLong(0),
                    open = k.getString(1).toDouble(),
                    high = k.getString(2).toDouble(),
                    low = k.getString(3).toDouble(),
                    close = k.getString(4).toDouble(),
                    volume = k.getString(5).toDouble(),
                    closeTime = k.getLong(6)
                )
            }
        }
    }
}
