

package com.example.companion

import android.content.Context

object GuardianStorage {
    // This perfectly matches the file and key you created in EmergencyProtocolScreen!
    private const val PREFS_NAME = "companion_prefs"
    private const val KEY_PHONE = "guardian_phone"

    // Call this during an SOS to instantly retrieve the number
    fun getNumber(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PHONE, null)
    }
}