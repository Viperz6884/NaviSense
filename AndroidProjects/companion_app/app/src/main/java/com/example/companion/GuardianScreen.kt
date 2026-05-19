package com.example.companion

import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.outlined.Security
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun GuardianScreen(navController: NavController) {
    // ─── STATE VARIABLES FOR TOGGLES ───
    var criticalAlertsEnabled by remember { mutableStateOf(true) }
    var dailyReportsEnabled by remember { mutableStateOf(true) }
    var hardwareAlertsEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Dark app background
            .padding(top = 40.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. GUARDIAN.OS TOP BAR
        GuardianTopBar()

        Spacer(modifier = Modifier.height(32.dp))

        // 2. ACTIVE SUBJECT CARD
        ActiveSubjectCard()

        Spacer(modifier = Modifier.height(40.dp))

        // 3. CONFIGURATION MODULE HEADER
        Text(
            text = "CONFIGURATION MODULE",
            color = Color(0xFFA87FFB), // Purple/Blue accent
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "COMMANDER_\n01",
            color = Color.White,
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 44.sp,
            letterSpacing = (-1).sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Adjust global security protocols, manage the mesh network of caregivers, and calibrate notification priority for the safety ecosystem.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 4. ACTIVE SESSION CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Placeholder Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF2A2A2E), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("ACTIVE SESSION", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("v4.2.0-STABLE", color = Color(0xFFA87FFB), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. NOTIFICATION PROTOCOLS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Title Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.NotificationsActive, contentDescription = null, tint = Color(0xFFA87FFB), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Notification Protocols", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle 1: Critical SOS Alerts
                NotificationToggleRow(
                    icon = Icons.Outlined.GpsFixed,
                    title = "Critical SOS Alerts",
                    subtitle = "Override silent mode for life-critical incidents",
                    iconTint = Color(0xFFFFB4B4), // Salmon red
                    isChecked = criticalAlertsEnabled,
                    onCheckedChange = { criticalAlertsEnabled = it }
                )

                // Toggle 2: Daily Safety Reports
                NotificationToggleRow(
                    icon = Icons.Outlined.Article,
                    title = "Daily Safety Reports",
                    subtitle = "Consolidated summary of activities at 08:00",
                    iconTint = Color(0xFFA87FFB), // Purple
                    isChecked = dailyReportsEnabled,
                    onCheckedChange = { dailyReportsEnabled = it }
                )

                // Toggle 3: Hardware Status Alerts
                NotificationToggleRow(
                    icon = Icons.Outlined.BatteryAlert,
                    title = "Hardware Status Alerts",
                    subtitle = "Notify when battery drops below 15% or signal is lost",
                    iconTint = Color(0xFFD4E05B), // Neon yellow
                    isChecked = hardwareAlertsEnabled,
                    onCheckedChange = { hardwareAlertsEnabled = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ─── TEMPORARY TEST BUTTON (RESTORED) ───
        Button(
            onClick = {
                // This manually triggers the SOS screen for testing!
                navController.navigate("guardianSos")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3A0A0A), // Dark red background
                contentColor = Color(0xFFFFB4B4)    // Salmon pink text
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "TEST: TRIGGER SOS SCREEN",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        // ─── LOG OUT SECTION ───
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // 1. Tell Firebase to securely end the active session (Data remains safe in Firestore)
                FirebaseAuth.getInstance().signOut()

                // 2. Safely route back to the login page and destroy the backstack
                navController.navigate("login") {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2A2A2E), // Dark gray background
                contentColor = Color(0xFFFFB4B4)    // Salmon/Pink text
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = "Log Out", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))



        Spacer(modifier = Modifier.height(40.dp))
    }
}
@Composable
fun GuardianTopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Side: Logo and Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Guardian Logo",
                tint = Color(0xFFA87FFB), // Purple accent
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

        // Right Side: Icons and Live Sync Badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CellTower,
                contentDescription = "Connection",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Surface(
                color = Color(0xFF2D2440),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "LIVE_SYNC",
                    color = Color(0xFFA87FFB),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun ActiveSubjectCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E)) // Dark gray background
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Top Row: Avatar and Real-Time Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Avatar Placeholder (You can swap this with AsyncImage later)
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2A2A2E))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "A", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Real-Time Badge
                Surface(
                    color = Color(0xFF2A2E1A), // Olive green tint
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "STATUS: REAL-TIME",
                        color = Color(0xFFD4E05B), // Neon yellow/green
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subject Name Section
            Text(
                text = "ACTIVE SUBJECT",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Aritra", // Placeholder for the blind user's name
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Hardware and Connection Details
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HARDWARE ID",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "GP-9920-X",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CONNECTION",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = "Encrypted",
                            tint = Color(0xFFD4E05B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ENCRYPTED",
                            color = Color(0xFFD4E05B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Establish Contact Button
            Button(
                onClick = { /* Call action goes here */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFA87FFB), // Light Purple
                    contentColor = Color(0xFF2D2440)    // Dark Text
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ESTABLISH CONTACT",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
@Composable
fun GuardianStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    accentColor: Color? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E))
    ) {
        // We use IntrinsicSize to allow the side-bar to perfectly match the height of the card
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {

            // The glowing accent bar on the left (Only shows up if an accentColor is provided!)
            if (accentColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(accentColor)
                )
            }

            // Card Content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = value,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Icon embedded in a dark box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A2A2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
@Composable
fun NotificationToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dark icon box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF2A2A2E), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = 16.sp)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // The functional toggle switch
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFA87FFB), // Purple active track
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF2A2A2E), // Dark inactive track
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}