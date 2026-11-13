package com.rustbeatz.disasteralert.data.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.rustbeatz.disasteralert.data.network.ApiException
import com.rustbeatz.disasteralert.data.network.BackendApi
import com.rustbeatz.disasteralert.data.network.AppConfig

data class ReportMineItem(
    val id: Long,
    val reportCode: String,
    val category: String,
    val note: String?,
    val state: String,
    val district: String,
    val city: String?,
    val status: String,
    val createdAt: String,
    val linkedToReportCode: String? = null,
    val clarification: ReportMineClarification? = null,
    val clarificationText: String? = null,
    val alert: ReportMineAlert? = null
) {
    val categoryInfo: CategoryInfo
        get() = ReportCategories.get(category)

    val formattedTime: String
        get() = TimeUtils.formatUtcToIst(createdAt)

    val locationLabel: String
        get() = if (!city.isNullOrBlank()) "$city, $district" else "$district, $state"

    val questionText: String?
        get() = clarification?.question ?: clarificationText
}

data class ReportMineAlert(
    val id: Long,
    val alertCode: String,
    val severity: String,
    val publishedAt: String,
    val resolvedAt: String? = null
) {
    val formattedPublishedAt: String
        get() = TimeUtils.formatUtcToIst(publishedAt)

    val formattedResolvedAt: String?
        get() = resolvedAt?.let { TimeUtils.formatUtcToIst(it) }
}

data class ReportExif(
    val hasGps: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val takenAt: String?,
    val distanceToReportedM: Double?
)

data class ReportDetailItem(
    val id: Long,
    val reportCode: String,
    val category: String,
    val note: String?,
    val state: String,
    val district: String,
    val city: String?,
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Double?,
    val capturedAt: String,
    val mockLocation: Boolean,
    val exif: ReportExif?,
    val trustFlags: List<String>,
    val status: String,
    val analysisStatus: String,
    val reporterId: Long,
    val reporterName: String,
    val photoUrl: String?,
    val createdAt: String,
    val linkedToReportId: Long? = null,
    val photoBytes: ByteArray? = null,
    val analysis: ReportAnalysis? = null,
    val decisions: List<ReportDecisionItem> = emptyList(),
    val clarifications: List<ReportClarificationItem> = emptyList(),
    val hazardAlert: HazardAlertDetail? = null,
    val possibleDuplicates: List<Any> = emptyList()
) {
    val categoryInfo: CategoryInfo
        get() = ReportCategories.get(category)

    val formattedCreatedAt: String
        get() = TimeUtils.formatUtcToIst(createdAt)

    val formattedCapturedAt: String
        get() = TimeUtils.formatUtcToIst(capturedAt)

    val locationLabel: String
        get() = if (!city.isNullOrBlank()) "$city, $district, $state" else "$district, $state"
}

data class ReportCreateRequest(
    val clientReportId: String,
    val category: String,
    val note: String?,
    val state: String,
    val district: String,
    val city: String?,
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Double?,
    val capturedAt: String,
    val mockLocation: Boolean,
    val photoBytes: ByteArray? = null
)

data class ReportCreateResult(
    val id: Long,
    val reportCode: String,
    val status: String,
    val analysisStatus: String,
    val trustFlags: List<String>,
    val duplicate: Boolean,
    val createdAt: String
)

data class ReporterUser(
    val id: Long,
    val userId: String,
    val displayName: String,
    val role: String,
    val state: String,
    val district: String
)

data class CreateReporterRequest(
    val userId: String,
    val pin: String,
    val displayName: String,
    val state: String,
    val district: String
)

data class CategoryInfo(
    val id: String,
    val label: String,
    val emoji: String,
    val description: String
)

object ReportCategories {
    val ALL = listOf(
        CategoryInfo("ROAD_BLOCKED", "Road Blocked", "🚧", "Passage blocked by debris, obstruction or collapse"),
        CategoryInfo("ROAD_CLEARED", "Road Cleared", "🛣️", "Obstruction cleared, normal traffic resumed"),
        CategoryInfo("FLOODED_AREA", "Flooded Area", "🌊", "Waterlogging, submerged roads or rising waters"),
        CategoryInfo("LANDSLIDE", "Landslide", "⛰️", "Slope failure, rockfall or mudslide"),
        CategoryInfo("POWER_OUTAGE", "Power Outage", "⚡", "Grid failure, fallen poles or snapped wires"),
        CategoryInfo("FOOD_NEEDED", "Food Needed", "🍞", "Civilian food relief or ration distribution point"),
        CategoryInfo("WATER_NEEDED", "Water Needed", "💧", "Safe drinking water shortage or supply request"),
        CategoryInfo("MEDICAL_NEEDED", "Medical Needed", "💊", "First aid, emergency medicine or casualties"),
        CategoryInfo("SHELTER", "Shelter Needed", "🏠", "Displaced civilians, emergency camp requirement"),
        CategoryInfo("OTHER", "Other Hazard", "⚠️", "General hazard or situation requiring review")
    )

    fun get(id: String): CategoryInfo {
        return ALL.find { it.id.equals(id, ignoreCase = true) }
            ?: CategoryInfo(id, id.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }, "🚨", "")
    }
}

