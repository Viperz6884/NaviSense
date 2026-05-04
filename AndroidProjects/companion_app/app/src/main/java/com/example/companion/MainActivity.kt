package com.example.companion

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.companion.ui.theme.CompanionTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private var internalNavController: NavController? = null

    private var isVolumeUpPressed = false
    private var isVolumeDownPressed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Companion)

        // Add this temporarily to check your key
        try {
            val ai = packageManager.getApplicationInfo(packageName, android.content.pm.PackageManager.GET_META_DATA)
            val apiKey = ai.metaData.getString("com.google.android.geo.API_KEY")
            android.util.Log.e("MAPS_DEBUG", "My injected API Key is: $apiKey")
        } catch (e: Exception) {
            android.util.Log.e("MAPS_DEBUG", "Failed to read API key")
        }

        setContent {
            CompanionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // We call our new intelligent router here
                    CompanionAppRouter(
                        onNavControllerCreated = { internalNavController = it }
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) isVolumeUpPressed = true
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) isVolumeDownPressed = true

        if (isVolumeUpPressed && isVolumeDownPressed) {
            android.util.Log.d("CompanionApp", "SOS Combo Detected!")
            vibratePhone()
            runOnUiThread {
                if (internalNavController?.currentDestination?.route != "blindHome") {
                    internalNavController?.navigate("blindHome")
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) isVolumeUpPressed = false
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) isVolumeDownPressed = false
        return super.onKeyUp(keyCode, event)
    }

    private fun vibratePhone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }
    }
}

@Composable
fun CompanionAppRouter(onNavControllerCreated: (NavController) -> Unit) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Pass the NavController back to the Activity for the SOS Volume Keys
    LaunchedEffect(navController) {
        onNavControllerCreated(navController)
    }

    // This state holds our starting screen. It starts as 'null' while we check Firebase.
    var startDestination by remember { mutableStateOf<String?>(null) }

    // Check Firebase Auth and Firestore exactly ONCE when the app opens
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser

        if (currentUser != null && currentUser.isEmailVerified) {
            db.collection("Users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val role = document.getString("role")
                        if (role == "Blind") {
                            startDestination = "blindHome"
                        } else {
                            startDestination = "guardianConnect"
                        }
                    } else {
                        // Rare glitch: Auth token exists but no database profile. Force sign out.
                        auth.signOut()
                        startDestination = "landing"
                    }
                }
                .addOnFailureListener {
                    // If the database fails to load, go to landing
                    startDestination = "landing"
                }
        } else {
            // No one is logged in, OR email isn't verified.
            auth.signOut()
            startDestination = "landing"
        }
    }

    // --- THE UI RENDERING ---

    if (startDestination == null) {
        // While Firebase is checking the token, show a loading spinner
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // Once Firebase gives us the answer, launch permissions and the Navigation Host!
        RequestPermissionsOnLaunch(onAllGranted = {})

        NavHost(
            navController = navController,
            startDestination = startDestination!!
        ) {
            // Note: I replaced your old 'LandingPage' with the new 'LandingScreen'
            // that wires directly into your new Login/Signup auth system!
            composable("landing") {
                LandingScreen(navController = navController)
            }

            composable("login") {
                LoginScreen(navController = navController)
            }

            composable("signup") {
                SignUpScreen(navController = navController)
            }

            composable("guardianConnect") {
                GuardianConnectScreen(
                    onConnected = {
                        navController.navigate("guardianHome") {
                            popUpTo("guardianConnect") { inclusive = true }
                        }
                    }
                )
            }

            composable("guardianHome") {
                GuardianHomeScreen()
            }

            composable("navigation/{dest}") {
                val dest = it.arguments?.getString("dest") ?: ""
                BlindNavigationScreen(dest)
            }

            composable("blindHome") {
                BlindHomeScreen(navController = navController)
            }
        }
    }
}