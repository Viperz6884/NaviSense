package com.example.companion

// Representing a single discovered Bluetooth hardware device
data class BluetoothDeviceModel(
    val id: String, // The MAC Address
    val name: String, // The broadcasted display name
    val rssi: Int = -50, // Signal strength (decibels, e.g., -30 is high, -90 is low)
    val isPaired: Boolean = false // NEW: Tracks if the device is already saved to the phone
) {
    // Utility function to convert RSSI to user-friendly status, matching Image 13
    fun getSignalStatus(): String = when {
        rssi >= -60 -> "SIGNAL: HIGH"
        rssi >= -80 -> "SIGNAL: MEDIUM"
        else -> "SIGNAL: LOW"
    }
}