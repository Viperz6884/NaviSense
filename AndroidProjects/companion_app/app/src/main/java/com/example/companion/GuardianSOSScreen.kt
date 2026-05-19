package com.example.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun GuardianSOSScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Deep dark background
            .verticalScroll(rememberScrollState())
    ) {
        // Top Padding and Top Bar
        Column(modifier = Modifier.padding(top = 40.dp, start = 20.dp, end = 20.dp)) {
            SOSTopBar()
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large Red Critical Alert Header
        CriticalAlertHeader()

        Spacer(modifier = Modifier.height(24.dp))

        // Main Content (Cards)
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            TelemetryCard()
            Spacer(modifier = Modifier.height(24.dp))
            LocationCard() // Back to the static version!
            Spacer(modifier = Modifier.height(24.dp))
            SOSFooter()
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SOSTopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo and Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Guardian Logo",
                tint = Color(0xFFFF4B4B), // Red logo for SOS
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "GUARDIAN.OS",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        // Active SOS Badge and Connection Icon
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = Color(0xFF4A0E0E), // Dark red background
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "✳ SOS_ACTIVE",
                    color = Color(0xFFFF4B4B), // Bright red text
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = Icons.Default.CellTower,
                contentDescription = "Connection",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CriticalAlertHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3A0A0A), // Deep red top
                        Color(0xFF121212)  // Fade into dark app background
                    )
                )
            )
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "CRITICAL\nALERT",
                color = Color(0xFFFFB4B4), // Salmon/Pink red
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 48.sp,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "INCIDENT_TIMESTAMP: 14:02:44_UTC",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun TelemetryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E))
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Left Red Accent Bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(Color(0xFFFFB4B4))
            )

            Column(modifier = Modifier.padding(20.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STICK TELEMETRY",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Surface(
                        color = Color(0xFF2D1616),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "CRITICAL_EVENT",
                            color = Color(0xFFFF4B4B).copy(alpha = 0.8f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Event Class & Impact Row
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "EVENT CLASS", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Obstacle\ndetected", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "IMPACT LEVEL", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "HIGH\nIMPACT", color = Color(0xFFFF4B4B), fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.End, lineHeight = 24.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Accelerometer Data
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2A1E1E)) // Slight reddish tint dark box
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ACCELEROMETER_PEAK: 4.8G",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun LocationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E))
    ) {
        Column {
            // Map Placeholder Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF222226)), // Slightly lighter than card
                contentAlignment = Alignment.Center
            ) {
                // Future Google Map goes here. For now, a location pin placeholder.
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Pin",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(48.dp)
                )
            }

            // Location Data Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF2A2A2E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, tint = Color(0xFFA87FFB), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "CURRENT COORDINATES", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(text = "47.6062° N, 122.3321° W", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Icon(imageVector = Icons.Default.OpenInFull, contentDescription = "Expand", tint = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun SOSFooter() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Column {
                Text("SYSTEM_HEALTH:", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text("98%", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("NETWORK:", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text("ENCRYPTED_MESH", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(Color(0xFFD4E05B), CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Text("LIVE_RECORDING_ACTIVE", color = Color(0xFFD4E05B), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}