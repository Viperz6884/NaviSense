package com.example.companion

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import java.util.Locale

@Composable
fun EmergencyProtocolScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("companion_prefs", Context.MODE_PRIVATE)

    // Added scroll state so the page scrolls smoothly with the new cards
    val scrollState = rememberScrollState()

    val tts = remember { TextToSpeech(context) {}.apply { language = Locale("en", "IN") } }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    // State for our single SOS contact
    var primarySosName by remember { mutableStateOf(prefs.getString("guardian_name", "")) }
    var primarySosPhone by remember { mutableStateOf(prefs.getString("guardian_phone", "")) }
    var phoneToCall by remember { mutableStateOf("") }

    // Launcher for Direct Calling Permission
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && phoneToCall.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneToCall"))
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Permission denied for direct calling.", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher to pick a contact from the phonebook
    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val realName = cursor.getString(nameIndex)

                var realPhone = ""
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val contactId = cursor.getString(idIndex)
                val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                if (hasPhoneIndex >= 0 && cursor.getString(hasPhoneIndex).toInt() > 0) {
                    val phoneCursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", arrayOf(contactId), null
                    )
                    if (phoneCursor != null && phoneCursor.moveToFirst()) {
                        val phoneIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        realPhone = phoneCursor.getString(phoneIndex).replace(" ", "")
                        phoneCursor.close()
                    }
                }
                cursor.close()

                if (realPhone.isNotEmpty()) {
                    prefs.edit().putString("guardian_name", realName).putString("guardian_phone", realPhone).apply()
                    primarySosName = realName
                    primarySosPhone = realPhone

                    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (currentUserId != null) {
                        FirebaseManager.setGuardianPhone(currentUserId, realPhone)
                    }

                    tts.speak("$realName set as primary emergency contact.", TextToSpeech.QUEUE_FLUSH, null, null)
                } else {
                    Toast.makeText(context, "No phone number found for this contact.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState) // <-- Added this to allow scrolling!
        ) {
            // Header
            Text("PRIORITY 01", color = Color(0xFFE57373), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Emergency Protocol", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 44.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Set the single primary contact who will be dialed instantly when you trigger the SOS button on your dashboard.", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp, lineHeight = 24.sp)

            Spacer(modifier = Modifier.height(48.dp))

            // Logic: Show ADD button if empty, otherwise show the Contact Card
            if (primarySosPhone.isNullOrEmpty()) {
                Button(
                    onClick = { pickContactLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth().height(70.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE57373).copy(alpha = 0.5f))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Add Primary Guardian", fontSize = 18.sp, color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // The Primary Contact Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1414))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = primarySosName ?: "Unknown", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = primarySosPhone ?: "", color = Color(0xFFE57373), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }

                        Row {
                            // Direct Call Button
                            IconButton(
                                onClick = {
                                    // Custom TTS Announcement
                                    tts.speak("Calling Primary Guardian.", TextToSpeech.QUEUE_FLUSH, null, null)

                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$primarySosPhone"))
                                        context.startActivity(intent)
                                    } else {
                                        phoneToCall = primarySosPhone ?: ""
                                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                    }
                                },
                                modifier = Modifier.size(48.dp).background(Color(0xFF4CAF50).copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Delete Button
                            IconButton(
                                onClick = {
                                    prefs.edit().remove("guardian_name").remove("guardian_phone").apply()
                                    primarySosName = ""
                                    primarySosPhone = ""
                                    tts.speak("Primary contact removed.", TextToSpeech.QUEUE_FLUSH, null, null)
                                },
                                modifier = Modifier.size(48.dp).background(Color(0xFFE57373).copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE57373), modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            // ── NEW: PUBLIC SERVICES SECTION ─────────────────────────────────────
            Spacer(modifier = Modifier.height(48.dp))
            Text("PUBLIC SERVICES", color = Color(0xFFE57373), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // List of immutable public services (Name, Number, UI Color)
            val publicServices = listOf(
                Triple("Police", "100", Color(0xFF64B5F6)), // Blue
                Triple("Ambulance", "102", Color(0xFF81C784)), // Green
                Triple("Fire Department", "101", Color(0xFFFF8A65)) // Orange
            )

            publicServices.forEach { service ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = service.first, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = service.second, color = service.third, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }

                        IconButton(
                            onClick = {
                                // 1. TTS Announcement ("Calling Police", "Calling Ambulance", etc.)
                                tts.speak("${service.first}.", TextToSpeech.QUEUE_FLUSH, null, null)

                                // 2. Instant Call Logic
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${service.second}"))
                                    context.startActivity(intent)
                                } else {
                                    phoneToCall = service.second
                                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                }
                            },
                            modifier = Modifier.size(48.dp).background(service.third.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call ${service.first}", tint = service.third, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp)) // Safe padding for the bottom of the scroll
        }
    }
}