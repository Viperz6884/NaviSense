package com.example.companion

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration

object FirebaseManager {

    val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    // ── Save blind person's location (called during walk) ─────────────────────
    fun updateBlindLocation(blindId: String, lat: Double, lng: Double) {
        db.collection("users").document(blindId)
            .set(
                mapOf(
                    "blindId" to blindId,
                    "role" to "blind",
                    "location" to mapOf("lat" to lat, "lng" to lng),
                    "isWalking" to true
                ),
                SetOptions.merge()
            )
    }

    // ── Stop walk — mark as not walking ───────────────────────────────────────
    fun stopWalk(blindId: String) {
        db.collection("users").document(blindId)
            .set(mapOf("isWalking" to false), SetOptions.merge())
    }

    // ── Guardian connects — saves guardian phone into blind person's doc ──────
    fun connectGuardian(
        blindId: String,
        guardianPhone: String,
        connectedBlindId: String,  // same as blindId here
        onSuccess: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        // First check if blind person exists
        db.collection("users").document(blindId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && doc.getString("role") == "blind") {
                    // Save guardian's phone into blind person's document
                    db.collection("users").document(blindId)
                        .set(
                            mapOf("guardianPhone" to guardianPhone),
                            SetOptions.merge()
                        )
                    onSuccess()
                } else {
                    onFailed("No blind person found with ID: $blindId")
                }
            }
            .addOnFailureListener {
                onFailed("Connection failed. Check internet.")
            }
    }

    // ── Register blind person in Firestore on first walk ─────────────────────
    fun registerBlindPerson(blindId: String) {
        db.collection("users").document(blindId)
            .set(
                mapOf(
                    "blindId" to blindId,
                    "role" to "blind",
                    "isWalking" to false
                ),
                SetOptions.merge()
            )
    }

    // ── Get guardian's phone for SOS (blind person reads their own doc) ───────
    fun getGuardianPhone(blindId: String, onResult: (String?) -> Unit) {
        db.collection("users").document(blindId).get()
            .addOnSuccessListener { onResult(it.getString("guardianPhone")) }
            .addOnFailureListener { onResult(null) }
    }

    // ── Real-time location listener for guardian ──────────────────────────────
    fun listenToBlindLocation(
        blindId: String,
        onUpdate: (lat: Double, lng: Double) -> Unit
    ): ListenerRegistration {
        return db.collection("users").document(blindId)
            .addSnapshotListener { snapshot, _ ->
                val loc = snapshot?.get("location") as? Map<*, *> ?: return@addSnapshotListener
                val lat = (loc["lat"] as? Double) ?: return@addSnapshotListener
                val lng = (loc["lng"] as? Double) ?: return@addSnapshotListener
                onUpdate(lat, lng)
            }
    }

    // ── Trigger SOS alert ─────────────────────────────────────────────────────
    fun triggerSosAlert(blindId: String, lat: Double, lng: Double) {
        db.collection("sos_alerts")
            .add(
                mapOf(
                    "blindId" to blindId,
                    "lat" to lat,
                    "lng" to lng,
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "isResolved" to false
                )
            )
    }
}