package com.rustbeatz.disasteralert.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rustbeatz.disasteralert.data.repo.ResponseItem
import com.rustbeatz.disasteralert.data.repo.ResponseTotals
import com.rustbeatz.disasteralert.ui.theme.AppIcons
import com.rustbeatz.disasteralert.ui.theme.EmergencyOrange
import com.rustbeatz.disasteralert.ui.theme.EmergencyRed

@Composable
fun ResponsesBottomSheetContent(
    items: List<ResponseItem>,
    totals: ResponseTotals?,
    selectedNeedFilter: String?,
    onToggleNeedFilter: (String) -> Unit,
    onClearNeedFilter: () -> Unit,
    onRowClick: (ResponseItem) -> Unit,
    onNavigateClick: (ResponseItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Drag Handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 4.5.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(2.5.dp)
                    )
            )
        }

        // Sheet Header: Title + Response Count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Help & Safety Responses",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${items.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (selectedNeedFilter != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClearNeedFilter() }
                ) {
                    Text(
                        text = "Clear Filter ✕",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Needs Filter Pills (Ergonomic placement right in sheet header)
        if (totals != null) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    NeedFilterPill(
                        label = "All (${items.size})",
                        isSelected = selectedNeedFilter == null,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onClearNeedFilter
                    )
                }
                item {
                    NeedFilterPill(
                        label = "Rescue (${totals.byNeed.emergency})",
                        isSelected = selectedNeedFilter.equals("EMERGENCY", ignoreCase = true),
                        color = Color(0xFF991B1B),
                        onClick = { onToggleNeedFilter("EMERGENCY") }
                    )
                }
                item {
                    NeedFilterPill(
                        label = "Medicine (${totals.byNeed.medical})",
                        isSelected = selectedNeedFilter.equals("MEDICAL", ignoreCase = true),
                        color = EmergencyRed,
                        onClick = { onToggleNeedFilter("MEDICAL") }
                    )
                }
                item {
                    NeedFilterPill(
                        label = "Food/Water (${totals.byNeed.foodWater})",
                        isSelected = selectedNeedFilter.equals("FOOD_WATER", ignoreCase = true),
                        color = EmergencyOrange,
                        onClick = { onToggleNeedFilter("FOOD_WATER") }
                    )
                }
                item {
                    NeedFilterPill(
                        label = "Shelter (${totals.byNeed.shelter})",
                        isSelected = selectedNeedFilter.equals("SHELTER", ignoreCase = true),
                        color = Color(0xFF2563EB),
                        onClick = { onToggleNeedFilter("SHELTER") }
                    )
                }
                item {
                    NeedFilterPill(
                        label = "Other (${totals.byNeed.other})",
                        isSelected = selectedNeedFilter.equals("OTHER", ignoreCase = true),
                        color = Color(0xFF6B7280),
                        onClick = { onToggleNeedFilter("OTHER") }
                    )
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            modifier = Modifier.padding(top = 4.dp)
        )

        // List in exact order returned by API
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedNeedFilter != null) "No responses matching $selectedNeedFilter" else "No help requests yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.replyId }) { item ->
                    ResponseRowItem(
                        item = item,
                        onClick = { onRowClick(item) },
                        onNavigateClick = { onNavigateClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NeedFilterPill(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) color else color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) color else color.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun ResponseRowItem(
    item: ResponseItem,
    onClick: () -> Unit,
    onNavigateClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Row 1: Name, SIMULATED badge, and Need Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (item.isSimulated) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFEF3C7),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Text(
                                text = "SIMULATED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB45309),
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // Need Tag Pill
                val pillBg = when (item.need?.uppercase()) {
                    "EMERGENCY" -> Color(0xFF991B1B)
                    "MEDICAL" -> EmergencyRed
                    "FOOD_WATER" -> EmergencyOrange
                    "SHELTER" -> Color(0xFF2563EB)
                    "OTHER", "UNSPECIFIED" -> Color(0xFF6B7280)
                    else -> if (item.response == "SAFE") Color(0xFF10B981) else Color(0xFF6B7280)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = pillBg
                ) {
                    Text(
                        text = item.friendlyNeedLabel,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Note text if present
            if (!item.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${item.note}\"",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Metadata (City, Accuracy, Time in IST) + Navigate Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!item.city.isNullOrBlank()) {
                            Text(
                                text = "📍 ${item.city}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "🕒 ${item.istTime}",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    if (item.hasLocation) {
                        Text(
                            text = if (item.accuracyM != null) "🎯 Accuracy ±${item.accuracyM.toInt()}m" else "🎯 Accuracy unknown",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Navigate Button: Only when coordinates exist
                if (item.hasLocation) {
                    OutlinedButton(
                        onClick = onNavigateClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Navigation,
                            contentDescription = "Navigate",
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Navigate",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