object ReportStatusUtils {
    fun getDisplayLabel(status: String): String = when (status.uppercase()) {
        "SUBMITTED" -> "Submitted"
        "PENDING_REVIEW" -> "Pending Review"
        "NEEDS_CLARIFICATION" -> "Needs Clarification"
        "VERIFIED" -> "Verified"
        "REJECTED" -> "Rejected"
        "ALERT_PUBLISHED" -> "Alert Published"
        "RESOLVED" -> "Resolved"
        "UNDER_REVIEW" -> "Under Review"
        "CLOSED" -> "Closed"
        else -> status
    }
}

interface ReportsRepository {
    suspend fun getMyReports(): List<ReportMineItem>
    suspend fun submitReport(request: ReportCreateRequest): ReportCreateResult
    suspend fun getAuthorityReports(district: String? = null, category: String? = null, status: String? = null): List<ReportDetailItem>
    suspend fun getReportDetail(reportId: Long): ReportDetailItem
    suspend fun retryAnalysis(reportId: Long): RetryAnalysisResult
    suspend fun submitDecision(reportId: Long, request: DecisionRequest): DecisionResult
    suspend fun saveHazardDraft(reportId: Long, request: HazardDraftRequest): HazardDraftResult
    suspend fun publishHazardAlert(hazardAlertId: Long, request: PublishAlertRequest): PublishAlertResult
    suspend fun getHazardAlert(hazardAlertId: Long): HazardAlertDetail
    suspend fun getReporters(): List<ReporterUser>
    suspend fun createReporter(request: CreateReporterRequest): ReporterUser
    suspend fun getReportPhoto(reportId: Long): ByteArray?
}

/**
 * Mock implementation of ReportsRepository for Slice S3a, S4a, and S4b.
 * Seeded with s4b-mock data and maintains live in-memory state.
 */
class MockReportsRepository : ReportsRepository {

    private val submittedClientIds = mutableMapOf<String, ReportCreateResult>()

    private val myReports = mutableListOf(
        ReportMineItem(
            id = 46,
            reportCode = "REPORT-00046",
            category = "WATER_NEEDED",
            note = "No drinking water",
            state = "Karnataka",
            district = "Udupi",
            city = "Brahmavar",
            status = "NEEDS_CLARIFICATION",
            createdAt = "2026-11-04T09:30:00Z",
            linkedToReportCode = null,
            clarification = ReportMineClarification("How many families are affected?", "2026-11-04T09:40:00Z"),
            alert = null
        ),
        ReportMineItem(
            id = 45,
            reportCode = "REPORT-00045",
            category = "POWER_OUTAGE",
            note = "Transformer down",
            state = "Karnataka",
            district = "Udupi",
            city = "Udupi",
            status = "PENDING_REVIEW",
            createdAt = "2026-11-04T09:10:00Z",
            linkedToReportCode = null,
            clarification = null,
            alert = null
        ),
        ReportMineItem(
            id = 41,
            reportCode = "REPORT-00041",
            category = "ROAD_BLOCKED",
            note = "Landslide across the road",
            state = "Karnataka",
            district = "Udupi",
            city = "Kollur",
            status = "ALERT_PUBLISHED",
            createdAt = "2026-11-04T08:10:20Z",
            linkedToReportCode = null,
            clarification = null,
            alert = ReportMineAlert(
                id = 9,
                alertCode = "HAZARD-00009",
                severity = "ORANGE",
                publishedAt = "2026-11-04T08:40:00Z",
                resolvedAt = null
            )
        ),
        ReportMineItem(
            id = 40,
            reportCode = "REPORT-00040",
            category = "ROAD_BLOCKED",
            note = "Debris on road",
            state = "Karnataka",
            district = "Udupi",
            city = "Kollur",
            status = "RESOLVED",
            createdAt = "2026-11-03T14:30:00Z",
            linkedToReportCode = null,
            clarification = null,
            alert = ReportMineAlert(
                id = 7,
                alertCode = "HAZARD-00007",
                severity = "YELLOW",
                publishedAt = "2026-11-03T15:00:00Z",
                resolvedAt = "2026-11-03T18:20:00Z"
            )
        ),
        ReportMineItem(
            id = 39,
            reportCode = "REPORT-00039",
            category = "FLOODED_AREA",
            note = "Water near bus stand",
            state = "Karnataka",
            district = "Udupi",
            city = "Udupi",
            status = "REJECTED",
            createdAt = "2026-11-03T07:02:44Z",
            linkedToReportCode = null,
            clarification = null,
            alert = null
        ),
        ReportMineItem(
            id = 38,
            reportCode = "REPORT-00038",
            category = "SHELTER",
            note = "School open as shelter",
            state = "Karnataka",
            district = "Udupi",
            city = "Kollur",
            status = "VERIFIED",
            createdAt = "2026-11-03T06:00:00Z",
            linkedToReportCode = null,
            clarification = null,
            alert = null
        )
    )

