package com.example.companion

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LandingPage(onBlindClick: () -> Unit, onGuardianClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome to Companion", fontSize = 28.sp, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(48.dp))

        Button(onClick = onBlindClick, modifier = Modifier.fillMaxWidth().height(80.dp)) {
            Text("I am a Blind Person", fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onGuardianClick, modifier = Modifier.fillMaxWidth().height(80.dp)) {
            Text("I am a Guardian", fontSize = 20.sp)
        }
    }
}