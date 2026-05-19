package com.example.companion

import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.filled.Refresh
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val tts = remember { TextToSpeech(context) {}.apply { language = Locale("en", "IN") } }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    // --- Live User Data State ---
    var userName by remember { mutableStateOf("Loading...") }
    var userEmail by remember { mutableStateOf("Loading...") }
    val userPhone by remember { mutableStateOf("Not Added Yet") }

    // --- Dialog Control States ---
    var showNameDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    // NEW: Re-Authentication State
    var showReAuthDialog by remember { mutableStateOf(false) }
    var pendingReAuthAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // --- Image Picker State ---
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // --- Replace your LaunchedEffect(Unit) block with this ---
    // Fetch data from Firebase safely when the screen initially opens
    LaunchedEffect(Unit) {
        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser != null) {
            userEmail = authUser.email ?: "No email linked"

            FirebaseFirestore.getInstance().collection("Users").document(authUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        userName = document.getString("name") ?: "Companion User"
                    } else {
                        userName = "Companion User"
                    }
                }
                .addOnFailureListener {
                    userName = "Failed to load name"
                }
        } else {
            userName = "Not Logged In"
            userEmail = "Not Logged In"
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
                Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Toast.makeText(context, "Voice command active", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text(text = "My Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        bottomBar = {
            CompanionBottomBar(
                onHomeClick = { navController.navigate("blindHome") },
                onStickClick = { navController.navigate("StickScreen") },
                onSpeakClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    }
                    speechLauncher.launch(intent)
                },
                onContactsClick = { navController.navigate("contacts") },
                onProfileClick = { /* Already here */ },
                selectedTab = 3
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ─── TOP PROFILE CARD ───
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF2A2A2E))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val firstLetter = userName.firstOrNull()?.uppercase() ?: "?"
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color(0xFF8C52FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = firstLetter,
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        FloatingActionButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 10.dp, y = 10.dp)
                                .size(36.dp),
                            shape = RoundedCornerShape(12.dp),
                            containerColor = Color(0xFFD4BFFF),
                            contentColor = Color(0xFF2D2440)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Picture",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ─── ACCOUNT INFORMATION SECTION ───
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ACCOUNT INFORMATION",
                    color = Color(0xFFA87FFB),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                )

                AccountInfoRow(
                    label = "FULL NAME",
                    value = userName,
                    onClick = { showNameDialog = true }
                )

                AccountInfoRow(
                    label = "EMAIL ADDRESS",
                    value = userEmail,
                    onClick = { showEmailDialog = true },
                    showRefresh = true,
                    onRefreshClick = {
                        val user = FirebaseAuth.getInstance().currentUser
                        if (user != null) {
                            Toast.makeText(context, "Refreshing status...", Toast.LENGTH_SHORT)
                                .show()

                            user.reload().addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val updatedEmail = user.email ?: "No email linked"
                                    userEmail = updatedEmail
                                    Toast.makeText(
                                        context,
                                        "Email verified and synced!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    FirebaseFirestore.getInstance().collection("Users")
                                        .document(user.uid)
                                        .update("email", updatedEmail)
                                } else {
                                    // ─── THE FIX: Capture the REAL error from Firebase ───
                                    val realError =
                                        task.exception?.localizedMessage ?: "Unknown security error"

                                    // Show exactly what Firebase is complaining about
                                    Toast.makeText(
                                        context,
                                        "Firebase Error: $realError",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    // If Firebase killed the session due to the email change, prompt a re-login
                                    Toast.makeText(
                                        context,
                                        "If your token expired, please log out and log in with your NEW email.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                )

                AccountInfoRow(
                    label = "PHONE NUMBER",
                    value = userPhone,
                    onClick = {
                        Toast.makeText(
                            context,
                            "Edit Phone coming soon",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── SECURITY SECTION ───
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SECURITY",
                    color = Color(0xFFA87FFB),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                )

                SecurityInfoRow(
                    icon = Icons.Outlined.Lock,
                    title = "Change Password",
                    onClick = { showPasswordDialog = true }
                )

                SecurityInfoRow(
                    icon = Icons.Outlined.Security,
                    title = "Two-Factor Authentication",
                    badgeText = "ENABLED",
                    onClick = {
                        Toast.makeText(
                            context,
                            "2FA Settings coming soon",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // ─── PREFERENCES SECTION ───
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "PREFERENCES",
                    color = Color(0xFFA87FFB),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                )

                PreferenceInfoRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Notification Settings",
                    onClick = {
                        Toast.makeText(
                            context,
                            "Notifications coming soon",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                PreferenceInfoRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "Theme",
                    valueText = "Dark",
                    onClick = {
                        Toast.makeText(
                            context,
                            "Theme settings coming soon",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                PreferenceInfoRow(
                    icon = Icons.Outlined.Language,
                    title = "Language",
                    valueText = "English (US)",
                    onClick = {
                        Toast.makeText(
                            context,
                            "Language settings coming soon",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── SUBSCRIPTION SECTION ───
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SUBSCRIPTION",
                    color = Color(0xFFA87FFB),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                )

                SubscriptionCard(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Manage Subscription coming soon",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // ─── SUPPORT SECTION ───
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SUPPORT",
                    color = Color(0xFFA87FFB),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                )

                PreferenceInfoRow(
                    icon = Icons.Outlined.HelpOutline,
                    title = "Help Center",
                    rightIcon = Icons.Outlined.OpenInNew, // Uses the 'external link' icon
                    onClick = {
                        Toast.makeText(
                            context,
                            "Opening Help Center...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                PreferenceInfoRow(
                    icon = Icons.Outlined.PrivacyTip, // Shield icon
                    title = "Privacy Policy",
                    onClick = {
                        Toast.makeText(
                            context,
                            "Opening Privacy Policy...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            // ─── LOG OUT SECTION ───
            Button(
                onClick = {
                    // 1. Tell Firebase to securely end the active session
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

            Spacer(modifier = Modifier.height(100.dp))
        }


        // ─── DIALOG LOGIC ───

        val currentUser = FirebaseAuth.getInstance().currentUser

        // 1. Edit Name Dialog
        if (showNameDialog) {
            EditInfoDialog(
                title = "Edit Full Name",
                currentValue = userName,
                onDismiss = { showNameDialog = false },
                onSave = { newName ->
                    userName = newName
                    showNameDialog = false
                    val uid = currentUser?.uid
                    if (uid != null) {
                        FirebaseFirestore.getInstance().collection("Users").document(uid).update("name", newName)
                            .addOnSuccessListener { Toast.makeText(context, "Name updated!", Toast.LENGTH_SHORT).show() }
                    }
                }
            )
        }

        // 2. Edit Email Dialog (Using verifyBeforeUpdateEmail)
        if (showEmailDialog) {
            EditInfoDialog(
                title = "Update Email Address",
                currentValue = userEmail,
                onDismiss = { showEmailDialog = false },
                onSave = { newEmail ->
                    if (currentUser != null) {
                        // The secure way to verify new emails
                        currentUser.verifyBeforeUpdateEmail(newEmail).addOnSuccessListener {
                            showEmailDialog = false
                            Toast.makeText(context, "Verification link sent to $newEmail! Please check your inbox to confirm.", Toast.LENGTH_LONG).show()
                        }.addOnFailureListener { e ->
                            showEmailDialog = false
                            if (e is FirebaseAuthRecentLoginRequiredException) {
                                // If token is old, trigger Re-Auth then try again!
                                pendingReAuthAction = {
                                    currentUser.verifyBeforeUpdateEmail(newEmail).addOnSuccessListener {
                                        Toast.makeText(context, "Verification link sent to $newEmail! Please check your inbox.", Toast.LENGTH_LONG).show()
                                    }
                                }
                                showReAuthDialog = true
                            } else {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            )
        }

        // 3. Change Password Dialog
        if (showPasswordDialog) {
            EditPasswordDialog(
                onDismiss = { showPasswordDialog = false },
                onSave = { newPassword ->
                    if (currentUser != null && newPassword.length >= 6) {
                        currentUser.updatePassword(newPassword).addOnSuccessListener {
                            Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                            showPasswordDialog = false
                        }.addOnFailureListener { e ->
                            showPasswordDialog = false
                            if (e is FirebaseAuthRecentLoginRequiredException) {
                                // If token is old, trigger Re-Auth then try again!
                                pendingReAuthAction = {
                                    currentUser.updatePassword(newPassword).addOnSuccessListener {
                                        Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                showReAuthDialog = true
                            } else {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // 4. Re-Authenticate Dialog
        if (showReAuthDialog) {
            ReAuthenticateDialog(
                userEmail = currentUser?.email ?: "",
                onDismiss = { showReAuthDialog = false },
                onSuccess = {
                    showReAuthDialog = false
                    // Once verified, run the password/email update they originally tried to do!
                    pendingReAuthAction?.invoke()
                }
            )
        }
    }
}

// ─── UI COMPONENTS ───

@Composable
fun AccountInfoRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    showRefresh: Boolean = false, // NEW: Control if the refresh icon shows
    onRefreshClick: () -> Unit = {} // NEW: Action for the refresh icon
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // If this is the email row, show the refresh button!
                if (showRefresh) {
                    IconButton(
                        onClick = onRefreshClick,
                        modifier = Modifier.size(32.dp).background(Color(0xFF2D2440), CircleShape)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                            contentDescription = "Refresh Email",
                            tint = Color(0xFFA87FFB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Edit", tint = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun SecurityInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, badgeText: String? = null, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Icon + Stacked Text & Badge
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))

                // Using a Column to stack the title and the badge
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // If a badge exists, draw it right underneath the text
                    if (badgeText != null) {
                        Spacer(modifier = Modifier.height(6.dp)) // Small gap between text and badge
                        Surface(color = Color(0xFF2D2440), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = badgeText,
                                color = Color(0xFFA87FFB),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Side: Arrow Icon
            Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Edit", tint = Color.White.copy(alpha = 0.5f))
        }
    }
}
@Composable
fun EditInfoDialog(title: String, currentValue: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var textValue by remember { mutableStateOf(currentValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1E),
        title = { Text(text = title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFA87FFB), unfocusedBorderColor = Color.Gray, cursorColor = Color(0xFFA87FFB)
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onSave(textValue) }) { Text("SAVE", color = Color(0xFFA87FFB), fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.White.copy(alpha = 0.5f)) } }
    )
}

@Composable
fun EditPasswordDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var newPassword by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1E),
        title = { Text("Change Password", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password", color = Color.Gray) },
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFA87FFB), unfocusedBorderColor = Color.Gray, cursorColor = Color(0xFFA87FFB)
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onSave(newPassword) }) { Text("UPDATE", color = Color(0xFFA87FFB), fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.White.copy(alpha = 0.5f)) } }
    )
}

@Composable
fun ReAuthenticateDialog(userEmail: String, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    var currentPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1E),
        title = { Text("Verify Identity", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("For security, please enter your current password to continue.", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password", color = Color.Gray) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFA87FFB), unfocusedBorderColor = Color.Gray, cursorColor = Color(0xFFA87FFB)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (currentPassword.isNotEmpty()) {
                    val credential = EmailAuthProvider.getCredential(userEmail, currentPassword)
                    FirebaseAuth.getInstance().currentUser?.reauthenticate(credential)
                        ?.addOnSuccessListener { onSuccess() }
                        ?.addOnFailureListener { Toast.makeText(context, "Incorrect Password", Toast.LENGTH_SHORT).show() }
                }
            }) {
                Text("VERIFY", color = Color(0xFFA87FFB), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.White.copy(alpha = 0.5f)) } }
    )
}
@Composable
fun PreferenceInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    valueText: String? = null,
    rightIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.KeyboardArrowRight, // Default is the arrow
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (valueText != null) {
                    Text(
                        text = valueText,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                // Uses the custom icon, or defaults to the arrow!
                Icon(imageVector = rightIcon, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}
@Composable
fun SubscriptionCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Purple Icon Box
                Box(
                    modifier = Modifier.size(48.dp).background(Color(0xFF2D2440), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.WorkspacePremium,
                        contentDescription = "Premium",
                        tint = Color(0xFFA87FFB),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))

                // Title and Date
                Column {
                    Text(text = "Premium Elite", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Active until Oct 12, 2025", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }

            // Manage Action Text
            Text(text = "Manage", color = Color(0xFFA87FFB), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}