    private val authorityReports = mutableListOf(
        ReportDetailItem(
            id = 41,
            reportCode = "REPORT-00041",
            category = "ROAD_BLOCKED",
            note = "Landslide across the road near the bridge",
            state = "Karnataka",
            district = "Udupi",
            city = "Kollur",
            latitude = 13.8626,
            longitude = 74.8147,
            accuracyM = 12.5,
            capturedAt = "2026-11-04T08:10:12Z",
            mockLocation = false,
            exif = ReportExif(hasGps = false, latitude = null, longitude = null, takenAt = null, distanceToReportedM = null),
            trustFlags = listOf("EXIF_MISSING"),
            status = "PENDING_REVIEW",
            analysisStatus = "COMPLETED",
            reporterId = 5,
            reporterName = "Anil",
            photoUrl = "/api/reports/41/photo",
            createdAt = "2026-11-04T08:10:20Z",
            analysis = ReportAnalysis(
                status = "COMPLETED",
                sceneSummary = "Mud and rock across a two-lane road, no vehicles visible.",
                suggestedCategory = "LANDSLIDE",
                hazard = "Road blocked by landslide",
                severity = "ORANGE",
                consistency = "CONSISTENT",
                confidence = 0.78,
                caveats = listOf("Photo shows no scale", "Location not verified against EXIF"),
                draftAlert = DraftAlert(
                    title = "Road blocked near Kollur",
                    message = "A landslide is blocking the road near Kollur bridge.",
                    instructions = listOf("Do not use this road", "Use the alternate route via ...")
                ),
                model = "gemini-2.5-flash",
                promptVersion = "v1",
                analyzedAt = "2026-11-04T08:12:41Z"
            ),
            decisions = emptyList(),
            clarifications = emptyList(),
            hazardAlert = null
        ),
        ReportDetailItem(
            id = 46,
            reportCode = "REPORT-00046",
            category = "WATER_NEEDED",
            note = "No drinking water",
            state = "Karnataka",
            district = "Udupi",
            city = "Brahmavar",
            latitude = 13.4350,
            longitude = 74.7450,
            accuracyM = 10.0,
            capturedAt = "2026-11-04T09:30:00Z",
            mockLocation = false,
            exif = null,
            trustFlags = emptyList(),
            status = "NEEDS_CLARIFICATION",
            analysisStatus = "COMPLETED",
            reporterId = 5,
            reporterName = "Anil",
            photoUrl = "/api/reports/46/photo",
            createdAt = "2026-11-04T09:30:00Z",
            decisions = listOf(
                ReportDecisionItem(
                    id = 1,
                    decision = "REQUEST_CLARIFICATION",
                    note = "How many families are affected?",
                    decidedBy = "District Collector Udupi",
                    decidedAt = "2026-11-04T09:40:00Z"
                )
            ),
            clarifications = listOf(
                ReportClarificationItem(
                    id = 1,
                    question = "How many families are affected?",
                    askedBy = "District Collector Udupi",
                    askedAt = "2026-11-04T09:40:00Z",
                    answer = null,
                    answeredAt = null
                )
            ),
            hazardAlert = null
        ),
        ReportDetailItem(
            id = 47,
            reportCode = "REPORT-00047",
            category = "ROAD_BLOCKED",
            note = "Landslide across the road near the bridge",
            state = "Karnataka",
            district = "Udupi",
            city = "Kollur",
            latitude = 13.8626,
            longitude = 74.8147,
            accuracyM = 12.5,
            capturedAt = "2026-11-04T08:10:12Z",
            mockLocation = false,
            exif = null,
            trustFlags = emptyList(),
            status = "VERIFIED",
            analysisStatus = "COMPLETED",
            reporterId = 5,
            reporterName = "Anil",
            photoUrl = "/api/reports/47/photo",
            createdAt = "2026-11-04T08:10:20Z",
            decisions = listOf(
                ReportDecisionItem(
                    id = 1,
                    decision = "VERIFY",
                    note = "Confirmed with district control room",
                    decidedBy = "District Collector Udupi",
                    decidedAt = "2026-11-04T08:30:00Z"
                )
            ),
            clarifications = emptyList(),
            hazardAlert = null
        ),
        ReportDetailItem(
            id = 48,
            reportCode = "REPORT-00048",
            category = "ROAD_BLOCKED",
            note = "Landslide across the road near the bridge",
            state = "Karnataka",
            district = "Udupi",
            city = "Kollur",
            latitude = 13.8626,
            longitude = 74.8147,
            accuracyM = 12.5,
            capturedAt = "2026-11-04T08:10:12Z",
            mockLocation = false,
            exif = null,
            trustFlags = emptyList(),
            status = "VERIFIED",
            analysisStatus = "COMPLETED",
            reporterId = 5,
            reporterName = "Anil",
            photoUrl = "/api/reports/48/photo",
            createdAt = "2026-11-04T08:10:20Z",
            decisions = listOf(
                ReportDecisionItem(
                    id = 1,
                    decision = "VERIFY",
                    note = "Confirmed with district control room",
                    decidedBy = "District Collector Udupi",
                    decidedAt = "2026-11-04T08:30:00Z"
                )
            ),
            hazardAlert = HazardAlertDetail(
                id = 9,
                alertCode = "HAZARD-00009",
                status = "DRAFT",
                draftVersion = 3,
                hazardType = "Landslide",
                severity = "ORANGE",
                state = "Karnataka",
                district = "Udupi",
                city = "Kollur",
                title = "Road blocked near Kollur",
                message = "A landslide is blocking the road near Kollur bridge.",
                instructions = "Do not use this road.\nUse the alternate route via the coastal highway.",
                validUntil = null,
                audienceEstimate = 42,
                dispatchStatus = null,
                createdBy = "District Collector Udupi",
                approvedBy = null,
                publishedAt = null,
                resolvedAt = null,
                delivery = HazardAlertDelivery(0, 0, 0)
            )
        ),
        ReportDetailItem(
            id = 49,
            reportCode = "REPORT-00049",
            category = "ROAD_BLOCKED",
            note = "Landslide across the road near the bridge",
            state = "Karnataka",
            district = "Udupi",
            city = "Kollur",
            latitude = 13.8626,
            longitude = 74.8147,
            accuracyM = 12.5,
            capturedAt = "2026-11-04T08:10:12Z",
            mockLocation = false,
            exif = null,
            trustFlags = emptyList(),
            status = "ALERT_PUBLISHED",
            analysisStatus = "COMPLETED",
            reporterId = 5,
            reporterName = "Anil",
            photoUrl = "/api/reports/49/photo",
            createdAt = "2026-11-04T08:10:20Z",
            decisions = listOf(
                ReportDecisionItem(
                    id = 1,
                    decision = "VERIFY",
                    note = "Confirmed",
                    decidedBy = "District Collector Udupi",
                    decidedAt = "2026-11-04T08:30:00Z"
                )
            ),
            hazardAlert = HazardAlertDetail(
                id = 11,
                alertCode = "HAZARD-00011",
                status = "PUBLISHED",
                draftVersion = 3,
                hazardType = "Landslide",
                severity = "ORANGE",
                state = "Karnataka",
                district = "Udupi",
                city = "Kollur",
                title = "Road blocked near Kollur",
                message = "A landslide is blocking the road near Kollur bridge.",
                instructions = "Do not use this road.\nUse the alternate route via the coastal highway.",
                audienceEstimate = 42,
                dispatchStatus = "SENDING",
                createdBy = "District Collector Udupi",
                approvedBy = "District Collector Udupi",
                publishedAt = "2026-11-04T08:40:00Z",
                resolvedAt = null,
                delivery = HazardAlertDelivery(12, 0, 30)
            )
        ),
        ReportDetailItem(
            id = 50,
            reportCode = "REPORT-00050",
            category = "ROAD_BLOCKED",
            note = "Landslide across the road near the bridge",
            state = "Karnataka",
            district = "Udupi",
            city = "Kollur",
            latitude = 13.8626,
            longitude = 74.8147,
            accuracyM = 12.5,
            capturedAt = "2026-11-04T08:10:12Z",
            mockLocation = false,
            exif = null,
            trustFlags = emptyList(),
            status = "ALERT_PUBLISHED",
            analysisStatus = "COMPLETED",
            reporterId = 5,
            reporterName = "Anil",
            photoUrl = "/api/reports/50/photo",
            createdAt = "2026-11-04T08:10:20Z",
            decisions = listOf(
                ReportDecisionItem(
                    id = 1,
                    decision = "VERIFY",
                    note = "Confirmed",
                    decidedBy = "District Collector Udupi",
                    decidedAt = "2026-11-04T08:30:00Z"
                )
            ),
            hazardAlert = HazardAlertDetail(
                id = 10,
                alertCode = "HAZARD-00010",
                status = "PUBLISHED",
                draftVersion = 2,
                hazardType = "Landslide",
                severity = "ORANGE",
                state = "Karnataka",
                district = "Udupi",
                city = "Kollur",
                title = "Road blocked near Kollur",
                message = "A landslide is blocking the road near Kollur bridge.",
                instructions = "Do not use this road.",
                audienceEstimate = 42,
                dispatchStatus = "COMPLETED_WITH_FAILURES",
                createdBy = "District Collector Udupi",
                approvedBy = "District Collector Udupi",
                publishedAt = "2026-11-04T08:40:00Z",
                resolvedAt = null,
                delivery = HazardAlertDelivery(40, 2, 0)
            )
        ),
        ReportDetailItem(
            id = 40,
            reportCode = "REPORT-00040",
            category = "ROAD_BLOCKED",
            note = "Debris on road",
            state = "Karnataka",
            district = "Udupi",
            city = "Kollur",
            latitude = 13.8626,
            longitude = 74.8147,
            accuracyM = 12.5,
            capturedAt = "2026-11-03T14:30:00Z",
            mockLocation = false,
            exif = null,
            trustFlags = emptyList(),
            status = "RESOLVED",
            analysisStatus = "COMPLETED",
            reporterId = 5,
            reporterName = "Anil",
            photoUrl = "/api/reports/40/photo",
            createdAt = "2026-11-03T14:30:00Z",
            decisions = listOf(
                ReportDecisionItem(
                    id = 1,
                    decision = "VERIFY",
                    note = "Confirmed",
                    decidedBy = "District Collector Udupi",
                    decidedAt = "2026-11-03T14:50:00Z"
                ),
                ReportDecisionItem(
                    id = 2,
                    decision = "RESOLVE",
                    note = "Road reopened",
                    decidedBy = "District Collector Udupi",
                    decidedAt = "2026-11-03T18:20:00Z"
                )
            ),
            hazardAlert = HazardAlertDetail(
                id = 7,
                alertCode = "HAZARD-00007",
                status = "RESOLVED",
                draftVersion = 1,
                hazardType = "Road Blocked",
                severity = "YELLOW",
                state = "Karnataka",
                district = "Udupi",
                city = "Kollur",
                title = "Debris on road near Kollur",
                message = "Debris cleared, open for traffic.",
                instructions = "Drive safely.",
                audienceEstimate = 42,
                dispatchStatus = "COMPLETED",
                createdBy = "District Collector Udupi",
                approvedBy = "District Collector Udupi",
                publishedAt = "2026-11-03T15:00:00Z",
                resolvedAt = "2026-11-03T18:20:00Z",
                delivery = HazardAlertDelivery(42, 0, 0)
            )
        ),
        ReportDetailItem(
            id = 39,
            reportCode = "REPORT-00039",
            category = "FLOODED_AREA",
            note = "Water near bus stand",
            state = "Karnataka",
            district = "Udupi",
            city = "Udupi",
            latitude = 13.3400,
            longitude = 74.7400,
            accuracyM = 15.0,
            capturedAt = "2026-11-03T07:02:44Z",
            mockLocation = false,
            exif = null,
            trustFlags = emptyList(),
            status = "REJECTED",
            analysisStatus = "COMPLETED",
            reporterId = 5,
            reporterName = "Anil",
            photoUrl = "/api/reports/39/photo",
            createdAt = "2026-11-03T07:02:44Z",
            decisions = listOf(
                ReportDecisionItem(
                    id = 1,
                    decision = "REJECT",
                    note = "Photo does not match the location",
                    decidedBy = "District Collector Udupi",
                    decidedAt = "2026-11-03T07:20:00Z"
                )
            ),
            hazardAlert = null
        )
    )

