package com.example.companion

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*

// All permissions the app needs
fun getRequiredPermissions(): Array<String> {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CALL_PHONE,
    )
    // Bluetooth permissions differ by Android version
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        permissions.add(Manifest.permission.BLUETOOTH)
    }
    // Notification permission only needed on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    return permissions.toTypedArray()
}

@Composable
fun RequestPermissionsOnLaunch(onAllGranted: () -> Unit) {
    var hasRequested by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Even if some are denied, proceed — handle missing perms gracefully later
        onAllGranted()
    }

    LaunchedEffect(Unit) {
        if (!hasRequested) {
            hasRequested = true
            launcher.launch(getRequiredPermissions())
        }
    }
}