package com.rustbeatz.disasteralert.ui.screens.reporter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rustbeatz.disasteralert.data.network.Session
import com.rustbeatz.disasteralert.data.repo.ReportMineItem
import com.rustbeatz.disasteralert.data.repo.ReportStatusUtils
import com.rustbeatz.disasteralert.data.repo.ReportsRepository
import com.rustbeatz.disasteralert.ui.theme.AppIcons
import com.rustbeatz.disasteralert.ui.theme.EmergencyBlue
import com.rustbeatz.disasteralert.ui.theme.EmergencyOrange
import com.rustbeatz.disasteralert.ui.theme.EmergencyRed
import com.rustbeatz.disasteralert.ui.theme.EmergencyYellow
import com.rustbeatz.disasteralert.ui.theme.ForestGreenPrimaryLight
import kotlinx.coroutines.launch

enum class ReporterTab {
    GROUP,
    AUTHORITY,
    HISTORY
}

/**
 * Three-tab shell for Field Reporters as specified in section 14.13 & 14.14:
 * 1. Group: District reporter group chat (placeholder in S3a, active in S3b)
 * 2. Authority: Private coordination thread (placeholder) + "NEW REPORT" action button
 * 3. History: Live list of reports filed by this reporter (status chips, category icons, timestamps)
 */
@Composable
fun ReporterShellScreen(
    repository: ReportsRepository,
    onNavigateToNewReport: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ReporterTab.HISTORY) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar for Reporter
            ReporterTopAppBar(
                reporterName = Session.officerName ?: "Field Reporter",
                district = Session.district ?: "Assigned District",
                state = Session.state ?: "India",
                onLogout = onLogout
            )

            // Screen Content by Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    ReporterTab.GROUP -> {
                        ReporterGroupTabPlaceholder(
                            district = Session.district ?: "Udupi",
                            state = Session.state ?: "Karnataka"
                        )
                    }
                    ReporterTab.AUTHORITY -> {
                        ReporterAuthorityTab(
                            district = Session.district ?: "Udupi",
                            onNewReportClick = onNavigateToNewReport
                        )
                    }
                    ReporterTab.HISTORY -> {
                        ReporterHistoryTab(
                            repository = repository,
                            onNewReportClick = onNavigateToNewReport,
                            onOpenAuthorityChat = { selectedTab = ReporterTab.AUTHORITY }
                        )
                    }
                }
            }

            // Bottom Navigation Bar
            ReporterBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    }
}

