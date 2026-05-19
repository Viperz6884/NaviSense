package com.example.companion

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.runtime.DisposableEffect
import kotlin.math.roundToInt
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.border
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.rememberCoroutineScope


import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun StickScreen(navController: NavController) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val guardianPhone: String? = null

    // ─── LIVE FIREBASE DATA STATES ───
    var connectionStatus by remember { mutableStateOf("SEARCHING...") }
    var connectionColor by remember { mutableStateOf(Color.Yellow) }
    var liveDistance by remember { mutableStateOf("--") }
    // ─── CENTRALIZED HARDWARE STATE (HOISTED) ───
    var batterySaverEnabled by remember { mutableStateOf(true) }
    var autoOffMinutes by remember { mutableIntStateOf(15) }
    var detectionRange by remember { mutableFloatStateOf(1.5f) }
    var warningFrequency by remember { mutableFloatStateOf(1.5f) }
    var voiceVolume by remember { mutableFloatStateOf(0.6f) }
    var tonePitch by remember { mutableFloatStateOf(1f) }

    // ─── BACKGROUND FIREBASE LISTENER ───
    LaunchedEffect(Unit) {
        val liveRef = FirebaseDatabase.getInstance("https://companionapp-prod-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("NaviSense/Live")
        liveRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    connectionStatus = "CONNECTED (LIVE)"
                    connectionColor = Color.Green
                    // Grab the actual distance from your ESP32!
                    val dist = snapshot.child("distance_ahead").value
                    liveDistance = if (dist != null) "$dist cm" else "--"
                }
            }
            override fun onCancelled(error: DatabaseError) {
                connectionStatus = "ERROR"
                connectionColor = Color.Red
            }
        })
    }

    // ── TTS ───────────────────────────────────────────────────────────────────
    val tts = remember {
        TextToSpeech(context) {}.apply {
            language = Locale("en", "IN")
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // ── Offline voice command parser ──────────────────────────────────────────
    fun extractPlaceOffline(input: String): String? {
        var text = input.lowercase()
        text = text
            .replace("go to", "").replace("take me to", "")
            .replace("navigate to", "").replace("show me", "")
            .replace("mujhe", "").replace("le chalo", "")
            .replace("le jaana", "").replace("jana hai", "")
            .replace("ke paas", "").replace("ke liye", "")
        text = text.trim()
        return if (text.isNotEmpty()) text else null
    }
    // Speech launcher for the center mic button
    // ── Speech recognition launcher ───────────────────────────────────────────
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()

        if (spokenText != null) {
            scope.launch {
                val cleanedPlace = extractPlaceOffline(spokenText)

                val finalCommand = try {
                    val geminiRaw = GeminiManager.processCommand(spokenText)
                    when {
                        geminiRaw.contains("CALL_GUARDIAN", true) -> "CALL_GUARDIAN"
                        geminiRaw.contains("NAVIGATION:", true) -> {
                            val place = geminiRaw.substringAfter("NAVIGATION:").trim()
                            "NAVIGATION:$place"
                        }
                        else -> {
                            if (!cleanedPlace.isNullOrEmpty()) "NAVIGATION:$cleanedPlace"
                            else "NAVIGATION:$spokenText"
                        }
                    }
                } catch (e: Exception) {
                    if (!cleanedPlace.isNullOrEmpty()) "NAVIGATION:$cleanedPlace"
                    else "NAVIGATION:$spokenText"
                }

                when {
                    finalCommand.startsWith("NAVIGATION:") -> {
                        val place = finalCommand.removePrefix("NAVIGATION:").trim()
                        tts.speak("Starting navigation to $place", TextToSpeech.QUEUE_FLUSH, null, null)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("google.navigation:q=$place")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                    finalCommand == "CALL_GUARDIAN" -> {
                        guardianPhone?.let {
                            tts.speak("Calling your guardian", TextToSpeech.QUEUE_FLUSH, null, null)
                            triggerSosCall(context, it)
                        } ?: run {
                            tts.speak("No guardian number saved", TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    }
                    else -> {
                        tts.speak("Sorry, I did not understand", TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }
            }
        } else {
            tts.speak("I didn't catch that, please try again", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // Scaffold adds the bottom bar to the screen
    Scaffold(
        bottomBar = {
            // The exact same Box and Surface from your BlindHomeScreen
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp, bottom = 8.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Home Button - NOW WIRED TO GO BACK!
                        Column(
                            modifier = Modifier.size(56.dp).clickable {
                                navController.navigate("blindHome") {
                                    popUpTo("blindHome") { inclusive = true }
                                }
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Home, "Home", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            Spacer(Modifier.height(2.dp))
                            Text("Home", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }

                        // 2. Stick Button (Already here!)
                        Column(
                            modifier = Modifier.size(56.dp).clickable {
                                Toast.makeText(context, "You are already on the Stick page!", Toast.LENGTH_SHORT).show()
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Highlighting this icon slightly to show we are on this page
                            Icon(Icons.Default.Accessibility, "Stick", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(2.dp))
                            Text("Stick", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        // 3. Invisible Spacer for the Mic button
                        Spacer(modifier = Modifier.width(56.dp))

                        // 4. Blank Slot & Contact Button
                        Box(modifier = Modifier.size(56.dp))
                        Column(
                            modifier = Modifier.size(56.dp).clickable {
                                // NEW: Real navigation command!
                                navController.navigate("contacts") {
                                    launchSingleTop = true
                                }
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Call, "Contact", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            Spacer(Modifier.height(2.dp))
                            Text("Contact", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }

                        // 5. Profile Button - NOW WIRED TO NAVIGATE TO PROFILE PAGE!
                        Column(
                            modifier = Modifier.size(56.dp).clickable {
                                // Make sure "profileScreen" matches the exact route name you defined in your NavHost!
                                navController.navigate("profileScreen") {
                                    launchSingleTop = true
                                }
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Icon is faded because it's not the active page
                            Icon(
                                Icons.Default.AccountCircle,
                                "Profile",
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Profile",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // The Overlapping Speak Button
                ExtendedFloatingActionButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        }
                        speechLauncher.launch(intent)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-36).dp)
                        .size(width = 85.dp, height = 85.dp)
                ) {
                    Icon(Icons.Default.Mic, "Tap to Speak", modifier = Modifier.size(48.dp))
                }
            }
        },
        floatingActionButton = {
            FloatingSOSButton()
        }
    ) { innerPadding ->

        // ── MAIN CONTENT AREA ──
        // Notice we added .padding(innerPadding) so the text doesn't hide behind the footer!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp)
                .padding(top = 24.dp)
        ) {

            // ── OVERHEAD CONFIG TEXT ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "CONFIG / V2.4.0",
                    color = Color(0xFFA87FFB).copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── MASSIVE HEADER TEXT ──
            Text(
                text = "STICK\nHARDWARE",
                color = Color(0xFFF5F5F5),
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 52.sp,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── SUBTITLE DESCRIPTION ──
            Text(
                text = "Calibrate the tactile and acoustic feedback response systems for the Guardian Pro hardware interface.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ── ADD THE NEW MODULE HERE ──
            PowerModuleCard()
            Spacer(modifier = Modifier.height(16.dp))

            // ── ADD THE SONAR MODULE HERE ──
            // ── ADD THE SONAR MODULE HERE ──
            SonarModuleCard(
                detectionRange = detectionRange,
                onDetectionRangeChange = { detectionRange = it },
                warningFrequency = warningFrequency,
                onWarningFrequencyChange = { warningFrequency = it }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ── ADD THE AUDIO CUES MODULE HERE ──
            AudioCuesModuleCard()

            Spacer(modifier = Modifier.height(16.dp))


            // ── ADD THE HARDWARE INTEGRITY MODULE HERE ──
            HardwareIntegrityCard(
                batterySaverEnabled, autoOffMinutes, detectionRange,
                warningFrequency, voiceVolume, tonePitch,
                connectionStatus, connectionColor, liveDistance,
                context
            )

            // Add a little space at the bottom so the scroll feels natural above the footer
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
@Composable
fun PowerModuleCard() {
    // ── State for Interactive Elements ──
    var batterySaverEnabled by remember { mutableStateOf(true) }
    // Each segment represents 5 minutes. 3 segments = 15 minutes.
    var autoOffMinutes by remember { mutableIntStateOf(15) }

    // ── Colors ──
    val neonYellow = Color(0xFFE6E68C)
    val darkCardBg = Color(0xFF222222)
    val darkerToggleBg = Color(0xFF1A1A1A)
    val inactiveSegment = Color(0xFF333333)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = darkCardBg)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // ── Top Row: Header & Battery Percentage ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "MODULE_04",
                        color = neonYellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Power",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Battery % Box
                Box(
                    modifier = Modifier
                        .border(1.dp, neonYellow.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "82%",
                        color = neonYellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Battery Saver Toggle Box ──
            Surface(
                color = darkerToggleBg,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Battery Saver",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Extend range by 25%",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }

                    // Interactive Toggle
                    Switch(
                        checked = batterySaverEnabled,
                        onCheckedChange = { batterySaverEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = neonYellow,
                            checkedTrackColor = neonYellow.copy(alpha = 0.2f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Auto-Off Timer Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AUTO-OFF TIMER",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "$autoOffMinutes MIN",
                    color = neonYellow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Interactive Segmented Progress Bar ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Creates 5 clickable segments (5, 10, 15, 20, 25 minutes)
                for (i in 1..5) {
                    val timeValue = i * 5
                    val isActive = timeValue <= autoOffMinutes

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(
                                color = if (isActive) neonYellow else inactiveSegment,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable {
                                // Updates the timer when a segment is tapped
                                autoOffMinutes = timeValue
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Bottom Stats ──
            Text(
                text = "CURRENT DRAW: 120MA",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "EST. REMAINING: 14H 22M",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun SonarModuleCard(
    detectionRange: Float,
    onDetectionRangeChange: (Float) -> Unit,
    warningFrequency: Float,
    onWarningFrequencyChange: (Float) -> Unit
) {
    // ── Interactive State
    // ── Colors ──
    val purpleAccent = Color(0xFFA87FFB)
    val darkCardBg = Color(0xFF222222)
    val darkerBoxBg = Color(0xFF151515)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = darkCardBg)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // ── Header Section ──
            Text(
                text = "MODULE_02",
                color = purpleAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sonar & Proximity",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Adjust the ultrasonic sensing depth and alert density.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Detection Range Slider ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DETECTION RANGE",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = String.format(java.util.Locale.US, "%.1fm", detectionRange),
                    color = purpleAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = detectionRange,
                onValueChange = onDetectionRangeChange,
                valueRange = 1f..3f, // Locked exactly to your 1m - 3m requirement
                colors = SliderDefaults.colors(
                    thumbColor = purpleAccent,
                    activeTrackColor = purpleAccent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1.0M", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                Text("3.0M", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Warning Frequency Slider ──
            Text(
                text = "WARNING FREQUENCY",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Slow",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Slider(
                    modifier = Modifier.weight(1f),
                    value = warningFrequency,
                    onValueChange = onWarningFrequencyChange,
                    valueRange = 1f..3f, // Locked exactly to your 1sec - 3sec requirement
                    colors = SliderDefaults.colors(
                        thumbColor = purpleAccent,
                        activeTrackColor = purpleAccent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Fast",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Latency Check Box ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(darkerBoxBg, RoundedCornerShape(12.dp))
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Mathematically matching latency to frequency (e.g., 1.5s * 1000 = 1500ms)
                    val latencyMs = warningFrequency * 1000f

                    Text(
                        text = String.format(java.util.Locale.US, "%.2fms", latencyMs),
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "LATENCY CHECK",
                        color = purpleAccent.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AudioCuesModuleCard() {
    val context = LocalContext.current

    // ── Audio & Voice States ──
    // Volume from 0.0 to 1.0
    var voiceVolume by remember { mutableFloatStateOf(0.6f) }
    // Pitch steps: 0f = Low, 1f = Medium, 2f = High
    var tonePitch by remember { mutableFloatStateOf(1f) }

    // ── Initialize Text-To-Speech ──
    val tts = remember {
        TextToSpeech(context) { _ -> }
    }
    // Clean up TTS when leaving the screen to prevent memory leaks
    DisposableEffect(Unit) {
        onDispose { tts.shutdown() }
    }

    // ── Colors ──
    val purpleAccent = Color(0xFFA87FFB)
    val darkCardBg = Color(0xFF222222)
    val iconBoxBg = Color(0xFF2E2A36)
    val buttonBg = Color(0xFF2A2A2A)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = darkCardBg)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // ── Header ──
            Text(
                text = "MODULE_03",
                color = purpleAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Audio Cues",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Test Buttons Row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Test Tone Beep
                Button(
                    onClick = {
                        // Plays a system beep adjusting volume based on slider
                        val mappedVolume = (voiceVolume * 100).toInt()
                        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, mappedVolume)
                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("TEST TONE", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, letterSpacing = 1.sp)
                }

                // 2. Voice Sample
                Button(
                    onClick = {
                        // Adjusts pitch based on slider (0.5 = low, 1.0 = normal, 1.5 = high)
                        val pitchValue = when (tonePitch.roundToInt()) {
                            0 -> 0.5f
                            1 -> 1.0f
                            2 -> 1.5f
                            else -> 1.0f
                        }
                        tts.setPitch(pitchValue)

                        // Set custom volume for this specific utterance
                        val params = Bundle()
                        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceVolume)

                        tts.speak(
                            "This is how my voice assistant will sound.",
                            TextToSpeech.QUEUE_FLUSH,
                            params,
                            "VoiceSample"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("VOICE SAMPLE", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, letterSpacing = 1.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Download / Manage Voices Button
            Button(
                onClick = {
                    // This launches the Android System Screen to download new TTS Voices!
                    val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                    context.startActivity(installIntent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = buttonBg.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("GET MORE VOICES", color = purpleAccent.copy(alpha = 0.8f), fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Voice Volume Slider ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(iconBoxBg, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = "Voice", tint = purpleAccent, modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("VOICE VOLUME", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("${(voiceVolume * 100).toInt()}%", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    }
                    Slider(
                        value = voiceVolume,
                        onValueChange = { voiceVolume = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = purpleAccent,
                            activeTrackColor = purpleAccent.copy(alpha = 0.5f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Tone Pitch Slider ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(iconBoxBg, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = "Pitch", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val pitchText = when (tonePitch.roundToInt()) {
                        0 -> "Low"
                        1 -> "Medium"
                        2 -> "High"
                        else -> "Medium"
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TONE PITCH", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(pitchText, color = purpleAccent.copy(alpha = 0.8f), fontSize = 14.sp)
                    }
                    Slider(
                        value = tonePitch,
                        onValueChange = { tonePitch = it },
                        valueRange = 0f..2f,
                        steps = 1, // Creates exactly 3 snapping points (0, 1, and 2)
                        colors = SliderDefaults.colors(
                            thumbColor = purpleAccent,
                            activeTrackColor = purpleAccent.copy(alpha = 0.5f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun HardwareIntegrityCard(
    batterySaverEnabled: Boolean,
    autoOffMinutes: Int,
    detectionRange: Float,
    warningFrequency: Float,
    voiceVolume: Float,
    tonePitch: Float,
    connectionStatus: String,
    connectionColor: Color,
    liveDistance: String,
    context: android.content.Context
) {
    val purpleAccent = Color(0xFFA87FFB)
    val darkCardBg = Color(0xFF222222)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = darkCardBg)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // ── Header ──
            Text(
                text = "HARDWARE INTEGRITY",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── System Status Section (NOW LIVE!) ──
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoRow(label = "Cloud Connection", value = connectionStatus, valueColor = connectionColor)
                InfoRow(label = "Live Sonar Ahead", value = liveDistance, valueColor = Color.White)
                InfoRow(label = "Firmware", value = "V2.4.0 (Latest)", valueColor = Color.White.copy(alpha = 0.5f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Apply Configuration Button ──
            Button(
                onClick = {
                    // PUSH THE SETTINGS TO FIREBASE! (Using your Silver Bullet URL)
                    val configRef = com.google.firebase.database.FirebaseDatabase.getInstance("https://companionapp-prod-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("NaviSense/Config")

                    configRef.child("battery_saver").setValue(batterySaverEnabled)
                    configRef.child("auto_off_mins").setValue(autoOffMinutes)
                    configRef.child("detection_range_m").setValue(detectionRange)
                    configRef.child("warning_freq_s").setValue(warningFrequency)
                    configRef.child("voice_volume").setValue(voiceVolume)
                    configRef.child("tone_pitch").setValue(tonePitch)

                    android.widget.Toast.makeText(context, "Configurations pushed to Cloud!", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = purpleAccent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "APPLY CONFIG TO STICK",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
// Helper Composable for each InfoRow
@Composable
fun InfoRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}