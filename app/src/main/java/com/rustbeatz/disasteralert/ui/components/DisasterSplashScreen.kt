package com.rustbeatz.disasteralert.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rustbeatz.disasteralert.R
import com.rustbeatz.disasteralert.ui.common.SafeAreaContainer
import kotlinx.coroutines.delay

@Composable
fun DisasterSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(0.7f) }
    val alphaAnim = remember { Animatable(0f) }

    // Pulsing radar wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(700, easing = FastOutSlowInEasing)
        )
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(500)
        )
        // Keep splash screen for 2.2 seconds then navigate to Home
        delay(2200)
        onSplashFinished()
    }

    SafeAreaContainer(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            onSplashFinished()
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(32.dp)
                    .scale(scaleAnim.value)
                    .alpha(alphaAnim.value)
            ) {
                // Radar Pulse Aura around Logo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(160.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(pulseScale)
                            .alpha(pulseAlpha)
                            .border(
                                width = 3.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.size(120.dp)
                    ) {}

                    // Vector Shield Logo
                    Image(
                        painter = painterResource(id = R.drawable.ic_disaster_shield),
                        contentDescription = "Disaster Alert Logo",
                        modifier = Modifier.size(96.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // App Title
                Text(
                    text = "DisasterAlert",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Telegram-Style Subtitle Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CRISIS BROADCAST NETWORK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "National Disaster Management\nAuthority Protocol System",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Discreet Progress Indicator
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Connecting 36 States & UTs...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            // Bottom Telegram Security Tag
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "NDMA SECURE INCIDENT DISPATCH • V1.0",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
