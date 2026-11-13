package com.rustbeatz.disasteralert.data.repo

import com.rustbeatz.disasteralert.ui.theme.EmergencyBlue
import com.rustbeatz.disasteralert.ui.theme.EmergencyOrange
import com.rustbeatz.disasteralert.ui.theme.EmergencyRed
import com.rustbeatz.disasteralert.ui.theme.EmergencyYellow
import com.rustbeatz.disasteralert.ui.theme.ForestGreenPrimaryLight
import androidx.compose.ui.graphics.Color

/**
 * Record of an authority officer's decision on a report.
 * POST /api/reports/{id}/decision
 */
data class ReportDecisionItem(
    val id: Long,
    val decision: String, // "VERIFY", "REJECT", "REQUEST_CLARIFICATION", "RESOLVE"
    val note: String?,
    val decidedBy: String?,
    val decidedAt: String?
) {
    val formattedDecidedAt: String
        get() = decidedAt?.let { TimeUtils.formatUtcToIst(it) } ?: ""

    val isVerify: Boolean get() = decision.equals("VERIFY", ignoreCase = true)
    val isReject: Boolean get() = decision.equals("REJECT", ignoreCase = true)
    val isClarification: Boolean get() = decision.equals("REQUEST_CLARIFICATION", ignoreCase = true)
    val isResolve: Boolean get() = decision.equals("RESOLVE", ignoreCase = true)
}

/**
 * Record of a question asked to the reporter and their answer.
 */
data class ReportClarificationItem(
    val id: Long,
    val question: String,
    val askedBy: String?,
    val askedAt: String?,
    val answer: String?,
    val answeredAt: String?
) {
    val formattedAskedAt: String
        get() = askedAt?.let { TimeUtils.formatUtcToIst(it) } ?: ""

    val formattedAnsweredAt: String?
        get() = answeredAt?.let { TimeUtils.formatUtcToIst(it) }

    val isAnswered: Boolean
        get() = !answer.isNullOrBlank()
}

/**
 * Citizen Telegram delivery progress summary.
 */
data class HazardAlertDelivery(
    val sent: Int,
    val failed: Int,
    val pending: Int
) {
    val total: Int get() = sent + failed + pending
}

/**
 * Full hazard alert model (draft or published) attached to a report.
 * Returned in report detail or GET /api/hazard-alerts/{id}.
 */
data class HazardAlertDetail(
    val id: Long,
    val alertCode: String,
    val status: String, // "DRAFT", "PUBLISHED", "RESOLVED", "WITHDRAWN"
    val draftVersion: Int,
    val hazardType: String,
    val severity: String, // "RED", "ORANGE", "YELLOW", "GREEN"
    val state: String,
    val district: String,
    val city: String?,
    val title: String,
    val message: String,
    val instructions: String?,
    val validUntil: String? = null,
    val audienceEstimate: Int = 0,
    val dispatchStatus: String? = null, // "SENDING", "COMPLETED", "COMPLETED_WITH_FAILURES", null
    val createdBy: String? = null,
    val approvedBy: String? = null,
    val publishedAt: String? = null,
    val resolvedAt: String? = null,
    val delivery: HazardAlertDelivery? = null
) {
    val isDraft: Boolean get() = status.equals("DRAFT", ignoreCase = true)
    val isPublished: Boolean get() = status.equals("PUBLISHED", ignoreCase = true)
    val isResolved: Boolean get() = status.equals("RESOLVED", ignoreCase = true)

    val formattedPublishedAt: String?
        get() = publishedAt?.let { TimeUtils.formatUtcToIst(it) }

    val formattedResolvedAt: String?
        get() = resolvedAt?.let { TimeUtils.formatUtcToIst(it) }
}

/**
 * Decision request payload for POST /api/reports/{id}/decision.
 */
data class DecisionRequest(
    val decision: String, // "VERIFY", "REJECT", "REQUEST_CLARIFICATION", "RESOLVE"
    val note: String? = null
)

/**
 * Decision response payload.
 */
data class DecisionResult(
    val reportId: Long,
    val status: String
)

/**
 * Hazard draft request for PUT /api/reports/{id}/hazard-alert.
 */
data class HazardDraftRequest(
    val hazardType: String,
    val severity: String,
    val state: String,
    val district: String,
    val city: String?,
    val title: String,
    val message: String,
    val instructions: String?,
    val validUntil: String? = null
)

/**
 * Hazard draft response.
 */
data class HazardDraftResult(
    val id: Long,
    val alertCode: String,
    val status: String,
    val draftVersion: Int,
    val audienceEstimate: Int
)

/**
 * Request payload for POST /api/hazard-alerts/{id}/publish.
 */
data class PublishAlertRequest(
    val confirm: Boolean = true,
    val draftVersion: Int
)

/**
 * Response payload for POST /api/hazard-alerts/{id}/publish.
 */
data class PublishAlertResult(
    val id: Long,
    val status: String,
    val dispatchStatus: String?,
    val publishedAt: String?,
    val alreadyPublished: Boolean = false
)

/**
 * Clarification item in reporter's History (GET /api/reports/mine).
 */
data class ReportMineClarification(
    val question: String,
    val askedAt: String
) {
    val formattedAskedAt: String
        get() = TimeUtils.formatUtcToIst(askedAt)
}

