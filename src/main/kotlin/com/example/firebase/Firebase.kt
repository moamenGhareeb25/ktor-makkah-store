package com.example.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.util.*

object Firebase {
    fun init() {
        try {
            if (FirebaseApp.getApps().isNotEmpty()) {
                println("✅ Firebase already initialized.")
                return
            }

            val firebaseCredentials = System.getenv("FIREBASE_CREDENTIALS")
                ?: throw IllegalStateException("❌ FIREBASE_CREDENTIALS environment variable is missing")

            val storageBucket = System.getenv("FIREBASE_STORAGE_BUCKET")
                ?: throw IllegalStateException("❌ FIREBASE_STORAGE_BUCKET is missing")

            // Decode Base64 credentials
            val decodedBytes = Base64.getDecoder().decode(firebaseCredentials)
            val serviceAccount = decodedBytes.inputStream()

            // Initialize Firebase
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setStorageBucket(storageBucket)
                .build()

            FirebaseApp.initializeApp(options)
            println("✅ Firebase initialized successfully!")

        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Firebase initialization failed: ${e.message}")
        }
    }
}
