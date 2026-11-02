package com.rustbeatz.disasteralert.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.rustbeatz.disasteralert.data.network.ApiException
import com.rustbeatz.disasteralert.data.network.BackendApi
import com.rustbeatz.disasteralert.data.network.DispatchRequest
import com.rustbeatz.disasteralert.data.network.DispatchResult
import com.rustbeatz.disasteralert.data.network.Session
import kotlinx.coroutines.launch
import com.rustbeatz.disasteralert.data.AlertSeverity
import com.rustbeatz.disasteralert.data.DisasterBroadcastRecord
import com.rustbeatz.disasteralert.data.DisasterRepository
import com.rustbeatz.disasteralert.data.DisasterType
import com.rustbeatz.disasteralert.data.IndianState
import com.rustbeatz.disasteralert.ui.common.SafeAreaContainer
import com.rustbeatz.disasteralert.ui.components.FilterableDisasterTypeSelector
import com.rustbeatz.disasteralert.ui.components.FilterableDistrictSelector
import com.rustbeatz.disasteralert.ui.components.FilterableStateSelector
import com.rustbeatz.disasteralert.ui.theme.AppIcons
import com.rustbeatz.disasteralert.ui.theme.EmergencyOrange
import com.rustbeatz.disasteralert.ui.theme.EmergencyRed
import com.rustbeatz.disasteralert.ui.theme.EmergencyYellow

