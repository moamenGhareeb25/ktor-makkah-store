package com.example.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

object Firebase {
    fun init() {
        try {
            val firebaseBase64 = System.getenv("FIREBASE_CONFIG")
                ?: throw IllegalStateException("❌ FIREBASE_CONFIG not set")

            val decodedJson = String(Base64.getDecoder().decode(firebaseBase64))

            // Parse JSON into a structured object
            val firebaseConfig = Json.decodeFromString<FirebaseConfig>(decodedJson)

            val tempFile = File.createTempFile("firebase-admin", ".json").apply {
                writeText(decodedJson)
            }

            // Initialize Firebase with credentials
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(tempFile.inputStream()))
                .setDatabaseUrl(firebaseConfig.database_url)
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

// 🔹 FirebaseConfig Data Class (holds Firebase details)
@kotlinx.serialization.Serializable
data class FirebaseConfig(
    val type: String,
    val project_id: String,
    val private_key_id: String,
    val private_key: String,
    val client_email: String,
    val client_id: String,
    val auth_uri: String,
    val token_uri: String,
    val auth_provider_x509_cert_url: String,
    val client_x509_cert_url: String,
    val universe_domain: String,
    val fcm_server_key: String,   // ✅ Added FCM Key
    val database_url: String,     // ✅ Added database URL
    val storage_bucket: String,   // ✅ Added Storage Bucket
    val auth_api_key: String      // ✅ Added API Key
)

