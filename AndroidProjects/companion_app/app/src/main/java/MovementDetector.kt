package com.example.companion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

class MovementDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    // Flow for walking status
    private val _isWalking = MutableStateFlow(false)
    val isWalkingFlow: StateFlow<Boolean> = _isWalking

    // NEW: Flow for Step Counting
    private val _stepCount = MutableStateFlow(0)
    val stepCountFlow: StateFlow<Int> = _stepCount

    // NEW: Shared Preferences to save steps when the app closes
    private val prefs = context.getSharedPreferences("companion_prefs", Context.MODE_PRIVATE)

    private val threshold = 2.0f
    private val stopDelay = 3000L

    // NEW: Debounce timer to prevent double-counting a single footstep
    private val stepDebounceDelay = 300L

    private var lastMovementTime = 0L
    private var lastStepTime = 0L // NEW: Tracks exactly when the last step happened

    init {
        // NEW: When the app opens, load the saved steps from memory!
        _stepCount.value = prefs.getInt("daily_steps", 0)
    }

    fun start() {
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    // NEW: A helpful function if you ever want to build a "Reset Steps" button
    fun resetSteps() {
        _stepCount.value = 0
        prefs.edit().putInt("daily_steps", 0).apply()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val currentTime = System.currentTimeMillis()

            if (magnitude > threshold) {
                lastMovementTime = currentTime

                if (!_isWalking.value) {
                    _isWalking.value = true
                }

                // NEW: Step Counting Logic!
                // If the movement is strong enough, AND 300ms have passed since the last step...
                if (currentTime - lastStepTime > stepDebounceDelay) {
                    _stepCount.value += 1 // Add a step
                    lastStepTime = currentTime // Reset the debounce timer

                    // Save the new step count to the phone's memory instantly
                    prefs.edit().putInt("daily_steps", _stepCount.value).apply()
                }

            } else {
                if (_isWalking.value && currentTime - lastMovementTime > stopDelay) {
                    _isWalking.value = false
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}