@Composable
fun AuthorityDisasterBroadcastScreen(
    onNavigateBack: () -> Unit,
    onBroadcastDispatched: (DisasterBroadcastRecord) -> Unit,
    onSessionExpired: () -> Unit = {},
    onViewResponseMap: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Form States
    var selectedDisasterType by remember { mutableStateOf<DisasterType?>(null) }
    var selectedState by remember { mutableStateOf<IndianState?>(null) }
    var selectedDistrict by remember { mutableStateOf("") }
    var cityLocality by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf(AlertSeverity.RED) }
    var instructionsText by remember {
        mutableStateOf("Urgent emergency warning issued for the designated sector. Local authorities, civil defense, and citizens must follow emergency protocols and evacuate low-lying / high-risk zones immediately.")
    }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showSuccessConfirmation by remember { mutableStateOf(false) }
    var lastDispatchedRecord by remember { mutableStateOf<DisasterBroadcastRecord?>(null) }
    var lastResult by remember { mutableStateOf<DispatchResult?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var reachCount by remember { mutableStateOf<Int?>(null) }
    var reachLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Dynamic districts based on selected state
    val availableDistricts = remember(selectedState) {
        if (selectedState == null) emptyList()
        else DisasterRepository.stateDistrictsMap[selectedState?.name] ?: listOf("${selectedState?.name} Central District")
    }

    // Ask the backend how many registered citizens this State + District would reach
    LaunchedEffect(selectedState, selectedDistrict) {
        val state = selectedState
        if (state == null || selectedDistrict.isBlank()) {
            reachCount = null
            return@LaunchedEffect
        }
        reachLoading = true
        reachCount = null
        try {
            reachCount = BackendApi.targetCount(state.name, selectedDistrict)
        } catch (e: ApiException) {
            if (e.unauthorized) onSessionExpired()
        } finally {
            reachLoading = false
        }
    }

    SafeAreaContainer(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Telegram-style Top Header Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = AppIcons.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Authority Incident Dispatch",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🔒",
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "Govt. of India • NDMA & SDMA Network",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Authority badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "ACTIVE SESSION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (showSuccessConfirmation && lastDispatchedRecord != null) {
                // Success Broadcast Confirmation View
                BroadcastSuccessView(
                    record = lastDispatchedRecord!!,
                    result = lastResult,
                    onDone = {
                        showSuccessConfirmation = false
                        onNavigateBack()
                    },
                    onViewResponseMap = onViewResponseMap?.let { callback ->
                        { alertId ->
                            showSuccessConfirmation = false
                            callback(alertId)
                        }
                    }
                )
            } else {
                // Main Form Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    // Authority Notice Banner
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = "🚨", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "OFFICIAL DISASTER INCIDENT ADDRESS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmergencyRed,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Address the disaster classification, affected State, District, and City. All citizen handsets and emergency teams in the target zone will receive immediate alert sirens.",
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 1. Disaster Classification Type Dropdown
                    FilterableDisasterTypeSelector(
                        disasterTypes = DisasterRepository.disasterTypes,
                        selectedType = selectedDisasterType,
                        onTypeSelected = { disaster ->
                            selectedDisasterType = disaster
                            // Auto-update advisory based on disaster
                            instructionsText = when (disaster.id) {
                                "flood" -> "Flash flood warning issued. River levels exceeding danger mark. Move immediately to multi-story flood shelters or higher ground. Avoid crossing water channels."
                                "cyclone" -> "Severe cyclone storm surge expected with high-speed gale winds. Evacuate coastal lowlands immediately. Secure loose structures and remain inside pucca shelters."
                                "landslide" -> "Imminent landslide hazard along slopes following intense rainfall. Evacuate vulnerable hill settlements immediately. NH and mountain roads closed."
                                "cloudburst" -> "High-intensity cloudburst trigger in upper catchment. Massive surge anticipated in local nullahs and streams. Stay clear of riverbanks."
                                else -> "Emergency alert issued for ${disaster.name}. Responders and residents in the affected district must follow civil defense guidance immediately."
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 2. State of India Dropdown (With KEYBOARD FILTERING)
                    FilterableStateSelector(
                        states = DisasterRepository.indianStates,
                        selectedState = selectedState,
                        onStateSelected = { state ->
                            selectedState = state
                            // Reset district when state changes
                            selectedDistrict = ""
                            cityLocality = ""
                        }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 3. District Dropdown (Dynamic per State + Searchable)
                    FilterableDistrictSelector(
                        districts = availableDistricts,
                        selectedDistrict = selectedDistrict,
                        stateName = selectedState?.name,
                        onDistrictSelected = { district ->
                            selectedDistrict = district
                            if (cityLocality.isBlank()) {
                                cityLocality = "$district Central Sector"
                            }
                        }
                    )

                    if (selectedState != null && selectedDistrict.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ReachIndicator(
                            loading = reachLoading,
                            count = reachCount,
                            district = selectedDistrict
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 4. City / Locality Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "City / Locality / Sector",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = cityLocality,
                            onValueChange = { cityLocality = it },
                            placeholder = {
                                Text(
                                    text = if (selectedDistrict.isNotBlank()) "e.g. $selectedDistrict Lowlands, Ward 4..."
                                    else "Enter affected city or locality...",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 5. Severity Rating
                    Text(
                        text = "Alert Severity Classification",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SeverityOptionPill(
                            severityOption = AlertSeverity.RED,
                            isSelected = severity == AlertSeverity.RED,
                            color = EmergencyRed,
                            onSelect = { severity = AlertSeverity.RED },
                            modifier = Modifier.weight(1f)
                        )
                        SeverityOptionPill(
                            severityOption = AlertSeverity.ORANGE,
                            isSelected = severity == AlertSeverity.ORANGE,
                            color = EmergencyOrange,
                            onSelect = { severity = AlertSeverity.ORANGE },
                            modifier = Modifier.weight(1f)
                        )
                        SeverityOptionPill(
                            severityOption = AlertSeverity.YELLOW,
                            isSelected = severity == AlertSeverity.YELLOW,
                            color = EmergencyYellow,
                            onSelect = { severity = AlertSeverity.YELLOW },
                            modifier = Modifier.weight(1f)
                        )
                        SeverityOptionPill(
                            severityOption = AlertSeverity.GREEN,
                            isSelected = severity == AlertSeverity.GREEN,
                            color = MaterialTheme.colorScheme.primary,
                            onSelect = { severity = AlertSeverity.GREEN },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 6. Broadcast Advisory & Instructions
                    Text(
                        text = "Emergency Advisory & Public Action",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = instructionsText,
                        onValueChange = { instructionsText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Validation Error Text
                    if (validationError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = AppIcons.Warning,
                                contentDescription = "Error",
                                tint = EmergencyRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = validationError!!,
                                color = EmergencyRed,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    // Broadcast Transmission Button
                    Button(
                        onClick = {
                            if (selectedDisasterType == null) {
                                validationError = "Please select the Disaster Type."
                                return@Button
                            }
                            if (selectedState == null) {
                                validationError = "Please select the State of India."
                                return@Button
                            }
                            if (selectedDistrict.isBlank()) {
                                validationError = "Please select the District."
                                return@Button
                            }

                            val instructions = instructionsText.trim()
                            if (instructions.isBlank()) {
                                validationError = "Please enter the emergency advisory."
                                return@Button
                            }

                            validationError = null
                            val type = selectedDisasterType!!
                            val state = selectedState!!
                            val district = selectedDistrict
                            val typedCity = cityLocality.trim()
                            // the field auto-fills "<district> Central Sector"; only send what the officer really typed
                            val cityForServer =
                                if (typedCity.isBlank() || typedCity == "$district Central Sector") null else typedCity
                            val request = DispatchRequest(
                                disasterType = type.id,
                                state = state.name,
                                district = district,
                                city = cityForServer,
                                severity = severity.name,
                                instructions = instructions,
                                authorityId = "NDMA-${state.code}-DISPATCH"
                            )

                            scope.launch {
                                isSending = true
                                try {
                                    val result = BackendApi.dispatch(request)
                                    val record = DisasterBroadcastRecord(
                                        id = result.alertCode,
                                        disasterType = type.name,
                                        emoji = type.emoji,
                                        state = state.name,
                                        district = district,
                                        city = cityForServer ?: "$district Sector",
                                        severity = severity,
                                        instructions = instructions,
                                        authorityId = request.authorityId.orEmpty(),
                                        timestamp = "Just now",
                                        isDelivered = result.recipients == 0 || result.failed == 0
                                    )
                                    lastResult = result
                                    lastDispatchedRecord = record
                                    onBroadcastDispatched(record)
                                    showSuccessConfirmation = true
                                } catch (e: ApiException) {
                                    if (e.unauthorized) onSessionExpired() else validationError = e.message
                                } finally {
                                    isSending = false
                                }
                            }
                        },
                        enabled = !isSending,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmergencyRed,
                            contentColor = Color.White,
                            disabledContainerColor = EmergencyRed.copy(alpha = 0.5f),
                            disabledContentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(text = "📡", fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSending) "TRANSMITTING…" else "TRANSMIT EMERGENCY BROADCAST",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SeverityOptionPill(
    severityOption: AlertSeverity,
    isSelected: Boolean,
    color: Color,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ),
        color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = severityOption.codeName,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BroadcastSuccessView(
    record: DisasterBroadcastRecord,
    result: DispatchResult?,
    onDone: () -> Unit,
    onViewResponseMap: ((Long) -> Unit)? = null
) {
    val subtitle = when {
        result == null -> "Alert dispatched to ${record.district}, ${record.state}."
        result.recipients == 0 -> "Alert recorded for ${record.district}, ${record.state}, but no citizens are registered there yet, so nobody received it."
        result.status == "SENDING" -> "Dispatching to ${result.recipients} registered citizens in ${record.district}, ${record.state}. Delivery continues in the background."
        result.failed > 0 -> "Delivered to ${result.delivered} of ${result.recipients} registered citizens in ${record.district}, ${record.state}. ${result.failed} could not be reached."
        else -> "Alert delivered to all ${result.delivered} registered citizens in ${record.district}, ${record.state}."
    }
    val badge = when {
        result == null -> "✓ SENT"
        result.recipients == 0 -> "ℹ NO RECIPIENTS"
        result.status == "SENDING" -> "⏳ SENDING"
        result.failed > 0 -> "⚠ PARTIAL"
        else -> "✓✓ DELIVERED"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(90.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "✅", fontSize = 42.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Emergency Broadcast Dispatched!",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            fontSize = 13.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Telegram-Style Receipt Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "BROADCAST RECEIPT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                ReceiptRow(label = "Dispatch ID", value = record.id)
                ReceiptRow(label = "Disaster Type", value = "${record.emoji} ${record.disasterType}")
                ReceiptRow(label = "Target State", value = record.state)
                ReceiptRow(label = "District & City", value = "${record.district} (${record.city})")
                ReceiptRow(label = "Severity Level", value = record.severity.label)
                ReceiptRow(label = "Authorized Officer", value = record.authorityId)
                ReceiptRow(label = "Signed in as", value = Session.officerName ?: "-")
                if (result != null) {
                    ReceiptRow(label = "Registered in district", value = result.recipients.toString())
                    ReceiptRow(label = "Delivered", value = result.delivered.toString())
                    ReceiptRow(label = "Failed", value = result.failed.toString())
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onDone,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "RETURN TO INCIDENT MONITOR",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        if (onViewResponseMap != null && result != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { onViewResponseMap(result.id) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Map,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VIEW RESPONSE MAP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ReachIndicator(loading: Boolean, count: Int?, district: String) {
    val (text, tint) = when {
        loading -> "Checking registered citizens…" to MaterialTheme.colorScheme.onSurfaceVariant
        count == null -> "Could not check registered citizens (is the server reachable?)" to MaterialTheme.colorScheme.onSurfaceVariant
        count == 0 -> "⚠️ No citizens are registered in $district yet. The alert will be recorded, but nobody will receive it." to EmergencyOrange
        count == 1 -> "📡 1 registered citizen in $district will receive this alert." to MaterialTheme.colorScheme.primary
        else -> "📡 $count registered citizens in $district will receive this alert." to MaterialTheme.colorScheme.primary
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}
