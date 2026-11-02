package com.rustbeatz.disasteralert

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rustbeatz.disasteralert.data.DisasterBroadcastRecord
import com.rustbeatz.disasteralert.data.DisasterRepository
import com.rustbeatz.disasteralert.data.network.Session
import com.rustbeatz.disasteralert.data.repo.NetworkResponsesRepository
import com.rustbeatz.disasteralert.data.repo.ResponsesRepository
import com.rustbeatz.disasteralert.ui.components.DisasterBottomNavBar
import com.rustbeatz.disasteralert.ui.components.MainBottomTab
import com.rustbeatz.disasteralert.ui.components.DisasterSplashScreen
import com.rustbeatz.disasteralert.ui.screens.AlertsHistoryScreen
import com.rustbeatz.disasteralert.ui.screens.AuthorityDisasterBroadcastScreen
import com.rustbeatz.disasteralert.ui.screens.HomeDashboardScreen
import com.rustbeatz.disasteralert.ui.screens.OfficerLoginScreen
import com.rustbeatz.disasteralert.ui.screens.map.ResponseMapScreen
import com.rustbeatz.disasteralert.ui.theme.DisasterAlertTheme

enum class AppDestination {
    SPLASH,
    LOGIN,
    HOME,
    AUTHORITY_DISPATCH
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enforce portrait orientation to turn off screen rotation completely
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val systemInDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemInDark) }
            var currentScreen by remember { mutableStateOf(AppDestination.SPLASH) }

            // Tab state: defaults to HOME (Emergency Dashboard)
            var currentTab by remember { mutableStateOf(MainBottomTab.HOME) }
            var mapPreselectedAlertId by remember { mutableStateOf<Long?>(null) }

            // Live broadcast alert feed initialized with realistic NDMA records
            val broadcastsList = remember {
                mutableStateListOf<DisasterBroadcastRecord>().apply {
                    addAll(DisasterRepository.sampleBroadcasts)
                }
            }

            // Live repository with automatic mock fallback when offline
            val repository: ResponsesRepository = remember { NetworkResponsesRepository() }

            DisasterAlertTheme(
                darkTheme = isDarkTheme,
                dynamicColor = false // Keep our custom near-green dual theme active
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        when {
                            targetState == AppDestination.AUTHORITY_DISPATCH -> {
                                (slideInHorizontally { width -> width } + fadeIn())
                                    .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut())
                            }
                            initialState == AppDestination.AUTHORITY_DISPATCH -> {
                                (slideInHorizontally { width -> -width / 3 } + fadeIn())
                                    .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                            }
                            else -> {
                                fadeIn().togetherWith(fadeOut())
                            }
                        }
                    },
                    label = "ScreenTransition",
                    modifier = Modifier.fillMaxSize()
                ) { screen ->
                    when (screen) {
                        AppDestination.SPLASH -> {
                            DisasterSplashScreen(
                                onSplashFinished = {
                                    // Officers must sign in before reaching the incident monitor
                                    currentScreen = if (Session.token != null) AppDestination.HOME else AppDestination.LOGIN
                                }
                            )
                        }

                        AppDestination.LOGIN -> {
                            OfficerLoginScreen(
                                onLoggedIn = {
                                    currentScreen = AppDestination.HOME
                                }
                            )
                        }

                        AppDestination.HOME -> {
                            // Three bottom tabs: HOME (Dashboard), ALERTS (Feed/History), MAP (Help Map)
                            HomeWithTabs(
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = { isDarkTheme = !isDarkTheme },
                                broadcasts = broadcastsList,
                                repository = repository,
                                currentTab = currentTab,
                                onTabSelected = { currentTab = it },
                                mapPreselectedAlertId = mapPreselectedAlertId,
                                onNavigateToEmergencyAlert = {
                                    currentScreen = AppDestination.AUTHORITY_DISPATCH
                                }
                            )
                        }

                        AppDestination.AUTHORITY_DISPATCH -> {
                            AuthorityDisasterBroadcastScreen(
                                onNavigateBack = {
                                    currentScreen = AppDestination.HOME
                                },
                                onSessionExpired = {
                                    // Backend restarted or token rejected: sign in again
                                    Session.clear()
                                    currentScreen = AppDestination.LOGIN
                                },
                                onBroadcastDispatched = { newAlert ->
                                    // Prepend the new broadcast to the top of the live feed
                                    broadcastsList.add(0, newAlert)
                                },
                                onViewResponseMap = { alertId ->
                                    // Switch to HOME → Map tab with the broadcast alert preselected
                                    mapPreselectedAlertId = alertId
                                    currentTab = MainBottomTab.MAP
                                    currentScreen = AppDestination.HOME
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * HOME destination wrapper: hosts 3 bottom tabs (Home Dashboard, Alerts History, Map).
 * All composables are kept in composition so the WebView and scroll states are preserved.
 * Uses floating rounded top nav and bottom navigation bar where content flows gracefully behind them.
 */
@Composable
private fun HomeWithTabs(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    broadcasts: List<DisasterBroadcastRecord>,
    repository: ResponsesRepository,
    currentTab: MainBottomTab,
    onTabSelected: (MainBottomTab) -> Unit,
    mapPreselectedAlertId: Long?,
    onNavigateToEmergencyAlert: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // TAB 1: MAP TAB — kept in composition at all times so the WebView doesn't reload
        Box(
            modifier = if (currentTab == MainBottomTab.MAP) Modifier.fillMaxSize()
            else Modifier.size(0.dp)
        ) {
            ResponseMapScreen(
                repository = repository,
                isTabVisible = currentTab == MainBottomTab.MAP,
                isDarkTheme = isDarkTheme,
                preselectedAlertId = mapPreselectedAlertId,
                modifier = Modifier.fillMaxSize()
            )
        }

        // TAB 2: ALERTS TAB — History & Live disaster alerts feed
        Box(
            modifier = if (currentTab == MainBottomTab.ALERTS) Modifier.fillMaxSize()
            else Modifier.size(0.dp)
        ) {
            AlertsHistoryScreen(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                broadcasts = broadcasts,
                modifier = Modifier.fillMaxSize()
            )
        }

        // TAB 3: HOME TAB — Emergency Incident Dashboard
        Box(
            modifier = if (currentTab == MainBottomTab.HOME) Modifier.fillMaxSize()
            else Modifier.size(0.dp)
        ) {
            HomeDashboardScreen(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onNavigateToEmergencyAlert = onNavigateToEmergencyAlert,
                broadcastsCount = broadcasts.size,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Floating rounded bottom navigation bar (Content flows behind it)
        DisasterBottomNavBar(
            selectedTab = currentTab,
            onTabSelected = onTabSelected,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