    private val reportersList = mutableListOf(
        ReporterUser(5L, "anil.rep", "Anil", "REPORTER", "Karnataka", "Udupi"),
        ReporterUser(6L, "meena.rep", "Meena", "REPORTER", "Karnataka", "Udupi")
    )

    override suspend fun getMyReports(): List<ReportMineItem> = withContext(Dispatchers.IO) {
        myReports.toList()
    }

    override suspend fun submitReport(request: ReportCreateRequest): ReportCreateResult = withContext(Dispatchers.IO) {
        val existing = submittedClientIds[request.clientReportId]
        if (existing != null) {
            return@withContext existing.copy(duplicate = true)
        }

        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val newId = (authorityReports.maxOfOrNull { it.id } ?: 40L) + 1L
        val reportCode = "REPORT-%05d".format(newId)

        val result = ReportCreateResult(
            id = newId,
            reportCode = reportCode,
            status = "SUBMITTED",
            analysisStatus = "QUEUED",
            trustFlags = if (request.mockLocation) listOf("MOCK_LOCATION") else emptyList(),
            duplicate = false,
            createdAt = nowIso
        )
        submittedClientIds[request.clientReportId] = result

        val newDetail = ReportDetailItem(
            id = newId,
            reportCode = reportCode,
            category = request.category,
            note = request.note,
            state = request.state,
            district = request.district,
            city = request.city,
            latitude = request.latitude,
            longitude = request.longitude,
            accuracyM = request.accuracyM,
            capturedAt = request.capturedAt,
            mockLocation = request.mockLocation,
            exif = null,
            trustFlags = if (request.mockLocation) listOf("MOCK_LOCATION") else emptyList(),
            status = "SUBMITTED",
            analysisStatus = "QUEUED",
            reporterId = 5L,
            reporterName = "Anil",
            photoUrl = "/api/reports/$newId/photo",
            createdAt = nowIso,
            photoBytes = request.photoBytes,
            analysis = ReportAnalysis(status = "QUEUED"),
            decisions = emptyList(),
            clarifications = emptyList(),
            hazardAlert = null
        )
        authorityReports.add(0, newDetail)

        val newMine = ReportMineItem(
            id = newId,
            reportCode = reportCode,
            category = request.category,
            note = request.note,
            state = request.state,
            district = request.district,
            city = request.city,
            status = "SUBMITTED",
            createdAt = nowIso,
            clarification = null,
            alert = null
        )
        myReports.add(0, newMine)

        result
    }

