package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class AuthResult {
    data class Success(val token: String, val expiresAt: Long) : AuthResult()
    data class Registered(val status: String) : AuthResult()
    data class Failure(val code: String) : AuthResult()
}

class AuthApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private suspend fun call(request: Request): Pair<Int, JSONObject?> =
        withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    val json = try { JSONObject(text) } catch (e: Exception) { null }
                    Pair(resp.code, json)
                }
            } catch (e: Exception) {
                Pair(-1, null)
            }
        }

    private fun postRequest(path: String, body: JSONObject): Request =
        Request.Builder()
            .url(BASE_URL + path)
            .post(body.toString().toRequestBody(jsonType))
            .build()

    suspend fun register(username: String, deviceName: String, deviceId: String): AuthResult {
        val body = JSONObject()
            .put("username", username)
            .put("device_name", deviceName)
            .put("device_id", deviceId)
        val (_, json) = call(postRequest("/register", body))
        if (json == null) return AuthResult.Failure("network")
        val status = json.optString("status")
        if (status == "banned") return AuthResult.Failure("banned")
        if (status.isNotEmpty()) return AuthResult.Registered(status)
        return AuthResult.Failure(json.optString("error", "server_error"))
    }

    suspend fun login(username: String, code: String, deviceId: String): AuthResult {
        val body = JSONObject()
            .put("username", username)
            .put("code", code)
            .put("device_id", deviceId)
        val (httpCode, json) = call(postRequest("/login", body))
        if (json == null) return AuthResult.Failure("network")
        if (httpCode == 200 && json.has("token")) {
            return AuthResult.Success(json.getString("token"), json.optLong("expires_at"))
        }
        return AuthResult.Failure(json.optString("error", "server_error"))
    }

    // returns: active, revoked, banned, invalid, or network
    suspend fun checkStatus(token: String): String {
        val request = Request.Builder()
            .url(BASE_URL + "/status")
            .header("Authorization", "Bearer $token")
            .build()
        val (_, json) = call(request)
        return json?.optString("status", "network") ?: "network"
    }

    companion object {
        const val BASE_URL = "https://crypto-signals-server.bsss87345.workers.dev"
    }
}
