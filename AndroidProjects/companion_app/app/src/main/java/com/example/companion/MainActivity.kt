package com.example.companion

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.companion.ui.theme.CompanionTheme

class MainActivity : ComponentActivity() {

    private var internalNavController: NavController? = null

    private var isVolumeUpPressed = false
    private var isVolumeDownPressed = false


    override fun onCreate(savedInstanceState: Bundle?) {
            installSplashScreen()   // ← ADD THIS LINE
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
                val navController = rememberNavController()
                internalNavController = navController

                RequestPermissionsOnLaunch(onAllGranted = {})

                NavHost(
                    navController = navController,
                    startDestination = "landing"
                ) {
                    composable("landing") {
                        LandingPage(
                            onBlindClick = {
                                vibratePhone()
                                navController.navigate("blindHome")
                            },
                            onGuardianClick = {
                                navController.navigate("guardianConnect")
                            }
                        )
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
                        BlindHomeScreen(navController= navController)
                    }
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
