package com.rustbeatz.disasteralert.data.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import com.rustbeatz.disasteralert.data.network.BackendApi

data class AlertSummary(
    val id: Long,
    val alertCode: String,
    val disasterType: String,
    val emoji: String,
    val severity: String,
    val state: String,
    val district: String,
    val createdAt: String
) {
    val displayLabel: String
        get() = "$alertCode · $disasterType · $district"
}

data class NeedTotals(
    val emergency: Int,
    val medical: Int,
    val foodWater: Int,
    val shelter: Int,
    val other: Int,
    val unspecified: Int
)

data class ResponseTotals(
    val safe: Int,
    val help: Int,
    val helpWithLocation: Int,
    val byNeed: NeedTotals
)

data class ResponseItem(
    val replyId: Long,
    val name: String,
    val response: String, // "HELP" or "SAFE"
    val need: String?,    // "EMERGENCY", "MEDICAL", "FOOD_WATER", "SHELTER", "OTHER", "UNSPECIFIED", or null
    val priority: String?,// "HIGH", "MEDIUM", "LOW", or null
    val city: String?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyM: Double?,
    val note: String?,
    val at: String,       // UTC ISO-8601
    val locationAt: String?
) {
    val isSimulated: Boolean
        get() = name.startsWith("Demo citizen", ignoreCase = true)

    val hasLocation: Boolean
        get() = latitude != null && longitude != null

    val istTime: String
        get() = TimeUtils.formatUtcToIst(at)

    val friendlyNeedLabel: String
        get() = when (need?.uppercase()) {
            "EMERGENCY" -> "Emergency / Rescue"
            "MEDICAL" -> "Medicine"
            "FOOD_WATER" -> "Food / Water"
            "SHELTER" -> "Shelter"
            "OTHER" -> "Other"
            "UNSPECIFIED" -> "Not specified"
            else -> if (response == "SAFE") "Safe" else "Help Request"
        }
}

data class AlertResponsesResult(
    val alertId: Long,
    val alertCode: String,
    val totals: ResponseTotals,
    val items: List<ResponseItem>,
    val rawJson: String
)

object TimeUtils {
    fun formatUtcToIst(utcIsoString: String): String {
        return try {
            val sdfUtc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdfUtc.parse(utcIsoString)
            if (date != null) {
                val sdfIst = SimpleDateFormat("hh:mm a, dd MMM", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                }
                sdfIst.format(date)
            } else {
                utcIsoString
            }
        } catch (_: Exception) {
            utcIsoString
        }
    }
}

interface ResponsesRepository {
    suspend fun alerts(): List<AlertSummary>
    suspend fun responses(alertId: Long): AlertResponsesResult
}

/**
 * Mock data repository for Slice S1 (Phase A).
 * Loads sample responses JSON and mock alert list, parsing with org.json exactly as in BackendApi.kt.
 */
class MockResponsesRepository : ResponsesRepository {

    // Built-in sample responses JSON exactly matching responses-sample.json
    private val sampleResponsesJson = """
    {
      "alertId": 12,
      "alertCode": "ALERT-00012",
      "totals": {
        "safe": 1,
        "help": 5,
        "helpWithLocation": 4,
        "byNeed": {
          "EMERGENCY": 1,
          "MEDICAL": 1,
          "FOOD_WATER": 2,
          "SHELTER": 0,
          "OTHER": 1,
          "UNSPECIFIED": 0
        }
      },
      "items": [
        {"replyId": 9, "name": "Ravi", "response": "HELP", "need": "EMERGENCY", "priority": "HIGH",
         "city": "Kollur", "latitude": 13.8711, "longitude": 74.8032, "accuracyM": 18.0,
         "note": null, "at": "2026-11-02T05:44:03Z", "locationAt": "2026-11-02T05:44:31Z"},
        {"replyId": 7, "name": "Asha", "response": "HELP", "need": "MEDICAL", "priority": "HIGH",
         "city": "Kollur", "latitude": 13.8626, "longitude": 74.8147, "accuracyM": 35.0,
         "note": null, "at": "2026-11-02T05:41:12Z", "locationAt": "2026-11-02T05:41:40Z"},
        {"replyId": 11, "name": "Demo citizen 1", "response": "HELP", "need": "FOOD_WATER", "priority": "MEDIUM",
         "city": "Udupi", "latitude": 13.8402, "longitude": 74.8399, "accuracyM": 52.0,
         "note": "Simulated request (demo data)", "at": "2026-11-02T05:43:20Z", "locationAt": "2026-11-02T05:43:40Z"},
        {"replyId": 8, "name": "Meena", "response": "HELP", "need": "FOOD_WATER", "priority": "MEDIUM",
         "city": "Udupi", "latitude": null, "longitude": null, "accuracyM": null,
         "note": null, "at": "2026-11-02T05:42:50Z", "locationAt": null},
        {"replyId": 10, "name": "Imran", "response": "HELP", "need": "OTHER", "priority": "LOW",
         "city": "Kollur", "latitude": 13.8555, "longitude": 74.7901, "accuracyM": null,
         "note": "Need blankets for 3 children", "at": "2026-11-02T05:40:02Z", "locationAt": "2026-11-02T05:40:30Z"},
        {"replyId": 6, "name": "Latha", "response": "SAFE", "need": null, "priority": null,
         "city": "Udupi", "latitude": null, "longitude": null, "accuracyM": null,
         "note": null, "at": "2026-11-02T05:39:10Z", "locationAt": null}
      ]
    }
    """.trimIndent()