/**
 * Validation rules and limits for hazard alert draft editor:
 * - hazardType: 1-60 chars
 * - severity: RED, ORANGE, YELLOW, GREEN
 * - title: 1-120 chars
 * - message: 1-1000 chars
 * - instructions: one string, one line per bullet, max 1000 chars
 */
object HazardDraftValidator {
    data class ValidationResult(
        val isValid: Boolean,
        val hazardTypeError: String? = null,
        val severityError: String? = null,
        val titleError: String? = null,
        val messageError: String? = null,
        val instructionsError: String? = null
    ) {
        val firstError: String?
            get() = hazardTypeError ?: severityError ?: titleError ?: messageError ?: instructionsError
    }

    fun validate(
        hazardType: String,
        severity: String,
        title: String,
        message: String,
        instructions: String?
    ): ValidationResult {
        val trimmedHazard = hazardType.trim()
        val trimmedTitle = title.trim()
        val trimmedMessage = message.trim()
        val instr = instructions ?: ""

        val hazardError = when {
            trimmedHazard.isEmpty() -> "Hazard type is required (1-60 characters)"
            trimmedHazard.length > 60 -> "Hazard type must be 1-60 characters"
            else -> null
        }

        val severityError = when {
            severity.uppercase() !in listOf("RED", "ORANGE", "YELLOW", "GREEN") -> "Severity must be RED, ORANGE, YELLOW or GREEN"
            else -> null
        }

        val titleError = when {
            trimmedTitle.isEmpty() -> "Title is required (1-120 characters)"
            trimmedTitle.length > 120 -> "Title must be 1-120 characters"
            else -> null
        }

        val messageError = when {
            trimmedMessage.isEmpty() -> "Message is required (1-1000 characters)"
            trimmedMessage.length > 1000 -> "Message must be 1-1000 characters"
            else -> null
        }

        val instructionsError = when {
            instr.length > 1000 -> "Instructions must be at most 1000 characters"
            else -> null
        }

        val isValid = hazardError == null && severityError == null && titleError == null && messageError == null && instructionsError == null
        return ValidationResult(
            isValid = isValid,
            hazardTypeError = hazardError,
            severityError = severityError,
            titleError = titleError,
            messageError = messageError,
            instructionsError = instructionsError
        )
    }
}

/**
 * Status and action rules as defined in section 14.2 & 14.5 of PROJECT_HANDOFF.md.
 */
object ReportActionRules {
    enum class AuthorityAction {
        VERIFY,
        REJECT,
        ASK_QUESTION,
        CREATE_DRAFT,
        PUBLISH_ALERT,
        CLOSE_WITHOUT_ALERT,
        MARK_RESOLVED
    }

    fun getAllowedActions(status: String, hasDraft: Boolean = false): Set<AuthorityAction> {
        return when (status.uppercase()) {
            "PENDING_REVIEW" -> setOf(
                AuthorityAction.VERIFY,
                AuthorityAction.REJECT,
                AuthorityAction.ASK_QUESTION
            )
            "VERIFIED" -> {
                if (hasDraft) {
                    setOf(
                        AuthorityAction.CREATE_DRAFT,
                        AuthorityAction.PUBLISH_ALERT,
                        AuthorityAction.CLOSE_WITHOUT_ALERT
                    )
                } else {
                    setOf(
                        AuthorityAction.CREATE_DRAFT,
                        AuthorityAction.CLOSE_WITHOUT_ALERT
                    )
                }
            }
            "ALERT_PUBLISHED" -> setOf(
                AuthorityAction.MARK_RESOLVED
            )
            "NEEDS_CLARIFICATION", "RESOLVED", "REJECTED", "SUBMITTED" -> emptySet()
            else -> emptySet()
        }
    }

    fun isReadOnly(status: String): Boolean = when (status.uppercase()) {
        "REJECTED", "RESOLVED" -> true
        else -> false
    }

    fun getStatusColor(status: String): Color = when (status.uppercase()) {
        "SUBMITTED" -> EmergencyBlue
        "PENDING_REVIEW" -> EmergencyYellow
        "NEEDS_CLARIFICATION" -> EmergencyOrange
        "VERIFIED" -> ForestGreenPrimaryLight
        "ALERT_PUBLISHED" -> EmergencyOrange
        "RESOLVED" -> Color(0xFF047857)
        "REJECTED" -> Color(0xFF4B5563)
        "UNDER_REVIEW" -> EmergencyYellow
        "CLOSED" -> Color(0xFF4B5563)
        else -> Color.Gray
    }
}

/**
 * Controller for polling hazard alert dispatch status (Section 14.5 F, Requirement 6).
 * Refreshes every 3 seconds while SENDING and stops on terminal statuses.
 */
object DispatchRefreshController {
    const val REFRESH_INTERVAL_MS = 3000L

    fun isPollingStatus(status: String?): Boolean {
        return status.equals("SENDING", ignoreCase = true)
    }

    fun isTerminalStatus(status: String?): Boolean {
        return status.equals("COMPLETED", ignoreCase = true) ||
                status.equals("COMPLETED_WITH_FAILURES", ignoreCase = true)
    }
}
