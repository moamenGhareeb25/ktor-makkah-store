package com.example.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.File
import java.util.*

object Firebase {
    fun init() {
        try {
            val firebaseBase64 = System.getenv("FIREBASE_CREDENTIALS")

            if (firebaseBase64.isNullOrEmpty()) {
                println("❌ FIREBASE_CREDENTIALS is missing! Firebase will NOT be initialized.")
                return
            }

            // Decode Base64 to JSON
            val decodedJson = String(Base64.getDecoder().decode(firebaseBase64))

            // Write the JSON to a temporary file (Render does not support in-memory Firebase credentials)
            val tempFile = File.createTempFile("firebase-admin", ".json").apply {
                writeText(decodedJson)
            }

            // Initialize Firebase
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(tempFile.inputStream()))
                .setStorageBucket(System.getenv("FIREBASE_STORAGE_BUCKET") ?: "makkah-store-operations.appspot.com") // Use env variable
                .build()

            synchronized(this) {
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options)
                    println("✅ Firebase initialized successfully!")
                } else {
                    println("✅ Firebase already initialized.")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Error initializing Firebase: ${e.message}")
        }
    }
}
