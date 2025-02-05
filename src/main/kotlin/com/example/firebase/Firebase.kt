package com.example.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

object Firebase {
    private var initialized = false

    fun init() {
        if (initialized) {
            println("✅ Firebase already initialized.")
            return
        }

        try {
            val firebaseConfigFile = File("/etc/secrets/serviceAccountKey.json") // 🔹 Read from secret file
            if (!firebaseConfigFile.exists()) {
                throw IllegalStateException("❌ Firebase config file not found at /etc/secrets/serviceAccountKey.json")
            }

            // ✅ Ensure JSON is well-formatted
            val jsonContent = firebaseConfigFile.readText()
            println("🔍 Firebase Config JSON: $jsonContent") // Log JSON for debugging

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(firebaseConfigFile.inputStream()))
                .setDatabaseUrl("https://makkah-store-operations-default-rtdb.firebaseio.com/") // ✅ Ensure database URL is set
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                println("✅ Firebase initialized successfully!")
            } else {
                println("✅ Firebase already initialized.")
            }

            initialized = true

        } catch (e: Exception) {
            println("❌ Firebase Initialization Failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}



// 🔹 Firebase Configuration Data Class

@Serializable
data class FirebaseConfig(
    @SerialName("type") val type: String = "service_account",
    @SerialName("project_id") val project_id: String,
    @SerialName("private_key_id") val private_key_id: String,
    @SerialName("private_key") val private_key: String,
    @SerialName("client_email") val client_email: String,
    @SerialName("client_id") val client_id: String,
    @SerialName("auth_uri") val auth_uri: String,
    @SerialName("token_uri") val token_uri: String,
    @SerialName("auth_provider_x509_cert_url") val auth_provider_x509_cert_url: String,
    @SerialName("client_x509_cert_url") val client_x509_cert_url: String,
    @SerialName("database_url") val database_url: String,
    @SerialName("storage_bucket") val storage_bucket: String,
    @SerialName("auth_api_key") val auth_api_key: String,
    @SerialName("messaging_sender_id") val messaging_sender_id: String,
    @SerialName("app_id") val app_id: String,
    @SerialName("measurement_id") val measurement_id: String,
    @SerialName("fcm_server_key") val fcm_server_key: String,
    @SerialName("web_push_certificate_key") val web_push_certificate_key: String
)

