package com.rustbeatz.disasteralert.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Signed-in officer for this app session (held in memory; the backend restarts => sign in again). */
object Session {
    var token: String? = null
    var officerName: String? = null

    fun clear() {
        token = null
        officerName = null
    }
}

data class LoginResult(val token: String, val officerName: String)

/** Exactly what the Spring Boot backend's POST /api/alerts expects. */
data class DispatchRequest(
    val disasterType: String,   // DisasterType.id, e.g. "flood"
    val state: String,          // IndianState.name
    val district: String,
    val city: String?,          // optional free text
    val severity: String,       // AlertSeverity.name: RED / ORANGE / YELLOW / GREEN
    val instructions: String,
    val authorityId: String?
)

data class DispatchResult(
    val id: Long,
    val alertCode: String,
    val status: String,         // SENDING or COMPLETED
    val scope: String,
    val recipients: Int,
    val delivered: Int,
    val failed: Int
)

class ApiException(message: String, val unauthorized: Boolean = false) : Exception(message)

/**
 * Talks to the DisasterAlert backend (Spring Boot).
 *
 * BASE_URL uses the phone's own loopback because the phone is on USB and the laptop forwards the port:
 *     adb reverse tcp:8080 tcp:8080
 * (Run it again every time the phone is re-plugged.)
 * On the Android emulator use http://10.0.2.2:8080 instead.
 */
object BackendApi {

    const val BASE_URL = "http://127.0.0.1:8080"

    suspend fun login(officerId: String, pin: String): LoginResult {
        val body = JSONObject().put("officerId", officerId).put("pin", pin)
        val json = call("POST", "/api/auth/login", body, authenticated = false)
        return LoginResult(
            token = json.getString("token"),
            officerName = json.optString("officerName", officerId)
        )
    }

    /** How many registered citizens a broadcast to this State + District would reach. */
    suspend fun targetCount(state: String, district: String): Int {
        val path = "/api/targets?state=${enc(state)}&district=${enc(district)}"
        return call("GET", path).getInt("citizens")
    }

    suspend fun dispatch(request: DispatchRequest): DispatchResult {
        val body = JSONObject()
            .put("disasterType", request.disasterType)
            .put("state", request.state)
            .put("district", request.district)
            .put("severity", request.severity)
            .put("instructions", request.instructions)
        if (!request.city.isNullOrBlank()) body.put("city", request.city)
        if (!request.authorityId.isNullOrBlank()) body.put("authorityId", request.authorityId)

        val json = call("POST", "/api/alerts", body)
        return DispatchResult(
            id = json.getLong("id"),
            alertCode = json.optString("alertCode", "ALERT-${json.getLong("id")}"),
            status = json.optString("status", "COMPLETED"),
            scope = json.optString("scope", ""),
            recipients = json.optInt("recipients", 0),
            delivered = json.optInt("delivered", 0),
            failed = json.optInt("failed", 0)
        )
    }

    // ---------------------------------------------------------------- plumbing

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private suspend fun call(
        method: String,
        path: String,
        body: JSONObject? = null,
        authenticated: Boolean = true
    ): JSONObject = withContext(Dispatchers.IO) {
        val token = Session.token
        if (authenticated && token == null) {
            throw ApiException("Please sign in again.", unauthorized = true)
        }

        val conn = URL(BASE_URL + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 8_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Accept", "application/json")
            if (authenticated && token != null) {
                conn.setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }

            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                .orEmpty()

            if (code in 200..299) {
                JSONObject(text.ifBlank { "{}" })
            } else {
                val serverMessage = try {
                    JSONObject(text).optString("error")
                } catch (e: JSONException) {
                    ""
                }
                when (code) {
                    401 -> throw ApiException(
                        if (authenticated) "Session expired. Please sign in again." else serverMessage.ifBlank { "Invalid officer ID or PIN." },
                        unauthorized = authenticated
                    )
                    else -> throw ApiException(serverMessage.ifBlank { "Server error ($code)." })
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: IOException) {
            throw ApiException(
                "Cannot reach the dispatch server. Check that the backend is running and " +
                    "'adb reverse tcp:8080 tcp:8080' is active."
            )
        } catch (e: JSONException) {
            throw ApiException("Unexpected response from the dispatch server.")
        } finally {
            conn.disconnect()
        }
    }
}