    override suspend fun getAuthorityReports(
        district: String?,
        category: String?,
        status: String?
    ): List<ReportDetailItem> = withContext(Dispatchers.IO) {
        var list = authorityReports.toList()
        if (!district.isNullOrBlank()) {
            list = list.filter { it.district.equals(district, ignoreCase = true) }
        }
        if (!category.isNullOrBlank()) {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }
        if (!status.isNullOrBlank()) {
            list = list.filter { it.status.equals(status, ignoreCase = true) }
            if (status.equals("PENDING_REVIEW", ignoreCase = true)) {
                // Pending review queue: newest first
                list = list.sortedByDescending { it.createdAt }
            }
        }
        list
    }

    override suspend fun getReportDetail(reportId: Long): ReportDetailItem = withContext(Dispatchers.IO) {
        val report = authorityReports.find { it.id == reportId }
            ?: throw ApiException("Report not found", statusCode = 404)
        report
    }

    override suspend fun retryAnalysis(reportId: Long): RetryAnalysisResult = withContext(Dispatchers.IO) {
        val index = authorityReports.indexOfFirst { it.id == reportId }
        if (index == -1) throw ApiException("Report not found", statusCode = 404)
        val report = authorityReports[index]
        val updated = report.copy(
            analysisStatus = "QUEUED",
            analysis = ReportAnalysis(status = "QUEUED")
        )
        authorityReports[index] = updated
        RetryAnalysisResult(
            reportId = reportId,
            analysisStatus = "QUEUED"
        )
    }

