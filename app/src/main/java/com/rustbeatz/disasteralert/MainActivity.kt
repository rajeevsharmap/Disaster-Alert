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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rustbeatz.disasteralert.data.DisasterBroadcastRecord
import com.rustbeatz.disasteralert.data.DisasterRepository
import com.rustbeatz.disasteralert.data.network.Session
import com.rustbeatz.disasteralert.ui.components.DisasterSplashScreen
import com.rustbeatz.disasteralert.ui.screens.AuthorityDisasterBroadcastScreen
import com.rustbeatz.disasteralert.ui.screens.HomeScreen
import com.rustbeatz.disasteralert.ui.screens.OfficerLoginScreen
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

            // Live broadcast alert feed initialized with realistic NDMA records
            val broadcastsList = remember {
                mutableStateListOf<DisasterBroadcastRecord>().apply {
                    addAll(DisasterRepository.sampleBroadcasts)
                }
            }

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
                            HomeScreen(
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = { isDarkTheme = !isDarkTheme },
                                onNavigateToEmergencyAlert = {
                                    currentScreen = AppDestination.AUTHORITY_DISPATCH
                                },
                                broadcasts = broadcastsList
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
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
