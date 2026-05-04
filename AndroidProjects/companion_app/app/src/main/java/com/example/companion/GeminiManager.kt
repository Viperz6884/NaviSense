package com.example.companion

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiManager {

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "YOUR_GEMINI_API_KEY_HERE"
    )

    suspend fun processCommand(userInput: String): String {
        return withContext(Dispatchers.IO) {

            val prompt = """
You are an assistant for a navigation app.

Task:
1. Detect if user wants navigation or calling guardian.
2. Extract ONLY the destination place.
3. Translate place into simple English if needed.

Return ONLY in one of these formats:
NAVIGATION:<place in English>
CALL_GUARDIAN
UNKNOWN

Examples:
"mujhe metro station le chalo" → NAVIGATION:metro station
"nearest hospital" → NAVIGATION:hospital
"airport jana hai" → NAVIGATION:airport
"call my guardian" → CALL_GUARDIAN

User input: "$userInput"
        """.trimIndent()

            val response = model.generateContent(prompt)
            response.text ?: "UNKNOWN"
        }
    }
}
