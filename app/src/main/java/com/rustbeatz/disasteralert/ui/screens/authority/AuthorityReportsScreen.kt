package com.rustbeatz.disasteralert.ui.screens.authority

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rustbeatz.disasteralert.data.repo.ReportCategories
import com.rustbeatz.disasteralert.data.repo.ReportDetailItem
import com.rustbeatz.disasteralert.data.repo.ReportsRepository
import com.rustbeatz.disasteralert.ui.screens.reporter.StatusBadge
import com.rustbeatz.disasteralert.ui.theme.AppIcons
import com.rustbeatz.disasteralert.ui.theme.EmergencyOrange
import com.rustbeatz.disasteralert.ui.theme.EmergencyRed
import com.rustbeatz.disasteralert.ui.theme.EmergencyYellow
import kotlinx.coroutines.launch

/**
 * Authority review queue of field incident reports.
 * Filter by category, view trust-flag hints, timestamps in IST, and tap to view full detail & EXIF.
 */
@Composable
fun AuthorityReportsScreen(
    repository: ReportsRepository,
    onNavigateBack: () -> Unit,
    onReportClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val reports = remember { mutableStateListOf<ReportDetailItem>() }
    var loading by remember { mutableStateOf(true) }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val statusFilters = listOf(
        null to "All",
        "PENDING_REVIEW" to "Pending review",
        "NEEDS_CLARIFICATION" to "Needs clarification",
        "VERIFIED" to "Verified",
        "ALERT_PUBLISHED" to "Alert published",
        "RESOLVED" to "Resolved",
        "REJECTED" to "Rejected"
    )

    fun loadReports() {
        loading = true
        scope.launch {
            try {
                val list = repository.getAuthorityReports(
                    category = selectedCategoryFilter,
                    status = selectedStatusFilter
                )
                reports.clear()
                reports.addAll(list)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(selectedCategoryFilter, selectedStatusFilter) {
        loadReports()
    }

    val filteredReports = run {
        var list = reports.filter { report ->
            (selectedStatusFilter == null || report.status.equals(selectedStatusFilter, ignoreCase = true)) &&
            (searchQuery.isBlank() ||
                report.reportCode.contains(searchQuery, ignoreCase = true) ||
                report.categoryInfo.label.contains(searchQuery, ignoreCase = true) ||
                report.reporterName.contains(searchQuery, ignoreCase = true) ||
                (report.city?.contains(searchQuery, ignoreCase = true) == true) ||
                report.district.contains(searchQuery, ignoreCase = true))
        }
        if (selectedStatusFilter.equals("PENDING_REVIEW", ignoreCase = true)) {
            list = list.sortedByDescending { it.createdAt }
        }
        list
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Bar
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
                                text = "Field Incident Reports",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Authority Operational Queue",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(onClick = { loadReports() }) {
                        Icon(
                            imageVector = AppIcons.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by code, category, location or reporter...") },
                    leadingIcon = {
                        Icon(imageVector = AppIcons.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = AppIcons.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Status Filter Chips Carousel (Requirement 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusFilters.forEach { (statusVal, label) ->
                    val isSelected = selectedStatusFilter == statusVal
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedStatusFilter = statusVal },
                        label = {
                            Text(
                                text = if (statusVal == "PENDING_REVIEW") "⏳ $label" else label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Category Filter Chips Carousel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = null },
                    label = { Text("All Reports (${reports.size})") },
                    shape = RoundedCornerShape(10.dp)
                )

                ReportCategories.ALL.forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat.id,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == cat.id) null else cat.id
                        },
                        label = { Text("${cat.emoji} ${cat.label}") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Reports List
            if (loading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filteredReports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🛡️", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No reports found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(filteredReports, key = { it.id }) { item ->
                        AuthorityReportCard(
                            report = item,
                            onClick = { onReportClick(item.id) },
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorityReportCard(
    report: ReportDetailItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = report.categoryInfo.emoji, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = report.categoryInfo.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${report.reportCode} • ${report.reporterName} (#${report.reporterId})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (report.analysisStatus.equals("FAILED", ignoreCase = true)) {
                        Icon(
                            imageVector = AppIcons.Warning,
                            contentDescription = "Analysis Failed",
                            tint = EmergencyRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    StatusBadge(status = report.status)
                }
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

            // Trust Flags row if present
            if (report.trustFlags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    report.trustFlags.forEach { flag ->
                        TrustFlagChip(flag = flag)
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
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = report.formattedCreatedAt,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TrustFlagChip(flag: String) {
    val (label, color) = when (flag) {
        "EXIF_MISSING" -> Pair("EXIF Missing", EmergencyYellow)
        "GPS_MISMATCH" -> Pair("GPS Mismatch", EmergencyRed)
        "OLD_PHOTO" -> Pair("Photo >30m", EmergencyOrange)
        "MOCK_LOCATION" -> Pair("Mock GPS", EmergencyRed)
        else -> Pair(flag, Color.Gray)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.14f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = "⚠️ $label",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
