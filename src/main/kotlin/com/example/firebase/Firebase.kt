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

    fun init(): FirebaseConfig {
        if (firebaseConfig != null) return firebaseConfig!!

        try {
            // 🔹 Retrieve & Decode Base64 Firebase Config
            val firebaseBase64 = System.getenv("FIREBASE_CONFIG")
                ?: throw IllegalStateException("❌ FIREBASE_CONFIG is not set in the environment.")

            val firebaseConfigJson = String(Base64.getDecoder().decode(firebaseBase64))

            // 🔹 Deserialize JSON
            val config = Json.decodeFromString<FirebaseConfig>(firebaseConfigJson)

            // 🔹 Fix Private Key Formatting
            val formattedPrivateKey = config.private_key
                .replace("\\n", "\n") // 🔥 Ensure newlines are correct

            // 🔹 Convert JSON back to correct format with fixed private key
            val correctedJson = Json.encodeToString(FirebaseConfig.serializer(), config.copy(private_key = formattedPrivateKey))

            // 🔹 Convert JSON to InputStream for Firebase SDK
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(ByteArrayInputStream(correctedJson.toByteArray())))
                .setDatabaseUrl(config.database_url)  // ✅ Ensure Database URL is set
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
    @SerialName("database_url") val database_url: String? = null,
    @SerialName("storage_bucket") val storage_bucket: String,
    @SerialName("auth_api_key") val auth_api_key: String,
    @SerialName("messaging_sender_id") val messaging_sender_id: String,
    @SerialName("app_id") val app_id: String,
    @SerialName("measurement_id") val measurement_id: String,
    @SerialName("fcm_server_key") val fcm_server_key: String,
    @SerialName("web_push_certificate_key") val web_push_certificate_key: String
)
