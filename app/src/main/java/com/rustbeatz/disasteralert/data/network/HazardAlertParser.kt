package com.rustbeatz.disasteralert.data.network

import com.rustbeatz.disasteralert.data.repo.HazardAlertDelivery
import com.rustbeatz.disasteralert.data.repo.HazardAlertDetail
import com.rustbeatz.disasteralert.data.repo.ReportClarificationItem
import com.rustbeatz.disasteralert.data.repo.ReportDecisionItem
import com.rustbeatz.disasteralert.data.repo.ReportDetailItem
import com.rustbeatz.disasteralert.data.repo.ReportExif
import com.rustbeatz.disasteralert.data.repo.ReportMineAlert
import com.rustbeatz.disasteralert.data.repo.ReportMineClarification
import com.rustbeatz.disasteralert.data.repo.ReportMineItem
import com.rustbeatz.disasteralert.data.repo.DecisionResult
import com.rustbeatz.disasteralert.data.repo.HazardDraftResult
import com.rustbeatz.disasteralert.data.repo.PublishAlertResult
import org.json.JSONArray
import org.json.JSONObject

object HazardAlertParser {

    fun parseDelivery(obj: JSONObject?): HazardAlertDelivery? {
        if (obj == null) return null
        return HazardAlertDelivery(
            sent = obj.optInt("sent", 0),
            failed = obj.optInt("failed", 0),
            pending = obj.optInt("pending", 0)
        )
    }

    fun parseHazardAlert(obj: JSONObject?): HazardAlertDetail? {
        if (obj == null) return null
        val delObj = if (obj.has("delivery") && !obj.isNull("delivery")) obj.optJSONObject("delivery") else null
        return HazardAlertDetail(
            id = obj.getLong("id"),
            alertCode = obj.getString("alertCode"),
            status = obj.optString("status", "DRAFT"),
            draftVersion = obj.optInt("draftVersion", 1),
            hazardType = obj.optString("hazardType", ""),
            severity = obj.optString("severity", "ORANGE"),
            state = obj.optString("state", ""),
            district = obj.optString("district", ""),
            city = if (obj.isNull("city")) null else obj.optString("city").ifBlank { null },
            title = obj.optString("title", ""),
            message = obj.optString("message", ""),
            instructions = if (obj.isNull("instructions")) null else obj.optString("instructions").ifBlank { null },
            validUntil = if (obj.isNull("validUntil")) null else obj.optString("validUntil").ifBlank { null },
            audienceEstimate = obj.optInt("audienceEstimate", 0),
            dispatchStatus = if (obj.isNull("dispatchStatus")) null else obj.optString("dispatchStatus").ifBlank { null },
            createdBy = if (obj.isNull("createdBy")) null else obj.optString("createdBy").ifBlank { null },
            approvedBy = if (obj.isNull("approvedBy")) null else obj.optString("approvedBy").ifBlank { null },
            publishedAt = if (obj.isNull("publishedAt")) null else obj.optString("publishedAt").ifBlank { null },
            resolvedAt = if (obj.isNull("resolvedAt")) null else obj.optString("resolvedAt").ifBlank { null },
            delivery = parseDelivery(delObj)
        )
    }

