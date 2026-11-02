package com.rustbeatz.disasteralert.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Professional SafeAreaView container for Android Jetpack Compose edge-to-edge layout.
 * Accurately prevents overlaps with status bar, display cutouts/notches, navigation bar,
 * and handles IME (keyboard) padding gracefully.
 */
@Composable
fun SafeAreaContainer(
    modifier: Modifier = Modifier,
    applyImePadding: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val imeModifier = if (applyImePadding) Modifier.imePadding() else Modifier

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .then(imeModifier),
        content = content
    )
}
