package com.example.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*
import java.io.ByteArrayInputStream
import java.util.Base64

object Firebase {
    private var firebaseConfig: FirebaseConfig? = null

    fun init(): FirebaseConfig {
        if (firebaseConfig != null) return firebaseConfig!!

        try {
            val firebaseConfigRaw = System.getenv("FIREBASE_CONFIG")
                ?: throw IllegalStateException("❌ FIREBASE_CONFIG not set in Render environment.")

            // 🔹 Decode if base64, otherwise use as-is
            val decodedJson = if (firebaseConfigRaw.startsWith("{")) {
                firebaseConfigRaw
            } else {
                String(Base64.getDecoder().decode(firebaseConfigRaw))
            }

            println("🔍 Decoded Firebase Config: $decodedJson")

            val config = Json { ignoreUnknownKeys = true }.decodeFromString<FirebaseConfig>(decodedJson)

            val credentialsStream = ByteArrayInputStream(decodedJson.toByteArray())

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                .setStorageBucket(config.storage_bucket)
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                println("✅ Firebase initialized successfully!")
            }

            firebaseConfig = config
            return config

        } catch (e: Exception) {
            e.printStackTrace()
            throw IllegalStateException("❌ Firebase Initialization Failed: ${e.message}")
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
    @SerialName("database_url") val database_url: String? = null, // ✅ Make Optional
    @SerialName("storage_bucket") val storage_bucket: String,
    @SerialName("auth_api_key") val auth_api_key: String,
    @SerialName("messaging_sender_id") val messaging_sender_id: String,
    @SerialName("app_id") val app_id: String,
    @SerialName("measurement_id") val measurement_id: String,
    @SerialName("fcm_server_key") val fcm_server_key: String,
    @SerialName("web_push_certificate_key") val web_push_certificate_key: String
)

