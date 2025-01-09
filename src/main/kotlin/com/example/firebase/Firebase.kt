package com.example.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream

object Firebase {
    fun init() {
        try {
            // Read the path from the environment variable
            val serviceAccountPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH")
                ?: throw IllegalStateException("FIREBASE_SERVICE_ACCOUNT_PATH not set")

            // Initialize Firebase using the credentials file
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(FileInputStream(serviceAccountPath)))
                .build()

            FirebaseApp.initializeApp(options)
            println("Firebase initialized successfully!")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error initializing Firebase: ${e.message}")
        }
    }
}