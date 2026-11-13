package com.rustbeatz.disasteralert.data.network

import com.rustbeatz.disasteralert.data.repo.DraftAlert
import com.rustbeatz.disasteralert.data.repo.ReportAnalysis
import org.json.JSONObject

/**
 * Optional-safe parser for the "analysis" object of GET /api/reports/{id}.
 * Guarantees that missing fields, unknown extra fields, or unexpected nulls never crash.
 */
object AnalysisParser {

    fun parseAnalysis(json: JSONObject?): ReportAnalysis? {
        if (json == null) return null
        val status = json.optString("status", "").ifBlank {
            if (json.has("error")) "FAILED" else return null
        }

        val sceneSummary = optNullableString(json, "sceneSummary")
        val suggestedCategory = optNullableString(json, "suggestedCategory")
        val hazard = optNullableString(json, "hazard")
        val severity = optNullableString(json, "severity")
        val consistency = optNullableString(json, "consistency")

        val confidence = if (json.has("confidence") && !json.isNull("confidence")) {
            val d = json.optDouble("confidence")
            if (d.isNaN()) null else d
        } else null

        val caveats = mutableListOf<String>()
        val caveatsArray = json.optJSONArray("caveats")
        if (caveatsArray != null) {
            for (i in 0 until caveatsArray.length()) {
                val caveat = caveatsArray.optString(i)
                if (!caveat.isNullOrBlank()) {
                    caveats.add(caveat)
                }
            }
        }

        val draftAlert = if (json.has("draftAlert") && !json.isNull("draftAlert")) {
            val dObj = json.optJSONObject("draftAlert")
            if (dObj != null) {
                val instructions = mutableListOf<String>()
                val instArray = dObj.optJSONArray("instructions")
                if (instArray != null) {
                    for (i in 0 until instArray.length()) {
                        val inst = instArray.optString(i)
                        if (!inst.isNullOrBlank()) {
                            instructions.add(inst)
                        }
                    }
                }
                DraftAlert(
                    title = optNullableString(dObj, "title"),
                    message = optNullableString(dObj, "message"),
                    instructions = instructions
                )
            } else null
        } else null

        val model = optNullableString(json, "model")
        val promptVersion = optNullableString(json, "promptVersion")
        val analyzedAt = optNullableString(json, "analyzedAt")
        val error = optNullableString(json, "error")

        return ReportAnalysis(
            status = status,
            sceneSummary = sceneSummary,
            suggestedCategory = suggestedCategory,
            hazard = hazard,
            severity = severity,
            consistency = consistency,
            confidence = confidence,
            caveats = caveats,
            draftAlert = draftAlert,
            model = model,
            promptVersion = promptVersion,
            analyzedAt = analyzedAt,
            error = error
        )
    }

    fun parseAnalysisFromString(jsonStr: String?): ReportAnalysis? {
        if (jsonStr.isNullOrBlank() || jsonStr.trim() == "null") return null
        return try {
            parseAnalysis(JSONObject(jsonStr))
        } catch (_: Exception) {
            null
        }
    }

    private fun optNullableString(json: JSONObject, key: String): String? {
        if (!json.has(key) || json.isNull(key)) return null
        val v = json.optString(key)
        return v.ifBlank { null }
    }
}
