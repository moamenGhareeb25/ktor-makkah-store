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

            // 🔹 Ensure JSON has 'type' field and it is 'service_account'
            if (config.type != "service_account") {
                throw IllegalStateException("❌ Invalid Firebase credentials: 'type' must be 'service_account'.")
            }

            // 🔹 Fix Private Key Formatting
            val formattedPrivateKey = config.privateKey
                .replace("\\n", "\n") // Convert escaped newlines
                .trim()

            // ✅ Create a corrected FirebaseConfig instance
            val correctedConfig = config.copy(privateKey = formattedPrivateKey)

            // ✅ Serialize it back to JSON
            val correctedJson = Json.encodeToString(FirebaseConfig.serializer(), correctedConfig)

            // ✅ Convert JSON to InputStream for Firebase SDK
            val credentials = GoogleCredentials.fromStream(ByteArrayInputStream(correctedJson.toByteArray()))

            // ✅ Initialize Firebase App
            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                println("✅ Firebase initialized successfully.")
            }

            firebaseConfig = correctedConfig
            return correctedConfig

        } catch (e: Exception) {
            println("❌ Firebase Initialization Failed: ${e.message}")
            e.printStackTrace()
            throw IllegalStateException("❌ Firebase Initialization Failed: ${e.message}")
        }
    }
}

@Serializable
data class FirebaseConfig(
    @SerialName("type") val type: String = "service_account",
    @SerialName("project_id") val projectId: String,
    @SerialName("private_key_id") val privateKeyId: String,
    @SerialName("private_key") val privateKey: String,  // ✅ Match with JSON key
    @SerialName("client_email") val clientEmail: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("auth_uri") val authUri: String,
    @SerialName("token_uri") val tokenUri: String,
    @SerialName("auth_provider_x509_cert_url") val authProviderCertUrl: String,
    @SerialName("client_x509_cert_url") val clientCertUrl: String,
    @SerialName("database_url") val databaseUrl: String? = null,
    @SerialName("storage_bucket") val storageBucket: String,
    @SerialName("auth_api_key") val authApiKey: String,
    @SerialName("messaging_sender_id") val messagingSenderId: String,
    @SerialName("app_id") val appId: String,
    @SerialName("measurement_id") val measurementId: String,
    @SerialName("fcm_server_key") val fcmServerKey: String,  // ✅ Ensure correct mapping
    @SerialName("web_push_certificate_key") val webPushCertificateKey: String
)
