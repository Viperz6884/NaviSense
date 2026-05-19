package com.example.companion

import kotlinx.coroutines.launch
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
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

data class TrustedContact(
    val id: String,
    val name: String,
    val relation: String,
    var phoneNumber: String = "",
    var liveAudio: Boolean = false,
    var locationHistory: Boolean = false,
    var sosNotification: Boolean = false
)

@Composable
fun BlindContactsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val tts = remember {
        TextToSpeech(context) {}.apply { language = Locale("en", "IN") }
    }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    var permissionsGranted by remember { mutableStateOf(false) }

    val addedContacts = remember {
        mutableStateListOf<TrustedContact>()
    }
    // Grabs the first contact's phone number as the default guardian, or null if empty
    val guardianPhone = addedContacts.firstOrNull()?.phoneNumber
    // ── FETCH SAVED CONTACTS ON LOAD ────────
    LaunchedEffect(Unit) {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            FirebaseManager.getTrustedContacts(
                blindId = currentUserId,
                onResult = { fetchedContacts ->
                    addedContacts.clear()
                    addedContacts.addAll(fetchedContacts)
                },
                onFailure = {
                    Toast.makeText(context, "Failed to load saved contacts", Toast.LENGTH_SHORT).show()
                }
            )
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            permissionsGranted = true
        }
    }

    var expandedContactId by remember { mutableStateOf<String?>(null) }
    var relationPickerVisible by remember { mutableStateOf(false) }
    var selectedContactNameForPicker by remember { mutableStateOf<String?>(null) }
    var selectedContactPhoneForPicker by remember { mutableStateOf("") }

    // Temporary holder for the phone number if we need to ask for permission
    var phoneToCall by remember { mutableStateOf("") }

    // Launcher to ask for Direct Call permission
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && phoneToCall.isNotEmpty()) {
            // Permission granted! Make the call immediately.
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneToCall"))
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Call permission denied. Cannot make direct calls.", Toast.LENGTH_SHORT).show()
        }
    }



    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionsGranted = isGranted
        if (isGranted) tts.speak("Contacts permission granted.", TextToSpeech.QUEUE_FLUSH, null, null)
    }

    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val realContactName = cursor.getString(nameIndex)

                var realPhoneNumber = ""
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val contactId = cursor.getString(idIndex)
                val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                if (hasPhoneIndex >= 0 && cursor.getString(hasPhoneIndex).toInt() > 0) {
                    val phoneCursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(contactId),
                        null
                    )
                    if (phoneCursor != null && phoneCursor.moveToFirst()) {
                        val phoneIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        realPhoneNumber = phoneCursor.getString(phoneIndex)
                        phoneCursor.close()
                    }
                }
                cursor.close()

                selectedContactNameForPicker = realContactName
                selectedContactPhoneForPicker = realPhoneNumber
                relationPickerVisible = true
                tts.speak("Choose the relationship for $realContactName.", TextToSpeech.QUEUE_FLUSH, null, null)
            }
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
                        // 1. Home Button
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

                        // 2. Stick Button - NOW WIRED TO NAVIGATE TO STICK PAGE!
                        Column(
                            modifier = Modifier.size(56.dp).clickable {
                                navController.navigate("stickScreen") {
                                    launchSingleTop = true
                                }
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Icon is faded because it's not the active page
                            Icon(Icons.Default.Accessibility, "Stick", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            Spacer(Modifier.height(2.dp))
                            Text("Stick", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }

                        // 3. Invisible Spacer for the Mic button
                        Spacer(modifier = Modifier.width(56.dp))

                        // 4. Contact Button - SHOWS TOAST & IS HIGHLIGHTED
                        Column(
                            modifier = Modifier.size(56.dp).clickable {
                                Toast.makeText(context, "You are already on the Contact page!", Toast.LENGTH_SHORT).show()
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Highlighting this icon slightly to show we are currently on this page
                            Icon(Icons.Default.Call, "Contact", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(2.dp))
                            Text("Contact", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(innerPadding) // This respects the footer space
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // Header section starts here without the back arrow
            Column(modifier = Modifier.padding(top = 24.dp, bottom = 24.dp)) {
                Text("SECURITY LAYER 02", color = Color(0xFFA87FFB), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Trusted Contacts", color = Color(0xFFF5F5F5), fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 52.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Manage the Inner Circle of your security network. Define granular permissions for those who protect you.", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp, lineHeight = 24.sp)
            }

            if (permissionsGranted && addedContacts.size < 5) {
                Button(
                    onClick = { pickContactLauncher.launch(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4BFFF).copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF44099C), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Add Contact", fontSize = 18.sp, color = Color(0xFF44099C), fontWeight = FontWeight.Bold)
                    }
                }
            } else if (!permissionsGranted) {
                Button(
                    onClick = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA87FFB)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Allow Access to My Contacts", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (permissionsGranted) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 1000.dp)
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(addedContacts) { contact ->
                        val isExpanded = contact.id == expandedContactId

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = if (isExpanded) Color(0xFF1A1A1E) else Color(0xFF121212), shape = RoundedCornerShape(16.dp))
                                .animateContentSize()
                        ) {
                            AddedContactCard(
                                contact = contact,
                                onPermissionsClick = {
                                    if (expandedContactId == contact.id) {
                                        expandedContactId = null
                                        tts.speak("Permissions closed.", TextToSpeech.QUEUE_FLUSH, null, null)
                                    } else {
                                        expandedContactId = contact.id
                                        tts.speak("Editing permissions.", TextToSpeech.QUEUE_FLUSH, null, null)
                                    }
                                },
                                onDeleteClick = {
                                    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                    if (currentUserId != null) {
                                        FirebaseManager.deleteTrustedContact(
                                            blindId = currentUserId,
                                            contactId = contact.id,
                                            onSuccess = {
                                                addedContacts.remove(contact)
                                                tts.speak("Contact removed.", TextToSpeech.QUEUE_FLUSH, null, null)
                                                Toast.makeText(context, "Contact deleted", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = { Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show() }
                                        )
                                    }
                                },
                                onCallClick = {
                                    // contact.phoneNumber is already decrypted by our fetcher!
                                    if (contact.phoneNumber.isNotEmpty()) {
                                        val phoneNumber = contact.phoneNumber.replace(" ", "") // Clean up any spaces

                                        // Check for the "Dangerous" Direct Call Permission
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                            // Permission is already there? CALL IMMEDIATELY
                                            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
                                            context.startActivity(intent)
                                        } else {
                                            // First time calling? Save number and ask for permission
                                            phoneToCall = phoneNumber
                                            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                        }
                                    } else {
                                        Toast.makeText(context, "No phone number found for this contact.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            if (isExpanded) {
                                PermissionDetailView(
                                    contact = contact,
                                    onSaveClick = {
                                        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"
                                        FirebaseManager.saveEncryptedContact(
                                            blindId = currentUserId,
                                            contact = contact,
                                            onSuccess = { },
                                            onFailure = { Toast.makeText(context, "Cloud update failed", Toast.LENGTH_SHORT).show() }
                                        )
                                        expandedContactId = null
                                        tts.speak("Permissions saved.", TextToSpeech.QUEUE_FLUSH, null, null)
                                    },
                                    onDeleteClick = {
                                        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                        if (currentUserId != null) {
                                            FirebaseManager.deleteTrustedContact(
                                                blindId = currentUserId,
                                                contactId = contact.id,
                                                onSuccess = {
                                                    addedContacts.remove(contact)
                                                    expandedContactId = null
                                                    tts.speak("Contact removed.", TextToSpeech.QUEUE_FLUSH, null, null)
                                                },
                                                onFailure = { }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }

        // Moved Dialog out of the main scrolling column but kept it inside Scaffold padding
        if (relationPickerVisible && selectedContactNameForPicker != null) {
            RelationPickerModal(
                name = selectedContactNameForPicker!!,
                onClose = { relationPickerVisible = false },
                onRelationSelect = { relation ->
                    if (addedContacts.size < 5) {
                        val newContact = TrustedContact(
                            id = System.currentTimeMillis().toString(),
                            name = selectedContactNameForPicker!!,
                            relation = relation,
                            phoneNumber = selectedContactPhoneForPicker
                        )
                        addedContacts.add(newContact)

                        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"
                        FirebaseManager.saveEncryptedContact(
                            blindId = currentUserId,
                            contact = newContact,
                            onSuccess = { },
                            onFailure = { Toast.makeText(context, "Cloud sync failed", Toast.LENGTH_SHORT).show() }
                        )

                        relationPickerVisible = false
                        selectedContactNameForPicker = null
                        tts.speak("Contact added.", TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }
            )
        }
    }
} // <--- THIS WAS THE MISSING CLOSING BRACKET

@Composable
fun AddedContactCard(contact: TrustedContact, onPermissionsClick: () -> Unit, onDeleteClick: () -> Unit, onCallClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1414)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = contact.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = contact.relation, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                }
                IconButton(onClick = onCallClick, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Button(
                    onClick = onPermissionsClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2440)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Permissions", fontSize = 12.sp, color = Color(0xFFA87FFB), fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
fun PermissionDetailView(contact: TrustedContact, onSaveClick: () -> Unit, onDeleteClick: () -> Unit) {
    var localAudio by remember { mutableStateOf(contact.liveAudio) }
    var localLocation by remember { mutableStateOf(contact.locationHistory) }
    var localSos by remember { mutableStateOf(contact.sosNotification) }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 24.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 32.dp)) {
            Box(modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E1414)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(45.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = contact.name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = contact.relation, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        }
        Text("ACTIVE PROTOCOL PERMISSIONS", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, letterSpacing = 2.sp, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PermissionToggleCard("Live Audio Access", "PROTOCOL: A-V3", localAudio) { localAudio = it }
            PermissionToggleCard("Location History", "30-DAY RETENTION", localLocation) { localLocation = it }
            PermissionToggleCard("SOS Notification", "PRIORITY 01", localSos) { localSos = it }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                contact.liveAudio = localAudio
                contact.locationHistory = localLocation
                contact.sosNotification = localSos
                onSaveClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA87FFB)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("SAVE", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onDeleteClick, modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(50.dp)) {
            Text("REMOVE CONTACT", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun PermissionToggleCard(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E)), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFFA87FFB)))
        }
    }
}

@Composable
fun RelationPickerModal(name: String, onClose: () -> Unit, onRelationSelect: (String) -> Unit) {
    val relations = listOf("Friend", "Family", "Known Person")
    Dialog(onDismissRequest = onClose) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(20.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Select relationship for $name.", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(20.dp))
                relations.forEach { relation ->
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRelationSelect(relation) }
                        .padding(vertical = 12.dp)) {
                        Text(relation, fontSize = 16.sp, color = Color.Black)
                    }
                    Divider()
                }
            }
        }
    }
}

@Composable
fun Dialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.6f))
        .clickable { onDismissRequest() }, contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
fun Divider() {
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(Color.Gray.copy(alpha = 0.2f)))
}