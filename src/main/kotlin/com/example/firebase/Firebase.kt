package com.example.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.util.*

object Firebase {
    private var firebaseConfig: FirebaseConfig? = null

    @Synchronized
    fun init(): FirebaseConfig {
        if (firebaseConfig != null) return firebaseConfig!!

        try {
            // 🔹 Retrieve & Decode Base64 Firebase Config
            val firebaseBase64 = System.getenv("FIREBASE_CONFIG")
                ?: throw IllegalStateException("❌ FIREBASE_CONFIG is not set in the environment. Please ensure the environment variable is correctly configured.")

            val decodedBytes = Base64.getDecoder().decode(firebaseBase64)
            val firebaseConfigJson = String(decodedBytes, Charsets.UTF_8)

            println("🔍 Decoded Firebase JSON:\n$firebaseConfigJson") // Debugging

            // 🔹 Validate JSON format
            try {
                Json.parseToJsonElement(firebaseConfigJson)
                println("✅ JSON is valid and well-formed")
            } catch (e: Exception) {
                throw IllegalStateException("❌ Invalid JSON format after decoding: ${e.message}")
            }

            // 🔹 Deserialize JSON
            val config = Json.decodeFromString<FirebaseConfig>(firebaseConfigJson)
                .also { cfg ->
                    if (cfg.project_id.isBlank() || cfg.private_key.isBlank() || cfg.client_email.isBlank()) {
                        throw IllegalStateException("❌ Invalid Firebase configuration. Required fields are missing.")
                    }
                }

            // 🔹 Fix Private Key Formatting
            val formattedPrivateKey = config.private_key.replace("\\n", "\n").trim()

            if (!formattedPrivateKey.startsWith("-----BEGIN PRIVATE KEY-----") ||
                !formattedPrivateKey.endsWith("-----END PRIVATE KEY-----")) {
                throw IllegalStateException("❌ Invalid private key format. Ensure the private key is correctly formatted.")
            }

            // 🔹 Pass Correct JSON to Firebase
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(ByteArrayInputStream(firebaseConfigJson.toByteArray(Charsets.UTF_8))))
                .setDatabaseUrl(config.database_url ?: throw IllegalStateException("❌ Database URL is missing in the Firebase configuration."))
                .build()

            // 🔹 Initialize Firebase App
            val appName = "makkah-store-operations"

            if (FirebaseApp.getApps().isEmpty() || FirebaseApp.getApps().none { it.name == appName }) {
                FirebaseApp.initializeApp(options, appName)
                println("✅ Firebase initialized successfully with app name: $appName")
            }

            firebaseConfig = config
            return config

        } catch (e: Exception) {
            println("❌ Firebase Initialization Failed: ${e.message}")
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
    @SerialName("database_url") val database_url: String? = null,
    @SerialName("storage_bucket") val storage_bucket: String,
    @SerialName("auth_api_key") val auth_api_key: String,
    @SerialName("messaging_sender_id") val messaging_sender_id: String,
    @SerialName("app_id") val app_id: String,
    @SerialName("measurement_id") val measurement_id: String,
    @SerialName("fcm_server_key") val fcm_server_key: String,
    @SerialName("web_push_certificate_key") val web_push_certificate_key: String
)