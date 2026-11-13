package com.rustbeatz.disasteralert.ui.screens.authority

import com.rustbeatz.disasteralert.data.network.ApiException
import com.rustbeatz.disasteralert.data.repo.DecisionRequest
import com.rustbeatz.disasteralert.data.repo.DecisionResult
import com.rustbeatz.disasteralert.data.repo.DispatchRefreshController
import com.rustbeatz.disasteralert.data.repo.HazardAlertDetail
import com.rustbeatz.disasteralert.data.repo.HazardDraftRequest
import com.rustbeatz.disasteralert.data.repo.HazardDraftResult
import com.rustbeatz.disasteralert.data.repo.HazardDraftValidator
import com.rustbeatz.disasteralert.data.repo.PublishAlertRequest
import com.rustbeatz.disasteralert.data.repo.PublishAlertResult
import com.rustbeatz.disasteralert.data.repo.ReportDetailItem
import com.rustbeatz.disasteralert.data.repo.ReportsRepository

/**
 * Controller/ViewModel managing state and business operations for ReportDetailScreen.
 * Pure Kotlin class easily testable in unit tests without Android UI dependencies.
 */
class ReportDetailViewModel(
    val repository: ReportsRepository,
    val reportId: Long
) {
    var report: ReportDetailItem? = null
        private set

    var loading: Boolean = false
        private set

    var errorMessage: String? = null
        private set

    var isSubmittingDecision: Boolean = false
        private set

    var decisionError: String? = null
        private set

    var isSavingDraft: Boolean = false
        private set

    var draftSaveError: String? = null
        private set

    var draftSuccessMessage: String? = null
        private set

    var showPublishConfirmDialog: Boolean = false
        private set

    var isPublishing: Boolean = false
        private set

    var publishError: String? = null
        private set

    var publishSuccessMessage: String? = null
        private set

    var staleDraftError: String? = null
        private set

    var isPollingDispatch: Boolean = false
        private set

    suspend fun loadReport(): ReportDetailItem {
        loading = true
        errorMessage = null
        try {
            val item = repository.getReportDetail(reportId)
            report = item
            isPollingDispatch = DispatchRefreshController.isPollingStatus(item.hazardAlert?.dispatchStatus)
            return item
        } catch (e: Exception) {
            val msg = e.message ?: "Failed to load report"
            errorMessage = msg
            throw e
        } finally {
            loading = false
        }
    }

    suspend fun verify(note: String?): DecisionResult {
        isSubmittingDecision = true
        decisionError = null
        try {
            val result = repository.submitDecision(
                reportId,
                DecisionRequest("VERIFY", note?.ifBlank { null })
            )
            loadReport()
            return result
        } catch (e: ApiException) {
            decisionError = e.message
            throw e
        } finally {
            isSubmittingDecision = false
        }
    }

    suspend fun reject(note: String): DecisionResult {
        if (note.isBlank()) {
            val err = "Reject reason note is required"
            decisionError = err
            throw ApiException(err, statusCode = 400)
        }
        isSubmittingDecision = true
        decisionError = null
        try {
            val result = repository.submitDecision(
                reportId,
                DecisionRequest("REJECT", note.trim())
            )
            loadReport()
            return result
        } catch (e: ApiException) {
            decisionError = e.message
            throw e
        } finally {
            isSubmittingDecision = false
        }
    }

    suspend fun askQuestion(question: String): DecisionResult {
        if (question.isBlank()) {
            val err = "Question text is required"
            decisionError = err
            throw ApiException(err, statusCode = 400)
        }
        isSubmittingDecision = true
        decisionError = null
        try {
            val result = repository.submitDecision(
                reportId,
                DecisionRequest("REQUEST_CLARIFICATION", question.trim())
            )
            loadReport()
            return result
        } catch (e: ApiException) {
            decisionError = e.message
            throw e
        } finally {
            isSubmittingDecision = false
        }
    }

    suspend fun closeWithoutAlert(): DecisionResult {
        isSubmittingDecision = true
        decisionError = null
        try {
            val result = repository.submitDecision(
                reportId,
                DecisionRequest("RESOLVE", "Closed without alert")
            )
            loadReport()
            return result
        } catch (e: ApiException) {
            decisionError = e.message
            throw e
        } finally {
            isSubmittingDecision = false
        }
    }

    suspend fun markResolved(): DecisionResult {
        isSubmittingDecision = true
        decisionError = null
        try {
            val result = repository.submitDecision(
                reportId,
                DecisionRequest("RESOLVE")
            )
            loadReport()
            return result
        } catch (e: ApiException) {
            decisionError = e.message
            throw e
        } finally {
            isSubmittingDecision = false
        }
    }

    suspend fun saveDraft(request: HazardDraftRequest): HazardDraftResult {
        val validation = HazardDraftValidator.validate(
            hazardType = request.hazardType,
            severity = request.severity,
            title = request.title,
            message = request.message,
            instructions = request.instructions
        )
        if (!validation.isValid) {
            val err = validation.firstError ?: "Invalid draft inputs"
            draftSaveError = err
            throw ApiException(err, statusCode = 400)
        }

        isSavingDraft = true
        draftSaveError = null
        draftSuccessMessage = null
        try {
            val result = repository.saveHazardDraft(reportId, request)
            loadReport()
            draftSuccessMessage = "Draft saved (v${result.draftVersion})"
            return result
        } catch (e: ApiException) {
            draftSaveError = e.message
            throw e
        } finally {
            isSavingDraft = false
        }
    }

    fun requestPublishConfirmation() {
        showPublishConfirmDialog = true
        publishError = null
        staleDraftError = null
    }

    fun dismissPublishConfirmation() {
        showPublishConfirmDialog = false
    }

    suspend fun confirmPublish(): PublishAlertResult {
        if (!showPublishConfirmDialog) {
            throw IllegalStateException("Publish cannot be called without confirmation dialog")
        }
        if (isPublishing) {
            throw IllegalStateException("Publish is already running (double-tap prevented)")
        }

        val curAlert = report?.hazardAlert
            ?: throw IllegalStateException("No hazard alert draft found on report $reportId")

        isPublishing = true
        publishError = null
        staleDraftError = null
        try {
            val result = repository.publishHazardAlert(
                curAlert.id,
                PublishAlertRequest(confirm = true, draftVersion = curAlert.draftVersion)
            )
            showPublishConfirmDialog = false
            if (result.alreadyPublished) {
                publishSuccessMessage = "Already published"
            } else {
                publishSuccessMessage = "Alert published successfully"
            }
            loadReport()
            return result
        } catch (e: ApiException) {
            if (e.statusCode == 409 && (e.message?.contains("changed", ignoreCase = true) == true || e.message?.contains("stale", ignoreCase = true) == true)) {
                // Stale draft: reload the draft, tell officer it changed, make them review again; NEVER retry automatically!
                showPublishConfirmDialog = false
                staleDraftError = e.message ?: "The draft changed after you opened it. Review the latest version and publish again"
                try {
                    loadReport()
                } catch (_: Exception) {}
            } else {
                publishError = e.message
            }
            throw e
        } finally {
            isPublishing = false
        }
    }

    suspend fun onPollingTick(): Boolean {
        val alertId = report?.hazardAlert?.id ?: return false
        try {
            val refreshedAlert = repository.getHazardAlert(alertId)
            report = report?.copy(hazardAlert = refreshedAlert)
            val terminal = DispatchRefreshController.isTerminalStatus(refreshedAlert.dispatchStatus)
            isPollingDispatch = !terminal
            return !terminal
        } catch (_: Exception) {
            return false
        }
    }
}
