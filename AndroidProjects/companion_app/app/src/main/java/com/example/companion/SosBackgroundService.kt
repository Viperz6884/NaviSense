package com.example.companion

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SosBackgroundService : Service() {

    // Memory to track the edge trigger
    private var previousSosState = false

    // We don't need binding for this simple service
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Connect to your specific Asian Firebase Server
        val databaseRef = FirebaseDatabase.getInstance("https://companionapp-prod-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("NaviSense/Critical/SOS_ACTIVE")

        // Attach the listener that runs in the background
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentSosState = snapshot.getValue(Boolean::class.java) ?: false

                // If hardware just flipped from false to true, FIRE THE CALL!
                if (currentSosState && !previousSosState) {

                    // ── PULL YOUR DYNAMIC NUMBER HERE ──
                    // This fetches the number saved by the user in the Emergency Protocol Screen
                    val actualGuardianNumber = GuardianStorage.getNumber(this@SosBackgroundService)

                    if (!actualGuardianNumber.isNullOrEmpty()) {
                        makeEmergencyCall(actualGuardianNumber)
                    } else {
                        println("SOS Triggered, but no Guardian number was saved in Emergency Protocol!")
                    }
                }

                previousSosState = currentSosState
            }

            override fun onCancelled(error: DatabaseError) {
                // Silently handle errors in the background
            }
        })

        // START_STICKY tells Android to restart this service if it gets killed to free up memory
        return START_STICKY
    }

    private fun makeEmergencyCall(phoneNumber: String) {
        // Check if we have permission before dialing
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                // FLAG_ACTIVITY_NEW_TASK is legally required to launch a call from a background service
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(callIntent)
        }
    }
}