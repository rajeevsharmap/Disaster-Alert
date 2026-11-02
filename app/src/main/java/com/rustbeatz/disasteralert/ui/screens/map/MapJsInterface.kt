package com.rustbeatz.disasteralert.ui.screens.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface

class MapJsInterface(private val context: Context) {

    @JavascriptInterface
    fun onNavigate(lat: Double, lon: Double, name: String) {
        val encodedName = Uri.encode(name)
        val geoUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($encodedName)")
        val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback: browser Google Maps if no dedicated maps app
            try {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (_: Exception) {
                // Ignore if device has no browser or intent handler
            }
        }
    }
}
