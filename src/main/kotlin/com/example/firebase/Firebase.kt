package com.example.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream

object Firebase {
    fun init() {
        try {
            // Path to your service account key file
            val serviceAccountPath = "C:/Users/moame/Downloads/makkah-store-operations-firebase-adminsdk-n1ol9-921f8322d9.json"

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
