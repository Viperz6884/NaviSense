package com.example.companion

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class VolumeKeyService : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action

        // Trigger when Volume Down is pressed
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && action == KeyEvent.ACTION_DOWN) {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(intent)
            return true // Consume the event
        }
        return super.onKeyEvent(event)
    }

    // This was the missing member!
    // We leave it empty because we only care about buttons, not screen changes.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No implementation needed for your use case
    }

    override fun onInterrupt() {
        // Required override, can be left empty
    }

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            // eventTypes = 0 means we aren't listening for UI changes (like clicks or text)
            eventTypes = 0
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            // This flag is what allows onKeyEvent to work
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        this.serviceInfo = info
    }
}