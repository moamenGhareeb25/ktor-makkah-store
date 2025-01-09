package com.example.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.github.cdimascio.dotenv.dotenv
import java.io.FileInputStream

object Firebase {
    fun init() {
        val env = dotenv {
            directory = "./"
            filename = ".env"
        }
        println("Loaded .env file: ${env.entries()}")
        try {
            val serviceAccountPath = env["FIREBASE_SERVICE_ACCOUNT_PATH"]
                ?: throw IllegalStateException("FIREBASE_SERVICE_ACCOUNT_PATH not set")

            println("Firebase service account path: $serviceAccountPath")

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