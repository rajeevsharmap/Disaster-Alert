package com.rustbeatz.disasteralert.data.repo

import androidx.compose.ui.graphics.Color
import com.rustbeatz.disasteralert.ui.theme.EmergencyBlue
import com.rustbeatz.disasteralert.ui.theme.EmergencyBlueContainerLight
import com.rustbeatz.disasteralert.ui.theme.EmergencyOrange
import com.rustbeatz.disasteralert.ui.theme.EmergencyOrangeContainerLight
import com.rustbeatz.disasteralert.ui.theme.EmergencyRed
import com.rustbeatz.disasteralert.ui.theme.EmergencyRedContainerLight
import com.rustbeatz.disasteralert.ui.theme.EmergencyYellow
import com.rustbeatz.disasteralert.ui.theme.EmergencyYellowContainerLight
import com.rustbeatz.disasteralert.ui.theme.ForestGreenPrimaryLight
import com.rustbeatz.disasteralert.ui.theme.SoftMintContainerLight
import kotlinx.coroutines.delay

/**
 * Draft alert embedded in an AI analysis result (read-only draft suggestion).
 */
data class DraftAlert(
    val title: String? = null,
    val message: String? = null,
    val instructions: List<String> = emptyList()
)

/**
 * The "analysis" object from GET /api/reports/{id}.
 * Stored AI analysis result. All fields except status are optional-safe.
 */
data class ReportAnalysis(
    val status: String, // "QUEUED", "RUNNING", "COMPLETED", "FAILED", "SKIPPED"
    val sceneSummary: String? = null,
    val suggestedCategory: String? = null,
    val hazard: String? = null,
    val severity: String? = null, // "RED", "ORANGE", "YELLOW", "GREEN"
    val consistency: String? = null, // "CONSISTENT", "PARTIAL", "INCONSISTENT", "UNCLEAR"
    val confidence: Double? = null,
    val caveats: List<String> = emptyList(),
    val draftAlert: DraftAlert? = null,
    val model: String? = null,
    val promptVersion: String? = null,
    val analyzedAt: String? = null,
    val error: String? = null
) {
    val isSimulated: Boolean
        get() = sceneSummary?.trimStart()?.startsWith("SIMULATED", ignoreCase = true) == true

    val formattedAnalyzedAt: String?
        get() = analyzedAt?.let { TimeUtils.formatUtcToIst(it) }

    val confidencePercentage: Int?
        get() = confidence?.let { (it * 100).toInt() }
}

data class RetryAnalysisResult(
    val reportId: Long,
    val analysisStatus: String,
    val alreadyRunning: Boolean = false
)

enum class ConsistencyChipStyle {
    NORMAL,
    WARNING
}

object AnalysisChipUtils {
    fun getConsistencyStyle(consistency: String?): ConsistencyChipStyle {
        return when (consistency?.uppercase()) {
            "INCONSISTENT", "UNCLEAR" -> ConsistencyChipStyle.WARNING
            else -> ConsistencyChipStyle.NORMAL
        }
    }

    fun getConsistencyLabel(consistency: String?): String {
        return when (consistency?.uppercase()) {
            "CONSISTENT" -> "Consistent"
            "PARTIAL" -> "Partial"
            "INCONSISTENT" -> "Inconsistent"
            "UNCLEAR" -> "Unclear"
            null -> "Unknown"
            else -> consistency
        }
    }
}

object SeverityChipUtils {
    data class SeverityColors(
        val textColor: Color,
        val containerColor: Color
    )

    fun getSeverityColors(severity: String?): SeverityColors {
        return when (severity?.uppercase()) {
            "RED" -> SeverityColors(EmergencyRed, EmergencyRedContainerLight)
            "ORANGE" -> SeverityColors(EmergencyOrange, EmergencyOrangeContainerLight)
            "YELLOW" -> SeverityColors(EmergencyYellow, EmergencyYellowContainerLight)
            "GREEN" -> SeverityColors(ForestGreenPrimaryLight, SoftMintContainerLight)
            else -> SeverityColors(Color(0xFF4B5563), Color(0xFFE5E7EB))
        }
    }
}

object AnalysisRefreshController {
    const val REFRESH_INTERVAL_MS = 3000L

    fun isPollingStatus(status: String?): Boolean {
        return status.equals("QUEUED", ignoreCase = true) ||
                status.equals("RUNNING", ignoreCase = true)
    }

    fun isTerminalStatus(status: String?): Boolean {
        return !isPollingStatus(status)
    }

    /**
     * Executes polling loop until a terminal status is returned or cancelled.
     * Used for unit testing the 3s refresh logic stops on a terminal status.
     */
    suspend fun pollUntilTerminal(
        initialStatus: String?,
        intervalMs: Long = REFRESH_INTERVAL_MS,
        fetchStatus: suspend () -> String?
    ): String? {
        var currentStatus = initialStatus
        while (isPollingStatus(currentStatus)) {
            delay(intervalMs)
            currentStatus = fetchStatus()
        }
        return currentStatus
    }
}
