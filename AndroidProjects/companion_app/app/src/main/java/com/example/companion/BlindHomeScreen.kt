package com.example.companion

import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.compose.material.icons.outlined.WarningAmber
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.Call
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccountCircle
import android.widget.Toast
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.util.Locale

// Data class for a single bottom navigation item
data class BottomNavItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit,
    val isHighlighted: Boolean = false // Special property for the middle Speak button
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlindHomeScreen(navController: NavController) {
    val context = LocalContext.current
    // Start the background service so it listens to Firebase even if the app gets closed
    LaunchedEffect(Unit) {
        val serviceIntent = Intent(context, SosBackgroundService::class.java)
        context.startService(serviceIntent)
    }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

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

    // ── Unique ID & User Details ──────────────────────────────────────────────
    val myUniqueId = remember {
        val prefs = context.getSharedPreferences("companion_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("blind_id", null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
                .replace("-", "")
                .take(6)
                .uppercase()
            prefs.edit().putString("blind_id", id).apply()
        }
        id!!
    }

    // ── Live User Details ──
    var myName by remember { mutableStateOf("Loading...") }
    var myEmail by remember { mutableStateOf("Loading...") }

    // ── State ─────────────────────────────────────────────────────────────────
    // ── Live Movement Detection ───────────────────────────────────────────────
    val movementDetector = remember { MovementDetector(context) }
    // This automatically watches the Flow we created and updates the UI instantly
    val isWalking by movementDetector.isWalkingFlow.collectAsState()
    // ── Live Weather & GPS State ─────────────────────────────────────────────
    // ── Live Weather & GPS State ─────────────────────────────────────────────
    var weatherData by remember { mutableStateOf(LiveWeatherData(condition = "Checking GPS...")) }

    // ── Live Hazard State ──────────────────────────────────────────────────
    // For now, we simulate fetching these short lines from the internet/maps
    var nearbyHazards by remember {
        mutableStateOf(
            listOf(
                "Construction work on left sidewalk.",
                "Uneven pavement reported 5 meters ahead."
            )
        )
    }

    // We deleted the duplicate 'val context = LocalContext.current' that was here!
    // We deleted the duplicate 'val context = LocalContext.current' that was here!

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // 1. Create a tool to ask the user for GPS permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 2. If they say YES, grab the exact location
        if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {

            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        // 3. We have the live GPS coordinates! Fetch the weather.
                        CoroutineScope(Dispatchers.Main).launch {
                            weatherData = fetchLiveWeather(
                                apiKey = "3d71904c91ec985a08271553a39559e2",
                                lat = location.latitude,
                                lon = location.longitude
                            )
                        }
                    } else {
                        weatherData = weatherData.copy(condition = "GPS Signal Lost")
                    }
                }
            } catch (e: SecurityException) {
                weatherData = weatherData.copy(condition = "GPS Error")
            }
        } else {
            // If they say NO
            weatherData = weatherData.copy(condition = "Permission Denied")
        }
    }

    // 4. Automatically trigger the permission check when the screen opens
    LaunchedEffect(Unit) {
        // Check if we already have permission from a previous launch
        val hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation) {
            // We already have permission, grab the location directly!
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        // WE ADDED THE MISSING COROUTINE WRAPPER RIGHT HERE:
                        CoroutineScope(Dispatchers.Main).launch {
                            weatherData = fetchLiveWeather(
                                apiKey = "3d71904c91ec985a08271553a39559e2",
                                lat = location.latitude,
                                lon = location.longitude
                            )
                        }
                    }
                }
            } catch (e: SecurityException) { }
        } else {
            // We don't have permission yet, launch the popup to ask the user
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }
    val stepCount by movementDetector.stepCountFlow.collectAsState()
    DisposableEffect(Unit) {
        movementDetector.start() // Turn the sensor on when the screen opens
        onDispose {
            movementDetector.stop() // Turn the sensor off to save battery when the screen closes
        }
    }
    var locationText by remember { mutableStateOf("Waiting for movement...") }
    var guardianPhone by remember { mutableStateOf<String?>(null) }
    var profileExpanded by remember { mutableStateOf(false) }

    // ── Load guardian phone + register in Firestore ───────────────────────────
    LaunchedEffect(Unit) {
        // NEW: Fetch live user profile data
        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser != null) {
            myEmail = authUser.email ?: "No email linked"

            FirebaseFirestore.getInstance().collection("Users").document(authUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // We will look for a 'name' field. If they haven't set one yet, we provide a default.
                        myName = document.getString("name") ?: "Companion User"
                    }
                }
        }
        FirebaseManager.registerBlindPerson(myUniqueId)

        FirebaseManager.getGuardianPhone(myUniqueId) { phone ->
            if (phone != null) {
                guardianPhone = phone
                context.getSharedPreferences("companion_prefs", Context.MODE_PRIVATE)
                    .edit().putString("guardian_phone", phone).apply()
            } else {
                val prefs = context.getSharedPreferences("companion_prefs", Context.MODE_PRIVATE)
                guardianPhone = prefs.getString("guardian_phone", null)
            }
        }
    }


    // ── Location tracking — pushes to Firestore when walking ──────────────────
    LaunchedEffect(isWalking) {
        if (isWalking) {
            val tracker = LocationTracker(context)
            tracker.getLocationUpdates(4000L).collect { loc ->
                locationText = "📍 ${loc.latitude.fmt(5)}, ${loc.longitude.fmt(5)}"
                FirebaseManager.updateBlindLocation(myUniqueId, loc.latitude, loc.longitude)
            }
        } else {
            locationText = "Standing Still. Location paused."
            FirebaseManager.stopWalk(myUniqueId)
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


    // ── UI ────────────────────────────────────────────────────────────────────
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Companion Menu",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider()

                // Expandable Profile Section
                NavigationDrawerItem(
                    label = { Text("My Profile") },
                    selected = profileExpanded,
                    onClick = { profileExpanded = !profileExpanded },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
                if (profileExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)
                    ) {
                        Text("Name: $myName", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))
                        Text("ID: $myUniqueId", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))
                        // Changed this from Phone to Email:
                        Text("Email: $myEmail", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // SOS List Button
                NavigationDrawerItem(
                    label = { Text("SOS List") },
                    selected = false,
                    onClick = { /* TODO: Navigate to SOS List Screen */ },
                    icon = { Icon(Icons.Default.List, contentDescription = "SOS List") }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Half (Menu + Title)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                            val firstName = myName.substringBefore(" ")
                            Text(
                                text = "Hello, $firstName \uD83D\uDC4B", // Added a little wave emoji!
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1 // Keeps it strictly on one line even if the name is long
                            )
                        }

                        // 2. THE MAGIC SPACER: This expands to push the Bluetooth button to the far right
                        Spacer(modifier = Modifier.weight(1f))

                        // 3. Right Side (Bluetooth Settings Button)
                        IconButton(
                            onClick = {
                                tts.speak("Opening Bluetooth Settings", TextToSpeech.QUEUE_FLUSH, null, null)
                                navController.navigate("bluetoothSettings")
                            },
                            // Add the background here: a subtle purple (from your palette) with high transparency, and rounded corners
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(
                                    color = Color(0xFFA87FFB).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Bluetooth,
                                contentDescription = "Bluetooth Settings",
                                tint = Color(0xFFA87FFB) // Changed to purple to match the background!
                            )
                        }

                    }
                }
            },
            bottomBar = {
                CompanionBottomBar(
                    onHomeClick = { Toast.makeText(context, "You are already home", Toast.LENGTH_SHORT).show() },
                    onStickClick = { navController.navigate("StickScreen") { launchSingleTop = true } },
                    onSpeakClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        }
                        speechLauncher.launch(intent)
                    },
                    onContactsClick = {
                        tts.speak("Opening your trusted contacts.", TextToSpeech.QUEUE_FLUSH, null, null)
                        navController.navigate("contacts") { launchSingleTop = true }
                    },
                    onProfileClick = {
                        tts.speak("Opening profile.", TextToSpeech.QUEUE_FLUSH, null, null)
                        navController.navigate("profileScreen") { launchSingleTop = true }
                    },
                    selectedTab = 0
                )
            },
            floatingActionButton = {
                FloatingSOSButton()
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                // 1. Changed Center to TopCenter so it scrolls naturally from the top down
                contentAlignment = Alignment.TopCenter
            ) {
                // 2. Remember the scroll position
                val scrollState = rememberScrollState()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp) // Gives a little breathing room at the top
                        .verticalScroll(scrollState) // 3. This makes the whole page scrollable!
                ) {
                    // ── Status & Steps Row ────────────────────────────────────
                    // ── Status & Steps Row ────────────────────────────────────
                    // ── Steps & Status Area (Right Aligned) ─────────────────────────
                    // ── Centered Step & Status Tracker ─────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        contentAlignment = Alignment.Center // This forces the card into the exact middle!
                    ) {
                        // Calls the new UI card and passes all the data it needs
                        StepTrackerCard(
                            stepCount = stepCount,
                            isWalking = isWalking,
                            locationText = locationText
                        )
                    }
                    // ── Hardware Status Card (Bluetooth & Battery) ────────
                    StickStatusCard(
                        isConnected = true,
                        batteryLevel = 85
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── NEW: Weather Safety Card ────────
                    // For now, we pass the exact design data. We will replace these
                    // with live variables once the API is connected!

                    // ── LIVE: Weather Safety Card ────────
                    WeatherSafetyCard(
                        condition = weatherData.condition,
                        temperature = weatherData.temperature,
                        visibility = weatherData.visibility,
                        safetyLevel = weatherData.safetyLevel,
                        safetyColor = weatherData.safetyColor,
                        cityName = weatherData.cityName
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── NEW: Live Hazard Alerts ────────
                    HazardAlertCard(
                        hazards = nearbyHazards,
                        searchRadius = 15
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── NEW: Safe Zone & AI Insights ────────
                    StatusCardsRow()

                    Spacer(Modifier.height(16.dp))

                    // ── NEW: Emergency Protocol Button ────────
                    EmergencyProtocolCard(
                        onClick = {
                            // 1. The TTS Add-on you requested
                            tts.speak("Emergency protocol menu opened.", TextToSpeech.QUEUE_FLUSH, null, null)

                            // 2. Open the new settings page
                            navController.navigate("emergencyProtocol")
                        }
                    )

                    Spacer(Modifier.height(24.dp))
                    // ── Unique ID Card ────────────────────────────────────────

                    // ── Unique ID Card ────────────────────────────────────────
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Your Companion ID",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = myUniqueId,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 8.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Share with your guardian to connect",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            )
                        }
                    }



                    // Large spacer to prevent content from hiding behind the big FAB
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

