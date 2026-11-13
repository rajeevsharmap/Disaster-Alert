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
import java.util.UUID
import com.rustbeatz.disasteralert.data.repo.AlertSummary
import com.rustbeatz.disasteralert.data.repo.AlertResponsesResult
import com.rustbeatz.disasteralert.data.repo.MockResponsesRepository
import com.rustbeatz.disasteralert.data.repo.ReportCreateRequest
import com.rustbeatz.disasteralert.data.repo.ReportCreateResult
import com.rustbeatz.disasteralert.data.repo.ReportMineItem
import com.rustbeatz.disasteralert.data.repo.ReportMineAlert
import com.rustbeatz.disasteralert.data.repo.ReportDetailItem
import com.rustbeatz.disasteralert.data.repo.ReportExif
import com.rustbeatz.disasteralert.data.repo.ReporterUser
import com.rustbeatz.disasteralert.data.repo.CreateReporterRequest
import com.rustbeatz.disasteralert.data.repo.RetryAnalysisResult
import com.rustbeatz.disasteralert.data.repo.DecisionRequest
import com.rustbeatz.disasteralert.data.repo.DecisionResult
import com.rustbeatz.disasteralert.data.repo.HazardDraftRequest
import com.rustbeatz.disasteralert.data.repo.HazardDraftResult
import com.rustbeatz.disasteralert.data.repo.PublishAlertRequest
import com.rustbeatz.disasteralert.data.repo.PublishAlertResult
import com.rustbeatz.disasteralert.data.repo.HazardAlertDetail

/** Global configuration toggle: when true, uses mock repository; when false, uses real backend. */
object AppConfig {
    /**
     * Switch to control backend connectivity for reports and login.
     * When true: uses MockReportsRepository and mock login credentials.
     * When false (default): talks directly to real Spring Boot backend.
     */
    var USE_MOCK_REPORTS: Boolean = false
}

/** Signed-in user (Authority officer or Field Reporter) for this app session. */
object Session {
    var token: String? = null
    var officerName: String? = null
    var role: String? = null        // "AUTHORITY" or "REPORTER"
    var userId: String? = null
    var state: String? = null       // reporter's assigned State (null for authority)
    var district: String? = null    // reporter's assigned District (null for authority)

    val isAuthority: Boolean
        get() = role == "AUTHORITY" || role == null

    val isReporter: Boolean
        get() = role == "REPORTER"

    fun clear() {
        token = null
        officerName = null
        role = null
        userId = null
        state = null
        district = null
    }
}

data class LoginResult(
    val token: String,
    val officerName: String,
    val role: String = "AUTHORITY",
    val userId: String? = null,
    val state: String? = null,
    val district: String? = null
)

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

