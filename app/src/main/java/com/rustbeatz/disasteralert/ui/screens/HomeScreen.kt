package com.rustbeatz.disasteralert.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rustbeatz.disasteralert.data.DisasterBroadcastRecord
import com.rustbeatz.disasteralert.ui.common.SafeAreaContainer
import com.rustbeatz.disasteralert.ui.components.EmergencyHeroCard
import com.rustbeatz.disasteralert.ui.components.TelegramAlertCard
import com.rustbeatz.disasteralert.ui.components.TelegramTopAppBar

@Composable
fun HomeScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToEmergencyAlert: () -> Unit,
    broadcasts: List<DisasterBroadcastRecord>,
    modifier: Modifier = Modifier
) {
    SafeAreaContainer(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Telegram-style Top App Bar with live badge & light/dark theme switch
            TelegramTopAppBar(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // MAIN TASK: Top Main Card with Emergency Alert Button defining its work
                    EmergencyHeroCard(
                        onEmergencyAlertClick = onNavigateToEmergencyAlert
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Quick Action Civil Defence Row (Telegram channel quick access)
                    QuickActionSection()

                    Spacer(modifier = Modifier.height(20.dp))

                    // Feed Section Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LIVE DISASTER BROADCAST FEED",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${broadcasts.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Real-time NDMA Mesh",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Telegram Alert Feed Items
                items(broadcasts, key = { it.id }) { alertItem ->
                    TelegramAlertCard(
                        record = alertItem,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickActionSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "EMERGENCY QUICK SERVICES",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickServicePill(
                icon = "📞",
                title = "NDRF 1078",
                subtitle = "Toll-Free",
                modifier = Modifier.weight(1f)
            )
            QuickServicePill(
                icon = "🏥",
                title = "Relief Camps",
                subtitle = "Active Shelters",
                modifier = Modifier.weight(1f)
            )
            QuickServicePill(
                icon = "🌦️",
                title = "Doppler Radar",
                subtitle = "IMD Satellite",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickServicePill(
    icon: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { /* Emergency contact dialog */ }
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp)
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
