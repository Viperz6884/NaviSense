package com.example.companion

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationTracker(context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    // Emits location updates as a Flow (stream of data)
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 5000L): Flow<Location> = callbackFlow {

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMs
        ).setMinUpdateDistanceMeters(5f).build() // Only update if moved 5+ meters

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(location) // Send location to whoever is collecting
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, null)

        // Clean up when the collector stops (e.g. walk ends)
        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }
}