@Composable
private fun ReporterTopAppBar(
    reporterName: String,
    district: String,
    state: String,
    onLogout: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "📡", fontSize = 20.sp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = reporterName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "REPORTER",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Sector: $district, $state",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onLogout,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Clear,
                    contentDescription = "Sign out",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Tab 1: Group Chat Placeholder
 */
@Composable
private fun ReporterGroupTabPlaceholder(
    district: String,
    state: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "👥", fontSize = 32.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "$district District Channel",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$district, $state",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "Coming in the next update (Slice S3b)\n\nReporters in $district will coordinate directly with field cards, hazard notices, and sector text.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tab 2: Authority Coordination Channel + Prominent "New Report" Action
 */
@Composable
private fun ReporterAuthorityTab(
    district: String,
    onNewReportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "🏛️", fontSize = 28.sp)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Authority Coordination Desk",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$district Emergency Operations Center",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Private thread with District Authority (Slice S3b).\nSubmitted reports, clarification inquiries, and restoration instructions will appear here.",
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Prominent New Report Card / Button
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FIELD INCIDENT DISPATCH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Encountered a blocked road, flood, landslide or relief need?",
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNewReportClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Camera,
                            contentDescription = "Camera",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "NEW REPORT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 3: Live History of Reports Filed by Reporter
 */
@Composable
private fun ReporterHistoryTab(
    repository: ReportsRepository,
    onNewReportClick: () -> Unit,
    onOpenAuthorityChat: () -> Unit
) {
    val reports = remember { mutableStateListOf<ReportMineItem>() }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        loading = true
        errorMessage = null
        scope.launch {
            try {
                val list = repository.getMyReports()
                reports.clear()
                reports.addAll(list)
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load reports"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (errorMessage != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = errorMessage!!,
                    color = EmergencyRed,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { loadData() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("RETRY")
                }
            }
        } else if (reports.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "📋", fontSize = 44.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Reports Filed Yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Your geotagged incident reports will appear here with live verification status.",
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onNewReportClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("FILE FIRST REPORT")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MY REPORTS (${reports.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = { loadData() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                items(reports, key = { it.id }) { report ->
                    ReportCardItem(
                        report = report,
                        onOpenAuthorityChat = onOpenAuthorityChat,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportCardItem(
    report: ReportMineItem,
    onOpenAuthorityChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = report.categoryInfo.emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = report.categoryInfo.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = report.reportCode,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(status = report.status)
            }

            if (!report.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = report.note,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Slice S4b: NEEDS_CLARIFICATION inquiry box and button to Authority chat tab (Requirement 9)
            if (report.status.equals("NEEDS_CLARIFICATION", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(10.dp))
                val qText = report.questionText ?: "Clarification requested by authority"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmergencyOrange.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmergencyOrange.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "❓", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Inquiry: \"$qText\"",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (report.clarification != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Asked at ${report.clarification.formattedAskedAt}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onOpenAuthorityChat,
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                        ) {
                            Text("CHAT WITH AUTHORITY", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📍 ${report.locationLabel}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = report.formattedTime,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Slice S4b: ALERT_PUBLISHED and RESOLVED show alert code, severity and times (Requirement 9)
            if (report.alert != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmergencyOrange.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmergencyOrange.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "📢", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${report.alert.alertCode} • ${report.alert.severity}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmergencyOrange
                                )
                            }
                            Text(
                                text = "Pub: ${report.alert.formattedPublishedAt}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (report.alert.resolvedAt != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Resolved at ${report.alert.formattedResolvedAt}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = ForestGreenPrimaryLight
                            )
                        }
                    }
                }
            }

            // Duplicate link info
            if (!report.linkedToReportCode.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🔗 Linked to ${report.linkedToReportCode}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status.uppercase()) {
        "SUBMITTED" -> Pair(EmergencyBlue.copy(alpha = 0.14f), EmergencyBlue)
        "PENDING_REVIEW" -> Pair(EmergencyYellow.copy(alpha = 0.18f), EmergencyYellow)
        "NEEDS_CLARIFICATION" -> Pair(EmergencyOrange.copy(alpha = 0.18f), EmergencyOrange)
        "VERIFIED" -> Pair(ForestGreenPrimaryLight.copy(alpha = 0.15f), ForestGreenPrimaryLight)
        "ALERT_PUBLISHED" -> Pair(EmergencyOrange.copy(alpha = 0.18f), EmergencyOrange)
        "RESOLVED" -> Pair(Color(0xFF10B981).copy(alpha = 0.18f), Color(0xFF047857))
        "REJECTED" -> Pair(Color(0xFF6B7280).copy(alpha = 0.15f), Color(0xFF4B5563))
        "UNDER_REVIEW" -> Pair(EmergencyYellow.copy(alpha = 0.18f), EmergencyYellow)
        "CLOSED" -> Pair(Color(0xFF6B7280).copy(alpha = 0.15f), Color(0xFF4B5563))
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = ReportStatusUtils.getDisplayLabel(status).uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ReporterBottomNavBar(
    selectedTab: ReporterTab,
    onTabSelected: (ReporterTab) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReporterNavItem(
                icon = AppIcons.Group,
                label = "Group",
                isSelected = selectedTab == ReporterTab.GROUP,
                onClick = { onTabSelected(ReporterTab.GROUP) },
                modifier = Modifier.weight(1f)
            )

            ReporterNavItem(
                icon = AppIcons.Shield,
                label = "Authority",
                isSelected = selectedTab == ReporterTab.AUTHORITY,
                onClick = { onTabSelected(ReporterTab.AUTHORITY) },
                modifier = Modifier.weight(1f)
            )

            ReporterNavItem(
                icon = AppIcons.History,
                label = "History",
                isSelected = selectedTab == ReporterTab.HISTORY,
                onClick = { onTabSelected(ReporterTab.HISTORY) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ReporterNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            modifier = Modifier.size(width = 50.dp, height = 28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
