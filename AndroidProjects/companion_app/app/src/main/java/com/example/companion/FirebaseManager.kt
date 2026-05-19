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
    // ── Save Encrypted Trusted Contact (SECURITY LAYER 02) ────────────────────
    // ── Save Encrypted Trusted Contact ────────────────────
    fun saveEncryptedContact(
        blindId: String,
        contact: TrustedContact,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val encryptedName = CryptoHelper.encrypt(contact.name)
        val encryptedRelation = CryptoHelper.encrypt(contact.relation)
        val encryptedPhone = CryptoHelper.encrypt(contact.phoneNumber) // NEW

        val contactData = hashMapOf(
            "id" to contact.id,
            "encName" to encryptedName,
            "encRelation" to encryptedRelation,
            "encPhone" to encryptedPhone, // NEW
            "liveAudio" to contact.liveAudio,
            "locationHistory" to contact.locationHistory,
            "sosNotification" to contact.sosNotification
        )

        db.collection("users").document(blindId)
            .collection("trusted_contacts").document(contact.id)
            .set(contactData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }
    // ── Fetch Encrypted Trusted Contacts ──────────────────────────────────────
    // ── Fetch Encrypted Trusted Contacts ──────────────────────────────────────
    fun getTrustedContacts(
        blindId: String,
        onResult: (List<TrustedContact>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("users").document(blindId).collection("trusted_contacts")
            .get()
            .addOnSuccessListener { snapshot ->
                val contacts = mutableListOf<TrustedContact>()
                for (doc in snapshot.documents) {
                    try {
                        val encName = doc.getString("encName") ?: ""
                        val encRelation = doc.getString("encRelation") ?: ""
                        val encPhone = doc.getString("encPhone") ?: "" // NEW

                        val decName = CryptoHelper.decrypt(encName)
                        val decRelation = CryptoHelper.decrypt(encRelation)
                        val decPhone = CryptoHelper.decrypt(encPhone) // NEW

                        contacts.add(
                            TrustedContact(
                                id = doc.getString("id") ?: doc.id,
                                name = decName,
                                relation = decRelation,
                                phoneNumber = decPhone, // NEW
                                liveAudio = doc.getBoolean("liveAudio") ?: false,
                                locationHistory = doc.getBoolean("locationHistory") ?: false,
                                sosNotification = doc.getBoolean("sosNotification") ?: false
                            )
                        )
                    } catch (e: Exception) {
                        // Safely skip corrupted data
                    }
                }
                onResult(contacts)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }
    // ── Delete Trusted Contact ──────────────────────────────────────────────
    fun deleteTrustedContact(
        blindId: String,
        contactId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("users").document(blindId)
            .collection("trusted_contacts").document(contactId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // ── Save Primary Guardian ──────────────────────────────────────────────
    fun setGuardianPhone(blindId: String, phone: String) {
        val updateData = mapOf("guardianPhone" to phone)

        // Call getInstance() directly here instead of using 'db'
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(blindId)
            .set(updateData, com.google.firebase.firestore.SetOptions.merge())
    }

}
