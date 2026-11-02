package com.rustbeatz.disasteralert.ui.screens.map

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.rustbeatz.disasteralert.data.repo.AlertResponsesResult
import com.rustbeatz.disasteralert.data.repo.AlertSummary
import com.rustbeatz.disasteralert.data.repo.ResponsesRepository
import com.rustbeatz.disasteralert.ui.theme.AppIcons
import com.rustbeatz.disasteralert.ui.theme.EmergencyRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponseMapScreen(
    repository: ResponsesRepository,
    isTabVisible: Boolean,
    isDarkTheme: Boolean,
    preselectedAlertId: Long? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State
    var alerts by remember { mutableStateOf<List<AlertSummary>>(emptyList()) }
    var selectedAlert by remember { mutableStateOf<AlertSummary?>(null) }
    var responsesResult by remember { mutableStateOf<AlertResponsesResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedNeedFilter by remember { mutableStateOf<String?>(null) }
    var isOverviewCollapsed by remember { mutableStateOf(false) }
    var isMapPageReady by remember { mutableStateOf(false) }

    // Bottom sheet state (peek height 70.dp, draggable up to list)
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    // Remember the WebView instance across recompositions and tab switches
    val jsInterface = remember(context) { MapJsInterface(context) }
    val webView = remember(context) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                userAgentString = "DisasterAlertHackathon/1.0"
            }
            addJavascriptInterface(jsInterface, "Android")
            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                    android.util.Log.d("LeafletMap", "${consoleMessage?.message()} (line ${consoleMessage?.lineNumber()})")
                    return true
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isMapPageReady = true
                }
            }
            val html = LeafletMapHtml.buildHtml(isDarkTheme)
            loadDataWithBaseURL("https://disasteralert.app/", html, "text/html", "UTF-8", null)
        }
    }

    // Helper to send data to Leaflet
    fun pushDataToMap(result: AlertResponsesResult?, needFilter: String?) {
        if (!isMapPageReady || result == null) return
        val items = if (needFilter == null) {
            result.items
        } else {
            result.items.filter { it.need.equals(needFilter, ignoreCase = true) }
        }

        val json = JSONObject().apply {
            put("alertId", result.alertId)
            put("alertCode", result.alertCode)
            val itemsArray = JSONArray()
            items.forEach { item ->
                itemsArray.put(JSONObject().apply {
                    put("replyId", item.replyId)
                    put("name", item.name)
                    put("response", item.response)
                    put("need", item.need ?: JSONObject.NULL)
                    put("priority", item.priority ?: JSONObject.NULL)
                    put("city", item.city ?: JSONObject.NULL)
                    put("latitude", item.latitude ?: JSONObject.NULL)
                    put("longitude", item.longitude ?: JSONObject.NULL)
                    put("accuracyM", item.accuracyM ?: JSONObject.NULL)
                    put("note", item.note ?: JSONObject.NULL)
                    put("at", item.at)
                    put("istTime", item.istTime)
                })
            }
            put("items", itemsArray)
        }

        val quoted = JSONObject.quote(json.toString())
        webView.post {
            webView.evaluateJavascript("if (window.setData) { window.setData(JSON.parse($quoted)); }", null)
        }
    }

    // Refresh map layout and invalidate size whenever the Map tab becomes visible
    LaunchedEffect(isTabVisible) {
        if (isTabVisible) {
            delay(150)
            webView.post {
                webView.evaluateJavascript("if (window.refreshMap) { window.refreshMap(); }", null)
            }
            if (isMapPageReady && responsesResult != null) {
                pushDataToMap(responsesResult, selectedNeedFilter)
            }
        }
    }

    // Update map when result or filter changes or page loads
    LaunchedEffect(isMapPageReady, responsesResult, selectedNeedFilter) {
        if (isMapPageReady && responsesResult != null) {
            pushDataToMap(responsesResult, selectedNeedFilter)
        }
    }

    // 1. Initial Load: Load Alerts List on open
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            errorMessage = null
            val loadedAlerts = repository.alerts()
            alerts = loadedAlerts
            if (loadedAlerts.isNotEmpty()) {
                // If preselectedAlertId is provided, find it; otherwise pick newest
                val target = if (preselectedAlertId != null) {
                    loadedAlerts.find { it.id == preselectedAlertId } ?: loadedAlerts.first()
                } else {
                    loadedAlerts.first()
                }
                selectedAlert = target
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load alerts."
        } finally {
            isLoading = false
        }
    }

    // Handle external preselection change
    LaunchedEffect(preselectedAlertId) {
        if (preselectedAlertId != null && alerts.isNotEmpty()) {
            alerts.find { it.id == preselectedAlertId }?.let {
                selectedAlert = it
            }
        }
    }

    // 2. Fetch responses for selected alert + Auto-refresh every 5 seconds ONLY while Map tab is visible
    LaunchedEffect(isTabVisible, selectedAlert?.id) {
        val currentId = selectedAlert?.id ?: return@LaunchedEffect
        if (!isTabVisible) return@LaunchedEffect

        // Initial fetch for this alert
        try {
            isLoading = responsesResult == null
            val res = repository.responses(currentId)
            responsesResult = res
            errorMessage = null
        } catch (e: Exception) {
            if (responsesResult == null) {
                errorMessage = e.message ?: "Failed to load responses."
            }
        } finally {
            isLoading = false
        }

        // Auto-refresh loop every 5 seconds
        while (isActive && isTabVisible) {
            delay(5000)
            if (isActive && isTabVisible) {
                try {
                    val updated = repository.responses(currentId)
                    responsesResult = updated
                } catch (_: Exception) {
                    // Silent retry on background 5s tick
                }
            }
        }
    }

    // Filtered items for bottom sheet list
    val filteredList = remember(responsesResult, selectedNeedFilter) {
        val items = responsesResult?.items ?: emptyList()
        if (selectedNeedFilter == null) items
        else items.filter { it.need.equals(selectedNeedFilter, ignoreCase = true) }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 72.dp,
        sheetShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
        sheetShadowElevation = 10.dp,
        sheetDragHandle = null,
        sheetContent = {
            ResponsesBottomSheetContent(
                items = filteredList,
                totals = responsesResult?.totals,
                selectedNeedFilter = selectedNeedFilter,
                onToggleNeedFilter = { tapped ->
                    selectedNeedFilter = if (selectedNeedFilter == tapped) null else tapped
                },
                onClearNeedFilter = { selectedNeedFilter = null },
                onRowClick = { clickedItem ->
                    // Focus on map marker and collapse sheet to peek height
                    if (clickedItem.hasLocation) {
                        webView.post {
                            webView.evaluateJavascript("if (window.focusOn) { window.focusOn(${clickedItem.replyId}); }", null)
                        }
                    }
                    coroutineScope.launch {
                        scaffoldState.bottomSheetState.partialExpand()
                    }
                },
                onNavigateClick = { item ->
                    if (item.latitude != null && item.longitude != null) {
                        jsInterface.onNavigate(item.latitude, item.longitude, item.name)
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // LAYER 1: Full-screen Leaflet WebView
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize()
            )

            // LAYER 2: Ultra-compact floating top pill (Alert selector & summary)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .align(Alignment.TopCenter)
            ) {
                MapTopOverlay(
                    alerts = alerts,
                    selectedAlert = selectedAlert,
                    onSelectAlert = { newAlert ->
                        selectedAlert = newAlert
                        selectedNeedFilter = null
                        responsesResult = null
                    },
                    totals = responsesResult?.totals
                )
            }

            // Loading Indicator overlay
            if (isLoading && responsesResult == null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shadowElevation = 6.dp,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading Help Map...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Error with Retry button
            if (errorMessage != null && responsesResult == null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.4f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(text = "⚠️", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Failed to load responses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                selectedAlert?.let {
                                    coroutineScope.launch {
                                        try {
                                            isLoading = true
                                            errorMessage = null
                                            responsesResult = repository.responses(it.id)
                                        } catch (e: Exception) {
                                            errorMessage = e.message
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = AppIcons.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Empty state if no alerts exist
            if (!isLoading && alerts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(text = "📢", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No alerts broadcast yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Broadcast an emergency alert from the Alerts tab to begin collecting citizen responses.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
