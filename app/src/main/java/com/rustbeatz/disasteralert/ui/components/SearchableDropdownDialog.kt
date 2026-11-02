package com.rustbeatz.disasteralert.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.rustbeatz.disasteralert.data.IndianState
import com.rustbeatz.disasteralert.ui.theme.AppIcons

/**
 * Filterable State Dropdown component specifically engineered to filter Indian States and UTs
 * in real-time as the authority types characters on the keyboard.
 */
@Composable
fun FilterableStateSelector(
    states: List<IndianState>,
    selectedState: IndianState?,
    onStateSelected: (IndianState) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "State / Union Territory of India",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Telegram-styled clickable field
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { showDialog = true }
                .border(
                    width = 1.dp,
                    color = if (selectedState != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
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
                    text = "🇮🇳",
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (selectedState != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedState.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = selectedState.code,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${selectedState.zone} Zone • ${if (selectedState.isUnionTerritory) "Union Territory" else "State"}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Type to search State / UT...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Icon(
                    imageVector = AppIcons.KeyboardArrowDown,
                    contentDescription = "Open States Dropdown",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDialog) {
        StateSearchFilterDialog(
            allStates = states,
            selectedState = selectedState,
            onDismiss = { showDialog = false },
            onSelect = { state ->
                onStateSelected(state)
                showDialog = false
            }
        )
    }
}

/**
 * Filter modal dialog where typing on the keyboard immediately filters the list of states.
 */
@Composable
fun StateSearchFilterDialog(
    allStates: List<IndianState>,
    selectedState: IndianState?,
    onDismiss: () -> Unit,
    onSelect: (IndianState) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Real-time keyboard filtering: matches name or code
    val filteredStates = remember(searchQuery, allStates) {
        if (searchQuery.isBlank()) {
            allStates
        } else {
            val query = searchQuery.trim().lowercase()
            allStates.filter { state ->
                state.name.lowercase().contains(query) ||
                        state.code.lowercase().contains(query) ||
                        state.zone.lowercase().contains(query)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.82f)
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Select Indian State / UT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Type on keyboard to filter in real-time",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Clear,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Keyboard Search Input (Autofocused for immediate typing)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = "Type state name (e.g. Kerala, Delhi)...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
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
                                Icon(
                                    imageVector = AppIcons.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                // Request focus so keyboard opens immediately
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Result Count Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "All 36 States & Union Territories"
                        else "Matching: ${filteredStates.size} found",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = "Filter: \"$searchQuery\"",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Filtered List
                if (filteredStates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🔍", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No states matching \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Check spelling or clear keyboard filter",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredStates, key = { it.code }) { state ->
                            val isSelected = selectedState?.code == state.code

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelect(state) },
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
                                    // Code Pill
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(width = 38.dp, height = 32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = state.code,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = state.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                fontSize = 14.5.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${state.zone} Zone",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (state.isUnionTerritory) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "• UT",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = AppIcons.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
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
}
