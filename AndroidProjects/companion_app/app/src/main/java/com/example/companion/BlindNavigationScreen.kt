package com.example.companion

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

@Composable
fun BlindNavigationScreen(destination: String) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("google.navigation:q=$destination")
        }
        context.startActivity(intent)
    }
}