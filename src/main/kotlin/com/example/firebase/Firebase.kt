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
            val firebaseBase64 = System.getenv("FIREBASE_CONFIG")
                ?: throw IllegalStateException("❌ FIREBASE_CONFIG not set. Make sure it's added to Render.")

            val decodedJson = try {
                String(Base64.getDecoder().decode(firebaseBase64))
            } catch (e: Exception) {
                throw IllegalStateException("❌ Error decoding FIREBASE_CONFIG: ${e.message}")
            }

            // 🔹 Normalize keys to match FirebaseConfig data class
            val normalizedJson = decodedJson
                .replace("FIREBASE_PROJECT_ID", "project_id")
                .replace("FIREBASE_PRIVATE_KEY_ID", "private_key_id")
                .replace("FIREBASE_PRIVATE_KEY", "private_key")
                .replace("FIREBASE_CLIENT_EMAIL", "client_email")
                .replace("FIREBASE_CLIENT_ID", "client_id")
                .replace("FIREBASE_AUTH_URI", "auth_uri")
                .replace("FIREBASE_TOKEN_URI", "token_uri")
                .replace("FIREBASE_AUTH_PROVIDER_X509_CERT_URL", "auth_provider_x509_cert_url")
                .replace("FIREBASE_CLIENT_X509_CERT_URL", "client_x509_cert_url")
                .replace("FIREBASE_DATABASE_URL", "database_url")
                .replace("FIREBASE_STORAGE_BUCKET", "storage_bucket")
                .replace("FIREBASE_AUTH_API_KEY", "auth_api_key")
                .replace("FCM_SERVER_KEY", "fcm_server_key")

            val firebaseConfig = try {
                json.decodeFromString<FirebaseConfig>(normalizedJson)
            } catch (e: Exception) {
                throw IllegalStateException("❌ Error parsing FirebaseConfig JSON: ${e.message}")
            }

            // ✅ LOG extracted Firebase Configuration (PARTIALLY for security)
            println("🔥 Firebase Configuration Loaded:")
            println("📌 Project ID: ${firebaseConfig.project_id}")
            println("📌 Database URL: ${firebaseConfig.database_url}")
            println("📌 Storage Bucket: ${firebaseConfig.storage_bucket}")
            println("📌 Auth API Key (First 10 chars): ${firebaseConfig.auth_api_key.take(10)}...")
            println("📌 FCM Server Key (First 10 chars): ${firebaseConfig.fcm_server_key.take(10)}...")

            // Create a temporary file for Firebase credentials
            val tempFile = File.createTempFile("firebase-admin", ".json").apply {
                writeText(normalizedJson.replace("\\n", "\n"))
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
