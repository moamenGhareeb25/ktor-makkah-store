package com.example.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.util.*

object Firebase {
    fun init() {
        try {
            val firebaseBase64 = System.getenv("FIREBASE_CREDENTIALS")
                ?: throw IllegalStateException("❌ FIREBASE_CREDENTIALS not set")

            // Decode Base64 to JSON
            val decodedJson = String(Base64.getDecoder().decode(firebaseBase64))
            val serviceAccount = decodedJson.byteInputStream()

            // Initialize Firebase
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setStorageBucket("makkah-store-operations.appspot.com") // Update with your bucket
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                println("✅ Firebase initialized successfully!")
            } else {
                println("✅ Firebase already initialized.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Error initializing Firebase: ${e.message}")
        }
    }
}
