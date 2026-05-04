package com.example.companion

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
    var weatherData by remember { mutableStateOf(LiveWeatherData(condition = "Checking GPS...")) }

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
    val navItems = listOf(
        // Slot 1: Home Button (Leftmost)
        BottomNavItem(
            title = "Home",
            icon = Icons.Default.Home,
            onClick = { Toast.makeText(context, "You are already home", Toast.LENGTH_SHORT).show() }
        ),
        // Slot 2: Stick Button
        BottomNavItem(
            title = "Stick",
            icon = Icons.Default.Accessibility, // A good placeholder for accessibility devices
            onClick = { Toast.makeText(context, " Personalized Stick page coming soon!", Toast.LENGTH_SHORT).show() }
        ),
        // Slot 3: Highlighted Speak Button (Middle)
        BottomNavItem(
            title = "Speak",
            icon = Icons.Default.Mic,
            isHighlighted = true, // Tell the system this button is special!
            onClick = {
                // This triggers the speech recognition you set up earlier
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                }
                speechLauncher.launch(intent)
            }
        ),
        // (Slot 4: We will skip this slot for now)

        // Slot 5: Profile Button (Rightmost)
        BottomNavItem(
            title = "Profile",
            icon = Icons.Default.AccountCircle,
            onClick = { Toast.makeText(context, "Opening Profile...", Toast.LENGTH_SHORT).show() }
        )
    )

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
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Half (Menu + Title)
                        Row(
                            modifier = Modifier.weight(1f),
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

                        // Right Half (SOS Button - takes 50% width)
                        Button(
                            onClick = {
                                if (guardianPhone != null) triggerSosCall(context, guardianPhone!!)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .padding(end = 16.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "SOS")
                            Spacer(Modifier.width(8.dp))
                            Text("SOS", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            bottomBar = {
                // We use a Box so we can draw the Speak button ON TOP of the slim footer panel
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // 1. The Slimmer Background Panel
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
                                // REDUCED PADDING: We shrunk this from 16.dp to 8.dp to slim the footer
                                .padding(top = 8.dp, bottom = 8.dp)
                                .navigationBarsPadding(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // 1. Home Button
                            Column(
                                modifier = Modifier.size(56.dp).clickable { /* TODO: Navigate to Home */ },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Home, "Home", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                Spacer(Modifier.height(2.dp))
                                Text("Home", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }

                            // 2. Stick Button
                            Column(
                                modifier = Modifier.size(56.dp).clickable { /* TODO: Open Stick settings */ },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Accessibility, "Stick", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                Spacer(Modifier.height(2.dp))
                                Text("Stick", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }

                            // 3. Invisible Spacer
                            // This holds the physical empty gap in the Row so the floating button has a place to sit
                            Spacer(modifier = Modifier.width(56.dp))

                            // 4. The Blank Slot
                            Box(modifier = Modifier.size(56.dp))
                            // 4. The Contact Button (Fourth from left)
                            Column(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clickable {
                                        if (guardianPhone != null) {
                                            // Speaks out loud to confirm, then triggers the phone call
                                            tts.speak("Calling guardian", TextToSpeech.QUEUE_FLUSH, null, null)
                                            triggerSosCall(context, guardianPhone!!)
                                        } else {
                                            // Failsafe if they haven't linked a guardian yet
                                            tts.speak("No guardian connected yet", TextToSpeech.QUEUE_FLUSH, null, null)
                                        }
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Contact",
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Contact",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }

                            // 5. Profile Button
                            Column(
                                modifier = Modifier.size(56.dp).clickable { /* TODO: Open Profile */ },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.AccountCircle, "Profile", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                Spacer(Modifier.height(2.dp))
                                Text("Profile", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }

                    // 2. The Overlapping Speak Button
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
                            // We align it to the TopCenter of the Box (which is the top curved edge of the footer)
                            .align(Alignment.TopCenter)
                            // This offset pushes it UP into the screen, making it break out of the footer!
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
        ){ innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
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

                    Spacer(Modifier.height(24.dp))

                    Spacer(Modifier.height(16.dp))

                    Spacer(Modifier.height(16.dp))

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

                    // ── SOS Emergency Contact Card ────────────────────────────
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (guardianPhone != null)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (guardianPhone != null)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "SOS Emergency Contact",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = guardianPhone ?: "No guardian connected yet",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
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
fun BlindPersonBottomBar(items: List<BottomNavItem>) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant, // Give it a distinct panel background
        shadowElevation = 16.dp, // Nice shadow for separation
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) // Round the top corners
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(), // Ensures it works with different system nav bars
            horizontalArrangement = Arrangement.SpaceBetween, // Automatically handle spacing!
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Loop through each of our 4 items
            items.forEachIndexed { index, item ->

                // --- Insert the blank slot logic before the 4th item (Profile) ---
                if (index == 3) {
                    // This creates an empty box that takes up the same space as a button
                    Box(modifier = Modifier.size(width = 60.dp, height = 60.dp))
                }

                if (item.isHighlighted) {
                    // --- Special layout for the central, highlighted Speak button ---
                    Column(
                        modifier = Modifier
                            .clickable { item.onClick() }
                            .padding(12.dp)
                            .size(60.dp) // Total size of the special element
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer, // High-contrast, highlighted color
                                shape = CircleShape // Round background shape
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer, // Text color on highlighted container
                            modifier = Modifier.size(28.dp) // A bit bigger icon
                        )
                    }
                } else {
                    // --- Standard layout for other buttons (Home, Stick, Profile) ---
                    Column(
                        modifier = Modifier
                            .clickable { item.onClick() }
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), // Softer color for secondary buttons
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.title,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

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
fun StickStatusCard(isConnected: Boolean, batteryLevel: Int) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        // Uses the standard background color for your app theme
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, // Pushes one item left, one item right!
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ── LEFT SIDE: Bluetooth Connection ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                // A green dot indicator to make it really obvious it's connected
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (isConnected) Color(0xFF4CAF50) else Color.Red,
                            shape = CircleShape
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                    contentDescription = "Bluetooth Status",
                    tint = if (isConnected) Color(0xFF4CAF50) else Color.Red,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = if (isConnected) "Stick Connected" else "Disconnected",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) Color(0xFF4CAF50) else Color.Red
                )
            }

            // ── RIGHT SIDE: Battery Percentage ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$batteryLevel%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.BatteryFull,
                    contentDescription = "Battery Level",
                    // Turns the battery icon green if above 20%, red if it's dying!
                    tint = if (batteryLevel > 20) Color(0xFF4CAF50) else Color.Red,
                    modifier = Modifier.size(24.dp)
                )
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