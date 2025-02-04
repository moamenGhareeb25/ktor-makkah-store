package com.example.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

object Firebase {
    private val json = Json { ignoreUnknownKeys = true }
    fun init() {
        try {
            val firebaseConfigRaw = System.getenv("FIREBASE_CONFIG")
                ?: throw IllegalStateException("❌ FIREBASE_CONFIG not set. Make sure it's added to Render.")

            val decodedJson = if (firebaseConfigRaw.startsWith("{")) {
                // 🔹 JSON is already in plain text, use it directly
                firebaseConfigRaw
            } else {
                // 🔹 Decode from Base64 (only if needed)
                String(Base64.getDecoder().decode(firebaseConfigRaw))
            }

            val firebaseConfig = try {
                Json.decodeFromString<FirebaseConfig>(decodedJson)
            } catch (e: Exception) {
                throw IllegalStateException("❌ Error parsing FirebaseConfig JSON: ${e.message}")
            }

            println("🔥 Firebase Configuration Loaded:")
            println("📌 Project ID: ${firebaseConfig.project_id}")
            println("📌 Database URL: ${firebaseConfig.database_url}")
            println("📌 Storage Bucket: ${firebaseConfig.storage_bucket}")

            val tempFile = File.createTempFile("firebase-admin", ".json").apply {
                writeText(decodedJson.replace("\\n", "\n"))
                deleteOnExit()
            }

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
            println("❌ Firebase Initialization Failed: ${e.message}")
        }
    }
}



    // 🔹 Firebase Configuration Data Class
@Serializable
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
    val fcm_server_key: String,
    val database_url: String,
    val storage_bucket: String,
    val auth_api_key: String
)
