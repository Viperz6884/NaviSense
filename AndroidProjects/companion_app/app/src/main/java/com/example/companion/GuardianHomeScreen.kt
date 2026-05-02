package com.example.companion

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianHomeScreen() {
    val context = LocalContext.current

    var blindLatLng by remember { mutableStateOf<LatLng?>(null) }
    var lastUpdateText by remember { mutableStateOf("Waiting for location...") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var connectedBlindId by remember { mutableStateOf("Loading...") }

    // ── Load from SharedPreferences — no Firebase auth needed ─────────────────
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("companion_prefs", Context.MODE_PRIVATE)
        val blindId = prefs.getString("connected_blind_id", "Unknown") ?: "Unknown"
        connectedBlindId = blindId

        if (blindId != "Unknown") {
            FirebaseManager.listenToBlindLocation(blindId) { lat, lng ->
                blindLatLng = LatLng(lat, lng)
                lastUpdateText = "Last update: just now"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Guardian View", fontWeight = FontWeight.Bold)
                        Text(
                            "Tracking: $connectedBlindId",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (blindLatLng != null)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (blindLatLng != null) "● Live" else "● Waiting",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (blindLatLng != null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Track") },
                    label = { Text("Track Person") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
                    label = { Text("Alerts") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> TrackPersonTab(
                blindLatLng = blindLatLng,
                lastUpdateText = lastUpdateText,
                connectedId = connectedBlindId,
                modifier = Modifier.padding(innerPadding)
            )
            1 -> AlertsTab(modifier = Modifier.padding(innerPadding))
            2 -> SettingsTab(
                connectedId = connectedBlindId,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun TrackPersonTab(
    blindLatLng: LatLng?,
    lastUpdateText: String,
    connectedId: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text("ID: $connectedId", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Text(
                lastUpdateText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (blindLatLng != null) {
                GoogleMapView(latLng = blindLatLng)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Waiting for $connectedId's location...",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Make sure they have started a walk",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickStatItem(
                    icon = Icons.Default.LocationOn,
                    label = "Coordinates",
                    value = if (blindLatLng != null)
                        "${blindLatLng.latitude.formatCoord(4)},\n${blindLatLng.longitude.formatCoord(4)}"
                    else "—"
                )
                HorizontalDivider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp)
                )
                QuickStatItem(
                    icon = Icons.Default.CheckCircle,
                    label = "Status",
                    value = if (blindLatLng != null) "Walking" else "Idle"
                )
            }
        }
    }
}

// ── UPDATED MAP COMPOSABLE ────────────────────────────────────────────────────
@Composable
fun GoogleMapView(latLng: LatLng) {
    // 1. Initialize the camera state targeting the incoming LatLng
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(latLng, 16f)
    }

    // 2. Animate the camera smoothly whenever the location updates
    LaunchedEffect(latLng) {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngZoom(latLng, 16f),
            durationMs = 1000 // 1 second smooth panning
        )
    }

    // 3. Render the native Compose Google Map
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            compassEnabled = true,
            myLocationButtonEnabled = false // Keep false unless location permissions are enabled
        )
    ) {
        // 4. Place the marker for the blind person
        Marker(
            state = MarkerState(position = latLng),
            title = "📍 Blind Person",
            snippet = "Current Location"
        )
    }
}

@Composable
fun AlertsTab(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "No alerts yet",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                "SOS alerts will appear here",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
fun SettingsTab(connectedId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Connection",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Card(shape = RoundedCornerShape(16.dp)) {
            ListItem(
                headlineContent = { Text("Connected Blind Person") },
                supportingContent = { Text("ID: $connectedId") },
                leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                trailingContent = {
                    TextButton(onClick = {
                        // Clear from SharedPreferences
                        context.getSharedPreferences("companion_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .remove("connected_blind_id")
                            .apply()
                    }) {
                        Text("Disconnect", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Notifications",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Card(shape = RoundedCornerShape(16.dp)) {
            ListItem(
                headlineContent = { Text("SOS Alerts") },
                supportingContent = { Text("Get notified when SOS is triggered") },
                leadingContent = { Icon(Icons.Default.Warning, contentDescription = null) },
                trailingContent = {
                    var checked by remember { mutableStateOf(true) }
                    Switch(checked = checked, onCheckedChange = { checked = it })
                }
            )
        }
    }
}

@Composable
fun QuickStatItem(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(
            label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

private fun Double.formatCoord(digits: Int) = "%.${digits}f".format(this)