class ApiException(
    message: String,
    val unauthorized: Boolean = false,
    val statusCode: Int = 0
) : Exception(message)

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
        if (AppConfig.USE_MOCK_REPORTS) {
            return if (officerId.contains("rep", ignoreCase = true) || officerId == "anil.rep" || officerId == "meena.rep") {
                LoginResult(
                    token = "mock-reporter-token-${System.currentTimeMillis()}",
                    officerName = if (officerId.startsWith("meena", ignoreCase = true)) "Meena" else "Anil",
                    role = "REPORTER",
                    userId = officerId,
                    state = "Karnataka",
                    district = "Udupi"
                )
            } else {
                LoginResult(
                    token = "mock-authority-token-${System.currentTimeMillis()}",
                    officerName = "District Collector Udupi",
                    role = "AUTHORITY",
                    userId = officerId,
                    state = null,
                    district = null
                )
            }
        }

        val body = JSONObject().put("officerId", officerId).put("pin", pin)
        val json = call("POST", "/api/auth/login", body, authenticated = false)
        val stateVal = if (json.isNull("state")) null else json.optString("state").ifBlank { null }
        val districtVal = if (json.isNull("district")) null else json.optString("district").ifBlank { null }
        return LoginResult(
            token = json.getString("token"),
            officerName = json.optString("officerName", officerId),
            role = json.optString("role", if (officerId.contains("rep", ignoreCase = true)) "REPORTER" else "AUTHORITY"),
            userId = json.optString("userId", officerId),
            state = stateVal,
            district = districtVal
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

    // ---------------------------------------------------------------- Slice S3a Endpoints

    /**
     * Submits a field report via multipart/form-data:
     * Part 1: "photo" (image/jpeg, filename="report.jpg")
     * Part 2: "data" (JSON with explicit Content-Type: application/json; charset=UTF-8)
     */
    suspend fun submitReport(request: ReportCreateRequest): ReportCreateResult = withContext(Dispatchers.IO) {
        val dataJson = JSONObject().apply {
            put("clientReportId", request.clientReportId)
            put("category", request.category)
            if (!request.note.isNullOrBlank()) put("note", request.note)
            put("state", request.state)
            put("district", request.district)
            if (!request.city.isNullOrBlank()) put("city", request.city)
            put("latitude", request.latitude)
            put("longitude", request.longitude)
            if (request.accuracyM != null) put("accuracyM", request.accuracyM)
            put("capturedAt", request.capturedAt)
            put("mockLocation", request.mockLocation)
        }

        val json = callMultipart("/api/reports", dataJson, request.photoBytes, "report.jpg")

        val flags = mutableListOf<String>()
        val flagsArray = json.optJSONArray("trustFlags")
        if (flagsArray != null) {
            for (i in 0 until flagsArray.length()) {
                flags.add(flagsArray.getString(i))
            }
        }

        ReportCreateResult(
            id = json.getLong("id"),
            reportCode = json.optString("reportCode", "REPORT-${json.optLong("id")}"),
            status = json.optString("status", "SUBMITTED"),
            analysisStatus = json.optString("analysisStatus", "NOT_STARTED"),
            trustFlags = flags,
            duplicate = json.optBoolean("duplicate", false),
            createdAt = json.optString("createdAt", "")
        )
    }

    /**
     * Fetch caller's reports: GET /api/reports/mine (Reporter only).
     */
    suspend fun getMyReports(): List<ReportMineItem> = withContext(Dispatchers.IO) {
        val array = callArray("GET", "/api/reports/mine")
        val list = mutableListOf<ReportMineItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(HazardAlertParser.parseReportMineItem(obj))
        }
        list
    }

    /**
     * Authority list: GET /api/reports?district=&category=&status=&limit=
     */
    suspend fun getAuthorityReports(
        district: String? = null,
        category: String? = null,
        status: String? = null,
        limit: Int = 100
    ): List<ReportDetailItem> = withContext(Dispatchers.IO) {
        val params = mutableListOf<String>()
        if (!district.isNullOrBlank()) params.add("district=${enc(district)}")
        if (!category.isNullOrBlank()) params.add("category=${enc(category)}")
        if (!status.isNullOrBlank()) params.add("status=${enc(status)}")
        params.add("limit=$limit")
        val query = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        val array = callArray("GET", "/api/reports$query")
        val list = mutableListOf<ReportDetailItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(parseReportDetailItem(obj))
        }
        list
    }

    /**
     * Authority detail: GET /api/reports/{id}
     */
    suspend fun getReportDetail(reportId: Long): ReportDetailItem = withContext(Dispatchers.IO) {
        val obj = call("GET", "/api/reports/$reportId")
        parseReportDetailItem(obj)
    }

    /**
     * Authority detail alias: GET /api/reports/{id}
     */
    suspend fun reportDetail(reportId: Long): ReportDetailItem = getReportDetail(reportId)

    /**
     * Retry AI analysis: POST /api/reports/{id}/analysis/retry
     */
    suspend fun retryAnalysis(reportId: Long): RetryAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val text = callRaw("POST", "/api/reports/$reportId/analysis/retry")
            val json = JSONObject(text.ifBlank { "{}" })
            RetryAnalysisResult(
                reportId = json.optLong("reportId", reportId),
                analysisStatus = json.optString("analysisStatus", "QUEUED")
            )
        } catch (e: ApiException) {
            if (e.statusCode == 409) {
                // 409 Conflict: Analysis is already running -> caller will just refresh
                RetryAnalysisResult(
                    reportId = reportId,
                    analysisStatus = "QUEUED",
                    alreadyRunning = true
                )
            } else {
                throw e
            }
        }
    }

    /**
     * Submit authority decision: POST /api/reports/{id}/decision
     */
    suspend fun submitDecision(reportId: Long, request: DecisionRequest): DecisionResult = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("decision", request.decision)
            if (!request.note.isNullOrBlank()) {
                put("note", request.note)
            }
        }
        val obj = call("POST", "/api/reports/$reportId/decision", body)
        HazardAlertParser.parseDecisionResult(obj)
    }

    /**
     * Save or update hazard alert draft: PUT /api/reports/{id}/hazard-alert
     */
    suspend fun saveHazardDraft(reportId: Long, request: HazardDraftRequest): HazardDraftResult = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("hazardType", request.hazardType)
            put("severity", request.severity)
            put("state", request.state)
            put("district", request.district)
            if (!request.city.isNullOrBlank()) put("city", request.city) else put("city", JSONObject.NULL)
            put("title", request.title)
            put("message", request.message)
            if (!request.instructions.isNullOrBlank()) put("instructions", request.instructions) else put("instructions", JSONObject.NULL)
            if (!request.validUntil.isNullOrBlank()) put("validUntil", request.validUntil) else put("validUntil", JSONObject.NULL)
        }
        val obj = call("PUT", "/api/reports/$reportId/hazard-alert", body)
        HazardAlertParser.parseHazardDraftResult(obj)
    }

    /**
     * Explicit publish: POST /api/hazard-alerts/{id}/publish
     */
    suspend fun publishHazardAlert(hazardAlertId: Long, request: PublishAlertRequest): PublishAlertResult = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("confirm", request.confirm)
            put("draftVersion", request.draftVersion)
        }
        val obj = call("POST", "/api/hazard-alerts/$hazardAlertId/publish", body)
        HazardAlertParser.parsePublishAlertResult(obj)
    }

    /**
     * Fetch hazard alert: GET /api/hazard-alerts/{id}
     */
    suspend fun getHazardAlert(hazardAlertId: Long): HazardAlertDetail = withContext(Dispatchers.IO) {
        val obj = call("GET", "/api/hazard-alerts/$hazardAlertId")
        HazardAlertParser.parseHazardAlert(obj)
            ?: throw ApiException("Failed to parse hazard alert", statusCode = 500)
    }

    /**
     * Download authenticated photo bytes: GET /api/reports/{id}/photo
     */
    suspend fun getReportPhoto(reportId: Long): ByteArray = withContext(Dispatchers.IO) {
        callRawBytes("/api/reports/$reportId/photo", authenticated = true)
    }

    /**
     * Authority reporters directory: GET /api/users
     */
    suspend fun getReporters(): List<ReporterUser> = withContext(Dispatchers.IO) {
        val array = callArray("GET", "/api/users")
        val list = mutableListOf<ReporterUser>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ReporterUser(
                    id = obj.getLong("id"),
                    userId = obj.getString("userId"),
                    displayName = obj.optString("displayName", obj.getString("userId")),
                    role = obj.optString("role", "REPORTER"),
                    state = obj.optString("state", ""),
                    district = obj.optString("district", "")
                )
            )
        }
        list
    }

    /**
     * Authority create reporter: POST /api/users
     */
    suspend fun createReporter(request: CreateReporterRequest): ReporterUser = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("userId", request.userId)
            put("pin", request.pin)
            put("displayName", request.displayName)
            put("state", request.state)
            put("district", request.district)
        }
        val obj = call("POST", "/api/users", body)
        ReporterUser(
            id = obj.getLong("id"),
            userId = obj.getString("userId"),
            displayName = obj.optString("displayName", request.displayName),
            role = obj.optString("role", "REPORTER"),
            state = obj.optString("state", request.state),
            district = obj.optString("district", request.district)
        )
    }

    private fun parseReportDetailItem(obj: JSONObject): ReportDetailItem {
        return HazardAlertParser.parseReportDetailItem(obj)
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

    private suspend fun callMultipart(
        path: String,
        dataJson: JSONObject,
        photoBytes: ByteArray?,
        photoFilename: String = "report.jpg",
        authenticated: Boolean = true
    ): JSONObject = withContext(Dispatchers.IO) {
        val token = Session.token
        if (authenticated && token == null) {
            throw ApiException("Please sign in again.", unauthorized = true)
        }

        if (photoBytes == null || photoBytes.isEmpty()) {
            throw ApiException("Photographic evidence is required.")
        }

        val boundary = "Boundary-" + UUID.randomUUID().toString()
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        val conn = URL(BASE_URL + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000
            conn.doInput = true
            conn.doOutput = true
            conn.useCaches = false
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            if (authenticated && token != null) {
                conn.setRequestProperty("Authorization", "Bearer $token")
            }

            conn.outputStream.use { os ->
                // Part 1: photo (image/jpeg, filename: report.jpg)
                val photoHeader = buildString {
                    append(twoHyphens).append(boundary).append(lineEnd)
                    append("Content-Disposition: form-data; name=\"photo\"; filename=\"$photoFilename\"").append(lineEnd)
                    append("Content-Type: image/jpeg").append(lineEnd)
                    append(lineEnd)
                }
                os.write(photoHeader.toByteArray(Charsets.UTF_8))
                os.write(photoBytes)
                os.write(lineEnd.toByteArray(Charsets.UTF_8))

                // Part 2: data (MUST have Content-Type: application/json; charset=UTF-8)
                val dataHeader = buildString {
                    append(twoHyphens).append(boundary).append(lineEnd)
                    append("Content-Disposition: form-data; name=\"data\"").append(lineEnd)
                    append("Content-Type: application/json; charset=UTF-8").append(lineEnd)
                    append(lineEnd)
                }
                os.write(dataHeader.toByteArray(Charsets.UTF_8))
                os.write(dataJson.toString().toByteArray(Charsets.UTF_8))
                os.write(lineEnd.toByteArray(Charsets.UTF_8))

                // Closing boundary
                val closing = "$twoHyphens$boundary$twoHyphens$lineEnd"
                os.write(closing.toByteArray(Charsets.UTF_8))
                os.flush()
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
                    400 -> throw ApiException(serverMessage.ifBlank { "Invalid report data." }, statusCode = code)
                    401 -> throw ApiException(
                        if (authenticated) "Session expired. Please sign in again." else serverMessage.ifBlank { "Invalid credentials." },
                        unauthorized = authenticated,
                        statusCode = code
                    )
                    403 -> throw ApiException(serverMessage.ifBlank { "You do not have permission to perform this action." }, statusCode = code)
                    404 -> throw ApiException(serverMessage.ifBlank { "Resource not found (404)." }, statusCode = code)
                    409 -> throw ApiException(serverMessage.ifBlank { "Conflict (409)." }, statusCode = code)
                    415 -> throw ApiException(serverMessage.ifBlank { "Unsupported media type: expected multipart data part with application/json." }, statusCode = code)
                    else -> throw ApiException(serverMessage.ifBlank { "Server error ($code)." }, statusCode = code)
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: IOException) {
            throw ApiException(
                "Cannot reach the dispatch server. Check that the backend is running and 'adb reverse tcp:8080 tcp:8080' is active."
            )
        } catch (e: JSONException) {
            throw ApiException("Unexpected response from the dispatch server.")
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun callRawBytes(
        path: String,
        authenticated: Boolean = true
    ): ByteArray = withContext(Dispatchers.IO) {
        val token = Session.token
        if (authenticated && token == null) {
            throw ApiException("Please sign in again.", unauthorized = true)
        }

        val conn = URL(BASE_URL + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8_000
            conn.readTimeout = 30_000
            if (authenticated && token != null) {
                conn.setRequestProperty("Authorization", "Bearer $token")
            }

            val code = conn.responseCode
            if (code in 200..299) {
                conn.inputStream.use { it.readBytes() }
            } else {
                val text = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val serverMessage = try {
                    JSONObject(text).optString("error")
                } catch (e: JSONException) {
                    ""
                }
                when (code) {
                    401 -> throw ApiException(
                        if (authenticated) "Session expired. Please sign in again." else "Unauthorized.",
                        unauthorized = authenticated,
                        statusCode = code
                    )
                    403 -> throw ApiException(serverMessage.ifBlank { "You do not have permission to view this photo." }, statusCode = code)
                    404 -> throw ApiException(serverMessage.ifBlank { "Photo not found." }, statusCode = code)
                    else -> throw ApiException(serverMessage.ifBlank { "Server error ($code)." }, statusCode = code)
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: IOException) {
            throw ApiException("Cannot download photo. Server unreachable.")
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun callRaw(
        method: String,
        path: String,
        body: JSONObject? = null,
        authenticated: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val token = Session.token
        if (authenticated && token == null) {
            throw ApiException("Please sign in again.", unauthorized = true, statusCode = 401)
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
                    400 -> throw ApiException(serverMessage.ifBlank { "Bad request (400)." }, statusCode = code)
                    401 -> throw ApiException(
                        if (authenticated) "Session expired. Please sign in again." else serverMessage.ifBlank { "Invalid officer ID or PIN." },
                        unauthorized = authenticated,
                        statusCode = code
                    )
                    403 -> throw ApiException(serverMessage.ifBlank { "Access forbidden (403)." }, statusCode = code)
                    404 -> throw ApiException(serverMessage.ifBlank { "Not found (404)." }, statusCode = code)
                    409 -> throw ApiException(serverMessage.ifBlank { "Conflict (409)." }, statusCode = code)
                    415 -> throw ApiException(serverMessage.ifBlank { "Unsupported media type (415)." }, statusCode = code)
                    else -> throw ApiException(serverMessage.ifBlank { "Server error ($code)." }, statusCode = code)
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
