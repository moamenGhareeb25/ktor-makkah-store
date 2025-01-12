package com.example.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.util.*

object Firebase {
    fun init() {
        try {
            val firebaseCredentials = System.getenv("FIREBASE_CREDENTIALS")
                ?: throw IllegalStateException("FIREBASE_CREDENTIALS not set")

            // Decode the Base64 string
            val decodedBytes = Base64.getDecoder().decode(firebaseCredentials)
            val serviceAccount = decodedBytes.inputStream()

            // Initialize Firebase
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setStorageBucket("your-bucket-name.appspot.com")
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                println("Firebase initialized successfully!")
            } else {
                println("Firebase already initialized.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error initializing Firebase: ${e.message}")
        }
    }
}

