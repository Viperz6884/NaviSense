package com.example.companion

import android.content.Context
import java.util.UUID

object UniqueIdManager {

    private const val PREF_NAME = "companion_prefs"
    private const val KEY_BLIND_ID = "blind_user_id"

    // Returns the stored ID, or generates + saves a new one on first launch
    fun getOrCreateBlindId(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_BLIND_ID, null)
        if (existing != null) return existing

        // Generate a clean 6-character alphanumeric ID (easy to read aloud)
        val newId = UUID.randomUUID().toString()
            .replace("-", "")
            .take(6)
            .uppercase()
        prefs.edit().putString(KEY_BLIND_ID, newId).apply()
        return newId
    }
    fun getOrCreateBlindIdFromUid(uid: String): String {
        return uid.take(6).uppercase()
    }
    // Guardian saves which blind ID they are connected to
    fun saveConnectedBlindId(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("connected_blind_id", id).apply()
    }

    fun getConnectedBlindId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString("connected_blind_id", null)
    }

    // Blind person shares their location under their unique ID key
    fun saveBlindLocation(context: Context, lat: Double, lng: Double) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("blind_lat", lat.toFloat())
            .putFloat("blind_lng", lng.toFloat())
            .apply()
    }

    // Guardian reads the stored location (same device demo; replace with Firebase for multi-device)
    fun getBlindLocation(context: Context): Pair<Double, Double>? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lat = prefs.getFloat("blind_lat", Float.MIN_VALUE)
        val lng = prefs.getFloat("blind_lng", Float.MIN_VALUE)
        if (lat == Float.MIN_VALUE) return null
        return Pair(lat.toDouble(), lng.toDouble())
    }
}