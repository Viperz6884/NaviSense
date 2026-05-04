package com.example.companion

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions // <-- NEW IMPORT REQUIRED FOR MERGING

fun getGoogleSignInClient(context: Context): com.google.android.gms.auth.api.signin.GoogleSignInClient {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail() // This automatically requests their profile (name) as well!
        .build()
    return GoogleSignIn.getClient(context, gso)
}

fun handleGoogleAuthResult(
    intent: Intent?,
    context: Context,
    navController: NavController,
    fallbackRole: String,
    originRoute: String,
    onLoadingChange: (Boolean) -> Unit
) {
    onLoadingChange(true)
    GoogleSignIn.getSignedInAccountFromIntent(intent).addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
            val auth = FirebaseAuth.getInstance()

            auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val user = auth.currentUser
                    val db = FirebaseFirestore.getInstance()

                    if (user != null) {
                        // NEW: Grab the name directly from the Google account object
                        val googleName = user.displayName ?: "Companion User"

                        db.collection("Users").document(user.uid).get()
                            .addOnSuccessListener { doc ->
                                if (doc != null && doc.exists()) {
                                    // Scenario 1: The user exists in the database. Let them in!
                                    val role = doc.getString("role") ?: "Blind"

                                    // NEW: We quietly update their name in the database just in case
                                    // they signed up before we added the name feature!
                                    val updateData = hashMapOf("name" to googleName)
                                    db.collection("Users").document(user.uid).set(updateData, SetOptions.merge())

                                    onLoadingChange(false)
                                    Toast.makeText(context, "Logged in successfully!", Toast.LENGTH_SHORT).show()

                                    if (role == "Blind") {
                                        navController.navigate("blindHome") { popUpTo(originRoute) { inclusive = true } }
                                    } else {
                                        navController.navigate("guardianConnect") { popUpTo(originRoute) { inclusive = true } }
                                    }
                                } else {
                                    // Scenario 2: The user DOES NOT exist in the database.
                                    if (originRoute == "login") {
                                        // If they clicked the Google button on the LOGIN screen, block them.
                                        onLoadingChange(false)
                                        Toast.makeText(context, "No account found. Please Sign Up first.", Toast.LENGTH_LONG).show()

                                        auth.signOut()
                                        getGoogleSignInClient(context).signOut()

                                    } else {
                                        // If they clicked the Google button on the SIGN-UP screen, create the account.
                                        val userProfile = hashMapOf(
                                            "name" to googleName, // <-- NEW: Saving the name to the fresh profile!
                                            "email" to (user.email ?: ""),
                                            "role" to fallbackRole,
                                            "pairedWith" to ""
                                        )
                                        db.collection("Users").document(user.uid).set(userProfile)
                                            .addOnSuccessListener {
                                                onLoadingChange(false)
                                                Toast.makeText(context, "Google Account Created!", Toast.LENGTH_SHORT).show()
                                                if (fallbackRole == "Blind") {
                                                    navController.navigate("blindHome") { popUpTo(originRoute) { inclusive = true } }
                                                } else {
                                                    navController.navigate("guardianConnect") { popUpTo(originRoute) { inclusive = true } }
                                                }
                                            }
                                    }
                                }
                            }
                    }
                } else {
                    onLoadingChange(false)
                    Toast.makeText(context, "Firebase Auth Failed", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            onLoadingChange(false)
            Toast.makeText(context, "Google Sign-In Canceled", Toast.LENGTH_SHORT).show()
        }
    }
}