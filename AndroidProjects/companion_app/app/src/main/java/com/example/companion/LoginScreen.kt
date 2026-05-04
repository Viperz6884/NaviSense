package com.example.companion

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(navController: androidx.navigation.NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // The Google Sign-In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleGoogleAuthResult(result.data, context, navController, "Blind", "login") { isLoading = it }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome Back", fontSize = 28.sp, modifier = Modifier.padding(bottom = 32.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                if (user != null && user.isEmailVerified) {
                                    db.collection("Users").document(user.uid).get()
                                        .addOnSuccessListener { document ->
                                            isLoading = false
                                            if (document != null && document.exists()) {
                                                val role = document.getString("role")
                                                Toast.makeText(context, "Logged in!", Toast.LENGTH_SHORT).show()
                                                if (role == "Blind") {
                                                    navController.navigate("blindHome") { popUpTo("login") { inclusive = true } }
                                                } else {
                                                    navController.navigate("guardianConnect") { popUpTo("login") { inclusive = true } }
                                                }
                                            }
                                        }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Please verify your email first!", Toast.LENGTH_LONG).show()
                                    auth.signOut()
                                }
                            } else {
                                isLoading = false
                                Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp)) else Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("OR", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        // The New Google Button
        OutlinedButton(
            onClick = {
                val intent = getGoogleSignInClient(context).signInIntent
                googleSignInLauncher.launch(intent)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !isLoading
        ) {
            Text("Continue with Google")
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { navController.navigate("signup") }) {
            Text("Don't have an account? Sign Up")
        }
    }
}