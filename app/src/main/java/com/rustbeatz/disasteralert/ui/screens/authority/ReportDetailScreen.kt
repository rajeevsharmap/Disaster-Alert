package com.rustbeatz.disasteralert.ui.screens.authority

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rustbeatz.disasteralert.data.network.ApiException
import com.rustbeatz.disasteralert.data.repo.AnalysisChipUtils
import com.rustbeatz.disasteralert.data.repo.AnalysisRefreshController
import com.rustbeatz.disasteralert.data.repo.ConsistencyChipStyle
import com.rustbeatz.disasteralert.data.repo.DispatchRefreshController
import com.rustbeatz.disasteralert.data.repo.HazardAlertDelivery
import com.rustbeatz.disasteralert.data.repo.HazardAlertDetail
import com.rustbeatz.disasteralert.data.repo.HazardDraftRequest
import com.rustbeatz.disasteralert.data.repo.HazardDraftValidator
import com.rustbeatz.disasteralert.data.repo.ReportActionRules
import com.rustbeatz.disasteralert.data.repo.ReportAnalysis
import com.rustbeatz.disasteralert.data.repo.ReportClarificationItem
import com.rustbeatz.disasteralert.data.repo.ReportDecisionItem
import com.rustbeatz.disasteralert.data.repo.ReportDetailItem
import com.rustbeatz.disasteralert.data.repo.ReportsRepository
import com.rustbeatz.disasteralert.data.repo.SeverityChipUtils
import com.rustbeatz.disasteralert.ui.screens.reporter.StatusBadge
import com.rustbeatz.disasteralert.ui.theme.AppIcons
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun ReportDetailScreen(
    reportId: Long,
    repository: ReportsRepository,
    onNavigateBack: () -> Unit,
    onSessionExpired: () -> Unit = {},
    onOpenInboxThread: ((reporterId: Long, reporterName: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(reportId, repository) { ReportDetailViewModel(repository, reportId) }

    var report by remember { mutableStateOf<ReportDetailItem?>(null) }
    var loading by remember { mutableStateOf(true) }
    var generalErrorMessage by remember { mutableStateOf<String?>(null) }
    var isRetryingAnalysis by remember { mutableStateOf(false) }
    var retryErrorMessage by remember { mutableStateOf<String?>(null) }

    // Dialog states
    var showVerifyDialog by remember { mutableStateOf(false) }
    var verifyNote by remember { mutableStateOf("") }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectNote by remember { mutableStateOf("") }
    var rejectError by remember { mutableStateOf<String?>(null) }
    var showAskDialog by remember { mutableStateOf(false) }
    var askQuestionText by remember { mutableStateOf("") }
    var askError by remember { mutableStateOf<String?>(null) }
    var showCloseWithoutAlertDialog by remember { mutableStateOf(false) }
    var showMarkResolvedDialog by remember { mutableStateOf(false) }
    var showInboxNoticeDialog by remember { mutableStateOf(false) }

    // Feedback messages
    var actionSuccessMessage by remember { mutableStateOf<String?>(null) }
    var actionErrorMessage by remember { mutableStateOf<String?>(null) }

    fun refreshReport(showSpinner: Boolean = false) {
        if (showSpinner) loading = true
        scope.launch {
            try {
                val updated = viewModel.loadReport()
                report = updated
                generalErrorMessage = null
            } catch (e: ApiException) {
                if (e.unauthorized) {
                    onSessionExpired()
                } else {
                    generalErrorMessage = e.message
                }
            } catch (e: Exception) {
                generalErrorMessage = e.message ?: "Failed to load report"
            } finally {
                if (showSpinner) loading = false
            }
        }
    }

    LaunchedEffect(reportId) {
        refreshReport(showSpinner = true)
    }

    // Polling 1: Slice S4a AI analysis refresh (every 3 s while QUEUED or RUNNING)
    val analysisStatus = report?.analysis?.status
    LaunchedEffect(reportId, analysisStatus) {
        if (AnalysisRefreshController.isPollingStatus(analysisStatus)) {
            while (isActive) {
                delay(AnalysisRefreshController.REFRESH_INTERVAL_MS)
                try {
                    val updated = repository.getReportDetail(reportId)
                    report = updated
                    if (AnalysisRefreshController.isTerminalStatus(updated.analysis?.status)) {
                        break
                    }
                } catch (e: ApiException) {
                    if (e.unauthorized) onSessionExpired()
                    break
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    // Polling 2: Slice S4b Hazard Alert dispatch polling (every 3 s while SENDING)
    val dispatchStatus = report?.hazardAlert?.dispatchStatus
    LaunchedEffect(reportId, dispatchStatus) {
        if (DispatchRefreshController.isPollingStatus(dispatchStatus)) {
            while (isActive) {
                delay(DispatchRefreshController.REFRESH_INTERVAL_MS)
                try {
                    val alertId = report?.hazardAlert?.id
                    if (alertId != null) {
                        val refreshedAlert = repository.getHazardAlert(alertId)
                        report = report?.copy(hazardAlert = refreshedAlert)
                        if (DispatchRefreshController.isTerminalStatus(refreshedAlert.dispatchStatus)) {
                            break
                        }
                    } else {
                        break
                    }
                } catch (e: ApiException) {
                    if (e.unauthorized) onSessionExpired()
                    break
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    fun handleRetryAnalysis() {
        scope.launch {
            isRetryingAnalysis = true
            retryErrorMessage = null
            try {
                val result = repository.retryAnalysis(reportId)
                val cur = report
                if (cur != null) {
                    val updatedAnalysis = cur.analysis?.copy(status = result.analysisStatus, error = null)
                        ?: ReportAnalysis(status = result.analysisStatus)
                    report = cur.copy(analysisStatus = result.analysisStatus, analysis = updatedAnalysis)
                }
            } catch (e: ApiException) {
                if (e.unauthorized) onSessionExpired() else retryErrorMessage = e.message
            } catch (e: Exception) {
                retryErrorMessage = e.message ?: "Failed to retry analysis"
            } finally {
                isRetryingAnalysis = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (report == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = generalErrorMessage ?: "Report not found", color = EmergencyRed)
            }
        } else {
            val item = report!!

            Column(modifier = Modifier.fillMaxSize()) {

                // Top App Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = AppIcons.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = item.reportCode,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Reported by ${item.reporterName} • ${item.locationLabel}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        StatusBadge(status = item.status)
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {

                    // Banner Messages (errors/success)
                    if (actionErrorMessage != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmergencyRed.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "⚠️ ${actionErrorMessage}",
                                        fontSize = 12.sp,
                                        color = EmergencyRed,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { actionErrorMessage = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = AppIcons.Clear, contentDescription = "Dismiss", tint = EmergencyRed)
                                    }
                                }
                            }
                        }
                    }

                    if (actionSuccessMessage != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ForestGreenPrimaryLight.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreenPrimaryLight.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "✅ ${actionSuccessMessage}",
                                        fontSize = 12.sp,
                                        color = ForestGreenPrimaryLight,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { actionSuccessMessage = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = AppIcons.Clear, contentDescription = "Dismiss", tint = ForestGreenPrimaryLight)
                                    }
                                }
                            }
                        }
                    }

                    // 1. Photo & Summary Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (item.photoBytes != null && item.photoBytes.isNotEmpty()) {
                                    val bitmap = remember(item.photoBytes) {
                                        BitmapFactory.decodeByteArray(item.photoBytes, 0, item.photoBytes.size)
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Report Photo",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF13221E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = item.categoryInfo.emoji, fontSize = 38.sp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Evidence: ${item.photoUrl ?: "/api/reports/${item.id}/photo"}",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFFA7F3D0),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = item.categoryInfo.emoji, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.categoryInfo.label,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = item.formattedCreatedAt,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (!item.note.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.note,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // 2. DECISION BAR BY STATUS (Requirement 2 & 7)
                    item {
                        DecisionBarCard(
                            report = item,
                            isSubmitting = viewModel.isSubmittingDecision,
                            onVerifyClick = {
                                verifyNote = ""
                                showVerifyDialog = true
                            },
                            onRejectClick = {
                                rejectNote = ""
                                rejectError = null
                                showRejectDialog = true
                            },
                            onAskQuestionClick = {
                                askQuestionText = ""
                                askError = null
                                showAskDialog = true
                            },
                            onOpenReporterThread = {
                                if (onOpenInboxThread != null) {
                                    onOpenInboxThread(item.reporterId, item.reporterName)
                                } else {
                                    showInboxNoticeDialog = true
                                }
                            },
                            onCloseWithoutAlertClick = {
                                showCloseWithoutAlertDialog = true
                            },
                            onMarkResolvedClick = {
                                showMarkResolvedDialog = true
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // 3. ALERT DRAFT EDITOR (VERIFIED only - Requirement 4 & 5)
                    if (item.status.equals("VERIFIED", ignoreCase = true)) {
                        item {
                            AlertDraftEditorCard(
                                report = item,
                                isSaving = viewModel.isSavingDraft,
                                isPublishing = viewModel.isPublishing,
                                draftSuccessMessage = viewModel.draftSuccessMessage,
                                draftSaveError = viewModel.draftSaveError,
                                staleDraftError = viewModel.staleDraftError,
                                onSaveDraft = { draftReq ->
                                    scope.launch {
                                        try {
                                            viewModel.saveDraft(draftReq)
                                            refreshReport(showSpinner = false)
                                            actionSuccessMessage = "Hazard draft saved (v${viewModel.report?.hazardAlert?.draftVersion})"
                                        } catch (e: ApiException) {
                                            if (e.unauthorized) onSessionExpired() else actionErrorMessage = e.message
                                        } catch (e: Exception) {
                                            actionErrorMessage = e.message
                                        }
                                    }
                                },
                                onPublishClick = {
                                    viewModel.requestPublishConfirmation()
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // 4. PUBLISHED ALERT CARD (Requirement 6)
                    if (item.hazardAlert != null && (item.hazardAlert.isPublished || item.hazardAlert.isResolved || item.status == "ALERT_PUBLISHED" || item.status == "RESOLVED")) {
                        item {
                            PublishedAlertCard(
                                alert = item.hazardAlert,
                                onRefreshAlert = {
                                    scope.launch {
                                        viewModel.onPollingTick()
                                        refreshReport(showSpinner = false)
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // 5. TIMELINE & CLARIFICATIONS (Requirement 3)
                    item {
                        TimelineAndClarificationsCard(
                            decisions = item.decisions,
                            clarifications = item.clarifications
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // 6. AI ANALYSIS CARD (Slice S4a, kept intact with advisory line - Requirement 8)
                    item {
                        AiAnalysisCard(
                            report = item,
                            isRetrying = isRetryingAnalysis,
                            retryErrorMessage = retryErrorMessage,
                            onRetryAnalysis = { handleRetryAnalysis() }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // 7. EXIF AUDIT & TRUST FLAGS
                    item {
                        ExifAndTrustCard(report = item)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // -------------------------------- Dialogs --------------------------------

    // 1. Verify Dialog (note optional)
    if (showVerifyDialog) {
        AlertDialog(
            onDismissRequest = { showVerifyDialog = false },
            title = { Text("Verify Hazard Report", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Confirm that this hazard report is verified. You can add an optional verification note for the audit timeline.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = verifyNote,
                        onValueChange = { verifyNote = it },
                        label = { Text("Note (Optional)") },
                        placeholder = { Text("e.g. Confirmed with district control room") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVerifyDialog = false
                        scope.launch {
                            try {
                                viewModel.verify(verifyNote)
                                refreshReport(showSpinner = false)
                                actionSuccessMessage = "Report verified successfully"
                            } catch (e: ApiException) {
                                if (e.unauthorized) onSessionExpired() else actionErrorMessage = e.message
                            } catch (e: Exception) {
                                actionErrorMessage = e.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimaryLight)
                ) {
                    Text("VERIFY")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVerifyDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // 2. Reject Dialog (note mandatory)
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Hazard Report", fontWeight = FontWeight.Bold, color = EmergencyRed) },
            text = {
                Column {
                    Text(
                        text = "Provide a clear reason for rejecting this report. This reason will be recorded on the permanent audit log.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectNote,
                        onValueChange = {
                            rejectNote = it
                            if (it.isNotBlank()) rejectError = null
                        },
                        label = { Text("Reason (Required)") },
                        placeholder = { Text("e.g. Photo does not match the location") },
                        isError = rejectError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (rejectError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = rejectError!!, color = EmergencyRed, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectNote.isBlank()) {
                            rejectError = "Reason cannot be empty"
                            return@Button
                        }
                        showRejectDialog = false
                        scope.launch {
                            try {
                                viewModel.reject(rejectNote)
                                refreshReport(showSpinner = false)
                                actionSuccessMessage = "Report rejected"
                            } catch (e: ApiException) {
                                if (e.unauthorized) onSessionExpired() else actionErrorMessage = e.message
                            } catch (e: Exception) {
                                actionErrorMessage = e.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("REJECT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // 3. Ask Question Dialog (note mandatory)
    if (showAskDialog) {
        AlertDialog(
            onDismissRequest = { showAskDialog = false },
            title = { Text("Ask Reporter a Question", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Ask the field reporter a clarifying question. This question will appear in their private coordination thread.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = askQuestionText,
                        onValueChange = {
                            askQuestionText = it
                            if (it.isNotBlank()) askError = null
                        },
                        label = { Text("Question (Required)") },
                        placeholder = { Text("e.g. Is the road fully blocked or one lane open?") },
                        isError = askError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (askError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = askError!!, color = EmergencyRed, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (askQuestionText.isBlank()) {
                            askError = "Question cannot be empty"
                            return@Button
                        }
                        showAskDialog = false
                        scope.launch {
                            try {
                                viewModel.askQuestion(askQuestionText)
                                refreshReport(showSpinner = false)
                                actionSuccessMessage = "Question sent to reporter"
                            } catch (e: ApiException) {
                                if (e.unauthorized) onSessionExpired() else actionErrorMessage = e.message
                            } catch (e: Exception) {
                                actionErrorMessage = e.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyOrange)
                ) {
                    Text("SEND QUESTION")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAskDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // 4. Close without Alert Confirm Dialog
    if (showCloseWithoutAlertDialog) {
        AlertDialog(
            onDismissRequest = { showCloseWithoutAlertDialog = false },
            title = { Text("Close Report Without Alert?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will mark the hazard report as RESOLVED without broadcasting a public alert to citizens. Continue?",
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCloseWithoutAlertDialog = false
                        scope.launch {
                            try {
                                viewModel.closeWithoutAlert()
                                refreshReport(showSpinner = false)
                                actionSuccessMessage = "Report closed without alert"
                            } catch (e: ApiException) {
                                if (e.unauthorized) onSessionExpired() else actionErrorMessage = e.message
                            } catch (e: Exception) {
                                actionErrorMessage = e.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimaryLight)
                ) {
                    Text("CLOSE REPORT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseWithoutAlertDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // 5. Mark Resolved Dialog (with warning requirement 7)
    if (showMarkResolvedDialog) {
        AlertDialog(
            onDismissRequest = { showMarkResolvedDialog = false },
            title = { Text("Mark Report Resolved?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Citizens will not be told it is over. Continue?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMarkResolvedDialog = false
                        scope.launch {
                            try {
                                viewModel.markResolved()
                                refreshReport(showSpinner = false)
                                actionSuccessMessage = "Report marked resolved"
                            } catch (e: ApiException) {
                                if (e.unauthorized) onSessionExpired() else actionErrorMessage = e.message
                            } catch (e: Exception) {
                                actionErrorMessage = e.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimaryLight)
                ) {
                    Text("CONTINUE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkResolvedDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // 6. Publish Confirmation Dialog (Requirement 5: "Send this alert to N citizens on Telegram? This cannot be undone.")
    if (viewModel.showPublishConfirmDialog) {
        val count = report?.hazardAlert?.audienceEstimate ?: 0
        AlertDialog(
            onDismissRequest = {
                if (!viewModel.isPublishing) viewModel.dismissPublishConfirmation()
            },
            title = { Text("Publish Hazard Alert", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Send this alert to $count citizens on Telegram? This cannot be undone.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Citizens in ${report?.district}, ${report?.state} will receive this broadcast immediately.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val res = viewModel.confirmPublish()
                                refreshReport(showSpinner = false)
                                actionSuccessMessage = if (res.alreadyPublished) "Already published" else "Alert published to citizens"
                            } catch (e: ApiException) {
                                if (e.unauthorized) {
                                    onSessionExpired()
                                } else if (e.statusCode == 409) {
                                    actionErrorMessage = viewModel.staleDraftError ?: e.message
                                } else {
                                    actionErrorMessage = e.message
                                }
                            } catch (e: Exception) {
                                actionErrorMessage = e.message
                            }
                        }
                    },
                    enabled = !viewModel.isPublishing,
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyOrange)
                ) {
                    if (viewModel.isPublishing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("SEND ALERT")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissPublishConfirmation() },
                    enabled = !viewModel.isPublishing
                ) {
                    Text("CANCEL")
                }
            }
        )
    }

    // 7. Inbox Notice Dialog (if onOpenInboxThread callback not connected to navigation)
    if (showInboxNoticeDialog) {
        AlertDialog(
            onDismissRequest = { showInboxNoticeDialog = false },
            title = { Text("Reporter Coordination Thread", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Opening private thread for reporter ${report?.reporterName} (#${report?.reporterId}) in the Authority Inbox (Slice S3b).",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(onClick = { showInboxNoticeDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

/**
 * Decision bar card displayed prominently above the report detail according to status.
 */
@Composable
private fun DecisionBarCard(
    report: ReportDetailItem,
    isSubmitting: Boolean,
    onVerifyClick: () -> Unit,
    onRejectClick: () -> Unit,
    onAskQuestionClick: () -> Unit,
    onOpenReporterThread: () -> Unit,
    onCloseWithoutAlertClick: () -> Unit,
    onMarkResolvedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DECISION ACTION BAR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
                StatusBadge(status = report.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (report.status.uppercase()) {
                "PENDING_REVIEW" -> {
                    Text(
                        text = "Review this incident report and choose an operational decision:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onVerifyClick,
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimaryLight),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("VERIFY", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onAskQuestionClick,
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ASK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onRejectClick,
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("REJECT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                "NEEDS_CLARIFICATION" -> {
                    val latestClarification = report.clarifications.lastOrNull()?.question
                        ?: report.decisions.lastOrNull { it.isClarification }?.note
                        ?: "Clarification requested by authority"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmergencyOrange.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmergencyOrange.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "❓", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Inquiry: \"$latestClarification\"",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Waiting for the reporter's answer",
                                fontSize = 12.sp,
                                color = EmergencyOrange,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onOpenReporterThread,
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyOrange),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("OPEN REPORTER THREAD IN INBOX", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                "VERIFIED" -> {
                    if (report.hazardAlert == null) {
                        Text(
                            text = "Report confirmed. You can draft a public alert or close without alerting citizens:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { /* editor is shown below */ },
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyOrange),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("EDIT ALERT DRAFT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onCloseWithoutAlertClick,
                                enabled = !isSubmitting,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CLOSE NO ALERT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Report confirmed. Alert draft v${report.hazardAlert.draftVersion} prepared below.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                "ALERT_PUBLISHED" -> {
                    Text(
                        text = "Public hazard alert is live. You may mark this report resolved when conditions clear:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onMarkResolvedClick,
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimaryLight),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("MARK RESOLVED", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                "RESOLVED" -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ForestGreenPrimaryLight.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✅ Incident resolved. Report is read-only.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ForestGreenPrimaryLight,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                "REJECTED" -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmergencyRed.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🚫 Report rejected. Read-only.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = EmergencyRed,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                else -> {
                    Text(
                        text = "Status: ${report.status}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Alert Draft Editor Card (VERIFIED reports only - Requirement 4 & 5).
 */
@Composable
private fun AlertDraftEditorCard(
    report: ReportDetailItem,
    isSaving: Boolean,
    isPublishing: Boolean,
    draftSuccessMessage: String?,
    draftSaveError: String?,
    staleDraftError: String?,
    onSaveDraft: (HazardDraftRequest) -> Unit,
    onPublishClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val existingDraft = report.hazardAlert

    var hazardType by remember(report.id, existingDraft) {
        mutableStateOf(existingDraft?.hazardType ?: report.analysis?.hazard ?: report.categoryInfo.label)
    }
    var severity by remember(report.id, existingDraft) {
        mutableStateOf(existingDraft?.severity ?: report.analysis?.severity ?: "ORANGE")
    }
    val state = report.state
    val district = report.district
    var city by remember(report.id, existingDraft) {
        mutableStateOf(existingDraft?.city ?: report.city ?: "")
    }
    var title by remember(report.id, existingDraft) {
        mutableStateOf(existingDraft?.title ?: report.analysis?.draftAlert?.title ?: "")
    }
    var message by remember(report.id, existingDraft) {
        mutableStateOf(existingDraft?.message ?: report.analysis?.draftAlert?.message ?: "")
    }
    var instructions by remember(report.id, existingDraft) {
        val initialInstr = existingDraft?.instructions
            ?: report.analysis?.draftAlert?.instructions?.joinToString("\n")
            ?: ""
        mutableStateOf(initialInstr)
    }
    var validUntil by remember(report.id, existingDraft) {
        mutableStateOf(existingDraft?.validUntil ?: "")
    }

    var localValidationError by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📢", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HAZARD ALERT DRAFT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                }

                if (existingDraft != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmergencyOrange.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Draft v${existingDraft.draftVersion}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmergencyOrange,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Audience estimate banner
            val audienceCount = existingDraft?.audienceEstimate ?: 42
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "👥 $audienceCount registered citizens in $district, $state will receive this",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }

            if (staleDraftError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmergencyRed.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmergencyRed)
                ) {
                    Text(
                        text = "⚠️ $staleDraftError",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmergencyRed,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Field 1: Hazard Type (max 60)
            OutlinedTextField(
                value = hazardType,
                onValueChange = { if (it.length <= 60) hazardType = it },
                label = { Text("Hazard Type (max 60)") },
                supportingText = { Text("${hazardType.length}/60") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Field 2: Severity Chips (RED, ORANGE, YELLOW, GREEN)
            Text(text = "Severity Level:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("RED", "ORANGE", "YELLOW", "GREEN").forEach { sev ->
                    FilterChip(
                        selected = severity.equals(sev, ignoreCase = true),
                        onClick = { severity = sev },
                        label = { Text(sev) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SeverityChipUtils.getSeverityColors(sev).containerColor,
                            selectedLabelColor = SeverityChipUtils.getSeverityColors(sev).textColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Field 3: Title (1-120)
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 120) title = it },
                label = { Text("Alert Title (1-120)") },
                supportingText = { Text("${title.length}/120") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Field 4: Message (1-1000)
            OutlinedTextField(
                value = message,
                onValueChange = { if (it.length <= 1000) message = it },
                label = { Text("Message Body (1-1000)") },
                supportingText = { Text("${message.length}/1000") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Field 5: Instructions (one string, one line per bullet, max 1000)
            OutlinedTextField(
                value = instructions,
                onValueChange = { if (it.length <= 1000) instructions = it },
                label = { Text("Instructions (one line per bullet, max 1000)") },
                supportingText = { Text("${instructions.length}/1000") },
                placeholder = { Text("Do not use this road\nUse alternate coastal route") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            if (localValidationError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = localValidationError!!, color = EmergencyRed, fontSize = 11.5.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions: Save Draft & Publish
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val validation = HazardDraftValidator.validate(
                            hazardType = hazardType,
                            severity = severity,
                            title = title,
                            message = message,
                            instructions = instructions
                        )
                        if (!validation.isValid) {
                            localValidationError = validation.firstError
                            return@Button
                        }
                        localValidationError = null
                        onSaveDraft(
                            HazardDraftRequest(
                                hazardType = hazardType.trim(),
                                severity = severity.uppercase(),
                                state = state,
                                district = district,
                                city = city.ifBlank { null },
                                title = title.trim(),
                                message = message.trim(),
                                instructions = instructions.trim().ifBlank { null },
                                validUntil = validUntil.ifBlank { null }
                            )
                        )
                    },
                    enabled = !isSaving && !isPublishing,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("SAVE DRAFT")
                }

                if (existingDraft != null) {
                    Button(
                        onClick = onPublishClick,
                        enabled = !isSaving && !isPublishing,
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("PUBLISH ALERT")
                    }
                }
            }
        }
    }
}

/**
 * Published Alert Card (Requirement 6).
 */
@Composable
private fun PublishedAlertCard(
    alert: HazardAlertDetail,
    onRefreshAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📡", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PUBLISHED HAZARD ALERT",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimaryLight,
                        letterSpacing = 0.8.sp
                    )
                }

                SeverityChip(severity = alert.severity)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${alert.alertCode} • ${alert.hazardType}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Approved by: ${alert.approvedBy ?: "Authority Officer"}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            alert.formattedPublishedAt?.let {
                Text(
                    text = "Published at: $it",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            // Dispatch and Delivery status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Dispatch: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = alert.dispatchStatus ?: "UNKNOWN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (alert.dispatchStatus?.uppercase()) {
                            "COMPLETED" -> ForestGreenPrimaryLight
                            "SENDING" -> EmergencyOrange
                            "COMPLETED_WITH_FAILURES" -> EmergencyRed
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                if (alert.dispatchStatus == "SENDING") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = EmergencyOrange)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Refreshing 3s", fontSize = 10.5.sp, color = EmergencyOrange)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Delivery Counts: Sent / Failed / Pending
            val del = alert.delivery ?: HazardAlertDelivery(0, 0, 0)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ForestGreenPrimaryLight.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${del.sent}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ForestGreenPrimaryLight)
                        Text(text = "Sent", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (del.failed > 0) EmergencyRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${del.failed}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (del.failed > 0) EmergencyRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = "Failed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (del.pending > 0) EmergencyOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${del.pending}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (del.pending > 0) EmergencyOrange else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = "Pending", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (alert.dispatchStatus == "COMPLETED_WITH_FAILURES") {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmergencyRed.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "⚠️ Delivery completed with failures (${del.failed} citizens did not receive). Redelivery available in S4c.",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmergencyRed,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Decisions Timeline & Clarifications Card (Requirement 3).
 */
@Composable
private fun TimelineAndClarificationsCard(
    decisions: List<ReportDecisionItem>,
    clarifications: List<ReportClarificationItem>,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Decisions Timeline
            Text(
                text = "AUDIT TIMELINE & DECISIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (decisions.isEmpty()) {
                Text(
                    text = "No officer review decisions recorded yet.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                decisions.forEachIndexed { index, dec ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Timeline dot & line
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = ReportActionRules.getStatusColor(dec.decision),
                                modifier = Modifier.size(10.dp)
                            ) {}
                            if (index < decisions.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(36.dp)
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dec.decision,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = ReportActionRules.getStatusColor(dec.decision)
                                )
                                Text(
                                    text = dec.formattedDecidedAt,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!dec.decidedBy.isNullOrBlank()) {
                                Text(
                                    text = "By ${dec.decidedBy}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (!dec.note.isNullOrBlank()) {
                                Text(
                                    text = "\"${dec.note}\"",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }

            // Clarifications section
            if (clarifications.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "CLARIFICATIONS & QUESTIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                clarifications.forEach { clar ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Q: ${clar.question}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Asked by ${clar.askedBy ?: "Officer"} • ${clar.formattedAskedAt}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (clar.isAnswered) {
                                Text(
                                    text = "A: ${clar.answer}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ForestGreenPrimaryLight
                                )
                                clar.formattedAnsweredAt?.let {
                                    Text(
                                        text = "Answered at $it",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(
                                    text = "⏳ No answer yet",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EmergencyOrange
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * EXIF & Trust Badges Card.
 */
@Composable
private fun ExifAndTrustCard(report: ReportDetailItem) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "EXIF METADATA & TRUST AUDIT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (report.exif != null) {
                ExifAuditRow("EXIF GPS Tag:", if (report.exif.hasGps) "Present" else "Missing")
                if (report.exif.hasGps && report.exif.latitude != null) {
                    ExifAuditRow("EXIF Coordinates:", "${report.exif.latitude}, ${report.exif.longitude}")
                    ExifAuditRow("EXIF Captured At:", report.exif.takenAt ?: "Unknown")
                    if (report.exif.distanceToReportedM != null) {
                        ExifAuditRow("GPS vs EXIF Delta:", "${"%.0f".format(report.exif.distanceToReportedM)} meters")
                    }
                }
            }

            if (report.trustFlags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    report.trustFlags.forEach { flag ->
                        TrustFlagChip(flag = flag)
                    }
                }
            }
        }
    }
}

/**
 * AI Analysis Card (Slice S4a, kept intact - Requirement 8).
 */
@Composable
fun AiAnalysisCard(
    report: ReportDetailItem,
    isRetrying: Boolean,
    retryErrorMessage: String?,
    onRetryAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val analysis = report.analysis ?: return
    var draftAlertExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🤖", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI ANALYSIS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                }

                if (analysis.isSimulated) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmergencyOrange.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmergencyOrange.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "SIMULATED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmergencyOrange,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (analysis.status.uppercase()) {
                "QUEUED", "RUNNING" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (analysis.status == "RUNNING") "Analysis in progress..." else "Analysis queued...",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                "FAILED" -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmergencyRed.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⚠️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Analysis Failed",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = EmergencyRed
                                )
                            }
                            analysis.error?.let { err ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = err, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onRetryAnalysis,
                                enabled = !isRetrying,
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("RETRY ANALYSIS", fontSize = 11.5.sp)
                            }
                        }
                    }
                }

                "SKIPPED" -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "AI analysis skipped: ${analysis.error ?: "No AI provider configured"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                "COMPLETED" -> {
                    analysis.sceneSummary?.let { summary ->
                        Text(
                            text = summary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        analysis.severity?.let { sev ->
                            SeverityChip(severity = sev)
                        }
                        analysis.consistency?.let { cons ->
                            ConsistencyChip(consistency = cons)
                        }
                        analysis.confidencePercentage?.let { conf ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = "CONF: $conf%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (analysis.caveats.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Caveats:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        analysis.caveats.forEach { cav ->
                            Text(text = "• $cav", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Mandatory advisory notice line (Requirement 8)
            Text(
                text = "ℹ️ AI analysis is automated and advisory only. Authority officers verify all facts before taking action.",
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun SeverityChip(severity: String) {
    val colors = SeverityChipUtils.getSeverityColors(severity)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colors.containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.textColor.copy(alpha = 0.4f))
    ) {
        Text(
            text = severity.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ConsistencyChip(consistency: String) {
    val style = AnalysisChipUtils.getConsistencyStyle(consistency)
    val label = AnalysisChipUtils.getConsistencyLabel(consistency)

    val (textColor, bgColor, borderAlpha) = when (style) {
        ConsistencyChipStyle.WARNING -> {
            when (consistency.uppercase()) {
                "INCONSISTENT" -> Triple(EmergencyRed, EmergencyRedContainerLight, 0.6f)
                "UNCLEAR" -> Triple(EmergencyOrange, EmergencyOrangeContainerLight, 0.6f)
                else -> Triple(EmergencyYellow, EmergencyYellowContainerLight, 0.6f)
            }
        }
        ConsistencyChipStyle.NORMAL -> {
            when (consistency.uppercase()) {
                "CONSISTENT" -> Triple(ForestGreenPrimaryLight, SoftMintContainerLight, 0.4f)
                "PARTIAL" -> Triple(EmergencyBlue, EmergencyBlueContainerLight, 0.4f)
                else -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant, 0.3f)
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = borderAlpha))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (style == ConsistencyChipStyle.WARNING) {
                Text(text = "⚠️ ", fontSize = 10.sp)
            }
            Text(
                text = label.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
private fun ExifAuditRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
