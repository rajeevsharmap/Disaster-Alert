package com.rustbeatz.disasteralert.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rustbeatz.disasteralert.data.DisasterType
import com.rustbeatz.disasteralert.ui.theme.AppIcons

/**
 * Disaster Type Dropdown with modal selection of disaster classifications.
 */
@Composable
fun FilterableDisasterTypeSelector(
    disasterTypes: List<DisasterType>,
    selectedType: DisasterType?,
    onTypeSelected: (DisasterType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Disaster Classification Type",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { showDialog = true }
                .border(
                    width = 1.dp,
                    color = if (selectedType != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(14.dp)
                ),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedType?.emoji ?: "⚠️",
                    fontSize = 22.sp
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (selectedType != null) {
                        Text(
                            text = selectedType.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${selectedType.category} • ${selectedType.description}",
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Select Disaster Type (Flood, Cyclone, Quake)...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Icon(
                    imageVector = AppIcons.KeyboardArrowDown,
                    contentDescription = "Open Disaster Types",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDialog) {
        DisasterTypeDialog(
            disasterTypes = disasterTypes,
            selectedType = selectedType,
            onDismiss = { showDialog = false },
            onSelect = {
                onTypeSelected(it)
                showDialog = false
            }
        )
    }
}

@Composable
private fun DisasterTypeDialog(
    disasterTypes: List<DisasterType>,
    selectedType: DisasterType?,
    onDismiss: () -> Unit,
    onSelect: (DisasterType) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, disasterTypes) {
        if (searchQuery.isBlank()) disasterTypes
        else disasterTypes.filter {
            it.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.category.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.80f)
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Select Disaster Type",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Official emergency hazard classification",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = AppIcons.Clear,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Search hazard type (e.g. Flood, Cyclone)...", fontSize = 13.5.sp)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = AppIcons.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(filtered, key = { it.id }) { item ->
                        val isSelected = selectedType?.id == item.id

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(item) },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = item.emoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = item.description,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = AppIcons.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Filterable District Selector: Adapts dynamically based on the selected state,
 * with real-time keyboard filtering of districts.
 */
@Composable
fun FilterableDistrictSelector(
    districts: List<String>,
    selectedDistrict: String,
    onDistrictSelected: (String) -> Unit,
    stateName: String?,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "District ${if (!stateName.isNullOrBlank()) "($stateName)" else ""}",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                    if (districts.isNotEmpty()) showDialog = true
                }
                .border(
                    width = 1.dp,
                    color = if (selectedDistrict.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(14.dp)
                ),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "📍", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (selectedDistrict.isNotBlank()) {
                        Text(
                            text = selectedDistrict,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Target Administrative District",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = if (districts.isEmpty()) "Select a State first to view districts"
                            else "Select District (e.g. Wayanad, Pune)...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Icon(
                    imageVector = AppIcons.KeyboardArrowDown,
                    contentDescription = "Open District Dropdown",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDialog && districts.isNotEmpty()) {
        DistrictFilterDialog(
            districts = districts,
            selectedDistrict = selectedDistrict,
            stateName = stateName ?: "State",
            onDismiss = { showDialog = false },
            onSelect = {
                onDistrictSelected(it)
                showDialog = false
            }
        )
    }
}

@Composable
private fun DistrictFilterDialog(
    districts: List<String>,
    selectedDistrict: String,
    stateName: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val filtered = remember(searchQuery, districts) {
        if (searchQuery.isBlank()) districts
        else districts.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.78f)
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Districts of $stateName",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Filter district name on keyboard",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = AppIcons.Clear,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("Type district name...", fontSize = 13.5.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = AppIcons.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = AppIcons.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(filtered) { district ->
                        val isSelected = selectedDistrict == district

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(district) },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = district,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 14.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isSelected) {
                                    Icon(
                                        imageVector = AppIcons.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