    override suspend fun submitDecision(reportId: Long, request: DecisionRequest): DecisionResult = withContext(Dispatchers.IO) {
        val index = authorityReports.indexOfFirst { it.id == reportId }
        if (index == -1) throw ApiException("Report not found", statusCode = 404)
        val report = authorityReports[index]

        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val newStatus = when (request.decision.uppercase()) {
            "VERIFY" -> {
                if (report.status != "PENDING_REVIEW") {
                    throw ApiException("Report cannot be verified from status ${report.status}", statusCode = 409)
                }
                "VERIFIED"
            }
            "REJECT" -> {
                if (report.status != "PENDING_REVIEW") {
                    throw ApiException("Report cannot be rejected from status ${report.status}", statusCode = 409)
                }
                if (request.note.isNullOrBlank()) {
                    throw ApiException("Reject reason note is required", statusCode = 400)
                }
                "REJECTED"
            }
            "REQUEST_CLARIFICATION" -> {
                if (report.status != "PENDING_REVIEW") {
                    throw ApiException("Cannot request clarification from status ${report.status}", statusCode = 409)
                }
                if (request.note.isNullOrBlank()) {
                    throw ApiException("Question note is required", statusCode = 400)
                }
                "NEEDS_CLARIFICATION"
            }
            "RESOLVE" -> {
                if (report.status != "VERIFIED" && report.status != "ALERT_PUBLISHED") {
                    throw ApiException("Report cannot be resolved from status ${report.status}", statusCode = 409)
                }
                "RESOLVED"
            }
            else -> throw ApiException("Unknown decision ${request.decision}", statusCode = 400)
        }

        val newDecisions = report.decisions.toMutableList()
        val newDecisionId = (newDecisions.maxOfOrNull { it.id } ?: 0L) + 1L
        newDecisions.add(
            ReportDecisionItem(
                id = newDecisionId,
                decision = request.decision.uppercase(),
                note = request.note,
                decidedBy = "District Collector Udupi",
                decidedAt = nowIso
            )
        )

        val newClarifications = report.clarifications.toMutableList()
        if (request.decision.equals("REQUEST_CLARIFICATION", ignoreCase = true)) {
            val newClarId = (newClarifications.maxOfOrNull { it.id } ?: 0L) + 1L
            newClarifications.add(
                ReportClarificationItem(
                    id = newClarId,
                    question = request.note ?: "",
                    askedBy = "District Collector Udupi",
                    askedAt = nowIso,
                    answer = null,
                    answeredAt = null
                )
            )
        }

        val updatedHazardAlert = if (newStatus == "RESOLVED" && report.hazardAlert != null) {
            report.hazardAlert.copy(status = "RESOLVED", dispatchStatus = "COMPLETED", resolvedAt = nowIso)
        } else report.hazardAlert

        val updatedReport = report.copy(
            status = newStatus,
            decisions = newDecisions,
            clarifications = newClarifications,
            hazardAlert = updatedHazardAlert
        )
        authorityReports[index] = updatedReport

        val mineIndex = myReports.indexOfFirst { it.id == reportId }
        if (mineIndex != -1) {
            val clar = if (request.decision.equals("REQUEST_CLARIFICATION", ignoreCase = true)) {
                ReportMineClarification(request.note ?: "", nowIso)
            } else myReports[mineIndex].clarification
            myReports[mineIndex] = myReports[mineIndex].copy(status = newStatus, clarification = clar)
        }

        DecisionResult(reportId = reportId, status = newStatus)
    }

