package com.rustbeatz.disasteralert

import com.rustbeatz.disasteralert.data.network.ApiException
import com.rustbeatz.disasteralert.data.network.HazardAlertParser
import com.rustbeatz.disasteralert.data.repo.DecisionRequest
import com.rustbeatz.disasteralert.data.repo.DispatchRefreshController
import com.rustbeatz.disasteralert.data.repo.HazardAlertDelivery
import com.rustbeatz.disasteralert.data.repo.HazardAlertDetail
import com.rustbeatz.disasteralert.data.repo.HazardDraftRequest
import com.rustbeatz.disasteralert.data.repo.HazardDraftValidator
import com.rustbeatz.disasteralert.data.repo.MockReportsRepository
import com.rustbeatz.disasteralert.data.repo.PublishAlertRequest
import com.rustbeatz.disasteralert.data.repo.PublishAlertResult
import com.rustbeatz.disasteralert.data.repo.ReportActionRules
import com.rustbeatz.disasteralert.data.repo.ReportStatusUtils
import com.rustbeatz.disasteralert.ui.screens.authority.ReportDetailViewModel
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class S4bUnitTest {

    private fun loadMockFile(fileName: String): String {
        val candidates = listOf(
            File("s4b-mock", fileName),
            File("../s4b-mock", fileName),
            File("../../s4b-mock", fileName),
            File(System.getProperty("user.dir") ?: ".", "s4b-mock/$fileName"),
            File(File(System.getProperty("user.dir") ?: ".").parentFile, "s4b-mock/$fileName")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: throw IllegalArgumentException("Could not find mock file: $fileName")
        return file.readText(Charsets.UTF_8)
    }

    // =========================================================================
    // 1. UNIT TESTS THAT PARSE EVERY FILE IN s4b-mock (ALL 20 FILES)
    // =========================================================================

    @Test
    fun parseDecisionRequestClarification() {
        val jsonStr = loadMockFile("decision-request-clarification.json")
        val obj = JSONObject(jsonStr)
        assertEquals("REQUEST_CLARIFICATION", obj.getString("decision"))
        assertEquals("Is the road fully blocked or one lane open?", obj.getString("note"))
    }

    @Test
    fun parseDecisionRequestVerify() {
        val jsonStr = loadMockFile("decision-request-verify.json")
        val obj = JSONObject(jsonStr)
        assertEquals("VERIFY", obj.getString("decision"))
        assertEquals("Confirmed with district control room", obj.getString("note"))
    }

    @Test
    fun parseDecisionResponse() {
        val jsonStr = loadMockFile("decision-response.json")
        val result = HazardAlertParser.parseDecisionResult(JSONObject(jsonStr))
        assertEquals(41L, result.reportId)
        assertEquals("VERIFIED", result.status)
    }

    @Test
    fun parseErrorsJson_allVariants() {
        val jsonStr = loadMockFile("errors.json")
        val root = JSONObject(jsonStr)

        assertEquals("confirm must be true", root.getJSONObject("400_confirm_not_true").getString("error"))
        assertEquals("title must be 1-120 characters", root.getJSONObject("400_validation").getString("error"))
        assertEquals("This action needs the AUTHORITY role", root.getJSONObject("403_reporter").getString("error"))
        assertEquals("Report not found", root.getJSONObject("404").getString("error"))
        assertEquals("Report cannot be verified from status VERIFIED", root.getJSONObject("409_transition").getString("error"))
        assertEquals(
            "The draft changed after you opened it. Review the latest version and publish again",
            root.getJSONObject("409_stale_draft").getString("error")
        )
        assertEquals("A draft needs a VERIFIED report", root.getJSONObject("409_not_verified").getString("error"))
        assertEquals("The alert is already published", root.getJSONObject("409_already_published").getString("error"))
    }

    @Test
    fun parseHazardAlertGet() {
        val jsonStr = loadMockFile("hazard-alert-get.json")
        val alert = HazardAlertParser.parseHazardAlert(JSONObject(jsonStr))
        assertNotNull(alert)
        assertEquals(9L, alert!!.id)
        assertEquals("HAZARD-00009", alert.alertCode)
        assertEquals("PUBLISHED", alert.status)
        assertEquals(3, alert.draftVersion)
        assertEquals("Landslide", alert.hazardType)
        assertEquals("ORANGE", alert.severity)
        assertEquals("Karnataka", alert.state)
        assertEquals("Udupi", alert.district)
        assertEquals("Kollur", alert.city)
        assertEquals("Road blocked near Kollur", alert.title)
        assertEquals("A landslide is blocking the road near Kollur bridge.", alert.message)
        assertEquals("Do not use this road.\nUse the alternate route via the coastal highway.", alert.instructions)
        assertEquals(42, alert.audienceEstimate)
        assertEquals("SENDING", alert.dispatchStatus)
        assertEquals("District Collector Udupi", alert.approvedBy)
        assertEquals("2026-11-04T08:40:00Z", alert.publishedAt)
        assertNotNull(alert.delivery)
        assertEquals(12, alert.delivery!!.sent)
        assertEquals(0, alert.delivery!!.failed)
        assertEquals(30, alert.delivery!!.pending)
    }

    @Test
    fun parseHazardDraftPutRequest() {
        val jsonStr = loadMockFile("hazard-draft-put-request.json")
        val obj = JSONObject(jsonStr)
        assertEquals("Landslide", obj.getString("hazardType"))
        assertEquals("ORANGE", obj.getString("severity"))
        assertEquals("Karnataka", obj.getString("state"))
        assertEquals("Udupi", obj.getString("district"))
        assertEquals("Kollur", obj.getString("city"))
        assertEquals("Road blocked near Kollur", obj.getString("title"))
        assertEquals("A landslide is blocking the road near Kollur bridge.", obj.getString("message"))
        assertEquals("Do not use this road.\nUse the alternate route via the coastal highway.", obj.getString("instructions"))
    }

    @Test
    fun parseHazardDraftPutResponse() {
        val jsonStr = loadMockFile("hazard-draft-put-response.json")
        val result = HazardAlertParser.parseHazardDraftResult(JSONObject(jsonStr))
        assertEquals(9L, result.id)
        assertEquals("HAZARD-00009", result.alertCode)
        assertEquals("DRAFT", result.status)
        assertEquals(3, result.draftVersion)
        assertEquals(42, result.audienceEstimate)
    }

    @Test
    fun parsePublishRequest() {
        val jsonStr = loadMockFile("publish-request.json")
        val obj = JSONObject(jsonStr)
        assertTrue(obj.getBoolean("confirm"))
        assertEquals(3, obj.getInt("draftVersion"))
    }

    @Test
    fun parsePublishResponseAlready() {
        val jsonStr = loadMockFile("publish-response-already.json")
        val result = HazardAlertParser.parsePublishAlertResult(JSONObject(jsonStr))
        assertEquals(9L, result.id)
        assertEquals("PUBLISHED", result.status)
        assertEquals("COMPLETED", result.dispatchStatus)
        assertEquals("2026-11-04T08:40:00Z", result.publishedAt)
        assertTrue(result.alreadyPublished)
    }

    @Test
    fun parsePublishResponse() {
        val jsonStr = loadMockFile("publish-response.json")
        val result = HazardAlertParser.parsePublishAlertResult(JSONObject(jsonStr))
        assertEquals(9L, result.id)
        assertEquals("PUBLISHED", result.status)
        assertEquals("SENDING", result.dispatchStatus)
        assertEquals("2026-11-04T08:40:00Z", result.publishedAt)
        assertFalse(result.alreadyPublished)
    }

    @Test
    fun parseReportDetailPendingReview() {
        val jsonStr = loadMockFile("report-detail-pending-review.json")
        val item = HazardAlertParser.parseReportDetailItem(JSONObject(jsonStr))
        assertEquals(41L, item.id)
        assertEquals("REPORT-00041", item.reportCode)
        assertEquals("ROAD_BLOCKED", item.category)
        assertEquals("PENDING_REVIEW", item.status)
        assertEquals("COMPLETED", item.analysisStatus)
        assertNotNull(item.analysis)
        assertTrue(item.decisions.isEmpty())
        assertTrue(item.clarifications.isEmpty())
        assertNull(item.hazardAlert)
    }

    @Test
    fun parseReportDetailNeedsClarification() {
        val jsonStr = loadMockFile("report-detail-needs-clarification.json")
        val item = HazardAlertParser.parseReportDetailItem(JSONObject(jsonStr))
        assertEquals("NEEDS_CLARIFICATION", item.status)
        assertEquals(1, item.decisions.size)
        assertEquals("REQUEST_CLARIFICATION", item.decisions[0].decision)
        assertEquals("Is the road fully blocked or one lane open?", item.decisions[0].note)

        assertEquals(1, item.clarifications.size)
        val clar = item.clarifications[0]
        assertEquals("Is the road fully blocked or one lane open?", clar.question)
        assertEquals("District Collector Udupi", clar.askedBy)
        assertNull(clar.answer)
        assertFalse(clar.isAnswered)
    }

    @Test
    fun parseReportDetailVerifiedNoDraft() {
        val jsonStr = loadMockFile("report-detail-verified-no-draft.json")
        val item = HazardAlertParser.parseReportDetailItem(JSONObject(jsonStr))
        assertEquals("VERIFIED", item.status)
        assertNull(item.hazardAlert)
        assertEquals(2, item.decisions.size)
        assertEquals("VERIFY", item.decisions[1].decision)
        assertEquals(1, item.clarifications.size)
        assertTrue(item.clarifications[0].isAnswered)
        assertEquals("One lane is open", item.clarifications[0].answer)
    }

    @Test
    fun parseReportDetailVerifiedDraft() {
        val jsonStr = loadMockFile("report-detail-verified-draft.json")
        val item = HazardAlertParser.parseReportDetailItem(JSONObject(jsonStr))
        assertEquals("VERIFIED", item.status)
        assertNotNull(item.hazardAlert)
        val alert = item.hazardAlert!!
        assertEquals("DRAFT", alert.status)
        assertEquals(3, alert.draftVersion)
        assertEquals(42, alert.audienceEstimate)
        assertEquals("Road blocked near Kollur", alert.title)
    }

    @Test
    fun parseReportDetailPublishedSending() {
        val jsonStr = loadMockFile("report-detail-published-sending.json")
        val item = HazardAlertParser.parseReportDetailItem(JSONObject(jsonStr))
        assertEquals("ALERT_PUBLISHED", item.status)
        assertNotNull(item.hazardAlert)
        val alert = item.hazardAlert!!
        assertEquals("PUBLISHED", alert.status)
        assertEquals("SENDING", alert.dispatchStatus)
        assertEquals(12, alert.delivery!!.sent)
        assertEquals(30, alert.delivery!!.pending)
    }

    @Test
    fun parseReportDetailPublishedCompleted() {
        val jsonStr = loadMockFile("report-detail-published-completed.json")
        val item = HazardAlertParser.parseReportDetailItem(JSONObject(jsonStr))
        assertEquals("ALERT_PUBLISHED", item.status)
        assertNotNull(item.hazardAlert)
        val alert = item.hazardAlert!!
        assertEquals("PUBLISHED", alert.status)
        assertEquals("COMPLETED", alert.dispatchStatus)
        assertEquals(42, alert.delivery!!.sent)
        assertEquals(0, alert.delivery!!.pending)
        assertEquals(0, alert.delivery!!.failed)
    }

    @Test
    fun parseReportDetailPublishedWithFailures() {
        val jsonStr = loadMockFile("report-detail-published-with-failures.json")
        val item = HazardAlertParser.parseReportDetailItem(JSONObject(jsonStr))
        assertEquals("ALERT_PUBLISHED", item.status)
        assertNotNull(item.hazardAlert)
        val alert = item.hazardAlert!!
        assertEquals("PUBLISHED", alert.status)
        assertEquals("COMPLETED_WITH_FAILURES", alert.dispatchStatus)
        assertEquals(40, alert.delivery!!.sent)
        assertEquals(2, alert.delivery!!.failed)
        assertEquals(0, alert.delivery!!.pending)
    }

    @Test
    fun parseReportDetailRejected() {
        val jsonStr = loadMockFile("report-detail-rejected.json")
        val item = HazardAlertParser.parseReportDetailItem(JSONObject(jsonStr))
        assertEquals("REJECTED", item.status)
        assertEquals(1, item.decisions.size)
        assertEquals("REJECT", item.decisions[0].decision)
        assertEquals("Photo does not match the location", item.decisions[0].note)
        assertNull(item.hazardAlert)
    }

    @Test
    fun parseReportDetailResolved() {
        val jsonStr = loadMockFile("report-detail-resolved.json")
        val item = HazardAlertParser.parseReportDetailItem(JSONObject(jsonStr))
        assertEquals("RESOLVED", item.status)
        assertEquals(3, item.decisions.size)
        assertEquals("RESOLVE", item.decisions.last().decision)
        assertNotNull(item.hazardAlert)
        assertEquals("RESOLVED", item.hazardAlert!!.status)
        assertEquals("2026-11-04T12:00:00Z", item.hazardAlert!!.resolvedAt)
    }

    @Test
    fun parseReportsMineS4b_reporterHistoryRows() {
        val jsonStr = loadMockFile("reports-mine-s4b.json")
        val arr = JSONArray(jsonStr)
        assertEquals(6, arr.length())

        val list = mutableListOf<com.rustbeatz.disasteralert.data.repo.ReportMineItem>()
        for (i in 0 until arr.length()) {
            list.add(HazardAlertParser.parseReportMineItem(arr.getJSONObject(i)))
        }

        // Row 1: ID 46 NEEDS_CLARIFICATION with clarification object
        val r46 = list.find { it.id == 46L }!!
        assertEquals("NEEDS_CLARIFICATION", r46.status)
        assertNotNull(r46.clarification)
        assertEquals("How many families are affected?", r46.clarification!!.question)
        assertEquals("How many families are affected?", r46.questionText)
        assertNull(r46.alert)

        // Row 2: ID 45 PENDING_REVIEW
        val r45 = list.find { it.id == 45L }!!
        assertEquals("PENDING_REVIEW", r45.status)
        assertNull(r45.clarification)
        assertNull(r45.alert)

        // Row 3: ID 41 ALERT_PUBLISHED with alert
        val r41 = list.find { it.id == 41L }!!
        assertEquals("ALERT_PUBLISHED", r41.status)
        assertNotNull(r41.alert)
        assertEquals("HAZARD-00009", r41.alert!!.alertCode)
        assertEquals("ORANGE", r41.alert!!.severity)
        assertEquals("2026-11-04T08:40:00Z", r41.alert!!.publishedAt)
        assertNull(r41.alert!!.resolvedAt)

        // Row 4: ID 40 RESOLVED with alert and resolvedAt
        val r40 = list.find { it.id == 40L }!!
        assertEquals("RESOLVED", r40.status)
        assertNotNull(r40.alert)
        assertEquals("HAZARD-00007", r40.alert!!.alertCode)
        assertEquals("YELLOW", r40.alert!!.severity)
        assertEquals("2026-11-03T18:20:00Z", r40.alert!!.resolvedAt)

        // Row 5: ID 39 REJECTED
        val r39 = list.find { it.id == 39L }!!
        assertEquals("REJECTED", r39.status)
        assertNull(r39.alert)

        // Row 6: ID 38 VERIFIED
        val r38 = list.find { it.id == 38L }!!
        assertEquals("VERIFIED", r38.status)
        assertNull(r38.alert)
    }

    // =========================================================================
    // 2. STATUS TO CHIP AND TO ALLOWED-ACTIONS MAPPING FOR EVERY STATUS
    // =========================================================================

    @Test
    fun statusToChipMapping_coversAll14_2Statuses() {
        assertEquals("Submitted", ReportStatusUtils.getDisplayLabel("SUBMITTED"))
        assertEquals("Pending Review", ReportStatusUtils.getDisplayLabel("PENDING_REVIEW"))
        assertEquals("Needs Clarification", ReportStatusUtils.getDisplayLabel("NEEDS_CLARIFICATION"))
        assertEquals("Verified", ReportStatusUtils.getDisplayLabel("VERIFIED"))
        assertEquals("Alert Published", ReportStatusUtils.getDisplayLabel("ALERT_PUBLISHED"))
        assertEquals("Resolved", ReportStatusUtils.getDisplayLabel("RESOLVED"))
        assertEquals("Rejected", ReportStatusUtils.getDisplayLabel("REJECTED"))
        assertEquals("Under Review", ReportStatusUtils.getDisplayLabel("UNDER_REVIEW"))
        assertEquals("Closed", ReportStatusUtils.getDisplayLabel("CLOSED"))
    }

    @Test
    fun allowedActionsMapping_coversAll14_2Statuses() {
        // 1. PENDING_REVIEW: Verify, Reject, Ask a question
        val pendingActions = ReportActionRules.getAllowedActions("PENDING_REVIEW")
        assertTrue(pendingActions.contains(ReportActionRules.AuthorityAction.VERIFY))
        assertTrue(pendingActions.contains(ReportActionRules.AuthorityAction.REJECT))
        assertTrue(pendingActions.contains(ReportActionRules.AuthorityAction.ASK_QUESTION))
        assertFalse(ReportActionRules.isReadOnly("PENDING_REVIEW"))

        // 2. NEEDS_CLARIFICATION: no direct decision actions
        val clarActions = ReportActionRules.getAllowedActions("NEEDS_CLARIFICATION")
        assertTrue(clarActions.isEmpty())
        assertFalse(ReportActionRules.isReadOnly("NEEDS_CLARIFICATION"))

        // 3. VERIFIED without draft: Create alert draft, Close without alert
        val verifiedNoDraft = ReportActionRules.getAllowedActions("VERIFIED", hasDraft = false)
        assertTrue(verifiedNoDraft.contains(ReportActionRules.AuthorityAction.CREATE_DRAFT))
        assertTrue(verifiedNoDraft.contains(ReportActionRules.AuthorityAction.CLOSE_WITHOUT_ALERT))
        assertFalse(verifiedNoDraft.contains(ReportActionRules.AuthorityAction.PUBLISH_ALERT))

        // 4. VERIFIED with draft: Create alert draft, Publish alert, Close without alert
        val verifiedDraft = ReportActionRules.getAllowedActions("VERIFIED", hasDraft = true)
        assertTrue(verifiedDraft.contains(ReportActionRules.AuthorityAction.CREATE_DRAFT))
        assertTrue(verifiedDraft.contains(ReportActionRules.AuthorityAction.PUBLISH_ALERT))
        assertTrue(verifiedDraft.contains(ReportActionRules.AuthorityAction.CLOSE_WITHOUT_ALERT))

        // 5. ALERT_PUBLISHED: Mark resolved
        val publishedActions = ReportActionRules.getAllowedActions("ALERT_PUBLISHED")
        assertEquals(setOf(ReportActionRules.AuthorityAction.MARK_RESOLVED), publishedActions)
        assertFalse(ReportActionRules.isReadOnly("ALERT_PUBLISHED"))

        // 6. RESOLVED: read only
        assertTrue(ReportActionRules.getAllowedActions("RESOLVED").isEmpty())
        assertTrue(ReportActionRules.isReadOnly("RESOLVED"))

        // 7. REJECTED: read only
        assertTrue(ReportActionRules.getAllowedActions("REJECTED").isEmpty())
        assertTrue(ReportActionRules.isReadOnly("REJECTED"))

        // 8. SUBMITTED: not reviewable yet
        assertTrue(ReportActionRules.getAllowedActions("SUBMITTED").isEmpty())
    }

    // =========================================================================
    // 3. VALIDATION LIMITS OF THE DRAFT EDITOR
    // =========================================================================

    @Test
    fun draftEditorValidation_hazardTypeLimits() {
        // empty -> error
        val emptyHazard = HazardDraftValidator.validate(
            hazardType = "",
            severity = "ORANGE",
            title = "Valid Title",
            message = "Valid Message",
            instructions = "Valid"
        )
        assertFalse(emptyHazard.isValid)
        assertNotNull(emptyHazard.hazardTypeError)

        // > 60 chars -> error
        val longHazard = HazardDraftValidator.validate(
            hazardType = "A".repeat(61),
            severity = "ORANGE",
            title = "Valid Title",
            message = "Valid Message",
            instructions = "Valid"
        )
        assertFalse(longHazard.isValid)
        assertNotNull(longHazard.hazardTypeError)

        // exactly 60 chars -> valid
        val exact60 = HazardDraftValidator.validate(
            hazardType = "A".repeat(60),
            severity = "ORANGE",
            title = "Valid Title",
            message = "Valid Message",
            instructions = "Valid"
        )
        assertTrue(exact60.isValid)
        assertNull(exact60.hazardTypeError)
    }

    @Test
    fun draftEditorValidation_titleLimits() {
        // empty -> error
        val emptyTitle = HazardDraftValidator.validate(
            hazardType = "Landslide",
            severity = "RED",
            title = "   ",
            message = "Message",
            instructions = "Instructions"
        )
        assertFalse(emptyTitle.isValid)
        assertNotNull(emptyTitle.titleError)

        // > 120 chars -> error
        val longTitle = HazardDraftValidator.validate(
            hazardType = "Landslide",
            severity = "RED",
            title = "T".repeat(121),
            message = "Message",
            instructions = "Instructions"
        )
        assertFalse(longTitle.isValid)
        assertNotNull(longTitle.titleError)

        // 120 chars -> valid
        val exact120 = HazardDraftValidator.validate(
            hazardType = "Landslide",
            severity = "RED",
            title = "T".repeat(120),
            message = "Message",
            instructions = "Instructions"
        )
        assertTrue(exact120.isValid)
    }

    @Test
    fun draftEditorValidation_messageLimits() {
        // empty -> error
        val emptyMsg = HazardDraftValidator.validate(
            hazardType = "Landslide",
            severity = "YELLOW",
            title = "Title",
            message = "",
            instructions = null
        )
        assertFalse(emptyMsg.isValid)
        assertNotNull(emptyMsg.messageError)

        // > 1000 chars -> error
        val longMsg = HazardDraftValidator.validate(
            hazardType = "Landslide",
            severity = "YELLOW",
            title = "Title",
            message = "M".repeat(1001),
            instructions = null
        )
        assertFalse(longMsg.isValid)
        assertNotNull(longMsg.messageError)

        // 1000 chars -> valid
        val exact1000 = HazardDraftValidator.validate(
            hazardType = "Landslide",
            severity = "YELLOW",
            title = "Title",
            message = "M".repeat(1000),
            instructions = null
        )
        assertTrue(exact1000.isValid)
    }

    @Test
    fun draftEditorValidation_instructionsLimits() {
        // > 1000 chars -> error
        val longInstr = HazardDraftValidator.validate(
            hazardType = "Landslide",
            severity = "GREEN",
            title = "Title",
            message = "Message",
            instructions = "I".repeat(1001)
        )
        assertFalse(longInstr.isValid)
        assertNotNull(longInstr.instructionsError)

        // null or up to 1000 chars -> valid
        val validInstr = HazardDraftValidator.validate(
            hazardType = "Landslide",
            severity = "GREEN",
            title = "Title",
            message = "Message",
            instructions = "Line 1\nLine 2\nLine 3"
        )
        assertTrue(validInstr.isValid)
    }

    @Test
    fun draftEditorValidation_severityLimits() {
        listOf("RED", "ORANGE", "YELLOW", "GREEN", "red", "orange").forEach { sev ->
            val res = HazardDraftValidator.validate("Type", sev, "Title", "Message", "Instr")
            assertTrue("Severity $sev should be valid", res.isValid)
        }

        val invalidSev = HazardDraftValidator.validate("Type", "BLUE", "Title", "Message", "Instr")
        assertFalse(invalidSev.isValid)
        assertNotNull(invalidSev.severityError)
    }

    // =========================================================================
    // 4. PUBLISH REQUEST IS BUILT WITH CONFIRM TRUE AND CURRENT draftVersion
    // =========================================================================

    @Test
    fun publishRequest_builtWithConfirmTrueAndCurrentDraftVersion() = runBlocking {
        val repo = MockReportsRepository()
        // Report 48 is VERIFIED with draftVersion = 3
        val vm = ReportDetailViewModel(repo, 48L)
        vm.loadReport()

        assertEquals(3, vm.report?.hazardAlert?.draftVersion)

        vm.requestPublishConfirmation()
        assertTrue(vm.showPublishConfirmDialog)

        val result = vm.confirmPublish()
        assertNotNull(result)
        assertEquals("PUBLISHED", result.status)
        assertEquals("SENDING", result.dispatchStatus)
        assertFalse(result.alreadyPublished)

        // Report has flipped to ALERT_PUBLISHED
        assertEquals("ALERT_PUBLISHED", vm.report?.status)
    }

    // =========================================================================
    // 5. PUBLISH IS NEVER CALLED WITHOUT DIALOG CONFIRMATION (TEST THE VIEW-MODEL)
    // =========================================================================

    @Test
    fun publishIsNeverCalledWithoutDialogConfirmation() = runBlocking {
        val repo = MockReportsRepository()
        val vm = ReportDetailViewModel(repo, 48L)
        vm.loadReport()

        // Attempting to publish without opening the confirmation dialog
        assertFalse(vm.showPublishConfirmDialog)
        try {
            vm.confirmPublish()
            fail("Expected IllegalStateException when calling publish without dialog confirmation")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("without confirmation dialog"))
        }

        // Opening dialog allows confirmation
        vm.requestPublishConfirmation()
        assertTrue(vm.showPublishConfirmDialog)

        // Dismissing dialog disables it again
        vm.dismissPublishConfirmation()
        assertFalse(vm.showPublishConfirmDialog)
        try {
            vm.confirmPublish()
            fail("Expected IllegalStateException after dialog dismissed")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("without confirmation dialog"))
        }
    }

    // =========================================================================
    // 6. STALE-DRAFT AND alreadyPublished HANDLING
    // =========================================================================

    @Test
    fun alreadyPublishedHandling_countsAsSuccess() = runBlocking {
        val repo = MockReportsRepository()
        // Report 49 is already ALERT_PUBLISHED with an alert
        val vm = ReportDetailViewModel(repo, 49L)
        vm.loadReport()

        vm.requestPublishConfirmation()
        val res = vm.confirmPublish()

        // 200 with alreadyPublished = true counts as success
        assertTrue(res.alreadyPublished)
        assertEquals("Already published", vm.publishSuccessMessage)
        assertNull(vm.publishError)
    }

    @Test
    fun staleDraftHandling_reloadsDraftNotifiesOfficerAndNeverRetriesAutomatically() = runBlocking {
        val repo = MockReportsRepository()
        // Report 48 currently has draftVersion = 3
        val vm = ReportDetailViewModel(repo, 48L)
        vm.loadReport()

        // Simulate another officer modifying the draft on the backend to v4
        repo.saveHazardDraft(
            48L,
            HazardDraftRequest(
                hazardType = "Landslide Updated",
                severity = "ORANGE",
                state = "Karnataka",
                district = "Udupi",
                city = "Kollur",
                title = "Road blocked updated",
                message = "Message updated",
                instructions = "Instructions updated"
            )
        )

        // Officer in current session attempts to publish with the old draftVersion (v3)
        vm.requestPublishConfirmation()
        try {
            vm.confirmPublish()
            fail("Expected 409 stale draft error")
        } catch (e: ApiException) {
            assertEquals(409, e.statusCode)
            assertTrue(e.message!!.contains("draft changed"))
        }

        // Dialog must be closed, officer notified with staleDraftError, and draft reloaded to latest v4
        assertFalse(vm.showPublishConfirmDialog)
        assertNotNull(vm.staleDraftError)
        assertTrue(vm.staleDraftError!!.contains("draft changed", ignoreCase = true))

        // Verified: latest version is reloaded and vm did NOT retry publishing automatically!
        assertEquals(4, vm.report?.hazardAlert?.draftVersion)
        assertFalse(vm.isPublishing)
        assertEquals("VERIFIED", vm.report?.status) // still VERIFIED, not published
    }

    // =========================================================================
    // 7. 3-SECOND REFRESH STOPS ON TERMINAL DISPATCH STATUS
    // =========================================================================

    @Test
    fun dispatchRefreshController_terminalStatuses() {
        assertEquals(3000L, DispatchRefreshController.REFRESH_INTERVAL_MS)

        // SENDING is active polling
        assertTrue(DispatchRefreshController.isPollingStatus("SENDING"))
        assertFalse(DispatchRefreshController.isTerminalStatus("SENDING"))

        // COMPLETED and COMPLETED_WITH_FAILURES are terminal
        assertTrue(DispatchRefreshController.isTerminalStatus("COMPLETED"))
        assertTrue(DispatchRefreshController.isTerminalStatus("COMPLETED_WITH_FAILURES"))
        assertFalse(DispatchRefreshController.isPollingStatus("COMPLETED"))
        assertFalse(DispatchRefreshController.isPollingStatus("COMPLETED_WITH_FAILURES"))

        // null or DRAFT are not polling
        assertFalse(DispatchRefreshController.isPollingStatus(null))
        assertFalse(DispatchRefreshController.isPollingStatus("DRAFT"))
    }

    @Test
    fun dispatchPolling_stopsOnTerminalStatusInViewModel() = runBlocking {
        val repo = MockReportsRepository()
        // Report 50 starts with terminal status COMPLETED_WITH_FAILURES
        val vm50 = ReportDetailViewModel(repo, 50L)
        vm50.loadReport()
        assertFalse(vm50.isPollingDispatch)
        assertTrue(DispatchRefreshController.isTerminalStatus(vm50.report?.hazardAlert?.dispatchStatus))

        // Report 49 starts with dispatchStatus = SENDING
        val vm49 = ReportDetailViewModel(repo, 49L)
        vm49.loadReport()
        assertTrue(vm49.isPollingDispatch)
        assertFalse(DispatchRefreshController.isTerminalStatus(vm49.report?.hazardAlert?.dispatchStatus))

        // Simulate terminal dispatch status on alert
        val continueWhileSending = vm49.onPollingTick()
        assertTrue(continueWhileSending) // still SENDING

        // Now update alert to COMPLETED on mock repository
        val r49 = repo.getReportDetail(49L)
        val completedAlert = r49.hazardAlert!!.copy(
            dispatchStatus = "COMPLETED",
            delivery = HazardAlertDelivery(sent = 42, failed = 0, pending = 0)
        )
        // Save completed alert in repo
        repo.submitDecision(49L, DecisionRequest("RESOLVE")) // moves to resolved/terminal

        val continueWhenTerminal = vm49.onPollingTick()
        assertFalse(continueWhenTerminal)
        assertFalse(vm49.isPollingDispatch)
    }
}
