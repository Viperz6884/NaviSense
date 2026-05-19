package com.example.companion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class BlindViewModel : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    // ── NEW: REAL HARDWARE LISTS ──
    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceModel>> = _pairedDevices.asStateFlow()

    private val _foundDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())
    val foundDevices: StateFlow<List<BluetoothDeviceModel>> = _foundDevices.asStateFlow()

    private val _guardianStick = MutableStateFlow<BluetoothDeviceModel?>(null)
    val guardianStick: StateFlow<BluetoothDeviceModel?> = _guardianStick.asStateFlow()

    // ── HARDWARE DATA RECEIVERS ──

    fun setScanningState(scanning: Boolean) {
        _isScanning.value = scanning
    }

    fun updatePairedDevices(devices: List<BluetoothDeviceModel>) {
        _pairedDevices.value = devices
    }

    fun addOrUpdateDiscoveredDevice(device: BluetoothDeviceModel) {
        // If it's the Guardian Stick, update it directly
        if (device.name.contains("Guardian", ignoreCase = true) || device.id == "GP-882-XQ") {
            _guardianStick.value = device
            return
        }

        // For all other devices: Update live RSSI if we already found it, or add it to the list
        val currentList = _foundDevices.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == device.id }

        if (existingIndex != -1) {
            currentList[existingIndex] = device // Updates the live signal strength!
        } else {
            currentList.add(device)
        }

        // Sort by signal strength (closest devices at the top)
        _foundDevices.value = currentList.sortedByDescending { it.rssi }
    }

    fun clearDiscoveredDevices() {
        _foundDevices.value = emptyList()
    }

    // ── CONNECTION LOGIC ──
    fun connectToDevice(device: BluetoothDeviceModel) {
        viewModelScope.launch {
            _connectionStatus.value = "Connecting"
            delay(2000) // We will replace this with actual connection sockets later
            _connectionStatus.value = "Connected"
        }
    }
}