    override suspend fun saveHazardDraft(reportId: Long, request: HazardDraftRequest): HazardDraftResult = withContext(Dispatchers.IO) {
        val index = authorityReports.indexOfFirst { it.id == reportId }
        if (index == -1) throw ApiException("Report not found", statusCode = 404)
        val report = authorityReports[index]

        if (!report.status.equals("VERIFIED", ignoreCase = true)) {
            throw ApiException("A draft needs a VERIFIED report", statusCode = 409)
        }

        val validation = HazardDraftValidator.validate(
            hazardType = request.hazardType,
            severity = request.severity,
            title = request.title,
            message = request.message,
            instructions = request.instructions
        )
        if (!validation.isValid) {
            throw ApiException(validation.firstError ?: "Validation failed", statusCode = 400)
        }

        val currentDraft = report.hazardAlert
        val nextVersion = (currentDraft?.draftVersion ?: 0) + 1
        val alertId = currentDraft?.id ?: 9L
        val alertCode = currentDraft?.alertCode ?: "HAZARD-%05d".format(alertId)
        val audienceEst = 42

        val newDraft = HazardAlertDetail(
            id = alertId,
            alertCode = alertCode,
            status = "DRAFT",
            draftVersion = nextVersion,
            hazardType = request.hazardType,
            severity = request.severity,
            state = request.state,
            district = request.district,
            city = request.city,
            title = request.title,
            message = request.message,
            instructions = request.instructions,
            validUntil = request.validUntil,
            audienceEstimate = audienceEst,
            dispatchStatus = null,
            createdBy = "District Collector Udupi",
            approvedBy = null,
            publishedAt = null,
            resolvedAt = null,
            delivery = HazardAlertDelivery(0, 0, 0)
        )

        authorityReports[index] = report.copy(hazardAlert = newDraft)

        HazardDraftResult(
            id = alertId,
            alertCode = alertCode,
            status = "DRAFT",
            draftVersion = nextVersion,
            audienceEstimate = audienceEst
        )
    }

    override suspend fun publishHazardAlert(hazardAlertId: Long, request: PublishAlertRequest): PublishAlertResult = withContext(Dispatchers.IO) {
        if (!request.confirm) {
            throw ApiException("confirm must be true", statusCode = 400)
        }

        val reportIndex = authorityReports.indexOfFirst { it.hazardAlert?.id == hazardAlertId }
        val report = if (reportIndex != -1) authorityReports[reportIndex] else null
        val alert = report?.hazardAlert

        if (alert != null && (alert.status == "PUBLISHED" || alert.status == "RESOLVED")) {
            return@withContext PublishAlertResult(
                id = hazardAlertId,
                status = "PUBLISHED",
                dispatchStatus = alert.dispatchStatus ?: "COMPLETED",
                publishedAt = alert.publishedAt,
                alreadyPublished = true
            )
        }

        if (alert != null && alert.draftVersion != request.draftVersion) {
            throw ApiException(
                "The draft changed after you opened it. Review the latest version and publish again",
                statusCode = 409
            )
        }

        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val updatedAlert = (alert ?: HazardAlertDetail(
            id = hazardAlertId,
            alertCode = "HAZARD-%05d".format(hazardAlertId),
            status = "PUBLISHED",
            draftVersion = request.draftVersion,
            hazardType = "Landslide",
            severity = "ORANGE",
            state = "Karnataka",
            district = "Udupi",
            city = "Kollur",
            title = "Hazard Alert",
            message = "Alert published to citizens",
            instructions = null,
            audienceEstimate = 42
        )).copy(
            status = "PUBLISHED",
            dispatchStatus = "SENDING",
            approvedBy = "District Collector Udupi",
            publishedAt = nowIso,
            delivery = HazardAlertDelivery(sent = 12, failed = 0, pending = 30)
        )

        if (reportIndex != -1) {
            authorityReports[reportIndex] = report!!.copy(
                status = "ALERT_PUBLISHED",
                hazardAlert = updatedAlert
            )
            val mineIndex = myReports.indexOfFirst { it.id == report.id }
            if (mineIndex != -1) {
                myReports[mineIndex] = myReports[mineIndex].copy(
                    status = "ALERT_PUBLISHED",
                    alert = ReportMineAlert(
                        id = updatedAlert.id,
                        alertCode = updatedAlert.alertCode,
                        severity = updatedAlert.severity,
                        publishedAt = nowIso,
                        resolvedAt = null
                    )
                )
            }
        }

        PublishAlertResult(
            id = hazardAlertId,
            status = "PUBLISHED",
            dispatchStatus = "SENDING",
            publishedAt = nowIso,
            alreadyPublished = false
        )
    }

    override suspend fun getHazardAlert(hazardAlertId: Long): HazardAlertDetail = withContext(Dispatchers.IO) {
        val alert = authorityReports.mapNotNull { it.hazardAlert }.find { it.id == hazardAlertId }
            ?: HazardAlertDetail(
                id = hazardAlertId,
                alertCode = "HAZARD-%05d".format(hazardAlertId),
                status = "PUBLISHED",
                draftVersion = 3,
                hazardType = "Landslide",
                severity = "ORANGE",
                state = "Karnataka",
                district = "Udupi",
                city = "Kollur",
                title = "Road blocked near Kollur",
                message = "A landslide is blocking the road near Kollur bridge.",
                instructions = "Do not use this road.",
                validUntil = null,
                audienceEstimate = 42,
                dispatchStatus = "SENDING",
                createdBy = "District Collector Udupi",
                approvedBy = "District Collector Udupi",
                publishedAt = "2026-11-04T08:40:00Z",
                resolvedAt = null,
                delivery = HazardAlertDelivery(sent = 12, failed = 0, pending = 30)
            )
        alert
    }