    fun parseDecisions(array: JSONArray?): List<ReportDecisionItem> {
        if (array == null) return emptyList()
        val list = mutableListOf<ReportDecisionItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ReportDecisionItem(
                    id = obj.getLong("id"),
                    decision = obj.getString("decision"),
                    note = if (obj.isNull("note")) null else obj.optString("note").ifBlank { null },
                    decidedBy = if (obj.isNull("decidedBy")) null else obj.optString("decidedBy").ifBlank { null },
                    decidedAt = if (obj.isNull("decidedAt")) null else obj.optString("decidedAt").ifBlank { null }
                )
            )
        }
        return list
    }

    fun parseClarifications(array: JSONArray?): List<ReportClarificationItem> {
        if (array == null) return emptyList()
        val list = mutableListOf<ReportClarificationItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ReportClarificationItem(
                    id = obj.getLong("id"),
                    question = obj.getString("question"),
                    askedBy = if (obj.isNull("askedBy")) null else obj.optString("askedBy").ifBlank { null },
                    askedAt = if (obj.isNull("askedAt")) null else obj.optString("askedAt").ifBlank { null },
                    answer = if (obj.isNull("answer")) null else obj.optString("answer").ifBlank { null },
                    answeredAt = if (obj.isNull("answeredAt")) null else obj.optString("answeredAt").ifBlank { null }
                )
            )
        }
        return list
    }

    fun parseDecisionResult(obj: JSONObject): DecisionResult {
        return DecisionResult(
            reportId = obj.getLong("reportId"),
            status = obj.getString("status")
        )
    }

    fun parseHazardDraftResult(obj: JSONObject): HazardDraftResult {
        return HazardDraftResult(
            id = obj.getLong("id"),
            alertCode = obj.getString("alertCode"),
            status = obj.getString("status"),
            draftVersion = obj.getInt("draftVersion"),
            audienceEstimate = obj.optInt("audienceEstimate", 0)
        )
    }

    fun parsePublishAlertResult(obj: JSONObject): PublishAlertResult {
        return PublishAlertResult(
            id = obj.getLong("id"),
            status = obj.getString("status"),
            dispatchStatus = if (obj.isNull("dispatchStatus")) null else obj.optString("dispatchStatus").ifBlank { null },
            publishedAt = if (obj.isNull("publishedAt")) null else obj.optString("publishedAt").ifBlank { null },
            alreadyPublished = obj.optBoolean("alreadyPublished", false)
        )
    }

    fun parseReportDetailItem(obj: JSONObject): ReportDetailItem {
        val id = obj.getLong("id")
        val flags = mutableListOf<String>()
        val flagsArray = obj.optJSONArray("trustFlags")
        if (flagsArray != null) {
            for (j in 0 until flagsArray.length()) {
                flags.add(flagsArray.getString(j))
            }
        }

        val exifObj = if (obj.isNull("exif")) null else obj.optJSONObject("exif")
        val exif = if (exifObj != null) {
            ReportExif(
                hasGps = exifObj.optBoolean("hasGps", false),
                latitude = if (exifObj.isNull("latitude")) null else exifObj.optDouble("latitude"),
                longitude = if (exifObj.isNull("longitude")) null else exifObj.optDouble("longitude"),
                takenAt = if (exifObj.isNull("takenAt")) null else exifObj.optString("takenAt").ifBlank { null },
                distanceToReportedM = if (exifObj.isNull("distanceToReportedM")) null else exifObj.optDouble("distanceToReportedM")
            )
        } else null

        val analysis = if (obj.has("analysis") && !obj.isNull("analysis")) {
            AnalysisParser.parseAnalysis(obj.optJSONObject("analysis"))
        } else null

        val decisions = if (obj.has("decisions") && !obj.isNull("decisions")) {
            parseDecisions(obj.optJSONArray("decisions"))
        } else emptyList()

        val clarifications = if (obj.has("clarifications") && !obj.isNull("clarifications")) {
            parseClarifications(obj.optJSONArray("clarifications"))
        } else emptyList()

        val hazardAlert = if (obj.has("hazardAlert") && !obj.isNull("hazardAlert")) {
            parseHazardAlert(obj.optJSONObject("hazardAlert"))
        } else null

        return ReportDetailItem(
            id = id,
            reportCode = obj.optString("reportCode", "REPORT-$id"),
            category = obj.optString("category", "OTHER"),
            note = if (obj.isNull("note")) null else obj.optString("note").ifBlank { null },
            state = obj.optString("state", ""),
            district = obj.optString("district", ""),
            city = if (obj.isNull("city")) null else obj.optString("city").ifBlank { null },
            latitude = obj.optDouble("latitude", 0.0),
            longitude = obj.optDouble("longitude", 0.0),
            accuracyM = if (obj.isNull("accuracyM")) null else obj.optDouble("accuracyM"),
            capturedAt = obj.optString("capturedAt", obj.optString("createdAt", "")),
            mockLocation = obj.optBoolean("mockLocation", false),
            exif = exif,
            trustFlags = flags,
            status = obj.optString("status", "SUBMITTED"),
            analysisStatus = obj.optString("analysisStatus", "NOT_STARTED"),
            reporterId = obj.optLong("reporterId", 0L),
            reporterName = obj.optString("reporterName", "Reporter"),
            photoUrl = if (obj.isNull("photoUrl")) "/api/reports/$id/photo" else obj.optString("photoUrl", "/api/reports/$id/photo"),
            createdAt = obj.optString("createdAt", ""),
            linkedToReportId = if (obj.isNull("linkedToReportId")) null else obj.optLong("linkedToReportId"),
            analysis = analysis,
            decisions = decisions,
            clarifications = clarifications,
            hazardAlert = hazardAlert
        )
    }

    fun parseReportMineItem(itemObj: JSONObject): ReportMineItem {
        val id = itemObj.getLong("id")
        val repCode = itemObj.optString("reportCode", "REPORT-$id")
        val cat = itemObj.optString("category", "OTHER")
        val note = if (itemObj.isNull("note")) null else itemObj.optString("note").ifBlank { null }
        val state = itemObj.optString("state", "")
        val district = itemObj.optString("district", "")
        val city = if (itemObj.isNull("city")) null else itemObj.optString("city").ifBlank { null }
        val status = itemObj.optString("status", "SUBMITTED")
        val createdAt = itemObj.optString("createdAt", "")
        val linked = if (itemObj.isNull("linkedToReportCode")) null else itemObj.optString("linkedToReportCode").ifBlank { null }

        var clarificationObj: ReportMineClarification? = null
        var clarificationText: String? = null
        if (itemObj.has("clarification") && !itemObj.isNull("clarification")) {
            val clarRaw = itemObj.get("clarification")
            if (clarRaw is JSONObject) {
                clarificationObj = ReportMineClarification(
                    question = clarRaw.getString("question"),
                    askedAt = clarRaw.optString("askedAt", "")
                )
            } else if (clarRaw is String) {
                clarificationText = clarRaw
            }
        }

        val alertObj = if (itemObj.has("alert") && !itemObj.isNull("alert")) itemObj.optJSONObject("alert") else null
        val alert = if (alertObj != null) {
            ReportMineAlert(
                id = alertObj.getLong("id"),
                alertCode = alertObj.getString("alertCode"),
                severity = alertObj.getString("severity"),
                publishedAt = alertObj.getString("publishedAt"),
                resolvedAt = if (alertObj.isNull("resolvedAt")) null else alertObj.optString("resolvedAt").ifBlank { null }
            )
        } else null

        return ReportMineItem(
            id = id,
            reportCode = repCode,
            category = cat,
            note = note,
            state = state,
            district = district,
            city = city,
            status = status,
            createdAt = createdAt,
            linkedToReportCode = linked,
            clarification = clarificationObj,
            clarificationText = clarificationText,
            alert = alert
        )
    }
}