// ── SOS CALL ──────────────────────────────────────────────────────────────────
fun triggerSosCall(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone"))
    context.startActivity(intent)
}

private fun Double.fmt(d: Int) = "%.${d}f".format(this)



@Composable
fun StepTrackerCard(stepCount: Int, isWalking: Boolean, locationText: String) {
    // Creates the beautiful two-tone purple gradient background
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE8E0FF), // Light pastel purple (top left)
            Color(0xFFBCA6FF)  // Richer purple (bottom right)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth() // Fills the available centered space
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // ── LEFT SIDE: The Icon in the bordered circle ──
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(1.dp, Color.Black, CircleShape)
                        // A slightly translucent purple behind the icon
                        .background(Color(0xFFD4BFFF).copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsWalk,
                        contentDescription = "Walking",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // ── RIGHT SIDE: The Text Column ──
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Big Step Number with a Drop Shadow
                    Text(
                        text = "$stepCount",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.25f),
                                offset = Offset(2f, 4f),
                                blurRadius = 4f
                            )
                        )
                    )

                    Text(
                        text = "Steps",
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // The dynamic status text right below "Steps"
                    Text(
                        text = if (isWalking) "🚶 You are walking. Location is tracking." else "🧍 You are standing still..",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )

                    // Displays the actual location text seamlessly inside the card if walking
                    if (isWalking && locationText.isNotEmpty()) {
                        Text(
                            text = locationText,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StickStatusCard(
    isConnected: Boolean,
    batteryLevel: Int,
    lastUpdated: String = "Just Now" // Ready for live timestamps later!
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E)), // Deep dark gray to match your theme
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // ── ROW 1: Stick Status ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The rounded square icon box
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF2A2A2E), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiTethering,
                        contentDescription = "Connection",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "STICK STATUS",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isConnected) "Online" else "Offline",
                        // That sleek neon greenish-yellow for "Online", red for "Offline"
                        color = if (isConnected) Color(0xFFE6EE9C) else Color(0xFFE57373),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // A subtle divider line
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 20.dp),
                color = Color.White.copy(alpha = 0.05f)
            )

            // ── ROW 2: Battery ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF2A2A2E), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryFull,
                        contentDescription = "Battery",
                        // Purple tint to match your app's accent color, rotated 90deg to lay flat like your image
                        tint = Color(0xFFA87FFB),
                        modifier = Modifier.size(28.dp).rotate(90f)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "BATTERY",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$batteryLevel%",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 20.dp),
                color = Color.White.copy(alpha = 0.05f)
            )

            // ── ROW 3: Last Updated ──
            Column {
                Text(
                    text = "LAST UPDATED",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Small green indicator dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isConnected) Color(0xFF81C784) else Color.Gray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = lastUpdated,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherSafetyCard(
    condition: String,
    temperature: Int,
    visibility: String,
    safetyLevel: String,
    safetyColor: Color,
    cityName: String
) {
    // We removed the badgeBackground color since we no longer need it!
    val cardBackground = Color(0xFF1A1A1E)
    val purpleAccent = Color(0xFFA87FFB)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // ── TOP ROW: Cloud Icon & City Name ──
            // Simplified this row to just hold the title cleanly on the left
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Cloud,
                    contentDescription = "Weather",
                    tint = purpleAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "WEATHER • ${cityName.uppercase()}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── BOTTOM ROW: Data & Safety Level ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left: Condition & Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = condition,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Visibility: $visibility | Temp: $temperature°C",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Right: Safety Level
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SAFETY LEVEL",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = safetyLevel,
                        color = safetyColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

// A simple container to hold our fetched weather data
data class LiveWeatherData(
    val condition: String = "Loading...",
    val temperature: Int = 0,
    val visibility: String = "Checking...",
    val safetyLevel: String = "UNKNOWN",
    val safetyColor: Color = Color.Gray,
    val cityName: String = "Locating..." // ── NEW: Holds the city name
)

// The function that actually talks to the internet in the background
// The function that actually talks to the internet in the background
suspend fun fetchLiveWeather(apiKey: String, lat: Double, lon: Double): LiveWeatherData {
    return withContext(Dispatchers.IO) {
        try {
            val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric"
            val response = URL(url).readText()
            val json = JSONObject(response)

            // ── NEW: Extract the city name from the JSON ──
            val cityName = json.optString("name", "Unknown Area")

            val temp = json.getJSONObject("main").getDouble("temp").toInt()
            val weatherArray = json.getJSONArray("weather")
            val rawCondition = if (weatherArray.length() > 0) weatherArray.getJSONObject(0).getString("main") else "Clear"

            val visMeters = json.optInt("visibility", 10000)
            val visibility = when {
                visMeters < 1000 -> "Low"
                visMeters < 5000 -> "Moderate"
                else -> "Good"
            }

            var safetyLevel = "SAFE"
            var safetyColor = Color(0xFF4CAF50)
            var displayCondition = "$rawCondition Expected"

            when {
                rawCondition.contains("Rain", ignoreCase = true) || rawCondition.contains("Thunderstorm", ignoreCase = true) -> {
                    safetyLevel = "MODERATE"
                    safetyColor = Color(0xFFA87FFB)
                    displayCondition = "Heavy Rain Expected"
                }
                rawCondition.contains("Snow", ignoreCase = true) || temp > 40 -> {
                    safetyLevel = "DANGEROUS"
                    safetyColor = Color.Red
                    displayCondition = if (temp > 40) "Extreme Heat" else "Extreme Weather"
                }
            }

            // ── UPDATE: Pass the cityName into the returned data ──
            LiveWeatherData(displayCondition, temp, visibility, safetyLevel, safetyColor, cityName)

        } catch (e: Exception) {
            LiveWeatherData("Weather Unavailable", 0, "Unknown", "UNKNOWN", Color.Gray, "Error")
        }
    }
}

@Composable
fun HazardAlertCard(
    hazards: List<String>, // We take a list of strings (the short alerts)
    searchRadius: Int = 15 // Default to 15 meters as you suggested
) {
    // Only show the card if there are actually hazards to report!
    if (hazards.isNotEmpty()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                // Use a soft, high-contrast warning color (like a dark amber/red)
                containerColor = Color(0xFF3E2723) // Dark brown/red
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // ── HEADER: Icon & Title ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = "Warning",
                        tint = Color(0xFFFFB300), // Bright amber warning color
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "HAZARDS WITHIN ${searchRadius}m",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── HAZARD LIST ──
                hazards.forEachIndexed { index, hazard ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // A small bullet point
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp, end = 8.dp)
                                .size(6.dp)
                                .background(Color(0xFFFFB300), CircleShape)
                        )
                        Text(
                            text = hazard,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp
                        )
                    }
                    // Add a tiny divider between items, but not after the last one
                    if (index < hazards.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusCardsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp) // Puts a perfect gap between the cards
    ) {
        // ── CARD 1: Safe Zone Status ──
        DashboardSmallCard(
            modifier = Modifier.weight(1f), // weight(1f) makes them share 50% of the screen each
            icon = Icons.Outlined.VerifiedUser,
            iconTint = Color(0xFFA87FFB), // Neon Purple
            iconBg = Color(0xFF2D2440),   // Dark Purple BG
            badgeText = "SECURE",
            badgeColor = Color(0xFFA87FFB),
            badgeBg = Color(0xFF2D2440),
            title = "SAFE ZONE STATUS",
            subtitle = "Residential Area - Level 1"
        )

        // ── CARD 2: AI Insights ──
        DashboardSmallCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Psychology,
            iconTint = Color(0xFFE0E0E0), // Light Grey
            iconBg = Color(0xFF2A2A2E),   // Dark Grey BG
            badgeText = "ACTIVE",
            badgeColor = Color(0xFFE0E0E0),
            badgeBg = Color(0xFF2A2A2E),
            title = "NEARBY AI INSIGHTS",
            subtitle = "Clear path ahead; low traffic detected."
        )
    }
}