    override suspend fun getReporters(): List<ReporterUser> = withContext(Dispatchers.IO) {
        reportersList.toList()
    }

    override suspend fun createReporter(request: CreateReporterRequest): ReporterUser = withContext(Dispatchers.IO) {
        if (reportersList.any { it.userId.equals(request.userId.trim(), ignoreCase = true) } ||
            request.userId.equals("asha.dc", ignoreCase = true) ||
            request.userId.equals("ravi.sdma", ignoreCase = true)) {
            throw ApiException("User ID '${request.userId}' already exists.")
        }

        val newId = (reportersList.maxOfOrNull { it.id } ?: 6L) + 1L
        val newUser = ReporterUser(
            id = newId,
            userId = request.userId.trim(),
            displayName = request.displayName.trim(),
            role = "REPORTER",
            state = request.state.trim(),
            district = request.district.trim()
        )
        reportersList.add(newUser)
        newUser
    }

    override suspend fun getReportPhoto(reportId: Long): ByteArray? = withContext(Dispatchers.IO) {
        authorityReports.find { it.id == reportId }?.photoBytes
    }
}

/**
 * Real network repository backed by BackendApi (Spring Boot REST).
 * When AppConfig.USE_MOCK_REPORTS is true, delegates directly to mockFallback.
 */
class NetworkReportsRepository(
    private val mockFallback: ReportsRepository = MockReportsRepository()
) : ReportsRepository {

    override suspend fun getMyReports(): List<ReportMineItem> {
        if (AppConfig.USE_MOCK_REPORTS) {
            return mockFallback.getMyReports()
        }
        return BackendApi.getMyReports()
    }

    override suspend fun submitReport(request: ReportCreateRequest): ReportCreateResult {
        if (AppConfig.USE_MOCK_REPORTS) {
            return mockFallback.submitReport(request)
        }
        return BackendApi.submitReport(request)
    }

    override suspend fun getAuthorityReports(
        district: String?,
        category: String?,
        status: String?
    ): List<ReportDetailItem> {
        if (AppConfig.USE_MOCK_REPORTS) {
            return mockFallback.getAuthorityReports(district, category, status)
        }
        return BackendApi.getAuthorityReports(district = district, category = category, status = status)
    }

    override suspend fun getReportDetail(reportId: Long): ReportDetailItem {
        if (AppConfig.USE_MOCK_REPORTS) {
            return mockFallback.getReportDetail(reportId)
        }
        val detail = BackendApi.getReportDetail(reportId)
        val photoBytes = try {
            BackendApi.getReportPhoto(reportId)
        } catch (_: Exception) {
            null
        }
        return detail.copy(photoBytes = photoBytes)
    }

    override suspend fun retryAnalysis(reportId: Long): RetryAnalysisResult {
        if (AppConfig.USE_MOCK_REPORTS) {
            return mockFallback.retryAnalysis(reportId)
        }
        return BackendApi.retryAnalysis(reportId)
    }

    override suspend fun submitDecision(reportId: Long, request: DecisionRequest): DecisionResult {
        if (AppConfig.USE_MOCK_REPORTS) {
            return mockFallback.submitDecision(reportId, request)
        }
        return BackendApi.submitDecision(reportId, request)
    }

    override suspend fun saveHazardDraft(reportId: Long, request: HazardDraftRequest): HazardDraftResult {
        if (AppConfig.USE_MOCK_REPORTS) {
            return mockFallback.saveHazardDraft(reportId, request)
        }
        return BackendApi.saveHazardDraft(reportId, request)
    }

    override suspend fun publishHazardAlert(hazardAlertId: Long, request: PublishAlertRequest): PublishAlertResult {
        if (AppConfig.USE_MOCK_REPORTS) {
            return mockFallback.publishHazardAlert(hazardAlertId, request)
        }
        return BackendApi.publishHazardAlert(hazardAlertId, request)
    }

    override suspend fun getHazardAlert(hazardAlertId: Long): HazardAlertDetail {
        if (AppConfig.USE_MOCK_REPORTS) {
            return mockFallback.getHazardAlert(hazardAlertId)
        }
        return BackendApi.getHazardAlert(hazardAlertId)
    }

    override suspend fun getReporters(): List<ReporterUser> {
        if (AppConfig.USE_MOCK_REPORTS) {
            return mockFallback.getReporters()
        }
        return BackendApi.getReporters()
    }

    override suspend fun createReporter(request: CreateReporterRequest): ReporterUser {
        if (AppConfig.USE_MOCK_REPORTS) {
            return mockFallback.createReporter(request)
        }
        return BackendApi.createReporter(request)
    }

    override suspend fun getReportPhoto(reportId: Long): ByteArray? {
        if (AppConfig.USE_MOCK_REPORTS) {
            return mockFallback.getReportPhoto(reportId)
        }
        return try {
            BackendApi.getReportPhoto(reportId)
        } catch (_: Exception) {
            null
        }
    }
}