    override suspend fun alerts(): List<AlertSummary> = withContext(Dispatchers.IO) {
        // Return small made-up alert list (newest first)
        listOf(
            AlertSummary(
                id = 12L,
                alertCode = "ALERT-00012",
                disasterType = "Flood",
                emoji = "🌊",
                severity = "RED",
                state = "Karnataka",
                district = "Udupi",
                createdAt = "2026-11-02T05:30:00Z"
            ),
            AlertSummary(
                id = 67L,
                alertCode = "ALERT-00067",
                disasterType = "Flood",
                emoji = "🌊",
                severity = "RED",
                state = "Karnataka",
                district = "Udupi",
                createdAt = "2026-11-02T04:45:00Z"
            ),
            AlertSummary(
                id = 94L,
                alertCode = "ALERT-00094",
                disasterType = "Landslide",
                emoji = "⛰️",
                severity = "ORANGE",
                state = "Kerala",
                district = "Wayanad",
                createdAt = "2026-11-02T03:20:00Z"
            ),
            AlertSummary(
                id = 101L,
                alertCode = "ALERT-00101",
                disasterType = "Cyclone",
                emoji = "🌀",
                severity = "RED",
                state = "Odisha",
                district = "Puri",
                createdAt = "2026-11-01T22:10:00Z"
            )
        )
    }

    override suspend fun responses(alertId: Long): AlertResponsesResult = withContext(Dispatchers.IO) {
        parseResponsesJson(sampleResponsesJson, alertId)
    }

    fun parseResponsesJson(jsonString: String, overrideAlertId: Long? = null): AlertResponsesResult {
        val root = JSONObject(jsonString)
        val alertId = overrideAlertId ?: root.getLong("alertId")
        val alertCode = root.optString("alertCode", "ALERT-$alertId")

        val totalsObj = root.getJSONObject("totals")
        val byNeedObj = totalsObj.getJSONObject("byNeed")
        val needTotals = NeedTotals(
            emergency = byNeedObj.optInt("EMERGENCY", 0),
            medical = byNeedObj.optInt("MEDICAL", 0),
            foodWater = byNeedObj.optInt("FOOD_WATER", 0),
            shelter = byNeedObj.optInt("SHELTER", 0),
            other = byNeedObj.optInt("OTHER", 0),
            unspecified = byNeedObj.optInt("UNSPECIFIED", 0)
        )
        val totals = ResponseTotals(
            safe = totalsObj.optInt("safe", 0),
            help = totalsObj.optInt("help", 0),
            helpWithLocation = totalsObj.optInt("helpWithLocation", 0),
            byNeed = needTotals
        )

        val itemsArray = root.getJSONArray("items")
        val itemsList = mutableListOf<ResponseItem>()
        for (i in 0 until itemsArray.length()) {
            val item = itemsArray.getJSONObject(i)
            itemsList.add(
                ResponseItem(
                    replyId = item.getLong("replyId"),
                    name = item.getString("name"),
                    response = item.optString("response", "HELP"),
                    need = if (item.isNull("need")) null else item.getString("need"),
                    priority = if (item.isNull("priority")) null else item.getString("priority"),
                    city = if (item.isNull("city")) null else item.optString("city"),
                    latitude = if (item.isNull("latitude")) null else item.getDouble("latitude"),
                    longitude = if (item.isNull("longitude")) null else item.getDouble("longitude"),
                    accuracyM = if (item.isNull("accuracyM")) null else item.getDouble("accuracyM"),
                    note = if (item.isNull("note")) null else item.optString("note"),
                    at = item.optString("at", ""),
                    locationAt = if (item.isNull("locationAt")) null else item.optString("locationAt")
                )
            )
        }

        return AlertResponsesResult(
            alertId = alertId,
            alertCode = alertCode,
            totals = totals,
            items = itemsList,
            rawJson = jsonString
        )
    }
}

/**
 * Production & Live repository for Slice S1.
 * Fetches real citizen help responses from BackendApi, and smoothly falls back to mock
 * data if the server is offline or unreachable.
 */
class NetworkResponsesRepository : ResponsesRepository {
    private val mockFallback = MockResponsesRepository()

    override suspend fun alerts(): List<AlertSummary> = withContext(Dispatchers.IO) {
        try {
            val live = BackendApi.getAlerts()
            if (live.isNotEmpty()) live else mockFallback.alerts()
        } catch (_: Exception) {
            mockFallback.alerts()
        }
    }

    override suspend fun responses(alertId: Long): AlertResponsesResult = withContext(Dispatchers.IO) {
        try {
            BackendApi.getResponses(alertId)
        } catch (_: Exception) {
            mockFallback.responses(alertId)
        }
    }
}
