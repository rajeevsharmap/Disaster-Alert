package com.rustbeatz.disasteralert.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import com.rustbeatz.disasteralert.data.repo.AlertSummary
import com.rustbeatz.disasteralert.data.repo.AlertResponsesResult
import com.rustbeatz.disasteralert.data.repo.MockResponsesRepository

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

    /**
     * Fetch list of recent alerts from the backend.
     */
    suspend fun getAlerts(): List<AlertSummary> = withContext(Dispatchers.IO) {
        val array = callArray("GET", "/api/alerts")
        val list = mutableListOf<AlertSummary>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val dType = obj.optString("disasterType", "Disaster")
            list.add(
                AlertSummary(
                    id = obj.optLong("id", i.toLong() + 1),
                    alertCode = obj.optString("alertCode", "ALERT-${obj.optLong("id")}"),
                    disasterType = dType,
                    emoji = when (dType.lowercase()) {
                        "flood" -> "🌊"
                        "cyclone" -> "🌀"
                        "landslide" -> "⛰️"
                        "cloudburst" -> "⛈️"
                        "earthquake" -> "🏚️"
                        "fire" -> "🔥"
                        "tsunami" -> "🌊"
                        else -> "🚨"
                    },
                    severity = obj.optString("severity", "RED"),
                    state = obj.optString("state", ""),
                    district = obj.optString("district", ""),
                    createdAt = obj.optString("createdAt", "")
                )
            )
        }
        list
    }

    /**
     * Fetch real-time responses and citizen coordinates for an alert.
     */
    suspend fun getResponses(alertId: Long): AlertResponsesResult = withContext(Dispatchers.IO) {
        val text = callRaw("GET", "/api/alerts/$alertId/responses?type=ALL")
        MockResponsesRepository().parseResponsesJson(text, alertId)
    }

    // ---------------------------------------------------------------- plumbing

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private suspend fun call(
        method: String,
        path: String,
        body: JSONObject? = null,
        authenticated: Boolean = true
    ): JSONObject = withContext(Dispatchers.IO) {
        val text = callRaw(method, path, body, authenticated)
        JSONObject(text.ifBlank { "{}" })
    }

    private suspend fun callArray(
        method: String,
        path: String,
        body: JSONObject? = null,
        authenticated: Boolean = true
    ): JSONArray = withContext(Dispatchers.IO) {
        val text = callRaw(method, path, body, authenticated)
        JSONArray(text.ifBlank { "[]" })
    }

    private suspend fun callRaw(
        method: String,
        path: String,
        body: JSONObject? = null,
        authenticated: Boolean = true
    ): String = withContext(Dispatchers.IO) {
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
                text
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
