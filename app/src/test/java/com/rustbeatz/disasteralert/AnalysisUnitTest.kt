package com.rustbeatz.disasteralert

import com.rustbeatz.disasteralert.data.network.AnalysisParser
import com.rustbeatz.disasteralert.data.repo.AnalysisChipUtils
import com.rustbeatz.disasteralert.data.repo.AnalysisRefreshController
import com.rustbeatz.disasteralert.data.repo.ConsistencyChipStyle
import com.rustbeatz.disasteralert.data.repo.ReportStatusUtils
import com.rustbeatz.disasteralert.data.repo.SeverityChipUtils
import com.rustbeatz.disasteralert.ui.theme.EmergencyOrange
import com.rustbeatz.disasteralert.ui.theme.EmergencyRed
import com.rustbeatz.disasteralert.ui.theme.EmergencyYellow
import com.rustbeatz.disasteralert.ui.theme.ForestGreenPrimaryLight
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AnalysisUnitTest {

    private fun loadMockFile(fileName: String): String {
        val candidates = listOf(
            File("s4a-mock", fileName),
            File("../s4a-mock", fileName),
            File(System.getProperty("user.dir") ?: ".", "s4a-mock/$fileName"),
            File(File(System.getProperty("user.dir") ?: ".").parentFile, "s4a-mock/$fileName")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: throw IllegalArgumentException("Could not find mock file: $fileName")
        return file.readText(Charsets.UTF_8)
    }

    // ---------------------------------------------------------------- Mock File Parsing Tests

    @Test
    fun parseAnalysisCompleted_allFieldsPopulated() {
        val jsonStr = loadMockFile("analysis-completed.json")
        val analysis = AnalysisParser.parseAnalysisFromString(jsonStr)

        assertNotNull(analysis)
        assertEquals("COMPLETED", analysis!!.status)
        assertEquals("Mud and rock across a two-lane road, no vehicles visible.", analysis.sceneSummary)
        assertEquals("LANDSLIDE", analysis.suggestedCategory)
        assertEquals("Road blocked by landslide", analysis.hazard)
        assertEquals("ORANGE", analysis.severity)
        assertEquals("CONSISTENT", analysis.consistency)
        assertEquals(0.78, analysis.confidence!!, 0.001)
        assertEquals(78, analysis.confidencePercentage)
        assertEquals(2, analysis.caveats.size)
        assertEquals("Photo shows no scale", analysis.caveats[0])
        assertEquals("Location not verified against EXIF", analysis.caveats[1])

        assertNotNull(analysis.draftAlert)
        assertEquals("Road blocked near Kollur", analysis.draftAlert!!.title)
        assertEquals("A landslide is blocking the road near Kollur bridge.", analysis.draftAlert!!.message)
        assertEquals(2, analysis.draftAlert!!.instructions.size)
        assertEquals("Do not use this road", analysis.draftAlert!!.instructions[0])
        assertEquals("Use the alternate route via ...", analysis.draftAlert!!.instructions[1])

        assertEquals("gemini-2.5-flash", analysis.model)
        assertEquals("v1", analysis.promptVersion)
        assertEquals("2026-11-03T08:12:41Z", analysis.analyzedAt)
        assertFalse(analysis.isSimulated)
        assertNull(analysis.error)
    }

    @Test
    fun parseAnalysisCompletedFake_detectsSimulatedBadge() {
        val jsonStr = loadMockFile("analysis-completed-fake.json")
        val analysis = AnalysisParser.parseAnalysisFromString(jsonStr)

        assertNotNull(analysis)
        assertEquals("COMPLETED", analysis!!.status)
        assertTrue(analysis.isSimulated)
        assertEquals("simulated-fake", analysis.model)
        assertEquals(0.5, analysis.confidence!!, 0.001)
        assertEquals(50, analysis.confidencePercentage)
        assertEquals(1, analysis.caveats.size)
        assertEquals("Simulated result for testing", analysis.caveats[0])
        assertEquals("Avoid this road", analysis.draftAlert!!.instructions[0])
    }

    @Test
    fun parseAnalysisInconsistent_warningConsistencyAndSuggestedCategory() {
        val jsonStr = loadMockFile("analysis-inconsistent.json")
        val analysis = AnalysisParser.parseAnalysisFromString(jsonStr)

        assertNotNull(analysis)
        assertEquals("COMPLETED", analysis!!.status)
        assertEquals("INCONSISTENT", analysis.consistency)
        assertEquals(ConsistencyChipStyle.WARNING, AnalysisChipUtils.getConsistencyStyle(analysis.consistency))
        assertEquals("GREEN", analysis.severity)
        assertEquals("OTHER", analysis.suggestedCategory)
        assertEquals(0.31, analysis.confidence!!, 0.001)
        assertEquals(31, analysis.confidencePercentage)
        assertEquals(2, analysis.caveats.size)
    }

    @Test
    fun parseAnalysisQueued_stateQueuedWithSafeDefaults() {
        val jsonStr = loadMockFile("analysis-queued.json")
        val analysis = AnalysisParser.parseAnalysisFromString(jsonStr)

        assertNotNull(analysis)
        assertEquals("QUEUED", analysis!!.status)
        assertNull(analysis.sceneSummary)
        assertNull(analysis.suggestedCategory)
        assertNull(analysis.hazard)
        assertNull(analysis.severity)
        assertNull(analysis.consistency)
        assertNull(analysis.confidence)
        assertTrue(analysis.caveats.isEmpty())
        assertNull(analysis.draftAlert)
        assertNull(analysis.error)
    }

    @Test
    fun parseAnalysisRunning_stateRunningWithSafeDefaults() {
        val jsonStr = loadMockFile("analysis-running.json")
        val analysis = AnalysisParser.parseAnalysisFromString(jsonStr)

        assertNotNull(analysis)
        assertEquals("RUNNING", analysis!!.status)
        assertNull(analysis.sceneSummary)
        assertNull(analysis.error)
    }

    @Test
    fun parseAnalysisFailed_hasErrorText() {
        val jsonStr = loadMockFile("analysis-failed.json")
        val analysis = AnalysisParser.parseAnalysisFromString(jsonStr)

        assertNotNull(analysis)
        assertEquals("FAILED", analysis!!.status)
        assertEquals("Provider error after 3 attempts: timeout", analysis.error)
        assertNull(analysis.sceneSummary)
    }

    @Test
    fun parseAnalysisSkipped_hasErrorText() {
        val jsonStr = loadMockFile("analysis-skipped.json")
        val analysis = AnalysisParser.parseAnalysisFromString(jsonStr)

        assertNotNull(analysis)
        assertEquals("SKIPPED", analysis!!.status)
        assertEquals("No AI provider configured", analysis.error)
        assertNull(analysis.sceneSummary)
    }

    @Test
    fun parseReportDetailAnalysisCompleted_parsesWholeReportWithAnalysis() {
        val jsonStr = loadMockFile("report-detail-analysis-completed.json")
        val root = JSONObject(jsonStr)

        assertEquals(41L, root.getLong("id"))
        assertEquals("REPORT-00041", root.getString("reportCode"))
        assertEquals("ROAD_BLOCKED", root.getString("category"))
        assertEquals("PENDING_REVIEW", root.getString("status"))
        assertEquals("COMPLETED", root.getString("analysisStatus"))

        val analysisObj = root.optJSONObject("analysis")
        assertNotNull(analysisObj)

        val analysis = AnalysisParser.parseAnalysis(analysisObj)
        assertNotNull(analysis)
        assertEquals("COMPLETED", analysis!!.status)
        assertEquals("LANDSLIDE", analysis.suggestedCategory)
        assertEquals("ORANGE", analysis.severity)
        assertEquals(78, analysis.confidencePercentage)
        assertEquals(2, analysis.draftAlert!!.instructions.size)
    }

    // ---------------------------------------------------------------- Nullable & Extra Fields

    @Test
    fun parseAnalysis_handlesNullInputGracefully() {
        assertNull(AnalysisParser.parseAnalysis(null))
        assertNull(AnalysisParser.parseAnalysisFromString(null))
        assertNull(AnalysisParser.parseAnalysisFromString(""))
        assertNull(AnalysisParser.parseAnalysisFromString("   "))
        assertNull(AnalysisParser.parseAnalysisFromString("null"))
    }

    @Test
    fun parseAnalysis_handlesExplicitNullFieldsWithoutCrash() {
        val json = JSONObject(
            """
            {
                "status": "COMPLETED",
                "sceneSummary": null,
                "suggestedCategory": null,
                "hazard": null,
                "severity": null,
                "consistency": null,
                "confidence": null,
                "caveats": null,
                "draftAlert": null,
                "model": null,
                "promptVersion": null,
                "analyzedAt": null,
                "error": null
            }
            """.trimIndent()
        )
        val analysis = AnalysisParser.parseAnalysis(json)
        assertNotNull(analysis)
        assertEquals("COMPLETED", analysis!!.status)
        assertNull(analysis.sceneSummary)
        assertNull(analysis.confidence)
        assertTrue(analysis.caveats.isEmpty())
        assertNull(analysis.draftAlert)
        assertNull(analysis.error)
    }

    @Test
    fun parseAnalysis_handlesUnknownExtraFieldsWithoutCrash() {
        val json = JSONObject(
            """
            {
                "status": "COMPLETED",
                "severity": "RED",
                "hazard": "Bridge collapse",
                "unknownFieldA": 9999,
                "unknownFieldB": "extra",
                "nestedObject": {"foo": "bar", "num": 123},
                "arrayField": [1, 2, 3]
            }
            """.trimIndent()
        )
        val analysis = AnalysisParser.parseAnalysis(json)
        assertNotNull(analysis)
        assertEquals("COMPLETED", analysis!!.status)
        assertEquals("RED", analysis.severity)
        assertEquals("Bridge collapse", analysis.hazard)
    }

    // ---------------------------------------------------------------- Severity Chip Mapping

    @Test
    fun severityChipMapping_themeColors() {
        assertEquals(EmergencyRed, SeverityChipUtils.getSeverityColors("RED").textColor)
        assertEquals(EmergencyOrange, SeverityChipUtils.getSeverityColors("ORANGE").textColor)
        assertEquals(EmergencyYellow, SeverityChipUtils.getSeverityColors("YELLOW").textColor)
        assertEquals(ForestGreenPrimaryLight, SeverityChipUtils.getSeverityColors("GREEN").textColor)

        // Case insensitivity
        assertEquals(EmergencyRed, SeverityChipUtils.getSeverityColors("red").textColor)
        assertEquals(EmergencyOrange, SeverityChipUtils.getSeverityColors("orange").textColor)
        assertEquals(EmergencyYellow, SeverityChipUtils.getSeverityColors("yellow").textColor)
        assertEquals(ForestGreenPrimaryLight, SeverityChipUtils.getSeverityColors("green").textColor)

        // Null and unknown fallback
        assertNotNull(SeverityChipUtils.getSeverityColors(null))
        assertNotNull(SeverityChipUtils.getSeverityColors("UNKNOWN"))
    }

    // ---------------------------------------------------------------- Consistency Chip Mapping

    @Test
    fun consistencyChipMapping_inconsistentAndUnclearAreWarning() {
        assertEquals(ConsistencyChipStyle.WARNING, AnalysisChipUtils.getConsistencyStyle("INCONSISTENT"))
        assertEquals(ConsistencyChipStyle.WARNING, AnalysisChipUtils.getConsistencyStyle("inconsistent"))
        assertEquals(ConsistencyChipStyle.WARNING, AnalysisChipUtils.getConsistencyStyle("UNCLEAR"))
        assertEquals(ConsistencyChipStyle.WARNING, AnalysisChipUtils.getConsistencyStyle("unclear"))

        assertEquals(ConsistencyChipStyle.NORMAL, AnalysisChipUtils.getConsistencyStyle("CONSISTENT"))
        assertEquals(ConsistencyChipStyle.NORMAL, AnalysisChipUtils.getConsistencyStyle("consistent"))
        assertEquals(ConsistencyChipStyle.NORMAL, AnalysisChipUtils.getConsistencyStyle("PARTIAL"))
        assertEquals(ConsistencyChipStyle.NORMAL, AnalysisChipUtils.getConsistencyStyle("partial"))

        // Null and unknown fallback to NORMAL
        assertEquals(ConsistencyChipStyle.NORMAL, AnalysisChipUtils.getConsistencyStyle(null))
        assertEquals(ConsistencyChipStyle.NORMAL, AnalysisChipUtils.getConsistencyStyle("OTHER"))
    }

    // ---------------------------------------------------------------- Refresh Logic & Terminal State

    @Test
    fun refreshController_pollingAndTerminalStates() {
        assertTrue(AnalysisRefreshController.isPollingStatus("QUEUED"))
        assertTrue(AnalysisRefreshController.isPollingStatus("RUNNING"))
        assertFalse(AnalysisRefreshController.isPollingStatus("COMPLETED"))
        assertFalse(AnalysisRefreshController.isPollingStatus("FAILED"))
        assertFalse(AnalysisRefreshController.isPollingStatus("SKIPPED"))
        assertFalse(AnalysisRefreshController.isPollingStatus(null))

        assertFalse(AnalysisRefreshController.isTerminalStatus("QUEUED"))
        assertFalse(AnalysisRefreshController.isTerminalStatus("RUNNING"))
        assertTrue(AnalysisRefreshController.isTerminalStatus("COMPLETED"))
        assertTrue(AnalysisRefreshController.isTerminalStatus("FAILED"))
        assertTrue(AnalysisRefreshController.isTerminalStatus("SKIPPED"))
        assertTrue(AnalysisRefreshController.isTerminalStatus(null))
    }

    @Test
    fun refreshController_pollingStopsOnTerminalStatus() = runBlocking {
        // Sequence simulating: QUEUED -> RUNNING -> COMPLETED
        val statusSequence = mutableListOf("RUNNING", "COMPLETED")
        var fetchCount = 0

        val finalStatus = AnalysisRefreshController.pollUntilTerminal(
            initialStatus = "QUEUED",
            intervalMs = 1L
        ) {
            fetchCount++
            statusSequence.removeAt(0)
        }

        assertEquals("COMPLETED", finalStatus)
        assertEquals(2, fetchCount)
        assertTrue(AnalysisRefreshController.isTerminalStatus(finalStatus))
    }

    @Test
    fun refreshController_pollingStopsImmediatelyIfAlreadyTerminal() = runBlocking {
        var fetchCount = 0

        val finalStatus = AnalysisRefreshController.pollUntilTerminal(
            initialStatus = "COMPLETED",
            intervalMs = 1L
        ) {
            fetchCount++
            "COMPLETED"
        }

        assertEquals("COMPLETED", finalStatus)
        assertEquals(0, fetchCount)
    }

    @Test
    fun refreshController_pollingStopsOnFailedOrSkipped() = runBlocking {
        var fetchCount = 0

        val finalStatus = AnalysisRefreshController.pollUntilTerminal(
            initialStatus = "QUEUED",
            intervalMs = 1L
        ) {
            fetchCount++
            "FAILED"
        }

        assertEquals("FAILED", finalStatus)
        assertEquals(1, fetchCount)
        assertTrue(AnalysisRefreshController.isTerminalStatus(finalStatus))
    }

    // ---------------------------------------------------------------- 14.2 Status Model Labels

    @Test
    fun reportStatusLabels_allSection14_2StatusesRender() {
        assertEquals("Submitted", ReportStatusUtils.getDisplayLabel("SUBMITTED"))
        assertEquals("Pending Review", ReportStatusUtils.getDisplayLabel("PENDING_REVIEW"))
        assertEquals("Needs Clarification", ReportStatusUtils.getDisplayLabel("NEEDS_CLARIFICATION"))
        assertEquals("Verified", ReportStatusUtils.getDisplayLabel("VERIFIED"))
        assertEquals("Rejected", ReportStatusUtils.getDisplayLabel("REJECTED"))
        assertEquals("Alert Published", ReportStatusUtils.getDisplayLabel("ALERT_PUBLISHED"))
        assertEquals("Resolved", ReportStatusUtils.getDisplayLabel("RESOLVED"))
        assertEquals("Under Review", ReportStatusUtils.getDisplayLabel("UNDER_REVIEW"))
        assertEquals("Closed", ReportStatusUtils.getDisplayLabel("CLOSED"))
    }
}
