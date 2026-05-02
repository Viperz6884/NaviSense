package com.example.companion

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianConnectScreen(onConnected: () -> Unit) {
    val context = LocalContext.current
    var codeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Connect to\nBlind Person",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Ask the blind person for their\n6-character Companion ID",
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(40.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = {
                            if (it.length <= 6) {
                                codeInput = it.uppercase().filter { c -> c.isLetterOrDigit() }
                                errorMessage = ""
                            }
                        },
                        label = { Text("Companion ID") },
                        placeholder = { Text("e.g. A3F9K2") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Ascii
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 6.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessage.isNotEmpty(),
                        supportingText = {
                            if (errorMessage.isNotEmpty()) {
                                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("${codeInput.length}/6 characters")
                            }
                        }
                    )

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (codeInput.length != 6) {
                                errorMessage = "Please enter a full 6-character ID"
                            } else {
                                isConnecting = true
                                val prefs = context.getSharedPreferences(
                                    "companion_prefs", Context.MODE_PRIVATE
                                )
                                // Guardian's own phone number saved locally
                                val guardianPhone = prefs.getString("guardian_phone", "") ?: ""

                                // ── Validate ID exists in Firestore ───────────
                                FirebaseManager.connectGuardian(
                                    blindId = codeInput,
                                    guardianPhone = guardianPhone,
                                    connectedBlindId = codeInput,
                                    onSuccess = {
                                        prefs.edit()
                                            .putString("connected_blind_id", codeInput)
                                            .apply()
                                        isConnecting = false
                                        onConnected()
                                    },
                                    onFailed = { err ->
                                        isConnecting = false
                                        errorMessage = err
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isConnecting
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Connect & Track",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}