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
                ?: throw IllegalStateException("❌ FIREBASE_CONFIG is not set. Ensure it is correctly configured in environment variables.")

            val firebaseConfigJson = String(Base64.getDecoder().decode(firebaseBase64)).trim()

            println("🔍 Decoded Firebase JSON (First 500 chars):\n${firebaseConfigJson.take(500)}...")

            // 🔹 Deserialize JSON into FirebaseConfig data class
            val config = Json.decodeFromString<FirebaseConfig>(firebaseConfigJson)

            // 🔹 Fix Private Key Formatting
            val formattedPrivateKey = config.private_key
                .replace("\\n", "\n")
                .trim()

            // 🔹 Ensure JSON is valid and 'type' field is present
            if (config.type != "service_account") {
                throw IllegalStateException("❌ Invalid Firebase credentials: 'type' must be 'service_account'.")
            }

            // 🔹 Create a corrected JSON string with properly formatted private key
            val correctedJson = Json.encodeToString(FirebaseConfig.serializer(), config.copy(private_key = formattedPrivateKey))

            // 🔹 Convert JSON to InputStream for Firebase SDK
            val credentials = GoogleCredentials.fromStream(ByteArrayInputStream(correctedJson.toByteArray()))

            // 🔹 Initialize Firebase App
            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setDatabaseUrl(config.database_url)
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                println("✅ Firebase initialized successfully.")
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

@Serializable
data class FirebaseConfig(
    @SerialName("type") val type: String,
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
    @SerialName("storage_bucket") val storage_bucket: String
)
