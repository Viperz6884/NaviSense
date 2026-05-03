package com.example.companion

import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SignUpScreen(navController: androidx.navigation.NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Blind") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val hasMinLength = password.length >= 8
    val hasUppercase = password.any { it.isUpperCase() }
    val hasLowercase = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecialChar = password.any { !it.isLetterOrDigit() }
    val isPasswordValid = hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecialChar

    fun isValidEmail(target: CharSequence) = target.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(target).matches()

    // The Google Sign-In Launcher (Uses selectedRole for new accounts!)
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleGoogleAuthResult(result.data, context, navController, selectedRole, "signup") { isLoading = it }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Create Account", fontSize = 28.sp, modifier = Modifier.padding(bottom = 24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            singleLine = true
        )

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

        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(text = "Password must contain:", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
            PasswordRequirementRow(text = "At least 8 characters", isMet = hasMinLength)
            PasswordRequirementRow(text = "1 uppercase letter", isMet = hasUppercase)
            PasswordRequirementRow(text = "1 lowercase letter", isMet = hasLowercase)
            PasswordRequirementRow(text = "1 number", isMet = hasDigit)
            PasswordRequirementRow(text = "1 special character", isMet = hasSpecialChar)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "I am a:")
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedRole == "Blind", onClick = { selectedRole = "Blind" })
                Text("Blind Person")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedRole == "Guardian", onClick = { selectedRole = "Guardian" })
                Text("Guardian")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (!isValidEmail(email)) {
                    Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isLoading = true
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null) {

                                val userProfile = hashMapOf(
                                    "name" to name.trim(),
                                    "email" to email.trim(),
                                    "role" to selectedRole,
                                    "pairedWith" to ""
                                )

                                FirebaseFirestore.getInstance().collection("Users").document(user.uid)
                                    .set(userProfile)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Account Created!", Toast.LENGTH_SHORT).show()

                                        // FIXED: Now we actually navigate them to their specific home screen!
                                        if (selectedRole == "Blind") {
                                            navController.navigate("blindHome") { popUpTo("signup") { inclusive = true } }
                                        } else {
                                            navController.navigate("guardianConnect") { popUpTo("signup") { inclusive = true } }
                                        }
                                    }
                            }
                        } else {
                            isLoading = false // Don't forget to stop loading on failure!
                            Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            // FIXED: Added name.isNotBlank() so the button stays greyed out until they type a name
            enabled = !isLoading && isPasswordValid && email.isNotEmpty() && name.isNotBlank()
        ) {
            if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp)) else Text("Sign Up")
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
            Text("Sign Up with Google")
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { navController.navigate("login") { popUpTo("signup") { inclusive = true } } }) {
            Text("Already have an account? Log In")
        }
    }
}

@Composable
fun PasswordRequirementRow(text: String, isMet: Boolean) {
    val color = if (isMet) Color(0xFF4CAF50) else Color.Gray
    val icon = if (isMet) Icons.Default.Check else Icons.Default.Close
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = color, fontSize = 12.sp)
    }
}