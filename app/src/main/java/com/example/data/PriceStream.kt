package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

// Live prices for all coins from one Binance stream. Keys are symbols like "BTCUSDT".
class PriceStream(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
) {
    private val url = "wss://data-stream.binance.vision/ws/!miniTicker@arr"

    private val _prices = MutableStateFlow<Map<String, Double>>(emptyMap())
    val prices: StateFlow<Map<String, Double>> = _prices.asStateFlow()

    @Volatile private var wanted = false
    @Volatile private var retrying = false
    private var socket: WebSocket? = null
    private var symbols: Set<String> = emptySet()

    // symbols example: setOf("BTCUSDT", "ETHUSDT")
    @Synchronized
    fun start(symbols: Set<String>) {
        if (wanted) return
        this.symbols = symbols
        wanted = true
        connect()
    }

    @Synchronized
    fun stop() {
        wanted = false
        socket?.close(1000, null)
        socket = null
    }

    @Synchronized
    private fun connect() {
        if (!wanted) return
        retrying = false
        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                handle(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                retry()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                retry()
            }
        })
    }

    private fun retry() {
        if (!wanted || retrying) return
        retrying = true
        thread {
            Thread.sleep(3000)
            connect()
        }
    }

    private fun handle(text: String) {
        try {
            val arr = JSONArray(text)
            val updates = HashMap<String, Double>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val s = o.getString("s")
                if (s in symbols) updates[s] = o.getString("c").toDouble()
            }
            if (updates.isNotEmpty()) {
                _prices.value = _prices.value + updates
            }
        } catch (e: Exception) {
            // ignore malformed messages
        }
    }
}
