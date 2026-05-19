package com.example.companion

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.os.Build
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.util.Locale

@SuppressLint("MissingPermission")
@Composable
fun BluetoothSettingsScreen(navController: NavController, viewModel: BlindViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── BLUETOOTH ADAPTER ACCESS ──
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    // ── TEXT-TO-SPEECH (For accessibility) ──
    val tts = remember { TextToSpeech(context) {}.apply { language = Locale("en", "IN") } }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    // ── LIVE STATE VARIABLES (From ViewModel & Hardware) ──

    // Initialize with the true hardware state
    var isBtEnabled by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }

    // ── NEW: LIVE HARDWARE DATA RECEIVER ──
    val pairedDevices by viewModel.pairedDevices.collectAsState() // Add this near your other viewmodel variables

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    // 1. Bluetooth flipped On/Off
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        if (state == BluetoothAdapter.STATE_ON) {
                            isBtEnabled = true

                            // Instantly grab all known/paired devices from the phone's memory
                            val bonded = bluetoothAdapter?.bondedDevices?.map {
                                BluetoothDeviceModel(it.address, it.name ?: "Unknown", -50, true)
                            }?.filter { it.name != "Unknown" } ?: emptyList()

                            viewModel.updatePairedDevices(bonded)

                            // Auto-start scanning for new devices
                            bluetoothAdapter?.startDiscovery()
                        } else if (state == BluetoothAdapter.STATE_OFF) {
                            isBtEnabled = false
                            viewModel.updatePairedDevices(emptyList())
                            viewModel.clearDiscoveredDevices()
                        }
                    }

                    // 2. The Antenna caught a live device signal!
                    BluetoothDevice.ACTION_FOUND -> {
                        @Suppress("DEPRECATION")
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()

                        if (device != null && device.name != null) {
                            // Send the device and its live signal strength to the ViewModel
                            viewModel.addOrUpdateDiscoveredDevice(
                                BluetoothDeviceModel(device.address, device.name, rssi, false)
                            )
                        }
                    }

                    // 3. Scan Status Updates
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> viewModel.setScanningState(true)
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> viewModel.setScanningState(false)
                }
            }
        }

        // Register the antenna listener
        val filter = android.content.IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(receiver, filter)

        // Safety cleanup when leaving the screen
        onDispose {
            context.unregisterReceiver(receiver)
            try { bluetoothAdapter?.cancelDiscovery() } catch(e: Exception){}
        }
    }

    val isScanning by viewModel.isScanning.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val foundDevices by viewModel.foundDevices.collectAsState()
    val guardianStick by viewModel.guardianStick.collectAsState()

    // Mock Guardian Stick data from image_13.png
    //val guardianStickMock = BluetoothDeviceModel(id = "GP-882-XQ", name = "Guardian Stick", rssi = -40)

    // ── LAUNCHERS (Required for permissions and intents) ──

    // Launcher 1: User response to BT activation request
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            isBtEnabled = true
            tts.speak("Bluetooth enabled.", TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            isBtEnabled = false
            tts.speak("Bluetooth activation canceled.", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // Launcher 2: Handling Permission requests cleanly (Android 12+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val canScan = permissions[Manifest.permission.BLUETOOTH_SCAN] == true
        val canConnect = permissions[Manifest.permission.BLUETOOTH_CONNECT] == true

        if (canScan && canConnect) {
            // Permission granted, initiate request
            requestBtEnablement(context, bluetoothAdapter, enableBluetoothLauncher, tts)
        } else {
            Toast.makeText(context, "Permissions required for stick connection.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = Color(0xFF121212), // Deep dark background
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
        },
        // We add the Distorted image's "RESCAN FOR DEVICES" floating action button (FAB) cleanly at the bottom
        floatingActionButton = {
            if (isBtEnabled) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (!isScanning) {
                            tts.speak("Rescanning.", TextToSpeech.QUEUE_FLUSH, null, null)
                            // Call scan function from ViewModel here in Op C
                        }
                    },
                    containerColor = Color(0xFF1A1A1E), // Match card background
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Default.Refresh, "Rescan", tint = Color.White.copy(alpha = 0.8f)) },
                    text = { Text("RESCAN FOR DEVICES", fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, letterSpacing = 1.sp) },
                    modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
                )
            }
        }
    ) { innerPadding ->

        // ── WE USE ONE SINGLE LAZYCOLUMN FOR THE ENTIRE SCREEN ──
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 40.dp), // Maintain top margin
            contentPadding = PaddingValues(bottom = 120.dp) // Keeps list from hiding behind FAB
        ) {

            // ── 1. CONCENTRIC RADAR DESIGN (Now scrollable) ──
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)
                ) {
                    Box(modifier = Modifier.size(240.dp).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(32.dp)))
                    Box(modifier = Modifier.size(160.dp).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)))
                    Box(
                        modifier = Modifier.size(80.dp).background(Color(0xFF2A2A2E), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Default.BluetoothSearching else Icons.Default.Bluetooth,
                            contentDescription = "Searching",
                            tint = Color(0xFFA87FFB), // The light purple accent
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // ── 2. TEXT HEADERS & TOGGLE (Now scrollable) ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = if (isScanning) "Searching for Devices" else "Bluetooth Config", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = if (isBtEnabled) "STATUS: ACTIVE" else "STATUS: OFF", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }

                    // USER REQUEST: BLUETOOTH ON/OFF TOGGLE
                    Switch(
                        checked = isBtEnabled,
                        onCheckedChange = { desiredState ->
                            if (desiredState) {
                                // 1. APP WANTS TO TURN SYSTEM BT ON
                                try {
                                    // Attempt direct, aggressive turn-on (Deprecated by Google, but works on some devices)
                                    @Suppress("DEPRECATION")
                                    val success = bluetoothAdapter?.enable() == true

                                    if (!success) {
                                        // Fallback: If Android blocks the silent turn-on, ask via the official system popup
                                        handleBluetoothActivate(context, permissionLauncher, tts)
                                    } else {
                                        tts.speak("Enabling Bluetooth hardware.", TextToSpeech.QUEUE_FLUSH, null, null)
                                    }
                                } catch (e: SecurityException) {
                                    // Fallback: If we lack permissions for silent turn-on
                                    handleBluetoothActivate(context, permissionLauncher, tts)
                                }

                            } else {
                                // 2. APP WANTS TO TURN SYSTEM BT OFF
                                try {
                                    // Attempt direct, aggressive turn-off
                                    @Suppress("DEPRECATION")
                                    val success = bluetoothAdapter?.disable() == true

                                    if (success) {
                                        Toast.makeText(context, "Bluetooth Disabled.", Toast.LENGTH_SHORT).show()
                                        tts.speak("Bluetooth hardware off.", TextToSpeech.QUEUE_FLUSH, null, null)
                                    } else {
                                        // Android 13+ blocked the silent disable.
                                        // We must open the system settings so the user can do it manually.
                                        Toast.makeText(context, "Android security requires disabling via Settings.", Toast.LENGTH_LONG).show()
                                        val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                } catch (e: SecurityException) {
                                    // Android 13+ threw an error to block the silent disable.
                                    Toast.makeText(context, "Android security requires disabling via Settings.", Toast.LENGTH_LONG).show()
                                    val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                                    context.startActivity(intent)
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFA87FFB),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF2A2A2E)
                        )
                    )
                }
            }

            // ── 3. DEVICE LIST SECTION ──
            if (isBtEnabled) {
                // 1. Preferred/Guardian Stick Section
                if (guardianStick != null) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 24.dp)) {
                            PrimaryDeviceCard(device = guardianStick!!, status = connectionStatus, viewModel = viewModel, tts = tts)
                        }
                    }
                }

                // 2. NEW: PAIRED DEVICES (Instantly populated when BT turns on)
                if (pairedDevices.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(text = "PAIRED DEVICES", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    items(count = pairedDevices.size) { index ->
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            DiscoveredDeviceItem(device = pairedDevices[index], tts = tts)
                        }
                    }
                }

                // 3. AVAILABLE (LIVE SCANNING) DEVICES
                if (foundDevices.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(text = "OTHER AVAILABLE DEVICES", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    items(count = foundDevices.size) { index ->
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            DiscoveredDeviceItem(device = foundDevices[index], tts = tts)
                        }
                    }
                }
            } else {
                // UI state when Bluetooth is off
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Turn on Bluetooth to scan for your Guardian Stick.",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── UTILITY FUNCTIONS & COMPONENTS (PLACE BELOW main BluetoothSettingsScreen) ──

// Clean component for the Preferred Device Card matching image_13.png structure
@Composable
fun PrimaryDeviceCard(device: BluetoothDeviceModel, status: String, viewModel: BlindViewModel, tts: TextToSpeech) {
    var connecting by remember { mutableStateOf(status == "Connecting") }
    connecting = status == "Connecting"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1414)), // Deep dark gray to match your theme
        modifier = Modifier.fillMaxWidth(),
        border = if (status == "Connected") androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA87FFB).copy(alpha = 0.5f)) else null
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {

            // Icon matching distorted image (Shield/Device icon)
            Box(
                modifier = Modifier.size(56.dp).background(Color(0xFF2A2A2E), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = "Verified device", tint = Color.White, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = device.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = "ID: ${device.id}", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    // Signal Tag component matching image_13.png
                    Surface(color = Color(0xFF2A2A2E), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = device.getSignalStatus(),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Reconstructed Connect Button matching image_13.png logic and shape
            Button(
                onClick = {
                    if (status != "Connected") {
                        tts.speak("Connecting to ${device.name}", TextToSpeech.QUEUE_FLUSH, null, null)
                        viewModel.connectToDevice(device) // <-- This now talks to the ViewModel!
                    }
                },
                enabled = status != "Connecting", // Disable button when connecting
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD4BFFF), // The sleek light purple background from Image 13
                    contentColor = Color(0xFF2D2440), // Deep dark purple text
                    disabledContainerColor = Color(0xFF2A2A2E),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Text(
                    text = when(status) {
                        "Connecting" -> "..."
                        "Connected" -> "Paired"
                        else -> "Connect"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// Reconstructed generic device item with plus icon matching image_13.png
@Composable
fun DiscoveredDeviceItem(device: BluetoothDeviceModel, tts: TextToSpeech) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {

            // Icon (mocking headphones based on top item in standard list)
            Icon(
                imageVector = Icons.Default.Headset,
                contentDescription = "Standard device",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = device.id, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            }

            // Small grey '+' button matching distorted image logic
            IconButton(
                onClick = {
                    tts.speak("Attempting to pair ${device.name}", TextToSpeech.QUEUE_FLUSH, null, null)
                    // Trigger actual pairing logic here
                },
                modifier = Modifier.size(36.dp).background(Color(0xFF2A2A2E), androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Pair", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
}


// --- Permissions Handling Logic (Paste in utils or below Screen component) ---

// Handle standard permissions request chain
private fun handleBluetoothActivate(context: Context, permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>, tts: TextToSpeech) {
    // Bluetooth on Android 12+ requires multiple specific runtime permissions
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Request specific S+ permissions
        permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
    } else {
        // Handle classic permissions (often implicitly granted if defined in Manifest)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Bluetooth permissions ready.", Toast.LENGTH_SHORT).show()
        } else {
            // Might still need location on older devices for LE scan
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }
}

// Handle actual intent call for Bluetooth activation (Android standard popup)
private fun requestBtEnablement(context: Context, adapter: BluetoothAdapter?, launcher: ManagedActivityResultLauncher<Intent, androidx.activity.result.ActivityResult>, tts: TextToSpeech) {
    if (adapter == null) {
        Toast.makeText(context, "Bluetooth radio not found on device.", Toast.LENGTH_SHORT).show()
        return
    }

    if (!adapter.isEnabled) {
        tts.speak("System requesting Bluetooth permission.", TextToSpeech.QUEUE_FLUSH, null, null)
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        // Check standard compliance before launch
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            launcher.launch(enableBtIntent)
        }
    } else {
        tts.speak("Bluetooth already on.", TextToSpeech.QUEUE_FLUSH, null, null)
    }
}