@Composable
fun DashboardSmallCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    badgeText: String,
    badgeColor: Color,
    badgeBg: Color,
    title: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E)) // Deep dark gray
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // ── Top Row: Icon Box & Badge ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // The square icon box with rounded corners
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconBg, RoundedCornerShape(10.dp))
                        .border(1.dp, iconTint.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // The little text badge (SECURE / ACTIVE)
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Bottom Row: Text ──
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun EmergencyProtocolCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        // A very dark, subtle reddish-black background
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1414))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // ── The Left Icon Box ──
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF332020), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE57373).copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Using a large asterisk just like your image!
                    Text(
                        text = "*",
                        color = Color(0xFFE57373),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Medium,
                        // Nudge the asterisk down slightly so it visually centers perfectly
                        modifier = Modifier.offset(y = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // ── The Text Column ──
                Column {
                    Text(
                        text = "EMERGENCY PROTOCOL",
                        color = Color(0xFFE57373), // Salmon / Light Red
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "TAP FOR IMMEDIATE SUPPORT",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // ── The Right Arrow ──
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Proceed",
                tint = Color(0xFFE57373).copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun FloatingSOSButton() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("companion_prefs", Context.MODE_PRIVATE)

    // Automatically grabs whatever guardian number is saved!
    val primarySosPhone = prefs.getString("guardian_phone", "")

    val tts = remember { TextToSpeech(context) {}.apply { language = Locale("en", "IN") } }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    var phoneToCall by remember { mutableStateOf("") }

    // Permission Launcher specific to this button
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && phoneToCall.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneToCall"))
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Permission denied for SOS call.", Toast.LENGTH_SHORT).show()
        }
    }

    FloatingActionButton(
        onClick = {
            if (!primarySosPhone.isNullOrEmpty()) {
                // Announce and Call
                tts.speak("Initiating SOS", TextToSpeech.QUEUE_FLUSH, null, null)

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$primarySosPhone"))
                    context.startActivity(intent)
                } else {
                    phoneToCall = primarySosPhone
                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                }
            } else {
                tts.speak("No primary guardian set.", TextToSpeech.QUEUE_FLUSH, null, null)
                Toast.makeText(context, "Please set a Guardian in Emergency Protocol first", Toast.LENGTH_LONG).show()
            }
        },
        containerColor = Color(0xFFD4BFFF), // The sleek light purple from your image
        contentColor = Color(0xFF2D2440),   // Deep dark purple text
        shape = RoundedCornerShape(16.dp),  // Perfectly rounded square
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
    ) {
        Text(
            text = "SOS",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun CompanionBottomBar(
    onHomeClick: () -> Unit,
    onStickClick: () -> Unit,
    onSpeakClick: () -> Unit,
    onContactsClick: () -> Unit,
    onProfileClick: () -> Unit,
    selectedTab: Int = -1
) {
    val navColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val highlightColor = Color(0xFFA87FFB)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. The main panel
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 16.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItemUi("Home", Icons.Default.Home, selectedTab == 0, onHomeClick, if (selectedTab == 0) highlightColor else navColor)
                BottomNavItemUi("Stick", Icons.Default.Accessibility, selectedTab == 1, onStickClick, if (selectedTab == 1) highlightColor else navColor)

                // Invisible Spacer for the floating button
                Spacer(modifier = Modifier.width(56.dp))

                BottomNavItemUi("Contact", Icons.Default.Call, selectedTab == 2, onContactsClick, if (selectedTab == 2) highlightColor else navColor)
                BottomNavItemUi("Profile", Icons.Default.AccountCircle, selectedTab == 3, onProfileClick, if (selectedTab == 3) highlightColor else navColor)
            }
        }

        // 2. The overlapping floating Speak button
        ExtendedFloatingActionButton(
            onClick = onSpeakClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-36).dp)
                .size(width = 85.dp, height = 85.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Tap to Speak",
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

// A small helper to draw the individual icons
@Composable
private fun BottomNavItemUi(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit, tintColor: Color) {
    Column(
        modifier = Modifier.size(56.dp).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(28.dp), tint = tintColor)
        Spacer(Modifier.height(2.dp))
        Text(text = title, fontSize = 11.sp, color = tintColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}